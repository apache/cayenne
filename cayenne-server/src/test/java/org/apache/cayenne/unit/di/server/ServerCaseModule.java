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
package org.apache.cayenne.unit.di.server;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DefaultObjectMapRetainStrategy;
import org.apache.cayenne.access.ObjectMapRetainStrategy;
import org.apache.cayenne.access.translator.batch.BatchTranslatorFactory;
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
import org.apache.cayenne.access.types.FloatType;
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
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.access.types.VoidType;
import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.DefaultConfigurationNameMapper;
import org.apache.cayenne.configuration.DefaultObjectStoreFactory;
import org.apache.cayenne.configuration.DefaultRuntimeProperties;
import org.apache.cayenne.configuration.ObjectStoreFactory;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.configuration.server.PkGeneratorFactoryProvider;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.configuration.xml.DefaultHandlerFactory;
import org.apache.cayenne.configuration.xml.HandlerFactory;
import org.apache.cayenne.configuration.xml.NoopDataChannelMetaData;
import org.apache.cayenne.configuration.xml.XMLDataMapLoader;
import org.apache.cayenne.configuration.xml.XMLReaderProvider;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.dba.db2.DB2Adapter;
import org.apache.cayenne.dba.db2.DB2PkGenerator;
import org.apache.cayenne.dba.derby.DerbyAdapter;
import org.apache.cayenne.dba.derby.DerbyPkGenerator;
import org.apache.cayenne.dba.firebird.FirebirdAdapter;
import org.apache.cayenne.dba.frontbase.FrontBaseAdapter;
import org.apache.cayenne.dba.frontbase.FrontBasePkGenerator;
import org.apache.cayenne.dba.h2.H2Adapter;
import org.apache.cayenne.dba.h2.H2PkGenerator;
import org.apache.cayenne.dba.hsqldb.HSQLDBAdapter;
import org.apache.cayenne.dba.ingres.IngresAdapter;
import org.apache.cayenne.dba.ingres.IngresPkGenerator;
import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.apache.cayenne.dba.mysql.MySQLPkGenerator;
import org.apache.cayenne.dba.openbase.OpenBaseAdapter;
import org.apache.cayenne.dba.openbase.OpenBasePkGenerator;
import org.apache.cayenne.dba.oracle.Oracle8Adapter;
import org.apache.cayenne.dba.oracle.OracleAdapter;
import org.apache.cayenne.dba.oracle.OraclePkGenerator;
import org.apache.cayenne.dba.postgres.PostgresAdapter;
import org.apache.cayenne.dba.postgres.PostgresPkGenerator;
import org.apache.cayenne.dba.sqlite.SQLiteAdapter;
import org.apache.cayenne.dba.sqlserver.SQLServerAdapter;
import org.apache.cayenne.dba.sybase.SybaseAdapter;
import org.apache.cayenne.dba.sybase.SybasePkGenerator;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.di.spi.DefaultScope;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.log.Slf4jJdbcEventLogger;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.reflect.generic.ValueComparisonStrategyFactory;
import org.apache.cayenne.reflect.generic.DefaultValueComparisonStrategyFactory;
import org.apache.cayenne.resource.ClassLoaderResourceLocator;
import org.apache.cayenne.resource.ResourceLocator;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.unit.DB2UnitDbAdapter;
import org.apache.cayenne.unit.DerbyUnitDbAdapter;
import org.apache.cayenne.unit.FirebirdUnitDbAdapter;
import org.apache.cayenne.unit.FrontBaseUnitDbAdapter;
import org.apache.cayenne.unit.H2UnitDbAdapter;
import org.apache.cayenne.unit.HSQLDBUnitDbAdapter;
import org.apache.cayenne.unit.IngresUnitDbAdapter;
import org.apache.cayenne.unit.MySQLUnitDbAdapter;
import org.apache.cayenne.unit.OpenBaseUnitDbAdapter;
import org.apache.cayenne.unit.OracleUnitDbAdapter;
import org.apache.cayenne.unit.PostgresUnitDbAdapter;
import org.apache.cayenne.unit.SQLServerUnitDbAdapter;
import org.apache.cayenne.unit.SQLiteUnitDbAdapter;
import org.apache.cayenne.unit.SybaseUnitDbAdapter;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestLifecycleManager;
import org.apache.cayenne.unit.testcontainers.Db2ContainerProvider;
import org.apache.cayenne.unit.testcontainers.MariaDbContainerProvider;
import org.apache.cayenne.unit.testcontainers.MysqlContainerProvider;
import org.apache.cayenne.unit.testcontainers.OracleContainerProvider;
import org.apache.cayenne.unit.testcontainers.PostgresContainerProvider;
import org.apache.cayenne.unit.testcontainers.SqlServerContainerProvider;
import org.apache.cayenne.unit.testcontainers.TestContainerProvider;
import org.apache.cayenne.unit.util.SQLTemplateCustomizer;
import org.xml.sax.XMLReader;

