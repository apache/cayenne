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

package org.apache.cayenne.remote.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.remote.ClientMessage;
import org.apache.cayenne.remote.RemoteService;
import org.apache.cayenne.remote.RemoteSession;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A generic implementation of an RemoteService. Can be subclassed to work with different
 * remoting mechanisms, such as Hessian or JAXRPC.
 * 
 * @since 1.2
 */
public abstract class BaseRemoteService implements RemoteService {

    // keep logger non-static so that it could be garbage collected with this instance.
    protected final Log logger;

    protected ObjectContextFactory contextFactory;
    protected String eventBridgeFactoryName;
    protected Map<String, String> eventBridgeParameters;

    /**
     * @since 3.1
     */
    public BaseRemoteService(ObjectContextFactory contextFactory,
            Map<String, String> eventBridgeProperties) {

        logger = LogFactory.getLog(getClass());

        // start Cayenne service
        logger.debug("ROP service is starting");

        this.contextFactory = contextFactory;
        initEventBridgeParameters(eventBridgeProperties);

        logger.debug(getClass().getName() + " started");
    }

    public String getEventBridgeFactoryName() {
        return eventBridgeFactoryName;
    }

    public Map<String, String> getEventBridgeParameters() {
        return eventBridgeParameters != null ? Collections
                .unmodifiableMap(eventBridgeParameters) : Collections.EMPTY_MAP;
    }

    /**
     * Creates a new ServerSession with a dedicated DataChannel.
     */
    protected abstract ServerSession createServerSession();

    /**
     * Creates a new ServerSession based on a shared DataChannel.
     * 
     * @param name shared session name used to lookup a shared DataChannel.
     */
    protected abstract ServerSession createServerSession(String name);

    /**
     * Returns a ServerSession object that represents Cayenne-related state associated
     * with the current session. If ServerSession hasn't been previously saved, returns
     * null.
     */
    protected abstract ServerSession getServerSession();

    public RemoteSession establishSession() {
        logger.debug("Session requested by client");

        RemoteSession session = createServerSession().getSession();

        logger.debug("Established client session: " + session);
        return session;
    }

    public RemoteSession establishSharedSession(String name) {
        logger.debug("Shared session requested by client. Group name: " + name);

        if (name == null) {
            throw new CayenneRuntimeException("Invalid null shared session name");
        }

        return createServerSession(name).getSession();
    }

    public Object processMessage(ClientMessage message) throws Throwable {

        if (message == null) {
            throw new IllegalArgumentException("Null client message.");
        }

        ServerSession handler = getServerSession();

        if (handler == null) {
            throw new MissingSessionException("No session associated with request.");
        }

        logger.debug("processMessage, sessionId: " + handler.getSession().getSessionId());

        // intercept and log exceptions
        try {
            return DispatchHelper.dispatch(handler.getChannel(), message);
        }
        catch (Throwable th) {

            StringBuilder wrapperMessage = new StringBuilder();
            wrapperMessage
                    .append("Exception processing message ")
                    .append(message.getClass().getName())
                    .append(" of type ")
                    .append(message);

            String wrapperMessageString = wrapperMessage.toString();
            logger.info(wrapperMessageString, th);

            // This exception will probably be propagated to the client.
            // Recast the exception to a serializable form.
            Exception cause = new Exception(Util
                    .unwindException(th)
                    .getLocalizedMessage());

            throw new CayenneRuntimeException(wrapperMessageString, cause);
        }
    }

    protected RemoteSession createRemoteSession(
            String sessionId,
            String name,
            boolean enableEvents) {
        RemoteSession session = (enableEvents) ? new RemoteSession(
                sessionId,
                eventBridgeFactoryName,
                eventBridgeParameters) : new RemoteSession(sessionId);

        session.setName(name);
        return session;
    }

    /**
     * Creates a server-side channel that will handle all client requests. For shared
     * sessions the same channel instance is reused for the entire group of clients. For
     * dedicated sessions, one channel per client is created.
     * <p/>
     * This implementation returns {@link ClientServerChannel} instance wrapping a
     * DataContext. Subclasses may override the method to customize channel creation. For
     * instance they may wrap channel in the custom interceptors to handle transactions or
     * security.
     */
    protected DataChannel createChannel() {
        return new ClientServerChannel((DataContext) contextFactory.createContext());
    }

    /**
     * Initializes EventBridge parameters for remote clients peer-to-peer communications.
     */
    protected void initEventBridgeParameters(Map<String, String> properties) {
        String eventBridgeFactoryName = properties
                .get(Constants.SERVER_ROP_EVENT_BRIDGE_FACTORY_PROPERTY);

        if (eventBridgeFactoryName != null) {

            Map<String, String> eventBridgeParameters = new HashMap<String, String>(
                    properties);
            eventBridgeParameters
                    .remove(Constants.SERVER_ROP_EVENT_BRIDGE_FACTORY_PROPERTY);

            this.eventBridgeFactoryName = eventBridgeFactoryName;
            this.eventBridgeParameters = eventBridgeParameters;
        }
    }
}
