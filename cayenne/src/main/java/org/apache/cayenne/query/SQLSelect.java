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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.dba.DbAdapter;

import java.util.List;
import java.util.Map;

/**
 * A selecting query based on raw SQL and featuring fluent API.
 *
 * @since 4.0
 * @deprecated in favor of {@link SQLQuery}, that combines selecting and non-selecting SQL in a single
 * query. */
@Deprecated(since = "6.0", forRemoval = true)
public class SQLSelect<T> extends SQLQuery<T> {

    /**
     * Creates a query that selects DataRows and uses default routing.
     */
    public static SQLSelect<DataRow> dataRowQuery(String sql) {
        SQLSelect<DataRow> query = new SQLSelect<>(sql);
        query.fetchingDataRows();
        return query;
    }

    /**
     * Creates a query that selects DataRows and uses default routing.
     *
     * @since 4.1
     */
    public static SQLSelect<DataRow> dataRowQuery(String sql, Class<?>... types) {
        SQLSelect<DataRow> query = new SQLSelect<>(sql);
        query.resultColumnsTypes(types);
        query.fetchingDataRows();
        return query;
    }

    /**
     * Creates a query that selects DataRows and uses routing based on the provided DataMap name.
     *
     * @since 4.1
     */
    public static SQLSelect<DataRow> dataRowQuery(String dataMapName, String sql, Class<?>... types) {
        SQLSelect<DataRow> query = dataRowQuery(dataMapName, sql);
        query.resultColumnsTypes(types);
        return query;
    }

    /**
     * Creates a query that selects DataRows and uses routing based on the provided DataMap name.
     */
    public static SQLSelect<DataRow> dataRowQuery(String dataMapName, String sql) {
        SQLSelect<DataRow> query = new SQLSelect<>(sql);
        query.dataMapName = dataMapName;
        query.fetchingDataRows();
        return query;
    }

    /**
     * Creates a query that selects Persistent objects.
     */
    public static <T> SQLSelect<T> query(Class<T> type, String sql) {
        return new SQLSelect<>(type, sql);
    }

    /**
     * Creates query that selects scalar value and uses default routing
     *
     * @since 4.1
     */
    public static <T> SQLSelect<T> scalarQuery(String sql, Class<T> type) {
        SQLSelect<T> query = new SQLSelect<>(sql);
        query.resultColumnsTypes(type);
        query.useScalar();
        return query;
    }

    /**
     * Creates query that selects scalar value and uses routing based on the provided DataMap name.
     *
     * @since 4.1
     */
    public static <T> SQLSelect<T> scalarQuery(String sql, String dataMapName, Class<T> type) {
        SQLSelect<T> query = new SQLSelect<>(sql);
        query.dataMapName = dataMapName;
        query.resultColumnsTypes(type);
        query.useScalar();
        return query;
    }

    /**
     * Creates query that selects scalar values (as Object[]) and uses default routing
     *
     * @since 4.2
     */
    public static SQLSelect<Object[]> columnQuery(String sql) {
        SQLSelect<Object[]> query = new SQLSelect<>(sql);
        query.useScalar();
        return query;
    }

    /**
     * Creates query that selects scalar values (as Object[]) and uses routing based on the provided DataMap name.
     *
     * @since 4.2
     */
    public static SQLSelect<Object[]> columnQuery(String sql, String dataMapName) {
        SQLSelect<Object[]> query = new SQLSelect<>(sql);
        query.dataMapName = dataMapName;
        query.useScalar();
        return query;
    }

    /**
     * Creates query that selects scalar values (as Object[]) and uses default routing
     *
     * @since 4.2
     */
    public static SQLSelect<Object[]> columnQuery(String sql, Class<?>... types) {
        SQLSelect<Object[]> query = new SQLSelect<>(sql);
        query.resultColumnsTypes(types);
        query.useScalar();
        return query;
    }

