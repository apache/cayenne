/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.Fault;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

import java.util.Map;

/**
 * {@link DataContext} delegates object snapshot creation to this class
 *
 * @see DataContext#currentSnapshot(Persistent)
 */
class DataContextSnapshotBuilder implements PropertyVisitor {

    private final EntityResolver resolver;
    private final ObjectStore objectStore;
    private final Persistent object;
    private DataRow snapshot;

    DataContextSnapshotBuilder(EntityResolver resolver, ObjectStore objectStore, Persistent object) {
        this.resolver = resolver;
        this.objectStore = objectStore;
        this.object = object;
    }

    public DataRow build() {
        if (object.getPersistenceState() == PersistenceState.HOLLOW
                && object.getObjectContext() != null) {
            return objectStore.getSnapshot(object.getObjectId());
        }

        ObjEntity entity = resolver.getObjEntity(object);
        final ClassDescriptor descriptor = resolver.getClassDescriptor(entity.getName());
        snapshot = new DataRow(10);
        snapshot.setEntityName(entity.getName());

        descriptor.visitProperties(this);

        // process object id map
        // we should ignore any object id values if a corresponding attribute
        // is a part of relationship "toMasterPK", since those values have been
        // set above when db relationships where processed.
        Map<String, Object> thisIdParts = object.getObjectId().getIdSnapshot();
        if (thisIdParts != null) {

            // put only those that do not exist in the map
            for (Map.Entry<String, Object> entry : thisIdParts.entrySet()) {
                String nextKey = entry.getKey();
                if (!snapshot.containsKey(nextKey)) {
                    snapshot.put(nextKey, entry.getValue());
                }
            }
        }

        return snapshot;
    }

    public boolean visitAttribute(AttributeProperty property) {
        ObjAttribute objAttr = property.getAttribute();
        // processing compound attributes correctly
        snapshot.put(objAttr.getDbAttributePath().value(), property.readPropertyDirectly(object));
        return true;
    }

    public boolean visitToMany(ToManyProperty property) {
        // do nothing
        return true;
    }

    public boolean visitToOne(ToOneProperty property) {
        ObjRelationship rel = property.getRelationship();

        // if target doesn't propagate its key value, skip it
        if (rel.isSourceIndependentFromTargetChange()) {
            return true;
        }

        Object targetObject = property.readPropertyDirectly(object);
        if (targetObject == null) {
            return true;
        }

        // if target is Fault, get id attributes from stored snapshot to avoid unneeded fault triggering
        if (targetObject instanceof Fault) {
            DataRow storedSnapshot = objectStore.getSnapshot(object.getObjectId());
            if (storedSnapshot == null) {
                throw new CayenneRuntimeException("No matching objects found for ObjectId %s"
                        + ". Object may have been deleted externally.", object.getObjectId());
            }

            DbRelationship dbRel = rel.getDbRelationships().get(0);
            for (DbJoin join : dbRel.getJoins()) {
                String key = join.getSourceName();
                snapshot.put(key, storedSnapshot.get(key));
            }

            return true;
        }

        // target is resolved, and we have an FK->PK to it, so extract it from target...
        Persistent target = (Persistent) targetObject;
        Map<String, Object> idParts = target.getObjectId().getIdSnapshot();

        // this may happen in uncommitted objects - see the warning in
        // the JavaDoc of this method.
        if (idParts.isEmpty()) {
            return true;
        }

        DbRelationship dbRel = rel.getDbRelationships().get(0);
        Map<String, Object> fk = dbRel.srcFkSnapshotWithTargetSnapshot(idParts);
        snapshot.putAll(fk);
        return true;
    }
}
