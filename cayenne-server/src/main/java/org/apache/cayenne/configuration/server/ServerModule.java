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

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.DataChannelQueryFilter;
import org.apache.cayenne.DataChannelSyncFilter;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataRowStoreFactory;
import org.apache.cayenne.access.DefaultDataRowStoreFactory;
import org.apache.cayenne.access.DefaultObjectMapRetainStrategy;
import org.apache.cayenne.access.ObjectMapRetainStrategy;
import org.apache.cayenne.access.dbsync.DefaultSchemaUpdateStrategyFactory;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategyFactory;
import org.apache.cayenne.access.flush.DataDomainFlushActionFactory;
import org.apache.cayenne.access.flush.DefaultDataDomainFlushActionFactory;
import org.apache.cayenne.access.flush.operation.DbRowOpSorter;
import org.apache.cayenne.access.flush.operation.DefaultDbRowOpSorter;
import org.apache.cayenne.access.jdbc.SQLTemplateProcessor;
import org.apache.cayenne.access.jdbc.reader.DefaultRowReaderFactory;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.batch.BatchTranslatorFactory;
import org.apache.cayenne.access.translator.batch.DefaultBatchTranslatorFactory;
import org.apache.cayenne.access.translator.select.DefaultSelectTranslatorFactory;
import org.apache.cayenne.access.translator.select.SelectTranslatorFactory;
import org.apache.cayenne.access.types.BigDecimalType;
import org.apache.cayenne.access.types.BigDecimalValueType;
import org.apache.cayenne.access.types.BigIntegerValueType;
import org.apache.cayenne.access.types.BooleanType;
import org.apache.cayenne.access.types.ByteArrayType;
import org.apache.cayenne.access.types.ByteType;
import org.apache.cayenne.access.types.CalendarType;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.CharacterValueType;
import org.apache.cayenne.access.types.DateType;
import org.apache.cayenne.access.types.DefaultValueObjectTypeRegistry;
import org.apache.cayenne.access.types.DoubleType;
import org.apache.cayenne.access.types.DurationType;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.FloatType;
import org.apache.cayenne.access.types.GeoJsonType;
import org.apache.cayenne.access.types.IntegerType;
import org.apache.cayenne.access.types.InternalUnsupportedTypeFactory;
import org.apache.cayenne.access.types.LocalDateTimeValueType;
import org.apache.cayenne.access.types.LocalDateValueType;
import org.apache.cayenne.access.types.LocalTimeValueType;
import org.apache.cayenne.access.types.LongType;
import org.apache.cayenne.access.types.PeriodValueType;
import org.apache.cayenne.access.types.ShortType;
import org.apache.cayenne.access.types.TimeType;
import org.apache.cayenne.access.types.TimestampType;
import org.apache.cayenne.access.types.UUIDValueType;
import org.apache.cayenne.access.types.UtilDateType;
import org.apache.cayenne.access.types.ValueObjectType;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.access.types.VoidType;
import org.apache.cayenne.access.types.WktType;
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
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.configuration.xml.DefaultHandlerFactory;
import org.apache.cayenne.configuration.xml.HandlerFactory;
import org.apache.cayenne.configuration.xml.NoopDataChannelMetaData;
import org.apache.cayenne.configuration.xml.XMLDataChannelDescriptorLoader;
import org.apache.cayenne.configuration.xml.XMLDataMapLoader;
import org.apache.cayenne.configuration.xml.XMLReaderProvider;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.dba.db2.DB2Adapter;
import org.apache.cayenne.dba.db2.DB2PkGenerator;
import org.apache.cayenne.dba.db2.DB2Sniffer;
import org.apache.cayenne.dba.derby.DerbyAdapter;
import org.apache.cayenne.dba.derby.DerbyPkGenerator;
import org.apache.cayenne.dba.derby.DerbySniffer;
import org.apache.cayenne.dba.firebird.FirebirdSniffer;
import org.apache.cayenne.dba.frontbase.FrontBaseAdapter;
import org.apache.cayenne.dba.frontbase.FrontBasePkGenerator;
import org.apache.cayenne.dba.frontbase.FrontBaseSniffer;
import org.apache.cayenne.dba.h2.H2Adapter;
import org.apache.cayenne.dba.h2.H2PkGenerator;
import org.apache.cayenne.dba.h2.H2Sniffer;
import org.apache.cayenne.dba.hsqldb.HSQLDBSniffer;
import org.apache.cayenne.dba.ingres.IngresAdapter;
import org.apache.cayenne.dba.ingres.IngresPkGenerator;
import org.apache.cayenne.dba.ingres.IngresSniffer;
import org.apache.cayenne.dba.mariadb.MariaDBSniffer;
import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.apache.cayenne.dba.mysql.MySQLPkGenerator;
import org.apache.cayenne.dba.mysql.MySQLSniffer;
import org.apache.cayenne.dba.openbase.OpenBaseAdapter;
import org.apache.cayenne.dba.openbase.OpenBasePkGenerator;
import org.apache.cayenne.dba.openbase.OpenBaseSniffer;
import org.apache.cayenne.dba.oracle.Oracle8Adapter;
import org.apache.cayenne.dba.oracle.OracleAdapter;
import org.apache.cayenne.dba.oracle.OraclePkGenerator;
import org.apache.cayenne.dba.oracle.OracleSniffer;
import org.apache.cayenne.dba.postgres.PostgresAdapter;
import org.apache.cayenne.dba.postgres.PostgresPkGenerator;
import org.apache.cayenne.dba.postgres.PostgresSniffer;
import org.apache.cayenne.dba.sqlite.SQLiteSniffer;
import org.apache.cayenne.dba.sqlserver.SQLServerAdapter;
import org.apache.cayenne.dba.sqlserver.SQLServerSniffer;
import org.apache.cayenne.dba.sybase.SybaseAdapter;
import org.apache.cayenne.dba.sybase.SybasePkGenerator;
import org.apache.cayenne.dba.sybase.SybaseSniffer;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.MapBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.event.EventBridge;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.event.EventManagerProvider;
import org.apache.cayenne.event.NoopEventBridgeProvider;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.log.Slf4jJdbcEventLogger;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.reflect.generic.ValueComparisonStrategyFactory;
import org.apache.cayenne.reflect.generic.DefaultValueComparisonStrategyFactory;
import org.apache.cayenne.resource.ClassLoaderResourceLocator;
import org.apache.cayenne.resource.ResourceLocator;
import org.apache.cayenne.template.CayenneSQLTemplateProcessor;
import org.apache.cayenne.template.DefaultTemplateContextFactory;
import org.apache.cayenne.template.TemplateContextFactory;
import org.apache.cayenne.tx.DefaultTransactionFactory;
import org.apache.cayenne.tx.DefaultTransactionManager;
import org.apache.cayenne.tx.TransactionFactory;
import org.apache.cayenne.tx.TransactionFilter;
import org.apache.cayenne.tx.TransactionManager;
import org.xml.sax.XMLReader;

