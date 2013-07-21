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
package org.apache.cayenne.unit.di.client;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.configuration.rop.client.ClientRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.remote.service.LocalConnection;

public class ClientServerChannelProvider implements Provider<ClientServerChannel> {

    @Inject
    protected Provider<ClientRuntime> clientRuntimeProvider;

    public ClientServerChannel get() throws ConfigurationException {

        LocalConnection connection = (LocalConnection) clientRuntimeProvider
                .get()
                .getConnection();
        
        ClientServerDataChannelDecorator channelDecorator = (ClientServerDataChannelDecorator) connection.getChannel();
        return (ClientServerChannel) channelDecorator.getDelegate();
    }
}
