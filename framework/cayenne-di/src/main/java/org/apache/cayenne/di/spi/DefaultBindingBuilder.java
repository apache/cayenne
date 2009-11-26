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

import org.apache.cayenne.di.BindingBuilder;
import org.apache.cayenne.di.DIException;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.di.Scope;
import org.apache.cayenne.di.Scopes;

/**
 * @since 3.1
 */
class DefaultBindingBuilder<T> implements BindingBuilder<T> {

    private Class<T> interfaceType;
    private DefaultInjector injector;
    private Scope scope;
    private String key;

    DefaultBindingBuilder(Class<T> interfaceType, DefaultInjector injector) {
        this.interfaceType = interfaceType;
        this.injector = injector;
        this.key = DIUtil.toKey(interfaceType);
    }

    public BindingBuilder<T> to(Class<? extends T> implementation) throws DIException {

        Provider<T> provider0 = new ConstructorInjectingProvider<T>(
                interfaceType,
                implementation,
                injector);
        Provider<T> provider1 = new FieldInjectingProvider<T>(provider0, injector, key);
        return bindInScope(provider1);
    }

    public BindingBuilder<T> toInstance(T instance) throws DIException {
        return bindInScope(new InstanceProvider<T>(instance));
    };

    public BindingBuilder<T> toProvider(
            Class<? extends Provider<? extends T>> providerType) {

        Provider<Provider<? extends T>> provider0 = new ConstructorInjectingProvidersProvider<T>(
                providerType);
        Provider<Provider<? extends T>> provider1 = new FieldInjectingProvider<Provider<? extends T>>(
                provider0,
                injector,
                key);

        Provider<T> provider2 = new CustomProvidersProvider<T>(provider1);
        Provider<T> provider3 = new FieldInjectingProvider<T>(provider2, injector, key);

        return bindInScope(provider3);
    }

    public void in(Scope scope) {

        if (this.scope != scope) {

            if (this.scope != null) {
                throw new IllegalStateException(
                        "Can't change binding scope. It is already set to " + this.scope);
            }

            this.scope = scope;

            if (key != null) {
                Provider<?> provider = injector.getBindings().get(key);
                injector.getBindings().put(key, scope.scope(provider));
            }
        }
    }

    private BindingBuilder<T> bindInScope(Provider<T> provider) {

        Scope scope = this.scope != null ? this.scope : Scopes.NO_SCOPE;

        // TODO: andrus 11/15/2009 - report overriding existing binding??
        injector.getBindings().put(key, scope.scope(provider));
        return this;
    }
}
