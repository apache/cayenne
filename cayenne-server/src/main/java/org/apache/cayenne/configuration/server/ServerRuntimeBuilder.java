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
package org.apache.cayenne.configuration.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.apache.cayenne.di.Module;

/**
 * A convenience class to assemble custom ServerRuntime.
 * 
 * @since 3.2
 */
public class ServerRuntimeBuilder {

    private Collection<String> configs;
    private Collection<Module> modules;

    public ServerRuntimeBuilder() {
        this.configs = new LinkedHashSet<String>();
        this.modules = new ArrayList<Module>();
    }

    public ServerRuntimeBuilder(String configurationLocation) {
        this();
        addConfig(configurationLocation);
    }

    public ServerRuntimeBuilder addConfig(String configurationLocation) {
        configs.add(configurationLocation);
        return this;
    }

    public ServerRuntimeBuilder addConfigs(Collection<String> configurationLocations) {
        configs.addAll(configurationLocations);
        return this;
    }

    public ServerRuntimeBuilder addModule(Module module) {
        modules.add(module);
        return this;
    }

    public ServerRuntimeBuilder addMoudles(Collection<Module> modules) {
        this.modules.addAll(modules);
        return this;
    }

    public ServerRuntime build() {
        String[] configs = this.configs.toArray(new String[this.configs.size()]);
        Module[] modules = this.modules.toArray(new Module[this.modules.size()]);
        return new ServerRuntime(configs, modules);
    }
}
