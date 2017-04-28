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
package org.apache.cayenne.configuration.rop.client;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.remote.ClientConnection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * A {@link ClientRuntime} that provides an ROP stack based on a local
 * connection on top of a server stack.
 *
 * @since 3.1
 * @deprecated since 4.0, use {@link ClientRuntimeBuilder#local(Injector)}
 */
@Deprecated
public class ClientLocalRuntime extends ClientRuntime {

    /**
     * @since 4.0 moved to {@link ClientRuntime#CLIENT_SERVER_CHANNEL_KEY}
     */
    public static final String CLIENT_SERVER_CHANNEL_KEY = "client-server-channel";

    private static Collection<Module> collectModules(
            Injector serverInjector,
            Module... extraModules) {

        Collection<Module> modules = extraModules != null ? asList(extraModules) : Collections.<Module>emptyList();
        return collectModules(serverInjector, modules);
    }

    private static Collection<Module> collectModules(final Injector serverInjector, Collection<Module> extraModules) {

        Collection<Module> modules = new ArrayList<>(extraModules.size() + 1);
        modules.add(new Module() {

            public void configure(Binder binder) {
                binder.bind(Key.get(DataChannel.class, ClientRuntime.CLIENT_SERVER_CHANNEL_KEY)).toProviderInstance(
                        new LocalClientServerChannelProvider(serverInjector));
                binder.bind(ClientConnection.class).toProviderInstance(new LocalConnectionProvider());
            }
        });

        modules.addAll(extraModules);
        return modules;
    }

    public ClientLocalRuntime(Injector serverInjector, Map<String, String> properties, Collection<Module> extraModules) {
        super(properties, collectModules(serverInjector, extraModules));
    }

    public ClientLocalRuntime(Injector serverInjector, Map<String, String> properties, Module... extraModules) {
        super(properties, collectModules(serverInjector, extraModules));
    }

}
