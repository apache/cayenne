/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne.rop.protostuff;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.rop.client.ClientConstants;
import org.apache.cayenne.configuration.rop.client.ClientLocalRuntime;
import org.apache.cayenne.configuration.rop.client.ClientRuntime;
import org.apache.cayenne.configuration.rop.client.ProtostuffModule;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.java8.Java8Module;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.remote.service.ProtostuffLocalConnectionProvider;
import org.junit.Before;

import java.util.HashMap;
import java.util.Map;

public class RuntimeBase extends ProtostuffProperties {

    protected ServerRuntime serverRuntime;
    protected ClientRuntime clientRuntime;
    protected ObjectContext context;

    @Before
    public void setUpRuntimes() throws Exception {
        this.serverRuntime = ServerRuntime
                .builder()
                .addConfig("cayenne-protostuff.xml")
                .addModule(new ProtostuffModule())
                .build();

        Map<String, String> properties = new HashMap<>();
        properties.put(ClientConstants.ROP_CHANNEL_EVENTS_PROPERTY, Boolean.TRUE.toString());

        Module module = binder -> binder.bind(ClientConnection.class)
                .toProviderInstance(new ProtostuffLocalConnectionProvider());

        this.clientRuntime = new ClientLocalRuntime(
                serverRuntime.getInjector(),
                properties,
                new ProtostuffModule(),
                new Java8Module(),
                module);

        this.context = clientRuntime.newContext();
    }


}
