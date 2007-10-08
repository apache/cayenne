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

import org.apache.commons.collections.Factory;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.NodeIdChangeOperation;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;

/**
 * A superclass of batch query wrappers.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
abstract class DataDomainSyncBucket {

    final Map objectsByEntity;
    final DataDomainFlushAction parent;

    List dbEntities;
    Map objEntitiesByDbEntity;

    DataDomainSyncBucket(DataDomainFlushAction parent) {
        this.objectsByEntity = new HashMap();
        this.parent = parent;
    }

    boolean isEmpty() {
        return objectsByEntity.isEmpty();
    }

    abstract void appendQueriesInternal(Collection queries);

    /**
     * Appends all queries originated in the bucket to provided collection.
     */
    void appendQueries(Collection queries) {

        if (!objectsByEntity.isEmpty()) {
            groupObjEntitiesBySpannedDbEntities();
            appendQueriesInternal(queries);
        }
    }

    void checkReadOnly(ObjEntity entity) throws CayenneRuntimeException {

        if (entity.isReadOnly()) {

            StringBuffer message = new StringBuffer();
            message
                    .append("Attempt to modify object(s) mapped to a read-only entity: ")
                    .append(entity.getName());

            if (entity != null) {
                message.append(" '").append(entity.getName()).append("'");
            }

            message.append(". Can't commit changes.");
            throw new CayenneRuntimeException(message.toString());
        }
    }

    private void groupObjEntitiesBySpannedDbEntities() {

        dbEntities = new ArrayList(objectsByEntity.size());
        objEntitiesByDbEntity = new HashMap(objectsByEntity.size() * 2);

        Iterator i = objectsByEntity.keySet().iterator();
        while (i.hasNext()) {
            ObjEntity objEntity = (ObjEntity) i.next();
            DbEntity dbEntity = objEntity.getDbEntity();

            List objEntitiesForDbEntity = (List) objEntitiesByDbEntity.get(dbEntity);
            if (objEntitiesForDbEntity == null) {
                objEntitiesForDbEntity = new ArrayList(1);
                dbEntities.add(dbEntity);
                objEntitiesByDbEntity.put(dbEntity, objEntitiesForDbEntity);
            }

            if (!objEntitiesForDbEntity.contains(objEntity)) {
                objEntitiesForDbEntity.add(objEntity);
            }

            // Note that this logic won't allow flattened attributes to span multiple
            // databases...
            Iterator j = objEntity.getAttributeMap().values().iterator();
            while (j.hasNext()) {
                ObjAttribute objAttribute = (ObjAttribute) j.next();
                if (!objAttribute.isCompound())
                    continue;
                dbEntity = (DbEntity) objAttribute.getDbAttribute().getEntity();
                objEntitiesForDbEntity = (List) objEntitiesByDbEntity.get(dbEntity);

                if (objEntitiesForDbEntity == null) {
                    objEntitiesForDbEntity = new ArrayList(1);
                    dbEntities.add(dbEntity);
                    objEntitiesByDbEntity.put(dbEntity, objEntitiesForDbEntity);
                }

                if (!objEntitiesForDbEntity.contains(objEntity)) {
                    objEntitiesForDbEntity.add(objEntity);
                }
            }
        }
    }

    void addDirtyObject(Object object, ObjEntity entity) {

        Collection objects = (Collection) objectsByEntity.get(entity);
        if (objects == null) {

            objects = new ArrayList();
            objectsByEntity.put(entity, objects);
        }

        objects.add(object);
    }

    void postprocess() {

        if (!objectsByEntity.isEmpty()) {

            CompoundDiff result = parent.getResultDiff();
            Map modifiedSnapshots = parent.getResultModifiedSnapshots();
            Collection deletedIds = parent.getResultDeletedIds();

            Iterator it = objectsByEntity.values().iterator();
            while (it.hasNext()) {

                Iterator objects = ((Collection) it.next()).iterator();
                while (objects.hasNext()) {
                    DataObject object = (DataObject) objects.next();
                    ObjectId id = object.getObjectId();

                    DataRow dataRow = object.getDataContext().currentSnapshot(object);
                    dataRow.setReplacesVersion(object.getSnapshotVersion());
                    object.setSnapshotVersion(dataRow.getVersion());

                    // record id change
                    if (id.isReplacementIdAttached()) {
                        ObjectId replacementId = id.createReplacementId();
                        result.add(new NodeIdChangeOperation(id, replacementId));

                        // classify replaced permanent ids as "deleted", as
                        // DataRowCache has no notion of replaced id...
                        if (!id.isTemporary()) {
                            deletedIds.add(id);
                        }

                        modifiedSnapshots.put(replacementId, dataRow);
                    }
                    else if (id.isTemporary()) {
                        throw new CayenneRuntimeException(
                                "Temporary ID hasn't been replaced on commit: " + object);
                    }
                    else {
                        modifiedSnapshots.put(id, dataRow);
                    }
                }
            }
        }
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
