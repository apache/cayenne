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
package org.apache.cayenne.unit.jdbc;

import java.lang.reflect.Proxy;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * Minimal JDBC {@link Connection} stub for tests. Tracks commits/rollbacks and
 * returns {@link TestPreparedStatement}s whose {@code executeUpdate()} returns
 * a value pre-configured via {@link #prepareUpdateCount(String, int)}.
 * <p>
 * The calls that change the connection state, and the calls to the statements of this connection, are recorded in
 * order in {@link #getCalls()}, e.g. "setAutoCommit(false)", "setFetchSize(10)", "executeQuery", "commit".
 */
public class TestConnection implements Connection {

    private final List<UpdateCountRule> rules = new ArrayList<>();
    private final List<String> calls = new ArrayList<>();
    private final List<TestPreparedStatement> preparedStatements = new ArrayList<>();
    private final Deque<SQLException> prepareFailures = new ArrayDeque<>();
    private RuntimeException queryFailure;
    private String databaseProductName = "";
    private final Map<String, ResultSet> metaDataResults = new HashMap<>();
    private int numberCommits;
    private int numberRollbacks;
    private boolean autoCommit = true;
    private boolean closed;

    /**
     * Registers an updateCount to be returned by {@link PreparedStatement#executeUpdate()}
     * when the prepared SQL contains the given fragment (case-insensitive).
     */
    public void prepareUpdateCount(String sqlFragment, int updateCount) {
        rules.add(new UpdateCountRule(sqlFragment, updateCount));
    }

    /**
     * Makes the next calls to "prepareStatement" throw the given exceptions, one per call.
     */
    public TestConnection failPrepareStatement(SQLException... failures) {
        prepareFailures.addAll(List.of(failures));
        return this;
    }

    /**
     * Makes {@link PreparedStatement#executeQuery()} of the statements of this connection throw the given exception.
     */
    public TestConnection failQuery(RuntimeException failure) {
        this.queryFailure = failure;
        return this;
    }

    /**
     * Sets the product name reported by the {@link DatabaseMetaData} of this connection.
     */
    public TestConnection databaseProductName(String name) {
        this.databaseProductName = name;
        return this;
    }

    /**
     * Sets the result returned by the {@link DatabaseMetaData} method of this name, e.g. "getTables". Other methods
     * returning a ResultSet return an empty one.
     */
    public TestConnection metaDataResult(String methodName, ResultSet result) {
        metaDataResults.put(methodName, result);
        return this;
    }

    /**
     * Sets the initial auto-commit state, without recording a call.
     */
    public TestConnection autoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
        return this;
    }

    public int getNumberCommits() {
        return numberCommits;
    }

    public int getNumberRollbacks() {
        return numberRollbacks;
    }

    /**
     * Returns the recorded calls to this connection and its statements, in order.
     */
    public List<String> getCalls() {
        return calls;
    }

    /**
     * Returns the statements prepared by this connection, in order.
     */
    public List<TestPreparedStatement> getPreparedStatements() {
        return preparedStatements;
    }

    void record(String call) {
        calls.add(call);
    }

    RuntimeException getQueryFailure() {
        return queryFailure;
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        SQLException failure = prepareFailures.poll();
        if (failure != null) {
            throw failure;
        }

        TestPreparedStatement statement = new TestPreparedStatement(this, sql, updateCountFor(sql));
        preparedStatements.add(statement);
        return statement;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return prepareStatement(sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return prepareStatement(sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return prepareStatement(sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        return prepareStatement(sql);
    }

    @Override
    public PreparedStatement prepareStatement(
            String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return prepareStatement(sql);
    }

    @Override
    public Statement createStatement() {
        return new TestStatement(this);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) {
        return createStatement();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
        return createStatement();
    }

    @Override
    public void commit() {
        record("commit");
        numberCommits++;
    }

    @Override
    public void rollback() {
        record("rollback");
        numberRollbacks++;
    }

    @Override
    public void rollback(Savepoint savepoint) {
        record("rollback");
        numberRollbacks++;
    }

    @Override
    public boolean getAutoCommit() {
        return autoCommit;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) {
        record("setAutoCommit(" + autoCommit + ")");
        this.autoCommit = autoCommit;
    }

    @Override
    public void close() {
        record("close");
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    private int updateCountFor(String sql) {
        String lower = sql == null ? "" : sql.toLowerCase();
        for (UpdateCountRule rule : rules) {
            if (lower.contains(rule.fragment.toLowerCase())) {
                return rule.updateCount;
            }
        }
        return 0;
    }

    private record UpdateCountRule(String fragment, int updateCount) {}

    // --- everything below is unused by tests ---

    private static UnsupportedOperationException uoe() {
        return new UnsupportedOperationException("not implemented in TestConnection");
    }

    @Override public CallableStatement prepareCall(String sql) { throw uoe(); }
    @Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) { throw uoe(); }
    @Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) { throw uoe(); }
    @Override public String nativeSQL(String sql) { return sql; }
    @Override public DatabaseMetaData getMetaData() { return stubMetaData(); }

    private DatabaseMetaData stubMetaData() {
        return (DatabaseMetaData) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{DatabaseMetaData.class},
                (proxy, method, args) -> {
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) return false;
                    if (rt == int.class || rt == short.class || rt == byte.class) return 0;
                    if (rt == long.class) return 0L;
                    if (rt == float.class) return 0f;
                    if (rt == double.class) return 0d;
                    if (rt == ResultSet.class) {
                        return metaDataResults.getOrDefault(method.getName(), new TestResultSet(""));
                    }
                    if (method.getName().equals("getDatabaseProductName")) return databaseProductName;
                    if (rt == String.class) return "";
                    if (rt == Connection.class) return this;
                    return null;
                });
    }
    @Override public void setReadOnly(boolean readOnly) {}
    @Override public boolean isReadOnly() { return false; }
    @Override public void setCatalog(String catalog) {}
    @Override public String getCatalog() { return null; }
    @Override public void setTransactionIsolation(int level) {}
    @Override public int getTransactionIsolation() { return TRANSACTION_NONE; }
    @Override public SQLWarning getWarnings() { return null; }
    @Override public void clearWarnings() {}
    @Override public Map<String, Class<?>> getTypeMap() { throw uoe(); }
    @Override public void setTypeMap(Map<String, Class<?>> map) {}
    @Override public void setHoldability(int holdability) {}
    @Override public int getHoldability() { return 0; }
    @Override public Savepoint setSavepoint() { throw uoe(); }
    @Override public Savepoint setSavepoint(String name) { throw uoe(); }
    @Override public void releaseSavepoint(Savepoint savepoint) {}
    @Override public Clob createClob() { throw uoe(); }
    @Override public Blob createBlob() { throw uoe(); }
    @Override public NClob createNClob() { throw uoe(); }
    @Override public SQLXML createSQLXML() { throw uoe(); }
    @Override public boolean isValid(int timeout) { return !closed; }
    @Override public void setClientInfo(String name, String value) throws SQLClientInfoException {}
    @Override public void setClientInfo(Properties properties) throws SQLClientInfoException {}
    @Override public String getClientInfo(String name) { return null; }
    @Override public Properties getClientInfo() { return new Properties(); }
    @Override public Array createArrayOf(String typeName, Object[] elements) { throw uoe(); }
    @Override public Struct createStruct(String typeName, Object[] attributes) { throw uoe(); }
    @Override public void setSchema(String schema) {}
    @Override public String getSchema() { return null; }
    @Override public void abort(Executor executor) {}
    @Override public void setNetworkTimeout(Executor executor, int milliseconds) {}
    @Override public int getNetworkTimeout() { return 0; }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("not supported"); }
    @Override public boolean isWrapperFor(Class<?> iface) { return false; }
}