/**
 * A DI module containing all Cayenne server runtime configuration.
 *
 * @since 3.1
 */
public class ServerModule implements Module {

    private static final int DEFAULT_MAX_ID_QUALIFIER_SIZE = 10000;

    /**
     * Sets transaction management to either external or internal transactions. Default is internally-managed transactions.
     *
     * @param binder      DI binder passed to the module during injector startup.
     * @param useExternal whether external (true) or internal (false) transaction management should be used.
     * @since 4.0
     */
    public static void useExternalTransactions(Binder binder, boolean useExternal) {
        contributeProperties(binder).put(Constants.SERVER_EXTERNAL_TX_PROPERTY, String.valueOf(useExternal));
    }

    /**
     * Sets max size of snapshot cache, in pre 4.0 version this was set in the Modeler.
     *
     * @param binder DI binder passed to the module during injector startup.
     * @param size   max size of snapshot cache
     * @since 4.0
     */
    public static void setSnapshotCacheSize(Binder binder, int size) {
        contributeProperties(binder).put(Constants.SNAPSHOT_CACHE_SIZE_PROPERTY, Integer.toString(size));
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
        return binder.bindList(String.class, Constants.SERVER_PROJECT_LOCATIONS_LIST);
    }

    /**
     * Provides access to a DI collection builder for {@link DataChannelFilter}'s that allows downstream modules to
     * "contribute" their own DataDomain filters
     *
     * @param binder DI binder passed to the module during injector startup.
     * @return ListBuilder for DataChannelFilter.
     * @since 4.0
     * @deprecated since 4.1 use {@link #contributeDomainQueryFilters(Binder)} and {@link #contributeDomainSyncFilters(Binder)}
     */
    @Deprecated
    public static ListBuilder<DataChannelFilter> contributeDomainFilters(Binder binder) {
        return binder.bindList(DataChannelFilter.class, Constants.SERVER_DOMAIN_FILTERS_LIST);
    }

