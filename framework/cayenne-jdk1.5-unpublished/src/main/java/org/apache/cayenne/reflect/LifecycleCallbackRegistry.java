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

import java.util.Collection;

import org.apache.cayenne.LifecycleListener;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.LifecycleEvent;

/**
 * A registry of lifecycle callbacks for all callback event types. Valid event types are
 * defined in {@link LifecycleEvent} enum.
 * 
 * @since 3.0
 */
public class LifecycleCallbackRegistry {

    private LifecycleCallbackEventHandler[] eventCallbacks;

    /**
     * Creates an empty callback registry.
     */
    public LifecycleCallbackRegistry(EntityResolver resolver) {
        eventCallbacks = new LifecycleCallbackEventHandler[LifecycleEvent.values().length];
        for (int i = 0; i < eventCallbacks.length; i++) {
            eventCallbacks[i] = new LifecycleCallbackEventHandler(resolver);
        }
    }

    /**
     * Removes all listeners for all event types.
     */
    public void clear() {
        for (LifecycleCallbackEventHandler eventCallback : eventCallbacks) {
            eventCallback.clear();
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
    public boolean isEmpty(LifecycleEvent type) {
        return eventCallbacks[type.ordinal()].isEmpty();
    }

    /**
     * Registers a {@link LifecycleListener} for all events on all entities. Note that
     * listeners are not required to implement {@link LifecycleListener} interface. Other
     * methods in this class can be used to register arbitrary listeners.
     */
    public void addDefaultListener(LifecycleListener listener) {
        addDefaultListener(LifecycleEvent.POST_ADD, listener, "postAdd");
        addDefaultListener(LifecycleEvent.PRE_PERSIST, listener, "prePersist");
        addDefaultListener(LifecycleEvent.POST_PERSIST, listener, "postPersist");
        addDefaultListener(LifecycleEvent.PRE_REMOVE, listener, "preRemove");
        addDefaultListener(LifecycleEvent.POST_REMOVE, listener, "postRemove");
        addDefaultListener(LifecycleEvent.PRE_UPDATE, listener, "preUpdate");
        addDefaultListener(LifecycleEvent.POST_UPDATE, listener, "postUpdate");
        addDefaultListener(LifecycleEvent.POST_LOAD, listener, "postLoad");
    }

    /**
     * Registers a callback method to be invoked on a provided non-entity object when a
     * lifecycle event occurs on any entity that does not suppress default callbacks.
     */
    public void addDefaultListener(LifecycleEvent type, Object listener, String methodName) {
        eventCallbacks[type.ordinal()].addDefaultListener(listener, methodName);
    }

    /**
     * Registers a {@link LifecycleListener} for all events on all entities. Note that
     * listeners are not required to implement {@link LifecycleListener} interface. Other
     * methods in this class can be used to register arbitrary listeners.
     */
    public void addListener(Class<?> entityClass, LifecycleListener listener) {
        addListener(LifecycleEvent.POST_ADD, entityClass, listener, "postAdd");
        addListener(LifecycleEvent.PRE_PERSIST, entityClass, listener, "prePersist");
        addListener(LifecycleEvent.POST_PERSIST, entityClass, listener, "postPersist");
        addListener(LifecycleEvent.PRE_REMOVE, entityClass, listener, "preRemove");
        addListener(LifecycleEvent.POST_REMOVE, entityClass, listener, "postRemove");
        addListener(LifecycleEvent.PRE_UPDATE, entityClass, listener, "preUpdate");
        addListener(LifecycleEvent.POST_UPDATE, entityClass, listener, "postUpdate");
        addListener(LifecycleEvent.POST_LOAD, entityClass, listener, "postLoad");
    }

    /**
     * Registers callback method to be invoked on a provided non-entity object when a
     * lifecycle event occurs for a specific entity.
     */
    public void addListener(
            LifecycleEvent type,
            Class<?> entityClass,
            Object listener,
            String methodName) {
        eventCallbacks[type.ordinal()].addListener(entityClass, listener, methodName);
    }

    /**
     * Registers a callback method to be invoked on an entity class instances when a
     * lifecycle event occurs.
     */
    public void addListener(LifecycleEvent type, Class<?> entityClass, String methodName) {
        eventCallbacks[type.ordinal()].addListener(entityClass, methodName);
    }

    /**
     * Invokes callbacks of a specific type for a given entity object.
     */
    public void performCallbacks(LifecycleEvent type, Persistent object) {
        eventCallbacks[type.ordinal()].performCallbacks(object);
    }

    /**
     * Invokes callbacks of a specific type for a collection of entity objects.
     */
    public void performCallbacks(LifecycleEvent type, Collection<?> objects) {
        eventCallbacks[type.ordinal()].performCallbacks(objects);
    }
}
