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
package org.objectstyle.cayenne.remote.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataChannel;
import org.objectstyle.cayenne.access.ClientServerChannel;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.DefaultConfiguration;
import org.objectstyle.cayenne.remote.ClientMessage;
import org.objectstyle.cayenne.remote.RemoteService;
import org.objectstyle.cayenne.remote.RemoteSession;
import org.objectstyle.cayenne.util.Util;

/**
 * A generic implementation of an RemoteService. Subclasses can be customized to work with
 * different remoting mechanisms, such as Hessian or JAXRPC.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public abstract class BaseRemoteService implements RemoteService {

    public static final String EVENT_BRIDGE_FACTORY_PROPERTY = "cayenne.RemoteService.EventBridge.factory";

    // keep logger non-static so that it could be garbage collected with this instance..
    private final Logger logObj = Logger.getLogger(BaseRemoteService.class);

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

        ServerSession handler = getServerSession();

        if (handler == null) {
            throw new CayenneRuntimeException("No session associated with request.");
        }

        logObj.debug("processMessage, sessionId: " + handler.getSession().getSessionId());

        // intercept and log exceptions
        try {
            return DispatchHelper.dispatch(handler.getChannel(), message);
        }
        catch (Throwable th) {
            th = Util.unwindException(th);
            logObj.info("error processing message", th);
            throw th;
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

    protected DataChannel createChannel() {
        return new ClientServerChannel(domain, false);
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
