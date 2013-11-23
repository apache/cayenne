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
package org.apache.cayenne.unit.di.server;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DefaultObjectMapRetainStrategy;
import org.apache.cayenne.access.ObjectMapRetainStrategy;
import org.apache.cayenne.access.jdbc.BatchQueryBuilderFactory;
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
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DefaultObjectStoreFactory;
import org.apache.cayenne.configuration.DefaultRuntimeProperties;
import org.apache.cayenne.configuration.ObjectStoreFactory;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.db2.DB2Adapter;
import org.apache.cayenne.dba.derby.DerbyAdapter;
import org.apache.cayenne.dba.firebird.FirebirdAdapter;
import org.apache.cayenne.dba.frontbase.FrontBaseAdapter;
import org.apache.cayenne.dba.h2.H2Adapter;
import org.apache.cayenne.dba.hsqldb.HSQLDBAdapter;
import org.apache.cayenne.dba.ingres.IngresAdapter;
import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.apache.cayenne.dba.openbase.OpenBaseAdapter;
import org.apache.cayenne.dba.oracle.Oracle8Adapter;
import org.apache.cayenne.dba.oracle.OracleAdapter;
import org.apache.cayenne.dba.postgres.PostgresAdapter;
import org.apache.cayenne.dba.sqlite.SQLiteAdapter;
import org.apache.cayenne.dba.sqlserver.SQLServerAdapter;
import org.apache.cayenne.dba.sybase.SybaseAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultScope;
import org.apache.cayenne.log.CommonsJdbcEventLogger;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.EntityResolver;
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
import org.apache.cayenne.unit.util.SQLTemplateCustomizer;

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

        binder.bindMap(UnitDbAdapterProvider.TEST_ADAPTERS_MAP).put(
                FirebirdAdapter.class.getName(),
                FirebirdUnitDbAdapter.class.getName()).put(
                OracleAdapter.class.getName(),
                OracleUnitDbAdapter.class.getName()).put(
                DerbyAdapter.class.getName(),
                DerbyUnitDbAdapter.class.getName()).put(
                Oracle8Adapter.class.getName(),
                OracleUnitDbAdapter.class.getName()).put(
                SybaseAdapter.class.getName(),
                SybaseUnitDbAdapter.class.getName()).put(
                MySQLAdapter.class.getName(),
                MySQLUnitDbAdapter.class.getName()).put(
                PostgresAdapter.class.getName(),
                PostgresUnitDbAdapter.class.getName()).put(
                OpenBaseAdapter.class.getName(),
                OpenBaseUnitDbAdapter.class.getName()).put(
                SQLServerAdapter.class.getName(),
                SQLServerUnitDbAdapter.class.getName()).put(
                DB2Adapter.class.getName(),
                DB2UnitDbAdapter.class.getName()).put(
                HSQLDBAdapter.class.getName(),
                HSQLDBUnitDbAdapter.class.getName()).put(
                H2Adapter.class.getName(),
                H2UnitDbAdapter.class.getName()).put(
                FrontBaseAdapter.class.getName(),
                FrontBaseUnitDbAdapter.class.getName()).put(
                IngresAdapter.class.getName(),
                IngresUnitDbAdapter.class.getName()).put(
                SQLiteAdapter.class.getName(),
                SQLiteUnitDbAdapter.class.getName());
        binder.bindMap(Constants.PROPERTIES_MAP);
        
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

        binder.bind(SchemaBuilder.class).to(SchemaBuilder.class);
        binder.bind(JdbcEventLogger.class).to(CommonsJdbcEventLogger.class);
        binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
        binder.bind(ObjectMapRetainStrategy.class).to(
                DefaultObjectMapRetainStrategy.class);

        // singleton objects
        binder.bind(UnitTestLifecycleManager.class).toInstance(
                new ServerCaseLifecycleManager(testScope));

        binder.bind(DataSourceInfo.class).toProvider(
                ServerCaseDataSourceInfoProvider.class);
        binder.bind(DataSourceFactory.class).to(ServerCaseSharedDataSourceFactory.class);
        binder.bind(DbAdapter.class).toProvider(ServerCaseDbAdapterProvider.class);
        binder.bind(JdbcAdapter.class).toProvider(ServerCaseDbAdapterProvider.class);
        binder.bind(UnitDbAdapter.class).toProvider(UnitDbAdapterProvider.class);

        // this factory is a hack that allows to inject to DbAdapters loaded outside of
        // server runtime... BatchQueryBuilderFactory is hardcoded and whatever is placed
        // in the ServerModule is ignored
        binder.bind(BatchQueryBuilderFactory.class).toProvider(
                ServerCaseBatchQueryBuilderFactoryProvider.class);
        binder.bind(DataChannelInterceptor.class).to(
                ServerCaseDataChannelInterceptor.class);
        binder.bind(SQLTemplateCustomizer.class).toProvider(
                SQLTemplateCustomizerProvider.class);
        binder.bind(ServerCaseDataSourceFactory.class).to(
                ServerCaseDataSourceFactory.class);
        binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
        binder.bind(ResourceLocator.class).to(ClassLoaderResourceLocator.class);
        binder.bind(ObjectStoreFactory.class).to(DefaultObjectStoreFactory.class);

        // test-scoped objects
        binder.bind(EntityResolver.class).toProvider(
                ServerCaseEntityResolverProvider.class).in(testScope);
        binder.bind(DataNode.class).toProvider(ServerCaseDataNodeProvider.class).in(
                testScope);
        binder.bind(ServerCaseProperties.class).to(ServerCaseProperties.class).in(
                testScope);
        binder.bind(ServerRuntime.class).toProvider(ServerRuntimeProvider.class).in(
                testScope);
        binder
                .bind(ObjectContext.class)
                .toProvider(ServerCaseObjectContextProvider.class)
                .withoutScope();
        binder
                .bind(DataContext.class)
                .toProvider(ServerCaseDataContextProvider.class)
                .withoutScope();

        binder.bind(DBHelper.class).toProvider(FlavoredDBHelperProvider.class).in(
                testScope);
    }
}
