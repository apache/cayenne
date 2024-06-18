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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.ResultIteratorCallback;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;

/**
 * A selecting query based on raw SQL and featuring fluent API.
 * 
 * @since 4.0
 */
public class SQLSelect<T> extends IndirectQuery implements Select<T> {

	private static final long serialVersionUID = -7074293371883740872L;

	private List<Class<?>> resultColumnsTypes;
	private boolean useScalar;
	private boolean isFetchingDataRows;
	private Function<T, ?> resultMapper;

	/**
	 * Creates a query that selects DataRows and uses default routing.
	 */
	public static SQLSelect<DataRow> dataRowQuery(String sql) {
		return new SQLSelect<>(sql)
				.fetchingDataRows();
	}

	/**
	 * Creates a query that selects DataRows and uses default routing.
	 * @since 4.1
	 */
	public static SQLSelect<DataRow> dataRowQuery(String sql, Class<?>... types) {
		return new SQLSelect<>(sql)
				.resultColumnsTypes(types)
				.fetchingDataRows();
	}

	/**
	 * Creates a query that selects DataRows and uses routing based on the
	 * provided DataMap name.
	 * @since 4.1
	 */
	public static SQLSelect<DataRow> dataRowQuery(String dataMapName, String sql, Class<?>... types) {
		return dataRowQuery(dataMapName, sql).resultColumnsTypes(types);
	}

	/**
	 * Creates a query that selects DataRows and uses routing based on the
	 * provided DataMap name.
	 */
	public static SQLSelect<DataRow> dataRowQuery(String dataMapName, String sql) {
		SQLSelect<DataRow> query = new SQLSelect<>(sql);
		query.dataMapName = dataMapName;
		return query.fetchingDataRows();
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
		return query.resultColumnsTypes(type).useScalar();
	}

	/**
	 * Creates query that selects scalar value and uses default routing
	 *
	 * @since 4.1
	 */
	public static <T> SQLSelect<T> scalarQuery(String sql, String dataMapName, Class<T> type) {
		SQLSelect<T> query = new SQLSelect<>(sql);
		query.dataMapName = dataMapName;
		return query.resultColumnsTypes(type).useScalar();
	}

	/**
	 *  Creates query that selects scalar values (as Object[]) and uses default routing
	 *
	 *  @since 4.2
	 */
	public static SQLSelect<Object[]> columnQuery(String sql) {
		SQLSelect<Object[]> query = new SQLSelect<>(sql);
		return query.useScalar();
	}

	/**
	 * Creates query that selects scalar values (as Object[]) and uses routing based on the
	 * provided DataMap name.
	 *
	 * @since 4.2
	 */
	public static SQLSelect<Object[]> columnQuery(String sql, String dataMapName) {
		SQLSelect<Object[]> query = new SQLSelect<>(sql);
		query.dataMapName = dataMapName;
		return query.useScalar();
	}

	/**
	 * Creates query that selects scalar values (as Object[]) and uses default routing
	 *
	 * @since 4.2
	 */
	public static SQLSelect<Object[]> columnQuery(String sql, Class<?>... types) {
		SQLSelect<Object[]> query = new SQLSelect<>(sql);
		return query.resultColumnsTypes(types).useScalar();
	}

	/**
	 * Creates query that selects scalar values (as Object[]) and uses routing based on the
	 * provided DataMap name.
	 *
	 * @since 4.2
	 */
	public static SQLSelect<Object[]> columnQuery(String sql,
												  String dataMapName,
												  Class<?>... types) {
		SQLSelect<Object[]> query = new SQLSelect<>(sql);
		query.dataMapName = dataMapName;
		return query.resultColumnsTypes(types).useScalar();
	}

	@SuppressWarnings("unchecked")
	private <E> SQLSelect<E> resultColumnsTypes(Class<?>... types) {
		if(types.length == 0) {
			throw new IllegalArgumentException("Empty types");
		}
		if(resultColumnsTypes == null) {
			resultColumnsTypes = new ArrayList<>(types.length);
		}
		Collections.addAll(resultColumnsTypes, types);
		return (SQLSelect<E>)this;
	}

	protected Class<T> persistentType;
	protected String dataMapName;
	protected StringBuilder sqlBuffer;
	protected QueryCacheStrategy cacheStrategy;
	protected String cacheGroup;
	protected Map<String, Object> params;
	protected List<Object> positionalParams;
	protected CapsStrategy columnNameCaps;
	protected int limit;
	protected int offset;
	protected int pageSize;
	protected int statementFetchSize;
	protected int queryTimeout;
	protected PrefetchTreeNode prefetches;

