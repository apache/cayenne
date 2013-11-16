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

package org.apache.cayenne.util;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelListener;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.event.EventSubject;
import org.apache.cayenne.graph.GraphEvent;

/**
 * Contains access stack events related utility methods.
 * 
 * @since 1.2
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

        for (EventSubject subject : subjects) {
            // assume that subject name and listener method name match
            String fqSubject = subject.getSubjectName();
            String method = fqSubject.substring(fqSubject.lastIndexOf('/') + 1);

            // use non-blocking listeners for multi-threaded EM; blocking for single
            // threaded...

            if (manager.isSingleThreaded()) {
                manager.addListener(listener, method, GraphEvent.class, subject, sender);
            }
            else {
                manager.addNonBlockingListener(
                        listener,
                        method,
                        GraphEvent.class,
                        subject,
                        sender);
            }
        }
    }

    // not for instantiation
    private EventUtil() {
    }
}
