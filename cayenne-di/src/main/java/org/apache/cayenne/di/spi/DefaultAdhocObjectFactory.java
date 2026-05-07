/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.di.spi;

import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Provider;

/**
 * A default implementation of {@link AdhocObjectFactory} that creates objects
 * using default no-arg constructor and injects dependencies into annotated
 * fields. Note that constructor injection is not supported by this factory.
 * 
 * @since 3.1
 */
public class DefaultAdhocObjectFactory implements AdhocObjectFactory {

    protected Injector injector;
    protected ClassLoaderManager classLoaderManager;

    /**
     * @since 4.0
     */
    public DefaultAdhocObjectFactory(@Inject Injector injector, @Inject ClassLoaderManager classLoaderManager) {
        this.injector = injector;
        this.classLoaderManager = classLoaderManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T newInstance(Class<? super T> superType, String className) {

        if (superType == null) {
            throw new NullPointerException("Null superType");
        }

        if (className == null) {
            throw new NullPointerException("Null className");
        }

        Class<T> type = (Class<T>) getJavaClass(className);

        if (!superType.isAssignableFrom(type)) {
            throw new DIRuntimeException("Class %s is not assignable to %s", className, superType.getName());
        }

        T instance;
        try {
            Provider<T> provider0 = new ConstructorInjectingProvider<>(type, (DefaultInjector) injector);
            Provider<T> provider1 = new FieldInjectingProvider<>(provider0, (DefaultInjector) injector);
            instance = provider1.get();
        } catch (Exception e) {
            throw new DIRuntimeException("Error creating instance of class %s of type %s", e, className,
                    superType.getName());
        }

        return instance;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<?> getJavaClass(String className) {
        // is there a better way to get array class from string name?
        if (className == null) {
            throw new NullPointerException("Null class name");
        }

        ClassLoader classLoader = classLoaderManager.getClassLoader(className.replace('.', '/'));

        // use custom logic on failure only, assuming primitives and arrays are not that common
        try {
            return Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException e) {
            if (!className.endsWith("[]")) {
                return switch (className) {
                    case "byte" -> Byte.TYPE;
                    case "int" -> Integer.TYPE;
                    case "short" -> Short.TYPE;
                    case "char" -> Character.TYPE;
                    case "double" -> Double.TYPE;
                    case "long" -> Long.TYPE;
                    case "float" -> Float.TYPE;
                    case "boolean" -> Boolean.TYPE;
                    case "void" -> Void.TYPE;
                    // try inner class often specified with "." instead of $
                    default -> {
                        int dot = className.lastIndexOf('.');
                        if (dot > 0 && dot + 1 < className.length()) {
                            String innerClass = className.substring(0, dot) + "$" + className.substring(dot + 1);
                            try {
                                yield Class.forName(innerClass, true, classLoader);
                            } catch (ClassNotFoundException nestedE) {
                                // ignore, throw the original exception...
                            }
                        }
                        throw new DIRuntimeException("Invalid class: '%s'", e, className);
                    }
                };
            }

            if (className.length() < 3) {
                throw new IllegalArgumentException("Invalid class name: '" + className + "'");
            }

            // TODO: support for multi-dim arrays
            className = className.substring(0, className.length() - 2);

            return switch (className) {
                case "byte" -> byte[].class;
                case "int" -> int[].class;
                case "long" -> long[].class;
                case "short" -> short[].class;
                case "char" -> char[].class;
                case "double" -> double[].class;
                case "float" -> float[].class;
                case "boolean" -> boolean[].class;
                default -> {
                    try {
                        yield Class.forName("[L" + className + ";", true, classLoader);
                    } catch (ClassNotFoundException e1) {
                        throw new DIRuntimeException("Invalid class: '%s'", e1, className);
                    }
                }
            };
        }
    }
}
