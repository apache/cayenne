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
package org.apache.cayenne.unit.di.runtime;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DefaultObjectMapRetainStrategy;
import org.apache.cayenne.access.ObjectMapRetainStrategy;
import org.apache.cayenne.access.translator.batch.BatchTranslatorFactory;
import org.apache.cayenne.access.types.*;
import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.apache.cayenne.configuration.DefaultConfigurationNameMapper;
import org.apache.cayenne.configuration.DefaultObjectStoreFactory;
import org.apache.cayenne.configuration.DefaultRuntimeProperties;
import org.apache.cayenne.configuration.ObjectStoreFactory;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.configuration.runtime.CoreModuleExtender;
import org.apache.cayenne.configuration.runtime.DataSourceFactory;
import org.apache.cayenne.configuration.runtime.PkGeneratorFactoryProvider;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.configuration.xml.DefaultHandlerFactory;
import org.apache.cayenne.configuration.xml.HandlerFactory;
import org.apache.cayenne.configuration.xml.NoopDataChannelMetaData;
import org.apache.cayenne.configuration.xml.XMLDataMapLoader;
import org.apache.cayenne.configuration.xml.XMLReaderProvider;
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
import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.di.spi.DefaultScope;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.log.Slf4jJdbcEventLogger;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.reflect.generic.DefaultValueComparisonStrategyFactory;
import org.apache.cayenne.reflect.generic.ValueComparisonStrategyFactory;
import org.apache.cayenne.resource.ClassLoaderResourceLocator;
import org.apache.cayenne.resource.ResourceLocator;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.unit.DB2UnitDbAdapter;
import org.apache.cayenne.unit.DerbyUnitDbAdapter;
import org.apache.cayenne.unit.FirebirdUnitDbAdapter;
import org.apache.cayenne.unit.FrontBaseUnitDbAdapter;
import org.apache.cayenne.unit.H2UnitDbAdapter;
import org.apache.cayenne.unit.HSQLDBUnitDbAdapter;
import org.apache.cayenne.unit.IngresUnitDbAdapter;
import org.apache.cayenne.unit.MySQLUnitDbAdapter;
import org.apache.cayenne.unit.OracleUnitDbAdapter;
import org.apache.cayenne.unit.PostgresUnitDbAdapter;
import org.apache.cayenne.unit.SQLServerUnitDbAdapter;
import org.apache.cayenne.unit.SQLiteUnitDbAdapter;
import org.apache.cayenne.unit.SybaseUnitDbAdapter;
import org.apache.cayenne.unit.UnitDataSourceDescriptor;
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

import java.util.Calendar;
import java.util.GregorianCalendar;

public class RuntimeCaseModule implements Module {

    protected DefaultScope testScope;

    public RuntimeCaseModule(DefaultScope testScope) {
        this.testScope = testScope;
    }

