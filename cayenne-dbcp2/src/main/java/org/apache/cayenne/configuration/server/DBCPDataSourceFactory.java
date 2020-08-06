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
package org.apache.cayenne.configuration.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.resource.Resource;
import org.apache.commons.dbcp2.BasicDataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link DataSourceFactory} based on DBCP2 connection pool library.
 * @deprecated since 4.1
 */
@Deprecated
public class DBCPDataSourceFactory implements DataSourceFactory {

	private static final String DBCP2_PROPERTIES = "dbcp2.properties";

	private static final Logger logger = LoggerFactory.getLogger(DBCPDataSourceFactory.class);

	@Override
	public DataSource getDataSource(DataNodeDescriptor nodeDescriptor) throws Exception {

		String location = nodeDescriptor.getParameters();
		if (location == null) {
			logger.debug("No explicit DBCP2 config location, will use default location: " + DBCP2_PROPERTIES);
			location = DBCP2_PROPERTIES;
		}

		Resource baseConfiguration = nodeDescriptor.getConfigurationSource();
		if (baseConfiguration == null) {
			throw new CayenneRuntimeException("Null 'configurationSource' for nodeDescriptor '%s'",
					nodeDescriptor.getName());
		}

		Resource dbcp2Configuration = baseConfiguration.getRelativeResource(location);
		if (dbcp2Configuration == null) {
			throw new CayenneRuntimeException("Missing DBCP2 configuration '%s' for nodeDescriptor '%s'", location,
					nodeDescriptor.getName());
		}

		Properties properties = getProperties(dbcp2Configuration);
		if (logger.isDebugEnabled()) {
			logger.debug("DBCP2 Properties: " + properties);
		}

		return BasicDataSourceFactory.createDataSource(properties);
	}

	private Properties getProperties(Resource dbcp2Configuration) throws IOException {
		Properties properties = new Properties();

		try (InputStream in = dbcp2Configuration.getURL().openStream();) {
			properties.load(in);
		}

		return properties;
	}
}
