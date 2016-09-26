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
import org.apache.cayenne.di.Provider;

import java.lang.reflect.Field;

/**
 * @since 4.0
 */
class FieldInjectingDecoratorProvider<T> implements DecoratorProvider<T> {

    private Class<? extends T> implementation;
    private DefaultInjector injector;
    private DecoratorProvider<T> delegate;

    FieldInjectingDecoratorProvider(Class<? extends T> implementation, DecoratorProvider<T> delegate,
            DefaultInjector injector) {
        this.delegate = delegate;
        this.injector = injector;
        this.implementation = implementation;
    }

    @Override
    public Provider<T> get(final Provider<T> undecorated) throws DIRuntimeException {
        return new FieldInjectingProvider<T>(delegate.get(undecorated), injector) {

            @Override
            protected Object value(Field field, String bindingName) {
                Class<?> fieldType = field.getType();

                // delegate (possibly) injected as Provider
                if (Provider.class.equals(fieldType)) {

                    Class<?> objectClass = DIUtil.parameterClass(field.getGenericType());

                    if (objectClass == null) {
                        throw new DIRuntimeException("Provider field %s.%s of type %s must be "
                                + "parameterized to be usable for injection", field.getDeclaringClass().getName(),
                                field.getName(), fieldType.getName());
                    }

                    if(objectClass.isAssignableFrom(implementation)) {
                        return undecorated;
                    }
                }
                else if (fieldType.isAssignableFrom(implementation)) {
                    return undecorated.get();
                }

                return super.value(field, bindingName);
            }
        };
    }
}
