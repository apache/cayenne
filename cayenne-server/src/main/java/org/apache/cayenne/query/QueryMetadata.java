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

package org.apache.cayenne.query;

import java.util.List;
import java.util.Map;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * Provides a common interface for accessing query metadata.
 * 
 * @since 1.2
 */
public interface QueryMetadata {

    /**
     * Defines the name of the property for the query {@link #getFetchLimit() fetch limit}
     * .
     */
    public static final String FETCH_LIMIT_PROPERTY = "cayenne.GenericSelectQuery.fetchLimit";

    /**
     * Defines default query fetch limit, which is zero, meaning that all matching rows
     * should be fetched.
     */
    public static final int FETCH_LIMIT_DEFAULT = 0;

    /**
     * Defines the name of the property for the query {@link #getFetchOffset() fetch
     * offset}.
     * 
     * @since 3.0
     */
    public static final String FETCH_OFFSET_PROPERTY = "cayenne.GenericSelectQuery.fetchOffset";

    /**
     * Defines default query fetch start index, which is 0, meaning that matching rows
     * selected starting from the first.
     * 
     * @since 3.0
     */
    public static final int FETCH_OFFSET_DEFAULT = 0;

    /**
     * Defines the name of the property for the query {@link #getPageSize() page size}.
     */
    public static final String PAGE_SIZE_PROPERTY = "cayenne.GenericSelectQuery.pageSize";

    /**
     * Defines default query page size, which is zero for no pagination.
     */
    public static final int PAGE_SIZE_DEFAULT = 0;

    public static final String FETCHING_DATA_ROWS_PROPERTY = "cayenne.GenericSelectQuery.fetchingDataRows";

    public static final boolean FETCHING_DATA_ROWS_DEFAULT = false;

    /**
     * @since 3.0
     */
    public static final String CACHE_STRATEGY_PROPERTY = "cayenne.GenericSelectQuery.cacheStrategy";

    /**
     * @since 3.0
     */
    public static final String CACHE_GROUPS_PROPERTY = "cayenne.GenericSelectQuery.cacheGroups";

    /**
     * Defines the name of the property for the query {@link #getStatementFetchSize() fetch
     * size}.
     * 
     * @since 3.0
     */
    public static final String STATEMENT_FETCH_SIZE_PROPERTY = "cayenne.GenericSelectQuery.statementFetchSize";

    /**
     * Defines default query fetch start index, which is 0, meaning that matching rows
     * selected starting from the first.
     * 
     * @since 3.0
     */
    public static final int STATEMENT_FETCH_SIZE_DEFAULT = 0;

    /**
     * @since 3.0
     */
    ClassDescriptor getClassDescriptor();

    /**
     * Returns an ObjEntity associated with a query or null if no such entity exists.
     */
    ObjEntity getObjEntity();

    /**
     * Returns a DbEntity associated with a query or null if no such entity exists.
     */
    DbEntity getDbEntity();

    /**
     * Returns a Procedure associated with a query or null if no such procedure exists.
     */
    Procedure getProcedure();

    /**
     * Returns a DataMap associated with a query or null if no such DataMap exists.
     */
    DataMap getDataMap();


    /**
     * Returns a caching strategy for this query.
     * 
     * @since 3.0
     */
    QueryCacheStrategy getCacheStrategy();

    /**
     * Returns a String that uniquely identifies this query for the purposes of result
     * caching. If null is returned, no caching is performed.
     */
    String getCacheKey();

    /**
     * Returns an optional array of cache "groups". Cache groups allow to invalidate query
     * caches in bulk on different events. Usually the first group in the array is
     * considered to be the "main" group that is used for declarative cache invalidation
     * with some cache providers.
     * 
     * @since 3.0
     */
    String[] getCacheGroups();

    /**
     * Returns <code>true</code> if this query should produce a list of data rows as
     * opposed to DataObjects, <code>false</code> for DataObjects. This is a hint to
     * QueryEngine executing this query.
     */
    boolean isFetchingDataRows();

    /**
     * Returns <code>true</code> if the query results should replace any currently cached
     * values, returns <code>false</code> otherwise. If {@link #isFetchingDataRows()}
     * returns <code>true</code>, this setting is not applicable and has no effect.
     */
    boolean isRefreshingObjects();

    /**
     * Returns query page size. Page size is a hint to Cayenne that query should be
     * performed page by page, instead of retrieving all results at once. If the value
     * returned is less than or equal to zero, no paging should occur.
     */
    int getPageSize();

    /**
     * Specifies a start of a range when fetching a subset of records.
     * 
     * @since 3.0
     */
    int getFetchOffset();


    /**
     * Returns the limit on the maximum number of records that can be returned by this
     * query. If the actual number of rows in the result exceeds the fetch limit, they
     * will be discarded. One possible use of fetch limit is using it as a safeguard
     * against large result sets that may lead to the application running out of memory,
     * etc. If a fetch limit is greater or equal to zero, all rows will be returned.
     * 
     * @return the limit on the maximum number of records that can be returned by this
     *         query
     */
    int getFetchLimit();

    /**
     * Returns a query that originated this query. Originating query is a query whose
     * result is needed to obtain the result of the query owning this metadata. Most often
     * than not the returned value is null. One example of non-null originating query is a
     * query for a range of objects in a previously fetched paginated list. The query that
     * fetched the original paginated list is an "originated" query. It may be used to
     * restore a list that got lost due to a cache overflow, etc.
     * 
     * @since 3.0
     */
    Query getOrginatingQuery();

    /**
     * Returns a root node of prefetch tree used by this query, or null of no prefetches
     * are configured.
     */
    PrefetchTreeNode getPrefetchTree();

    /**
     * Returns a map of aliases vs. expression subpaths that is used to build split joins.
     * 
     * @since 3.0
     */
    Map<String, String> getPathSplitAliases();

    /**
     * Returns an optional list of result set mapping hints. Elements in the list can be
     * either {@link EntityResultSegment} or {@link ScalarResultSegment}. The returned
     * list can be null.
     * 
     * @since 3.0
     */
    List<Object> getResultSetMapping();
    
    /**
     * @return statement's fetch size
     * @since 3.0
     */
    public int getStatementFetchSize();
}
