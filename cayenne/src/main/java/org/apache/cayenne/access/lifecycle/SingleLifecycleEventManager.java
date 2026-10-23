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
package org.apache.cayenne.access.lifecycle;

import org.apache.cayenne.Persistent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

class SingleLifecycleEventManager {

    private final Map<String, Collection<Consumer<? super Persistent>>> listeners;
    private final Collection<Consumer<? super Persistent>> defaultListeners;

    SingleLifecycleEventManager() {
        this.listeners = new HashMap<>();
        this.defaultListeners = new ArrayList<>();
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
     * Registers a listener to be invoked when a lifecycle event occurs on any entity.
     */
    void addDefaultListener(Consumer<? super Persistent> listener) {
        defaultListeners.add(listener);
    }

    /**
     * Registers a listener to be invoked when a lifecycle event occurs on a given entity or its subclasses.
     */
    <T extends Persistent> void addListener(Class<T> entityClass, Consumer<? super T> listener) {
        // only instances of "entityClass" are dispatched to this callback, so the cast is safe
        @SuppressWarnings("unchecked")
        Consumer<? super Persistent> callback = (Consumer<? super Persistent>) listener;
        listeners.computeIfAbsent(entityClass.getName(), k -> new ArrayList<>(3)).add(callback);
    }

    /**
     * Invokes callbacks for a given entity object.
     */
    void performCallbacks(Persistent object) {
        if (object == null) {
            // this can happen if object resolved to null from some query with outer join (e.g. in SQLTemplate)
            return;
        }

        // default listeners are invoked first
        if (!defaultListeners.isEmpty()) {
            for (Consumer<? super Persistent> listener : defaultListeners) {
                listener.accept(object);
            }
        }

        // apply per-entity listeners
        performCallbacks(object, object.getClass());
    }

    /**
     * Invokes callbacks for a collection of entity objects.
     */
    void performCallbacks(Collection<? extends Persistent> objects) {
        for (Persistent object : objects) {
            performCallbacks(object);
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
        performCallbacks(object, callbackEntityClass.getSuperclass());

        // perform callbacks on provided class
        String key = callbackEntityClass.getName();
        Collection<Consumer<? super Persistent>> entityListeners = listeners.get(key);

        if (entityListeners != null) {
            for (Consumer<? super Persistent> listener : entityListeners) {
                listener.accept(object);
            }
        }
    }

}