	public SQLSelect(String sql) {
		this(null, sql);
	}

	public SQLSelect(Class<T> persistentType, String sql) {
		this.persistentType = persistentType;
		this.sqlBuffer = sql != null ? new StringBuilder(sql) : new StringBuilder();
		this.limit = QueryMetadata.FETCH_LIMIT_DEFAULT;
		this.offset = QueryMetadata.FETCH_OFFSET_DEFAULT;
		this.pageSize = QueryMetadata.PAGE_SIZE_DEFAULT;
	}

	@Override
	public List<T> select(ObjectContext context) {
		return context.select(this);
	}

	@Override
	public T selectOne(ObjectContext context) {
		return context.selectOne(this);
	}

	@Override
	public T selectFirst(ObjectContext context) {
		return context.selectFirst(limit(1));
	}

	@Override
	public void iterate(ObjectContext context, ResultIteratorCallback<T> callback) {
		context.iterate(this, callback);
	}

	@Override
	public ResultIterator<T> iterator(ObjectContext context) {
		return context.iterator(this);
	}

	@Override
	public ResultBatchIterator<T> batchIterator(ObjectContext context, int size) {
		return context.batchIterator(this, size);
	}

	public boolean isFetchingDataRows() {
		return isFetchingDataRows;
	}

	public String getSql() {
		String sql = sqlBuffer.toString();
		return sql.length() > 0 ? sql : null;
	}

	/**
	 * Appends a piece of SQL to the previously stored SQL template.
	 */
	public SQLSelect<T> append(String sqlChunk) {
		sqlBuffer.append(sqlChunk);
		this.replacementQuery = null;
		return this;
	}

