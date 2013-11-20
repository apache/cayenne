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
package org.apache.cayenne.di;

import org.apache.cayenne.di.DIRuntimeException;


/**
 * A binding builder that helps with fluent binding creation.
 * 
 * @param <T> An interface type of the service being bound.
 * @since 3.1
 */
public interface BindingBuilder<T> {

    BindingBuilder<T> to(Class<? extends T> implementation) throws DIRuntimeException;

    BindingBuilder<T> toInstance(T instance) throws DIRuntimeException;

    BindingBuilder<T> toProvider(Class<? extends Provider<? extends T>> providerType)
            throws DIRuntimeException;

    BindingBuilder<T> toProviderInstance(Provider<? extends T> provider)
            throws DIRuntimeException;

    /**
     * Sets the scope of a bound instance. This method is used to change the default scope
     * which is usually a singleton to a custom scope.
     */
    void in(Scope scope);

    /**
     * Sets the scope of a bound instance to singleton. Singleton is normally the default,
     * so calling this method explicitly is rarely needed.
     */
    void inSingletonScope();

    /**
     * Sets the scope of a bound instance to "no scope". This means that a new instance of
     * an object will be created on every call to {@link Injector#getInstance(Class)}.
     */
    void withoutScope();
}
