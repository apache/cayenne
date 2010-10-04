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
import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.ashwood.AshwoodEntitySorter;
import org.apache.cayenne.configuration.AdhocObjectFactory;
import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.DefaultAdhocObjectFactory;
import org.apache.cayenne.configuration.DefaultConfigurationNameMapper;
import org.apache.cayenne.configuration.DefaultRuntimeProperties;
import org.apache.cayenne.configuration.ObjectContextFactory;
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
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
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

    /**
     * A property defining the location of the runtime configuration XML resource or file.
     */
    public static final String CONFIGURATION_LOCATION = "cayenne.config.location";

    protected String configurationLocation;

    public ServerModule(String configurationLocation) {
        this.configurationLocation = configurationLocation;
    }

    public void configure(Binder binder) {

        // configure global stack properties
        binder.bindMap(DefaultRuntimeProperties.PROPERTIES_MAP).put(
                ServerModule.CONFIGURATION_LOCATION,
                configurationLocation);

        CommonsJdbcEventLogger logger = new CommonsJdbcEventLogger();
        QueryLogger.setLogger(logger);
        binder.bind(JdbcEventLogger.class).toInstance(logger);

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

        binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
        binder.bind(ConfigurationNameMapper.class).to(
                DefaultConfigurationNameMapper.class);

        binder.bind(EventManager.class).to(DefaultEventManager.class);

        // a service to provide the main stack DataDomain
        binder.bind(DataDomain.class).toProvider(DataDomainProvider.class);

        // will return DataDomain for request for a DataChannel
        binder.bind(DataChannel.class).toProvider(DomainDataChannelProvider.class);

        binder.bind(ObjectContextFactory.class).to(DataContextFactory.class);

        // a service to load project XML descriptors
        binder.bind(DataChannelDescriptorLoader.class).to(
                XMLDataChannelDescriptorLoader.class);

        // a service to load DataMap XML descriptors
        binder.bind(DataMapLoader.class).to(XMLDataMapLoader.class);

        // a locator of resources, such as XML descriptors
        binder.bind(ResourceLocator.class).to(ClassLoaderResourceLocator.class);

        // a global properties object
        binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);

        // a service to load DataSourceFactories
        binder.bind(DataSourceFactoryLoader.class).to(
                DefaultDataSourceFactoryLoader.class);

        // a default SchemaUpdateStrategy (used when no explicit strategy is specified in
        // XML)
        binder.bind(SchemaUpdateStrategy.class).to(SkipSchemaUpdateStrategy.class);

        // a default DBAdapterFactory used to load custom and automatic DbAdapters
        binder.bind(DbAdapterFactory.class).to(DefaultDbAdapterFactory.class);

        // binding AshwoodEntitySorter without scope, as this is a stateful object and is
        // configured by the owning domain
        binder.bind(EntitySorter.class).to(AshwoodEntitySorter.class).withoutScope();
    }
}
