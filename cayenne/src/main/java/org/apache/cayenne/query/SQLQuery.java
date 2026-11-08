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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.SelectResult;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.compiled.CompiledDataMap;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.query.resolved.ResolvedQuery;
import org.apache.cayenne.query.resolved.ResolvedSQL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A query based on raw SQL and featuring fluent API. It can run any SQL: selects, updates, deletes, DDL, etc. A
 * selecting query returns its rows as persistent objects, {@link DataRow}s, scalars or {@code Object[]} depending on
 * the factory method used to create it, and everything a query produced, including update counts, is available via
 * {@link #execute(ObjectContext)}.
 *
 * @param <T> the type of the rows the query selects
 * @since 6.0
 */
public class SQLQuery<T> implements Query<T> {

    private List<Class<?>> resultColumnsTypes;
    private boolean useScalar;
    private boolean isFetchingDataRows;
    Class<T> persistentType;
    private String entityName;
    String dataMapName;
    private String dataNodeName;
    private final StringBuilder sql;
    private Map<String, StringBuilder> sqlDialects;
    private QueryCacheStrategy cacheStrategy;
    private String cacheGroup;
    private Map<String, Object> params;
    private List<Object> positionalParams;
    private CapsStrategy columnNameCaps;
    private boolean returnGeneratedKeys;
    private int limit;
    private int offset;
    private int pageSize;
    private int statementFetchSize;
    private int queryTimeout;
    private PrefetchTreeNode prefetches;

    /**
     * Creates a query running provided SQL. If the SQL selects, the rows are returned as DataRows. This is the form to
     * use for updates, deletes and DDL, when the shape of the result does not matter. A query without a persistent type
     * is routed to the default DataNode, unless {@link #dataMap(String)} or {@link #dataNode(String)} says otherwise.
     */
    public static SQLQuery<DataRow> query(String sql) {
        return new SQLQuery<>(sql).fetchingDataRows();
    }

    /**
     * Creates a query running provided SQL, returning the rows as DataRows with the columns read as the given Java
     * types, in the order of the columns.
     */
    public static SQLQuery<DataRow> query(String sql, Class<?>... types) {
        return query(sql).resultColumnsTypes(types);
    }

    /**
     * Creates a query that selects Persistent objects.
     */
    public static <T> SQLQuery<T> query(Class<T> type, String sql) {
        SQLQuery<T> query = new SQLQuery<>(sql);
        query.persistentType = type;
        return query;
    }

    /**
     * Creates a query running provided SQL and returning the rows as objects of the named ObjEntity. This is the form
     * used for queries mapped in a DataMap, where the entity is known by name.
     */
    public static SQLQuery<Persistent> entityQuery(String entityName, String sql) {
        SQLQuery<Persistent> query = new SQLQuery<>(sql);
        query.entityName = entityName;
        return query;
    }

    /**
     * Creates query that selects scalar value.
     */
    public static <T> SQLQuery<T> scalarQuery(String sql, Class<T> type) {
        SQLQuery<T> query = new SQLQuery<>(sql);
        return query.resultColumnsTypes(type).useScalar();
    }

    /**
     * Creates query that selects scalar values (as Object[]).
     */
    public static SQLQuery<Object[]> columnQuery(String sql) {
        SQLQuery<Object[]> query = new SQLQuery<>(sql);
        return query.useScalar();
    }

    /**
     * Creates query that selects scalar values (as Object[]), with the columns read as the given Java types, in the
     * order of the columns.
     */
    public static SQLQuery<Object[]> columnQuery(String sql, Class<?>... types) {
        SQLQuery<Object[]> query = new SQLQuery<>(sql);
        return query.resultColumnsTypes(types).useScalar();
    }

    // a raw SQL statement can't fetch disjoint prefetches, as they need selects of their own
    private static final PrefetchProcessor NO_DISJOINT_PREFETCHES = new PrefetchProcessor() {
        @Override
        public boolean startPhantomPrefetch(PrefetchTreeNode node) {
            return true;
        }

        @Override
        public boolean startDisjointPrefetch(PrefetchTreeNode node) {
            throw new CayenneRuntimeException("Disjoint prefetches are not supported by SQLQuery: %s. Use joint or "
                    + "disjoint-by-id prefetches instead.", node.getPath());
        }

        @Override
        public boolean startDisjointByIdPrefetch(PrefetchTreeNode node) {
            return true;
        }

        @Override
        public boolean startJointPrefetch(PrefetchTreeNode node) {
            return true;
        }

        @Override
        public boolean startUnknownPrefetch(PrefetchTreeNode node) {
            return startDisjointPrefetch(node);
        }

        @Override
        public void finishPrefetch(PrefetchTreeNode node) {
        }
    };

    protected SQLQuery(String sql) {
        this.sql = sql != null ? new StringBuilder(sql) : new StringBuilder();
        this.limit = ResolvedQuery.FETCH_LIMIT_DEFAULT;
        this.offset = ResolvedQuery.FETCH_OFFSET_DEFAULT;
        this.pageSize = ResolvedQuery.PAGE_SIZE_DEFAULT;
        this.queryTimeout = ResolvedQuery.QUERY_TIMEOUT_DEFAULT;
    }

    @Override
    public T selectFirst(ObjectContext context) {
        return context.selectFirst(limit(1));
    }

    /**
     * Appends a piece of SQL to the previously stored default SQL template. Dialects registered with
     * {@link #dialect(Class, String)} stay unchanged, use {@link #dialectAppend(Class, String)} to extend those.
     */
    public SQLQuery<T> append(String sqlChunk) {
        sql.append(sqlChunk);
        return this;
    }

    /**
     * Sets an alternative SQL template to use when the query runs against a DataNode with the given adapter. Allows
     * a single query to carry multiple "dialects" of the same SQL, one per database. The default SQL (passed to the
     * query factory method and extended with {@link #append(String)}) is used for adapters without a dialect.
     * Matching is done by the exact adapter class, so a dialect registered for an adapter superclass does not apply
     * to its subclasses.
     *
     * @param adapterType the adapter class this dialect applies to, e.g. {@code MySQLAdapter.class}
     * @param sql         the complete SQL template to use with this adapter
     */
    public SQLQuery<T> dialect(Class<? extends DbAdapter> adapterType, String sql) {
        return dialect(adapterType.getName(), sql);
    }

    /**
     * Sets the SQL to run on the adapter of a given class name, replacing any dialect set for it before. This is the
     * form used for queries mapped in a DataMap, where the adapter is known by name.
     */
    public SQLQuery<T> dialect(String adapterClassName, String sql) {
        if (sqlDialects == null) {
            sqlDialects = new HashMap<>();
        }
        sqlDialects.put(adapterClassName, new StringBuilder(sql));
        return this;
    }

    /**
     * Makes the query return its rows as DataRows instead of objects.
     */
    public SQLQuery<DataRow> fetchDataRows() {
        return fetchingDataRows();
    }

    /**
     * Appends a piece of SQL to the dialect registered for the given adapter with {@link #dialect(Class, String)}.
     * If no dialect was registered yet, the chunk becomes the start of a new one.
     */
    public SQLQuery<T> dialectAppend(Class<? extends DbAdapter> adapterType, String sqlChunk) {
        if (sqlDialects == null) {
            sqlDialects = new HashMap<>();
        }
        sqlDialects.computeIfAbsent(adapterType.getName(), k -> new StringBuilder()).append(sqlChunk);
        return this;
    }

    public SQLQuery<T> param(String name, Object value) {
        params(Collections.singletonMap(name, value));
        return this;
    }

    public SQLQuery<T> params(Map<String, ?> parameters) {
        if (this.params == null) {
            this.params = new HashMap<>(parameters);
        } else {
            this.params.putAll(parameters);
        }

        // since named parameters are specified, resetting positional parameters
        this.positionalParams = null;
        return this;
    }

    /**
     * Initializes positional parameters of the query. Parameters are bound in the order they are found in the SQL
     * template. If a given parameter name is used more than once, only the first occurrence is treated as "position",
     * subsequent occurrences are bound with the same value as the first one. If template parameters count is
     * different from the array parameter count, an exception will be thrown.
     * <p>
     * Note that calling this method will reset any previously set *named* parameters.
     */
    public SQLQuery<T> paramsArray(Object... params) {
        return paramsList(params != null ? Arrays.asList(params) : null);
    }

    /**
     * Initializes positional parameters of the query. Parameters are bound in the order they are found in the SQL
     * template. If a given parameter name is used more than once, only the first occurrence is treated as "position",
     * subsequent occurrences are bound with the same value as the first one. If template parameters count is
     * different from the list parameter count, an exception will be thrown.
     * <p>
     * Note that calling this method will reset any previously set *named* parameters.
     */
    public SQLQuery<T> paramsList(List<Object> params) {
        this.positionalParams = params;

        // since positional parameters are specified, resetting named parameters
        this.params = null;
        return this;
    }

    @Override
    public ResolvedSQL resolve(CompiledMapping mapping) {
        boolean typedColumns = resultColumnsTypes != null && !resultColumnsTypes.isEmpty();
        if (!useScalar && !isFetchingDataRows && typedColumns) {
            throw new CayenneRuntimeException(
                    "Error caused by using root in query with resultColumnTypes without scalar or dataRow.");
        }
        if (isFetchingDataRows && useScalar) {
            throw new CayenneRuntimeException("Can't set both use scalar and fetching data rows.");
        }

        // the root: a persistent type or an entity name resolve to the query entity, a DataMap name to the DataMap
        // the query is routed to; without a root the query is routed by its DataNode name
        CompiledEntity entity = null;
        CompiledDataMap dataMap = null;
        if (persistentType != null) {
            entity = mapping.entity(persistentType);
        } else if (entityName != null) {
            entity = mapping.entity(entityName);
        } else if (dataMapName != null) {
            dataMap = mapping.schema().map(dataMapName);
            if (dataMap == null) {
                throw new CayenneRuntimeException("Invalid dataMapName '%s'", dataMapName);
            }
        }

        CompiledTable table = null;
        if (entity != null) {
            table = entity.table();
            dataMap = entity.map();
        }

        ResultSegments resultSegments = resolveResultSegments(mapping, typedColumns);

        String sql = !this.sql.isEmpty() ? this.sql.toString() : null;
        Map<String, Object> params = this.params != null ? this.params : Collections.emptyMap();
        List<Object> positionalParams = this.positionalParams != null ? this.positionalParams : Collections.emptyList();
        CapsStrategy columnNameCaps = this.columnNameCaps != null ? this.columnNameCaps : CapsStrategy.DEFAULT;

        QueryCacheStrategy cacheStrategy = this.cacheStrategy != null
                ? this.cacheStrategy
                : QueryCacheStrategy.getDefaultStrategy();
        String cacheKey = cacheStrategy != QueryCacheStrategy.NO_CACHE
                ? cacheKey(entity, table, sql, params, positionalParams)
                : null;

        Map<String, String> dialects = new HashMap<>();
        if (sqlDialects != null) {
            sqlDialects.forEach((adapter, dialect) -> dialects.put(adapter, dialect.toString()));
        }

        return new ResolvedSQL(this, entity, table, dataMap, dataNodeName, sql, dialects,
                params, positionalParams, columnNameCaps, returnGeneratedKeys,
                resultColumnsTypes != null ? resultColumnsTypes : Collections.emptyList(),
                useScalar, isFetchingDataRows, limit, offset, pageSize, statementFetchSize, queryTimeout,
                cacheStrategy, cacheKey, cacheGroup, prefetches != null ? prefetches.deepClone() : null,
                resultSegments);
    }

    /**
     * Resolves the result mapping: one scalar per declared column type, an empty mapping for a scalar query without
     * declared types (meaning "one scalar per ResultSet column", to be resolved at execution), or null for objects
     * and DataRows.
     */
    private ResultSegments resolveResultSegments(CompiledMapping mapping, boolean typedColumns) {
        List<ResultSegment> segments;
        if (typedColumns && useScalar) {
            SQLResult result = new SQLResult();
            for (int i = 0; i < resultColumnsTypes.size(); i++) {
                result.addColumnResult(String.valueOf(i));
            }
            segments = result.getResolvedComponents(mapping);
        } else {
            segments = useScalar ? Collections.emptyList() : null;
        }

        // a single segment is returned as is, anything else - as an Object[]
        return segments != null ? new ResultSegments(segments, segments.size() != 1) : null;
    }

    /**
     * Creates a unique key based on entity, SQL, and parameters.
     */
    private String cacheKey(
            CompiledEntity entity,
            CompiledTable table,
            String sql,
            Map<String, Object> parameters,
            List<Object> positionalParameters) {

        StringBuilder key = new StringBuilder();

        if (entity != null) {
            key.append(entity.name());
        } else if (table != null) {
            key.append("db:").append(table.name());
        }

        if (sql != null) {
            key.append('/').append(sql);
        }

        if (!parameters.isEmpty()) {
            List<String> keys = new ArrayList<>(parameters.keySet());
            Collections.sort(keys);
            for (String parameterKey : keys) {
                key.append('/').append(parameterKey).append('=').append(parameters.get(parameterKey));
            }
        }

        for (Object parameter : positionalParameters) {
            key.append("/p:").append(parameter);
        }

        if (offset > 0 || limit > 0) {
            key.append('/');
            if (offset > 0) {
                key.append('o').append(offset);
            }
            if (limit > 0) {
                key.append('l').append(limit);
            }
        }

        return key.toString();
    }

    /**
     * Instructs Cayenne to look for query results in the "local" cache when running the query. This is a short-hand
     * notation for:
     *
     * <pre>
     * query.cacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
     * </pre>
     */
    public SQLQuery<T> localCache() {
        return cacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
    }

    /**
     * Instructs Cayenne to look for query results in the "local" cache when running the query. This is a short-hand
     * notation for:
     *
     * <pre>
     * query.cacheStrategy(QueryCacheStrategy.LOCAL_CACHE, cacheGroup);
     * </pre>
     */
    public SQLQuery<T> localCache(String cacheGroup) {
        return cacheStrategy(QueryCacheStrategy.LOCAL_CACHE, cacheGroup);
    }

    /**
     * Instructs Cayenne to look for query results in the "shared" cache when running the query. This is a short-hand
     * notation for:
     *
     * <pre>
     * query.cacheStrategy(QueryCacheStrategy.SHARED_CACHE);
     * </pre>
     */
    public SQLQuery<T> sharedCache() {
        return cacheStrategy(QueryCacheStrategy.SHARED_CACHE);
    }

    /**
     * Instructs Cayenne to look for query results in the "shared" cache when running the query. This is a short-hand
     * notation for:
     *
     * <pre>
     * query.cacheStrategy(QueryCacheStrategy.SHARED_CACHE, cacheGroup);
     * </pre>
     */
    public SQLQuery<T> sharedCache(String cacheGroup) {
        return cacheStrategy(QueryCacheStrategy.SHARED_CACHE, cacheGroup);
    }

    public SQLQuery<T> cacheStrategy(QueryCacheStrategy strategy) {
        this.cacheStrategy = strategy;
        this.cacheGroup = null;
        return this;
    }

    public SQLQuery<T> cacheStrategy(QueryCacheStrategy strategy, String cacheGroup) {
        return cacheStrategy(strategy).cacheGroup(cacheGroup);
    }

    public SQLQuery<T> cacheGroup(String cacheGroup) {
        this.cacheGroup = cacheGroup;
        return this;
    }

    /**
     * Sets a column name capitalization policy applied to selecting queries. This is used to simplify mapping of the
     * queries like "SELECT * FROM ...", ensuring that a chosen Cayenne column mapping strategy (e.g. all column names
     * in uppercase) is portable across database engines that can have varying default capitalization. Default (null)
     * value indicates that column names provided in result set are used unchanged.
     * <p>
     * Note that while a non-default setting is useful for queries that do not rely on a #result directive to describe
     * columns, it works for all SQLTemplates the same way.
     */
    public SQLQuery<T> columnNameCaps(CapsStrategy columnNameCaps) {
        this.columnNameCaps = columnNameCaps;

        return this;
    }

    /**
     * Equivalent of setting {@link CapsStrategy#UPPER}
     */
    public SQLQuery<T> upperColumnNames() {
        return columnNameCaps(CapsStrategy.UPPER);
    }

    /**
     * Equivalent of setting {@link CapsStrategy#LOWER}
     */
    public SQLQuery<T> lowerColumnNames() {
        return columnNameCaps(CapsStrategy.LOWER);
    }

    /**
     * Routes the query to the DataNode of the named DataMap. Only used by queries that have no persistent type, as a
     * typed query is routed by the DataMap of its entity. Without it, and without a {@link #dataNode(String) node
     * name}, such queries are routed to the default DataNode.
     *
     * @param dataMapName the name of the DataMap whose DataNode runs the query
     * @since 6.0
     */
    public SQLQuery<T> dataMap(String dataMapName) {
        this.dataMapName = dataMapName;
        return this;
    }

    /**
     * Routes the query to the DataNode with the given name. This allows to run SQL in a project without any mapping,
     * as the query needs neither a persistent type nor a DataMap to find its database. The node name is only used by
     * queries that have neither a persistent type nor a {@link #dataMap(String) DataMap name}. Without it such queries
     * are routed to the default DataNode.
     *
     * @param dataNodeName the name of the DataNode to run the query against
     */
    public SQLQuery<T> dataNode(String dataNodeName) {
        this.dataNodeName = dataNodeName;
        return this;
    }

    /**
     * Flag indicating that generated keys should be returned by this query execution. Generated keys are returned as
     * a {@link SelectResult} item of {@link #execute(ObjectContext)} result.
     *
     * @param returnGeneratedKeys flag value
     * @see java.sql.Statement#RETURN_GENERATED_KEYS
     */
    public SQLQuery<T> returnGeneratedKeys(boolean returnGeneratedKeys) {
        this.returnGeneratedKeys = returnGeneratedKeys;

        return this;
    }

    public SQLQuery<T> limit(int fetchLimit) {
        this.limit = fetchLimit;

        return this;
    }

    public SQLQuery<T> offset(int fetchOffset) {
        this.offset = fetchOffset;

        return this;
    }

    public SQLQuery<T> pageSize(int pageSize) {
        this.pageSize = pageSize;

        return this;
    }

    /**
     * Sets JDBC statement's fetch size (0 for no default size)
     */
    public SQLQuery<T> statementFetchSize(int size) {
        this.statementFetchSize = size;

        return this;
    }

    /**
     * Sets query timeout
     */
    public SQLQuery<T> queryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;

        return this;
    }

    /**
     * Merges a prefetch path with specified semantics into the query prefetch tree.
     *
     * @param path      Path expression
     * @param semantics Defines a strategy to prefetch relationships. See {@link PrefetchTreeNode}
     * @return this object
     */
    public SQLQuery<T> addPrefetch(String path, int semantics) {
        if (path == null) {
            return this;
        }
        if (prefetches == null) {
            prefetches = new PrefetchTreeNode();
        }
        PrefetchTreeNode node = prefetches.addPath(path);
        node.setSemantics(semantics);
        node.setPhantom(false);
        node.traverse(NO_DISJOINT_PREFETCHES);
        return this;
    }

    /**
     * Merges a prefetch into the query prefetch tree.
     *
     * @param node Prefetch which will added to query prefetch tree
     * @return this object
     */
    public SQLQuery<T> addPrefetch(PrefetchTreeNode node) {
        if (node == null) {
            return this;
        }
        node.traverse(NO_DISJOINT_PREFETCHES);
        if (prefetches == null) {
            prefetches = new PrefetchTreeNode();
        }
        prefetches.merge(node);
        return this;
    }

    @SuppressWarnings("unchecked")
    protected <E> SQLQuery<E> resultColumnsTypes(Class<?>... types) {
        if (types.length == 0) {
            throw new IllegalArgumentException("Empty types");
        }
        if (resultColumnsTypes == null) {
            resultColumnsTypes = new ArrayList<>(types.length);
        }
        Collections.addAll(resultColumnsTypes, types);
        return (SQLQuery<E>) this;
    }

    @SuppressWarnings("unchecked")
    protected <E> SQLQuery<E> fetchingDataRows() {
        this.isFetchingDataRows = true;
        return (SQLQuery<E>) this;
    }

    @SuppressWarnings("unchecked")
    protected <E> SQLQuery<E> useScalar() {
        this.useScalar = true;
        return (SQLQuery<E>) this;
    }
}