	/**
	 * @since 4.2
	 */
	public SQLSelect<T> param(String name, Object value) {
		params(Collections.singletonMap(name, value));
		return this;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public SQLSelect<T> params(Map<String, ?> parameters) {
		if (this.params == null) {
			this.params = new HashMap<>(parameters);
		} else {
			this.params.putAll(parameters);
		}
		this.replacementQuery = null;

		// since named parameters are specified, resetting positional parameters
		this.positionalParams = null;
		return this;
	}

	/**
	 * Initializes positional parameters of the query. Parameters are bound in
	 * the order they are found in the SQL template. If a given parameter name
	 * is used more than once, only the first occurrence is treated as
	 * "position", subsequent occurrences are bound with the same value as the
	 * first one. If template parameters count is different from the array
	 * parameter count, an exception will be thrown.
	 * <p>
	 * Note that calling this method will reset any previously set *named*
	 * parameters.
	 */
	public SQLSelect<T> paramsArray(Object... params) {
		return paramsList(params != null ? Arrays.asList(params) : null);
	}

	/**
	 * Initializes positional parameters of the query. Parameters are bound in
	 * the order they are found in the SQL template. If a given parameter name
	 * is used more than once, only the first occurrence is treated as
	 * "position", subsequent occurrences are bound with the same value as the
	 * first one. If template parameters count is different from the list
	 * parameter count, an exception will be thrown.
	 * <p>
	 * Note that calling this method will reset any previously set *named*
	 * parameters.
	 */
	public SQLSelect<T> paramsList(List<Object> params) {
		this.positionalParams = params;
		this.replacementQuery = null;

		// since named parameters are specified, resetting positional parameters
		this.params = null;
		return this;
	}

	/**
	 * Returns a potentially immmutable map of named parameters that will be
	 * bound to SQL.
	 */
	public Map<String, Object> getParams() {
		return params != null ? params : Collections.<String, Object> emptyMap();
	}

	/**
	 * Returns a potentially immmutable list of positional parameters that will
	 * be bound to SQL.
	 */
	public List<Object> getPositionalParams() {
		return positionalParams != null ? positionalParams : Collections.emptyList();
	}

	@Override
	protected Query createReplacementQuery(EntityResolver resolver) {

		Object root;

		if (persistentType != null) {
			root = persistentType;
		} else if (dataMapName != null) {
			DataMap map = resolver.getDataMap(dataMapName);
			if (map == null) {
				throw new CayenneRuntimeException("Invalid dataMapName '%s'", dataMapName);
			}
			root = map;
		} else {
			// will route via default node. TODO: allow explicit node name?
			root = null;
		}

		SQLTemplate template = new SQLTemplate();
		template.setFetchingDataRows(isFetchingDataRows);
		template.setRoot(root);
		template.setDefaultTemplate(getSql());
		template.setCacheGroup(cacheGroup);
		template.setCacheStrategy(cacheStrategy);
		template.setResultColumnsTypes(resultColumnsTypes);
		if (prefetches != null) {
			template.addPrefetch(prefetches);
		}

		if (positionalParams != null) {
			template.setParamsList(positionalParams);
		} else {
			template.setParams(params);
		}

		template.setColumnNamesCapitalization(columnNameCaps);
		template.setFetchLimit(limit);
		template.setFetchOffset(offset);
		template.setPageSize(pageSize);
		template.setStatementFetchSize(statementFetchSize);
		template.setQueryTimeout(queryTimeout);
		template.setUseScalar(useScalar);
		template.setResultMapper(resultMapper);

		return template;
	}

	/**
	 * Instructs Cayenne to look for query results in the "local" cache when
	 * running the query. This is a short-hand notation for:
	 * 
	 * <pre>
	 * query.cacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
	 * </pre>
	 */
	public SQLSelect<T> localCache() {
		return cacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
	}

	/**
	 * Instructs Cayenne to look for query results in the "local" cache when
	 * running the query. This is a short-hand notation for:
	 *
	 * <pre>
	 * query.cacheStrategy(QueryCacheStrategy.LOCAL_CACHE, cacheGroup);
	 * </pre>
	 */
	public SQLSelect<T> localCache(String cacheGroup) {
		return cacheStrategy(QueryCacheStrategy.LOCAL_CACHE, cacheGroup);
	}

	/**
	 * Instructs Cayenne to look for query results in the "shared" cache when
	 * running the query. This is a short-hand notation for:
	 * 
	 * <pre>
	 * query.cacheStrategy(QueryCacheStrategy.SHARED_CACHE);
	 * </pre>
	 */
	public SQLSelect<T> sharedCache() {
		return cacheStrategy(QueryCacheStrategy.SHARED_CACHE);
	}

	/**
	 * Instructs Cayenne to look for query results in the "shared" cache when
	 * running the query. This is a short-hand notation for:
	 *
	 * <pre>
	 * query.cacheStrategy(QueryCacheStrategy.SHARED_CACHE, cacheGroup);
	 * </pre>
	 */
	public SQLSelect<T> sharedCache(String cacheGroup) {
		return cacheStrategy(QueryCacheStrategy.SHARED_CACHE, cacheGroup);
	}

	public QueryCacheStrategy getCacheStrategy() {
		return cacheStrategy;
	}

	public SQLSelect<T> cacheStrategy(QueryCacheStrategy strategy) {
		if(cacheStrategy != strategy) {
			cacheStrategy = strategy;
			replacementQuery = null;
		}
		if(cacheGroup != null) {
			cacheGroup = null;
			replacementQuery = null;
		}

		return this;
	}

	public SQLSelect<T> cacheStrategy(QueryCacheStrategy strategy, String cacheGroup) {
		return cacheStrategy(strategy).cacheGroup(cacheGroup);
	}

	public String getCacheGroup() {
		return cacheGroup;
	}

	public SQLSelect<T> cacheGroup(String cacheGroup) {
		this.cacheGroup = cacheGroup;
		this.replacementQuery = null;
		return this;
	}

	/**
	 * Returns a column name capitalization policy applied to selecting queries.
	 * This is used to simplify mapping of the queries like "SELECT * FROM ...",
	 * ensuring that a chosen Cayenne column mapping strategy (e.g. all column
	 * names in uppercase) is portable across database engines that can have
	 * varying default capitalization. Default (null) value indicates that
	 * column names provided in result set are used unchanged.
	 */
	public CapsStrategy getColumnNameCaps() {
		return columnNameCaps;
	}

	/**
	 * Sets a column name capitalization policy applied to selecting queries.
	 * This is used to simplify mapping of the queries like "SELECT * FROM ...",
	 * ensuring that a chosen Cayenne column mapping strategy (e.g. all column
	 * names in uppercase) is portable across database engines that can have
	 * varying default capitalization. Default (null) value indicates that
	 * column names provided in result set are used unchanged.
	 * <p>
	 * Note that while a non-default setting is useful for queries that do not
	 * rely on a #result directive to describe columns, it works for all
	 * SQLTemplates the same way.
	 */
	public SQLSelect<T> columnNameCaps(CapsStrategy columnNameCaps) {
		if (this.columnNameCaps != columnNameCaps) {
			this.columnNameCaps = columnNameCaps;
			this.replacementQuery = null;
		}

		return this;
	}

	/**
	 * Equivalent of setting {@link CapsStrategy#UPPER}
	 */
	public SQLSelect<T> upperColumnNames() {
		return columnNameCaps(CapsStrategy.UPPER);
	}

	/**
	 * Equivalent of setting {@link CapsStrategy#LOWER}
	 */
	public SQLSelect<T> lowerColumnNames() {
		return columnNameCaps(CapsStrategy.LOWER);
	}

	public int getLimit() {
		return limit;
	}

	public SQLSelect<T> limit(int fetchLimit) {
		if (this.limit != fetchLimit) {
			this.limit = fetchLimit;
			this.replacementQuery = null;
		}

		return this;
	}

	public int getOffset() {
		return offset;
	}

	public SQLSelect<T> offset(int fetchOffset) {
		if (this.offset != fetchOffset) {
			this.offset = fetchOffset;
			this.replacementQuery = null;
		}

		return this;
	}

	public int getPageSize() {
		return pageSize;
	}

	public SQLSelect<T> pageSize(int pageSize) {
		if (this.pageSize != pageSize) {
			this.pageSize = pageSize;
			this.replacementQuery = null;
		}

		return this;
	}

	/**
	 * Sets JDBC statement's fetch size (0 for no default size)
	 */
	public SQLSelect<T> statementFetchSize(int size) {
		if (this.statementFetchSize != size) {
			this.statementFetchSize = size;
			this.replacementQuery = null;
		}

		return this;
	}

	/**
	 * Sets query timeout
	 * @since 4.2
	 */
	public SQLSelect<T> queryTimeout(int queryTimeout) {
		if(this.queryTimeout != queryTimeout) {
			this.queryTimeout = queryTimeout;
			this.replacementQuery = null;
		}

		return this;
	}

	/**
	 * @return JBDC statement's fetch size
	 */
	public int getStatementFetchSize() {
		return statementFetchSize;
	}

	public int getQueryTimeout() {
		return queryTimeout;
	}

	/**
	 * Merges a prefetch path with specified semantics into the query prefetch tree.
	 * @param path Path expression
	 * @param semantics Defines a strategy to prefetch relationships. See {@link PrefetchTreeNode}
	 * @return this object
	 * @since 4.1
	 */
	public SQLSelect<T> addPrefetch(String path, int semantics) {
		if (path == null) {
			return this;
		}
		if (prefetches == null) {
			prefetches = new PrefetchTreeNode();
		}
		prefetches.addPath(path).setSemantics(semantics);
		return this;
	}

	/**
	 * Merges a prefetch into the query prefetch tree.
	 * @param node Prefetch which will added to query prefetch tree
	 * @return this object
	 * @since 4.1
	 */
	public SQLSelect<T> addPrefetch(PrefetchTreeNode node) {
		if (node == null) {
			return this;
		}
		if (prefetches == null) {
			prefetches = new PrefetchTreeNode();
		}
		prefetches.merge(node);
		return this;
	}

	/**
	 * Map result of this query by processing with a given function.
	 * <br/>
	 * Could be used to map plain Object[] to some domain-specific object.
	 * <br/>
	 * <b>Note:</b> this method could be called multiple time, result will be mapped by all functions in the call order.
	 * @param mapper function that maps result to the required type.
	 * @return this query with changed result type
	 * @param <E> new result type
	 *
	 * @see ColumnSelect#map(Function)
	 *
	 * @since 4.2
	 */
	@SuppressWarnings("unchecked")
	public <E> SQLSelect<E> map(Function<T, E> mapper) {
		this.resultMapper = mapper;
		return (SQLSelect<E>)this;
	}

	@SuppressWarnings("unchecked")
	private <E> SQLSelect<E> fetchingDataRows() {
		this.isFetchingDataRows = true;
		return (SQLSelect<E>) this;
	}

	@SuppressWarnings("unchecked")
	private <E> SQLSelect<E> useScalar() {
		this.useScalar = true;
		return (SQLSelect<E>) this;
	}
}
