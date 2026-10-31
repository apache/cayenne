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
import org.apache.cayenne.QueryResult;
import org.apache.cayenne.UpdateResult;
import org.apache.cayenne.dba.DbAdapter;

import java.util.List;
import java.util.Map;

/**
 * A generic query based on raw SQL and featuring fluent API. While {@link SQLExec} can be used to select data (see
 * {@link #execute(ObjectContext)}), it is normally used for updates, DDL operations, etc.
 *
 * @since 4.0
 * @deprecated in favor of {@link SQLQuery}, that combines selecting and non-selecting SQL in a single
 * query. Update counts are read from the {@link UpdateResult} items of {@link SQLQuery#execute(ObjectContext)}.
 */
@Deprecated(since = "6.0", forRemoval = true)
public class SQLExec extends SQLQuery<DataRow> {

    /**
     * Creates a query executing provided SQL run against default database.
     *
     * @deprecated in favor of {@link SQLQuery#query(String)}
     */
    @Deprecated(since = "6.0", forRemoval = true)
    public static SQLExec query(String sql) {
        return new SQLExec(sql);
    }

    /**
     * Creates a query executing provided SQL that performs routing based on the provided DataMap name.
     *
     * @deprecated in favor of {@link SQLQuery#query(String, String)}
     */
    @Deprecated(since = "6.0", forRemoval = true)
    public static SQLExec query(String dataMapName, String sql) {
        SQLExec query = new SQLExec(sql);
        query.dataMapName = dataMapName;
        return query;
    }

    public SQLExec(String sql) {
        super(sql);
        fetchingDataRows();
    }

    /**
     * @deprecated in favor of {@link #param(String, Object)}
     */
    @Deprecated(since = "6.0", forRemoval = true)
    public SQLExec params(String name, Object value) {
        param(name, value);
        return this;
    }

    /**
     * Executes the query, returning the count of its single update.
     *
     * @throws CayenneRuntimeException if the query produced anything other than a single update result.
     * @deprecated Read the count from the {@link UpdateResult} returned by {@link #execute(ObjectContext)}.
     */
    @Deprecated(since = "6.0", forRemoval = true)
    public int update(ObjectContext context) {
        return singleUpdate(context).count();
    }

    /**
     * Executes the query, returning the counts of its single batch update.
     *
     * @throws CayenneRuntimeException if the query produced anything other than a single update result.
     * @deprecated Read the counts from the {@link UpdateResult} returned by
     * {@link #execute(ObjectContext)}.
     */
    @Deprecated(since = "6.0", forRemoval = true)
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

    // --- covariant overrides, so that chains built from the factory methods still assign to SQLExec ---

    @Override
    public SQLExec append(String sqlChunk) {
        super.append(sqlChunk);
        return this;
    }

    @Override
    public SQLExec dialect(Class<? extends DbAdapter> adapterType, String sql) {
        super.dialect(adapterType, sql);
        return this;
    }

    @Override
    public SQLExec dialectAppend(Class<? extends DbAdapter> adapterType, String sqlChunk) {
        super.dialectAppend(adapterType, sqlChunk);
        return this;
    }

    @Override
    public SQLExec param(String name, Object value) {
        super.param(name, value);
        return this;
    }

    @Override
    public SQLExec params(Map<String, ?> parameters) {
        super.params(parameters);
        return this;
    }

    @Override
    public SQLExec paramsArray(Object... params) {
        super.paramsArray(params);
        return this;
    }

    @Override
    public SQLExec paramsList(List<Object> params) {
        super.paramsList(params);
        return this;
    }

    @Override
    public SQLExec returnGeneratedKeys(boolean returnGeneratedKeys) {
        super.returnGeneratedKeys(returnGeneratedKeys);
        return this;
    }

    @Override
    public SQLExec queryTimeout(int queryTimeout) {
        super.queryTimeout(queryTimeout);
        return this;
    }

    @Override
    public SQLExec dataNode(String dataNodeName) {
        super.dataNode(dataNodeName);
        return this;
    }
}
