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
package org.apache.cayenne.configuration.runtime;

import java.sql.Driver;

import javax.sql.DataSource;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.datasource.DataSourceBuilder;
import org.apache.cayenne.datasource.UnmanagedPoolingDataSource;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;

/**
 * A DataSourceFactrory that creates a DataSource based on system properties.
 * Properties can be set per domain/node name or globally, applying to all nodes
 * without explicit property set. The following properties are supported:
 * <ul>
 * <li>cayenne.jdbc.driver[.domain_name.node_name]
 * <li>cayenne.jdbc.url[.domain_name.node_name]
 * <li>cayenne.jdbc.username[.domain_name.node_name]
 * <li>cayenne.jdbc.password[.domain_name.node_name]
 * <li>cayenne.jdbc.min.connections[.domain_name.node_name]
 * <li>cayenne.jdbc.max.conections[.domain_name.node_name]
 * </ul>
 * At least url and driver properties must be specified for this factory to
 * return a valid DataSource.
 * 
 * @since 3.1
 */
public class PropertyDataSourceFactory implements DataSourceFactory {

	@Inject
	protected RuntimeProperties properties;

	@Inject
	private AdhocObjectFactory objectFactory;

	@Override
	public DataSource getDataSource(DataNodeDescriptor nodeDescriptor) throws Exception {

		String suffix = "." + nodeDescriptor.getDataChannelDescriptor().getName() + "." + nodeDescriptor.getName();

		String driverClass = getProperty(Constants.JDBC_DRIVER_PROPERTY, suffix);
		String url = getProperty(Constants.JDBC_URL_PROPERTY, suffix);
		String username = getProperty(Constants.JDBC_USERNAME_PROPERTY, suffix);
		String password = getProperty(Constants.JDBC_PASSWORD_PROPERTY, suffix);
		int minConnections = getIntProperty(Constants.JDBC_MIN_CONNECTIONS_PROPERTY, suffix, 1);
		int maxConnections = getIntProperty(Constants.JDBC_MAX_CONNECTIONS_PROPERTY, suffix, 1);
		long maxQueueWaitTime = properties.getLong(Constants.JDBC_MAX_QUEUE_WAIT_TIME,
				UnmanagedPoolingDataSource.MAX_QUEUE_WAIT_DEFAULT);
		String validationQuery = properties.get(Constants.JDBC_VALIDATION_QUERY_PROPERTY);

		Driver driver = objectFactory.<Driver>getJavaClass(driverClass).getDeclaredConstructor().newInstance();
		return DataSourceBuilder.url(url).driver(driver).userName(username).password(password)
				.pool(minConnections, maxConnections).maxQueueWaitTime(maxQueueWaitTime)
				.validationQuery(validationQuery).build();
	}

	protected int getIntProperty(String propertyName, String suffix, int defaultValue) {
		String string = getProperty(propertyName, suffix);

		if (string == null) {
			return defaultValue;
		}

		try {
			return Integer.parseInt(string);
		} catch (NumberFormatException e) {
			throw new ConfigurationException("Invalid int property '%s': '%s'", propertyName, string);
		}
	}

	protected String getProperty(String propertyName, String suffix) {
		String value = properties.get(propertyName + suffix);
		return value != null ? value : properties.get(propertyName);
	}
}
