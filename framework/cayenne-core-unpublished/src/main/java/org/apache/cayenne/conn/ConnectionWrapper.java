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

package org.apache.cayenne.conn;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * ConnectionWrapper is a <code>java.sql.Connection</code> implementation that wraps
 * another Connection, delegating method calls to this connection. It works in conjunction
 * with PooledConnectionImpl, to generate pool events, provide limited automated
 * reconnection functionality, etc.
 * 
 */
public class ConnectionWrapper implements Connection {

    private Connection connection;
    private PooledConnectionImpl pooledConnection;
    private long lastReconnected;
    private int reconnectCount;

    /**
     * Fixes Sybase problems with autocommit. Used idea from Jonas
     * org.objectweb.jonas.jdbc_xa.ConnectionImpl (http://www.objectweb.org/jonas/).
     * <p>
     * If problem is not the one that can be fixed by this patch, original exception is
     * rethrown. If exception occurs when fixing the problem, new exception is thrown.
     * </p>
     */
    static void sybaseAutoCommitPatch(Connection c, SQLException e, boolean autoCommit)
            throws SQLException {

        String s = e.getMessage().toLowerCase();
        if (s.contains("set chained command not allowed")) {
            c.commit();
            c.setAutoCommit(autoCommit); // Shouldn't fail now.
        }
        else {
            throw e;
        }
    }

    /**
     * Creates new ConnectionWrapper
     */
    public ConnectionWrapper(Connection connection, PooledConnectionImpl pooledConnection) {
        this.connection = connection;
        this.pooledConnection = pooledConnection;
    }

    protected void reconnect(SQLException exception) throws SQLException {

        // if there was a relatively recent reconnect, just rethrow an error
        // and retire itself. THIS WILL PREVENT RECONNECT LOOPS
        if (reconnectCount > 0 && System.currentTimeMillis() - lastReconnected < 60000) {

            retire(exception);
            throw exception;
        }

        pooledConnection.reconnect();

        // Pooled connection will wrap returned connection into
        // another ConnectionWrapper.... lets get the real connection
        // underneath...
        Connection connection = pooledConnection.getConnection();
        if (connection instanceof ConnectionWrapper) {
            this.connection = ((ConnectionWrapper) connection).connection;
        }
        else {
            this.connection = connection;
        }

        lastReconnected = System.currentTimeMillis();
        reconnectCount++;
    }

    protected void retire(SQLException exception) {
        // notify all the listeners....
        pooledConnection.connectionErrorNotification(exception);
    }

