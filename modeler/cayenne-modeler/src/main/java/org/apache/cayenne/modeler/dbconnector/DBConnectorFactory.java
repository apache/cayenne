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

package org.apache.cayenne.modeler.dbconnector;

import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.runtime.DbAdapterFactory;
import org.apache.cayenne.datasource.DriverDataSource;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.modeler.pref.dbconnector.DBConnector;
import org.apache.cayenne.modeler.service.classloader.ModelerClassLoader;
import org.apache.cayenne.util.Util;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * Creates live {@code DataSource} and {@code DbAdapter} instances from a {@link DBConnector}
 * configuration, using a {@link ModelerClassLoader} to load the JDBC driver at runtime.
 */
public class DBConnectorFactory {

    private final ModelerClassLoader classLoader;

    public DBConnectorFactory(ModelerClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public DbAdapter makeAdapter(DBConnector connector, DbAdapterFactory adapterFactory) throws Exception {
        return makeAdapter(connector, adapterFactory, false);
    }

    public DbAdapter makeAdapter(DBConnector connector, DbAdapterFactory adapterFactory,
                                 boolean allowDataSourceFailure) throws Exception {
        DataNodeDescriptor descriptor = new DataNodeDescriptor();
        descriptor.setAdapterType(connector.getDbAdapter());
        DataSource dataSource = makeDataSource(connector, allowDataSourceFailure);
        return adapterFactory.createAdapter(descriptor, dataSource);
    }

    public DataSource makeDataSource(DBConnector connector) throws SQLException {
        return makeDataSource(connector, false);
    }

    public DataSource makeDataSource(DBConnector connector, boolean allowDataSourceFailure) throws SQLException {
        if (connector.getJdbcDriver() == null) {
            if (allowDataSourceFailure) {
                return new DeferredDataSource(connector);
            }
            throw new SQLException("No JDBC driver set.");
        }

        if (connector.getUrl() == null) {
            if (allowDataSourceFailure) {
                return new DeferredDataSource(connector);
            }
            throw new SQLException("No DB URL set.");
        }

        if (!Util.isBlank(connector.getPassword()) && Util.isBlank(connector.getUserName())) {
            throw new SQLException("No username when password is set.");
        }

        Driver driver;
        try {
            driver = classLoader.loadClass(Driver.class, connector.getJdbcDriver()).getDeclaredConstructor().newInstance();
        } catch (Throwable th) {
            throw new SQLException("Driver load error: " + Util.unwindException(th).getLocalizedMessage());
        }

        return new DriverDataSource(driver, connector.getUrl(), connector.getUserName(), connector.getPassword());
    }

    private class DeferredDataSource implements DataSource {

        private final DBConnector connector;

        DeferredDataSource(DBConnector connector) {
            this.connector = connector;
        }

        DataSource getDelegate() throws SQLException {
            return makeDataSource(connector, false);
        }

        @Override
        public Connection getConnection() throws SQLException {
            return getDelegate().getConnection();
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getDelegate().getConnection(username, password);
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return getDelegate().getLogWriter();
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
            getDelegate().setLogWriter(out);
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            getDelegate().setLoginTimeout(seconds);
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return getDelegate().getLoginTimeout();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return getDelegate().unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return getDelegate().isWrapperFor(iface);
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            try {
                return getDelegate().getParentLogger();
            } catch (SQLException e) {
                throw new SQLFeatureNotSupportedException(e);
            }
        }
    }
}
