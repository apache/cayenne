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

import org.apache.cayenne.di.*;

import java.util.Map;
import java.util.Map.Entry;

/**
 * @since 3.1
 */
class DefaultMapBuilder<T> implements MapBuilder<T> {

    private DefaultInjector injector;
    private Key<Map<String, T>> bindingKey;

    DefaultMapBuilder(Key<Map<String, T>> bindingKey, DefaultInjector injector) {
        this.injector = injector;
        this.bindingKey = bindingKey;

        // trigger initialization of the MapProvider right away, as we need to bind an
        // empty map even if the user never calls 'put'
        getMapProvider();
    }

    @Override
    public MapBuilder<T> put(String key, Class<? extends T> interfaceType)
            throws DIRuntimeException {

        Key<? extends T> bindingKey = Key.get(interfaceType);
        Binding<? extends T> binding = injector.getBinding(bindingKey);

        if (binding == null) {
            return putWithBinding(key, interfaceType);
        }

        // TODO: andrus 11/15/2009 - report overriding the key??
        getMapProvider().put(key, binding.getScoped());
        return this;
    }

    <K extends T> MapBuilder<T> putWithBinding(String key, Class<K> interfaceType) {
        Key<K> bindingKey = Key.get(interfaceType);

        Provider<K> provider0 = new ConstructorInjectingProvider<>(interfaceType, injector);
        Provider<K> provider1 = new FieldInjectingProvider<>(provider0, injector);
        injector.putBinding(bindingKey, provider1);

        getMapProvider().put(key, injector.getProvider(bindingKey));
        return this;
    }

    @Override
    public MapBuilder<T> put(String key, T value) throws DIRuntimeException {

        Provider<T> provider0 = new InstanceProvider<T>(value);
        Provider<T> provider1 = new FieldInjectingProvider<T>(provider0, injector);

        // TODO: andrus 11/15/2009 - report overriding the key??
        getMapProvider().put(key, provider1);
        return this;
    }

    @Override
    public MapBuilder<T> putAll(Map<String, T> map) throws DIRuntimeException {

        MapProvider<T> provider = getMapProvider();

        for (Entry<String, T> entry : map.entrySet()) {

            Provider<T> provider0 = new InstanceProvider<T>(entry.getValue());
            Provider<T> provider1 = new FieldInjectingProvider<T>(provider0, injector);
            provider.put(entry.getKey(), provider1);
        }

        return this;
    }

    private MapProvider<T> getMapProvider() {
        MapProvider<T> provider;

        Binding<Map<String, T>> binding = injector.getBinding(bindingKey);
        if (binding == null) {
            provider = new MapProvider<>();
            injector.putBinding(bindingKey, provider);
        } else {
            provider = (MapProvider<T>) binding.getOriginal();
        }

        return provider;
    }

    @Override
    public void in(Scope scope) {
        injector.changeBindingScope(bindingKey, scope);
    }
}
