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
package org.objectstyle.cayenne.remote.hessian;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.event.EventBridge;
import org.objectstyle.cayenne.remote.BaseConnection;
import org.objectstyle.cayenne.remote.ClientMessage;
import org.objectstyle.cayenne.remote.RemoteService;
import org.objectstyle.cayenne.remote.RemoteSession;
import org.objectstyle.cayenne.util.Util;

import com.caucho.hessian.client.HessianRuntimeException;
import com.caucho.hessian.io.HessianProtocolException;

/**
 * An ClientConnection that passes messages to a remotely deployed HessianService. It
 * supports HTTP BASIC authentication. HessianConnection serializes messages using Hessian
 * binary web service protocol over HTTP. For more info on Hessian see Caucho site at <a
 * href="http://www.caucho.com/resin-3.0/protocols/hessian.xtp">http://www.caucho.com/resin-3.0/protocols/hessian.xtp</a>.
 * HessianConnection supports logging of message traffic via Jakarta commons-logging API.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class HessianConnection extends BaseConnection {

    public static final String[] CLIENT_SERIALIZER_FACTORIES = new String[] {
            ClientSerializerFactory.class.getName(), EnumSerializerProxy.class.getName()
    };

    protected String url;
    protected String userName;
    protected String password;
    protected String sharedSessionName;

    protected RemoteSession session;
    protected RemoteService service;

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
     * Retunrs internal RemoteSession instance.
     */
    RemoteSession getSession() {
        return session;
    }

    /**
     * Establishes server session if needed.
     */
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
    protected Object doSendMessage(ClientMessage message) throws CayenneRuntimeException {
        try {
            return service.processMessage(message);
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
            StringBuffer log = new StringBuffer("Connecting to [");
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
            th.printStackTrace();
            String message = buildExceptionMessage(
                    "Error establishing remote session",
                    th);
            throw new CayenneRuntimeException(message, th);
        }

        // TODO: send a connect event...
    }

    String buildExceptionMessage(String message, Throwable th) {

        StringBuffer buffer = new StringBuffer(message);
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
}