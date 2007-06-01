/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.event;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.event.EventManager.Dispatch;
import org.objectstyle.cayenne.util.Invocation;

/**
 * Stores a set of Invocation objects, organizing them by sender. 
 * Listeners have an option to receive events for a particular sender
 * or to receive all events. EventManager creates one DispatchQueue per
 * EventSubject. DispatchQueue is thread-safe - all methods that read/modify
 * internal collections are synchronized.
 * 
 * @author Andrei Adamchik
 * @since 1.1
 */
class DispatchQueue {
    private static Logger logObj = Logger.getLogger(DispatchQueue.class);

    // TODO: implement a maintenance thread to cleanup dead invocations with deallocated targets

    private Set subjectInvocations = new HashSet();
    private Map invocationsBySender = new WeakHashMap();

    /**
     * Dispatches event to all listeners in the queue that are
     * registered for this event and sender.
     */
    synchronized void dispatchEvent(Dispatch dispatch) {
        // dispatch to "any sender" listeners
        dispatchEvent(subjectInvocations, dispatch);

        // dispatch to the given sender listeners
        Object sender = dispatch.getSender();
        dispatchEvent(invocationsForSender(sender, false), dispatch);
    }

    synchronized void addInvocation(Invocation invocation, Object sender) {
        if (sender == null) {
            subjectInvocations.add(invocation);
        }
        else {
            invocationsForSender(sender, true).add(invocation);
        }
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

        Iterator it = invocations.iterator();
        while (it.hasNext()) {
            Invocation invocation = (Invocation) it.next();

            // fire invocation, detect if anything went wrong (e.g. GC'ed invocation targets)
            if (!dispatch.fire(invocation)) {
                it.remove();
                logObj.debug(
                    "Failed invocation, removing: " + invocation.getMethod().getName());
            }
        }
    }
}
