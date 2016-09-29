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
package org.apache.cayenne.dbsync.unit;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.CayenneDbSyncModule;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.ServerCaseDataSourceFactory;
import org.apache.cayenne.unit.di.server.ServerCaseProperties;
import org.apache.cayenne.unit.di.server.ServerRuntimeProvider;

import java.util.ArrayList;
import java.util.Collection;

public class DbSyncServerRuntimeProvider extends ServerRuntimeProvider {

    public DbSyncServerRuntimeProvider(@Inject ServerCaseDataSourceFactory dataSourceFactory,
                                       @Inject ServerCaseProperties properties,
                                       @Inject Provider<DbAdapter> dbAdapterProvider,
                                       @Inject UnitDbAdapter unitDbAdapter) {
        super(dataSourceFactory, properties, dbAdapterProvider, unitDbAdapter);
    }

    @Override
    protected Collection<? extends Module> getExtraModules() {
        Collection<Module> modules = new ArrayList<>();
        modules.addAll(super.getExtraModules());
        modules.add(new CayenneDbSyncModule());
        return modules;
    }
}
