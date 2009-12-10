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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;

/**
 * @since 3.1
 */
class ConstructorInjectingProvider<T> implements Provider<T> {

    private Class<T> interfaceType;
    private Constructor<? extends T> constructor;
    private DefaultInjector injector;

    ConstructorInjectingProvider(Class<T> interfaceType,
            Class<? extends T> implementation, DefaultInjector injector) {

        initConstructor(implementation);

        if (constructor == null) {
            throw new ConfigurationException(
                    "Can't find approprate constructor for implementation class '%s'",
                    implementation.getName());
        }

        this.constructor.setAccessible(true);
        this.injector = injector;
        this.interfaceType = interfaceType;
    }

    private void initConstructor(Class<? extends T> implementation) {

        Constructor<?>[] constructors = implementation.getDeclaredConstructors();
        Constructor<?> lastMatch = null;
        int lastSize = -1;

        // pick the first constructor with all injection-annotated parameters, or the
        // default constructor; constructor with the longest parameter list is preferred
        // if multiple matches are found
        for (Constructor<?> constructor : constructors) {

            int size = constructor.getParameterTypes().length;
            if (size <= lastSize) {
                continue;
            }

            if (size == 0) {
                lastSize = 0;
                lastMatch = constructor;
                continue;
            }

            boolean injectable = true;
            for (Annotation[] annotations : constructor.getParameterAnnotations()) {

                boolean parameterInjectable = false;
                for (Annotation annotation : annotations) {
                    if (annotation.annotationType().equals(Inject.class)) {
                        parameterInjectable = true;
                        break;
                    }
                }

                if (!parameterInjectable) {
                    injectable = false;
                    break;
                }
            }

            if (injectable) {
                lastSize = size;
                lastMatch = constructor;
            }
        }

        // the cast is lame, but Class.getDeclaredConstructors() is not using
        // generics in Java 5 and using <?> in Java 6, creating compilation problems.
        this.constructor = (Constructor<? extends T>) lastMatch;
    }

    public T get() {

        Class<?>[] constructorParameters = constructor.getParameterTypes();
        Type[] genericTypes = constructor.getGenericParameterTypes();
        Object[] args = new Object[constructorParameters.length];
        InjectionStack stack = injector.getInjectionStack();

        for (int i = 0; i < constructorParameters.length; i++) {

            Class<?> parameter = constructorParameters[i];

            // parameter must be declared as "java.util.Map". Use of specific map
            // implementations in the declaration will make the injection much more
            // complicated
            if (Map.class.equals(parameter)) {
                args[i] = injector.getMapConfiguration(interfaceType);
            }
            // parameter must be declared as "java.util.List". Use of specific list
            // implementations in the declaration will make the injection much more
            // complicated
            else if (List.class.equals(parameter)) {
                args[i] = injector.getListConfiguration(interfaceType);
            }
            else if (Provider.class.equals(parameter)) {

                Class<?> objectClass = DIUtil.parameterClass(genericTypes[i]);

                if (objectClass == null) {
                    throw new ConfigurationException(
                            "Constructor provider parameter %s must be "
                                    + "parameterized to be usable for injection",
                            parameter.getName());
                }

                args[i] = injector.getProvider(objectClass);
            }
            else {

                stack.push(DIUtil.toKey(parameter));
                try {
                    args[i] = injector.getInstance(parameter);
                }
                finally {
                    stack.pop();
                }
            }
        }

        try {
            return constructor.newInstance(args);
        }
        catch (Exception e) {
            throw new ConfigurationException(
                    "Error instantiating class '%s'",
                    e,
                    constructor.getDeclaringClass().getName());
        }
    }
}
