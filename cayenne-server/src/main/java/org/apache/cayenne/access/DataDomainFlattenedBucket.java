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

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.BatchQueryRow;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A sync bucket that holds flattened queries.
 * 
 * @since 1.2
 * @deprecated since 4.2 as part of deprecated {@link LegacyDataDomainFlushAction}
 */
@Deprecated
class DataDomainFlattenedBucket {

    final LegacyDataDomainFlushAction parent;
    final Map<DbEntity, List<FlattenedArcKey>> insertArcKeys;
    final Map<DbEntity, DeleteBatchQuery> flattenedDeleteQueries;

    DataDomainFlattenedBucket(LegacyDataDomainFlushAction parent) {
        this.parent = parent;
        this.insertArcKeys = new HashMap<>();
        this.flattenedDeleteQueries = new HashMap<>();
    }

    boolean isEmpty() {
        return insertArcKeys.isEmpty() && flattenedDeleteQueries.isEmpty();
    }

    void addInsertArcKey(DbEntity flattenedEntity, FlattenedArcKey flattenedArcKey) {
        List<FlattenedArcKey> arcKeys = insertArcKeys.get(flattenedEntity);

        if (arcKeys == null) {
            arcKeys = new ArrayList<>();
            insertArcKeys.put(flattenedEntity, arcKeys);
        }

        arcKeys.add(flattenedArcKey);
    }

    void addFlattenedDelete(DbEntity flattenedEntity, FlattenedArcKey flattenedDeleteInfo) {

        DeleteBatchQuery relationDeleteQuery = flattenedDeleteQueries.get(flattenedEntity);
        if (relationDeleteQuery == null) {
            Collection<DbAttribute> pk = flattenedEntity.getPrimaryKeys();
            List<DbAttribute> pkList = pk instanceof List ? (List<DbAttribute>) pk : new ArrayList<>(pk);
            relationDeleteQuery = new DeleteBatchQuery(flattenedEntity, pkList, Collections.<String> emptySet(), 50);
            relationDeleteQuery.setUsingOptimisticLocking(false);
            flattenedDeleteQueries.put(flattenedEntity, relationDeleteQuery);
        }

        DataNode node = parent.getDomain().lookupDataNode(flattenedEntity.getDataMap());
        List<Map<String, Object>> flattenedSnapshots = flattenedDeleteInfo.buildJoinSnapshotsForDelete(node);
        if (!flattenedSnapshots.isEmpty()) {
            for (Map<String, Object> flattenedSnapshot : flattenedSnapshots) {
                relationDeleteQuery.add(flattenedSnapshot);
            }
        }
    }

    /**
     * responsible for adding the flattened Insert Queries. Its possible an insert query for the same DbEntity/ObjectId
     * already has been added from the insert bucket queries if that Object also has an attribute. So we want to merge
     * the data for each insert into a single insert.
     */
    void appendInserts(Collection<Query> queries) {

        // TODO: see "O(N) lookups" TODO's below. The first is relatively benign, as N is the number of DbEntities in the
        // preceeding DataDomainInsertBucket processing. The second nested one is potentially much worse, as it may
        // result in a linear scan of thousands of objects. E.g. it will affect cases with vertical inheritance with
        // relationships in subclasses...

        // The fix to the above is to merge this code into DataDomainInsertBucket, so that we can combine regular and
        // flattened snapshots at the point of InsertBatchQuery creation.

        for (Map.Entry<DbEntity, List<FlattenedArcKey>> entry : insertArcKeys.entrySet()) {
            DbEntity dbEntity = entry.getKey();
            List<FlattenedArcKey> flattenedArcKeys = entry.getValue();

            DataNode node = parent.getDomain().lookupDataNode(dbEntity.getDataMap());

            // TODO: O(N) lookup
            InsertBatchQuery existingQuery = findInsertBatchQuery(queries, dbEntity);
            InsertBatchQuery newQuery = new InsertBatchQuery(dbEntity, 50);

            // merge the snapshots of the FAKs by ObjectId for all ToOne relationships in case we have multiple Arcs per Object
            Map<ObjectId, Map<String, Object>> toOneSnapshots = new HashMap<>();

            // gather the list of the ToMany snapshots (these will actually be their own insert rows)
            List<Map<String, Object>> toManySnapshots = new ArrayList<>();

            for (FlattenedArcKey flattenedArcKey : flattenedArcKeys) {
                Map<String, Object> joinSnapshot = flattenedArcKey.buildJoinSnapshotForInsert(node);

                if (flattenedArcKey.relationship.isToMany()) {
                    toManySnapshots.add(joinSnapshot);
                } else {
                    ObjectId objectId = flattenedArcKey.id1.getSourceId();

                    Map<String, Object> snapshot = toOneSnapshots.get(objectId);

                    if (snapshot == null) {
                        toOneSnapshots.put(objectId, joinSnapshot);
                    } else {
                        // merge joinSnapshot data with existing snapshot
                        for (Map.Entry<String, Object> dbValue : joinSnapshot.entrySet()) {
                            snapshot.put(dbValue.getKey(), dbValue.getValue());
                        }
                    }
                }
            }

            // apply the merged ToOne snapshots information and possibly merge it with an existing BatchQueryRow
            for (Map.Entry<ObjectId, Map<String, Object>> flattenedSnapshot : toOneSnapshots.entrySet()) {
                ObjectId objectId = flattenedSnapshot.getKey();
                Map<String, Object> snapshot = flattenedSnapshot.getValue();

                if (existingQuery != null) {

                    // TODO: O(N) lookup
                    BatchQueryRow existingRow = findRowForObjectId(existingQuery.getRows(), objectId);
                    // todo: do we need to worry about flattenedArcKey.id2 ?

                    if (existingRow != null) {
                        List<DbAttribute> existingQueryDbAttributes = existingQuery.getDbAttributes();

                        for(int i=0; i < existingQueryDbAttributes.size(); i++) {
                            Object value = existingRow.getValue(i);
                            if (value != null) {
                                snapshot.put(existingQueryDbAttributes.get(i).getName(), value);
                            }
                        }
                    }
                }

                newQuery.add(snapshot, objectId);
            }

            // apply the ToMany snapshots as new BatchQueryRows
            for(Map<String, Object> toManySnapshot : toManySnapshots) {
                newQuery.add(toManySnapshot);
            }

            if (existingQuery != null) {
                queries.remove(existingQuery);
            }

            queries.add(newQuery);
        }
    }

    void appendDeletes(Collection<Query> queries) {
        if (!flattenedDeleteQueries.isEmpty()) {
            queries.addAll(flattenedDeleteQueries.values());
        }
    }

    private InsertBatchQuery findInsertBatchQuery(Collection<Query> queries, DbEntity dbEntity) {
        for(Query query : queries) {
            if (query instanceof InsertBatchQuery) {
                InsertBatchQuery insertBatchQuery = (InsertBatchQuery)query;
                if (insertBatchQuery.getDbEntity().equals(dbEntity)) {
                    return insertBatchQuery;
                }
            }
        }
        return null;
    }

    private BatchQueryRow findRowForObjectId(List<BatchQueryRow> rows, ObjectId objectId) {
        for (BatchQueryRow row : rows) {
            if (row.getObjectId().equals(objectId)) {
                return row;
            }
        }
        return null;
    }
}
