/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.NodeIdChangeOperation;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyException;
import org.apache.cayenne.reflect.ToManyMapProperty;
import org.apache.commons.collections.Factory;

/**
 * A superclass of batch query wrappers.
 * 
 * @since 1.2
 */
abstract class DataDomainSyncBucket {

    final Map<ClassDescriptor, List<Persistent>> objectsByDescriptor;
    final DataDomainFlushAction parent;

    List<DbEntity> dbEntities;
    Map<DbEntity, Collection<DbEntityClassDescriptor>> descriptorsByDbEntity;

    DataDomainSyncBucket(DataDomainFlushAction parent) {
        this.objectsByDescriptor = new HashMap<ClassDescriptor, List<Persistent>>();
        this.parent = parent;
    }

    boolean isEmpty() {
        return objectsByDescriptor.isEmpty();
    }

    abstract void appendQueriesInternal(Collection<Query> queries);

    /**
     * Appends all queries originated in the bucket to provided collection.
     */
    void appendQueries(Collection<Query> queries) {

        if (!objectsByDescriptor.isEmpty()) {
            groupObjEntitiesBySpannedDbEntities();
            appendQueriesInternal(queries);
        }
    }

    void checkReadOnly(ObjEntity entity) throws CayenneRuntimeException {

        if (entity == null) {
            throw new NullPointerException("Entity must not be null.");
        }

        if (entity.isReadOnly()) {

            StringBuilder message = new StringBuilder();
            message
                    .append("Attempt to modify object(s) mapped to a read-only entity: ")
                    .append(entity.getName());

            message.append(" '").append(entity.getName()).append("'");

            message.append(". Can't commit changes.");
            throw new CayenneRuntimeException(message.toString());
        }
    }

    private void groupObjEntitiesBySpannedDbEntities() {

        dbEntities = new ArrayList<DbEntity>(objectsByDescriptor.size());
        descriptorsByDbEntity = new HashMap<DbEntity, Collection<DbEntityClassDescriptor>>(
                objectsByDescriptor.size() * 2);

        for (ClassDescriptor descriptor : objectsByDescriptor.keySet()) {

            // root DbEntity
            {
                DbEntityClassDescriptor dbEntityDescriptor = new DbEntityClassDescriptor(
                        descriptor);
                DbEntity dbEntity = dbEntityDescriptor.getDbEntity();

                Collection<DbEntityClassDescriptor> descriptors = descriptorsByDbEntity
                        .get(dbEntity);
                if (descriptors == null) {
                    descriptors = new ArrayList<DbEntityClassDescriptor>(1);
                    dbEntities.add(dbEntity);
                    descriptorsByDbEntity.put(dbEntity, descriptors);
                }

                if (!containsClassDescriptor(descriptors, descriptor)) {
                    descriptors.add(dbEntityDescriptor);
                }
            }

            // secondary DbEntities...

            // Note that this logic won't allow flattened attributes to span multiple
            // databases...
            for (ObjAttribute objAttribute : descriptor.getEntity().getAttributes()) {

                if (objAttribute.isFlattened()) {
                    DbEntityClassDescriptor dbEntityDescriptor = new DbEntityClassDescriptor(
                            descriptor,
                            objAttribute);

                    DbEntity dbEntity = dbEntityDescriptor.getDbEntity();
                    Collection<DbEntityClassDescriptor> descriptors = descriptorsByDbEntity
                            .get(dbEntity);

                    if (descriptors == null) {
                        descriptors = new ArrayList<DbEntityClassDescriptor>(1);
                        dbEntities.add(dbEntity);
                        descriptorsByDbEntity.put(dbEntity, descriptors);
                    }

                    if (!containsClassDescriptor(descriptors, descriptor)) {
                        descriptors.add(dbEntityDescriptor);
                    }
                }
            }
        }
    }

    private boolean containsClassDescriptor(
            Collection<DbEntityClassDescriptor> descriptors,
            ClassDescriptor classDescriptor) {
        for (DbEntityClassDescriptor descriptor : descriptors) {
            if (classDescriptor.equals(descriptor.getClassDescriptor())) {
                return true;
            }
        }
        return false;
    }

