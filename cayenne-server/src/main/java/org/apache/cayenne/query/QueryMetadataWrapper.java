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

import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper for a QueryMetadata instance allowing that may override a subset of metadata
 * properties.
 * 
 * @since 1.2
 */
class QueryMetadataWrapper extends QueryMetadataProxy {

    static final String CACHE_KEY_PROPERTY = "QueryMetadataWrapper.CacheKey";

    Map<String, Object> overrides;

    public QueryMetadataWrapper(QueryMetadata info) {
        super(info);
    }

    /**
     * Overrides a property with an alternative value. Property names are defined in the
     * {@link QueryMetadata} interface.
     */
    void override(String key, Object value) {
        if (overrides == null) {
            overrides = new HashMap<>();
        }

        overrides.put(key, value);
    }

    boolean overrideExists(String key) {
        return overrides != null && overrides.containsKey(key);
    }

    public String getCacheKey() {
        return (overrideExists(CACHE_KEY_PROPERTY))
                ? (String) overrides.get(CACHE_KEY_PROPERTY)
                : super.getCacheKey();
    }

    /**
     * @since 3.0
     */
    public QueryCacheStrategy getCacheStrategy() {
        return (overrideExists(QueryMetadata.CACHE_STRATEGY_PROPERTY))
                ? (QueryCacheStrategy) overrides.get(QueryMetadata.CACHE_STRATEGY_PROPERTY)
                : super.getCacheStrategy();
    }

    /**
     * @since 4.0
     */
    public String getCacheGroup() {
        if(overrideExists(QueryMetadata.CACHE_GROUPS_PROPERTY)) {
            String[] cacheGroups = (String[]) overrides.get(QueryMetadata.CACHE_GROUPS_PROPERTY);
            if(cacheGroups == null || cacheGroups.length == 0) {
                return null;
            } else {
                return cacheGroups[0];
            }
        }

        return super.getCacheGroup();
    }

    public boolean isFetchingDataRows() {
        if (!overrideExists(QueryMetadata.FETCHING_DATA_ROWS_PROPERTY)) {
            return super.isFetchingDataRows();
        }

        Boolean b = (Boolean) overrides.get(QueryMetadata.FETCHING_DATA_ROWS_PROPERTY);
        return b != null && b;
    }

    public boolean isRefreshingObjects() {
        return true;
    }

    public int getPageSize() {
        if (!overrideExists(QueryMetadata.PAGE_SIZE_PROPERTY)) {
            return super.getPageSize();
        }

        Number n = (Number) overrides.get(QueryMetadata.PAGE_SIZE_PROPERTY);
        return n != null ? n.intValue() : 0;
    }

    public int getFetchLimit() {
        if (!overrideExists(QueryMetadata.FETCH_LIMIT_PROPERTY)) {
            return super.getFetchLimit();
        }

        Number n = (Number) overrides.get(QueryMetadata.FETCH_LIMIT_PROPERTY);
        return n != null ? n.intValue() : 0;
    }

    public int getStatementFetchSize() {
        if (!overrideExists(QueryMetadata.STATEMENT_FETCH_SIZE_PROPERTY)) {
            return super.getPageSize();
        }

        Number n = (Number) overrides.get(QueryMetadata.STATEMENT_FETCH_SIZE_PROPERTY);
        return n != null ? n.intValue() : 0;
    }

}
