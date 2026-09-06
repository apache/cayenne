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
package org.apache.cayenne.reflect;

import org.apache.cayenne.CayenneRuntimeException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;

/**
 * A cached handle to the no-arg constructor of a class. Resolving a constructor via
 * {@link Class#getDeclaredConstructor(Class[])} is comparatively expensive (a lookup, a defensive copy and an access
 * check), so callers that instantiate the same class repeatedly should resolve it once and keep this object around.
 *
 * @since 5.0
 */
public final class DefaultConstructor<T> {

    private static final MethodType NO_ARGS = MethodType.methodType(Object.class);

    private final Class<T> type;
    private final MethodHandle handle;

    public DefaultConstructor(Class<T> type) {
        this.type = type;
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }
            this.handle = MethodHandles.lookup().unreflectConstructor(constructor).asType(NO_ARGS);
        } catch (NoSuchMethodException e) {
            throw new CayenneRuntimeException("No default constructor found for class '%s'", e, type.getName());
        } catch (IllegalAccessException | RuntimeException e) {
            throw new CayenneRuntimeException("Default constructor of class '%s' is inaccessible", e, type.getName());
        }
    }

    public Class<T> getType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    public T newInstance() {
        try {
            return (T) handle.invokeExact();
        } catch (Throwable e) {
            throw new CayenneRuntimeException("Error creating object of class '%s'", e, type.getName());
        }
    }
}
