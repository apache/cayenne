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
import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DefaultObjectMapRetainStrategy;
import org.apache.cayenne.access.ObjectMapRetainStrategy;
import org.apache.cayenne.access.dbsync.DefaultSchemaUpdateStrategyFactory;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategyFactory;
import org.apache.cayenne.access.jdbc.SQLTemplateProcessor;
import org.apache.cayenne.access.jdbc.reader.DefaultRowReaderFactory;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.batch.BatchTranslatorFactory;
import org.apache.cayenne.access.translator.batch.DefaultBatchTranslatorFactory;
import org.apache.cayenne.access.translator.select.DefaultSelectTranslatorFactory;
import org.apache.cayenne.access.translator.select.SelectTranslatorFactory;
import org.apache.cayenne.access.types.*;
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
import org.apache.cayenne.di.*;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.event.DefaultEventManager;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.log.CommonsJdbcEventLogger;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.resource.ClassLoaderResourceLocator;
import org.apache.cayenne.resource.ResourceLocator;
import org.apache.cayenne.tx.DefaultTransactionFactory;
import org.apache.cayenne.tx.DefaultTransactionManager;
import org.apache.cayenne.tx.TransactionFactory;
import org.apache.cayenne.tx.TransactionFilter;
import org.apache.cayenne.tx.TransactionManager;
import org.apache.cayenne.velocity.VelocitySQLTemplateProcessor;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * A DI module containing all Cayenne server runtime configuration.
 *
 * @since 3.1
 */
public class ServerModule implements Module {

    private static final int DEFAULT_MAX_ID_QUALIFIER_SIZE = 10000;

    @Deprecated
    protected String[] configurationLocations;

    /**
     * Sets transaction management to either external or internal transactions. Default is internally-managed transactions.
     *
     * @param binder  DI binder passed to the module during injector startup.
     * @param useExternal whether external (true) or internal (false) transaction management should be used.
     * @since 4.0
     */
    public static void useExternalTransactions(Binder binder, boolean useExternal) {
        contributeProperties(binder).put(Constants.SERVER_EXTERNAL_TX_PROPERTY, String.valueOf(useExternal));
    }

    /**
     * Provides access to a DI collection builder for String locations that allows downstream modules to
     * "contribute" their own Cayenne project locations.
     *
     * @param binder DI binder passed to the module during injector startup.
     * @return ListBuilder for String locations.
     * @since 4.0
     */
    public static ListBuilder<String> contributeProjectLocations(Binder binder) {
        return binder.bindList(Constants.SERVER_PROJECT_LOCATIONS_LIST);
    }

    /**
     * Provides access to a DI collection builder for {@link DataChannelFilter}'s that allows downstream modules to
     * "contribute" their own DataDomain filters
     *
     * @param binder DI binder passed to the module during injector startup.
     * @return ListBuilder for DataChannelFilter.
     * @since 4.0
     */
    public static ListBuilder<DataChannelFilter> contributeDomainFilters(Binder binder) {
        return binder.bindList(Constants.SERVER_DOMAIN_FILTERS_LIST);
    }

    /**
     * Provides access to a DI collection builder for {@link DbAdapterDetector}'s that allows downstream modules to
     * "contribute" their own adapter detectors.
     *
     * @param binder DI binder passed to the module during injector startup.
     * @return ListBuilder for DbAdapterDetectors.
     * @since 4.0
     */
    public static ListBuilder<DbAdapterDetector> contributeAdapterDetectors(Binder binder) {
        return binder.bindList(Constants.SERVER_ADAPTER_DETECTORS_LIST);
    }

    /**
     * Provides access to a DI map builder for runtime properties that allows downstream modules to
     * "contribute" their own properties.
     *
     * @param binder DI binder passed to the module during injector startup.
     * @return MapBuilder for properties.
     * @since 4.0
     */
    public static MapBuilder<String> contributeProperties(Binder binder) {
        return binder.bindMap(Constants.PROPERTIES_MAP);
    }

