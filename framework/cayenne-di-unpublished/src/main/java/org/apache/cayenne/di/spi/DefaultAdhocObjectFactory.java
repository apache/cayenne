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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Provider;

/**
 * A default implementation of {@link AdhocObjectFactory} that creates objects using
 * default no-arg constructor and injects dependencies into annotated fields. Note that
 * constructor injection is not supported by this factory.
 * 
 * @since 3.1
 */
public class DefaultAdhocObjectFactory implements AdhocObjectFactory {

    @Inject
    protected Injector injector;

    public <T> T newInstance(Class<? super T> superType, String className) {

        if (superType == null) {
            throw new NullPointerException("Null superType");
        }

        if (className == null) {
            throw new NullPointerException("Null className");
        }

        Class<T> type;
        try {
            type = (Class<T>)getJavaClass(className);
        }
        catch (ClassNotFoundException e) {
            throw new CayenneRuntimeException(
                    "Invalid class %s of type %s",
                    e,
                    className,
                    superType.getName());
        }

        if (!superType.isAssignableFrom(type)) {
            throw new CayenneRuntimeException(
                    "Class %s is not assignable to %s",
                    className,
                    superType.getName());
        }

        T instance;
        try {
            Provider<T> provider0 = new ConstructorInjectingProvider<T>(type, (DefaultInjector)injector);
            Provider<T> provider1 = new FieldInjectingProvider<T>(provider0, (DefaultInjector)injector);
            instance = provider1.get();
        }
        catch (Exception e) {
            throw new CayenneRuntimeException(
                    "Error creating instance of class %s of type %s",
                    e,
                    className,
                    superType.getName());
        }

        return instance;
    }

    public Class<?> getJavaClass(String className) throws ClassNotFoundException {

        // is there a better way to get array class from string name?

        if (className == null) {
            throw new ClassNotFoundException("Null class name");
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        if (classLoader == null) {
            classLoader = DefaultAdhocObjectFactory.class.getClassLoader();
        }

        // use custom logic on failure only, assuming primitives and arrays are not that
        // common
        try {
            return Class.forName(className, true, classLoader);
        }
        catch (ClassNotFoundException e) {
            if (!className.endsWith("[]")) {
                if ("byte".equals(className)) {
                    return Byte.TYPE;
                }
                else if ("int".equals(className)) {
                    return Integer.TYPE;
                }
                else if ("short".equals(className)) {
                    return Short.TYPE;
                }
                else if ("char".equals(className)) {
                    return Character.TYPE;
                }
                else if ("double".equals(className)) {
                    return Double.TYPE;
                }
                else if ("long".equals(className)) {
                    return Long.TYPE;
                }
                else if ("float".equals(className)) {
                    return Float.TYPE;
                }
                else if ("boolean".equals(className)) {
                    return Boolean.TYPE;
                }
                // try inner class often specified with "." instead of $
                else {
                    int dot = className.lastIndexOf('.');
                    if (dot > 0 && dot + 1 < className.length()) {
                        className = className.substring(0, dot)
                                + "$"
                                + className.substring(dot + 1);
                        try {
                            return Class.forName(className, true, classLoader);
                        }
                        catch (ClassNotFoundException nestedE) {
                            // ignore, throw the original exception...
                        }
                    }
                }

                throw e;
            }

            if (className.length() < 3) {
                throw new IllegalArgumentException("Invalid class name: " + className);
            }

            // TODO: support for multi-dim arrays
            className = className.substring(0, className.length() - 2);

            if ("byte".equals(className)) {
                return byte[].class;
            }
            else if ("int".equals(className)) {
                return int[].class;
            }
           else if ("long".equals(className)) {
               return long[].class;
           }
            else if ("short".equals(className)) {
                return short[].class;
            }
            else if ("char".equals(className)) {
                return char[].class;
            }
            else if ("double".equals(className)) {
                return double[].class;
            }
            else if ("float".equals(className)) {
                return float[].class;
            }
            else if ("boolean".equals(className)) {
                return boolean[].class;
            }

            return Class.forName("[L" + className + ";", true, classLoader);
        }
    }
}
