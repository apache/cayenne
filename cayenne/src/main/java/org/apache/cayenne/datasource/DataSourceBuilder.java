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

import org.apache.cayenne.CayenneRuntimeException;

import javax.sql.DataSource;
import java.sql.Driver;

/**
 * A builder class that allows to build a {@link DataSource} with optional
 * pooling.
 * 
 * @since 4.0
 */
public class DataSourceBuilder {

	private String userName;
	private String password;
	private String driverClassName;
	private Driver driver;
	private String url;

	public static DataSourceBuilder url(String url) {
		return new DataSourceBuilder(url);
	}

	private DataSourceBuilder(String url) {
		this.url = url;
	}

	public DataSourceBuilder userName(String userName) {
		this.userName = userName;
		return this;
	}

	public DataSourceBuilder password(String password) {
		this.password = password;
		return this;
	}

	public DataSourceBuilder driver(String driver) {
		// TODO: guess the driver from URL
		this.driver = null;
		this.driverClassName = driver;
		return this;
	}

	public DataSourceBuilder driver(Driver driver) {
		this.driver = driver;
		this.driverClassName = null;
		return this;
	}

	/**
	 * Turns produced DataSource into a pooled DataSource.
	 */
	public PoolingDataSourceBuilder pool(int minConnection, int maxConnections) {
		return new PoolingDataSourceBuilder(this)
				.minConnections(minConnection)
				.maxConnections(maxConnections);
	}

	/**
	 * Builds a non-pooling DataSource. To create connection pool use
	 * {@link #pool(int, int)} method.
	 */
	public DataSource build() {
		Driver driver = loadDriver();
		return new DriverDataSource(driver, url, userName, password);
	}

	private Driver loadDriver() {

		if (driver != null) {
			return driver;
		}

		Class<?> driverClass;
		try {
			// note: implicitly using current class's ClassLoader ....
			driverClass = Class.forName(driverClassName);
		} catch (Exception ex) {
			throw new CayenneRuntimeException("Can not load JDBC driver named '%s': %s"
					, driverClassName, ex.getMessage());
		}

		try {
			return (Driver) driverClass.getDeclaredConstructor().newInstance();
		} catch (Exception ex) {
			throw new CayenneRuntimeException("Error instantiating driver '%s': %s"
					, driverClassName, ex.getMessage());
		}
	}
}
