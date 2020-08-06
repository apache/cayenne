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

package org.apache.cayenne.rop.protostuff;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.rop.client.ClientConstants;
import org.apache.cayenne.configuration.rop.client.ClientRuntime;
import org.apache.cayenne.configuration.rop.client.LocalClientServerChannelProvider;
import org.apache.cayenne.configuration.rop.client.ProtostuffModule;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.remote.service.ProtostuffLocalConnectionProvider;
import org.junit.Before;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RuntimeBase extends ProtostuffProperties {

    protected ServerRuntime serverRuntime;
    protected ClientRuntime clientRuntime;
    protected ObjectContext context;

    @Before
    public void setUpRuntimes() throws Exception {
        this.serverRuntime = ServerRuntime.builder()
                .addConfig("cayenne-protostuff.xml")
                .build();

        Module module = binder -> {
            binder.bind(ClientConnection.class)
                    .toProviderInstance(new ProtostuffLocalConnectionProvider());
            binder.bind(Key.get(DataChannel.class, ClientRuntime.CLIENT_SERVER_CHANNEL_KEY))
                    .toProviderInstance(new LocalClientServerChannelProvider(serverRuntime.getInjector()));
        };

        this.clientRuntime = ClientRuntime.builder()
                .properties(Collections.singletonMap(ClientConstants.ROP_CHANNEL_EVENTS_PROPERTY, "true"))
                .addModule(module)
                .build();

        this.context = clientRuntime.newContext();
    }


}
