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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

/**
 * A runtime callback processor for a single kind of lifecycle events.
 * 
 * @since 3.0
 */
class LifecycleCallbackEventHandler {

    private EntityResolver resolver;
    private Map<String, Collection<AbstractCallback>> listeners;
    private Collection<AbstractCallback> defaultListeners;

    LifecycleCallbackEventHandler(EntityResolver resolver) {
        this.resolver = resolver;
        this.listeners = new HashMap<String, Collection<AbstractCallback>>();
        this.defaultListeners = new ArrayList<AbstractCallback>();
    }

    private boolean excludingDefaultListeners(String entityName) {
        ObjEntity entity = resolver.getObjEntity(entityName);
        return entity != null && entity.isExcludingDefaultListeners();
    }

    private boolean excludingSuperclassListeners(String entityName) {
        ObjEntity entity = resolver.getObjEntity(entityName);
        return entity != null && entity.isExcludingSuperclassListeners();
    }

    boolean isEmpty() {
        return listeners.isEmpty() && defaultListeners.isEmpty();
    }

    /**
     * Removes all listeners.
     */
    void clear() {
        listeners.clear();
        defaultListeners.clear();
    }
    
    int defaultListenersSize() {
        return defaultListeners.size();
    }
    
    int listenersSize() {
        return listeners.size();
    }

    /**
     * Registers a callback method to be invoked on a provided non-entity object when a
     * lifecycle event occurs on any entity that does not suppress default callbacks.
     */
    void addDefaultListener(Object listener, String methodName) {
        CallbackOnListener callback = new CallbackOnListener(listener, methodName);
        addDefaultCallback(callback);
    }

    /**
     * Registers a callback object to be invoked when a lifecycle event occurs.
     */
    private void addDefaultCallback(AbstractCallback callback) {
        defaultListeners.add(callback);
    }

    /**
     * Registers a callback method to be invoked on an entity class instances when a
     * lifecycle event occurs.
     */
    void addListener(Class<?> entityClass, String methodName) {
        addCallback(entityClass, new CallbackOnEntity(entityClass, methodName));
    }

    /**
     * Registers callback method to be invoked on a provided non-entity object when a
     * lifecycle event occurs.
     */
    void addListener(Class<?> entityClass, Object listener, String methodName) {
        CallbackOnListener callback = new CallbackOnListener(
                listener,
                methodName,
                entityClass);
        addCallback(entityClass, callback);
    }

    void addListener(Class<?> entityClass, Object listener, Method method) {
        CallbackOnListener callback = new CallbackOnListener(
                listener,
                method,
                entityClass);
        addCallback(entityClass, callback);
    }

    /**
     * Registers a callback object to be invoked when a lifecycle event occurs.
     */
    private void addCallback(Class<?> entityClass, AbstractCallback callback) {
        Collection<AbstractCallback> entityListeners = listeners.get(entityClass
                .getName());

        if (entityListeners == null) {
            entityListeners = new ArrayList<AbstractCallback>(3);
            listeners.put(entityClass.getName(), entityListeners);
        }

        entityListeners.add(callback);
    }

    /**
     * Invokes callbacks for a given entity object.
     */
    void performCallbacks(Persistent object) {

        // default listeners are invoked first
        if (!defaultListeners.isEmpty()
                && !excludingDefaultListeners(object.getObjectId().getEntityName())) {
            for (final AbstractCallback listener : defaultListeners) {
                listener.performCallback(object);
            }
        }

        // apply per-entity listeners
        performCallbacks(object, object.getClass());
    }

    /**
     * Invokes callbacks for a collection of entity objects.
     */
    void performCallbacks(Collection<?> objects) {
        for (Object object : objects) {
            performCallbacks((Persistent) object);
        }
    }

    /**
     * Invokes callbacks for the class hierarchy, starting from the most generic
     * superclass.
     */
    private void performCallbacks(Persistent object, Class<?> callbackEntityClass) {

        if (callbackEntityClass == null || Object.class.equals(callbackEntityClass)) {
            return;
        }

        // recursively perform super callbacks first
        if (!excludingSuperclassListeners(object.getObjectId().getEntityName())) {
            performCallbacks(object, callbackEntityClass.getSuperclass());
        }

        // perform callbacks on provided class
        String key = callbackEntityClass.getName();
        Collection<AbstractCallback> entityListeners = listeners.get(key);

        if (entityListeners != null) {
            for (final AbstractCallback listener : entityListeners) {
                listener.performCallback(object);
            }
        }
    }

}