    /**
     * Provides access to a DI collection builder for {@link DataChannelQueryFilter}'s that allows downstream modules to
     * "contribute" their own DataDomain query filters
     *
     * @param binder DI binder passed to the module during injector startup.
     * @return ListBuilder for DataChannelQueryFilter.
     * @since 4.1
     */
    public static ListBuilder<DataChannelQueryFilter> contributeDomainQueryFilters(Binder binder) {
        return binder.bindList(DataChannelQueryFilter.class);
    }

    /**
     * Provides access to a DI collection builder for {@link DataChannelSyncFilter}'s that allows downstream modules to
     * "contribute" their own DataDomain sync filters
     *
     * @param binder DI binder passed to the module during injector startup.
     * @return ListBuilder for DataChannelSyncFilter.
     * @since 4.1
     */
    public static ListBuilder<DataChannelSyncFilter> contributeDomainSyncFilters(Binder binder) {
        return binder.bindList(DataChannelSyncFilter.class);
    }

    /**
     * Provides access to a DI collection builder for lifecycle events listeners.
     *
     * @param binder DI binder passed to the module during injector startup.
     * @return ListBuilder for listener Objects.
     * @since 4.0
     */
    public static ListBuilder<Object> contributeDomainListeners(Binder binder) {
        return binder.bindList(Object.class, Constants.SERVER_DOMAIN_LISTENERS_LIST);
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
        return binder.bindList(DbAdapterDetector.class, Constants.SERVER_ADAPTER_DETECTORS_LIST);
    }

    /**
     * Provides access to a DI map builder for {@link PkGenerator}'s that allows downstream modules to
     * "contribute" their own pk generators.
     *
     * @param binder DI binder passed to the module during injector startup.
     * @return MapBuilder for properties.
     * @since 4.1
     */
    public static MapBuilder<PkGenerator> contributePkGenerators(Binder binder) {
        return binder.bindMap(PkGenerator.class);
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
        return binder.bindMap(String.class, Constants.PROPERTIES_MAP);
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
        return binder.bindList(ExtendedTypeFactory.class, Constants.SERVER_TYPE_FACTORIES_LIST);
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
        return binder.bindList(ExtendedType.class, Constants.SERVER_DEFAULT_TYPES_LIST);
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
        return binder.bindList(ExtendedType.class, Constants.SERVER_USER_TYPES_LIST);
    }

    /**
     * @param binder DI binder passed to module during injector startup
     * @return ListBuilder for user-contributed ValueObjectTypes
     * @since 4.0
     */
    public static ListBuilder<ValueObjectType> contributeValueObjectTypes(Binder binder) {
        return binder.bindList(ValueObjectType.class);
    }

    /**
     * Creates a new {@link ServerModule}.
     *
     * @since 4.0
     */
    public ServerModule() {
    }

    public void configure(Binder binder) {

        // configure global stack properties
        contributeProperties(binder)
                .put(Constants.SERVER_MAX_ID_QUALIFIER_SIZE_PROPERTY, String.valueOf(DEFAULT_MAX_ID_QUALIFIER_SIZE));
        contributeProperties(binder)
                .put(Constants.SERVER_CONTEXTS_SYNC_PROPERTY, String.valueOf(false));

        binder.bind(JdbcEventLogger.class).to(Slf4jJdbcEventLogger.class);
        binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
        binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);

        // configure known DbAdapter detectors in reverse order of popularity.
        // Users can add their own to install custom adapters automatically

        contributeAdapterDetectors(binder)
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
                .add(MySQLSniffer.class)
                .add(MariaDBSniffer.class);

        //installing Pk for adapters
        binder.bind(PkGeneratorFactoryProvider.class).to(PkGeneratorFactoryProvider.class);
        binder.bind(PkGenerator.class).to(JdbcPkGenerator.class);

