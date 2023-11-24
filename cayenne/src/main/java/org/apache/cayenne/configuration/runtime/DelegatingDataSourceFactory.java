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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.BeforeScopeEnd;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.ScopeEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link DataSourceFactory} that delegates DataSource creation to another factory,
 * which is determined dynamically per DataNodeDescriptor. The delegate class may be
 * explicitly defined in the {@link DataNodeDescriptor}. If not, and if the descriptor has
 * a configuration resource attached to it, {@link XMLPoolingDataSourceFactory} is used.
 * <p>
 * If the environment contains properties <em>cayenne.jdbc.url.domain_name.node_name</em>
 * (or <em>cayenne.jdbc.url</em>) and <em>cayenne.jdbc.driver.domain_name.node_name</em>
 * (or <em>cayenne.jdbc.driver</em>), any DataSourceFactory configured in the
 * DataNodeDescriptor is ignored, and the {@link PropertyDataSourceFactory} is used.
 *
 * @since 3.1
 */
public class DelegatingDataSourceFactory implements DataSourceFactory {

    private static final Logger logger = LoggerFactory
            .getLogger(DelegatingDataSourceFactory.class);

    @Inject
    protected AdhocObjectFactory objectFactory;

    @Inject
    protected RuntimeProperties properties;

    protected Map<DataSource, ScopeEventListener> managedDataSources;

    public DelegatingDataSourceFactory() {
        managedDataSources = new ConcurrentHashMap<>();
    }

    @Override
    public DataSource getDataSource(DataNodeDescriptor nodeDescriptor) throws Exception {
        DataSource dataSource = getDataSourceFactory(nodeDescriptor).getDataSource(
                nodeDescriptor);
        attachToScope(dataSource);
        return dataSource;
    }

    @BeforeScopeEnd
    public void shutdown() {
        for (ScopeEventListener listener : managedDataSources.values()) {
            listener.beforeScopeEnd();
        }

        managedDataSources.clear();
    }

    /**
     * Ensure that DataSource implementations returned from this factory receive
     * {@link BeforeScopeEnd} events.
     */
    protected void attachToScope(DataSource dataSource) {

        if (!managedDataSources.containsKey(dataSource)) {
            if (dataSource instanceof ScopeEventListener) {
                managedDataSources.put(dataSource, (ScopeEventListener) dataSource);
            }
        }
    }

    protected DataSourceFactory getDataSourceFactory(DataNodeDescriptor nodeDescriptor) {
        String typeName;

        if (shouldConfigureDataSourceFromProperties(nodeDescriptor)) {
            typeName = PropertyDataSourceFactory.class.getName();
        }
        else {
            typeName = nodeDescriptor.getDataSourceFactoryType();
        }

        if (typeName == null) {
            if (nodeDescriptor.getDataSourceDescriptor() == null) {
                throw new CayenneRuntimeException(
                        "DataNodeDescriptor '%s' has null 'dataSourceFactoryType' and 'dataSourceDescriptor' properties",
                        nodeDescriptor.getName());
            }

            typeName = XMLPoolingDataSourceFactory.class.getName();
        }

        return objectFactory.newInstance(DataSourceFactory.class, typeName);
    }

    private String getDataNodePropertyName(DataNodeDescriptor nodeDescriptor, String propertyConstant) {
        return propertyConstant
                + "."
                + nodeDescriptor.getDataChannelDescriptor().getName()
                + "."
                + nodeDescriptor.getName();
    }

    private void findUnusedProperties(DataNodeDescriptor nodeDescriptor) {
        if(!logger.isInfoEnabled() || nodeDescriptor.getDataChannelDescriptor() == null) {
            return;
        }
        boolean found = false;
        StringBuilder logResult = new StringBuilder();
        String nodeName = nodeDescriptor.getDataChannelDescriptor().getName()
                + "."
                + nodeDescriptor.getName();
        logResult.append("Following runtime properties were ignored for node '").append(nodeName).append("': ");
        String[] verifiableProperties = new String[] {
                Constants.JDBC_USERNAME_PROPERTY, Constants.JDBC_PASSWORD_PROPERTY,
                Constants.JDBC_MAX_CONNECTIONS_PROPERTY, Constants.JDBC_MIN_CONNECTIONS_PROPERTY,
                Constants.JDBC_MAX_QUEUE_WAIT_TIME, Constants.JDBC_VALIDATION_QUERY_PROPERTY
        };
        for (String propertyConstant : verifiableProperties) {
            String property = properties.get(getDataNodePropertyName(nodeDescriptor, propertyConstant));
            if (property != null) {
                logResult.append(getDataNodePropertyName(nodeDescriptor, propertyConstant)).append(", ");
                found = true;
            }
            property = properties.get(propertyConstant);
            if (property != null) {
                logResult.append(propertyConstant).append(", ");
                found = true;
            }
        }
        if (found) {
            logResult.delete(logResult.length() - 2, logResult.length())
                    .append(". Will use project DataSource configuration. ")
                    .append("Set driver and url properties to enable DataSource configuration override. ");
            logger.info(logResult.toString());
        }
    }

    protected boolean shouldConfigureDataSourceFromProperties(
            DataNodeDescriptor nodeDescriptor) {

        String channelName = nodeDescriptor.getDataChannelDescriptor() != null
                ? nodeDescriptor.getDataChannelDescriptor().getName()
                : null;

        String driver = properties.get(Constants.JDBC_DRIVER_PROPERTY);

        if (driver == null && channelName != null) {
            driver = properties.get(getDataNodePropertyName(nodeDescriptor, Constants.JDBC_DRIVER_PROPERTY));
        }

        if (driver == null) {
            findUnusedProperties(nodeDescriptor);
            return false;
        }

        String url = properties.get(Constants.JDBC_URL_PROPERTY);

        if (url == null && channelName != null) {
            url = properties.get(getDataNodePropertyName(nodeDescriptor, Constants.JDBC_URL_PROPERTY));
        }

        if (url == null) {
            findUnusedProperties(nodeDescriptor);
            return false;
        }

        logger
                .info(String
                        .format(
                                "Found DataSourceFactory system property overrides for URL and Driver "
                                        + "of '%s.%s' node. Will ignore project DataSource configuration.",
                                channelName,
                                nodeDescriptor.getName()));
        return true;
    }

}
