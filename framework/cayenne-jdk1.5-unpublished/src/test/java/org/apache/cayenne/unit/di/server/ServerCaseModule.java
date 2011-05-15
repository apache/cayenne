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
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultScope;
import org.apache.cayenne.log.CommonsJdbcEventLogger;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.unit.AccessStackAdapter;
import org.apache.cayenne.unit.CayenneResources;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestLifecycleManager;
import org.apache.cayenne.unit.util.SQLTemplateCustomizer;

public class ServerCaseModule implements Module {

    protected CayenneResources resources;
    protected DefaultScope testScope;

    public ServerCaseModule(CayenneResources resources, DefaultScope testScope) {
        this.resources = resources;
        this.testScope = testScope;
    }

    public void configure(Binder binder) {

        // these are the objects injectable in unit tests that subclass from
        // ServerCase. Note that ServerRuntimeProvider creates ServerRuntime
        // instances complete with their own DI injectors, independent from the
        // unit test injector. ServerRuntime injector contents are customized
        // inside ServerRuntimeProvider.

        binder.bind(JdbcEventLogger.class).to(CommonsJdbcEventLogger.class);

        // singleton objects
        binder.bind(UnitTestLifecycleManager.class).toInstance(
                new ServerCaseLifecycleManager(testScope));

        binder.bind(DataSource.class).toProviderInstance(
                new CayenneResourcesDataSourceProvider(resources));
        binder.bind(DbAdapter.class).toProviderInstance(
                new CayenneResourcesDbAdapterProvider(resources));
        binder.bind(AccessStackAdapter.class).toProviderInstance(
                new CayenneResourcesAccessStackAdapterProvider(resources));
        binder.bind(BatchQueryBuilderFactory.class).toProvider(
                ServerCaseBatchQueryBuilderFactoryProvider.class);
        binder.bind(DataChannelInterceptor.class).to(
                ServerCaseDataChannelInterceptor.class);
        binder.bind(SQLTemplateCustomizer.class).toProviderInstance(
                new CayenneResourcesSQLTemplateCustomizerProvider(resources));

        // test-scoped objects
        binder.bind(EntityResolver.class).toProvider(
                ServerCaseEntityResolverProvider.class).in(testScope);
        binder.bind(DataNode.class).toProvider(ServerCaseDataNodeProvider.class).in(
                testScope);
        binder.bind(ServerCaseProperties.class).to(ServerCaseProperties.class).in(
                testScope);
        binder.bind(ServerRuntime.class).toProviderInstance(
                new ServerRuntimeProvider(resources)).in(testScope);
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