        //set PkGenerators for current Adapters
        contributePkGenerators(binder)
                .put(DB2Adapter.class.getName(), DB2PkGenerator.class)
                .put(DerbyAdapter.class.getName(), DerbyPkGenerator.class)
                .put(FrontBaseAdapter.class.getName(), FrontBasePkGenerator.class)
                .put(H2Adapter.class.getName(), H2PkGenerator.class)
                .put(IngresAdapter.class.getName(), IngresPkGenerator.class)
                .put(MySQLAdapter.class.getName(), MySQLPkGenerator.class)
                .put(OpenBaseAdapter.class.getName(), OpenBasePkGenerator.class)
                .put(OracleAdapter.class.getName(), OraclePkGenerator.class)
                .put(Oracle8Adapter.class.getName(), OraclePkGenerator.class)
                .put(PostgresAdapter.class.getName(), PostgresPkGenerator.class)
                .put(SQLServerAdapter.class.getName(), SybasePkGenerator.class)
                .put(SybaseAdapter.class.getName(), SybasePkGenerator.class);

        // configure a filter chain with only one TransactionFilter as default
        contributeDomainFilters(binder);
        contributeDomainQueryFilters(binder);
        contributeDomainSyncFilters(binder).add(TransactionFilter.class);

        // init listener list
        contributeDomainListeners(binder);

        // configure extended types
        contributeDefaultTypes(binder)
                .add(new VoidType())
                .add(new BigDecimalType())
                .add(new BooleanType())
                .add(new ByteType(false))
                .add(new CharType(false, true))
                .add(new DoubleType())
                .add(new FloatType())
                .add(new IntegerType())
                .add(new LongType())
                .add(new ShortType(false))
                .add(new ByteArrayType(false, true))
                .add(new DateType())
                .add(new TimeType())
                .add(new TimestampType())
                .add(new DurationType())
                // should be converted from ExtendedType to ValueType
                .add(new UtilDateType())
                .add(new CalendarType<>(GregorianCalendar.class))
                .add(new CalendarType<>(Calendar.class))
                // non-standard types
                .add(GeoJsonType.class)
                .add(WktType.class);
        contributeUserTypes(binder);
        contributeTypeFactories(binder)
                .add(new InternalUnsupportedTypeFactory());

        // Custom ValueObjects types contribution
        contributeValueObjectTypes(binder)
                .add(BigIntegerValueType.class)
                .add(BigDecimalValueType.class)
                .add(UUIDValueType.class)
                .add(LocalDateValueType.class)
                .add(LocalTimeValueType.class)
                .add(LocalDateTimeValueType.class)
                .add(PeriodValueType.class)
                .add(CharacterValueType.class);

        binder.bind(ValueObjectTypeRegistry.class).to(DefaultValueObjectTypeRegistry.class);
        binder.bind(ValueComparisonStrategyFactory.class).to(DefaultValueComparisonStrategyFactory.class);

        // configure explicit configurations
        contributeProjectLocations(binder);

        binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);

        binder.bind(EventManager.class).toProvider(EventManagerProvider.class);

        binder.bind(QueryCache.class).toProvider(MapQueryCacheProvider.class);

        binder.bind(EventBridge.class).toProvider(NoopEventBridgeProvider.class);

        binder.bind(DataRowStoreFactory.class).to(DefaultDataRowStoreFactory.class);

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

        // a default DBAdapterFactory used to load custom and automatic DbAdapters
        binder.bind(DbAdapterFactory.class).to(DefaultDbAdapterFactory.class);

        // binding AshwoodEntitySorter without scope, as this is a stateful object and is
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

        binder.bind(SQLTemplateProcessor.class).to(CayenneSQLTemplateProcessor.class);
        binder.bind(TemplateContextFactory.class).to(DefaultTemplateContextFactory.class);

        binder.bind(HandlerFactory.class).to(DefaultHandlerFactory.class);
        binder.bind(DataChannelMetaData.class).to(NoopDataChannelMetaData.class);
        binder.bind(XMLReader.class).toProviderInstance(new XMLReaderProvider(false)).withoutScope();

        binder.bind(DataDomainFlushActionFactory.class).to(DefaultDataDomainFlushActionFactory.class);
        binder.bind(DbRowOpSorter.class).to(DefaultDbRowOpSorter.class);
    }
}
