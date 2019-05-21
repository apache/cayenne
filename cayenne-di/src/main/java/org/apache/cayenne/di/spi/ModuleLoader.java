/*
 *    Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.di.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Module;

/**
 * Auto-loads DI modules using ServiceLoader. To make a module auto-loadable, you will need to ship the jar with a file
 * "META-INF/services/&lt;full.provider.class.name&gt;" that contains provider implementation for the module in
 * question.
 *
 * @see ModuleProvider
 * @since 4.0
 */
public class ModuleLoader {

    /**
     * Auto-loads all modules declared on classpath. Modules are loaded from the SPI declarations stored in
     * "META-INF/services/&lt;full.provider.class.name&gt;", and then sorted in the order of override dependency.
     *
     * @return a sorted collection of auto-loadable modules.
     * @throws DIRuntimeException if auto-loaded modules have circular override dependencies.
     */
    public List<Module> load(Class<? extends ModuleProvider> providerClass) {
        return loadModules(ServiceLoader.load(providerClass));
    }

    /**
     * Auto-loads all modules declared on classpath. Modules are loaded from the SPI declarations stored in
     * "META-INF/services/&lt;full.provider.class.name&gt;", and then sorted in the order of override dependency.
     *
     * @return a sorted collection of auto-loadable modules.
     * @throws DIRuntimeException if auto-loaded modules have circular override dependencies.
     * @since 4.2
     */
    public List<Module> load(Class<? extends ModuleProvider> providerClass, ClassLoader classLoader) {
        return loadModules(ServiceLoader.load(providerClass, classLoader));
    }

    private List<Module> loadModules(ServiceLoader<? extends ModuleProvider> serviceLoader) {
        // map providers by class

        Map<Class<? extends Module>, ModuleProvider> providers = new HashMap<>();

        for (ModuleProvider provider : serviceLoader) {

            ModuleProvider existing = providers.put(provider.moduleType(), provider);
            if (existing != null && !existing.getClass().equals(provider.getClass())) {
                throw new DIRuntimeException("More than one provider for module type '%s': %s and %s",
                        provider.moduleType().getName(),
                        existing.getClass().getName(),
                        provider.getClass().getName());
            }
        }

        if (providers.size() == 0) {
            return Collections.emptyList();
        }

        // do override dependency sort...
        DIGraph<Class<? extends Module>> overrideGraph = new DIGraph<>();
        for (Map.Entry<Class<? extends Module>, ModuleProvider> e : providers.entrySet()) {

            overrideGraph.add(e.getKey());
            for (Class<? extends Module> overridden : e.getValue().overrides()) {
                overrideGraph.add(e.getKey(), overridden);
            }
        }

        Collection<Class<? extends Module>> moduleTypes = overrideGraph.topSort();
        List<Module> modules = new ArrayList<>(moduleTypes.size());
        for (Class<? extends Module> type : moduleTypes) {
            modules.add(providers.get(type).module());
        }

        return modules;
    }
}
