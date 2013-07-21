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

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.configuration.rop.client.ClientLocalRuntime;
import org.apache.cayenne.configuration.rop.client.ClientRuntime;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.Provider;

public class ClientRuntimeProvider implements Provider<ClientRuntime> {

    @Inject
    // injecting provider to make this provider independent from scoping of ServerRuntime
    protected Provider<ServerRuntime> serverRuntimeProvider;

    @Inject
    protected ClientCaseProperties clientCaseProperties;

    public ClientRuntime get() throws ConfigurationException {
        Injector serverInjector = serverRuntimeProvider.get().getInjector();
        return new ClientLocalRuntime(serverInjector, clientCaseProperties
                .getRuntimeProperties(), new ClientExtraModule(serverInjector));
    }

    class ClientExtraModule implements Module {

        private Injector serverInjector;

        ClientExtraModule(Injector serverInjector) {
            this.serverInjector = serverInjector;
        }

        public void configure(Binder binder) {

            // these are the objects overriding standard ClientLocalModule definitions or
            // dependencies needed by such overrides

            // add an interceptor between client and server parts to capture and inspect
            // the traffic
            binder
                    .bind(
                            Key.get(
                                    DataChannel.class,
                                    ClientLocalRuntime.CLIENT_SERVER_CHANNEL_KEY))
                    .toProviderInstance(
                            new InterceptingClientServerChannelProvider(serverInjector));
        }
    }
}
