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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.DataObject;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.DeleteBatchQuery;

/**
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataDomainDeleteBucket extends DataDomainSyncBucket {

    DataDomainDeleteBucket(DataDomainFlushAction parent) {
        super(parent);
    }

    void postprocess() {

        if (!objectsByEntity.isEmpty()) {

            Collection deletedIds = parent.getResultDeletedIds();

            Iterator it = objectsByEntity.values().iterator();
            while (it.hasNext()) {
                Iterator objects = ((Collection) it.next()).iterator();
                while (objects.hasNext()) {
                    Persistent object = (Persistent) objects.next();
                    deletedIds.add(object.getObjectId());
                }
            }
        }
    }

    void appendQueriesInternal(Collection queries) {

        DataNodeSyncQualifierDescriptor qualifierBuilder = new DataNodeSyncQualifierDescriptor();

        EntitySorter sorter = parent.getDomain().getEntitySorter();
        sorter.sortDbEntities(dbEntities, true);

        for (Iterator i = dbEntities.iterator(); i.hasNext();) {
            DbEntity dbEntity = (DbEntity) i.next();
            List objEntitiesForDbEntity = (List) objEntitiesByDbEntity.get(dbEntity);
            Map batches = new LinkedHashMap();

            for (Iterator j = objEntitiesForDbEntity.iterator(); j.hasNext();) {
                ObjEntity entity = (ObjEntity) j.next();

                qualifierBuilder.reset(entity, dbEntity);

                boolean isRootDbEntity = (entity.getDbEntity() == dbEntity);

                // remove object set for dependent entity, so that it does not show up
                // on post processing
                List objects = (List) objectsByEntity.get(entity);

                if (objects.isEmpty()) {
                    continue;
                }

                checkReadOnly(entity);

                if (isRootDbEntity) {
                    sorter.sortObjectsForEntity(entity, objects, true);
                }

                for (Iterator k = objects.iterator(); k.hasNext();) {
                    DataObject o = (DataObject) k.next();
                    ObjectDiff diff = parent.objectDiff(o.getObjectId());

                    Map qualifierSnapshot = qualifierBuilder
                            .createQualifierSnapshot(diff);

                    // organize batches by the nulls in qualifier
                    Set nullQualifierNames = new HashSet();
                    Iterator it = qualifierSnapshot.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry entry = (Map.Entry) it.next();
                        if (entry.getValue() == null) {
                            nullQualifierNames.add(entry.getKey());
                        }
                    }

                    List batchKey = Arrays.asList(new Object[] {
                        nullQualifierNames
                    });

                    DeleteBatchQuery batch = (DeleteBatchQuery) batches.get(batchKey);
                    if (batch == null) {
                        batch = new DeleteBatchQuery(dbEntity, qualifierBuilder
                                .getAttributes(), nullQualifierNames, 27);
                        batch.setUsingOptimisticLocking(qualifierBuilder
                                .isUsingOptimisticLocking());
                        batches.put(batchKey, batch);
                    }

                    batch.add(qualifierSnapshot);
                }
            }

            queries.addAll(batches.values());
        }
    }
}
