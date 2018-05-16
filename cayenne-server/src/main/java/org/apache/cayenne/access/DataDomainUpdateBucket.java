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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.UpdateBatchQuery;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 1.2
 */
class DataDomainUpdateBucket extends DataDomainSyncBucket {

    DataDomainUpdateBucket(DataDomainFlushAction parent) {
        super(parent);
    }

    @Override
    void addSpannedDbEntities(ClassDescriptor descriptor) {
        super.addSpannedDbEntities(descriptor);
        // for update we need to add entities for flattened toOne relationships
        for(ObjRelationship objRelationship : descriptor.getEntity().getRelationships()) {
            if(objRelationship.isFlattened() && !objRelationship.isToMany()) {
                addDescriptor(descriptor, new DbEntityClassDescriptor(descriptor, objRelationship));
            }
        }
    }

    @Override
    void appendQueriesInternal(Collection<Query> queries) {

        DataDomainDBDiffBuilder diffBuilder = new DataDomainDBDiffBuilder();
        DataNodeSyncQualifierDescriptor qualifierBuilder = new DataNodeSyncQualifierDescriptor();

        for (DbEntity dbEntity : dbEntities) {

            Collection<DbEntityClassDescriptor> descriptors = descriptorsByDbEntity.get(dbEntity);
            Map<Object, Query> batches = new LinkedHashMap<>();

            for (DbEntityClassDescriptor descriptor : descriptors) {
                ObjEntity entity = descriptor.getEntity();

                diffBuilder.reset(descriptor);
                qualifierBuilder.reset(descriptor);
                boolean isRootDbEntity = entity.getDbEntity() == dbEntity;

                for (Persistent o : objectsByDescriptor.get(descriptor.getClassDescriptor())) {
                    ObjectDiff diff = parent.objectDiff(o.getObjectId());

                    Map<String, Object> snapshot = diffBuilder.buildDBDiff(diff);

                    // check whether MODIFIED object has real db-level modifications
                    if (snapshot == null) {
                        continue;
                    }

                    // after we filtered out "fake" modifications, check if an
                    // attempt is made to modify a read only entity
                    checkReadOnly(entity);

                    Map<String, Object> qualifierSnapshot = qualifierBuilder.createQualifierSnapshot(diff);

                    // organize batches by the updated columns + nulls in qualifier
                    Set<String> snapshotSet = snapshot.keySet();
                    Set<String> nullQualifierNames = new HashSet<>();
                    for (Map.Entry<String, Object> entry : qualifierSnapshot.entrySet()) {
                        if (entry.getValue() == null) {
                            nullQualifierNames.add(entry.getKey());
                        }
                    }

                    List<Set<String>> batchKey = Arrays.asList(snapshotSet, nullQualifierNames);

                    UpdateBatchQuery batch = (UpdateBatchQuery) batches.get(batchKey);
                    if (batch == null) {
                        batch = new UpdateBatchQuery(
                                dbEntity,
                                qualifierBuilder.getAttributes(),
                                updatedAttributes(dbEntity, snapshot),
                                nullQualifierNames,
                                10);

                        batch.setUsingOptimisticLocking(qualifierBuilder.isUsingOptimisticLocking());
                        batches.put(batchKey, batch);
                    }

                    batch.add(qualifierSnapshot, snapshot, o.getObjectId());

                    // update replacement id with meaningful PK changes
                    if (isRootDbEntity) {
                        Map<String, Object> replacementId = o
                                .getObjectId()
                                .getReplacementIdMap();

                        for (DbAttribute pk : dbEntity.getPrimaryKeys()) {
                            String name = pk.getName();
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
    private List<DbAttribute> updatedAttributes(DbEntity entity, Map<String, Object> updatedSnapshot) {
        List<DbAttribute> attributes = new ArrayList<>(updatedSnapshot.size());
        Map<String, ? extends Attribute> entityAttributes = entity.getAttributeMap();

        for (String name : updatedSnapshot.keySet()) {
            attributes.add((DbAttribute)entityAttributes.get(name));
        }

        return attributes;
    }
}
