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

/**
 * A query that returns result set.
 * 
 * @author Andrus Adamchik
 * @deprecated Since 1.2 this interface obsolete. Query parameters are described via
 *             {@link org.apache.cayenne.query.QueryMetadata}.
 */
public interface GenericSelectQuery extends Query {

    /**
     * A cache policy that disables caching of query results.
     * 
     * @since 1.1
     */
    public static final String NO_CACHE = "nocache";

    /**
     * A cache policy ruling that query results shall be cached separately for each
     * DataContext.
     * 
     * @since 1.1
     */
    public static final String LOCAL_CACHE = "localcache";

    /**
     * A cache policy ruling that query results shall be stored in a shared cache
     * accessible by all DataContexts.
     * 
     * @since 1.1
     */
    public static final String SHARED_CACHE = "sharedcache";

    /**
     * Defines the name of the property for the query {@link #getFetchLimit() fetch limit}.
     * 
     * @since 1.1
     */
    public static final String FETCH_LIMIT_PROPERTY = "cayenne.GenericSelectQuery.fetchLimit";

    /**
     * Defines default query fetch limit, which is zero, meaning that all matching rows
     * should be fetched.
     * 
     * @since 1.1
     */
    public static final int FETCH_LIMIT_DEFAULT = 0;

    /**
     * Defines the name of the property for the query {@link #getPageSize() page size}.
     * 
     * @since 1.1
     */
    public static final String PAGE_SIZE_PROPERTY = "cayenne.GenericSelectQuery.pageSize";

    /**
     * Defines default query page size, which is zero for no pagination.
     * 
     * @since 1.1
     */
    public static final int PAGE_SIZE_DEFAULT = 0;

    /**
     * @since 1.1
     */
    public static final String FETCHING_DATA_ROWS_PROPERTY = "cayenne.GenericSelectQuery.fetchingDataRows";

    /**
     * @since 1.1
     */
    public static final boolean FETCHING_DATA_ROWS_DEFAULT = false;

    /**
     * @since 1.1
     */
    public static final String REFRESHING_OBJECTS_PROPERTY = "cayenne.GenericSelectQuery.refreshingObjects";

    /**
     * @since 1.1
     */
    public static final boolean REFRESHING_OBJECTS_DEFAULT = true;

    /**
     * @since 1.1
     */
    public static final String RESOLVING_INHERITED_PROPERTY = "cayenne.GenericSelectQuery.resolvingInherited";

    /**
     * @since 1.1
     */
    public static final boolean RESOLVING_INHERITED_DEFAULT = true;

    /**
     * @since 1.1
     */
    public static final String CACHE_POLICY_PROPERTY = "cayenne.GenericSelectQuery.cachePolicy";

    /**
     * @since 1.1
     */
    public static final String CACHE_POLICY_DEFAULT = NO_CACHE;

    /**
     * Returns query cache policy, which can be one of {@link #NO_CACHE},
     * {@link #LOCAL_CACHE}, or {@link #SHARED_CACHE}. NO_CACHE is generally a default
     * policy.
     * 
     * @since 1.1
     */
    String getCachePolicy();

    /**
     * Returns <code>true</code> if this query should produce a list of data rows as
     * opposed to DataObjects, <code>false</code> for DataObjects. This is a hint to
     * QueryEngine executing this query.
     */
    boolean isFetchingDataRows();

    /**
     * Returns <code>true</code> if the query results should replace any currently
     * cached values, returns <code>false</code> otherwise. If
     * {@link #isFetchingDataRows()}returns <code>true</code>, this setting is not
     * applicable and has no effect.
     * 
     * @since 1.1
     */
    boolean isRefreshingObjects();

    /**
     * Returns true if objects fetched via this query should be fully resolved according
     * to the inheritance hierarchy.
     * 
     * @since 1.1
     */
    boolean isResolvingInherited();

    /**
     * Returns query page size. Page size is a hint to Cayenne that query should be
     * performed page by page, instead of retrieveing all results at once. If the value
     * returned is less than or equal to zero, no paging should occur.
     */
    int getPageSize();

    /**
     * Returns the limit on the maximium number of records that can be returned by this
     * query. If the actual number of rows in the result exceeds the fetch limit, they
     * will be discarded. One possible use of fetch limit is using it as a safeguard
     * against large result sets that may lead to the application running out of memory,
     * etc. If a fetch limit is greater or equal to zero, all rows will be returned.
     * 
     * @return the limit on the maximium number of records that can be returned by this
     *         query
     */
    int getFetchLimit();

    /**
     * Returns a root node of prefetch tree used by this query, or null of no prefetches
     * are configured.
     * 
     * @since 1.2
     */
    PrefetchTreeNode getPrefetchTree();
}
