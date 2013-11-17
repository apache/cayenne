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

package org.apache.cayenne.event;

import java.util.EventObject;

/**
 * This class acts as bridge between an Object that wants to inform others about its
 * current state or a change thereof (Publisher) and a list of objects interested in the
 * Subject (Listeners).
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
     * Register an <code>EventListener</code> for events sent by any sender.
     * 
     * @throws RuntimeException if <code>methodName</code> is not found.
     */
    void addListener(
            Object listener,
            String methodName,
            Class<?> eventParameterClass,
            EventSubject subject);

    void addNonBlockingListener(
            Object listener,
            String methodName,
            Class<?> eventParameterClass,
            EventSubject subject);

    /**
     * Register an <code>EventListener</code> for events sent by a specific sender.
     * 
     * @param listener the object to be notified about events
     * @param methodName the name of the listener method to be invoked
     * @param eventParameterClass the class of the single event argument passed to
     *            <code>methodName</code>
     * @param subject the event subject that the listener is interested in
     * @param sender the object whose events the listener is interested in;
     *            <code>null</code> means 'any sender'.
     * @throws RuntimeException if <code>methodName</code> is not found
     */
    void addListener(
            Object listener,
            String methodName,
            Class<?> eventParameterClass,
            EventSubject subject,
            Object sender);

    void addNonBlockingListener(
            Object listener,
            String methodName,
            Class<?> eventParameterClass,
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
     * @param event the event to be posted to the observers
     * @param subject the subject about which observers will be notified
     * @throws IllegalArgumentException if event or subject are null
     */
    void postEvent(EventObject event, EventSubject subject);

    /**
     * Sends an event to all registered objects about a particular subject. Event is
     * queued by EventManager, releasing the sender thread, and is later dispatched in a
     * separate thread.
     * 
     * @param event the event to be posted to the observers
     * @param subject the subject about which observers will be notified
     * @throws IllegalArgumentException if event or subject are null
     * @since 1.1
     */
    void postNonBlockingEvent(EventObject event, EventSubject subject);
}