public class ServerCaseModule implements Module {

    protected DefaultScope testScope;

    public ServerCaseModule(DefaultScope testScope) {
        this.testScope = testScope;
    }

    public void configure(Binder binder) {

        // these are the objects injectable in unit tests that subclass from
        // ServerCase. Note that ServerRuntimeProvider creates ServerRuntime
        // instances complete with their own DI injectors, independent from the
        // unit test injector. ServerRuntime injector contents are customized
        // inside ServerRuntimeProvider.

        binder.bindMap(String.class, UnitDbAdapterProvider.TEST_ADAPTERS_MAP)
                .put(FirebirdAdapter.class.getName(), FirebirdUnitDbAdapter.class.getName())
                .put(OracleAdapter.class.getName(), OracleUnitDbAdapter.class.getName())
                .put(DerbyAdapter.class.getName(), DerbyUnitDbAdapter.class.getName())
                .put(Oracle8Adapter.class.getName(), OracleUnitDbAdapter.class.getName())
                .put(SybaseAdapter.class.getName(), SybaseUnitDbAdapter.class.getName())
                .put(MySQLAdapter.class.getName(), MySQLUnitDbAdapter.class.getName())
                .put(PostgresAdapter.class.getName(), PostgresUnitDbAdapter.class.getName())
                .put(OpenBaseAdapter.class.getName(), OpenBaseUnitDbAdapter.class.getName())
                .put(SQLServerAdapter.class.getName(), SQLServerUnitDbAdapter.class.getName())
                .put(DB2Adapter.class.getName(), DB2UnitDbAdapter.class.getName())
                .put(HSQLDBAdapter.class.getName(), HSQLDBUnitDbAdapter.class.getName())
                .put(H2Adapter.class.getName(), H2UnitDbAdapter.class.getName())
                .put(FrontBaseAdapter.class.getName(), FrontBaseUnitDbAdapter.class.getName())
                .put(IngresAdapter.class.getName(), IngresUnitDbAdapter.class.getName())
                .put(SQLiteAdapter.class.getName(), SQLiteUnitDbAdapter.class.getName());
        ServerModule.contributeProperties(binder)
                // Use soft references instead of default weak.
                // Should remove problems with random-failing tests (those that are GC-sensitive).
                .put(Constants.SERVER_OBJECT_RETAIN_STRATEGY_PROPERTY, "soft");

        ServerModule.contributeDomainFilters(binder);
        ServerModule.contributeDomainSyncFilters(binder);
        ServerModule.contributeDomainQueryFilters(binder);

        binder.bind(PkGeneratorFactoryProvider.class).to(PkGeneratorFactoryProvider.class);
        binder.bind(PkGenerator.class).to(JdbcPkGenerator.class);
        ServerModule.contributePkGenerators(binder)
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

        // configure extended types
        ServerModule.contributeDefaultTypes(binder)
                .add(new VoidType())
                .add(new BigDecimalType())
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
                .add(new CalendarType<>(GregorianCalendar.class))
                .add(new CalendarType<>(Calendar.class))
                .add(new DurationType());
        ServerModule.contributeUserTypes(binder);
        ServerModule.contributeTypeFactories(binder)
                .add(new InternalUnsupportedTypeFactory());
        ServerModule.contributeValueObjectTypes(binder)
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

        binder.bind(SchemaBuilder.class).to(SchemaBuilder.class);
        binder.bind(JdbcEventLogger.class).to(Slf4jJdbcEventLogger.class);
        binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
        binder.bind(ObjectMapRetainStrategy.class).to(DefaultObjectMapRetainStrategy.class);

        // singleton objects
        binder.bind(UnitTestLifecycleManager.class).toInstance(new ServerCaseLifecycleManager(testScope));

        binder.bindMap(TestContainerProvider.class)
                .put("mysql", MysqlContainerProvider.class)
                .put("mariadb", MariaDbContainerProvider.class)
                .put("postgres", PostgresContainerProvider.class)
                .put("sqlserver", SqlServerContainerProvider.class)
                .put("oracle", OracleContainerProvider.class)
                .put("db2", Db2ContainerProvider.class);

        binder.bind(DataSourceInfo.class).toProvider(ServerCaseDataSourceInfoProvider.class);
        binder.bind(DataSourceFactory.class).to(ServerCaseSharedDataSourceFactory.class);
        binder.bind(DbAdapter.class).toProvider(ServerCaseDbAdapterProvider.class);
        binder.bind(JdbcAdapter.class).toProvider(ServerCaseDbAdapterProvider.class);
        binder.bind(UnitDbAdapter.class).toProvider(UnitDbAdapterProvider.class);

        // this factory is a hack that allows to inject to DbAdapters loaded outside of
        // server runtime... BatchQueryBuilderFactory is hardcoded and whatever is placed
        // in the ServerModule is ignored
        binder.bind(BatchTranslatorFactory.class).toProvider(ServerCaseBatchQueryBuilderFactoryProvider.class);
        binder.bind(DataChannelInterceptor.class).to(ServerCaseDataChannelInterceptor.class);
        binder.bind(SQLTemplateCustomizer.class).toProvider(SQLTemplateCustomizerProvider.class);
        binder.bind(ServerCaseDataSourceFactory.class).to(ServerCaseDataSourceFactory.class);
        binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
        binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
        binder.bind(ResourceLocator.class).to(ClassLoaderResourceLocator.class);
        binder.bind(Key.get(ResourceLocator.class, Constants.SERVER_RESOURCE_LOCATOR)).to(ClassLoaderResourceLocator.class);
        binder.bind(ObjectStoreFactory.class).to(DefaultObjectStoreFactory.class);
        binder.bind(DataMapLoader.class).to(XMLDataMapLoader.class);
        binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
        binder.bind(HandlerFactory.class).to(DefaultHandlerFactory.class);
        binder.bind(DataChannelMetaData.class).to(NoopDataChannelMetaData.class);

        binder.bind(XMLReader.class).toProviderInstance(new XMLReaderProvider(false)).withoutScope();

        // test-scoped objects
        binder.bind(EntityResolver.class).toProvider(ServerCaseEntityResolverProvider.class).in(testScope);
        binder.bind(DataNode.class).toProvider(ServerCaseDataNodeProvider.class).in(testScope);
        binder.bind(ServerCaseProperties.class).to(ServerCaseProperties.class).in(testScope);
        binder.bind(ServerRuntime.class).toProvider(ServerRuntimeProvider.class).in(testScope);
        binder.bind(ObjectContext.class).toProvider(ServerCaseObjectContextProvider.class).withoutScope();
        binder.bind(DataContext.class).toProvider(ServerCaseDataContextProvider.class).withoutScope();
        binder.bind(DBHelper.class).toProvider(FlavoredDBHelperProvider.class).in(testScope);
        binder.bind(DBCleaner.class).toProvider(DBCleanerProvider.class).in(testScope);
    }
}
