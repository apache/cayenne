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
 * @author Andrus Adamchik
 */
class CallbackOnListener extends AbstractCallback {

    private Method callbackMethod;
    private Object listener;

    CallbackOnListener(Object listener, String methodName)
            throws IllegalArgumentException {
        this(listener, methodName, Object.class);
    }

    CallbackOnListener(Object listener, String methodName, Class entityType)
            throws IllegalArgumentException {

        if (listener == null) {
            throw new IllegalArgumentException("Null listener");
        }

        this.callbackMethod = findMethod(listener.getClass(), methodName, entityType);
        this.listener = listener;
    }

    public void performCallback(Object entity) {
        try {
            callbackMethod.invoke(listener, new Object[] {
                entity
            });
        }
        catch (Exception e) {
            throw new CayenneRuntimeException(
                    "Error invoking entity listener callback method "
                            + callbackMethod.getName(),
                    e);
        }
    }

    private Method findMethod(Class objectClass, String methodName, Class entityType)
            throws IllegalArgumentException {

        Method[] methods = objectClass.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methodName.equals(methods[i].getName())) {

                // must be non-static, void, with a single arg assinable to entity type
                // JPA spec also requires it to be non-final, but we don't care
                int modifiers = methods[i].getModifiers();
                Class[] parameters = methods[i].getParameterTypes();
                if (!Modifier.isStatic(modifiers)
                        && Void.TYPE.isAssignableFrom(methods[i].getReturnType())
                        && parameters.length == 1
                        && parameters[0].isAssignableFrom(entityType)) {

                    if (!Util.isAccessible(methods[i])) {
                        methods[i].setAccessible(true);
                    }

                    return methods[i];
                }
            }
        }

        throw new IllegalArgumentException("Class "
                + objectClass.getName()
                + " has no valid listener callback method '"
                + methodName
                + "'");
    }
}
