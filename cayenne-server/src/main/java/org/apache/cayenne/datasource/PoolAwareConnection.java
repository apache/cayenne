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
package org.apache.cayenne.datasource;

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
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * A {@link Connection} wrapper that interacts with the
 * {@link UnmanagedPoolingDataSource}, allowing to recycle connections and track
 * failures.
 * 
 * @since 4.0
 */
public class PoolAwareConnection implements Connection {

	private UnmanagedPoolingDataSource parent;
	private Connection connection;
	private String validationQuery;

	public PoolAwareConnection(UnmanagedPoolingDataSource parent, Connection connection, String validationQuery) {
		this.parent = parent;
		this.connection = connection;
		this.validationQuery = validationQuery;
	}

	Connection getConnection() {
		return connection;
	}

	boolean validate() {

		if (validationQuery == null) {
			return true;
		}

		try {

			try (Statement statement = connection.createStatement();) {

				try (ResultSet rs = statement.executeQuery(validationQuery);) {

					if (!rs.next()) {
						throw new SQLException("Connection validation failed, no result for query: " + validationQuery);
					}
				}
			}
		} catch (SQLException e) {
			return false;
		}

		return true;
	}

	void recover(SQLException reconnectCause) throws SQLException {

		try {
			connection.close();
		} catch (SQLException e) {
			// ignore exception, since connection is expected to be in a bad
			// state
		}

		// TODO: autocommit, tx isolation, and other connection settings may
		// change when resetting connection and need to be restored...
		try {
			connection = parent.createUnwrapped();
		} catch (SQLException e) {
			parent.retire(this);
			throw reconnectCause;
		}
	}

	@Override
	public void clearWarnings() throws SQLException {
		try {
			connection.clearWarnings();
		} catch (SQLException sqlEx) {
			parent.retire(this);
			throw sqlEx;
		}
	}

	@Override
	public void close() throws SQLException {
		parent.reclaim(this);
	}

	@Override
	public void commit() throws SQLException {
		try {
			connection.commit();
		} catch (SQLException sqlEx) {
			parent.retire(this);
			throw sqlEx;
		}
	}

	@Override
	public Statement createStatement() throws SQLException {
		try {
			return connection.createStatement();
		} catch (SQLException sqlEx) {
			recover(sqlEx);
			return connection.createStatement();
		}
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		try {
			return connection.createStatement(resultSetType, resultSetConcurrency);
		} catch (SQLException e) {
			recover(e);
			return connection.createStatement(resultSetType, resultSetConcurrency);
		}
	}

	@Override
	public boolean getAutoCommit() throws SQLException {
		try {
			return connection.getAutoCommit();
		} catch (SQLException sqlEx) {
			parent.retire(this);
			throw sqlEx;
		}
	}

