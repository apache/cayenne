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

import java.io.Serializable;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataChannel;
import org.objectstyle.cayenne.event.EventBridge;
import org.objectstyle.cayenne.remote.BaseConnection;
import org.objectstyle.cayenne.remote.ClientMessage;
import org.objectstyle.cayenne.remote.hessian.service.HessianUtil;
import org.objectstyle.cayenne.util.Util;

/**
 * A ClientConnection that connects to a DataChannel. Used as an emulator of a remote
 * service. Emulation includes serialization/deserialization of objects.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class LocalConnection extends BaseConnection {

    public static final int NO_SERIALIZATION = 0;
    public static final int JAVA_SERIALIZATION = 1;
    public static final int HESSIAN_SERIALIZATION = 2;

    protected DataChannel channel;
    protected int serializationPolicy;

    /**
     * Creates LocalConnector with specified handler and no serialization.
     */
    public LocalConnection(DataChannel handler) {
        this(handler, NO_SERIALIZATION);
    }

    /**
     * Creates a LocalConnector with specified handler and serialization policy. Valid
     * policies are defined as final static int field in this class.
     */
    public LocalConnection(DataChannel handler, int serializationPolicy) {
        this.channel = handler;

        // convert invalid policy to NO_SER..
        this.serializationPolicy = serializationPolicy == JAVA_SERIALIZATION
                || serializationPolicy == HESSIAN_SERIALIZATION
                ? serializationPolicy
                : NO_SERIALIZATION;
    }

    public boolean isSerializingMessages() {
        return serializationPolicy == JAVA_SERIALIZATION
                || serializationPolicy == HESSIAN_SERIALIZATION;
    }

    /**
     * Returns wrapped DataChannel.
     */
    public DataChannel getChannel() {
        return channel;
    }

    /**
     * Returns null.
     */
    public EventBridge getServerEventBridge() {
        return null;
    }

    /**
     * Does nothing.
     */
    protected void beforeSendMessage(ClientMessage message) {
        // noop
    }

    /**
     * Dispatches a message to an internal handler.
     */
    protected Object doSendMessage(ClientMessage message) throws CayenneRuntimeException {

        ClientMessage processedMessage;

        try {
            switch (serializationPolicy) {
                case HESSIAN_SERIALIZATION:
                    processedMessage = (ClientMessage) HessianUtil
                            .cloneViaClientServerSerialization(message, channel
                                    .getEntityResolver());
                    break;

                case JAVA_SERIALIZATION:
                    processedMessage = (ClientMessage) Util
                            .cloneViaSerialization(message);
                    break;

                default:
                    processedMessage = message;
            }

        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error serializing message", ex);
        }

        Serializable result = (Serializable) DispatchHelper.dispatch(
                channel,
                processedMessage);

        try {
            switch (serializationPolicy) {
                case HESSIAN_SERIALIZATION:
                    return HessianUtil.cloneViaServerClientSerialization(result, channel
                            .getEntityResolver());
                case JAVA_SERIALIZATION:
                    return Util.cloneViaSerialization(result);
                default:
                    return result;
            }
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error deserializing result", ex);
        }

    }
}
