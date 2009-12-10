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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;

/**
 * @since 3.1
 */
class FieldInjectingProvider<T> implements Provider<T> {

    private String bindingKey;
    private DefaultInjector injector;
    private Provider<T> delegate;

    FieldInjectingProvider(Provider<T> delegate, DefaultInjector injector,
            String bindingKey) {
        this.delegate = delegate;
        this.injector = injector;
        this.bindingKey = bindingKey;
    }

    private Collection<Field> initInjectionPoints(
            Class<?> type,
            Collection<Field> injectableFields) {

        if (type == null) {
            return injectableFields;
        }

        for (Field field : type.getDeclaredFields()) {

            Inject inject = field.getAnnotation(Inject.class);
            if (inject != null) {
                field.setAccessible(true);
                injectableFields.add(field);
            }
        }

        return initInjectionPoints(type.getSuperclass(), injectableFields);
    }

    public T get() throws ConfigurationException {
        T object = delegate.get();
        injectMembers(object);
        return object;
    }

    private void injectMembers(T object) {

        Collection<Field> injectableFields = initInjectionPoints(
                object.getClass(),
                new ArrayList<Field>());

        InjectionStack stack = injector.getInjectionStack();

        for (Field field : injectableFields) {
            Object value;
            Class<?> fieldType = field.getType();

            if (Provider.class.equals(fieldType)) {

                Class<?> objectClass = DIUtil.parameterClass(field.getGenericType());

                if (objectClass == null) {
                    throw new ConfigurationException(
                            "Provider field %s.%s of type %s must be "
                                    + "parameterized to be usable for injection",
                            field.getDeclaringClass().getName(),
                            field.getName(),
                            fieldType.getName());
                }

                value = injector.getProvider(objectClass);
            }
            else {
                stack.push(bindingKey);
                try {
                    value = injector.getInstance(fieldType);
                }
                finally {
                    stack.pop();
                }
            }

            try {
                field.set(object, value);
            }
            catch (Exception e) {
                String message = String.format(
                        "Error injecting into field %s.%s of type %s",
                        field.getDeclaringClass().getName(),
                        field.getName(),
                        fieldType.getName());
                throw new ConfigurationException(message, e);
            }
        }

    }

}
