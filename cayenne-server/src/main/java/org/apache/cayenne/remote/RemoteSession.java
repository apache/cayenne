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

package org.apache.cayenne.remote;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.event.EventBridge;
import org.apache.cayenne.event.EventBridgeFactory;
import org.apache.cayenne.event.EventSubject;
import org.apache.cayenne.util.HashCodeBuilder;
import org.apache.cayenne.util.ToStringBuilder;

/**
 * A descriptor used by default service implementation to pass session parameters to the
 * client. It provides the client with details on how to invoke the service and how to
 * listen for the server events.
 * 
 * @since 1.2
 */
public class RemoteSession implements Serializable {

    static final Collection<EventSubject> SUBJECTS = Arrays.asList(
            DataChannel.GRAPH_CHANGED_SUBJECT,
            DataChannel.GRAPH_FLUSHED_SUBJECT,
            DataChannel.GRAPH_ROLLEDBACK_SUBJECT);

    protected String name;
    protected String sessionId;

    protected String eventBridgeFactory;
    protected Map eventBridgeParameters;

    // private constructor used by hessian deserialization mechanism
    @SuppressWarnings("unused")
    private RemoteSession() {

    }

    /**
     * Creates a HessianServiceDescriptor without server events support.
     */
    public RemoteSession(String sessionId) {
        this(sessionId, null, null);
    }

    /**
     * Creates a HessianServiceDescriptor. If <code>eventBridgeFactory</code> argument
     * is not null, session will support server events.
     */
    public RemoteSession(String sessionId, String eventBridgeFactory,
            Map eventBridgeParameters) {

        if (sessionId == null) {
            throw new IllegalArgumentException("Null sessionId");
        }

        this.sessionId = sessionId;
        this.eventBridgeFactory = eventBridgeFactory;
        this.eventBridgeParameters = eventBridgeParameters;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(71, 5).append(sessionId).toHashCode();
    }

    /**
     * Returns server session id. This is often the same as HttpSession id.
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns session group name. Group name is used for shared sessions.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isServerEventsEnabled() {
        return eventBridgeFactory != null;
    }

    /**
     * Creates an EventBridge that will listen for server events. Returns null if server
     * events support is not configured in the descriptor.
     * 
     * @throws CayenneRuntimeException if EventBridge startup fails for any reason.
     */
    public EventBridge createServerEventBridge() throws CayenneRuntimeException {

        if (!isServerEventsEnabled()) {
            return null;
        }

        try {
            EventBridgeFactory factory = (EventBridgeFactory) Class.forName(
                    eventBridgeFactory).newInstance();

            Map parameters = eventBridgeParameters != null
                    ? eventBridgeParameters
                    : Collections.EMPTY_MAP;

            // must use "name", not the sessionId as an external subject for the event
            // bridge
            return factory.createEventBridge(SUBJECTS, name, parameters);
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error creating EventBridge.", ex);
        }
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this)
                .append("sessionId", sessionId);

        if (eventBridgeFactory != null) {
            builder.append("eventBridgeFactory", eventBridgeFactory);
        }

        if (name != null) {
            builder.append("name", name);
        }

        return builder.toString();
    }
}
