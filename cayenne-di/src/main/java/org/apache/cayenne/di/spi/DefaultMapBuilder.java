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
package org.apache.cayenne.di.spi;

import org.apache.cayenne.di.*;

import java.util.Map;
import java.util.Map.Entry;

/**
 * @since 3.1
 */
class DefaultMapBuilder<T> extends DICollectionBuilder<Map<String, T>, T> implements MapBuilder<T> {

    DefaultMapBuilder(Key<Map<String, T>> bindingKey, DefaultInjector injector) {
        super(bindingKey, injector);

        // trigger initialization of the MapProvider right away, as we need to bind an
        // empty map even if the user never calls 'put'
        findOrCreateMapProvider();
    }

    @Override
    public MapBuilder<T> put(String key, Class<? extends T> interfaceType) throws DIRuntimeException {

        Provider<? extends T> provider = createTypeProvider(interfaceType);
        // TODO: andrus 11/15/2009 - report overriding the key??
        findOrCreateMapProvider().put(key, provider);
        return this;
    }

    @Override
    public MapBuilder<T> put(String key, T value) throws DIRuntimeException {
        // TODO: andrus 11/15/2009 - report overriding the key??
        findOrCreateMapProvider().put(key, createInstanceProvider(value));
        return this;
    }

    @Override
    public MapBuilder<T> putAll(Map<String, T> map) throws DIRuntimeException {

        MapProvider<T> provider = findOrCreateMapProvider();

        for (Entry<String, T> entry : map.entrySet()) {
            provider.put(entry.getKey(), createInstanceProvider(entry.getValue()));
        }

        return this;
    }

    private MapProvider<T> findOrCreateMapProvider() {
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
}
