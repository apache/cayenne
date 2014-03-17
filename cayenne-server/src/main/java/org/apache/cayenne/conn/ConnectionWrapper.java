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

    @Override
    public void clearWarnings() throws SQLException {
        try {
            connection.clearWarnings();
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    @Override
    public void close() throws SQLException {
        if (null != pooledConnection) {
            pooledConnection.returnConnectionToThePool();
        }
        connection = null;
        pooledConnection = null;
    }

    @Override
    public void commit() throws SQLException {
        try {
            connection.commit();
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    @Override
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

    @Override
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

    @Override
    public boolean getAutoCommit() throws SQLException {
        try {
            return connection.getAutoCommit();
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    @Override
    public String getCatalog() throws SQLException {
        try {
            return connection.getCatalog();
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        try {
            return connection.getMetaData();
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        try {
            return connection.getTransactionIsolation();
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        try {
            return connection.getWarnings();
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    @Override
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

    @Override
    public boolean isReadOnly() throws SQLException {
        try {
            return connection.isReadOnly();
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        try {
            return connection.nativeSQL(sql);
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public void rollback() throws SQLException {
        try {
            connection.rollback();
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    @Override
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

    @Override
    public void setCatalog(String catalog) throws SQLException {
        try {
            connection.setCatalog(catalog);
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        try {
            connection.setReadOnly(readOnly);
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        try {
            connection.setTransactionIsolation(level);
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    @Override
    public Map<String,Class<?>> getTypeMap() throws SQLException {
        try {
            return connection.getTypeMap();
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        try {
            connection.setTypeMap(map);
        }
        catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        throw new java.lang.UnsupportedOperationException(
                "Method setHoldability() not yet implemented.");
    }

    @Override
    public int getHoldability() throws SQLException {
        throw new java.lang.UnsupportedOperationException(
                "Method getHoldability() not yet implemented.");
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        throw new java.lang.UnsupportedOperationException(
                "Method setSavepoint() not yet implemented.");
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        throw new java.lang.UnsupportedOperationException(
                "Method setSavepoint() not yet implemented.");
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        throw new java.lang.UnsupportedOperationException(
                "Method rollback() not yet implemented.");
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        throw new java.lang.UnsupportedOperationException(
                "Method releaseSavepoint() not yet implemented.");
    }

    @Override
    public Statement createStatement(
            int resultSetType,
            int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        throw new java.lang.UnsupportedOperationException(
                "Method createStatement() not yet implemented.");
    }

    @Override
    public PreparedStatement prepareStatement(
            String sql,
            int resultSetType,
            int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        throw new java.lang.UnsupportedOperationException(
                "Method prepareStatement() not yet implemented.");
    }

    @Override
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
    
    @Override
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

    @Override
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

    @Override
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
    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        try {
            return connection.createArrayOf(typeName, elements);
        }
        catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return createArrayOf(typeName, elements);
        }
    }

    /**
     * @since 3.0
     */
    @Override
    public Blob createBlob() throws SQLException {
        try {
            return connection.createBlob();
        } catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return createBlob();
        }
    }

    /**
     * @since 3.0
     */
    @Override
    public Clob createClob() throws SQLException {
        try {
            return connection.createClob();
        } catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return createClob();
        }
    }

    /**
     * @since 3.0
     */
    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        try {
            return connection.createStruct(typeName, attributes);
        } catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return createStruct(typeName, attributes);
        }
    }

    /**
     * @since 3.0
     */
    @Override
    public Properties getClientInfo() throws SQLException {
        try {
            return connection.getClientInfo();
        } catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return getClientInfo();
        }
    }

    /**
     * @since 3.0
     */
    @Override
    public String getClientInfo(String name) throws SQLException {
        try {
            return connection.getClientInfo(name);
        } catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return getClientInfo(name);
        }
    }

    /**
     * @since 3.0
     */
    @Override
    public boolean isValid(int timeout) throws SQLException {
        try {
            return connection.isValid(timeout);
        } catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return isValid(timeout);
        }
    }

    /**
     * @since 3.0
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        // TODO: we can implement that now.
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.0
     */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        // TODO: we can implement that now.
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.0
     */
    @Override
    public NClob createNClob() throws SQLException {
        try {
            return connection.createNClob();
        } catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return createNClob();
        }
    }

    /**
     * @since 3.0
     */
    @Override
    public SQLXML createSQLXML() throws SQLException {
        try {
            return connection.createSQLXML();
        } catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return createSQLXML();
        }
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        // TODO: we can implement that now.
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        // TODO: we can implement that now.
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
