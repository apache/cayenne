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

import java.lang.reflect.Type;

import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Provider;

/**
 * @since 3.2
 */
public class ConstructorInjectingDecoratorProvider<T> implements DecoratorProvider<T> {

    private Class<? extends T> implementation;
    private DefaultInjector injector;

    public ConstructorInjectingDecoratorProvider(Class<? extends T> implementation, DefaultInjector injector) {
        this.implementation = implementation;
        this.injector = injector;
    }

    @Override
    public Provider<T> get(final Provider<T> undecorated) throws DIRuntimeException {

        return new ConstructorInjectingProvider<T>(implementation, injector) {
            @Override
            protected Object value(Class<?> parameter, Type genericType, String bindingName, InjectionStack stack) {

                if (parameter.isAssignableFrom(implementation)) {
                    return undecorated.get();
                }

                return super.value(parameter, genericType, bindingName, stack);
            }
        };
    }
}
