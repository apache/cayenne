/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.query;

/**
 * A query that returns result set. Concrete implementations can be object queries, raw
 * sql queries, stored procedure queries, etc. The most commonly used GenericSelectQueries
 * are {@link SelectQuery},{@link SQLTemplate}and {@link ProcedureQuery}.
 * 
 * @author Andrei Adamchik
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
    public String getCachePolicy();

    /**
     * Returns <code>true</code> if this query should produce a list of data rows as
     * opposed to DataObjects, <code>false</code> for DataObjects. This is a hint to
     * QueryEngine executing this query.
     */
    public boolean isFetchingDataRows();

    /**
     * Returns <code>true</code> if the query results should replace any currently
     * cached values, returns <code>false</code> otherwise. If
     * {@link #isFetchingDataRows()}returns <code>true</code>, this setting is not
     * applicable and has no effect.
     * 
     * @since 1.1
     */
    public boolean isRefreshingObjects();

    /**
     * Returns true if objects fetched via this query should be fully resolved according
     * to the inheritance hierarchy.
     * 
     * @since 1.1
     */
    public boolean isResolvingInherited();

    /**
     * Returns query page size. Page size is a hint to Cayenne that query should be
     * performed page by page, instead of retrieveing all results at once. If the value
     * returned is less than or equal to zero, no paging should occur.
     */
    public int getPageSize();

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
    public int getFetchLimit();
}