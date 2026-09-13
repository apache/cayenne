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
package org.apache.cayenne.event;

import java.util.EventObject;

/**
 * Acts as bridge between an Object that wants to inform others about its
 * current state or a change thereof (Publisher) and a list of objects interested in the
 * Subject (Listeners).
 * <p>
 * Listeners are held via weak references, so a listener that is no longer reachable from anywhere else is
 * released together with its registration. This only works when the {@link EventHandler} is an unbound method
 * reference (e.g. {@code MyListener::onEvent}) that does not capture the listener itself.
 *
 * @since 3.1 before 3.1 this was a concrete class.
 */
public interface EventManager {

    /**
     * Returns true if this EventManager is single-threaded. If so it will throw an
     * exception on any attempt to register an unblocking listener or dispatch a
     * non-blocking event.
     * 
     * @since 1.2
     */
    boolean isSingleThreaded();

    /**
     * Registers a listener for events sent by any sender.
     *
     * @param listener the object to be notified about events
     * @param eventClass the class of events the listener is interested in; events of other types are ignored
     * @param handler the callback invoked with the listener and the event, normally an unbound method reference
     * @param subject the event subject that the listener is interested in
     * @since 5.0
     */
    <L, E extends EventObject> void addListener(
            L listener,
            Class<E> eventClass,
            EventHandler<? super L, ? super E> handler,
            EventSubject subject);

    /**
     * Registers a listener for events sent by any sender, to be notified on a dispatch thread instead of the thread
     * that posted the event.
     *
     * @since 5.0
     * @see #addListener(Object, Class, EventHandler, EventSubject)
     */
    <L, E extends EventObject> void addNonBlockingListener(
            L listener,
            Class<E> eventClass,
            EventHandler<? super L, ? super E> handler,
            EventSubject subject);

    /**
     * Registers a listener for events sent by a specific sender.
     *
     * @param listener the object to be notified about events
     * @param eventClass the class of events the listener is interested in; events of other types are ignored
     * @param handler the callback invoked with the listener and the event, normally an unbound method reference
     * @param subject the event subject that the listener is interested in
     * @param sender the object whose events the listener is interested in; null means 'any sender'
     * @since 5.0
     */
    <L, E extends EventObject> void addListener(
            L listener,
            Class<E> eventClass,
            EventHandler<? super L, ? super E> handler,
            EventSubject subject,
            Object sender);

    /**
     * Registers a listener for events sent by a specific sender, to be notified on a dispatch thread instead of the
     * thread that posted the event.
     *
     * @since 5.0
     */
    <L, E extends EventObject> void addNonBlockingListener(
            L listener,
            Class<E> eventClass,
            EventHandler<? super L, ? super E> handler,
            EventSubject subject,
            Object sender);

    /**
     * Unregister the specified listener from all event subjects handled by this manager
     * instance.
     * 
     * @param listener the object to be unregistered
     * @return <code>true</code> if <code>listener</code> could be removed for any
     *         existing subjects, else returns <code>false</code>.
     */
    boolean removeListener(Object listener);

    /**
     * Removes all listeners for a given subject.
     */
    boolean removeAllListeners(EventSubject subject);

    /**
     * Unregister the specified listener for the events about the given subject.
     * 
     * @param listener the object to be unregistered
     * @param subject the subject from which the listener is to be unregistered
     * @return <code>true</code> if <code>listener</code> could be removed for the given
     *         subject, else returns <code>false</code>.
     */
    boolean removeListener(Object listener, EventSubject subject);

    /**
     * Unregister the specified listener for the events about the given subject and the
     * given sender.
     * 
     * @param listener the object to be unregistered
     * @param subject the subject from which the listener is to be unregistered
     * @param sender the object whose events the listener was interested in;
     *            <code>null</code> means 'any sender'.
     * @return <code>true</code> if <code>listener</code> could be removed for the given
     *         subject, else returns <code>false</code>.
     */
    boolean removeListener(Object listener, EventSubject subject, Object sender);

    /**
     * Sends an event to all registered objects about a particular subject. Event is sent
     * synchronously, so the sender thread is blocked until all the listeners finish
     * processing the event.
     * 
     * @throws IllegalArgumentException if event or subject are null
     */
    void postEvent(EventObject event, EventSubject subject);

    /**
     * Sends an event to all registered objects about a particular subject. Event is
     * queued by EventManager, releasing the sender thread, and is later dispatched in a
     * separate thread.
     * 
     * @throws IllegalArgumentException if event or subject are null
     * @since 1.1
     */
    void postNonBlockingEvent(EventObject event, EventSubject subject);
}
