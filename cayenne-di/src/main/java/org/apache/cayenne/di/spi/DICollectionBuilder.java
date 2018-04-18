/*
 *    Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.di.spi;

import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.di.Scope;


/**
 * A superclass of DI List and Map builders.
 *
 * @param <K> DI key type.
 * @param <E> Collection element type.
 * @since 4.0
 */
public abstract class DICollectionBuilder<K, E> {

    protected DefaultInjector injector;
    protected Key<K> bindingKey;

    public DICollectionBuilder(Key<K> bindingKey, DefaultInjector injector) {
        this.injector = injector;
        this.bindingKey = bindingKey;
    }

    protected Provider<E> createInstanceProvider(E value) {
        Provider<E> provider0 = new InstanceProvider<>(value);
        return new FieldInjectingProvider<>(provider0, injector);
    }

    protected <SubT extends E> Provider<SubT> createTypeProvider(final Class<SubT> interfaceType) throws DIRuntimeException {

        // Create deferred provider to prevent caching the intermediate provider from the Injector.
        // The actual provider may get overridden after list builder is created.

        return new Provider<SubT>() {

            @Override
            public SubT get() throws DIRuntimeException {
                return findOrCreateBinding(interfaceType).getScoped().get();
            }
        };
    }

    protected <SubT extends E> Binding<SubT> findOrCreateBinding(Class<SubT> interfaceType) {

        Key<SubT> key = Key.get(interfaceType);
        Binding<SubT> binding = injector.getBinding(key);

        if (binding == null) {

            Provider<SubT> provider0 = new ConstructorInjectingProvider<>(interfaceType, injector);
            Provider<SubT> provider1 = new FieldInjectingProvider<>(provider0, injector);
            injector.putBinding(key, provider1);

            binding = injector.getBinding(key);
        }

        return binding;
    }

    public void in(Scope scope) {
        injector.changeBindingScope(bindingKey, scope);
    }

    /**
     * @since 4.1
     */
    public void withoutScope() {
        in(injector.getNoScope());
    }
}
