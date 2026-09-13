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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.cayenne.event.DefaultEventManager.Dispatch;

/**
 * Stores a set of ListenerRegistration objects, organizing them by sender. Listeners have an option
 * to receive events for a particular sender or to receive all events. EventManager
 * creates one DispatchQueue per EventSubject. DispatchQueue is thread-safe - all methods
 * that read/modify internal collections are synchronized.
 * 
 * @since 1.1
 */
class DispatchQueue {

    private final ConcurrentMap<ListenerRegistration<?, ?>, Object> subjectRegistrations;
    private final Map<Object, ConcurrentMap<ListenerRegistration<?, ?>, Object>> registrationsBySender;

    DispatchQueue() {
        subjectRegistrations = new ConcurrentHashMap<>();

        // TODO: need something like com.google.common.collect.MapMaker to avoid synchronization on registrationsBySender
        registrationsBySender = new WeakHashMap<>();
    }

    /**
     * Dispatches event to all listeners in the queue that are registered for this event
     * and sender.
     */
    void dispatchEvent(Dispatch dispatch) {
        // dispatch to "any sender" listeners
        dispatchEvent(subjectRegistrations.keySet(), dispatch);

        // dispatch to the given sender listeners
        Object sender = dispatch.getSender();
        Map<ListenerRegistration<?, ?>, Object> senderRegistrations = registrationsForSender(sender, false);
        if (senderRegistrations != null) {
            dispatchEvent(senderRegistrations.keySet(), dispatch);
        }
    }

    void addRegistration(ListenerRegistration<?, ?> registration, Object sender) {
        ConcurrentMap<ListenerRegistration<?, ?>, Object> registrations;

        if (sender == null) {
            registrations = subjectRegistrations;
        } else {
            registrations = registrationsForSender(sender, true);
        }

        // perform maintenance of the given registrations set, as failure to do that can
        // result in a memory leak per CAY-770. This seemed to happen when lots of
        // listeners got registered, but no events were dispatched (hence the stale
        // registration removal during dispatch did not happen)
        registrations.keySet().removeIf(r -> r.getListener() == null);
        registrations.putIfAbsent(registration, Boolean.TRUE);
    }

    boolean removeRegistrations(Object listener, Object sender) {

        // remove only for specific sender
        if (sender != null) {
            return removeRegistrations(registrationsForSender(sender, false), listener);
        }

        // remove listener from all collections
        boolean didRemove = removeRegistrations(subjectRegistrations, listener);

        synchronized (registrationsBySender) {
            for (ConcurrentMap<ListenerRegistration<?, ?>, Object> senderRegistrations : registrationsBySender.values()) {
                didRemove = removeRegistrations(senderRegistrations, listener) || didRemove;
            }
        }

        return didRemove;
    }

    private ConcurrentMap<ListenerRegistration<?, ?>, Object> registrationsForSender(Object sender, boolean create) {

        synchronized (registrationsBySender) {
            ConcurrentMap<ListenerRegistration<?, ?>, Object> senderRegistrations = registrationsBySender.get(sender);
            if (create && senderRegistrations == null) {
                senderRegistrations = new ConcurrentHashMap<>();
                registrationsBySender.put(sender, senderRegistrations);
            }

            return senderRegistrations;
        }
    }

    // removes all registrations for a given listener
    private boolean removeRegistrations(
            ConcurrentMap<ListenerRegistration<?, ?>, Object> registrations,
            Object listener) {
        if (registrations == null || registrations.isEmpty()) {
            return false;
        }

        boolean didRemove = false;

        Iterator<ListenerRegistration<?, ?>> registrationsIt = registrations.keySet().iterator();
        while (registrationsIt.hasNext()) {
            ListenerRegistration<?, ?> registration = registrationsIt.next();
            if (registration.getListener() == listener) {
                registrationsIt.remove();
                didRemove = true;
            }
        }

        return didRemove;
    }

    // dispatches event to a list of listeners
    private void dispatchEvent(Collection<ListenerRegistration<?, ?>> registrations, Dispatch dispatch) {
        // fire registration, clean up GC'd registrations...
        registrations.removeIf(registration -> !dispatch.fire(registration));
    }
}
