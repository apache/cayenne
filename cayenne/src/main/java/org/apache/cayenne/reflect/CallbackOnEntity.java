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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.util.Util;

/**
 * Defines a generic callback operation executed via reflection on a persistent object.
 * Note that the method must be declared in the class itself. Callback will not look up
 * the class hierarchy.
 * 
 * @since 3.0
 */
class CallbackOnEntity extends AbstractCallback {

    private final Method callbackMethod;

    CallbackOnEntity(Class<?> objectClass, String methodName) throws IllegalArgumentException {
        this(findMethod(objectClass, methodName));
    }

    /**
     * @since 4.2
     */
    CallbackOnEntity(Method method) throws IllegalArgumentException {
        if(!validateMethod(method)) {
            throw new IllegalArgumentException("Class " + method.getDeclaringClass().getName()
                    + " has no valid callback method '" + method.getName() + "'");
        }
        this.callbackMethod = method;
        if (!Util.isAccessible(callbackMethod)) {
            callbackMethod.setAccessible(true);
        }
    }

    @Override
    public void performCallback(Object entity) {
        try {
            callbackMethod.invoke(entity, (Object[]) null);
        } catch (Exception e) {
            throw new CayenneRuntimeException("Error invoking entity callback method "
                    + callbackMethod.getName(), e);
        }
    }

    @Override
    public String toString() {
        return "callback-entity: "
                + callbackMethod.getDeclaringClass().getName()
                + "."
                + callbackMethod.getName();
    }

    static private boolean validateMethod(Method method) {
        int modifiers = method.getModifiers();
        // must be non-static, void, with no args
        // JPA spec also requires it to be non-final, but we don't care
        return !Modifier.isStatic(modifiers)
                && Void.TYPE.isAssignableFrom(method.getReturnType())
                && method.getParameterTypes().length == 0;
    }

    static private Method findMethod(Class<?> objectClass, String methodName) throws IllegalArgumentException {
        Method method;
        try {
            method = objectClass.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException("Class " + objectClass.getName()
                    + " has no valid callback method '" + methodName + "'");
        }
        return method;
    }
}
