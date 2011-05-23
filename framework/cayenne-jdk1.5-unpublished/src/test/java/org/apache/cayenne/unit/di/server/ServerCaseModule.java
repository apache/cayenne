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

import javax.sql.DataSource;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.jdbc.BatchQueryBuilderFactory;
import org.apache.cayenne.configuration.AdhocObjectFactory;
import org.apache.cayenne.configuration.DefaultAdhocObjectFactory;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.db2.DB2Adapter;
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
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultScope;
import org.apache.cayenne.log.CommonsJdbcEventLogger;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.unit.AccessStackAdapter;
import org.apache.cayenne.unit.CayenneResources;
import org.apache.cayenne.unit.DB2StackAdapter;
import org.apache.cayenne.unit.FrontBaseStackAdapter;
import org.apache.cayenne.unit.H2StackAdapter;
import org.apache.cayenne.unit.HSQLDBStackAdapter;
import org.apache.cayenne.unit.IngresStackAdapter;
import org.apache.cayenne.unit.MySQLStackAdapter;
import org.apache.cayenne.unit.OpenBaseStackAdapter;
import org.apache.cayenne.unit.OracleStackAdapter;
import org.apache.cayenne.unit.PostgresStackAdapter;
import org.apache.cayenne.unit.SQLServerStackAdapter;
import org.apache.cayenne.unit.SQLiteStackAdapter;
import org.apache.cayenne.unit.SybaseStackAdapter;
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

        binder.bindMap(AccessStackAdapterProvider.TEST_ADAPTERS_MAP).put(
                OracleAdapter.class.getName(),
                OracleStackAdapter.class.getName()).put(
                Oracle8Adapter.class.getName(),
                OracleStackAdapter.class.getName()).put(
                SybaseAdapter.class.getName(),
                SybaseStackAdapter.class.getName()).put(
                MySQLAdapter.class.getName(),
                MySQLStackAdapter.class.getName()).put(
                PostgresAdapter.class.getName(),
                PostgresStackAdapter.class.getName()).put(
                OpenBaseAdapter.class.getName(),
                OpenBaseStackAdapter.class.getName()).put(
                SQLServerAdapter.class.getName(),
                SQLServerStackAdapter.class.getName()).put(
                DB2Adapter.class.getName(),
                DB2StackAdapter.class.getName()).put(
                HSQLDBAdapter.class.getName(),
                HSQLDBStackAdapter.class.getName()).put(
                H2Adapter.class.getName(),
                H2StackAdapter.class.getName()).put(
                FrontBaseAdapter.class.getName(),
                FrontBaseStackAdapter.class.getName()).put(
                IngresAdapter.class.getName(),
                IngresStackAdapter.class.getName()).put(
                SQLiteAdapter.class.getName(),
                SQLiteStackAdapter.class.getName());

        binder.bind(CayenneResources.class).toProvider(CayenneResourcesProvider.class);
        binder.bind(JdbcEventLogger.class).to(CommonsJdbcEventLogger.class);

        // singleton objects
        binder.bind(UnitTestLifecycleManager.class).toInstance(
                new ServerCaseLifecycleManager(testScope));

        binder.bind(DataSourceInfo.class).toProvider(
                ServerCaseDataSourceInfoProvider.class);
        binder
                .bind(DataSource.class)
                .toProvider(ServerCaseSharedDataSourceProvider.class);
        binder.bind(DbAdapter.class).toProvider(ServerCaseDbAdapterProvider.class);
        binder
                .bind(AccessStackAdapter.class)
                .toProvider(AccessStackAdapterProvider.class);
        binder.bind(BatchQueryBuilderFactory.class).toProvider(
                ServerCaseBatchQueryBuilderFactoryProvider.class);
        binder.bind(DataChannelInterceptor.class).to(
                ServerCaseDataChannelInterceptor.class);
        binder.bind(SQLTemplateCustomizer.class).toProvider(
                SQLTemplateCustomizerProvider.class);
        binder.bind(ServerCaseDataSourceFactory.class).to(
                ServerCaseDataSourceFactory.class);
        binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);

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
