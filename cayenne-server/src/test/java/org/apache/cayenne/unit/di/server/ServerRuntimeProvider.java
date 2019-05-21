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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.server.DataNodeFactory;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.unit.UnitDbAdapter;

public class ServerRuntimeProvider implements Provider<ServerRuntime> {

    private ServerCaseProperties properties;
    private ServerCaseDataSourceFactory dataSourceFactory;
    private UnitDbAdapter unitDbAdapter;

    private Provider<DbAdapter> dbAdapterProvider;

    public ServerRuntimeProvider(@Inject ServerCaseDataSourceFactory dataSourceFactory,
            @Inject ServerCaseProperties properties,
            @Inject Provider<DbAdapter> dbAdapterProvider,
            @Inject UnitDbAdapter unitDbAdapter) {

        this.dataSourceFactory = dataSourceFactory;
        this.properties = properties;
        this.dbAdapterProvider = dbAdapterProvider;
        this.unitDbAdapter = unitDbAdapter;
    }

    @Override
    public ServerRuntime get() throws ConfigurationException {

        String configurationLocation = properties.getConfigurationLocation();
        if (configurationLocation == null) {
            throw new NullPointerException("Null 'configurationLocation', "
                    + "annotate your test case with @UseServerRuntime");
        }

        Collection<Module> modules = new ArrayList<>(getExtraModules());

        return ServerRuntime.builder()
                        .addConfig(configurationLocation)
                        .addModules(modules)
                        .build();
    }

    protected Collection<? extends Module> getExtraModules() {
        return Collections.singleton(new ServerExtraModule());
    }

    class ServerExtraModule implements Module {

        @Override
        public void configure(Binder binder) {

            // these are the objects overriding standard ServerModule definitions or
            // dependencies needed by such overrides

            binder.bind(DbAdapter.class).toProviderInstance(dbAdapterProvider);
            binder.bind(DataDomain.class).toProvider(ServerCaseDataDomainProvider.class);
            binder.bind(DataNodeFactory.class).to(ServerCaseDataNodeFactory.class);
            binder.bind(UnitDbAdapter.class).toInstance(unitDbAdapter);

            ServerModule.contributeProperties(binder)
                    // Use soft references instead of default weak.
                    // Should remove problems with random-failing tests (those that are GC-sensitive).
                    .put(Constants.SERVER_OBJECT_RETAIN_STRATEGY_PROPERTY, "soft");

            // map DataSources for all test DataNode names
            binder.bind(ServerCaseDataSourceFactory.class).toInstance(dataSourceFactory);
        }
    }
}
