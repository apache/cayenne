/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.util;

import org.objectstyle.cayenne.DataChannel;
import org.objectstyle.cayenne.DataChannelListener;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.event.EventSubject;
import org.objectstyle.cayenne.graph.GraphEvent;

/**
 * Contains access stack events related utility methods.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class EventUtil {

    static final EventSubject[] CHANNEL_SUBJECTS = new EventSubject[] {
            DataChannel.GRAPH_CHANGED_SUBJECT, DataChannel.GRAPH_FLUSHED_SUBJECT,
            DataChannel.GRAPH_ROLLEDBACK_SUBJECT
    };

    /**
     * Utility method that sets up a GraphChangeListener to be notified when DataChannel
     * posts an event.
     * 
     * @return false if an DataChannel doesn't have an EventManager and therefore does not
     *         support events.
     */
    public static boolean listenForChannelEvents(
            DataChannel channel,
            DataChannelListener listener) {

        EventManager manager = channel.getEventManager();

        if (manager == null) {
            return false;
        }

        listenForSubjects(manager, listener, channel, CHANNEL_SUBJECTS);
        return true;
    }

    /**
     * Listen for events from all channels that use a given EventManager.
     */
    public static boolean listenForChannelEvents(
            EventManager manager,
            DataChannelListener listener) {

        if (manager == null) {
            return false;
        }

        listenForSubjects(manager, listener, null, CHANNEL_SUBJECTS);
        return true;
    }

    /**
     * Registers GraphEventListener for multiple subjects at once.
     */
    static void listenForSubjects(
            EventManager manager,
            DataChannelListener listener,
            Object sender,
            EventSubject[] subjects) {

        for (int i = 0; i < subjects.length; i++) {
            // assume that subject name and listener method name match
            String fqSubject = subjects[i].getSubjectName();
            String method = fqSubject.substring(fqSubject.lastIndexOf('/') + 1);

            manager.addListener(listener, method, GraphEvent.class, subjects[i], sender);
        }
    }

    // not for instantiation
    private EventUtil() {
    }
}
