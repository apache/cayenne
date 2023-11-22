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
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.QueryRouter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DataDomain query action that relies on externally provided OperationObserver to process
 * the results.
 * 
 * @since 1.2
 */
class DataDomainLegacyQueryAction implements QueryRouter, OperationObserver {

    static final boolean DONE = true;

    DataDomain domain;
    OperationObserver callback;
    Query query;
    QueryMetadata metadata;

    Map<QueryEngine, List<Query>> queriesByNode;
    Map<Query, Query> queriesByExecutedQueries;

    DataDomainLegacyQueryAction(DataDomain domain, Query query, OperationObserver callback) {
        this.domain = domain;
        this.query = query;
        this.metadata = query.getMetaData(domain.getEntityResolver());
        this.callback = callback;
    }

    /*
     * Gets response from the underlying DataNodes.
     */
    final void execute() {

        // reset
        queriesByNode = null;
        queriesByExecutedQueries = null;

        // categorize queries by node and by "executable" query...
        query.route(this, domain.getEntityResolver(), null);

        // run categorized queries
        if (queriesByNode != null) {
            for (final Map.Entry<QueryEngine, List<Query>> entry : queriesByNode
                    .entrySet()) {
                QueryEngine nextNode = entry.getKey();
                Collection<Query> nodeQueries = entry.getValue();
                nextNode.performQueries(nodeQueries, this);
            }
        }
    }

    @Override
    public void route(QueryEngine engine, Query query, Query substitutedQuery) {

        List<Query> queries = null;
        if (queriesByNode == null) {
            queriesByNode = new HashMap<>();
        }
        else {
            queries = queriesByNode.get(engine);
        }

        if (queries == null) {
            queries = new ArrayList<>(5);
            queriesByNode.put(engine, queries);
        }

        queries.add(query);

        // handle case when routing resuled in an "executable" query different from the
        // original query.
        if (substitutedQuery != null && substitutedQuery != query) {

            if (queriesByExecutedQueries == null) {
                queriesByExecutedQueries = new HashMap<>();
            }

            queriesByExecutedQueries.put(query, substitutedQuery);
        }
    }

    @Override
    public QueryEngine engineForDataMap(DataMap map) {
        if (map == null) {
            throw new NullPointerException("Null DataMap, can't determine DataNode.");
        }

        QueryEngine node = domain.lookupDataNode(map);

        if (node == null) {
            throw new CayenneRuntimeException("No DataNode exists for DataMap %s", map);
        }

        return node;
    }
    
    /**
     * @since 4.0
     */
    @Override
    public QueryEngine engineForName(String name) {

        QueryEngine node;

        if (name != null) {
            node = domain.getDataNode(name);
            if (node == null) {
                throw new CayenneRuntimeException("No DataNode exists for name %s", name);
            }
        } else {
            node = domain.getDefaultNode();
            if (node == null) {
                throw new CayenneRuntimeException("No default DataNode exists.");
            }
        }

        return node;
    }

    @Override
    public void nextCount(Query query, int resultCount) {
        callback.nextCount(queryForExecutedQuery(query), resultCount);
    }

    @Override
    public void nextBatchCount(Query query, int[] resultCount) {
        callback.nextBatchCount(queryForExecutedQuery(query), resultCount);
    }

    @Override
    public void nextRows(Query query, List<?> dataRows) {
        callback.nextRows(queryForExecutedQuery(query), dataRows);
    }

    @Override
    public void nextRows(Query q, ResultIterator it) {
        callback.nextRows(queryForExecutedQuery(q), it);
    }

    @Override
    public void nextGeneratedRows(Query query, ResultIterator<?> keys, List<ObjectId> idsToUpdate) {
        callback.nextGeneratedRows(queryForExecutedQuery(query), keys, idsToUpdate);
    }

    @Override
    public void nextQueryException(Query query, Exception ex) {
        callback.nextQueryException(queryForExecutedQuery(query), ex);
    }

    @Override
    public void nextGlobalException(Exception e) {
        callback.nextGlobalException(e);
    }

    @Override
    public boolean isIteratedResult() {
        return callback.isIteratedResult();
    }

    Query queryForExecutedQuery(Query executedQuery) {
        Query q = null;

        if (queriesByExecutedQueries != null) {
            q = queriesByExecutedQueries.get(executedQuery);
        }

        return q != null ? q : executedQuery;
    }
}
