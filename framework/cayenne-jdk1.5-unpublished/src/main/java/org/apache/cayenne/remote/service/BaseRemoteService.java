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
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.conf.DefaultConfiguration;
import org.apache.cayenne.remote.ClientMessage;
import org.apache.cayenne.remote.RemoteService;
import org.apache.cayenne.remote.RemoteSession;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A generic implementation of an RemoteService. Subclasses can be customized to work with
 * different remoting mechanisms, such as Hessian or JAXRPC.
 * 
 * @since 1.2
 */
public abstract class BaseRemoteService implements RemoteService {

    public static final String EVENT_BRIDGE_FACTORY_PROPERTY = "cayenne.RemoteService.EventBridge.factory";

    // keep logger non-static so that it could be garbage collected with this instance..
    private final Log logObj = LogFactory.getLog(BaseRemoteService.class);

    protected Configuration configuration;
    protected DataDomain domain;

    protected String eventBridgeFactoryName;
    protected Map eventBridgeParameters;

    public String getEventBridgeFactoryName() {
        return eventBridgeFactoryName;
    }

    public Map getEventBridgeParameters() {
        return eventBridgeParameters != null ? Collections
                .unmodifiableMap(eventBridgeParameters) : Collections.EMPTY_MAP;
    }

    /**
     * A method that sets up a service, initializing Cayenne stack. Should be invoked by
     * subclasses from their appropriate service lifecycle methods.
     */
    protected void initService(Map properties) throws CayenneRuntimeException {

        // start Cayenne service
        logObj.debug(this.getClass().getName() + " is starting");

        initCayenneStack(properties);
        initEventBridgeParameters(properties);

        logObj.debug(getClass().getName() + " started");
    }

    /**
     * Shuts down this service. Should be invoked by subclasses from their appropriate
     * service lifecycle methods.
     */
    protected void destroyService() {
        if (configuration != null) {
            configuration.shutdown();
        }

        logObj.debug(getClass().getName() + " destroyed");
    }

    /**
     * Returns a DataChannel that is a parent of all session DataChannels.
     */
    public DataChannel getRootChannel() {
        return domain;
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
        logObj.debug("Session requested by client");

        RemoteSession session = createServerSession().getSession();

        logObj.debug("Established client session: " + session);
        return session;
    }

    public RemoteSession establishSharedSession(String name) {
        logObj.debug("Shared session requested by client. Group name: " + name);

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

        logObj.debug("processMessage, sessionId: " + handler.getSession().getSessionId());

        // intercept and log exceptions
        try {
            return DispatchHelper.dispatch(handler.getChannel(), message);
        }
        catch (Throwable th) {
            th = Util.unwindException(th);
            logObj.info("error processing message", th);

            // This exception will probably be propagated to the client.
            // Recast the exception to a serializable form.
            Exception cause = new Exception(th.getLocalizedMessage());

            StringBuilder wrapperMessage = new StringBuilder();
            wrapperMessage.append("Exception processing message ")
                .append(message.getClass().getName())
                .append(" of type ").append(message.toString());
            
            throw new CayenneRuntimeException(wrapperMessage.toString(), cause);
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
     * dedicated sessions, one channel per client is created. <p/> This implementation
     * returns {@link ClientServerChannel} instance wrapping a DataContext. Subclasses may
     * override the method to customize channel creation. For instance they may wrap
     * channel in the custom interceptors to handle transactions or security.
     */
    protected DataChannel createChannel() {
        return new ClientServerChannel(domain);
    }

    /**
     * Sets up Cayenne stack.
     */
    protected void initCayenneStack(Map properties) {
        Configuration cayenneConfig = new DefaultConfiguration(
                Configuration.DEFAULT_DOMAIN_FILE);

        try {
            cayenneConfig.initialize();
            cayenneConfig.didInitialize();
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error starting Cayenne", ex);
        }

        this.configuration = cayenneConfig;
        
        // TODO (Andrus 10/15/2005) this assumes that mapping has a single domain...
        // do something about multiple domains
        this.domain = cayenneConfig.getDomain();
    }

    /**
     * Initializes EventBridge parameters for remote clients peer-to-peer communications.
     */
    protected void initEventBridgeParameters(Map properties) {
        String eventBridgeFactoryName = (String) properties
                .get(BaseRemoteService.EVENT_BRIDGE_FACTORY_PROPERTY);

        if (eventBridgeFactoryName != null) {

            Map eventBridgeParameters = new HashMap(properties);
            eventBridgeParameters.remove(BaseRemoteService.EVENT_BRIDGE_FACTORY_PROPERTY);

            this.eventBridgeFactoryName = eventBridgeFactoryName;
            this.eventBridgeParameters = eventBridgeParameters;
        }
    }
}
