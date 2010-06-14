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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.cayenne.di.Provider;
import org.apache.cayenne.di.Scope;

/**
 * A base superclass of DI scopes that send scope events.
 * 
 * @since 3.1
 */
public abstract class EventfulScope implements Scope {

    protected ConcurrentMap<String, Collection<ScopeEventBinding>> listeners;
    protected Collection<Class<? extends Annotation>> eventAnnotations;

    public EventfulScope() {
        this.listeners = new ConcurrentHashMap<String, Collection<ScopeEventBinding>>();
        this.eventAnnotations = new HashSet<Class<? extends Annotation>>();
    }

    /**
     * Adds an annotation type that marks handler methods for a specific event type.
     * Subclasses of EventfulScope should use this method to configure supported
     * annotation types. This method should be invoked before any calls to
     * {@link #addScopeEventListener(Object)}. Often it is invoked form the scope
     * constructor.
     */
    protected void addEventAnnotation(Class<? extends Annotation> annotationType) {
        eventAnnotations.add(annotationType);
    }

    /**
     * Registers annotated methods of an arbitrary object for this scope lifecycle events.
     */
    public void addScopeEventListener(Object object) {

        // not caching metadata for now, as all services in SingletonScope are unique...
        // If we start using RequestScope or similar, may need to figure out per-class
        // metadata cache.

        // 'getMethods' grabs public method from the class and its superclasses...
        for (Method method : object.getClass().getMethods()) {

            for (Class<? extends Annotation> annotationType : eventAnnotations) {

                if (method.isAnnotationPresent(annotationType)) {
                    String typeName = annotationType.getName();

                    Collection<ScopeEventBinding> newListeners = new CopyOnWriteArrayList<ScopeEventBinding>();
                    Collection<ScopeEventBinding> eventListeners = listeners.putIfAbsent(
                            typeName,
                            newListeners);
                    if (eventListeners == null) {
                        eventListeners = newListeners;
                    }

                    eventListeners.add(new ScopeEventBinding(object, method));
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
            for (ScopeEventBinding listener : eventListeners) {
                listener.onScopeEvent(eventParameters);
            }
        }
    }

    public abstract <T> Provider<T> scope(Provider<T> unscoped);
}
