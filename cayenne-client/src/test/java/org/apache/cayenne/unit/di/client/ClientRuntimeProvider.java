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
package org.apache.cayenne.unit.di.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.configuration.rop.client.ClientRuntime;
import org.apache.cayenne.configuration.rop.client.LocalConnectionProvider;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.remote.ClientConnection;

public class ClientRuntimeProvider implements Provider<ClientRuntime> {

    // injecting provider to make this provider independent from scoping of ServerRuntime
    @Inject
    protected Provider<ServerRuntime> serverRuntimeProvider;

    @Inject
    protected ClientCaseProperties clientCaseProperties;

    protected Collection<? extends Module> getModules() {
        return Collections.singleton(binder -> {
            // add an interceptor between client and server parts to capture and inspect the traffic
            binder.bind(Key.get(DataChannel.class, ClientRuntime.CLIENT_SERVER_CHANNEL_KEY))
                    .toProviderInstance(new InterceptingClientServerChannelProvider(serverRuntimeProvider.get().getInjector()));
            // create local connection
            binder.bind(ClientConnection.class).toProviderInstance(new LocalConnectionProvider());
        });
    }

    public ClientRuntime get() throws ConfigurationException {
        Collection<Module> modules = new ArrayList<>(getModules());
        return ClientRuntime.builder()
                .properties(clientCaseProperties.getRuntimeProperties())
                .addModules(modules)
                .build();
    }
}
