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
package org.apache.cayenne.runtime;

import java.util.Collection;

import javax.sql.DataSource;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.configuration.AdhocObjectFactory;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.DataSourceFactory;
import org.apache.cayenne.configuration.DataSourceFactoryLoader;
import org.apache.cayenne.configuration.DbAdapterFactory;
import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.ResourceLocator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link DataChannel} provider that provides a single instance of DataDomain configured
 * per configuration supplied via injected {@link DataChannelDescriptorLoader}.
 * 
 * @since 3.1
 */
public class DataDomainProvider implements Provider<DataDomain> {

    private static Log logger = LogFactory.getLog(DataDomainProvider.class);

    @Inject
    protected ResourceLocator resourceLocator;

    @Inject
    protected DataChannelDescriptorLoader loader;

    @Inject
    protected RuntimeProperties configurationProperties;

    @Inject
    protected SchemaUpdateStrategy defaultSchemaUpdateStrategy;

    @Inject
    protected DbAdapterFactory adapterFactory;

    @Inject
    protected DataSourceFactoryLoader dataSourceFactoryLoader;

    @Inject
    protected AdhocObjectFactory objectFactory;

    @Inject
    protected ConfigurationNameMapper nameMapper;

    protected volatile DataDomain dataDomain;

    public DataDomain get() throws ConfigurationException {

        if (dataDomain == null) {
            synchronized (this) {
                if (dataDomain == null) {

                    try {
                        createDataChannel();
                    }
                    catch (ConfigurationException e) {
                        throw e;
                    }
                    catch (Exception e) {
                        throw new ConfigurationException(
                                "Error loading DataChannel: '%s'",
                                e,
                                e.getMessage());
                    }
                }
            }
        }

        return dataDomain;
    }

    protected void createDataChannel() throws Exception {
        String runtimeName = configurationProperties
                .get(RuntimeProperties.CAYENNE_RUNTIME_NAME);

        long t0 = System.currentTimeMillis();
        if (logger.isDebugEnabled()) {
            logger.debug("starting configuration loading: " + runtimeName);
        }

        String resourceName = nameMapper.configurationLocation(
                DataChannelDescriptor.class,
                runtimeName);
        Collection<Resource> configurations = resourceLocator.findResources(resourceName);

        if (configurations.isEmpty()) {
            throw new ConfigurationException(
                    "Configuration file \"%s\" is not found.",
                    resourceName);
        }

        Resource configurationResource = configurations.iterator().next();

        // no support for multiple configs yet, but this is not a hard error
        if (configurations.size() > 1) {
            logger.info("found "
                    + configurations.size()
                    + " configurations, will use the first one: "
                    + configurationResource.getURL());
        }

        DataChannelDescriptor descriptor = loader.load(configurationResource);
        long t1 = System.currentTimeMillis();

        if (logger.isDebugEnabled()) {
            logger.debug("finished configuration loading: "
                    + runtimeName
                    + " in "
                    + (t1 - t0)
                    + " ms.");
        }

        DataDomain dataDomain = new DataDomain(descriptor.getName());
        dataDomain.initWithProperties(descriptor.getProperties());

        for (DataMap dataMap : descriptor.getDataMaps()) {
            dataDomain.addMap(dataMap);
        }

        for (DataNodeDescriptor nodeDescriptor : descriptor.getDataNodeDescriptors()) {
            DataNode dataNode = new DataNode(nodeDescriptor.getName());

            dataNode.setDataSourceLocation(nodeDescriptor.getParameters());

            DataSourceFactory dataSourceFactory = dataSourceFactoryLoader
                    .getDataSourceFactory(nodeDescriptor);

            DataSource dataSource = dataSourceFactory.getDataSource(nodeDescriptor);

            dataNode.setDataSourceFactory(nodeDescriptor.getDataSourceFactoryType());
            dataNode.setDataSource(dataSource);

            // schema update strategy
            String schemaUpdateStrategyType = nodeDescriptor
                    .getSchemaUpdateStrategyType();

            if (schemaUpdateStrategyType == null) {
                dataNode.setSchemaUpdateStrategy(defaultSchemaUpdateStrategy);
                dataNode.setSchemaUpdateStrategyName(defaultSchemaUpdateStrategy
                        .getClass()
                        .getName());
            }
            else {
                SchemaUpdateStrategy strategy = objectFactory.newInstance(
                        SchemaUpdateStrategy.class,
                        schemaUpdateStrategyType);
                dataNode.setSchemaUpdateStrategyName(schemaUpdateStrategyType);
                dataNode.setSchemaUpdateStrategy(strategy);
            }

            // DbAdapter
            dataNode.setAdapter(adapterFactory.createAdapter(nodeDescriptor, dataSource));

            // DataMaps
            for (String dataMapName : nodeDescriptor.getDataMapNames()) {
                dataNode.addDataMap(dataDomain.getMap(dataMapName));
            }

            dataDomain.addNode(dataNode);
        }

        this.dataDomain = dataDomain;
    }
}
