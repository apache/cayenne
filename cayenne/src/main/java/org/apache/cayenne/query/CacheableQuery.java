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

package org.apache.cayenne.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared functionality for cacheable queries.
 *
 * @since 4.0
 */
public abstract class CacheableQuery implements Query {

    protected static final Logger logger = LoggerFactory.getLogger(CacheableQuery.class);

    abstract protected BaseQueryMetadata getBaseMetaData();

    /**
     * @since 3.0
     */
    public QueryCacheStrategy getCacheStrategy() {
        return getBaseMetaData().getCacheStrategy();
    }

    /**
     * @since 3.0
     */
    public void setCacheStrategy(QueryCacheStrategy strategy) {
        getBaseMetaData().setCacheStrategy(strategy);
    }

    /**
     * @since 4.0
     */
    public String getCacheGroup() {
        return getBaseMetaData().getCacheGroup();
    }

    /**
     * @since 4.0
     */
    public void setCacheGroup(String cacheGroup) {
        getBaseMetaData().setCacheGroup(cacheGroup);
    }


    /**
     * Instructs Cayenne to look for query results in the "local" cache when
     * running the query. This is a short-hand notation for:
     *
     * <pre>
     * query.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
     * </pre>
     *
     * @since 4.0
     */
    public void useLocalCache() {
        setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
    }

    /**
     * Instructs Cayenne to look for query results in the "local" cache when
     * running the query. This is a short-hand notation for:
     *
     * <pre>
     * query.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
     * query.setCacheGroups(&quot;group1&quot;, &quot;group2&quot;);
     * </pre>
     *
     * @since 4.0
     */
    public void useLocalCache(String cacheGroup) {
        setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        setCacheGroup(cacheGroup);
    }



    /**
     * Instructs Cayenne to look for query results in the "shared" cache when
     * running the query. This is a short-hand notation for:
     *
     * <pre>
     * query.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
     * </pre>
     *
     * @since 4.0
     */
    public void useSharedCache() {
        setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
    }

    /**
     * Instructs Cayenne to look for query results in the "shared" cache when
     * running the query. This is a short-hand notation for:
     *
     * <pre>
     * query.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
     * query.setCacheGroups(&quot;group1&quot;, &quot;group2&quot;);
     * </pre>
     *
     * @since 4.0
     */
    public void useSharedCache(String cacheGroup) {
        setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        setCacheGroup(cacheGroup);
    }

}
