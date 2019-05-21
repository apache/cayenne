/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne.remote.service;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.event.EventBridge;
import org.apache.cayenne.remote.BaseConnection;
import org.apache.cayenne.remote.ClientMessage;
import org.apache.cayenne.rop.ROPSerializationService;

import java.io.IOException;

/**
 * A ClientConnection that connects to a DataChannel. Used as an emulator of a remote
 * service. Emulation includes serialization/deserialization of objects via {@link ROPSerializationService}.
 *
 * {@link LocalConnection} should be replaced by this one after moving all ROP functionality to the separate module.
 * It'll provide more flexibility around which serialization should be used.
 */
public class ProtostuffLocalConnection extends BaseConnection {

    protected DataChannel channel;

    @Inject
    private ROPSerializationService serializationService;

    public ProtostuffLocalConnection(DataChannel channel) {
        this.channel = channel;
    }

    @Override
    public EventBridge getServerEventBridge() throws CayenneRuntimeException {
        return null;
    }

    @Override
    protected void beforeSendMessage(ClientMessage message) throws CayenneRuntimeException {
        // noop
    }

    @Override
    protected Object doSendMessage(ClientMessage message) throws CayenneRuntimeException {
        try {
            ClientMessage processedMessage = (ClientMessage) cloneViaSerializationService(message);

            Object result = DispatchHelper.dispatch(channel, processedMessage);

            return cloneViaSerializationService(result);
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error deserializing result", ex);
        }
    }

    public Object cloneViaSerializationService(Object object) throws IOException {
        byte[] data = serializationService.serialize(object);
        return serializationService.deserialize(data, object.getClass());
    }

}
