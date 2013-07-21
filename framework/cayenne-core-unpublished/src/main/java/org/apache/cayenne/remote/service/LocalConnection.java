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

import java.io.Serializable;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.event.EventBridge;
import org.apache.cayenne.remote.BaseConnection;
import org.apache.cayenne.remote.ClientMessage;
import org.apache.cayenne.remote.hessian.service.HessianUtil;
import org.apache.cayenne.util.Util;

/**
 * A ClientConnection that connects to a DataChannel. Used as an emulator of a remote
 * service. Emulation includes serialization/deserialization of objects.
 * 
 * @since 1.2
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
    @Override
    protected void beforeSendMessage(ClientMessage message) {
        // noop
    }

    /**
     * Dispatches a message to an internal handler.
     */
    @Override
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
