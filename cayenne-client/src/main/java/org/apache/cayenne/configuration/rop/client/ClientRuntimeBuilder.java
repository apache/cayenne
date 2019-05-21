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
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.ModuleLoader;
import org.apache.cayenne.remote.ClientConnection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * <p>
 * A convenience class to assemble custom ClientRuntime.
 * It allows to easily configure custom modules and create local runtime.
 * </p>
 * <p>
 * To create this builder use {@link ClientRuntime#builder()} method.
 * </p>
 *
 * @since 4.0
 */
public class ClientRuntimeBuilder {

    private List<Module> modules;
    private boolean autoLoadModules;
    private boolean local;
    Injector serverInjector;
    private Map<String, String> properties;

    ClientRuntimeBuilder() {
        modules = new ArrayList<>();
        autoLoadModules = true;
        local = false;
        properties = null;
    }

    /**
     * Disables DI module auto-loading. By default auto-loading is enabled based on
     * {@link CayenneClientModuleProvider} service provider interface.
     * If you decide to disable auto-loading, make sure you provide all the modules that you need.
     *
     * @return this builder instance.
     */
    public ClientRuntimeBuilder disableModulesAutoLoading() {
        this.autoLoadModules = false;
        return this;
    }

    public ClientRuntimeBuilder addModule(Module module) {
        modules.add(module);
        return this;
    }

    public ClientRuntimeBuilder addModules(Collection<Module> modules) {
        this.modules.addAll(modules);
        return this;
    }

    /**
     * @param properties contributed to {@link ServerModule}
     * @return this builder
     */
    public ClientRuntimeBuilder properties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }

    /**
     * Create {@link ClientRuntime} that provides an ROP stack based on a local
     * connection on top of a server stack.
     *
     * @param serverInjector server injector
     * @return this builder
     */
    public ClientRuntimeBuilder local(Injector serverInjector) {
        this.local = true;
        this.serverInjector = serverInjector;
        return this;
    }

    public ClientRuntime build() {
        Collection<Module> allModules = new ArrayList<>();

        // first load default or auto-loaded modules...
        allModules.addAll(autoLoadModules ? autoLoadedModules() : defaultModules());

        // custom modules override default and auto-loaded ...
        allModules.addAll(modules);

        // builder modules override default, auto-loaded and custom modules...
        allModules.addAll(builderModules());

        return new ClientRuntime(allModules);
    }

    private Collection<? extends Module> autoLoadedModules() {
        return new ModuleLoader().load(CayenneClientModuleProvider.class);
    }

    private Collection<? extends Module> defaultModules() {
        return Collections.singleton(new ClientModule());
    }

    private Collection<? extends Module> builderModules() {
        Collection<Module> modules = new ArrayList<>();

        if(properties != null) {
            modules.add(binder -> ServerModule.contributeProperties(binder).putAll(properties));
        }

        if(local) {
            modules.add(binder -> {
                binder.bind(Key.get(DataChannel.class, ClientRuntime.CLIENT_SERVER_CHANNEL_KEY))
                        .toProviderInstance(new LocalClientServerChannelProvider(serverInjector));
                binder.bind(ClientConnection.class).toProviderInstance(new LocalConnectionProvider());
            });
        }

        return modules;
    }
}