    public void configure(Binder binder) {

        // these are the objects injectable in unit tests that subclass from
        // RuntimeCase. Note that CayenneRuntimeProvider creates CayenneRuntime
        // instances complete with their own DI injectors, independent of the
        // unit test injector. CayenneRuntime injector contents are customized
        // inside CayenneRuntimeProvider.

        binder.bindMap(String.class, UnitDbAdapterProvider.TEST_ADAPTERS_MAP)
                .put(FirebirdAdapter.class.getName(), FirebirdUnitDbAdapter.class.getName())
                .put(OracleAdapter.class.getName(), OracleUnitDbAdapter.class.getName())
                .put(DerbyAdapter.class.getName(), DerbyUnitDbAdapter.class.getName())
                .put(Oracle8Adapter.class.getName(), OracleUnitDbAdapter.class.getName())
                .put(SybaseAdapter.class.getName(), SybaseUnitDbAdapter.class.getName())
                .put(MySQLAdapter.class.getName(), MySQLUnitDbAdapter.class.getName())
                .put(PostgresAdapter.class.getName(), PostgresUnitDbAdapter.class.getName())
                .put(SQLServerAdapter.class.getName(), SQLServerUnitDbAdapter.class.getName())
                .put(DB2Adapter.class.getName(), DB2UnitDbAdapter.class.getName())
                .put(HSQLDBAdapter.class.getName(), HSQLDBUnitDbAdapter.class.getName())
                .put(H2Adapter.class.getName(), H2UnitDbAdapter.class.getName())
                .put(FrontBaseAdapter.class.getName(), FrontBaseUnitDbAdapter.class.getName())
                .put(IngresAdapter.class.getName(), IngresUnitDbAdapter.class.getName())
                .put(SQLiteAdapter.class.getName(), SQLiteUnitDbAdapter.class.getName());

        binder.bind(PkGeneratorFactoryProvider.class).to(PkGeneratorFactoryProvider.class);
        binder.bind(PkGenerator.class).to(JdbcPkGenerator.class);

        new RuntimeCaseModuleExtender(binder)
                .initAllExtensions()

                // Use soft references instead of default weak.
                // Should remove problems with random-failing tests (those that are GC-sensitive).
                .setProperty(Constants.OBJECT_RETAIN_STRATEGY_PROPERTY, "soft")

                .addPkGenerator(DB2Adapter.class, DB2PkGenerator.class)
                .addPkGenerator(DerbyAdapter.class, DerbyPkGenerator.class)
                .addPkGenerator(FrontBaseAdapter.class, FrontBasePkGenerator.class)
                .addPkGenerator(H2Adapter.class, H2PkGenerator.class)
                .addPkGenerator(IngresAdapter.class, IngresPkGenerator.class)
                .addPkGenerator(MySQLAdapter.class, MySQLPkGenerator.class)
                .addPkGenerator(OracleAdapter.class, OraclePkGenerator.class)
                .addPkGenerator(Oracle8Adapter.class, OraclePkGenerator.class)
                .addPkGenerator(PostgresAdapter.class, PostgresPkGenerator.class)
                .addPkGenerator(SQLServerAdapter.class, SybasePkGenerator.class)
                .addPkGenerator(SybaseAdapter.class, SybasePkGenerator.class)

                // configure extended types
                .addDefaultExtendedType(new VoidType())
                .addDefaultExtendedType(new BigDecimalType())
                .addDefaultExtendedType(new BooleanType())
                .addDefaultExtendedType(new ByteArrayType(false, true))
                .addDefaultExtendedType(new ByteType(false))
                .addDefaultExtendedType(new CharType(false, true))
                .addDefaultExtendedType(new DateType())
                .addDefaultExtendedType(new DoubleType())
                .addDefaultExtendedType(new FloatType())
                .addDefaultExtendedType(new IntegerType())
                .addDefaultExtendedType(new LongType())
                .addDefaultExtendedType(new ShortType(false))
                .addDefaultExtendedType(new TimeType())
                .addDefaultExtendedType(new TimestampType())
                .addDefaultExtendedType(new UtilDateType())
                .addDefaultExtendedType(new CalendarType<>(GregorianCalendar.class))
                .addDefaultExtendedType(new CalendarType<>(Calendar.class))
                .addDefaultExtendedType(new DurationType())

                .addExtendedTypeFactory(new InternalUnsupportedTypeFactory())

                .addValueObjectType(BigIntegerValueType.class)
                .addValueObjectType(BigDecimalValueType.class)
                .addValueObjectType(UUIDValueType.class)
                .addValueObjectType(LocalDateValueType.class)
                .addValueObjectType(LocalTimeValueType.class)
                .addValueObjectType(LocalDateTimeValueType.class)
                .addValueObjectType(PeriodValueType.class)
                .addValueObjectType(CharacterValueType.class);

        binder.bind(ValueObjectTypeRegistry.class).to(DefaultValueObjectTypeRegistry.class);
        binder.bind(ValueComparisonStrategyFactory.class).to(DefaultValueComparisonStrategyFactory.class);

        binder.bind(SchemaBuilder.class).to(SchemaBuilder.class);
        binder.bind(JdbcEventLogger.class).to(Slf4jJdbcEventLogger.class);
        binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
        binder.bind(ObjectMapRetainStrategy.class).to(DefaultObjectMapRetainStrategy.class);

        // singleton objects
        binder.bind(UnitTestLifecycleManager.class).toInstance(new RuntimeCaseLifecycleManager(testScope));

        binder.bindMap(TestContainerProvider.class)
                .put("mysql", MysqlContainerProvider.class)
                .put("mariadb", MariaDbContainerProvider.class)
                .put("postgres", PostgresContainerProvider.class)
                .put("sqlserver", SqlServerContainerProvider.class)
                .put("oracle", OracleContainerProvider.class)
                .put("db2", Db2ContainerProvider.class);

        binder.bind(UnitDataSourceDescriptor.class).toProvider(RuntimeCaseDataSourceDescriptorProvider.class);
        binder.bind(DataSourceDescriptor.class).toProviderInstance(new Provider<>() {
            @Inject
            UnitDataSourceDescriptor unitDataSourceDescriptor;

            @Override
            public DataSourceDescriptor get() throws DIRuntimeException {
                return unitDataSourceDescriptor;
            }
        });
        binder.bind(DataSourceFactory.class).to(RuntimeCaseSharedDataSourceFactory.class);
        binder.bind(DbAdapter.class).toProvider(RuntimeCaseDbAdapterProvider.class);
        binder.bind(JdbcAdapter.class).toProvider(RuntimeCaseDbAdapterProvider.class);
        binder.bind(UnitDbAdapter.class).toProvider(UnitDbAdapterProvider.class);

        // this factory is a hack that allows to inject to DbAdapters loaded outside of
        // server runtime... BatchQueryBuilderFactory is hardcoded and whatever is placed
        // in the CoreModule is ignored
        binder.bind(BatchTranslatorFactory.class).toProvider(RuntimeCaseBatchQueryBuilderFactoryProvider.class);
        binder.bind(DataChannelInterceptor.class).to(RuntimeCaseDataChannelInterceptor.class);
        binder.bind(SQLTemplateCustomizer.class).toProvider(SQLTemplateCustomizerProvider.class);
        binder.bind(RuntimeCaseDataSourceFactory.class).to(RuntimeCaseDataSourceFactory.class);
        binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
        binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
        binder.bind(ResourceLocator.class).to(ClassLoaderResourceLocator.class);
        binder.bind(Key.get(ResourceLocator.class, Constants.RESOURCE_LOCATOR)).to(ClassLoaderResourceLocator.class);
        binder.bind(ObjectStoreFactory.class).to(DefaultObjectStoreFactory.class);
        binder.bind(DataMapLoader.class).to(XMLDataMapLoader.class);
        binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
        binder.bind(HandlerFactory.class).to(DefaultHandlerFactory.class);
        binder.bind(DataChannelMetaData.class).to(NoopDataChannelMetaData.class);

        binder.bind(XMLReader.class).toProviderInstance(new XMLReaderProvider(false)).withoutScope();

        // test-scoped objects
        binder.bind(EntityResolver.class).toProvider(RuntimeCaseEntityResolverProvider.class).in(testScope);
        binder.bind(DataNode.class).toProvider(RuntimeCaseDataNodeProvider.class).in(testScope);
        binder.bind(RuntimeCaseProperties.class).to(RuntimeCaseProperties.class).in(testScope);
        binder.bind(RuntimeCaseExtraModules.class).to(RuntimeCaseExtraModules.class).in(testScope);
        binder.bind(CayenneRuntime.class).toProvider(CayenneRuntimeProvider.class).in(testScope);
        binder.bind(ObjectContext.class).toProvider(RuntimeCaseObjectContextProvider.class).withoutScope();
        binder.bind(DataContext.class).toProvider(RuntimeCaseDataContextProvider.class).withoutScope();
        binder.bind(DBHelper.class).toProvider(FlavoredDBHelperProvider.class).in(testScope);
        binder.bind(DBCleaner.class).toProvider(DBCleanerProvider.class).in(testScope);
    }

    // this class exists so that ToolsModule can call "initAllExtensions()" that is protected in CoreModuleExtender.
    static class RuntimeCaseModuleExtender extends CoreModuleExtender {
        public RuntimeCaseModuleExtender(Binder binder) {
            super(binder);
        }

        @Override
        protected CoreModuleExtender initAllExtensions() {
            return super.initAllExtensions();
        }
    }
}
