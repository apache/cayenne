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
package org.apache.cayenne.rop;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.remote.ClientMessage;
import org.apache.cayenne.remote.RemoteService;
import org.apache.cayenne.remote.RemoteSession;

import java.io.IOException;
import java.rmi.RemoteException;

public class ProxyRemoteService implements RemoteService {

    protected ROPSerializationService serializationService;

    protected ROPConnector ropConnector;

    public ProxyRemoteService(@Inject ROPSerializationService serializationService, @Inject ROPConnector ropConnector) {
        this.serializationService = serializationService;
        this.ropConnector = ropConnector;
    }

    @Override
    public RemoteSession establishSession() throws RemoteException {
        try {
            return serializationService.deserialize(ropConnector.establishSession(), RemoteSession.class);
        } catch (IOException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public RemoteSession establishSharedSession(String name) throws RemoteException {
        try {
            return serializationService.deserialize(ropConnector.establishSharedSession(name), RemoteSession.class);
        } catch (IOException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public Object processMessage(ClientMessage message) throws RemoteException, Throwable {
        return serializationService.deserialize(ropConnector.sendMessage(serializationService.serialize(message)), Object.class);
    }
}
