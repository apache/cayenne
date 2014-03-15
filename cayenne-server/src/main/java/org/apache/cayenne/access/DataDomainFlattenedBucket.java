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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.Query;

/**
 * A sync bucket that holds flattened queries.
 * 
 * @since 1.2
 */
class DataDomainFlattenedBucket {

    final DataDomainFlushAction parent;
    final Map<DbEntity, InsertBatchQuery> flattenedInsertQueries;
    final Map<DbEntity, DeleteBatchQuery> flattenedDeleteQueries;

    DataDomainFlattenedBucket(DataDomainFlushAction parent) {
        this.parent = parent;
        this.flattenedInsertQueries = new HashMap<DbEntity, InsertBatchQuery>();
        this.flattenedDeleteQueries = new HashMap<DbEntity, DeleteBatchQuery>();
    }

    boolean isEmpty() {
        return flattenedInsertQueries.isEmpty() && flattenedDeleteQueries.isEmpty();
    }

    void addFlattenedInsert(DbEntity flattenedEntity, FlattenedArcKey flattenedArcKey) {

        InsertBatchQuery relationInsertQuery = flattenedInsertQueries.get(flattenedEntity);

        if (relationInsertQuery == null) {
            relationInsertQuery = new InsertBatchQuery(flattenedEntity, 50);
            flattenedInsertQueries.put(flattenedEntity, relationInsertQuery);
        }

        DataNode node = parent.getDomain().lookupDataNode(flattenedEntity.getDataMap());
        Map flattenedSnapshot = flattenedArcKey.buildJoinSnapshotForInsert(node);
        relationInsertQuery.add(flattenedSnapshot);
    }

    void addFlattenedDelete(DbEntity flattenedEntity, FlattenedArcKey flattenedDeleteInfo) {

        DeleteBatchQuery relationDeleteQuery = flattenedDeleteQueries.get(flattenedEntity);
        if (relationDeleteQuery == null) {
            boolean optimisticLocking = false;
            Collection<DbAttribute> pk = flattenedEntity.getPrimaryKeys();
            List<DbAttribute> pkList = pk instanceof List ? (List<DbAttribute>) pk : new ArrayList<DbAttribute>(pk);
            relationDeleteQuery = new DeleteBatchQuery(flattenedEntity, pkList, Collections.<String> emptySet(), 50);
            relationDeleteQuery.setUsingOptimisticLocking(optimisticLocking);
            flattenedDeleteQueries.put(flattenedEntity, relationDeleteQuery);
        }

        DataNode node = parent.getDomain().lookupDataNode(flattenedEntity.getDataMap());
        List flattenedSnapshots = flattenedDeleteInfo.buildJoinSnapshotsForDelete(node);
        if (!flattenedSnapshots.isEmpty()) {
            Iterator snapsIt = flattenedSnapshots.iterator();
            while (snapsIt.hasNext()) {
                relationDeleteQuery.add((Map) snapsIt.next());
            }
        }
    }

    void appendInserts(Collection<Query> queries) {
        if (!flattenedInsertQueries.isEmpty()) {
            queries.addAll(flattenedInsertQueries.values());
        }
    }

    void appendDeletes(Collection<Query> queries) {
        if (!flattenedDeleteQueries.isEmpty()) {
            queries.addAll(flattenedDeleteQueries.values());
        }
    }
}
