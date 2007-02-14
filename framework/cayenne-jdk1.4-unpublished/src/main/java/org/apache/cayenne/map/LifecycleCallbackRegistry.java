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
package org.apache.cayenne.map;

import java.util.Collection;

import org.apache.cayenne.LifecycleListener;

/**
 * A registry of lifecycle callbacks for all callback event types.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class LifecycleCallbackRegistry {

    protected LifecycleCallbackEventHandler[] eventCallbacks;

    /**
     * Creates an empty callback registry.
     */
    public LifecycleCallbackRegistry(EntityResolver resolver) {
        eventCallbacks = new LifecycleCallbackEventHandler[CallbackMap.CALLBACKS.length];
        for (int i = 0; i < eventCallbacks.length; i++) {
            eventCallbacks[i] = new LifecycleCallbackEventHandler(resolver);
        }
    }

    /**
     * Removes all listeners for all event types.
     */
    public void clear() {
        for (int i = 0; i < eventCallbacks.length; i++) {
            eventCallbacks[i].clear();
        }
    }

    /**
     * Removes listeners for a single event type.
     */
    public void clear(int type) {
        eventCallbacks[type].clear();
    }

    /**
     * Returns true if there are no listeners for a specific event type.
     */
    public boolean isEmpty(int type) {
        return eventCallbacks[type].isEmpty();
    }

    /**
     * Registers a {@link LifecycleListener} for all events on all entities. Note that
     * listeners are not required to implement {@link LifecycleListener} interface. Other
     * methods in this class can be used to register arbitrary listeners.
     */
    public void addDefaultListener(LifecycleListener listener) {
        addDefaultListener(CallbackMap.PRE_PERSIST, listener, "prePersist");
        addDefaultListener(CallbackMap.POST_PERSIST, listener, "postPersist");
        addDefaultListener(CallbackMap.PRE_REMOVE, listener, "preRemove");
        addDefaultListener(CallbackMap.POST_REMOVE, listener, "postRemove");
        addDefaultListener(CallbackMap.PRE_UPDATE, listener, "preUpdate");
        addDefaultListener(CallbackMap.POST_UPDATE, listener, "postUpdate");
        addDefaultListener(CallbackMap.POST_LOAD, listener, "postLoad");
    }

    /**
     * Registers a callback method to be invoked on a provided non-entity object when a
     * lifecycle event occurs on any entity that does not suppress default callbacks.
     */
    public void addDefaultListener(int type, Object listener, String methodName) {
        eventCallbacks[type].addDefaultListener(listener, methodName);
    }

    /**
     * Registers a {@link LifecycleListener} for all events on all entities. Note that
     * listeners are not required to implement {@link LifecycleListener} interface. Other
     * methods in this class can be used to register arbitrary listeners.
     */
    public void addListener(Class entityClass, LifecycleListener listener) {
        addListener(CallbackMap.PRE_PERSIST, entityClass, listener, "prePersist");
        addListener(CallbackMap.POST_PERSIST, entityClass, listener, "postPersist");
        addListener(CallbackMap.PRE_REMOVE, entityClass, listener, "preRemove");
        addListener(CallbackMap.POST_REMOVE, entityClass, listener, "postRemove");
        addListener(CallbackMap.PRE_UPDATE, entityClass, listener, "preUpdate");
        addListener(CallbackMap.POST_UPDATE, entityClass, listener, "postUpdate");
        addListener(CallbackMap.POST_LOAD, entityClass, listener, "postLoad");
    }

    /**
     * Registers callback method to be invoked on a provided non-entity object when a
     * lifecycle event occurs for a specific entity.
     */
    public void addListener(
            int type,
            Class entityClass,
            Object listener,
            String methodName) {
        eventCallbacks[type].addListener(entityClass, listener, methodName);
    }

    /**
     * Registers a callback method to be invoked on an entity class instances when a
     * lifecycle event occurs.
     */
    public void addListener(int type, Class entityClass, String methodName) {
        eventCallbacks[type].addListener(entityClass, methodName);
    }

    /**
     * Invokes callbacks of a specific type for a given entity object.
     */
    public void performCallbacks(int type, Object object) {
        eventCallbacks[type].performCallbacks(object);
    }

    /**
     * Invokes callbacks of a specific type for a collection of entity objects.
     */
    public void performCallbacks(int type, Collection objects) {
        eventCallbacks[type].performCallbacks(objects);
    }
}
