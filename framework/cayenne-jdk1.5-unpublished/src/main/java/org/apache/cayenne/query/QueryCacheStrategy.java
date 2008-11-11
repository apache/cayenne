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
 * Defines query result caching policy.
 * 
 * @since 3.0
 */
public enum QueryCacheStrategy {

    /**
     * A default cache policy stating that the query results should not be cached.
     */
    NO_CACHE,

    /**
     * A cache policy stating that query results shall be cached by the ObjectContext that
     * originated the query, independent from any other ObjectContexts.
     */
    LOCAL_CACHE,

    /**
     * A cache policy stating that query results shall be cached by the ObjectContext that
     * originated the query, independent from any other ObjectContexts, however the query
     * that uses this policy should treat current cache state as expired, and force the
     * database fetch.
     */
    LOCAL_CACHE_REFRESH,

    /**
     * A cache policy ruling that query results shall be cached in a shared location
     * accessible by all ObjectContexts.
     */
    SHARED_CACHE,

    /**
     * A cache policy ruling that query results shall be cached in a shared location
     * accessible by all ObjectContexts, however the query that uses this policy should
     * treat current cache state as expired, and force the database fetch.
     */
    SHARED_CACHE_REFRESH;

    /**
     * Returns QueryCacheStrategy for the specified string name or default strategy for
     * invalid names.
     */
    public static QueryCacheStrategy safeValueOf(String string) {
        try {
            return QueryCacheStrategy.valueOf(string);
        }
        catch (IllegalArgumentException e) {
            return getDefaultStrategy();
        }
    }

    /**
     * Returns the default strategy - {@link #NO_CACHE}.
     */
    public static QueryCacheStrategy getDefaultStrategy() {
        return NO_CACHE;
    }
}
