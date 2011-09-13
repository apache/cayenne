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

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DefaultObjectMapRetainStrategy;
import org.apache.cayenne.access.ObjectMapRetainStrategy;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.access.jdbc.BatchQueryBuilderFactory;
import org.apache.cayenne.access.jdbc.DefaultBatchQueryBuilderFactory;
import org.apache.cayenne.ashwood.AshwoodEntitySorter;
import org.apache.cayenne.cache.MapQueryCacheProvider;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.configuration.DataChannelDescriptorMerger;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.DefaultConfigurationNameMapper;
import org.apache.cayenne.configuration.DefaultDataChannelDescriptorMerger;
import org.apache.cayenne.configuration.DefaultObjectStoreFactory;
import org.apache.cayenne.configuration.DefaultRuntimeProperties;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.configuration.ObjectStoreFactory;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.configuration.XMLDataChannelDescriptorLoader;
import org.apache.cayenne.configuration.XMLDataMapLoader;
import org.apache.cayenne.dba.db2.DB2Sniffer;
import org.apache.cayenne.dba.derby.DerbySniffer;
import org.apache.cayenne.dba.frontbase.FrontBaseSniffer;
import org.apache.cayenne.dba.h2.H2Sniffer;
import org.apache.cayenne.dba.hsqldb.HSQLDBSniffer;
import org.apache.cayenne.dba.ingres.IngresSniffer;
import org.apache.cayenne.dba.mysql.MySQLSniffer;
import org.apache.cayenne.dba.openbase.OpenBaseSniffer;
import org.apache.cayenne.dba.oracle.OracleSniffer;
import org.apache.cayenne.dba.postgres.PostgresSniffer;
import org.apache.cayenne.dba.sqlite.SQLiteSniffer;
import org.apache.cayenne.dba.sqlserver.SQLServerSniffer;
import org.apache.cayenne.dba.sybase.SybaseSniffer;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.event.DefaultEventManager;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.log.CommonsJdbcEventLogger;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.resource.ClassLoaderResourceLocator;
import org.apache.cayenne.resource.ResourceLocator;

/**
 * A DI module containing all Cayenne server runtime configuration.
 * 
 * @since 3.1
 */
public class ServerModule implements Module {

    protected String[] configurationLocations;

    /**
     * Creates a ServerModule with at least one configuration location. For multi-module
     * projects additional locations can be specified as well.
     */
    public ServerModule(String... configurationLocations) {

        if (configurationLocations == null) {
            throw new NullPointerException("Null configurationLocations");
        }

        if (configurationLocations.length < 1) {
            throw new IllegalArgumentException("Empty configurationLocations");
        }

        this.configurationLocations = configurationLocations;
    }

    public void configure(Binder binder) {

        // configure empty global stack properties
        binder.bindMap(DefaultRuntimeProperties.PROPERTIES_MAP);

        binder.bind(JdbcEventLogger.class).to(CommonsJdbcEventLogger.class);

        // configure known DbAdapter detectors in reverse order of popularity. Users can
        // add their own to install custom adapters automatically
        binder
                .bindList(DefaultDbAdapterFactory.DETECTORS_LIST)
                .add(new OpenBaseSniffer())
                .add(new FrontBaseSniffer())
                .add(new IngresSniffer())
                .add(new SQLiteSniffer())
                .add(new DB2Sniffer())
                .add(new H2Sniffer())
                .add(new HSQLDBSniffer())
                .add(new SybaseSniffer())
                .add(new DerbySniffer())
                .add(new SQLServerSniffer())
                .add(new OracleSniffer())
                .add(new PostgresSniffer())
                .add(new MySQLSniffer());

        // configure an empty filter chain
        binder.bindList(DataDomainProvider.FILTERS_LIST);

        // configure explicit configurations
        ListBuilder<Object> locationsListBuilder = binder
                .bindList(DataDomainProvider.LOCATIONS_LIST);
        for (String location : configurationLocations) {
            locationsListBuilder.add(location);
        }

        binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
        binder.bind(ConfigurationNameMapper.class).to(
                DefaultConfigurationNameMapper.class);

        binder.bind(EventManager.class).to(DefaultEventManager.class);

        binder.bind(QueryCache.class).toProvider(MapQueryCacheProvider.class);

        // a service to provide the main stack DataDomain
        binder.bind(DataDomain.class).toProvider(DataDomainProvider.class);

        // will return DataDomain for request for a DataChannel
        binder.bind(DataChannel.class).toProvider(DomainDataChannelProvider.class);

        binder.bind(ObjectContextFactory.class).to(DataContextFactory.class);

        // a service to load project XML descriptors
        binder.bind(DataChannelDescriptorLoader.class).to(
                XMLDataChannelDescriptorLoader.class);
        binder.bind(DataChannelDescriptorMerger.class).to(
                DefaultDataChannelDescriptorMerger.class);

        // a service to load DataMap XML descriptors
        binder.bind(DataMapLoader.class).to(XMLDataMapLoader.class);

        // a locator of resources, such as XML descriptors
        binder.bind(ResourceLocator.class).to(ClassLoaderResourceLocator.class);

        // a global properties object
        binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);

        // a service to load DataSourceFactories. DelegatingDataSourceFactory will attempt
        // to find the actual worker factory dynamically on each call depending on
        // DataNodeDescriptor data and the environment
        binder.bind(DataSourceFactory.class).to(DelegatingDataSourceFactory.class);

        // a default SchemaUpdateStrategy (used when no explicit strategy is specified in
        // XML)
        binder.bind(SchemaUpdateStrategy.class).to(SkipSchemaUpdateStrategy.class);

        // a default DBAdapterFactory used to load custom and automatic DbAdapters
        binder.bind(DbAdapterFactory.class).to(DefaultDbAdapterFactory.class);

        // binding AshwoodEntitySorter without scope, as this is a stateful object and is
        // configured by the owning domain
        binder.bind(EntitySorter.class).to(AshwoodEntitySorter.class).withoutScope();

        binder.bind(BatchQueryBuilderFactory.class).to(
                DefaultBatchQueryBuilderFactory.class);

        // a default ObjectMapRetainStrategy used to create objects map for ObjectStore
        binder.bind(ObjectMapRetainStrategy.class).to(
                DefaultObjectMapRetainStrategy.class);

        // a default ObjectStoreFactory used to create ObjectStores for contexts
        binder.bind(ObjectStoreFactory.class).to(DefaultObjectStoreFactory.class);
    }
}
