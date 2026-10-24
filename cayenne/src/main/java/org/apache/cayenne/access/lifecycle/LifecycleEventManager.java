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

/**
 * Listeners of a single lifecycle event type, accessed via the per-event methods of {@link LifecycleEventsManager},
 * e.g. {@code lifecycleEventsManager.postAdd()}.
 *
 * @since 6.0
 */
public class LifecycleEventManager {

    private final Map<Class<?>, Collection<Consumer<? super Persistent>>> listeners;
    private final Collection<Consumer<? super Persistent>> defaultListeners;

    LifecycleEventManager() {
        this.listeners = new HashMap<>();
        this.defaultListeners = new ArrayList<>();
    }

    public boolean hasListeners() {
        return !listeners.isEmpty() || !defaultListeners.isEmpty();
    }

    /**
     * Registers a listener to be invoked when a lifecycle event occurs on the given entities and their subclasses, or
     * on any entity if no entity classes are given.
     */
    @SafeVarargs
    public final <T extends Persistent> void addListener(
            Consumer<? super T> listener,
            Class<? extends T>... entityClasses) {
        @SuppressWarnings("unchecked")
        Consumer<? super Persistent> callback = (Consumer<? super Persistent>) listener;

        if (entityClasses.length == 0) {
            defaultListeners.add(callback);
        } else {
            for (Class<? extends T> entityClass : entityClasses) {
                listeners.computeIfAbsent(entityClass, k -> new ArrayList<>(3)).add(callback);
            }
        }
    }

    /**
     * Dispatches the event for a collection of entity objects to their listeners.
     */
    public void onEvent(Collection<? extends Persistent> objects) {
        for (Persistent object : objects) {
            // a null can happen if an object resolved to null from some query with outer join (e.g. in SQLTemplate)
            if (object != null) {
                onEvent(object);
            }
        }
    }

    private void onEvent(Persistent object) {

        // default listeners are invoked first
        if (!defaultListeners.isEmpty()) {
            for (Consumer<? super Persistent> listener : defaultListeners) {
                listener.accept(object);
            }
        }

        // apply per-entity listeners
        onEvent(object, object.getClass());
    }

    private void onEvent(Persistent object, Class<?> callbackEntityClass) {

        if (callbackEntityClass == null || Object.class.equals(callbackEntityClass)) {
            return;
        }

        // recursively perform super callbacks first
        onEvent(object, callbackEntityClass.getSuperclass());

        Collection<Consumer<? super Persistent>> entityListeners = listeners.get(callbackEntityClass);
        if (entityListeners != null) {
            for (Consumer<? super Persistent> listener : entityListeners) {
                listener.accept(object);
            }
        }
    }
}
