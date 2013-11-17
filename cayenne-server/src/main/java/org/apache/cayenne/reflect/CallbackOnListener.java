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
package org.apache.cayenne.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.util.Util;

/**
 * Defines a generic callback operation executed via reflection on an arbitrary listener
 * object. Note that the method must be declared in the class itself. Callback will not
 * look up the class hierarchy.
 * 
 * @since 3.0
 */
class CallbackOnListener extends AbstractCallback {

    private Method callbackMethod;
    private Object listener;

    CallbackOnListener(Object listener, String methodName)
            throws IllegalArgumentException {
        this(listener, methodName, Object.class);
    }

    CallbackOnListener(Object listener, String methodName, Class<?> entityType)
            throws IllegalArgumentException {

        if (listener == null) {
            throw new IllegalArgumentException("Null listener");
        }

        this.callbackMethod = findMethod(listener.getClass(), methodName, entityType);
        this.listener = listener;
    }

    CallbackOnListener(Object listener, Method method, Class<?> entityType)
            throws IllegalArgumentException {

        if (listener == null) {
            throw new IllegalArgumentException("Null listener");
        }

        if (!verifyMethod(method, entityType)) {
            throw new IllegalArgumentException("Invalid annotated listener method: "
                    + method.getName());
        }

        this.callbackMethod = method;
        this.listener = listener;
    }

    @Override
    public void performCallback(Object entity) {
        try {
            callbackMethod.invoke(listener, entity);
        }
        catch (Exception e) {
            throw new CayenneRuntimeException(
                    "Error invoking entity listener callback method "
                            + callbackMethod.getName(),
                    e);
        }
    }

    @Override
    public String toString() {
        return "callback-listener: "
                + callbackMethod.getDeclaringClass().getName()
                + "."
                + callbackMethod.getName();
    }

    private boolean verifyMethod(Method method, Class<?> entityType) {
        // must be non-static, void, with a single arg assignable to entity type
        // JPA spec also requires it to be non-final, but we don't care
        int modifiers = method.getModifiers();
        Class<?>[] parameters = method.getParameterTypes();
        if (!Modifier.isStatic(modifiers)
                && Void.TYPE.isAssignableFrom(method.getReturnType())
                && parameters.length == 1
                && parameters[0].isAssignableFrom(entityType)) {

            if (!Util.isAccessible(method)) {
                method.setAccessible(true);
            }

            return true;
        }

        return false;
    }

    private Method findMethod(Class<?> objectClass, String methodName, Class<?> entityType)
            throws IllegalArgumentException {

        Method[] methods = objectClass.getDeclaredMethods();
        for (Method method : methods) {
            if (methodName.equals(method.getName()) && verifyMethod(method, entityType)) {
                return method;
            }
        }

        Class<?> superclass = objectClass.getSuperclass();
        if (superclass == null || "java.lang.Object".equals(superclass.getName())) {

            throw new IllegalArgumentException("Class "
                    + objectClass.getName()
                    + " has no valid listener callback method '"
                    + methodName
                    + "'");
        }
        else {
            return findMethod(superclass, methodName, entityType);
        }
    }
}
