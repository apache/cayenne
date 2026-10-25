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
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.util.ToStringBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A query that executes unchanged (except for template preprocessing) "raw" SQL
 * specified by the user. <h3>Template Script</h3>
 * <p>
 * SQLTemplate stores a dynamic template for the SQL query that supports
 * parameters and customization using Velocity scripting language. The most
 * straightforward use of scripting abilities is to build parameterized queries.
 * For example:
 * </p>
 * 
 * <pre>
 *                  SELECT ID, NAME FROM SOME_TABLE WHERE NAME LIKE $a
 * </pre>
 * <p>
 * <i>For advanced scripting options see "Scripting SQLTemplate" chapter in the
 * User Guide. </i>
 * </p>
 * <h3>Per-Database Template Customization</h3>
 * <p>
 * SQLTemplate has a {@link #getDefaultTemplate() default template script}, but
 * also it allows to configure multiple templates and switch them dynamically.
 * This way a single query can have multiple "dialects" specific to a given
 * database.
 * </p>
 * <h3>Query Root</h3>
 * <p>
 * The root of a SQLTemplate determines the DataNode that runs the SQL and, for a query fetching objects, their
 * entity. It is one of a persistent class ({@link #query(Class, String)}), an ObjEntity name
 * ({@link #entityQuery(String, String)}) or a DataMap name ({@link #dataRowQuery(String, String)}), and is fixed
 * when the query is created. A query with no root ({@link #SQLTemplate(String)}, {@link #dataRowQuery(String)})
 * runs on the DataNode set via {@link #setDataNodeName(String)}, or on the default DataNode.
 * </p>
 * 
 * @param <T> the type of the result elements. Depending on the query root and configuration, this may be a
 *            persistent object, a {@link DataRow}, a scalar or an {@code Object[]}.
 * @since 1.1
 */
public class SQLTemplate<T> implements Select<T> {

	private final Class<T> persistentType;
	private final String entityName;
	private final String dataMapName;
	private final SQLTemplateMetadata metaData;

	protected String defaultTemplate;
	protected Map<String, String> templates;
	protected Map<String, ?> params;
	protected List<Object> positionalParams;
	protected CapsStrategy columnNamesCapitalization;
	protected SQLResult result;
	private String dataNodeName;
	protected boolean returnGeneratedKeys;

	private List<Class<?>> resultColumnsTypes;
	private boolean useScalar;

	/**
	 * Creates a query fetching objects of the given persistent type, routed to the DataNode of the type's entity.
	 *
	 * @since 6.0
	 */
	public static <T> SQLTemplate<T> query(Class<T> type, String sql) {
		return new SQLTemplate<>(type, sql);
	}

	/**
	 * Creates a query fetching objects of the named ObjEntity, routed to the DataNode of that entity. This is the
	 * form of the queries mapped in a DataMap, where the entity class is not necessarily known.
	 *
	 * @since 6.0
	 */
	public static SQLTemplate<Persistent> entityQuery(String entityName, String sql) {
		return new SQLTemplate<>(null, entityName, null, sql);
	}

	/**
	 * Creates a query fetching DataRows, routed to the DataNode set via {@link #setDataNodeName(String)} or to the
	 * default DataNode.
	 *
	 * @since 6.0
	 */
	public static SQLTemplate<DataRow> dataRowQuery(String sql) {
		SQLTemplate<DataRow> query = new SQLTemplate<>(sql);
		query.setFetchingDataRows(true);
		return query;
	}

	/**
	 * Creates a query fetching DataRows, routed to the DataNode of the named DataMap.
	 *
	 * @since 6.0
	 */
	public static SQLTemplate<DataRow> dataRowQuery(String dataMapName, String sql) {
		SQLTemplate<DataRow> query = new SQLTemplate<>(null, null, dataMapName, sql);
		query.setFetchingDataRows(true);
		return query;
	}

	/**
	 * Creates a query with no root. It is routed to the DataNode set via {@link #setDataNodeName(String)}, or to the
	 * default DataNode, and the shape of its result, if it has one, is defined by
	 * {@link #setFetchingDataRows(boolean)}, {@link #setUseScalar(boolean)} or {@link #setResult(SQLResult)}.
	 *
	 * @since 6.0
	 */
	public SQLTemplate(String sql) {
		this(null, null, null, sql);
	}

	/**
	 * Creates a query fetching objects of the given persistent type, routed to the DataNode of the type's entity.
	 *
	 * @since 1.2
	 */
	public SQLTemplate(Class<T> persistentType, String sql) {
		this(persistentType, null, null, sql);
	}

	private SQLTemplate(Class<T> persistentType, String entityName, String dataMapName, String sql) {
		this.persistentType = persistentType;
		this.entityName = entityName;
		this.dataMapName = dataMapName;
		this.defaultTemplate = sql;
		this.metaData = new SQLTemplateMetadata();
	}

	public void setResultColumnsTypes(Class<?> ...types) {
		if(resultColumnsTypes == null) {
			resultColumnsTypes = new ArrayList<>(types.length);
		}
		Collections.addAll(resultColumnsTypes, types);
	}

	Class<T> getPersistentType() {
		return persistentType;
	}

	String getEntityName() {
		return entityName;
	}

	String getDataMapName() {
		return dataMapName;
	}

	@Override
	public String toString() {
		ToStringBuilder builder = new ToStringBuilder(this);
		if (persistentType != null) {
			builder.append("persistentType", persistentType.getName());
		} else if (entityName != null) {
			builder.append("entityName", entityName);
		} else if (dataMapName != null) {
			builder.append("dataMapName", dataMapName);
		}
		return builder.append("sql", defaultTemplate).toString();
	}

	@Override
	public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
		DataMap map = getMetaData(resolver).getDataMap();

		DataNode dataNode;
		if (map != null) {
			dataNode = router.nodeForDataMap(map);
		} else {
			dataNode = router.nodeForName(getDataNodeName());
		}

		router.route(dataNode, this, substitutedQuery);
	}

	/**
	 * @since 1.2
	 */
	@Override
	public QueryMetadata getMetaData(EntityResolver resolver) {
		metaData.resolve(resolver, this);
		return metaData;
	}

	/**
	 * Calls <em>sqlAction(this)</em> on the visitor.
	 * 
	 * @since 1.2
	 */
	@Override
	public SQLAction createSQLAction(SQLActionVisitor visitor) {
		return visitor.sqlAction(this);
	}

	/**
	 * @since 5.0
	 */
	@Override
	public T selectFirst(ObjectContext context) {
		setFetchLimit(1);
		return context.selectFirst(this);
	}

	/**
	 * Initializes named parameters of this query. Note that calling this method
	 * will reset any positional parameters.
	 * 
	 * @since 4.0
	 */
	public void setParams(Map<String, ?> params) {
		// since named parameters are specified, resetting positional parameters
		this.positionalParams = null;
		// copy parameters so that later changes to the caller's map do not affect the query
		this.params = params != null ? new HashMap<>(params) : null;
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
	 * 
	 * @since 4.0
	 */
	public void setParamsArray(Object... params) {
		setParamsList(params != null ? Arrays.asList(params) : null);
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
	 * 
	 * @since 4.0
	 */
	public void setParamsList(List<Object> params) {
		// since positional parameters are specified, resetting named parameters
		this.params = null;
		this.positionalParams = params != null ? new ArrayList<>(params) : null;
	}

	/**
	 * Returns a new query built using this query as a prototype and a new set
	 * of named parameters.
	 */
	public SQLTemplate<T> queryWithParameters(Map<String, ?> parameters) {
		// create a query replica
		SQLTemplate<T> query = new SQLTemplate<>(persistentType, entityName, dataMapName, defaultTemplate);

		if (templates != null) {
			query.templates = new HashMap<>(templates);
		}

		query.metaData.copyFromInfo(this.metaData);
		query.setParams(parameters);
		query.setColumnNamesCapitalization(this.getColumnNamesCapitalization());

		return query;
	}

	/**
	 * @since 3.0
	 */
	public QueryCacheStrategy getCacheStrategy() {
		return metaData.getCacheStrategy();
	}

	/**
	 * @since 3.0
	 */
	public void setCacheStrategy(QueryCacheStrategy strategy) {
		metaData.setCacheStrategy(strategy);
	}

	/**
	 * @since 4.0
	 */
	public String getCacheGroup() {
		return metaData.getCacheGroup();
	}

	/**
	 * @since 4.0
	 */
	public void setCacheGroup(String cacheGroup) {
		metaData.setCacheGroup(cacheGroup);
	}

	public int getFetchLimit() {
		return metaData.getFetchLimit();
	}

	public void setFetchLimit(int fetchLimit) {
		this.metaData.setFetchLimit(fetchLimit);
	}

	/**
	 * @since 3.0
	 */
	public int getFetchOffset() {
		return metaData.getFetchOffset();
	}

	/**
	 * @since 3.0
	 */
	public void setFetchOffset(int fetchOffset) {
		metaData.setFetchOffset(fetchOffset);
	}

	public int getPageSize() {
		return metaData.getPageSize();
	}

	public void setPageSize(int pageSize) {
		metaData.setPageSize(pageSize);
	}

	public void setFetchingDataRows(boolean flag) {
		metaData.setFetchingDataRows(flag);
	}

	public boolean isFetchingDataRows() {
		return metaData.isFetchingDataRows();
	}

	/**
	 * Returns default SQL template for this query.
	 */
	public String getDefaultTemplate() {
		return defaultTemplate;
	}

	/**
	 * Sets default SQL template for this query.
	 */
	public void setDefaultTemplate(String string) {
		defaultTemplate = string;
	}

	/**
	 * Returns a template for key, or a default template if a template for key
	 * is not found.
	 */
	public synchronized String getTemplate(String key) {
		if (templates == null) {
			return defaultTemplate;
		}

		String template = templates.get(key);
		return (template != null) ? template : defaultTemplate;
	}

	/**
	 * Returns template for key, or null if there is no template configured for
	 * this key. Unlike {@link #getTemplate(String)}this method does not return
	 * a default template as a failover strategy, rather it returns null.
	 */
	public synchronized String getCustomTemplate(String key) {
		return (templates != null) ? templates.get(key) : null;
	}

	/**
	 * Adds a SQL template string for a given key. Note the keys understood
	 * by Cayenne must be fully qualified adapter class names. This way the
	 * framework can relate current DataNode to the right template. E.g.
	 * "org.apache.cayenne.dba.oracle.OracleAdapter" is a key that should be
	 * used to setup an Oracle-specific template.
	 * 
	 * @see #setDefaultTemplate(String)
	 */
	public synchronized void setTemplate(String key, String template) {
		if (templates == null) {
			templates = new HashMap<>();
		}

		templates.put(key, template);
	}

	public synchronized void removeTemplate(String key) {
		if (templates != null) {
			templates.remove(key);
		}
	}

	/**
	 * Returns a collection of configured template keys.
	 */
	public synchronized Collection<String> getTemplateKeys() {
		return (templates != null) ? templates.keySet() : Collections.emptyList();
	}

	/**
	 * Returns a map of named parameters that will be bound to SQL.
	 * 
	 * @since 4.0
	 */
	public Map<String, ?> getParams() {
		return params != null ? params : Collections.emptyMap();
	}

	/**
	 * Returns a list of positional parameters that will be bound to SQL.
	 * 
	 * @since 4.0
	 */
	public List<Object> getPositionalParams() {
		return positionalParams != null ? positionalParams : Collections.emptyList();
	}

	/**
	 * @since 1.2
	 */
	public PrefetchTreeNode getPrefetchTree() {
		return metaData.getPrefetchTree();
	}


	/**
	 * Adds a prefetch.
	 * 
	 * @since 1.2
	 */
	public PrefetchTreeNode addPrefetch(String prefetchPath) {
		// by default use JOINT_PREFETCH_SEMANTICS
		return metaData.addPrefetch(prefetchPath, PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
	}

	/**
	 * Adds a prefetch with specified relationship path to the query.
	 *
	 * @since 4.0
	 */
	public void addPrefetch(PrefetchTreeNode prefetchElement) {
		checkForDisjointNode(prefetchElement);
		metaData.mergePrefetch(prefetchElement);
	}

	/**
	 * Check for disjoint element and throw if it's found.
	 * @param prefetchElement to check
	 */
	private void checkForDisjointNode(PrefetchTreeNode prefetchElement) {
		if (prefetchElement.isDisjointPrefetch()) {
			throw new CayenneRuntimeException("This query supports only 'joint' and 'disjointById' prefetching semantics.");
		}
		for (PrefetchTreeNode child : prefetchElement.getChildren()) {
			checkForDisjointNode(child);
		}
	}

	/**
	 * @since 1.2
	 */
	public void removePrefetch(String prefetch) {
		metaData.removePrefetch(prefetch);
	}

	/**
	 * Adds all prefetches from a provided collection.
	 * 
	 * @since 1.2
	 */
	public void addPrefetches(Collection<String> prefetches) {
		metaData.addPrefetches(prefetches, PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
	}

	/**
	 * Clears all prefetches.
	 * 
	 * @since 1.2
	 */
	public void clearPrefetches() {
		metaData.clearPrefetches();
	}

	/**
	 * Returns a column name capitalization policy applied to selecting queries.
	 * This is used to simplify mapping of the queries like "SELECT * FROM ...",
	 * ensuring that a chosen Cayenne column mapping strategy (e.g. all column
	 * names in uppercase) is portable across database engines that can have
	 * varying default capitalization. Default (null) value indicates that
	 * column names provided in result set are used unchanged.
	 * 
	 * @since 3.0
	 */
	public CapsStrategy getColumnNamesCapitalization() {
		return columnNamesCapitalization != null ? columnNamesCapitalization : CapsStrategy.DEFAULT;
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
	 * 
	 * @since 3.0
	 */
	public void setColumnNamesCapitalization(CapsStrategy columnNameCapitalization) {
		this.columnNamesCapitalization = columnNameCapitalization;
	}

	/**
	 * Sets an optional explicit mapping of the result set. If result set
	 * mapping is specified, the result of SQLTemplate may not be a normal list
	 * of Persistent objects or DataRows, instead it will follow the
	 * {@link SQLResult} rules.
	 * 
	 * @since 3.0
	 */
	public void setResult(SQLResult resultSet) {
		this.result = resultSet;
	}

	/**
	 * @since 3.0
	 */
	public SQLResult getResult() {
		return result;
	}

	/**
	 * Sets statement's fetch size (0 for no default size)
	 * 
	 * @since 3.0
	 */
	public void setStatementFetchSize(int size) {
		metaData.setStatementFetchSize(size);
	}

	/**
	 * @return statement's fetch size
	 * @since 3.0
	 */
	public int getStatementFetchSize() {
		return metaData.getStatementFetchSize();
	}

	/**
	 * Sets query timeout.
	 * @since 4.2
	 */
	public void setQueryTimeout(int queryTimeout) {
		metaData.setQueryTimeout(queryTimeout);
	}

	/**
	 * @return query timeout
	 * @since 4.2
	 */
	public int getQueryTimeout() {
		return metaData.getQueryTimeout();
	}

	/**
	 * Returns a name of the DataNode to use with this SQLTemplate. This
	 * information will be used during query execution if no other routing
	 * information is provided such as entity name or class, etc.
	 * 
	 * @since 4.0
	 */
	public String getDataNodeName() {
		return dataNodeName;
	}

	/**
	 * Sets a name of the DataNode to use with this SQLTemplate. This
	 * information will be used during query execution if no other routing
	 * information is provided such as entity name or class, etc.
	 * 
	 * @since 4.0
	 */
	public void setDataNodeName(String dataNodeName) {
		this.dataNodeName = dataNodeName;
	}

	/**
	 * @return returnGeneratedKeys flag
	 *
	 * @since 4.1
	 */
	public boolean isReturnGeneratedKeys() {
		return returnGeneratedKeys;
	}

	/**
	 * Sets flag to return generated keys.
	 *
	 * @since 4.1
	 */
	public void setReturnGeneratedKeys(boolean returnGeneratedKeys) {
		this.returnGeneratedKeys = returnGeneratedKeys;
	}

	public List<Class<?>> getResultColumnsTypes() {
		return resultColumnsTypes;
	}

	public void setResultColumnsTypes(List<Class<?>> resultColumnsTypes) {
		this.resultColumnsTypes = resultColumnsTypes;
	}

	/**
	 * Sets flag to use scalars.
	 *
	 * @since 4.1
	 */
	public void setUseScalar(boolean useScalar) {
	    this.useScalar = useScalar;
	}

	public boolean isUseScalar() {
		return useScalar;
	}

	/**
	 * @since 4.2
	 */
    public void setResultMapper(Function<?,?> resultMapper) {
		this.metaData.setResultMapper(resultMapper);
    }
}
