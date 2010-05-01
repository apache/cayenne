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
package org.apache.cayenne.di.spi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.Provider;

/**
 * A default Cayenne implementations of a DI injector.
 * 
 * @since 3.1
 */
public class DefaultInjector implements Injector {

    private Map<Key<?>, Provider<?>> bindings;
    private Map<Key<?>, MapProvider> mapConfigurations;
    private Map<Key<?>, ListProvider> listConfigurations;
    private InjectionStack injectionStack;

    public DefaultInjector(Module... modules) throws ConfigurationException {

        this.bindings = new HashMap<Key<?>, Provider<?>>();
        this.mapConfigurations = new HashMap<Key<?>, MapProvider>();
        this.listConfigurations = new HashMap<Key<?>, ListProvider>();
        this.injectionStack = new InjectionStack();

        DefaultBinder binder = new DefaultBinder(this);

        // bind self for injector injection...
        binder.bind(Injector.class).toInstance(this);

        // bind modules
        if (modules != null && modules.length > 0) {

            for (Module module : modules) {
                module.configure(binder);
            }
        }
    }

    InjectionStack getInjectionStack() {
        return injectionStack;
    }

    Map<Key<?>, Provider<?>> getBindings() {
        return bindings;
    }

    Map<Key<?>, MapProvider> getMapConfigurations() {
        return mapConfigurations;
    }

    Map<Key<?>, ListProvider> getListConfigurations() {
        return listConfigurations;
    }

    public <T> T getInstance(Class<T> type) throws ConfigurationException {
        return getProvider(type).get();
    }

    public <T> T getInstance(Key<T> key) throws ConfigurationException {
        return getProvider(key).get();
    }

    public <T> List<?> getListConfiguration(Class<T> type) {
        if (type == null) {
            throw new NullPointerException("Null type");
        }

        ListProvider provider = listConfigurations.get(Key.get(type));

        if (provider == null) {
            throw new ConfigurationException(
                    "Type '%s' has no bound list configuration in the DI container."
                            + " Injection stack: %s",
                    type.getName(),
                    injectionStack);
        }

        return provider.get();
    }

    public <T> Map<String, ?> getMapConfiguration(Class<T> type) {
        if (type == null) {
            throw new NullPointerException("Null type");
        }

        MapProvider provider = mapConfigurations.get(Key.get(type));

        if (provider == null) {
            throw new ConfigurationException(
                    "Type '%s' has no bound map configuration in the DI container."
                            + " Injection stack: %s",
                    type.getName(),
                    injectionStack);
        }

        return provider.get();
    }

    public <T> Provider<T> getProvider(Class<T> type) throws ConfigurationException {
        return getProvider(Key.get(type));
    }

    public <T> Provider<T> getProvider(Key<T> key) throws ConfigurationException {

        if (key == null) {
            throw new NullPointerException("Null key");
        }

        Provider<T> provider = (Provider<T>) bindings.get(key);

        if (provider == null) {
            throw new ConfigurationException(
                    "DI container has no binding for key %s",
                    key);
        }

        return provider;
    }

    public void injectMembers(Object object) {
        Provider<Object> provider0 = new InstanceProvider<Object>(object);
        Provider<Object> provider1 = new FieldInjectingProvider<Object>(
                provider0,
                this,
                Key.get(object.getClass()));
        provider1.get();
    }

}
