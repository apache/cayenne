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

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.di.BindingBuilder;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.di.Scope;

/**
 * @since 3.1
 */
class DefaultBindingBuilder<T> implements BindingBuilder<T> {

    protected DefaultInjector injector;
    protected Key<T> bindingKey;

    DefaultBindingBuilder(Key<T> bindingKey, DefaultInjector injector) {
        this.injector = injector;
        this.bindingKey = bindingKey;
    }

    public BindingBuilder<T> to(Class<? extends T> implementation)
            throws ConfigurationException {

        Provider<T> provider0 = new ConstructorInjectingProvider<T>(
                implementation,
                injector);
        Provider<T> provider1 = new FieldInjectingProvider<T>(provider0, injector);

        injector.putBinding(bindingKey, provider1);
        return this;
    }

    public BindingBuilder<T> toInstance(T instance) throws ConfigurationException {
        Provider<T> provider0 = new InstanceProvider<T>(instance);
        Provider<T> provider1 = new FieldInjectingProvider<T>(provider0, injector);
        injector.putBinding(bindingKey, provider1);
        return this;
    };

    public BindingBuilder<T> toProvider(
            Class<? extends Provider<? extends T>> providerType) {

        Provider<Provider<? extends T>> provider0 = new ConstructorInjectingProvider<Provider<? extends T>>(
                providerType,
                injector);
        Provider<Provider<? extends T>> provider1 = new FieldInjectingProvider<Provider<? extends T>>(
                provider0,
                injector);

        Provider<T> provider2 = new CustomProvidersProvider<T>(provider1);
        Provider<T> provider3 = new FieldInjectingProvider<T>(provider2, injector);

        injector.putBinding(bindingKey, provider3);
        return this;
    }

    public BindingBuilder<T> toProviderInstance(Provider<? extends T> provider) {

        Provider<Provider<? extends T>> provider0 = new InstanceProvider<Provider<? extends T>>(
                provider);
        Provider<Provider<? extends T>> provider1 = new FieldInjectingProvider<Provider<? extends T>>(
                provider0,
                injector);

        Provider<T> provider2 = new CustomProvidersProvider<T>(provider1);
        Provider<T> provider3 = new FieldInjectingProvider<T>(provider2, injector);

        injector.putBinding(bindingKey, provider3);
        return this;
    }

    public void in(Scope scope) {
        injector.changeBindingScope(bindingKey, scope);
    }

    public void withoutScope() {
        in(injector.getNoScope());
    }

    public void inSingletonScope() {
        in(injector.getSingletonScope());
    }
}