    /**
     * Creates query that selects scalar values (as Object[]) and uses routing based on the provided DataMap name.
     *
     * @since 4.2
     */
    public static SQLSelect<Object[]> columnQuery(String sql, String dataMapName, Class<?>... types) {
        SQLSelect<Object[]> query = new SQLSelect<>(sql);
        query.dataMapName = dataMapName;
        query.resultColumnsTypes(types);
        query.useScalar();
        return query;
    }

    public SQLSelect(String sql) {
        super(sql);
    }

    public SQLSelect(Class<T> persistentType, String sql) {
        super(persistentType, sql);
    }

    // --- covariant overrides, so that chains built from the factory methods still assign to SQLSelect ---

    @Override
    public SQLSelect<T> append(String sqlChunk) {
        super.append(sqlChunk);
        return this;
    }

    @Override
    public SQLSelect<T> dialect(Class<? extends DbAdapter> adapterType, String sql) {
        super.dialect(adapterType, sql);
        return this;
    }

    @Override
    public SQLSelect<T> dialectAppend(Class<? extends DbAdapter> adapterType, String sqlChunk) {
        super.dialectAppend(adapterType, sqlChunk);
        return this;
    }

    @Override
    public SQLSelect<T> param(String name, Object value) {
        super.param(name, value);
        return this;
    }

    @Override
    public SQLSelect<T> params(Map<String, ?> parameters) {
        super.params(parameters);
        return this;
    }

    @Override
    public SQLSelect<T> paramsArray(Object... params) {
        super.paramsArray(params);
        return this;
    }

    @Override
    public SQLSelect<T> paramsList(List<Object> params) {
        super.paramsList(params);
        return this;
    }

    @Override
    public SQLSelect<T> localCache() {
        super.localCache();
        return this;
    }

    @Override
    public SQLSelect<T> localCache(String cacheGroup) {
        super.localCache(cacheGroup);
        return this;
    }

    @Override
    public SQLSelect<T> sharedCache() {
        super.sharedCache();
        return this;
    }

    @Override
    public SQLSelect<T> sharedCache(String cacheGroup) {
        super.sharedCache(cacheGroup);
        return this;
    }

    @Override
    public SQLSelect<T> cacheStrategy(QueryCacheStrategy strategy) {
        super.cacheStrategy(strategy);
        return this;
    }

    @Override
    public SQLSelect<T> cacheStrategy(QueryCacheStrategy strategy, String cacheGroup) {
        super.cacheStrategy(strategy, cacheGroup);
        return this;
    }

    @Override
    public SQLSelect<T> cacheGroup(String cacheGroup) {
        super.cacheGroup(cacheGroup);
        return this;
    }

    @Override
    public SQLSelect<T> columnNameCaps(CapsStrategy columnNameCaps) {
        super.columnNameCaps(columnNameCaps);
        return this;
    }

    @Override
    public SQLSelect<T> upperColumnNames() {
        super.upperColumnNames();
        return this;
    }

    @Override
    public SQLSelect<T> lowerColumnNames() {
        super.lowerColumnNames();
        return this;
    }

    @Override
    public SQLSelect<T> dataNode(String dataNodeName) {
        super.dataNode(dataNodeName);
        return this;
    }

    @Override
    public SQLSelect<T> limit(int fetchLimit) {
        super.limit(fetchLimit);
        return this;
    }

    @Override
    public SQLSelect<T> offset(int fetchOffset) {
        super.offset(fetchOffset);
        return this;
    }

    @Override
    public SQLSelect<T> pageSize(int pageSize) {
        super.pageSize(pageSize);
        return this;
    }

    @Override
    public SQLSelect<T> statementFetchSize(int size) {
        super.statementFetchSize(size);
        return this;
    }

    @Override
    public SQLSelect<T> queryTimeout(int queryTimeout) {
        super.queryTimeout(queryTimeout);
        return this;
    }

    @Override
    public SQLSelect<T> addPrefetch(String path, int semantics) {
        super.addPrefetch(path, semantics);
        return this;
    }

    @Override
    public SQLSelect<T> addPrefetch(PrefetchTreeNode node) {
        super.addPrefetch(node);
        return this;
    }
}
