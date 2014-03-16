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

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DefaultObjectMapRetainStrategy;
import org.apache.cayenne.access.ObjectMapRetainStrategy;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.access.jdbc.reader.DefaultRowReaderFactory;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.batch.BatchTranslatorFactory;
import org.apache.cayenne.access.translator.batch.DefaultBatchTranslatorFactory;
import org.apache.cayenne.access.types.BigDecimalType;
import org.apache.cayenne.access.types.BigIntegerType;
import org.apache.cayenne.access.types.BooleanType;
import org.apache.cayenne.access.types.ByteArrayType;
import org.apache.cayenne.access.types.ByteType;
import org.apache.cayenne.access.types.CalendarType;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.DateType;
import org.apache.cayenne.access.types.DoubleType;
import org.apache.cayenne.access.types.FloatType;
import org.apache.cayenne.access.types.IntegerType;
import org.apache.cayenne.access.types.LongType;
import org.apache.cayenne.access.types.ShortType;
import org.apache.cayenne.access.types.TimeType;
import org.apache.cayenne.access.types.TimestampType;
import org.apache.cayenne.access.types.UUIDType;
import org.apache.cayenne.access.types.UtilDateType;
import org.apache.cayenne.access.types.VoidType;
import org.apache.cayenne.ashwood.AshwoodEntitySorter;
import org.apache.cayenne.cache.MapQueryCacheProvider;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.Constants;
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
import org.apache.cayenne.dba.firebird.FirebirdSniffer;
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
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.event.DefaultEventManager;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.log.CommonsJdbcEventLogger;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.resource.ClassLoaderResourceLocator;
import org.apache.cayenne.resource.ResourceLocator;
import org.apache.cayenne.tx.DefaultTransactionManager;
import org.apache.cayenne.tx.TransactionManager;

/**
 * A DI module containing all Cayenne server runtime configuration.
 * 
 * @since 3.1
 */
public class ServerModule implements Module {

    private static final int DEFAULT_MAX_ID_QUALIFIER_SIZE = 10000;

    protected String[] configurationLocations;

    /**
     * Creates a ServerModule with at least one configuration location. For multi-module
     * projects additional locations can be specified as well.
     */
    public ServerModule(String... configurationLocations) {

        if (configurationLocations == null) {
            configurationLocations = new String[0];
        }

        this.configurationLocations = configurationLocations;
    }

    public void configure(Binder binder) {

        // configure global stack properties
        binder.bindMap(Constants.PROPERTIES_MAP)
            .put(Constants.SERVER_MAX_ID_QUALIFIER_SIZE_PROPERTY, String.valueOf(DEFAULT_MAX_ID_QUALIFIER_SIZE));

        binder.bind(JdbcEventLogger.class).to(CommonsJdbcEventLogger.class);  
        binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
        binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);

        // configure known DbAdapter detectors in reverse order of popularity. Users can
        // add their own to install custom adapters automatically
        
        // a bit ugly - need to bind all sniffers explicitly first before placing then in a list
        binder.bind(FirebirdSniffer.class).to(FirebirdSniffer.class);
        binder.bind(OpenBaseSniffer.class).to(OpenBaseSniffer.class);
        binder.bind(FrontBaseSniffer.class).to(FrontBaseSniffer.class);
        binder.bind(IngresSniffer.class).to(IngresSniffer.class);
        binder.bind(SQLiteSniffer.class).to(SQLiteSniffer.class);
        binder.bind(DB2Sniffer.class).to(DB2Sniffer.class);
        binder.bind(H2Sniffer.class).to(H2Sniffer.class);
        binder.bind(HSQLDBSniffer.class).to(HSQLDBSniffer.class);
        binder.bind(SybaseSniffer.class).to(SybaseSniffer.class);
        binder.bind(DerbySniffer.class).to(DerbySniffer.class);
        binder.bind(SQLServerSniffer.class).to(SQLServerSniffer.class);
        binder.bind(OracleSniffer.class).to(OracleSniffer.class);
        binder.bind(PostgresSniffer.class).to(PostgresSniffer.class);
        binder.bind(MySQLSniffer.class).to(MySQLSniffer.class);
        
        binder.bindList(Constants.SERVER_ADAPTER_DETECTORS_LIST)
                .add(FirebirdSniffer.class)
                .add(OpenBaseSniffer.class)
                .add(FrontBaseSniffer.class)
                .add(IngresSniffer.class)
                .add(SQLiteSniffer.class)
                .add(DB2Sniffer.class)
                .add(H2Sniffer.class)
                .add(HSQLDBSniffer.class)
                .add(SybaseSniffer.class)
                .add(DerbySniffer.class)
                .add(SQLServerSniffer.class)
                .add(OracleSniffer.class)
                .add(PostgresSniffer.class)
                .add(MySQLSniffer.class);

        // configure an empty filter chain
        binder.bindList(Constants.SERVER_DOMAIN_FILTERS_LIST);
        
        // configure extended types
        binder
                .bindList(Constants.SERVER_DEFAULT_TYPES_LIST)
                .add(new VoidType())
                .add(new BigDecimalType())
                .add(new BigIntegerType())
                .add(new BooleanType())
                .add(new ByteArrayType(false, true))
                .add(new ByteType(false))
                .add(new CharType(false, true))
                .add(new DateType())
                .add(new DoubleType())
                .add(new FloatType())
                .add(new IntegerType())
                .add(new LongType())
                .add(new ShortType(false))
                .add(new TimeType())
                .add(new TimestampType())
                .add(new UtilDateType())
                .add(new CalendarType<GregorianCalendar>(GregorianCalendar.class))
                .add(new CalendarType<Calendar>(Calendar.class))
                .add(new UUIDType()); 
        binder.bindList(Constants.SERVER_USER_TYPES_LIST);
        binder.bindList(Constants.SERVER_TYPE_FACTORIES_LIST);

        // configure explicit configurations
        ListBuilder<Object> locationsListBuilder = binder
                .bindList(Constants.SERVER_PROJECT_LOCATIONS_LIST);
        for (String location : configurationLocations) {
            locationsListBuilder.add(location);
        }

        binder.bind(ConfigurationNameMapper.class).to(
                DefaultConfigurationNameMapper.class);

        binder.bind(EventManager.class).to(DefaultEventManager.class);

        binder.bind(QueryCache.class).toProvider(MapQueryCacheProvider.class);

        // a service to provide the main stack DataDomain
        binder.bind(DataDomain.class).toProvider(DataDomainProvider.class);
        
        binder.bind(DataNodeFactory.class).to(DefaultDataNodeFactory.class);

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

        binder.bind(BatchTranslatorFactory.class).to(
                DefaultBatchTranslatorFactory.class);

        // a default ObjectMapRetainStrategy used to create objects map for ObjectStore
        binder.bind(ObjectMapRetainStrategy.class).to(
                DefaultObjectMapRetainStrategy.class);

        // a default ObjectStoreFactory used to create ObjectStores for contexts
        binder.bind(ObjectStoreFactory.class).to(DefaultObjectStoreFactory.class);
        
        binder.bind(TransactionManager.class).to(DefaultTransactionManager.class);
        binder.bind(RowReaderFactory.class).to(DefaultRowReaderFactory.class);
    }
}
