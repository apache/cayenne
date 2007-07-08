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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.UpdateBatchQuery;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataDomainUpdateBucket extends DataDomainSyncBucket {

    DataDomainUpdateBucket(DataDomainFlushAction parent) {
        super(parent);
    }

    void appendQueriesInternal(Collection queries) {

        DataDomainDBDiffBuilder diffBuilder = new DataDomainDBDiffBuilder();
        DataNodeSyncQualifierDescriptor qualifierBuilder = new DataNodeSyncQualifierDescriptor();

        for (Iterator i = dbEntities.iterator(); i.hasNext();) {
            DbEntity dbEntity = (DbEntity) i.next();
            List objEntitiesForDbEntity = (List) descriptorsByDbEntity.get(dbEntity);
            Map batches = new LinkedHashMap();

            for (Iterator j = objEntitiesForDbEntity.iterator(); j.hasNext();) {
                ClassDescriptor descriptor = (ClassDescriptor) j.next();
                ObjEntity entity = descriptor.getEntity();

                diffBuilder.reset(entity, dbEntity);
                qualifierBuilder.reset(entity, dbEntity);
                boolean isRootDbEntity = entity.getDbEntity() == dbEntity;

                List objects = (List) objectsByDescriptor.get(descriptor);

                for (Iterator k = objects.iterator(); k.hasNext();) {
                    Persistent o = (Persistent) k.next();
                    ObjectDiff diff = parent.objectDiff(o.getObjectId());

                    Map snapshot = diffBuilder.buildDBDiff(diff);

                    // check whether MODIFIED object has real db-level modifications
                    if (snapshot == null) {

                        if (isRootDbEntity) {
                            k.remove();
                            o.setPersistenceState(PersistenceState.COMMITTED);
                        }

                        continue;
                    }

                    // after we filtered out "fake" modifications, check if an
                    // attempt is made to modify a read only entity
                    checkReadOnly(entity);

                    Map qualifierSnapshot = qualifierBuilder
                            .createQualifierSnapshot(diff);

                    // organize batches by the updated columns + nulls in qualifier
                    Set snapshotSet = snapshot.keySet();
                    Set nullQualifierNames = new HashSet();
                    Iterator it = qualifierSnapshot.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry entry = (Map.Entry) it.next();
                        if (entry.getValue() == null) {
                            nullQualifierNames.add(entry.getKey());
                        }
                    }

                    List batchKey = Arrays.asList(new Object[] {
                            snapshotSet, nullQualifierNames
                    });

                    UpdateBatchQuery batch = (UpdateBatchQuery) batches.get(batchKey);
                    if (batch == null) {
                        batch = new UpdateBatchQuery(
                                dbEntity,
                                qualifierBuilder.getAttributes(),
                                updatedAttributes(dbEntity, snapshot),
                                nullQualifierNames,
                                10);

                        batch.setUsingOptimisticLocking(qualifierBuilder
                                .isUsingOptimisticLocking());
                        batches.put(batchKey, batch);
                    }

                    batch.add(qualifierSnapshot, snapshot, o.getObjectId());

                    // update replacement id with meaningful PK changes
                    if (isRootDbEntity) {
                        Map replacementId = o.getObjectId().getReplacementIdMap();

                        Iterator pkIt = dbEntity.getPrimaryKey().iterator();
                        while (pkIt.hasNext()) {
                            String name = ((DbAttribute) pkIt.next()).getName();
                            if (snapshot.containsKey(name)
                                    && !replacementId.containsKey(name)) {
                                replacementId.put(name, snapshot.get(name));
                            }
                        }
                    }
                }
            }

            queries.addAll(batches.values());
        }
    }

    /**
     * Creates a list of DbAttributes that are updated in a snapshot
     */
    private List updatedAttributes(DbEntity entity, Map updatedSnapshot) {
        List attributes = new ArrayList(updatedSnapshot.size());
        Map entityAttributes = entity.getAttributeMap();

        Iterator it = updatedSnapshot.keySet().iterator();
        while (it.hasNext()) {
            Object name = it.next();
            attributes.add(entityAttributes.get(name));
        }

        return attributes;
    }
}
