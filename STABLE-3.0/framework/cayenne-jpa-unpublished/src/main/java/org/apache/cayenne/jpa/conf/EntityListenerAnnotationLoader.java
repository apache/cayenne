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


package org.apache.cayenne.jpa.conf;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import org.apache.cayenne.jpa.map.JpaEntityListener;
import org.apache.cayenne.jpa.map.JpaLifecycleCallback;

/**
 * Loads annotations from the entity listener class. Only deals with non-entity formats of
 * annotation methods.
 * <h3>JPA Spec, 3.4.1:</h3>
 * <p>
 * Callback methods defined on an entity class have the following signature: <em>void
 * <METHOD>()</em>
 * Callback methods defined on an entity listener class have the following signature:
 * <em>void <METHOD>(Object)</em> The Object argument is the entity instance for which
 * the callback method is invoked. It maybe declared as the actual entity type. The
 * callback methods can have public, private, protected, or package level access, but must
 * not be static or final.
 * </p>
 * 
 */
public class EntityListenerAnnotationLoader {

    /**
     * Returns a listener methods descriptor for the annotated listener, or null if none
     * of the class methods are properly annotated.
     */
    public JpaEntityListener getEntityListener(Class<?> listenerClass) {
        JpaEntityListener listener = new JpaEntityListener();

        boolean hasAnnotations = false;
        Method[] methods = listenerClass.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {

            if (isValidListenerMethod(methods[i])) {
                if (processAnnotations(methods[i], listener)) {
                    hasAnnotations = true;
                }
            }
        }

        if (hasAnnotations) {
            listener.setClassName(listenerClass.getName());
            return listener;
        }

        return null;
    }

    /**
     * Checks that the method signature is one of a valid listener method,
     * <em>void METHOD(Object)</em>.
     */
    protected boolean isValidListenerMethod(Method m) {
        int modifiers = m.getModifiers();
        if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
            return false;
        }

        if (!Void.TYPE.equals(m.getReturnType())) {
            return false;
        }

        Class<?>[] params = m.getParameterTypes();
        if (params.length != 1 || !Object.class.equals(params[0])) {
            return false;
        }

        return true;
    }

    protected boolean processAnnotations(Method method, JpaEntityListener listener) {
        boolean hasListenerAnnotations = false;

        if (method.isAnnotationPresent(PrePersist.class)) {
            listener.setPrePersist(new JpaLifecycleCallback(method.getName()));
            hasListenerAnnotations = true;
        }

        if (method.isAnnotationPresent(PostPersist.class)) {
            listener.setPostPersist(new JpaLifecycleCallback(method.getName()));
            hasListenerAnnotations = true;
        }

        if (method.isAnnotationPresent(PreRemove.class)) {
            listener.setPreRemove(new JpaLifecycleCallback(method.getName()));
            hasListenerAnnotations = true;
        }

        if (method.isAnnotationPresent(PostRemove.class)) {
            listener.setPostRemove(new JpaLifecycleCallback(method.getName()));
            hasListenerAnnotations = true;
        }

        if (method.isAnnotationPresent(PreUpdate.class)) {
            listener.setPreUpdate(new JpaLifecycleCallback(method.getName()));
            hasListenerAnnotations = true;
        }

        if (method.isAnnotationPresent(PostUpdate.class)) {
            listener.setPostUpdate(new JpaLifecycleCallback(method.getName()));
            hasListenerAnnotations = true;
        }

        if (method.isAnnotationPresent(PostLoad.class)) {
            listener.setPostLoad(new JpaLifecycleCallback(method.getName()));
            hasListenerAnnotations = true;
        }

        return hasListenerAnnotations;
    }
}
