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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * Minimal JDBC {@link DataSource} stub for tests. Returns a configurable
 * {@link Connection} (default: a fresh {@link TestConnection}) from {@link #getConnection()}, or fails with a
 * configured exception, and counts the requests for a connection.
 */
public class TestDataSource implements DataSource {

    private Connection connection = new TestConnection();
    private SQLException failure;
    private int connectionRequests;

    public void setupConnection(Connection connection) {
        this.connection = connection;
    }

    /**
     * Makes the requests for a connection fail with the given exception, as a DataSource of a DB that is down.
     */
    public TestDataSource failGetConnection(SQLException failure) {
        this.failure = failure;
        return this;
    }

    public int getConnectionRequests() {
        return connectionRequests;
    }

    @Override
    public Connection getConnection() throws SQLException {
        connectionRequests++;
        if (failure != null) {
            throw failure;
        }
        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getConnection();
    }

    @Override public PrintWriter getLogWriter() { return null; }
    @Override public void setLogWriter(PrintWriter out) {}
    @Override public void setLoginTimeout(int seconds) {}
    @Override public int getLoginTimeout() { return 0; }
    @Override public Logger getParentLogger() { return Logger.getLogger("global"); }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("not supported"); }
    @Override public boolean isWrapperFor(Class<?> iface) { return false; }
}
