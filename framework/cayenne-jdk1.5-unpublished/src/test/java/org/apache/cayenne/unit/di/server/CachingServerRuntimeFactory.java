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

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.unit.CayenneResources;
import org.apache.cayenne.unit.di.UnitTestScope;

public class CachingServerRuntimeFactory implements ServerRuntimeFactory {

    protected CayenneResources resources;
    protected UnitTestScope testScope;
    protected Map<String, ServerRuntime> cache;

    public CachingServerRuntimeFactory(CayenneResources resources, UnitTestScope testScope) {
        this.resources = resources;
        this.testScope = testScope;
        this.cache = new HashMap<String, ServerRuntime>();
    }

    public ServerRuntime get(String configurationLocation) {
        
        if (configurationLocation == null) {
            throw new NullPointerException("Null 'configurationLocation'");
        }
        
        ServerRuntime runtime = cache.get(configurationLocation);

        if (runtime == null) {
            runtime = create(configurationLocation);
            cache.put(configurationLocation, runtime);
        }

        return runtime;
    }

    protected ServerRuntime create(String configurationLocation) {
        return new ServerRuntime(configurationLocation, new ServerExtraModule());
    }

    class ServerExtraModule implements Module {

        public void configure(Binder binder) {

            // these are the objects overriding standard ServerModule definitions or
            // dependencies needed by such overrides

            binder.bind(DbAdapter.class).toProviderInstance(
                    new CayenneResourcesDbAdapterProvider(resources));
            binder.bind(DataDomain.class).toProvider(ServerCaseDataDomainProvider.class);
            binder.bind(DataSource.class).toProviderInstance(
                    new CayenneResourcesDataSourceProvider(resources));
        }
    }
}
