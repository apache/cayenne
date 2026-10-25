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
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResult;
import org.apache.cayenne.SelectResult;
import org.apache.cayenne.UpdateResult;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.EntityResolver;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A generic query based on raw SQL and featuring fluent API. While
 * {@link SQLExec} can be used to select data (see {@link #execute(ObjectContext)}
 * ), it is normally used for updates, DDL operations, etc.
 *
 * @since 4.0
 */
public class SQLExec extends IndirectQuery {

    /**
     * Creates a query executing provided SQL run against default database.
     */
    public static SQLExec query(String sql) {
        return new SQLExec(sql);
    }

    /**
     * Creates a query executing provided SQL that performs routing based on the
     * provided DataMap name.
     */
    public static SQLExec query(String dataMapName, String sql) {
        SQLExec query = new SQLExec(sql);
        query.dataMapName = dataMapName;
        return query;
    }

    protected String dataMapName;
    protected String dataNodeName;
    protected StringBuilder sql;
    protected Map<String, StringBuilder> sqlDialects;
    protected Map<String, Object> params;
    protected List<Object> positionalParams;
    protected boolean returnGeneratedKeys;
    protected int queryTimeout;

    public SQLExec(String sql) {
        this.sql = sql != null ? new StringBuilder(sql) : new StringBuilder();
    }

    public String getSql() {
        String sql = this.sql.toString();
        return sql.length() > 0 ? sql : null;
    }

    /**
     * Appends a piece of SQL to the previously stored default SQL template. Dialects registered with
     * {@link #dialect(Class, String)} stay unchanged, use {@link #dialectAppend(Class, String)} to extend those.
     */
    public SQLExec append(String sqlChunk) {
        sql.append(sqlChunk);
        this.replacementQuery = null;
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
     * @since 6.0
     */
    public SQLExec dialect(Class<? extends DbAdapter> adapterType, String sql) {
        if (sqlDialects == null) {
            sqlDialects = new HashMap<>();
        }
        sqlDialects.put(adapterType.getName(), new StringBuilder(sql));
        this.replacementQuery = null;
        return this;
    }

    /**
     * Appends a piece of SQL to the dialect registered for the given adapter with {@link #dialect(Class, String)}.
     * If no dialect was registered yet, the chunk becomes the start of a new one.
     *
     * @since 6.0
     */
    public SQLExec dialectAppend(Class<? extends DbAdapter> adapterType, String sqlChunk) {
        if (sqlDialects == null) {
            sqlDialects = new HashMap<>();
        }
        sqlDialects.computeIfAbsent(adapterType.getName(), k -> new StringBuilder()).append(sqlChunk);
        this.replacementQuery = null;
        return this;
    }

    public SQLExec params(String name, Object value) {
        params(Collections.singletonMap(name, value));
        return this;
    }

    public SQLExec params(Map<String, ?> parameters) {

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
    public SQLExec paramsArray(Object... params) {
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
    public SQLExec paramsList(List<Object> params) {
        this.positionalParams = params;
        this.replacementQuery = null;

        // since named parameters are specified, resetting positional parameters
        this.params = null;
        return this;
    }

    /**
     * Returns a potentially immutable map of named parameters that will be
     * bound to SQL.
     */
    public Map<String, Object> getParams() {
        return params != null ? params : Collections.emptyMap();
    }

    /**
     * Returns a potentially immutable list of positional parameters that will
     * be bound to SQL.
     */
    public List<Object> getPositionalParams() {
        return positionalParams != null ? positionalParams : Collections.emptyList();
    }

    /**
     * Executes the query, returning all of its result sets and update counts in the order they were produced.
     */
    public List<QueryResult> execute(ObjectContext context) {
        return context.execute(this);
    }

    public int update(ObjectContext context) {
        return singleUpdate(context).count();
    }

    public int[] updateBatch(ObjectContext context) {
        return singleUpdate(context).counts();
    }

    private UpdateResult singleUpdate(ObjectContext context) {
        List<QueryResult> results = execute(context);
        if (results.size() != 1) {
            throw new CayenneRuntimeException("Expected a single update result. Got a total of %d", results.size());
        }

        if (results.getFirst() instanceof UpdateResult update) {
            return update;
        }

        throw new CayenneRuntimeException("Expected an update result. Got %s", results.getFirst());
    }

    /**
     * @return returnGeneratedKeys flag value
     *
     * @since 4.1
     */
    public boolean isReturnGeneratedKeys() {
        return returnGeneratedKeys;
    }

    /**
     * Flag indicating that generated keys should be returned by this query execution.
     * Generated keys are returned as a {@link SelectResult} item of {@link #execute(ObjectContext)}
     * result.
     *
     * @param returnGeneratedKeys flag value
     * @see java.sql.Statement#RETURN_GENERATED_KEYS
     * @since 4.1
     */
    public SQLExec returnGeneratedKeys(boolean returnGeneratedKeys) {
        this.returnGeneratedKeys = returnGeneratedKeys;
        return this;
    }

    /**
     * @since 4.2
     */
    public SQLExec queryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
        return this;
    }

    /**
     * Routes the query to the DataNode with the given name. This allows to run SQL in a project without any mapping,
     * as the query does not need a DataMap to find its database. The node name is only used by queries created
     * without a DataMap name (see {@link #query(String)}). Without it such queries are routed to the default DataNode.
     *
     * @param dataNodeName the name of the DataNode to run the query against
     * @since 6.0
     */
    public SQLExec dataNode(String dataNodeName) {
        this.dataNodeName = dataNodeName;
        this.replacementQuery = null;
        return this;
    }

    @Override
    protected Query createReplacementQuery(EntityResolver resolver) {

        // a data row query in case a result set is returned; without a DataMap it is routed via the named or the
        // default node
        SQLTemplate<?> template = dataMapName != null
                ? SQLTemplate.dataRowQuery(dataMapName, getSql())
                : SQLTemplate.dataRowQuery(getSql());
        template.setDataNodeName(dataNodeName);
        if (sqlDialects != null) {
            sqlDialects.forEach((adapter, sql) -> template.setTemplate(adapter, sql.toString()));
        }

        template.setReturnGeneratedKeys(returnGeneratedKeys);
        template.setQueryTimeout(queryTimeout);

        if (positionalParams != null) {
            template.setParamsList(positionalParams);
        } else {
            template.setParams(params);
        }

        return template;

    }
}
