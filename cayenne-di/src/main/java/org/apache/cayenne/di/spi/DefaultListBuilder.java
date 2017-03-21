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
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.di.Scope;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @since 3.1
 */
class DefaultListBuilder<T> implements ListBuilder<T> {

    protected static AtomicLong incrementer = new AtomicLong();
    protected DefaultInjector injector;
    protected Key<List<T>> bindingKey;

    DefaultListBuilder(Key<List<T>> bindingKey, DefaultInjector injector) {
        this.injector = injector;
        this.bindingKey = bindingKey;

        // trigger initialization of the ListProvider right away, as we need to bind an
        // empty list even if the user never calls 'put'
        getListProvider();
    }

    @Override
    public ListBuilder<T> add(Class<? extends T> interfaceType) {

        Provider<? extends T> provider = getProvider(interfaceType);
        getListProvider().add(Key.get(interfaceType), provider);
        return this;
    }

    @Override
    public ListBuilder<T> addAfter(Class<? extends T> interfaceType, Class<? extends T> afterType) {

        Provider<? extends T> provider = getProvider(interfaceType);
        getListProvider().addAfter(Key.get(interfaceType), provider, Key.get(afterType));
        return this;
    }

    @Override
    public ListBuilder<T> insertBefore(Class<? extends T> interfaceType, Class<? extends T> beforeType) {

        Provider<? extends T> provider = getProvider(interfaceType);
        getListProvider().insertBefore(Key.get(interfaceType), provider, Key.get(beforeType));
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ListBuilder<T> add(T value) {
        Key<? extends T> key = Key.get((Class<? extends T>)value.getClass(),
                String.valueOf(incrementer.getAndIncrement()));
        getListProvider().add(key, createProvider(value));
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ListBuilder<T> addAfter(T value, Class<? extends T> afterType) {
        Key<? extends T> key = Key.get((Class<? extends T>)value.getClass(),
                String.valueOf(incrementer.getAndIncrement()));
        getListProvider().addAfter(key, createProvider(value), Key.get(afterType));
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ListBuilder<T> insertBefore(T value, Class<? extends T> beforeType) {
        Key<? extends T> key = Key.get((Class<? extends T>)value.getClass(),
                String.valueOf(incrementer.getAndIncrement()));
        getListProvider().insertBefore(key, createProvider(value), Key.get(beforeType));
        return this;
    }

    @Override
    public ListBuilder<T> addAll(Collection<T> values) {
        getListProvider().addAll(createProviderMap(values));
        return this;
    }

    @Override
    public ListBuilder<T> addAllAfter(Collection<T> values, Class<? extends T> afterType) {
        getListProvider().addAllAfter(createProviderMap(values), Key.get(afterType));
        return this;
    }

    @Override
    public ListBuilder<T> insertAllBefore(Collection<T> values, Class<? extends T> beforeType) {
        getListProvider().insertAllBefore(createProviderMap(values), Key.get(beforeType));
        return this;
    }

    private Provider<? extends T> getProvider(Class<? extends T> interfaceType)
            throws DIRuntimeException {

        Key<? extends T> key = Key.get(interfaceType);
        Binding<? extends T> binding = injector.getBinding(key);
        if (binding == null) {
            return addWithBinding(interfaceType);
        }
        return binding.getScoped();
    }

    @SuppressWarnings("unchecked")
    private Provider<T> createProvider(T value) {
        Provider<T> provider0 = new InstanceProvider<>(value);
        return new FieldInjectingProvider<>(provider0, injector);
    }

    private Map<Key<? extends T>, Provider<? extends T>> createProviderMap(Collection<T> objects) {
        Map<Key<? extends T>, Provider<? extends T>> keyProviderMap = new LinkedHashMap<>();
        for (T object : objects) {
            Provider<T> provider0 = new InstanceProvider<>(object);
            Provider<T> provider1 = new FieldInjectingProvider<>(provider0, injector);

            @SuppressWarnings("unchecked")
            Class<? extends T> objectType = (Class<? extends T>)object.getClass();
            keyProviderMap.put(Key.get(objectType, String.valueOf(incrementer.getAndIncrement())), provider1);
        }

        return keyProviderMap;
    }

    private <K extends T> Provider<? extends T> addWithBinding(Class<K> interfaceType) {
        Key<K> key = Key.get(interfaceType);

        Provider<K> provider0 = new ConstructorInjectingProvider<>(interfaceType, injector);
        Provider<K> provider1 = new FieldInjectingProvider<>(provider0, injector);
        injector.putBinding(key, provider1);

        return injector.getProvider(key);
    }

    private ListProvider<T> getListProvider() {

        ListProvider<T> provider;

        Binding<List<T>> binding = injector.getBinding(bindingKey);
        if (binding == null) {
            provider = new ListProvider<>();
            injector.putBinding(bindingKey, provider);
        } else {
            provider = (ListProvider<T>) binding.getOriginal();
        }

        return provider;
    }

    @Override
    public void in(Scope scope) {
        injector.changeBindingScope(bindingKey, scope);
    }
}
