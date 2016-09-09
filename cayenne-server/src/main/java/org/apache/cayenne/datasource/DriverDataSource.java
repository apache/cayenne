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

package org.apache.cayenne.datasource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A non-pooling DataSource implementation wrapping a JDBC driver.
 */
public class DriverDataSource implements DataSource {

	private static final Log LOGGER = LogFactory.getLog(DriverDataSource.class);

	protected Driver driver;
	protected String connectionUrl;
	protected String userName;
	protected String password;

	/**
	 * Loads JDBC driver using current thread class loader.
	 * 
	 * @since 3.0
	 * @deprecated since 4.0 as class loading should not happen here.
	 */
	@Deprecated
	private static Driver loadDriver(String driverClassName) {

		Class<?> driverClass;
		try {
			driverClass = Util.getJavaClass(driverClassName);
		} catch (Exception ex) {
			throw new CayenneRuntimeException("Can not load JDBC driver named '" + driverClassName + "': "
					+ ex.getMessage());
		}

		try {
			return (Driver) driverClass.newInstance();
		} catch (Exception ex) {
			throw new CayenneRuntimeException("Error instantiating driver '" + driverClassName + "': "
					+ ex.getMessage());
		}
	}

	/**
	 * Creates a new DriverDataSource. If "driverClassName" is null,
	 * DriverDataSource will consult DriverManager for a registered driver for
	 * the given URL. So when specifying null, a user must take care of
	 * registering the driver. "connectionUrl" on the other hand must NOT be
	 * null.
	 * 
	 * @deprecated since 4.0 as class loading should not happen here. Use {
	 *             {@link #DriverDataSource(Driver, String, String, String)}.
	 */
	@Deprecated
	public DriverDataSource(String driverClassName, String connectionUrl) {
		this(driverClassName, connectionUrl, null, null);
	}

	/**
	 * Creates a new DriverDataSource. If "driverClassName" is null,
	 * DriverDataSource will consult DriverManager for a registered driver for
	 * the given URL. So when specifying null, a user must take care of
	 * registering the driver. "connectionUrl" on the other hand must NOT be
	 * null.
	 * 
	 * @deprecated since 4.0 as class loading should not happen here. Use
	 *             {@link #DriverDataSource(Driver, String, String, String)}.
	 */
	@Deprecated
	public DriverDataSource(String driverClassName, String connectionUrl, String userName, String password) {
		this(loadDriver(driverClassName), connectionUrl, userName, password);
	}

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

		if (connectionUrl == null) {
			throw new NullPointerException("Null 'connectionUrl'");
		}

		this.driver = driver;
		this.connectionUrl = connectionUrl;
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

			logConnect(connectionUrl, userName, password);
			Connection c = null;

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

	private void logConnect(String url, String userName, String password) {
		if (LOGGER.isInfoEnabled()) {
			StringBuilder buf = new StringBuilder("Connecting to '").append(url).append("' as '").append(userName)
					.append("'");
			LOGGER.info(buf.toString());
		}
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return -1;
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		// noop
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return DriverManager.getLogWriter();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		DriverManager.setLogWriter(out);
	}

	/**
	 * @since 3.0
	 * @deprecated since 4.0. Connection parameters are immutable and not
	 *             readable.
	 */
	@Deprecated
	public String getConnectionUrl() {
		return connectionUrl;
	}

	/**
	 * @since 3.0
	 * @deprecated since 4.0. Connection parameters are immutable.
	 */
	@Deprecated
	public void setConnectionUrl(String connectionUrl) {
		this.connectionUrl = connectionUrl;
	}

	/**
	 * @since 3.0
	 * @deprecated since 4.0. Connection parameters are immutable and not
	 *             readable.
	 */
	@Deprecated
	public String getPassword() {
		return password;
	}

	/**
	 * @deprecated since 4.0. Connection parameters are immutable.
	 * @since 3.0
	 */
	@Deprecated
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @since 3.0
	 * @deprecated since 4.0. Connection parameters are immutable and not
	 *             readable.
	 */
	@Deprecated
	public String getUserName() {
		return userName;
	}

	/**
	 * @since 3.0
	 * @deprecated since 4.0. Connection parameters are immutable.
	 */
	@Deprecated
	public void setUserName(String userName) {
		this.userName = userName;
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
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new UnsupportedOperationException();
	}
}
