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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.event.DefaultEventManager;
import org.apache.cayenne.remote.ClientChannel;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.remote.MockClientConnection;
import org.apache.cayenne.remote.hessian.HessianConnection;

public class ClientModuleTest extends TestCase {

    public void testClientConnection() {

        Map<String, String> properties = new HashMap<String, String>();
        properties.put(Constants.ROP_SERVICE_URL_PROPERTY, "http://localhost/YuM");
        ClientModule module = new ClientModule(properties);

        Injector injector = DIBootstrap.createInjector(module);

        ClientConnection connection = injector.getInstance(ClientConnection.class);
        assertNotNull(connection);
        assertTrue(connection instanceof HessianConnection);

        assertSame("Connection must be a singleton", connection, injector
                .getInstance(ClientConnection.class));
    }

    public void testObjectContextFactory() {

        Map<String, String> properties = new HashMap<String, String>();
        ClientModule module = new ClientModule(properties) {

            @Override
            public void configure(Binder binder) {
                super.configure(binder);

                // use a noop connection to prevent startup errors...
                binder.bind(ClientConnection.class).to(MockClientConnection.class);
            }
        };

        Injector injector = DIBootstrap.createInjector(module);

        ObjectContextFactory factory = injector.getInstance(ObjectContextFactory.class);
        assertNotNull(factory);
        assertSame("ObjectContextFactory must be a singleton", factory, injector
                .getInstance(ObjectContextFactory.class));
    }

    public void testDataChannel() {

        Map<String, String> properties = new HashMap<String, String>();
        ClientModule module = new ClientModule(properties) {

            @Override
            public void configure(Binder binder) {
                super.configure(binder);

                // use a noop connection to prevent startup errors...
                binder.bind(ClientConnection.class).to(MockClientConnection.class);
            }
        };

        Injector injector = DIBootstrap.createInjector(module);

        DataChannel channel = injector.getInstance(DataChannel.class);
        assertNotNull(channel);
        assertTrue(channel instanceof ClientChannel);
        assertSame("DataChannel must be a singleton", channel, injector
                .getInstance(DataChannel.class));

        ClientChannel clientChannel = (ClientChannel) channel;
        assertTrue(clientChannel.getConnection() instanceof MockClientConnection);
        assertTrue(clientChannel.getEventManager() instanceof DefaultEventManager);
        assertFalse(clientChannel.isChannelEventsEnabled());
    }

    public void testDataChannel_NoChannelEvents() {

        Map<String, String> properties = new HashMap<String, String>();
        properties.put(Constants.ROP_CHANNEL_EVENTS_PROPERTY, "true");
        ClientModule module = new ClientModule(properties) {

            @Override
            public void configure(Binder binder) {
                super.configure(binder);

                // use a noop connection to prevent startup errors...
                binder.bind(ClientConnection.class).to(MockClientConnection.class);
            }
        };

        Injector injector = DIBootstrap.createInjector(module);

        DataChannel channel = injector.getInstance(DataChannel.class);
        ClientChannel clientChannel = (ClientChannel) channel;
        assertTrue(clientChannel.isChannelEventsEnabled());
    }
}
