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

package org.apache.cayenne.util;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelListener;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.event.EventHandler;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.event.EventSubject;
import org.apache.cayenne.graph.GraphEvent;

/**
 * Contains access stack events related utility methods.
 * 
 * @since 1.2
 */
public class EventUtil {

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

        listenForSubjects(manager, listener, channel);
        return true;
    }

    /**
     * Utility method that sets up a GraphChangeListener to be notified when an ObjectContext posts an event, such as
     * the events a parent context posts on flush, rollback, or a change merged from one of its nested contexts.
     *
     * @return false if the context's channel doesn't have an EventManager and therefore does not support events.
     * @since 5.0
     */
    public static boolean listenForChannelEvents(
            ObjectContext context,
            DataChannelListener listener) {

        EventManager manager = context.getChannel().getEventManager();

        if (manager == null) {
            return false;
        }

        listenForSubjects(manager, listener, context);
        return true;
    }

    /**
     * Registers GraphEventListener for multiple subjects at once.
     */
    static void listenForSubjects(EventManager manager, DataChannelListener listener, Object sender) {
        listen(manager, listener, sender, DataChannel.GRAPH_CHANGED_SUBJECT, DataChannelListener::graphChanged);
        listen(manager, listener, sender, DataChannel.GRAPH_FLUSHED_SUBJECT, DataChannelListener::graphFlushed);
        listen(manager, listener, sender, DataChannel.GRAPH_ROLLEDBACK_SUBJECT, DataChannelListener::graphRolledback);
    }

    private static void listen(
            EventManager manager,
            DataChannelListener listener,
            Object sender,
            EventSubject subject,
            EventHandler<DataChannelListener, GraphEvent> handler) {

        // use non-blocking listeners for multi-threaded EM; blocking for single threaded...
        if (manager.isSingleThreaded()) {
            manager.addListener(listener, GraphEvent.class, handler, subject, sender);
        } else {
            manager.addNonBlockingListener(listener, GraphEvent.class, handler, subject, sender);
        }
    }

    // not for instantiation
    private EventUtil() {
    }
}
