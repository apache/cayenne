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
package org.apache.cayenne.configuration.rop.client;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.remote.service.LocalConnection;

/**
 * @since 3.1
 */
public class LocalConnectionProvider implements Provider<ClientConnection> {

    @Inject(ClientLocalRuntime.CLIENT_SERVER_CHANNEL_KEY)
    protected Provider<DataChannel> clientServerChannelProvider;

    public ClientConnection get() throws ConfigurationException {

        DataChannel clientServerChannel = clientServerChannelProvider.get();
        return new LocalConnection(
                clientServerChannel,
                LocalConnection.HESSIAN_SERIALIZATION);
    }
}
