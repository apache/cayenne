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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * A wrapper for a QueryMetadata instance allowing that may override a subset of metadata
 * properties.
 * 
 * @since 1.2
 */
class QueryMetadataWrapper implements QueryMetadata {

    static final String CACHE_KEY_PROPERTY = "QueryMetadataWrapper.CacheKey";

    QueryMetadata info;
    Map<String, Object> overrides;

    public QueryMetadataWrapper(QueryMetadata info) {
        this.info = info;
    }

    /**
     * Overrides a property with an alternative value. Property names are defined in the
     * {@link QueryMetadata} interface.
     */
    void override(String key, Object value) {
        if (overrides == null) {
            overrides = new HashMap<String, Object>();
        }

        overrides.put(key, value);
    }

    boolean overrideExists(String key) {
        return overrides != null && overrides.containsKey(key);
    }

    /**
     * @since 3.0
     */
    public List<Object> getResultSetMapping() {
        return info.getResultSetMapping();
    }

    public DataMap getDataMap() {
        return info.getDataMap();
    }

    public Procedure getProcedure() {
        return info.getProcedure();
    }

    /**
     * @since 3.0
     */
    public Map<String, String> getPathSplitAliases() {
        return info.getPathSplitAliases();
    }

    public DbEntity getDbEntity() {
        return info.getDbEntity();
    }

    public ObjEntity getObjEntity() {
        return info.getObjEntity();
    }

    public Query getOrginatingQuery() {
        return info.getOrginatingQuery();
    }

    /**
     * @since 3.0
     */
    public ClassDescriptor getClassDescriptor() {
        return info.getClassDescriptor();
    }

    public String getCacheKey() {
        return (overrideExists(CACHE_KEY_PROPERTY)) ? (String) overrides
                .get(CACHE_KEY_PROPERTY) : info.getCacheKey();
    }

    /**
     * @since 3.0
     */
    public QueryCacheStrategy getCacheStrategy() {
        return (overrideExists(QueryMetadata.CACHE_STRATEGY_PROPERTY))
                ? (QueryCacheStrategy) overrides
                        .get(QueryMetadata.CACHE_STRATEGY_PROPERTY)
                : info.getCacheStrategy();
    }

    public String[] getCacheGroups() {
        return (overrideExists(QueryMetadata.CACHE_GROUPS_PROPERTY))
                ? (String[]) overrides.get(QueryMetadata.CACHE_GROUPS_PROPERTY)
                : info.getCacheGroups();
    }

    public boolean isFetchingDataRows() {
        if (!overrideExists(QueryMetadata.FETCHING_DATA_ROWS_PROPERTY)) {
            return info.isFetchingDataRows();
        }

        Boolean b = (Boolean) overrides.get(QueryMetadata.FETCHING_DATA_ROWS_PROPERTY);
        return b != null && b.booleanValue();
    }

    public boolean isRefreshingObjects() {
        return true;
    }

    public int getPageSize() {
        if (!overrideExists(QueryMetadata.PAGE_SIZE_PROPERTY)) {
            return info.getPageSize();
        }

        Number n = (Number) overrides.get(QueryMetadata.PAGE_SIZE_PROPERTY);
        return n != null ? n.intValue() : 0;
    }

    public int getFetchOffset() {
        return info.getFetchOffset();
    }

    public int getFetchLimit() {
        if (!overrideExists(QueryMetadata.FETCH_LIMIT_PROPERTY)) {
            return info.getFetchLimit();
        }

        Number n = (Number) overrides.get(QueryMetadata.FETCH_LIMIT_PROPERTY);
        return n != null ? n.intValue() : 0;
    }

    public PrefetchTreeNode getPrefetchTree() {
        return info.getPrefetchTree();
    }

    public int getStatementFetchSize() {
        if (!overrideExists(QueryMetadata.STATEMENT_FETCH_SIZE_PROPERTY)) {
            return info.getPageSize();
        }

        Number n = (Number) overrides.get(QueryMetadata.STATEMENT_FETCH_SIZE_PROPERTY);
        return n != null ? n.intValue() : 0;
    }
}