	@Override
	public String getCatalog() throws SQLException {
		try {
			return connection.getCatalog();
		} catch (SQLException sqlEx) {
			parent.retire(this);
			throw sqlEx;
		}
	}

	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		try {
			return connection.getMetaData();
		} catch (SQLException sqlEx) {
			parent.retire(this);
			throw sqlEx;
		}
	}

	@Override
	public int getTransactionIsolation() throws SQLException {
		try {
			return connection.getTransactionIsolation();
		} catch (SQLException sqlEx) {
			parent.retire(this);
			throw sqlEx;
		}
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		try {
			return connection.getWarnings();
		} catch (SQLException sqlEx) {
			parent.retire(this);
			throw sqlEx;
		}
	}

	@Override
	public boolean isClosed() throws SQLException {

		try {
			return connection.isClosed();
		} catch (SQLException sqlEx) {
			parent.retire(this);
			throw sqlEx;
		}
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		try {
			return connection.isReadOnly();
		} catch (SQLException sqlEx) {
			parent.retire(this);
			throw sqlEx;
		}
	}

	@Override
	public String nativeSQL(String sql) throws SQLException {
		try {
			return connection.nativeSQL(sql);
		} catch (SQLException sqlEx) {
			parent.retire(this);
			throw sqlEx;
		}
	}

	@Override
	public CallableStatement prepareCall(String sql) throws SQLException {
		try {
			return connection.prepareCall(sql);
		} catch (SQLException sqlEx) {
			recover(sqlEx);
			return connection.prepareCall(sql);
		}
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		try {
			return connection.prepareCall(sql, resultSetType, resultSetConcurrency);
		} catch (SQLException sqlEx) {

			recover(sqlEx);
			return connection.prepareCall(sql, resultSetType, resultSetConcurrency);
		}
	}

	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		try {
			return connection.prepareStatement(sql);
		} catch (SQLException sqlEx) {
			recover(sqlEx);
			return connection.prepareStatement(sql);
		}
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
		try {
			return connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
		} catch (SQLException sqlEx) {

			recover(sqlEx);
			return connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
		}
	}

	@Override
	public void rollback() throws SQLException {
		try {
			connection.rollback();
		} catch (SQLException sqlEx) {
			parent.retire(this);
			throw sqlEx;
		}
	}

	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		try {
			connection.setAutoCommit(autoCommit);
		} catch (SQLException sqlEx) {

			try {
				UnmanagedPoolingDataSource.sybaseAutoCommitPatch(connection, sqlEx, autoCommit);
			} catch (SQLException patchEx) {
				parent.retire(this);
				throw sqlEx;
			}
		}
	}

	@Override
	public void setCatalog(String catalog) throws SQLException {
		try {
			connection.setCatalog(catalog);
		} catch (SQLException sqlEx) {
			parent.retire(this);
			throw sqlEx;
		}
	}

	@Override
	public void setReadOnly(boolean readOnly) throws SQLException {
		try {
			connection.setReadOnly(readOnly);
		} catch (SQLException sqlEx) {
			parent.retire(this);
			throw sqlEx;
		}
	}

	@Override
	public void setTransactionIsolation(int level) throws SQLException {
		try {
			connection.setTransactionIsolation(level);
		} catch (SQLException sqlEx) {
			parent.retire(this);
			throw sqlEx;
		}
	}

	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		try {
			return connection.getTypeMap();
		} catch (SQLException sqlEx) {
			parent.retire(this);
			throw sqlEx;
		}
	}

	@Override
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		try {
			connection.setTypeMap(map);
		} catch (SQLException sqlEx) {
			parent.retire(this);
			throw sqlEx;
		}
	}

	@Override
	public void setHoldability(int holdability) throws SQLException {
		throw new java.lang.UnsupportedOperationException("Method setHoldability() not yet implemented.");
	}

	@Override
	public int getHoldability() throws SQLException {
		throw new java.lang.UnsupportedOperationException("Method getHoldability() not yet implemented.");
	}

	@Override
	public Savepoint setSavepoint() throws SQLException {
		throw new java.lang.UnsupportedOperationException("Method setSavepoint() not yet implemented.");
	}

	@Override
	public Savepoint setSavepoint(String name) throws SQLException {
		throw new java.lang.UnsupportedOperationException("Method setSavepoint() not yet implemented.");
	}

	@Override
	public void rollback(Savepoint savepoint) throws SQLException {
		throw new java.lang.UnsupportedOperationException("Method rollback() not yet implemented.");
	}

	@Override
	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		throw new java.lang.UnsupportedOperationException("Method releaseSavepoint() not yet implemented.");
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		throw new java.lang.UnsupportedOperationException("Method createStatement() not yet implemented.");
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		throw new java.lang.UnsupportedOperationException("Method prepareStatement() not yet implemented.");
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		try {
			return connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		} catch (SQLException e) {

			recover(e);
			return connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		}
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {

		try {
			return connection.prepareStatement(sql, autoGeneratedKeys);
		} catch (SQLException e) {

			recover(e);
			return connection.prepareStatement(sql, autoGeneratedKeys);
		}
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		try {
			return connection.prepareStatement(sql, columnIndexes);
		} catch (SQLException e) {

			recover(e);
			return connection.prepareStatement(sql, columnIndexes);
		}
	}

	@Override
	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		try {
			return connection.prepareStatement(sql, columnNames);
		} catch (SQLException sqlEx) {

			recover(sqlEx);
			return connection.prepareStatement(sql, columnNames);
		}
	}

	@Override
	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		try {
			return connection.createArrayOf(typeName, elements);
		} catch (SQLException sqlEx) {

			recover(sqlEx);
			return connection.createArrayOf(typeName, elements);
		}
	}

	@Override
	public Blob createBlob() throws SQLException {
		try {
			return connection.createBlob();
		} catch (SQLException sqlEx) {

			recover(sqlEx);
			return connection.createBlob();
		}
	}

	@Override
	public Clob createClob() throws SQLException {
		try {
			return connection.createClob();
		} catch (SQLException sqlEx) {

			recover(sqlEx);
			return connection.createClob();
		}
	}

	@Override
	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		try {
			return connection.createStruct(typeName, attributes);
		} catch (SQLException sqlEx) {

			recover(sqlEx);
			return connection.createStruct(typeName, attributes);
		}
	}

	@Override
	public Properties getClientInfo() throws SQLException {
		try {
			return connection.getClientInfo();
		} catch (SQLException sqlEx) {

			recover(sqlEx);
			return connection.getClientInfo();
		}
	}

	@Override
	public String getClientInfo(String name) throws SQLException {
		try {
			return connection.getClientInfo(name);
		} catch (SQLException sqlEx) {

			recover(sqlEx);
			return connection.getClientInfo(name);
		}
	}

	@Override
	public boolean isValid(int timeout) throws SQLException {
		try {
			return connection.isValid(timeout);
		} catch (SQLException sqlEx) {

			recover(sqlEx);
			return connection.isValid(timeout);
		}
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return (PoolAwareConnection.class.equals(iface)) ? true : connection.isWrapperFor(iface);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return PoolAwareConnection.class.equals(iface) ? (T) this : connection.unwrap(iface);
	}

	@Override
	public NClob createNClob() throws SQLException {
		try {
			return connection.createNClob();
		} catch (SQLException sqlEx) {

			recover(sqlEx);
			return connection.createNClob();
		}
	}

	@Override
	public SQLXML createSQLXML() throws SQLException {
		try {
			return connection.createSQLXML();
		} catch (SQLException sqlEx) {

			recover(sqlEx);
			return connection.createSQLXML();
		}
	}

	@Override
	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		connection.setClientInfo(properties);
	}

	@Override
	public void setClientInfo(String name, String value) throws SQLClientInfoException {
		connection.setClientInfo(name, value);
	}

	@Override
	public void setSchema(String schema) throws SQLException {
		connection.setSchema(schema);
	}

	@Override
	public String getSchema() throws SQLException {
		return connection.getSchema();
	}

	@Override
	public void abort(Executor executor) throws SQLException {
		connection.abort(executor);
	}

	@Override
	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
		connection.setNetworkTimeout(executor, milliseconds);
	}

	@Override
	public int getNetworkTimeout() throws SQLException {
		return connection.getNetworkTimeout();
	}

}
