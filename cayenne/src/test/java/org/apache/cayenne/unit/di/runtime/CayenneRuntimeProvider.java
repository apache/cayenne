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

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.runtime.CoreModule;
import org.apache.cayenne.configuration.runtime.DataNodeFactory;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.unit.UnitDbAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class CayenneRuntimeProvider implements Provider<CayenneRuntime> {

    private RuntimeCaseProperties properties;
    private RuntimeCaseExtraModules extraModules;
    private RuntimeCaseDataSourceFactory dataSourceFactory;
    private UnitDbAdapter unitDbAdapter;
    private Provider<DbAdapter> dbAdapterProvider;

    public CayenneRuntimeProvider(@Inject RuntimeCaseDataSourceFactory dataSourceFactory,
                                  @Inject RuntimeCaseProperties properties,
                                  @Inject RuntimeCaseExtraModules extraModules,
                                  @Inject Provider<DbAdapter> dbAdapterProvider,
                                  @Inject UnitDbAdapter unitDbAdapter) {

        this.dataSourceFactory = dataSourceFactory;
        this.properties = properties;
        this.extraModules = extraModules;
        this.dbAdapterProvider = dbAdapterProvider;
        this.unitDbAdapter = unitDbAdapter;
    }

    @Override
    public CayenneRuntime get() throws ConfigurationException {

        String configurationLocation = properties.getConfigurationLocation();
        if (configurationLocation == null) {
            throw new NullPointerException("Null 'configurationLocation', "
                    + "annotate your test case with @UseCayenneRuntime");
        }

        Collection<Module> modules = new ArrayList<>(getExtraModules());
        modules.addAll(extraModules.getExtraModules());

        return CayenneRuntime.builder()
                        .addConfig(configurationLocation)
                        .addModules(modules)
                        .build();
    }

    protected Collection<? extends Module> getExtraModules() {
        return Collections.singleton(new ExtraModule());
    }

    class ExtraModule implements Module {

        @Override
        public void configure(Binder binder) {

            // these are the objects overriding standard CoreModule definitions or
            // dependencies needed by such overrides

            binder.bind(DbAdapter.class).toProviderInstance(dbAdapterProvider);
            binder.bind(DataDomain.class).toProvider(RuntimeCaseDataDomainProvider.class);
            binder.bind(DataNodeFactory.class).to(RuntimeCaseDataNodeFactory.class);
            binder.bind(UnitDbAdapter.class).toInstance(unitDbAdapter);

            CoreModule.extend(binder)
                    // Use soft references instead of default weak.
                    // Should remove problems with random-failing tests (those that are GC-sensitive).
                    .setProperty(Constants.OBJECT_RETAIN_STRATEGY_PROPERTY, "soft");

            // map DataSources for all test DataNode names
            binder.bind(RuntimeCaseDataSourceFactory.class).toInstance(dataSourceFactory);
        }
    }
}
