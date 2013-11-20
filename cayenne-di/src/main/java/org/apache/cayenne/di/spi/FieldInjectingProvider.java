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

import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Provider;

/**
 * @since 3.1
 */
class FieldInjectingProvider<T> implements Provider<T> {

    private DefaultInjector injector;
    private Provider<T> delegate;

    FieldInjectingProvider(Provider<T> delegate, DefaultInjector injector) {
        this.delegate = delegate;
        this.injector = injector;
    }

    public T get() throws DIRuntimeException {
        T object = delegate.get();
        injectMembers(object, object.getClass());
        return object;
    }

    private void injectMembers(T object, Class<?> type) {

        // bail on recursion stop condition
        if (type == null) {
            return;
        }

        for (Field field : type.getDeclaredFields()) {

            Inject inject = field.getAnnotation(Inject.class);
            if (inject != null) {
                injectMember(object, field, inject.value());
            }
        }

        injectMembers(object, type.getSuperclass());
    }

    private void injectMember(Object object, Field field, String bindingName) {

        InjectionStack stack = injector.getInjectionStack();

        Object value;
        Class<?> fieldType = field.getType();

        if (Provider.class.equals(fieldType)) {

            Class<?> objectClass = DIUtil.parameterClass(field.getGenericType());

            if (objectClass == null) {
                throw new DIRuntimeException(
                        "Provider field %s.%s of type %s must be "
                                + "parameterized to be usable for injection",
                        field.getDeclaringClass().getName(),
                        field.getName(),
                        fieldType.getName());
            }

            value = injector.getProvider(Key.get(objectClass, bindingName));
        }
        else {

            Key<?> key = Key.get(fieldType, bindingName);

            stack.push(key);
            try {
                value = injector.getInstance(key);
            }
            finally {
                stack.pop();
            }
        }

        field.setAccessible(true);
        try {
            field.set(object, value);
        }
        catch (Exception e) {
            String message = String.format(
                    "Error injecting into field %s.%s of type %s",
                    field.getDeclaringClass().getName(),
                    field.getName(),
                    fieldType.getName());
            throw new DIRuntimeException(message, e);
        }
    }
}
