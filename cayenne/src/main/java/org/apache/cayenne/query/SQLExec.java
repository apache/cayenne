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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.QueryResult;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.util.QueryResultBuilder;

/**
 * A generic query based on raw SQL and featuring fluent API. While
 * {@link SQLExec} can be used to select data (see {@link #execute(ObjectContext)}
 * ), it is normally used for updates, DDL operations, etc.
 *
 * @since 4.0
 */
public class SQLExec extends IndirectQuery {

    private static final long serialVersionUID = -6533566707148045615L;

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
    protected StringBuilder sqlBuffer;
    protected Map<String, Object> params;
    protected List<Object> positionalParams;
    protected boolean returnGeneratedKeys;
    protected int queryTimeout;

    public SQLExec(String sql) {
        this.sqlBuffer = sql != null ? new StringBuilder(sql) : new StringBuilder();
    }

    public String getSql() {
        String sql = sqlBuffer.toString();
        return sql.length() > 0 ? sql : null;
    }

    /**
     * Appends a piece of SQL to the previously stored SQL template.
     */
    public SQLExec append(String sqlChunk) {
        sqlBuffer.append(sqlChunk);
        this.replacementQuery = null;
        return this;
    }

    public SQLExec params(String name, Object value) {
        params(Collections.singletonMap(name, value));
        return this;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
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
        return params != null ? params : Collections.<String, Object>emptyMap();
    }

    /**
     * Returns a potentially immutable list of positional parameters that will
     * be bound to SQL.
     */
    public List<Object> getPositionalParams() {
        return positionalParams != null ? positionalParams : Collections.emptyList();
    }

    public QueryResult execute(ObjectContext context) {

        // TODO: switch ObjectContext to QueryResult instead of QueryResponse
        // and create its own 'exec' method
        QueryResponse response = context.performGenericQuery(this);

        QueryResultBuilder builder = QueryResultBuilder.builder(response.size());
        for (response.reset(); response.next(); ) {

            if (response.isList()) {
                builder.addSelectResult(response.currentList());
            } else {
                builder.addBatchUpdateResult(response.currentUpdateCount());
            }
        }

        return builder.build();
    }

    public int update(ObjectContext context) {

        // TODO: create a corresponding method in ObjectContext
        QueryResult results = execute(context);

        if (results.size() != 1) {
            throw new CayenneRuntimeException("Expected a single update result. Got a total of %d", results.size());
        }

        return results.firstUpdateCount();
    }

    public int[] updateBatch(ObjectContext context) {
        // TODO: create a corresponding method in ObjectContext
        QueryResult results = execute(context);

        if (results.size() != 1) {
            throw new CayenneRuntimeException("Expected a single update result. Got a total of %d", results.size());
        }

        return results.firstBatchUpdateCount();
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
     * Generated keys could be read via {@link QueryResponse#currentList()} method
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

    @Override
    protected Query createReplacementQuery(EntityResolver resolver) {

        Object root;

        if (dataMapName != null) {
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
        template.setRoot(root);
        template.setDefaultTemplate(getSql());
        template.setFetchingDataRows(true); // in case result set will be returned
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
