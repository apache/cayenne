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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.cayenne.di.ScopeEventListener;

/**
 * A wrapper for {@link UnmanagedPoolingDataSource} that automatically manages
 * the underlying connection pool size.
 * 
 * @since 4.0
 */
public class ManagedPoolingDataSource implements PoolingDataSource, ScopeEventListener {

	private final PoolingDataSourceManager dataSourceManager;
	private DataSource dataSource;

	public ManagedPoolingDataSource(UnmanagedPoolingDataSource dataSource) {
		// wake every 2 minutes...
		this(dataSource, 120000);
	}

	public ManagedPoolingDataSource(UnmanagedPoolingDataSource dataSource, long managerWakeTime) {
		this.dataSource = dataSource;
		this.dataSourceManager = new PoolingDataSourceManager(dataSource, managerWakeTime);

		dataSourceManager.start();
	}

	PoolingDataSourceManager getDataSourceManager() {
		return dataSourceManager;
	}

	int poolSize() {
		return dataSourceManager.getDataSource().poolSize();
	}

	int availableSize() {
		return dataSourceManager.getDataSource().availableSize();
	}
	
	int canExpandSize() {
		return dataSourceManager.getDataSource().canExpandSize();
	}

	/**
	 * Calls {@link #close()} to drain the underlying pool, close open
	 * connections and block the DataSource from creating any new connections.
	 */
	@Override
	public void beforeScopeEnd() {
		close();
	}

	@Override
	public void close() {

		// swap the underlying DataSource to prevent further interaction with
		// the callers
		this.dataSource = new StoppedDataSource(dataSource);

		// shut down the thread..
		this.dataSourceManager.shutdown();
	}

	@Override
	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return dataSource.getConnection(username, password);
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return dataSource.getLogWriter();
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return dataSource.getLoginTimeout();
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return ManagedPoolingDataSource.class.equals(iface) || dataSource.isWrapperFor(iface);
	}

	@Override
	public void setLogWriter(PrintWriter arg0) throws SQLException {
		dataSource.setLogWriter(arg0);
	}

	@Override
	public void setLoginTimeout(int arg0) throws SQLException {
		dataSource.setLoginTimeout(arg0);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return ManagedPoolingDataSource.class.equals(iface) ? (T) this : dataSource.unwrap(iface);
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return dataSource.getParentLogger();
	}

}
