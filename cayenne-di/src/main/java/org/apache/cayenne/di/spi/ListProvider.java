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

import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 3.1
 */
class ListProvider<T> implements Provider<List<T>> {

    private Map<Key<? extends T>, Provider<? extends T>> providers;
    private DIGraph<Key<? extends T>> graph;

    public ListProvider() {
        this.providers = new HashMap<>();
        this.graph = new DIGraph<>();
    }

    @Override
    public List<T> get() throws DIRuntimeException {
        List<Key<? extends T>> insertOrder = graph.topSort();

        if (insertOrder.size() != providers.size()) {
            List<Key<? extends T>> emptyKeys = new ArrayList<>();

            for (Key<? extends T> key : insertOrder) {
                if (!providers.containsKey(key)) {
                    emptyKeys.add(key);
                }
            }

            throw new DIRuntimeException("DI list has no providers for keys: %s", emptyKeys);
        }

        List<T> list = new ArrayList<>(insertOrder.size());
        for (Key<? extends T> key : insertOrder) {
            list.add(providers.get(key).get());
        }

        return list;
    }

    void add(Key<? extends T> key, Provider<? extends T> provider) {
        providers.put(key, provider);
        graph.add(key);
    }

    void addAfter(Key<? extends T> key, Provider<? extends T> provider, Key<? extends T> after) {
        providers.put(key, provider);
        graph.addWithOverride(key, after);
    }

    void insertBefore(Key<? extends T> key, Provider<? extends T> provider, Key<? extends T> before) {
        providers.put(key, provider);
        graph.addWithOverride(before, key);
    }

    void addAll(Map<Key<? extends T>, Provider<? extends T>> keyProviderMap) {
        providers.putAll(keyProviderMap);
        graph.addAll(keyProviderMap.keySet());
    }

    void addAllAfter(Map<Key<? extends T>, Provider<? extends T>> keyProviderMap, Key<? extends T> after) {
        providers.putAll(keyProviderMap);
        for (Key<? extends T> key : keyProviderMap.keySet()) {
            graph.addWithOverride(key, after);
        }
    }

    void insertAllBefore(Map<Key<? extends T>, Provider<? extends T>> keyProviderMap, Key<? extends T> before) {
        providers.putAll(keyProviderMap);
        for (Key<? extends T> key : keyProviderMap.keySet()) {
            graph.addWithOverride(before, key);
        }
    }
}
