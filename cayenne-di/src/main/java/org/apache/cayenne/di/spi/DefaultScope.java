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
package org.apache.cayenne.di.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import org.apache.cayenne.di.BeforeScopeEnd;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.di.Scope;

/**
 * An implementation of a DI scopes with support scope events.
 * 
 * @since 3.1
 */
public class DefaultScope implements Scope {

    protected Collection<Class<? extends Annotation>> eventTypes;
    protected ConcurrentMap<String, Collection<ScopeEventBinding>> listeners;

    private static final String SPECIAL_EVENT = AfterScopeEnd.class.getName();

    public DefaultScope(Class<? extends Annotation>... customEventTypes) {
        this.listeners = new ConcurrentHashMap<String, Collection<ScopeEventBinding>>();
        this.eventTypes = new HashSet<Class<? extends Annotation>>();

        // initialize the event listener data structures in constructor to avoid
        // synchronization concerns on everything but per-event lists.

        // standard event types
        eventTypes.add(BeforeScopeEnd.class);
        eventTypes.add(AfterScopeEnd.class);

        // custom event types
        if (customEventTypes != null) {
            for (Class<? extends Annotation> type : customEventTypes) {
                eventTypes.add(type);
            }
        }

        for (Class<? extends Annotation> type : eventTypes) {
            listeners.put(type.getName(), new ConcurrentLinkedQueue<ScopeEventBinding>());
        }
    }

    /**
     * Shuts down this scope, posting {@link BeforeScopeEnd} and {@link AfterScopeEnd}
     * events.
     */
    public void shutdown() {
        postScopeEvent(BeforeScopeEnd.class);

        // this will notify providers that they should reset their state and unregister
        // object event listeners that just went out of scope
        postScopeEvent(AfterScopeEnd.class);
    }

    /**
     * Registers annotated methods of an arbitrary object for this scope lifecycle events.
     */
    public void addScopeEventListener(Object object) {

        // TODO: cache metadata for non-singletons scopes for performance

        // 'getMethods' grabs public method from the class and its superclasses...
        for (Method method : object.getClass().getMethods()) {

            for (Class<? extends Annotation> annotationType : eventTypes) {

                if (method.isAnnotationPresent(annotationType)) {
                    String typeName = annotationType.getName();

                    Collection<ScopeEventBinding> eventListeners = listeners
                            .get(typeName);
                    eventListeners.add(new ScopeEventBinding(object, method));
                }
            }
        }
    }

    public void removeScopeEventListener(Object object) {

        // TODO: 2 level-deep full scan will not be very efficient for short scopes. Right
        // now this would only affect the unit test scope, but if we start creating the
        // likes of HTTP request scope, we may need to create a faster listener
        // removal algorithm.

        for (Entry<String, Collection<ScopeEventBinding>> entry : listeners.entrySet()) {

            if (SPECIAL_EVENT.equals(entry.getKey())) {
                // no scanning and removal of Scope providers ...
                // for faster scan skip those
                continue;
            }

            Iterator<ScopeEventBinding> it = entry.getValue().iterator();
            while (it.hasNext()) {
                ScopeEventBinding binding = it.next();
                if (binding.getObject() == object) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Posts a scope event to all registered listeners. There's no predetermined order of
     * event dispatching. An exception thrown by any of the listeners stops further event
     * processing and is rethrown.
     */
    public void postScopeEvent(
            Class<? extends Annotation> type,
            Object... eventParameters) {

        Collection<ScopeEventBinding> eventListeners = listeners.get(type.getName());

        if (eventListeners != null) {
            Iterator<ScopeEventBinding> it = eventListeners.iterator();
            while (it.hasNext()) {
                ScopeEventBinding listener = it.next();
                if (!listener.onScopeEvent(eventParameters)) {
                    // remove listeners that were garbage collected
                    it.remove();
                }
            }
        }
    }

    public <T> Provider<T> scope(Provider<T> unscoped) {
        return new DefaultScopeProvider<T>(this, unscoped);
    }
}
