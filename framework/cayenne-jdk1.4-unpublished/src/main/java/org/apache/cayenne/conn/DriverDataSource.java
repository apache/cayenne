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
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

/**
 * A DataSource implementation wrapping a JDBC driver.
 * 
 * @author Andrus Adamchik
 */
public class DriverDataSource implements DataSource {

    protected Driver driver;

    protected String connectionUrl;
    protected String userName;
    protected String password;

    protected ConnectionEventLoggingDelegate logger;

    /**
     * Creates a new DriverDataSource.
     */
    public DriverDataSource(String driverClassName, String connectionUrl)
            throws SQLException {

        this.connectionUrl = connectionUrl;

        if (driverClassName != null) {
            try {
                this.driver = (Driver) Class.forName(driverClassName).newInstance();
            }
            catch (Exception ex) {
                throw new SQLException("Can not load JDBC driver named '"
                        + driverClassName
                        + "': "
                        + ex.getMessage());
            }
        }
    }

    /**
     * Creates a new DriverDataSource wrapping a given Driver.
     * 
     * @since 1.1
     */
    public DriverDataSource(Driver driver, String connectionUrl, String userName,
            String password) {
        this.driver = driver;
        this.connectionUrl = connectionUrl;
        this.userName = userName;
        this.password = password;
    }

    /**
     * Returns a new database connection, using preconfigured data to locate the database
     * and obtain a connection.
     */
    public Connection getConnection() throws SQLException {
        // login with internal credentials
        return getConnection(userName, password);
    }

    /**
     * Returns a new database connection using provided credentials to login to the
     * database.
     */
    public Connection getConnection(String userName, String password) throws SQLException {
        try {
            if (logger != null) {
                logger.logConnect(connectionUrl, userName, password);
            }

            Connection c = null;

            if (driver == null) {
                c = DriverManager.getConnection(connectionUrl, userName, password);
            }
            else {
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

            if (logger != null) {
                logger.logConnectSuccess();
            }

            return c;
        }
        catch (SQLException sqlex) {
            if (logger != null) {
                logger.logConnectFailure(sqlex);
            }

            throw sqlex;
        }
    }

    public int getLoginTimeout() throws SQLException {
        return -1;
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        // noop
    }

    public PrintWriter getLogWriter() throws SQLException {
        return DriverManager.getLogWriter();
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        DriverManager.setLogWriter(out);
    }

    public ConnectionEventLoggingDelegate getLogger() {
        return logger;
    }

    public void setLogger(ConnectionEventLoggingDelegate delegate) {
        logger = delegate;
    }
}
