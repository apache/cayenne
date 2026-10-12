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
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyDescriptor;
import org.apache.cayenne.reflect.ToOneProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds {@link DataRow} snapshots of persistent objects from their current, possibly uncommitted, in-memory state.
 *
 * @since 5.0
 */
public final class ObjectSnapshots {

    private ObjectSnapshots() {
    }

    /**
     * Returns a {@link DataRow} reflecting the current, possibly uncommitted, state of an object registered in the
     * given context, with the given snapshot version.
     * <p>
     * <strong>Warning:</strong> the snapshot is partial if the object, or a related object that propagates its key
     * to it, has a temporary id. Do not use it where a DataRow has to represent the complete object state.
     *
     * @param context the context the object is registered in
     * @param object  the object to snapshot
     * @param version the snapshot version of the returned row
     */
    public static DataRow snapshot(DataContext context, Persistent object, long version) {
        return new Builder(context.getEntityResolver(), context.getObjectStore(), object, version).build();
    }

    private static final class Builder {

        private final EntityResolver resolver;
        private final DataContextObjectStore objectStore;
        private final Persistent object;
        private final long version;
        private final HashMap<String, Object> values = new HashMap<>(10);

        Builder(EntityResolver resolver, DataContextObjectStore objectStore, Persistent object, long version) {
            this.resolver = resolver;
            this.objectStore = objectStore;
            this.object = object;
            this.version = version;
        }

        DataRow build() {
            if (object.getPersistenceState() == PersistenceState.HOLLOW
                    && object.getObjectContext() != null) {
                return objectStore.getSnapshot(object.getObjectId());
            }

            ObjEntity entity = resolver.getObjEntity(object);
            ClassDescriptor descriptor = resolver.getClassDescriptor(entity.getName());

            // to-many relationships contribute nothing to the snapshot
            for (PropertyDescriptor property : descriptor.getProperties()) {
                switch (property) {
                    case AttributeProperty attribute -> addAttribute(attribute);
                    case ToOneProperty toOne -> addToOne(toOne);
                    default -> {
                    }
                }
            }

            // process object id map
            // we should ignore any object id values if a corresponding attribute
            // is a part of relationship "toMasterPK", since those values have been
            // set above when db relationships where processed.
            Map<String, Object> thisIdParts = object.getObjectId().getIdSnapshot();
            if (thisIdParts != null) {

                // put only those that do not exist in the map
                for (Map.Entry<String, Object> entry : thisIdParts.entrySet()) {
                    String nextKey = entry.getKey();
                    if (!values.containsKey(nextKey)) {
                        values.put(nextKey, entry.getValue());
                    }
                }
            }

            // Ensure primary keys of additional entities are also included in the snapshot
            for (CayennePath path : descriptor.getAdditionalDbEntities().keySet()) {
                ObjectId flattenedId = objectStore.getFlattenedId(object.getObjectId(), path);
                if (flattenedId != null) {
                    for (Map.Entry<String, Object> idEntry : flattenedId.getIdSnapshot().entrySet()) {
                        values.putIfAbsent(path.dot(idEntry.getKey()).value(), idEntry.getValue());
                    }
                }
            }

            return DataRow.of(entity.getName(), values, version);
        }

        private void addAttribute(AttributeProperty property) {
            ObjAttribute objAttr = property.getAttribute();
            // processing compound attributes correctly
            values.put(objAttr.getDbAttributePath().value(), property.readPropertyDirectly(object));
        }

        private void addToOne(ToOneProperty property) {
            ObjRelationship rel = property.getRelationship();

            // if target doesn't propagate its key value, skip it
            if (rel.isSourceIndependentFromTargetChange()) {
                return;
            }

            Object targetObject = property.readPropertyDirectly(object);
            if (targetObject == null) {
                return;
            }

            // if target is Fault, get id attributes from stored snapshot to avoid unneeded fault triggering
            if (targetObject instanceof Fault) {
                DataRow storedSnapshot = objectStore.getSnapshot(object.getObjectId());
                if (storedSnapshot == null) {
                    throw new CayenneRuntimeException("No matching objects found for ObjectId %s"
                            + ". Object may have been deleted externally.", object.getObjectId());
                }

                DbRelationship dbRel = rel.getDbRelationships().getFirst();
                for (DbJoin join : dbRel.getJoins()) {
                    String key = join.getSourceName();
                    values.put(key, storedSnapshot.get(key));
                }

                return;
            }

            // target is resolved, and we have an FK->PK to it, so extract it from target...
            Persistent target = (Persistent) targetObject;
            Map<String, Object> idParts = target.getObjectId().getIdSnapshot();

            // this may happen in uncommitted objects - see the warning in the JavaDoc of this method.
            if (idParts.isEmpty()) {
                return;
            }

            DbRelationship dbRel = rel.getDbRelationships().getFirst();
            Map<String, Object> fk = dbRel.srcFkSnapshotWithTargetSnapshot(idParts);
            values.putAll(fk);
        }
    }
}