    public void clearWarnings() throws SQLException {
        try {
            connection.clearWarnings();
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public void close() throws SQLException {
        if (null != pooledConnection) {
            pooledConnection.returnConnectionToThePool();
        }
        connection = null;
        pooledConnection = null;
    }

    public void commit() throws SQLException {
        try {
            connection.commit();
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public Statement createStatement() throws SQLException {
        try {
            return connection.createStatement();
        }
        catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return createStatement();
        }
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException {
        try {
            return connection.createStatement(resultSetType, resultSetConcurrency);
        }
        catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return createStatement(resultSetType, resultSetConcurrency);
        }
    }

    public boolean getAutoCommit() throws SQLException {
        try {
            return connection.getAutoCommit();
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public String getCatalog() throws SQLException {
        try {
            return connection.getCatalog();
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        try {
            return connection.getMetaData();
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public int getTransactionIsolation() throws SQLException {
        try {
            return connection.getTransactionIsolation();
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public SQLWarning getWarnings() throws SQLException {
        try {
            return connection.getWarnings();
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public boolean isClosed() throws SQLException {
        if (connection != null) {
            try {
                return connection.isClosed();
            }
            catch (SQLException sqlEx) {
                retire(sqlEx);
                throw sqlEx;
            }
        }
        else
            return true;
    }

    public boolean isReadOnly() throws SQLException {
        try {
            return connection.isReadOnly();
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public String nativeSQL(String sql) throws SQLException {
        try {
            return connection.nativeSQL(sql);
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        try {
            return connection.prepareCall(sql);
        }
        catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return prepareCall(sql);
        }
    }

    public CallableStatement prepareCall(
            String sql,
            int resultSetType,
            int resultSetConcurrency) throws SQLException {
        try {
            return connection.prepareCall(sql, resultSetType, resultSetConcurrency);
        }
        catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return prepareCall(sql, resultSetType, resultSetConcurrency);
        }
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        try {
            return connection.prepareStatement(sql);
        }
        catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return prepareStatement(sql);
        }
    }

    public PreparedStatement prepareStatement(
            String sql,
            int resultSetType,
            int resultSetConcurrency) throws SQLException {
        try {
            return connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }
        catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return prepareStatement(sql, resultSetType, resultSetConcurrency);
        }
    }

    public void rollback() throws SQLException {
        try {
            connection.rollback();
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        try {
            connection.setAutoCommit(autoCommit);
        }
        catch (SQLException sqlEx) {

            try {
                // apply Sybase patch
                sybaseAutoCommitPatch(connection, sqlEx, autoCommit);
            }
            catch (SQLException patchEx) {
                retire(sqlEx);
                throw sqlEx;
            }
        }
    }

    public void setCatalog(String catalog) throws SQLException {
        try {
            connection.setCatalog(catalog);
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        try {
            connection.setReadOnly(readOnly);
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public void setTransactionIsolation(int level) throws SQLException {
        try {
            connection.setTransactionIsolation(level);
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public Map<String,Class<?>> getTypeMap() throws SQLException {
        try {
            return connection.getTypeMap();
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        try {
            connection.setTypeMap(map);
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public void setHoldability(int holdability) throws SQLException {
        throw new java.lang.UnsupportedOperationException(
                "Method setHoldability() not yet implemented.");
    }

    public int getHoldability() throws SQLException {
        throw new java.lang.UnsupportedOperationException(
                "Method getHoldability() not yet implemented.");
    }

    public Savepoint setSavepoint() throws SQLException {
        throw new java.lang.UnsupportedOperationException(
                "Method setSavepoint() not yet implemented.");
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        throw new java.lang.UnsupportedOperationException(
                "Method setSavepoint() not yet implemented.");
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        throw new java.lang.UnsupportedOperationException(
                "Method rollback() not yet implemented.");
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        throw new java.lang.UnsupportedOperationException(
                "Method releaseSavepoint() not yet implemented.");
    }

    public Statement createStatement(
            int resultSetType,
            int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        throw new java.lang.UnsupportedOperationException(
                "Method createStatement() not yet implemented.");
    }

    public PreparedStatement prepareStatement(
            String sql,
            int resultSetType,
            int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        throw new java.lang.UnsupportedOperationException(
                "Method prepareStatement() not yet implemented.");
    }

    public CallableStatement prepareCall(
            String sql,
            int resultSetType,
            int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        try {
            return connection.prepareCall(
                    sql,
                    resultSetType,
                    resultSetConcurrency,
                    resultSetHoldability);
        }
        catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return prepareCall(
                    sql,
                    resultSetType,
                    resultSetConcurrency,
                    resultSetHoldability);
        }
    }
    
    

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException {

        try {
            return connection.prepareStatement(sql, autoGeneratedKeys);
        }
        catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return prepareStatement(sql, autoGeneratedKeys);
        }
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
            throws SQLException {
        try {
            return connection.prepareStatement(sql, columnIndexes);
        }
        catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return prepareStatement(sql, columnIndexes);
        }
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames)
            throws SQLException {
        try {
            return connection.prepareStatement(sql, columnNames);
        }
        catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return prepareStatement(sql, columnNames);
        }
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public Array createArrayOf(String arg0, Object[] arg1) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public Blob createBlob() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public Clob createClob() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public Struct createStruct(String arg0, Object[] arg1) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public Properties getClientInfo() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public String getClientInfo(String arg0) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public boolean isValid(int arg0) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public NClob createNClob() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public SQLXML createSQLXML() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.1
     *
     * JDBC 4.1 compatibility under Java 1.5
     */
    public void setSchema(String schema) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.1
     *
     * JDBC 4.1 compatibility under Java 1.5
     */
    public String getSchema() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.1
     *
     * JDBC 4.1 compatibility under Java 1.5
     */
    public void abort(Executor executor) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.1
     *
     * JDBC 4.1 compatibility under Java 1.5
     */
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.1
     *
     * JDBC 4.1 compatibility under Java 1.5
     */
    public int getNetworkTimeout() throws SQLException {
        throw new UnsupportedOperationException();
    }
}
