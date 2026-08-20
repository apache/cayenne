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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

/**
 * A non-pooling DataSource implementation wrapping a JDBC driver.
 */
public class DriverDataSource implements DataSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(DriverDataSource.class);

    protected final Driver driver;
    protected final String connectionUrl;
    protected final String userName;
    protected final String password;

    /**
     * Creates a DriverDataSource wrapping a given Driver. If "driver" is null,
     * DriverDataSource will consult DriverManager for a registered driver for
     * the given URL. So when specifying null, a user must take care of
     * registering the driver. "connectionUrl" on the other hand must NOT be
     * null.
     *
     * @since 1.1
     */
    public DriverDataSource(Driver driver, String connectionUrl, String userName, String password) {
        this.driver = driver;
        this.connectionUrl = Objects.requireNonNull(connectionUrl, "Null 'connectionUrl'");
        this.userName = userName;
        this.password = password;
    }

    /**
     * Returns a new database connection, using preconfigured data to locate the
     * database and obtain a connection.
     */
    @Override
    public Connection getConnection() throws SQLException {
        // login with internal credentials
        return getConnection(userName, password);
    }

    /**
     * Returns a new database connection using provided credentials to login to
     * the database.
     */
    @Override
    public Connection getConnection(String userName, String password) throws SQLException {
        try {

            logConnect(connectionUrl, userName);
            Connection c;

            if (driver == null) {
                c = DriverManager.getConnection(connectionUrl, userName, password);
            } else {
                Properties connectProperties = new Properties();

                if (userName != null) {
                    connectProperties.put("user", userName);
                }

                if (password != null) {
                    connectProperties.put("password", password);
                }
                c = driver.connect(connectionUrl, connectProperties);
            }

            // some drivers (Oracle) return null connections instead of throwing
            // an exception... fix it here

            if (c == null) {
                throw new SQLException("Can't establish connection: " + connectionUrl);
            }

            LOGGER.info("+++ Connecting: SUCCESS.");

            return c;
        } catch (SQLException ex) {
            LOGGER.info("*** Connecting: FAILURE.", ex);
            throw ex;
        }
    }

    private void logConnect(String url, String userName) {
        LOGGER.info("Connecting to '{}' as '{}'", url, userName);
    }

    @Override
    public int getLoginTimeout() {
        return -1;
    }

    @Override
    public void setLoginTimeout(int seconds) {
        // noop
    }

    @Override
    public PrintWriter getLogWriter() {
        return DriverManager.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        DriverManager.setLogWriter(out);
    }

    /**
     * @since 3.0
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.0
     */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.1
     */
    @Override
    public java.util.logging.Logger getParentLogger() {
        throw new UnsupportedOperationException();
    }
}