    void addDirtyObject(Persistent object, ClassDescriptor descriptor) {

        List<Persistent> objects = objectsByDescriptor.get(descriptor);
        if (objects == null) {

            objects = new ArrayList<Persistent>();
            objectsByDescriptor.put(descriptor, objects);
        }

        objects.add(object);
    }

    void postprocess() {

        if (!objectsByDescriptor.isEmpty()) {

            CompoundDiff result = parent.getResultDiff();
            Map<ObjectId, DataRow> modifiedSnapshots = parent
                    .getResultModifiedSnapshots();
            Collection<ObjectId> deletedIds = parent.getResultDeletedIds();

            for (Map.Entry<ClassDescriptor, List<Persistent>> entry : objectsByDescriptor
                    .entrySet()) {

                ClassDescriptor descriptor = entry.getKey();

                for (Persistent object : entry.getValue()) {
                    ObjectId id = object.getObjectId();

                    ObjectId finalId;

                    // record id change and update attributes for generated ids
                    if (id.isReplacementIdAttached()) {

                        Map<String, Object> replacement = id.getReplacementIdMap();
                        for (AttributeProperty property : descriptor.getIdProperties()) {

                            Object value = replacement.get(property
                                    .getAttribute()
                                    .getDbAttributeName());

                            // TODO: andrus, 11/28/2006: this operation may be redundant
                            // if the id wasn't generated. We may need to optimize it...
                            if (value != null) {
                                property.writePropertyDirectly(object, null, value);
                            }
                        }

                        ObjectId replacementId = id.createReplacementId();

                        result.add(new NodeIdChangeOperation(id, replacementId));

                        // classify replaced permanent ids as "deleted", as
                        // DataRowCache has no notion of replaced id...
                        if (!id.isTemporary()) {
                            deletedIds.add(id);
                        }

                        finalId = replacementId;
                    }
                    else if (id.isTemporary()) {
                        throw new CayenneRuntimeException(
                                "Temporary ID hasn't been replaced on commit: " + object);
                    }
                    else {
                        finalId = id;
                    }

                    // do not take the snapshot until generated columns are processed (see
                    // code above)
                    DataRow dataRow = parent.getContext().currentSnapshot(object);

                    if (object instanceof DataObject) {
                        DataObject dataObject = (DataObject) object;
                        dataRow.setReplacesVersion(dataObject.getSnapshotVersion());
                        dataObject.setSnapshotVersion(dataRow.getVersion());
                    }

                    modifiedSnapshots.put(finalId, dataRow);

                    // update Map reverse relationships
                    for (ArcProperty arc : descriptor.getMapArcProperties()) {
                        ToManyMapProperty reverseArc = (ToManyMapProperty) arc
                                .getComplimentaryReverseArc();

                        // must resolve faults... hopefully for to-one this will not cause
                        // extra fetches...
                        Object source = arc.readProperty(object);
                        if (source != null && !reverseArc.isFault(source)) {
                            remapTarget(reverseArc, source, object);
                        }
                    }
                }
            }
        }
    }

    private final void remapTarget(
            ToManyMapProperty property,
            Object source,
            Object target) throws PropertyException {

        Map<Object, Object> map = (Map<Object, Object>) property.readProperty(source);
        Object newKey = property.getMapKey(target);
        Object currentValue = map.get(newKey);

        if (currentValue == target) {
            // nothing to do
            return;
        }
        // else - do not check for conflicts here (i.e. another object mapped for the same
        // key), as we have no control of the order in which this method is called, so
        // another object may be remapped later by the caller

        // must do a slow map scan to ensure the object is not mapped under a different
        // key...
        Iterator<?> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Object, Object> e = (Map.Entry<Object, Object>) it.next();
            if (e.getValue() == target) {
                it.remove();
                break;
            }
        }

        map.put(newKey, target);
    }

    // a factory for extracting PKs generated on commit.
    final static class PropagatedValueFactory implements Factory {

        ObjectId masterID;
        String masterKey;

        PropagatedValueFactory(ObjectId masterID, String masterKey) {
            this.masterID = masterID;
            this.masterKey = masterKey;
        }

        public Object create() {
            Object value = masterID.getIdSnapshot().get(masterKey);
            if (value == null) {
                throw new CayenneRuntimeException("Can't extract a master key. "
                        + "Missing key ("
                        + masterKey
                        + "), master ID ("
                        + masterID
                        + ")");
            }

            return value;
        }
    }
}
