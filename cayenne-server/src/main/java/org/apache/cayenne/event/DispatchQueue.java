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
import org.apache.cayenne.util.Invocation;

/**
 * Stores a set of Invocation objects, organizing them by sender. Listeners have an option
 * to receive events for a particular sender or to receive all events. EventManager
 * creates one DispatchQueue per EventSubject. DispatchQueue is thread-safe - all methods
 * that read/modify internal collections are synchronized.
 * 
 * @since 1.1
 */
class DispatchQueue {

    private final ConcurrentMap<Invocation, Object> subjectInvocations;
    private final Map<Object, ConcurrentMap<Invocation, Object>> invocationsBySender;

    DispatchQueue() {
        subjectInvocations = new ConcurrentHashMap<>();

        // TODO: need something like com.google.common.collect.MapMaker to avoid synchronization on invocationsBySender
        invocationsBySender = new WeakHashMap<>();
    }

    /**
     * Dispatches event to all listeners in the queue that are registered for this event
     * and sender.
     */
    void dispatchEvent(Dispatch dispatch) {
        // dispatch to "any sender" listeners
        dispatchEvent(subjectInvocations.keySet(), dispatch);

        // dispatch to the given sender listeners
        Object sender = dispatch.getSender();
        Map<Invocation, Object> senderInvocations = invocationsForSender(sender, false);
        if (senderInvocations != null) {
            dispatchEvent(senderInvocations.keySet(), dispatch);
        }
    }

    void addInvocation(Invocation invocation, Object sender) {
        ConcurrentMap<Invocation, Object> invocations;

        if (sender == null) {
            invocations = subjectInvocations;
        } else {
            invocations = invocationsForSender(sender, true);
        }

        // perform maintenance of the given invocations set, as failure to do that can
        // result in a memory leak per CAY-770. This seemed to happen when lots of
        // invocations got registered, but no events were dispatched (hence the stale
        // invocation removal during dispatch did not happen)
        invocations.keySet().removeIf(i -> i.getTarget() == null);
        invocations.putIfAbsent(invocation, Boolean.TRUE);
    }

    boolean removeInvocations(Object listener, Object sender) {

        // remove only for specific sender
        if (sender != null) {
            return removeInvocations(invocationsForSender(sender, false), listener);
        }

        // remove listener from all collections
        boolean didRemove = removeInvocations(subjectInvocations, listener);

        synchronized (invocationsBySender) {
            for (ConcurrentMap<Invocation, Object> senderInvocations : invocationsBySender.values()) {
                didRemove = removeInvocations(senderInvocations, listener) || didRemove;
            }
        }

        return didRemove;
    }

    private ConcurrentMap<Invocation, Object> invocationsForSender(Object sender, boolean create) {

        synchronized (invocationsBySender) {
            ConcurrentMap<Invocation, Object> senderInvocations = invocationsBySender.get(sender);
            if (create && senderInvocations == null) {
                senderInvocations = new ConcurrentHashMap<>();
                invocationsBySender.put(sender, senderInvocations);
            }

            return senderInvocations;
        }
    }

    // removes all invocations for a given listener
    private boolean removeInvocations(
            ConcurrentMap<Invocation, Object> invocations,
            Object listener) {
        if (invocations == null || invocations.isEmpty()) {
            return false;
        }

        boolean didRemove = false;

        Iterator<Invocation> invocationsIt = invocations.keySet().iterator();
        while (invocationsIt.hasNext()) {
            Invocation invocation = invocationsIt.next();
            if (invocation.getTarget() == listener) {
                invocationsIt.remove();
                didRemove = true;
            }
        }

        return didRemove;
    }

    // dispatches event to a list of listeners
    private void dispatchEvent(Collection<Invocation> invocations, Dispatch dispatch) {
        // fire invocation, clean up GC'd invocations...
        invocations.removeIf(invocation -> !dispatch.fire(invocation));
    }
}
