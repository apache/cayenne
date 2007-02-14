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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

/**
 * A runtime callback processor for a single kind of lifecycle events.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class LifecycleCallbackEventHandler {

    private EntityResolver resolver;
    private Map listeners;
    private Collection defaultListeners;

    LifecycleCallbackEventHandler(EntityResolver resolver) {
        this.resolver = resolver;
        this.listeners = new HashMap();
        this.defaultListeners = new ArrayList();
    }

    private boolean excludingDefaultListeners(Class objectClass) {
        ObjEntity entity = resolver.lookupObjEntity(objectClass);
        return entity != null && entity.isExcludingDefaultListeners();
    }

    private boolean excludingSuperclassListeners(Class objectClass) {
        ObjEntity entity = resolver.lookupObjEntity(objectClass);
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
    void addListener(Class entityClass, String methodName) {
        addCallback(entityClass, new CallbackOnEntity(entityClass, methodName));
    }

    /**
     * Registers callback method to be invoked on a provided non-entity object when a
     * lifecycle event occurs.
     */
    void addListener(Class entityClass, Object listener, String methodName) {
        CallbackOnListener callback = new CallbackOnListener(
                listener,
                methodName,
                entityClass);
        addCallback(entityClass, callback);
    }

    /**
     * Registers a callback object to be invoked when a lifecycle event occurs.
     */
    private void addCallback(Class entityClass, AbstractCallback callback) {
        Collection entityListeners = (Collection) listeners.get(entityClass.getName());

        if (entityListeners == null) {
            entityListeners = new ArrayList(3);
            listeners.put(entityClass.getName(), entityListeners);
        }

        entityListeners.add(callback);
    }

    /**
     * Invokes callbacks for a given entity object.
     */
    void performCallbacks(Object object) {

        // default listeners are invoked first
        if (!defaultListeners.isEmpty() && !excludingDefaultListeners(object.getClass())) {
            Iterator it = (Iterator) defaultListeners.iterator();
            while (it.hasNext()) {
                ((AbstractCallback) it.next()).performCallback(object);
            }
        }

        // apply per-entity listeners
        performCallbacks(object, object.getClass());
    }

    /**
     * Invokes callbacks for a collection of entity objects.
     */
    void performCallbacks(Collection objects) {
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            Object object = it.next();
            performCallbacks(object);
        }
    }

    /**
     * Invokes callbacks for the class hierarchy, starting from the most generic
     * superclass.
     */
    private void performCallbacks(Object object, Class callbackEntityClass) {
        if (Object.class.equals(callbackEntityClass) || callbackEntityClass == null) {
            return;
        }

        // recursively perform super callbacks first
        if (!excludingSuperclassListeners(callbackEntityClass)) {
            performCallbacks(object, callbackEntityClass.getSuperclass());
        }

        // perform callbacks on provided class
        String key = callbackEntityClass.getName();
        Collection entityListeners = (Collection) listeners.get(key);

        if (entityListeners != null) {
            Iterator it = (Iterator) entityListeners.iterator();
            while (it.hasNext()) {
                ((AbstractCallback) it.next()).performCallback(object);
            }
        }
    }

}
