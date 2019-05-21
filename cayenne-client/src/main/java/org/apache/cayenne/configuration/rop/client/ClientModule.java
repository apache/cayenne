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
package org.apache.cayenne.configuration.rop.client;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.cache.MapQueryCacheProvider;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.configuration.DefaultRuntimeProperties;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.event.EventManagerProvider;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.remote.RemoteService;
import org.apache.cayenne.rop.HttpClientConnectionProvider;
import org.apache.cayenne.rop.ProxyRemoteService;
import org.apache.cayenne.rop.ROPSerializationService;
import org.apache.cayenne.rop.http.ClientHessianSerializationServiceProvider;

/**
 * A DI module containing all Cayenne ROP client runtime configurations.
 * 
 * @since 3.1
 * @since 4.0 this module is auto-loaded by {@link ClientRuntimeBuilder}
 */
public class ClientModule implements Module {

    /**
     * @since 4.0
     */
    public ClientModule() {
    }

    @SuppressWarnings("deprecation")
    public void configure(Binder binder) {

        // Contribute always to create binding
        ServerModule.contributeProperties(binder);

        binder.bind(ObjectContextFactory.class).to(CayenneContextFactory.class);
        binder.bind(ROPSerializationService.class).toProvider(ClientHessianSerializationServiceProvider.class);
        binder.bind(RemoteService.class).to(ProxyRemoteService.class);
        binder.bind(ClientConnection.class).toProvider(HttpClientConnectionProvider.class);
        binder.bind(EventManager.class).toProvider(EventManagerProvider.class);
        binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
        binder.bind(DataChannel.class).toProvider(ClientChannelProvider.class);
        binder.bind(QueryCache.class).toProvider(MapQueryCacheProvider.class);
    }

}
