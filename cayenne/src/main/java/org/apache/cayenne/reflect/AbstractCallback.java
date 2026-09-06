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
import java.lang.reflect.Method;

/**
 * Defines a callback operation.
 * 
 * @since 3.0
 */
abstract class AbstractCallback {

    /**
     * The uniform shape every callback handle is adapted to, so that it can be called with "invokeExact".
     */
    static final MethodType CALLBACK_TYPE = MethodType.methodType(void.class, Object.class);

    abstract void performCallback(Object entity);

    /**
     * Turns a validated (and, if needed, made accessible) callback method into a method handle.
     */
    static MethodHandle unreflect(Method method) {
        try {
            return MethodHandles.lookup().unreflect(method);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Inaccessible callback method: " + method, e);
        }
    }

    /**
     * Rethrows an exception raised by a callback. Unchecked exceptions and errors propagate as they are, checked
     * exceptions are wrapped.
     */
    static RuntimeException callbackFailure(String callbackName, Throwable th) {
        if (th instanceof RuntimeException re) {
            return re;
        }
        if (th instanceof Error error) {
            throw error;
        }
        return new CayenneRuntimeException("Error invoking callback method " + callbackName, th);
    }

    static String callbackName(Method method) {
        return method.getDeclaringClass().getName() + "." + method.getName();
    }
}
