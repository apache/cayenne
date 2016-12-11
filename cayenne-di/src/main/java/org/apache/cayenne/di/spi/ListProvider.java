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
class ListProvider implements Provider<List<?>> {

    private Map<Key<?>, Provider<?>> providers;
    private DIGraph<Key<?>> graph;
    private Key<?> lastKey;
    private Collection<Key<?>> lastKeys;

    public ListProvider() {
        this.providers = new HashMap<>();
        this.graph = new DIGraph<>();
        this.lastKeys = Collections.emptySet();
    }

    @Override
    public List<?> get() throws DIRuntimeException {
        List<Key<?>> insertOrder = graph.topSort();

        if (insertOrder.size() != providers.size()) {
            List<Key<?>> emptyKeys = new ArrayList<>();

            for (Key<?> key : insertOrder) {
                if (!providers.containsKey(key)) {
                    emptyKeys.add(key);
                }
            }

            throw new DIRuntimeException("DI list has no providers for keys: %s", emptyKeys);
        }

        List<Object> list = new ArrayList<>(insertOrder.size());
        for (Key<?> key : insertOrder) {
            list.add(providers.get(key).get());
        }

        return list;
    }

    void add(Key<?> key, Provider<?> provider) {
        providers.put(key, provider);
        graph.add(key);
        lastKey = key;
        lastKeys.clear();
    }

    void addAll(Map<Key<?>, Provider<?>> keyProviderMap) {
        providers.putAll(keyProviderMap);
        graph.addAll(keyProviderMap.keySet());
        lastKeys = keyProviderMap.keySet();
    }

    void after(Key<?> after) {
        if (!lastKeys.isEmpty()) {
            for (Key<?> key : lastKeys) {
                graph.add(key, after);
            }

            return;
        }

        graph.add(lastKey, after);
    }

    void before(Key<?> before) {
        if (!lastKeys.isEmpty()) {
            for (Key<?> key: lastKeys) {
                graph.add(before, key);
            }

            return;
        }

        graph.add(before, lastKey);
    }

}
