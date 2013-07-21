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

package org.apache.cayenne.access;

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
 * A wrapper of a JDBC connection that is attached to a transaction. The behavior of this
 * object to delegate all method calls to the underlying connection, except for the
 * 'close' method that is implemented as noop in hope that a transaction originator will
 * close the underlying Connection object.
 * 
 * @since 1.2
 */
class TransactionConnectionDecorator implements Connection {

    Connection connection;

    TransactionConnectionDecorator(Connection connection) {
        this.connection = connection;
    }

    // the only method that is NOT delegated...
    public void close() throws SQLException {
        // noop
    }

    public void clearWarnings() throws SQLException {
        connection.clearWarnings();
    }

    public void commit() throws SQLException {
        connection.commit();
    }

    public Statement createStatement() throws SQLException {
        return connection.createStatement();
    }

    public Statement createStatement(
            int resultSetType,
            int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        return connection.createStatement(
                resultSetType,
                resultSetConcurrency,
                resultSetHoldability);
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException {
        return connection.createStatement(resultSetType, resultSetConcurrency);
    }

    public boolean getAutoCommit() throws SQLException {
        return connection.getAutoCommit();
    }

    public String getCatalog() throws SQLException {
        return connection.getCatalog();
    }

    public int getHoldability() throws SQLException {
        return connection.getHoldability();
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return connection.getMetaData();
    }

    public int getTransactionIsolation() throws SQLException {
        return connection.getTransactionIsolation();
    }

    public Map<String,Class<?>> getTypeMap() throws SQLException {
        return connection.getTypeMap();
    }

    public SQLWarning getWarnings() throws SQLException {
        return connection.getWarnings();
    }

    public boolean isClosed() throws SQLException {
        return connection.isClosed();
    }

    public boolean isReadOnly() throws SQLException {
        return connection.isReadOnly();
    }

    public String nativeSQL(String sql) throws SQLException {
        return connection.nativeSQL(sql);
    }

    public CallableStatement prepareCall(
            String sql,
            int resultSetType,
            int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        return connection.prepareCall(
                sql,
                resultSetType,
                resultSetConcurrency,
                resultSetHoldability);
    }

    public CallableStatement prepareCall(
            String sql,
            int resultSetType,
            int resultSetConcurrency) throws SQLException {
        return connection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        return connection.prepareCall(sql);
    }

    public PreparedStatement prepareStatement(
            String sql,
            int resultSetType,
            int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        return connection.prepareStatement(
                sql,
                resultSetType,
                resultSetConcurrency,
                resultSetHoldability);
    }

    public PreparedStatement prepareStatement(
            String sql,
            int resultSetType,
            int resultSetConcurrency) throws SQLException {
        return connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException {
        return connection.prepareStatement(sql, autoGeneratedKeys);
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
            throws SQLException {
        return connection.prepareStatement(sql, columnIndexes);
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames)
            throws SQLException {
        return connection.prepareStatement(sql, columnNames);
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        connection.releaseSavepoint(savepoint);
    }

    public void rollback() throws SQLException {
        connection.rollback();
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        connection.rollback(savepoint);
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        connection.setAutoCommit(autoCommit);
    }

    public void setCatalog(String catalog) throws SQLException {
        connection.setCatalog(catalog);
    }

    public void setHoldability(int holdability) throws SQLException {
        connection.setHoldability(holdability);
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        connection.setReadOnly(readOnly);
    }

    public Savepoint setSavepoint() throws SQLException {
        return connection.setSavepoint();
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        return connection.setSavepoint(name);
    }

    public void setTransactionIsolation(int level) throws SQLException {
        connection.setTransactionIsolation(level);
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        connection.setTypeMap(map);
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
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
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
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
    public String getClientInfo(String name) throws SQLException {
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
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public boolean isValid(int timeout) throws SQLException {
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
