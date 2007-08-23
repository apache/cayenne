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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.cayenne.event.EventManager.Dispatch;
import org.apache.cayenne.util.Invocation;

/**
 * Stores a set of Invocation objects, organizing them by sender. Listeners have an option
 * to receive events for a particular sender or to receive all events. EventManager
 * creates one DispatchQueue per EventSubject. DispatchQueue is thread-safe - all methods
 * that read/modify internal collections are synchronized.
 * 
 * @author Andrus Adamchik
 * @since 1.1
 */
class DispatchQueue {

    private Set subjectInvocations = new HashSet();
    private Map invocationsBySender = new WeakHashMap();

    /**
     * Dispatches event to all listeners in the queue that are registered for this event
     * and sender.
     */
    synchronized void dispatchEvent(Dispatch dispatch) {
        // dispatch to "any sender" listeners
        dispatchEvent(subjectInvocations, dispatch);

        // dispatch to the given sender listeners
        Object sender = dispatch.getSender();
        dispatchEvent(invocationsForSender(sender, false), dispatch);
    }

    synchronized void addInvocation(Invocation invocation, Object sender) {
        Collection invocations;

        if (sender == null) {
            invocations = subjectInvocations;
        }
        else {
            invocations = invocationsForSender(sender, true);
        }

        // perform maintenance of the given invocations set, as failure to do taht can
        // result in a memory leak per CAY-770. This seemed to happen when lots of
        // invocations got registered, but no events where dispatched (hence the stale
        // inocation removal during dispatch did not happen)
        Iterator it = invocations.iterator();
        while (it.hasNext()) {
            Invocation i = (Invocation) it.next();
            if (i.getTarget() == null) {
                it.remove();
            }
        }

        invocations.add(invocation);
    }

    synchronized boolean removeInvocations(Object listener, Object sender) {

        // remove only for specific sender
        if (sender != null) {
            return removeInvocations(invocationsForSender(sender, false), listener);
        }

        boolean didRemove = false;

        // remove listener from all collections
        didRemove = removeInvocations(subjectInvocations, listener);

        Iterator sets = invocationsBySender.values().iterator();
        while (sets.hasNext()) {
            Collection senderInvocations = (Collection) sets.next();
            if (senderInvocations == null) {
                continue;
            }

            Iterator it = senderInvocations.iterator();
            while (it.hasNext()) {
                Invocation invocation = (Invocation) it.next();
                if (invocation.getTarget() == listener) {
                    it.remove();
                    didRemove = true;
                }
            }
        }

        return didRemove;
    }

    private Collection invocationsForSender(Object sender, boolean create) {
        Collection senderInvocations = (Collection) invocationsBySender.get(sender);
        if (create && senderInvocations == null) {
            senderInvocations = new HashSet();
            invocationsBySender.put(sender, senderInvocations);
        }

        return senderInvocations;
    }

    // removes all invocations for a given listener
    private boolean removeInvocations(Collection invocations, Object listener) {
        if (invocations == null || invocations.isEmpty()) {
            return false;
        }

        boolean didRemove = false;

        Iterator invocationsIt = invocations.iterator();
        while (invocationsIt.hasNext()) {
            Invocation invocation = (Invocation) invocationsIt.next();
            if (invocation.getTarget() == listener) {
                invocationsIt.remove();
                didRemove = true;
            }
        }

        return didRemove;
    }

    // dispatches event to a list of listeners
    private void dispatchEvent(Collection invocations, Dispatch dispatch) {
        if (invocations == null || invocations.isEmpty()) {
            return;
        }

        // iterate over copy of the collection as there is a chance a caller would want to
        // (un)register another listener during event processing
        Iterator it = new ArrayList(invocations).iterator();
        while (it.hasNext()) {
            Invocation invocation = (Invocation) it.next();

            // fire invocation, detect if anything went wrong (e.g. GC'ed invocation
            // targets)
            if (!dispatch.fire(invocation)) {
                invocations.remove(invocation);
            }
        }
    }
}
