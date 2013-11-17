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
package org.apache.cayenne.configuration.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.ResourceLocator;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link DataSourceFactory} based on DBCP connection pool library.
 * 
 * @since 3.1
 */
public class DBCPDataSourceFactory implements DataSourceFactory {

    private static final String DBCP_PROPERTIES = "dbcp.properties";

    private static final Log logger = LogFactory.getLog(DBCPDataSourceFactory.class);

    @Inject
    protected ResourceLocator resourceLocator;

    @Override
    public DataSource getDataSource(DataNodeDescriptor nodeDescriptor) throws Exception {

        String location = nodeDescriptor.getParameters();
        if (location == null) {
            logger.debug("No explicit DBCP config location, will use default location: "
                    + DBCP_PROPERTIES);
            location = DBCP_PROPERTIES;
        }

        Resource baseConfiguration = nodeDescriptor.getConfigurationSource();
        if (baseConfiguration == null) {
            throw new CayenneRuntimeException(
                    "Null 'configurationSource' for nodeDescriptor '%s'",
                    nodeDescriptor.getName());
        }

        Resource dbcpConfiguration = baseConfiguration.getRelativeResource(location);
        if (dbcpConfiguration == null) {
            throw new CayenneRuntimeException(
                    "Missing DBCP configuration '%s' for nodeDescriptor '%s'",
                    location,
                    nodeDescriptor.getName());
        }

        Properties properties = getProperties(dbcpConfiguration);
        if (logger.isDebugEnabled()) {
            logger.debug("DBCP Properties: " + properties);
        }

        properties = filteredDeprecatedProperties(properties);
        return BasicDataSourceFactory.createDataSource(properties);
    }

    /**
     * Converts old-style cayene.dbcp.xyz properties to just cayenne.dbcp.
     */
    private Properties filteredDeprecatedProperties(Properties unfiltered) {
        Properties properties = new Properties();

        final String deprecatedPrefix = "cayenne.dbcp.";

        for (Entry<Object, Object> entry : unfiltered.entrySet()) {
            Object key = entry.getKey();
            if (key instanceof String && key.toString().startsWith(deprecatedPrefix)) {

                String oldKey = key.toString();
                key = oldKey.substring(deprecatedPrefix.length());
                logger.info("Deprecated use of 'cayenne.dbcp.' prefix in '"
                        + oldKey
                        + "', converting to "
                        + key);
            }

            properties.put(key, entry.getValue());
        }

        return properties;
    }

    private Properties getProperties(Resource dbcpConfiguration) throws IOException {
        Properties properties = new Properties();
        InputStream in = dbcpConfiguration.getURL().openStream();
        try {
            properties.load(in);
        }
        finally {
            try {
                in.close();
            }
            catch (IOException e) {
            }
        }

        return properties;
    }
}
