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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.cayenne.di.ScopeEventListener;

/**
 * A wrapper for {@link PoolingDataSourceManager} that manages the underlying
 * connection pool size, shrinking it if needed.
 * 
 * @since 4.0
 */
public class ManagedPoolingDataSource implements DataSource, ScopeEventListener {

	private PoolingDataSourceManager dataSourceManager;
	private PoolingDataSource dataSource;

	public ManagedPoolingDataSource(PoolingDataSource dataSource) {

		this.dataSource = dataSource;
		this.dataSourceManager = new PoolingDataSourceManager();

		dataSourceManager.start();
	}

	@Override
	public void beforeScopeEnd() {
		dataSourceManager.shouldStop();
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
		return (ManagedPoolingDataSource.class.equals(iface)) ? true : dataSource.isWrapperFor(iface);
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

	// JDBC 4.1 compatibility under Java 1.6 and newer
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new UnsupportedOperationException();
	}

	boolean shouldShrinkPool() {
		int unused = dataSource.getCurrentlyUnused();
		int used = dataSource.getCurrentlyInUse();
		int total = unused + used;
		int median = dataSource.getMinConnections() + 1
				+ (dataSource.getMaxConnections() - dataSource.getMinConnections()) / 2;

		return unused > 0 && total > median;
	}

	class PoolingDataSourceManager extends Thread {

		private volatile boolean shouldStop;

		PoolingDataSourceManager() {
			setName("PoolManagerCleanup-" + dataSource.hashCode());
			setDaemon(true);
			this.shouldStop = false;
		}

		public void shouldStop() {
			shouldStop = true;
			interrupt();
		}

		@Override
		public void run() {
			while (true) {

				try {
					// don't do it too often
					Thread.sleep(600000);
				} catch (InterruptedException iex) {
					// ignore...
				}

				synchronized (dataSource) {

					// simple pool management - close one connection if the
					// count is
					// above median and there are any idle connections.

					if (shouldStop) {
						break;
					}

					if (shouldShrinkPool()) {
						dataSource.shrinkPool(1);
					}
				}
			}
		}
	}

}
