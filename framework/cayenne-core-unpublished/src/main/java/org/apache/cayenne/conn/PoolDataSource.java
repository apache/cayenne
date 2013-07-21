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
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;

/**
 * <p>PoolDataSource allows to generate pooled connections.</p>
 *
 * <p>It is implemented as a wrapper around a non-pooled data source object. 
 * Delegates all method calls except for "getPooledConnection" to the underlying 
 * datasource.
 * </p>
 * 
 */
public class PoolDataSource implements ConnectionPoolDataSource {
	private DataSource nonPooledDatasource;

	/** Creates new PoolDataSource */
	public PoolDataSource(DataSource nonPooledDatasource) {
		this.nonPooledDatasource = nonPooledDatasource;
	}

	public PoolDataSource(String jdbcDriver, String connectionUrl) throws SQLException {
		nonPooledDatasource = new DriverDataSource(jdbcDriver, connectionUrl);
	}

	public int getLoginTimeout() throws SQLException {
		return nonPooledDatasource.getLoginTimeout();
	}

	public void setLoginTimeout(int seconds) throws SQLException {
		nonPooledDatasource.setLoginTimeout(seconds);
	}

	public PrintWriter getLogWriter() throws SQLException {
		return nonPooledDatasource.getLogWriter();
	}

	public void setLogWriter(PrintWriter out) throws SQLException {
		nonPooledDatasource.setLogWriter(out);
	}

	public PooledConnection getPooledConnection() throws SQLException {
		return new PooledConnectionImpl(nonPooledDatasource, null, null);
	}

	public PooledConnection getPooledConnection(String user, String password) throws SQLException {
		return new PooledConnectionImpl(nonPooledDatasource, user, password);
	}

    /**
     * @since 3.1
     *
     * JDBC 4.1 compatibility under Java 1.5
     */
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new UnsupportedOperationException();
    }
}
