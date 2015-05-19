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

import java.sql.Driver;

import javax.sql.DataSource;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.di.AdhocObjectFactory;

/**
 * A builder class that allows to build a {@link DataSource} with optional
 * pooling.
 * 
 * @since 4.0
 */
public class DataSourceBuilder {

	private AdhocObjectFactory objectFactory;
	private String userName;
	private String password;
	private String driver;
	private String url;
	private PoolingDataSourceParameters poolParameters;

	public static DataSourceBuilder builder(AdhocObjectFactory objectFactory) {
		return new DataSourceBuilder(objectFactory);
	}

	private DataSourceBuilder(AdhocObjectFactory objectFactory) {
		this.objectFactory = objectFactory;
		this.poolParameters = new PoolingDataSourceParameters();

		poolParameters.setMinConnections(1);
		poolParameters.setMaxConnections(1);
		poolParameters.setMaxQueueWaitTime(PoolingDataSource.MAX_QUEUE_WAIT_DEFAULT);
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
		this.driver = driver;
		return this;
	}

	public DataSourceBuilder url(String url) {
		this.url = url;
		return this;
	}

	public DataSourceBuilder minConnections(int minConnections) {
		poolParameters.setMinConnections(minConnections);
		return this;
	}

	public DataSourceBuilder maxConnections(int maxConnections) {
		poolParameters.setMaxConnections(maxConnections);
		return this;
	}

	public DataSourceBuilder maxQueueWaitTime(long maxQueueWaitTime) {
		poolParameters.setMaxQueueWaitTime(maxQueueWaitTime);
		return this;
	}

	public DataSourceBuilder validationQuery(String validationQuery) {
		poolParameters.setValidationQuery(validationQuery);
		return this;
	}

	public DataSource build() {

		// sanity checks...
		if (poolParameters.getMaxConnections() < 0) {
			throw new CayenneRuntimeException("Maximum number of connections can not be negative ("
					+ poolParameters.getMaxConnections() + ").");
		}

		if (poolParameters.getMinConnections() < 0) {
			throw new CayenneRuntimeException("Minimum number of connections can not be negative ("
					+ poolParameters.getMinConnections() + ").");
		}

		if (poolParameters.getMinConnections() > poolParameters.getMaxConnections()) {
			throw new CayenneRuntimeException("Minimum number of connections can not be bigger then maximum.");
		}

		return buildManaged(buildPooling(buildNonPooling()));
	}

	private DataSource buildNonPooling() {
		Driver driver = objectFactory.newInstance(Driver.class, this.driver);
		return new DriverDataSource(driver, url, userName, password);
	}

	private PoolingDataSource buildPooling(DataSource nonPoolingDataSource) {
		return new PoolingDataSource(nonPoolingDataSource, poolParameters);
	}

	private DataSource buildManaged(PoolingDataSource dataSource) {
		return new ManagedPoolingDataSource(dataSource);
	}
}
