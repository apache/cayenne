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

package org.apache.cayenne.remote.hessian;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.event.EventBridge;
import org.apache.cayenne.remote.BaseConnection;
import org.apache.cayenne.remote.ClientMessage;
import org.apache.cayenne.remote.RemoteService;
import org.apache.cayenne.remote.RemoteSession;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.caucho.hessian.client.HessianRuntimeException;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.SerializerFactory;

/**
 * An ClientConnection that passes messages to a remotely deployed HessianService. It
 * supports HTTP BASIC authentication. HessianConnection serializes messages using Hessian
 * binary web service protocol over HTTP. For more info on Hessian see Caucho site at <a
 * href="http://www.caucho.com/resin-3.0/protocols/hessian.xtp">http://www.caucho.com/resin-3.0/protocols/hessian.xtp</a>.
 * HessianConnection supports logging of message traffic via Jakarta commons-logging API.
 * 
 * @since 1.2
 */
public class HessianConnection extends BaseConnection {

    private static Log logger = LogFactory.getLog(HessianConnection.class);
    
    public static final String[] CLIENT_SERIALIZER_FACTORIES = new String[] {
            ClientSerializerFactory.class.getName()
    };

    protected String url;
    protected String userName;
    protected String password;
    protected String sharedSessionName;

    protected RemoteSession session;
    protected RemoteService service;
    protected SerializerFactory serializerFactory;
    
    /**
     * Creates HessianConnection that will establish dedicated session and will not use
     * HTTP basic authentication.
     */
    public HessianConnection(String url) {
        this(url, null, null, null);
    }

    /**
     * Creates a HessianConnection. This constructor can optionally setup basic
     * authentication credentials and configure shared session. <code>url</code> is the
     * only required parameter.
     */
    public HessianConnection(String url, String userName, String password,
            String sharedSessionName) {
        if (url == null) {
            throw new IllegalArgumentException("URL of Cayenne service is null.");
        }

        this.url = url;
        this.userName = userName;
        this.password = password;
        this.sharedSessionName = sharedSessionName;
    }

    /**
     * Returns a URL of Cayenne service used by this connector.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns user name that is used for basic authentication when connecting to the
     * cayenne server.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Returns password that is used for basic authentication when connecting to the
     * cayenne server.
     */
    public String getPassword() {
        return password;
    }

    public String getSharedSessionName() {
        return sharedSessionName;
    }

    public EventBridge getServerEventBridge() throws CayenneRuntimeException {
        if (session == null) {
            connect();
        }

        return session.isServerEventsEnabled() ? session.createServerEventBridge() : null;
    }

    /**
     * Returns internal RemoteSession instance.
     */
    public RemoteSession getSession() {
        return session;
    }

    /**
     * Establishes server session if needed.
     */
    @Override
    protected void beforeSendMessage(ClientMessage message)
            throws CayenneRuntimeException {
        // for now only support session-based communications...
        if (session == null) {
            connect();
        }
    }

    /**
     * Sends a message to remote Cayenne Hessian service.
     */
    @Override
    protected Object doSendMessage(ClientMessage message) throws CayenneRuntimeException {
        try {
            return service.processMessage(message);
        }
        catch (CayenneRuntimeException e) {
            throw e;
        }
        catch (Throwable th) {
            th = unwindThrowable(th);
            String errorMessage = buildExceptionMessage("Remote error", th);
            throw new CayenneRuntimeException(errorMessage, th);
        }
    }

    /**
     * Establishes a session with remote service.
     */
    protected synchronized void connect() throws CayenneRuntimeException {
        if (session != null) {
            return;
        }

        long t0 = 0;
        if (logger.isInfoEnabled()) {
            t0 = System.currentTimeMillis();
            StringBuilder log = new StringBuilder("Connecting to [");
            if (userName != null) {
                log.append(userName);

                if (password != null) {
                    log.append(":*******");
                }

                log.append("@");
            }

            log.append(url);
            log.append("]");

            if (sharedSessionName != null) {
                log.append(" - shared session '").append(sharedSessionName).append("'");
            }
            else {
                log.append(" - dedicated session.");
            }

            logger.info(log.toString());
        }

        // init service proxy...
        HessianProxyFactory factory = new HessianProxyFactory(this);
        factory.setSerializerFactory(HessianConfig.createFactory(
                CLIENT_SERIALIZER_FACTORIES,
                null));
        factory.setUser(userName);
        factory.setPassword(password);
        factory.setReadTimeout(getReadTimeout());

        this.serializerFactory = factory.getSerializerFactory();

        try {
            this.service = (RemoteService) factory.create(RemoteService.class, url);
        }
        catch (Throwable th) {
            th = unwindThrowable(th);
            String message = buildExceptionMessage("URL error", th);
            throw new CayenneRuntimeException(message, th);
        }

        // create server session...
        try {
            session = (sharedSessionName != null) ? service
                    .establishSharedSession(sharedSessionName) : service
                    .establishSession();

            if (logger.isInfoEnabled()) {
                long time = System.currentTimeMillis() - t0;
                logger.info("=== Connected, session: "
                        + session
                        + " - took "
                        + time
                        + " ms.");
            }
        }
        catch (Throwable th) {
            th = unwindThrowable(th);
            String message = buildExceptionMessage(
                    "Error establishing remote session",
                    th);
            logger.info(message, th);
            throw new CayenneRuntimeException(message, th);
        }

        // TODO: send a connect event...
    }

    String buildExceptionMessage(String message, Throwable th) {

        StringBuilder buffer = new StringBuilder(message);
        buffer.append(". URL - ").append(url);

        String thMessage = th.getMessage();
        if (!Util.isEmptyString(thMessage)) {
            buffer.append("; CAUSE - ").append(thMessage);
        }

        return buffer.toString();
    }

    /**
     * Utility method to get exception cause. Implements special handling of Hessian
     * exceptions.
     */
    Throwable unwindThrowable(Throwable th) {
        if (th instanceof HessianProtocolException) {
            Throwable cause = ((HessianProtocolException) th).getRootCause();

            if (cause != null) {
                return unwindThrowable(cause);
            }
        }
        else if (th instanceof HessianRuntimeException) {
            Throwable cause = ((HessianRuntimeException) th).getRootCause();

            if (cause != null) {
                return unwindThrowable(cause);
            }
        }

        return Util.unwindException(th);
    }

    public SerializerFactory getSerializerFactory() {
        return serializerFactory;
    }

}
