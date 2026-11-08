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

package org.apache.cayenne.query.resolved;

import org.apache.cayenne.map.compiled.CompiledDataMap;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.ResultSegments;

/**
 * An immutable, fully resolved description of a statement to run, produced by {@link Query#resolve(org.apache.cayenne.map.compiled.CompiledMapping)}
 * for a user query, or by the flush for a batch. It carries everything the runtime needs to route, translate and
 * execute the statement, so that the query itself is not consulted again once resolved.
 * <p>
 * The accessors that only make sense for a user query have defaults, inherited by the {@link ResolvedBatch} records.
 *
 * @since 6.0
 */
public sealed interface ResolvedQuery permits ResolvedSelect, ResolvedSQL, ResolvedProcedure, ResolvedBatch {

    /**
     * Defines default query fetch limit, which is zero, meaning that all matching rows should be fetched.
     */
    int FETCH_LIMIT_DEFAULT = 0;

    /**
     * Defines default query fetch start index, which is 0, meaning that matching rows selected starting from the first.
     */
    int FETCH_OFFSET_DEFAULT = 0;

    int QUERY_TIMEOUT_DEFAULT = -1;

    /**
     * Defines default query page size, which is zero for no pagination.
     */
    int PAGE_SIZE_DEFAULT = 0;

    boolean FETCHING_DATA_ROWS_DEFAULT = false;

    /**
     * Defines default statement fetch size, which is 0, meaning that the JDBC driver default is used.
     */
    int STATEMENT_FETCH_SIZE_DEFAULT = 0;

    /**
     * Returns the query this statement was resolved from. For a prefetch select this is the root user query, for a
     * batch - null.
     */
    Query<?> source();

    /**
     * Returns the DataMap the statement is routed on, or null for a raw SQL statement without a root, that is routed
     * by its {@link #dataNodeName() DataNode name}.
     */
    CompiledDataMap dataMap();

    /**
     * Returns the name of the DataNode the query is explicitly routed to, or null when it is routed by its DataMap or
     * runs on the default node. Only a raw SQL statement without a root names its node.
     */
    default String dataNodeName() {
        return null;
    }

    /**
     * Returns a DbEntity associated with the statement or null if no such entity exists.
     */
    CompiledTable table();

    /**
     * Returns an ObjEntity associated with the statement or null if no such entity exists.
     */
    default CompiledEntity entity() {
        return null;
    }

    default QueryCacheStrategy cacheStrategy() {
        return QueryCacheStrategy.getDefaultStrategy();
    }

    /**
     * Returns a String that uniquely identifies this query for the purposes of result caching. If null is returned, no
     * caching is performed.
     *
     * @param valueObjectTypes the registry rendering the values of value object types found in the query into the
     *                         key, or null to render them with their own String form
     */
    default String cacheKey(ValueObjectTypeRegistry valueObjectTypes) {
        return null;
    }

    /**
     * Returns an optional cache "group". Cache groups allow to invalidate query caches in bulk on different events.
     */
    default String cacheGroup() {
        return null;
    }

    /**
     * Returns <code>true</code> if this query should produce a list of data rows as opposed to Persistent objects.
     */
    default boolean fetchingDataRows() {
        return FETCHING_DATA_ROWS_DEFAULT;
    }

    /**
     * Returns true if this is a read-only (selecting) query that does not need to run in its own transaction. Defaults
     * to false, so any query not explicitly marked read-only is still wrapped in a transaction.
     */
    default boolean readOnly() {
        return false;
    }

    /**
     * Returns query page size. Page size is a hint to Cayenne that query should be performed page by page, instead of
     * retrieving all results at once. If the value returned is less than or equal to zero, no paging should occur.
     */
    default int pageSize() {
        return PAGE_SIZE_DEFAULT;
    }

    default int fetchOffset() {
        return FETCH_OFFSET_DEFAULT;
    }

    /**
     * Returns the limit on the maximum number of records that can be returned by this query. If a fetch limit is zero,
     * all rows will be returned.
     */
    default int fetchLimit() {
        return FETCH_LIMIT_DEFAULT;
    }

    default int statementFetchSize() {
        return STATEMENT_FETCH_SIZE_DEFAULT;
    }

    default int queryTimeout() {
        return QUERY_TIMEOUT_DEFAULT;
    }

    /**
     * Returns a root node of prefetch tree used by this query, or null if no prefetches are configured. The tree is
     * private to the resolved query, so the runtime is free to annotate it.
     */
    default PrefetchTreeNode prefetchTree() {
        return null;
    }

    /**
     * Returns query result set mapping. May be null if the query returns whole objects or data rows.
     */
    default ResultSegments resultSegments() {
        return null;
    }
}