    /**
     * Provides access to a DI collection builder for {@link ExtendedTypeFactory}'s that allows downstream modules to
     * "contribute" their own factories.
     *
     * @param binder DI binder passed to the module during injector startup.
     * @return ListBuilder for ExtendedTypes.
     * @since 4.0
     */
    public static ListBuilder<ExtendedTypeFactory> contributeTypeFactories(Binder binder) {
        return binder.bindList(Constants.SERVER_TYPE_FACTORIES_LIST);
    }

    /**
     * Provides access to a DI collection builder for default adapter-agnostic {@link ExtendedType}'s that allows
     * downstream modules to "contribute" their own types. "Default" types are loaded before adapter-provided or "user"
     * types, so they may be overridden by those.
     *
     * @param binder DI binder passed to the module during injector startup.
     * @return ListBuilder for ExtendedTypes.
     * @since 4.0
     */
    public static ListBuilder<ExtendedType> contributeDefaultTypes(Binder binder) {
        return binder.bindList(Constants.SERVER_DEFAULT_TYPES_LIST);
    }

    /**
     * Provides access to a DI collection builder for {@link ExtendedType}'s that allows downstream modules to "contribute"
     * their own types. Unlike "default" types (see {@link #contributeDefaultTypes(Binder)}), "user" types are loaded
     * after the adapter-provided types and can override those.
     *
     * @param binder DI binder passed to the module during injector startup.
     * @return ListBuilder for ExtendedTypes.
     * @since 4.0
     */
    public static ListBuilder<ExtendedType> contributeUserTypes(Binder binder) {
        return binder.bindList(Constants.SERVER_USER_TYPES_LIST);
    }

    /**
     * Creates a new {@link ServerModule}.
     *
     * @since 4.0
     */
    public ServerModule() {
        this.configurationLocations = new String[0];
    }

    /**
     * Creates a ServerModule with at least one configuration location. For multi-module projects additional locations
     * can be specified as well.
     *
     * @deprecated since 4.0 use {@link ServerRuntimeBuilder#addConfig(String)} and/or
     * {@link ServerModule#contributeProjectLocations(Binder)} to specify locations.
     */
    @Deprecated
    public ServerModule(String firstConfigLocation, String... configurationLocations) {
        if (configurationLocations == null) {
            configurationLocations = new String[0];
        }

        this.configurationLocations = new String[configurationLocations.length + 1];
        this.configurationLocations[0] = firstConfigLocation;

        if (configurationLocations.length > 0) {
            System.arraycopy(configurationLocations, 0, this.configurationLocations, 1, configurationLocations.length);
        }
    }

    public void configure(Binder binder) {

        // configure global stack properties
        contributeProperties(binder)
                .put(Constants.SERVER_MAX_ID_QUALIFIER_SIZE_PROPERTY, String.valueOf(DEFAULT_MAX_ID_QUALIFIER_SIZE));

        binder.bind(JdbcEventLogger.class).to(CommonsJdbcEventLogger.class);
        binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
        binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);

        // configure known DbAdapter detectors in reverse order of popularity.
        // Users can add their own to install custom adapters automatically

        contributeAdapterDetectors(binder).add(FirebirdSniffer.class).add(OpenBaseSniffer.class)
                .add(FrontBaseSniffer.class).add(IngresSniffer.class).add(SQLiteSniffer.class).add(DB2Sniffer.class)
                .add(H2Sniffer.class).add(HSQLDBSniffer.class).add(SybaseSniffer.class).add(DerbySniffer.class)
                .add(SQLServerSniffer.class).add(OracleSniffer.class).add(PostgresSniffer.class)
                .add(MySQLSniffer.class);

        // configure a filter chain with only one TransactionFilter as default
        contributeDomainFilters(binder).add(TransactionFilter.class);

        // configure extended types
        contributeDefaultTypes(binder).add(new VoidType()).add(new BigDecimalType())
                .add(new BigIntegerType()).add(new BooleanType()).add(new ByteArrayType(false, true))
                .add(new ByteType(false)).add(new CharType(false, true)).add(new DateType()).add(new DoubleType())
                .add(new FloatType()).add(new IntegerType()).add(new LongType()).add(new ShortType(false))
                .add(new TimeType()).add(new TimestampType()).add(new UtilDateType())
                .add(new CalendarType<GregorianCalendar>(GregorianCalendar.class))
                .add(new CalendarType<Calendar>(Calendar.class)).add(new UUIDType());
        contributeUserTypes(binder);
        contributeTypeFactories(binder);

        // configure explicit configurations
        ListBuilder<String> locationsListBuilder = contributeProjectLocations(binder);
        for (String location : configurationLocations) {
            locationsListBuilder.add(location);
        }

        binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);

        binder.bind(EventManager.class).to(DefaultEventManager.class);

        binder.bind(QueryCache.class).toProvider(MapQueryCacheProvider.class);

        // a service to provide the main stack DataDomain
        binder.bind(DataDomain.class).toProvider(DataDomainProvider.class);

        binder.bind(DataNodeFactory.class).to(DefaultDataNodeFactory.class);

        // will return DataDomain for request for a DataChannel
        binder.bind(DataChannel.class).toProvider(DomainDataChannelProvider.class);

        binder.bind(ObjectContextFactory.class).to(DataContextFactory.class);

        binder.bind(TransactionFactory.class).to(DefaultTransactionFactory.class);

        // a service to load project XML descriptors
        binder.bind(DataChannelDescriptorLoader.class).to(XMLDataChannelDescriptorLoader.class);
        binder.bind(DataChannelDescriptorMerger.class).to(DefaultDataChannelDescriptorMerger.class);

        // a service to load DataMap XML descriptors
        binder.bind(DataMapLoader.class).to(XMLDataMapLoader.class);

        // a locator of resources, such as XML descriptors
        binder.bind(ResourceLocator.class).to(ClassLoaderResourceLocator.class);
        binder.bind(Key.get(ResourceLocator.class, Constants.SERVER_RESOURCE_LOCATOR)).to(ClassLoaderResourceLocator.class);

        // a global properties object
        binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);

        // a service to load DataSourceFactories. DelegatingDataSourceFactory
        // will attempt to find the actual worker factory dynamically on each
        // call depending on DataNodeDescriptor data and the environment
        binder.bind(DataSourceFactory.class).to(DelegatingDataSourceFactory.class);

        binder.bind(SchemaUpdateStrategyFactory.class).to(DefaultSchemaUpdateStrategyFactory.class);

        // a default DBAdapterFactory used to load custom and automatic
        // DbAdapters
        binder.bind(DbAdapterFactory.class).to(DefaultDbAdapterFactory.class);

        // binding AshwoodEntitySorter without scope, as this is a stateful
        // object and is
        // configured by the owning domain
        binder.bind(EntitySorter.class).to(AshwoodEntitySorter.class).withoutScope();

        binder.bind(BatchTranslatorFactory.class).to(DefaultBatchTranslatorFactory.class);
        binder.bind(SelectTranslatorFactory.class).to(DefaultSelectTranslatorFactory.class);

        // a default ObjectMapRetainStrategy used to create objects map for
        // ObjectStore
        binder.bind(ObjectMapRetainStrategy.class).to(DefaultObjectMapRetainStrategy.class);

        // a default ObjectStoreFactory used to create ObjectStores for contexts
        binder.bind(ObjectStoreFactory.class).to(DefaultObjectStoreFactory.class);

        binder.bind(TransactionManager.class).to(DefaultTransactionManager.class);
        binder.bind(RowReaderFactory.class).to(DefaultRowReaderFactory.class);

        binder.bind(SQLTemplateProcessor.class).to(VelocitySQLTemplateProcessor.class);
    }
}
