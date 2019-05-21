/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.configuration.rop.client;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.remote.ClientChannel;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.remote.MockClientConnection;
import org.apache.cayenne.rop.HttpClientConnection;
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * @since 4.0
 */
public class ClientRuntimeBuilderTest {

    private ClientRuntime runtime;

    @After
    public void stopRuntime() {
        if (runtime != null) {
            runtime.shutdown();
        }
    }

    @Test
    public void testDefaultBuild() {
        runtime = new ClientRuntimeBuilder().build();

        Map<String, String> properties = runtime.getInjector()
                .getInstance(Key.getMapOf(String.class, String.class, Constants.PROPERTIES_MAP));
        assertTrue(properties.isEmpty());

        Collection<Module> modules = runtime.getModules();
        assertEquals(1, modules.size());
        assertThat(modules.iterator().next(), instanceOf(ClientModule.class));
    }

    @Test
    public void testNoAutoLoading() {
        runtime = new ClientRuntimeBuilder().disableModulesAutoLoading().build();

        Map<String, String> properties = runtime.getInjector()
                .getInstance(Key.getMapOf(String.class, String.class, Constants.PROPERTIES_MAP));
        assertTrue(properties.isEmpty());

        Collection<Module> modules = runtime.getModules();
        assertEquals(1, modules.size());
        assertThat(modules.iterator().next(), instanceOf(ClientModule.class));
    }

    @Test
    public void testExtraModules() {

        Module m = mock(Module.class);

        runtime = new ClientRuntimeBuilder().addModule(m).build();

        Collection<Module> modules = runtime.getModules();
        assertEquals(2, modules.size());
        Module[] array = modules.toArray(new Module[2]);
        assertThat(array[0], instanceOf(ClientModule.class));
        assertSame(m, array[1]);
    }

    @Test
    public void testProperties() {
        Map<String, String> properties = Collections.singletonMap("test", "test_value");

        runtime = new ClientRuntimeBuilder().properties(properties).build();

        Map<String, String> injectedProperties = runtime.getInjector()
                .getInstance(Key.getMapOf(String.class, String.class, Constants.PROPERTIES_MAP));
        assertEquals(properties, injectedProperties);
    }

    @Test
    public void testClientConnection() {

        Map<String, String> properties1 = new HashMap<>();
        properties1.put(ClientConstants.ROP_SERVICE_URL_PROPERTY, "http://localhost/YuM");
        ClientModule module = new ClientModule(){

            @Override
            public void configure(Binder binder) {
                super.configure(binder);
                ServerModule.contributeProperties(binder).putAll(properties1);
            }
        };


        Injector injector = DIBootstrap.createInjector(module);

        ClientConnection connection = injector.getInstance(ClientConnection.class);
        assertNotNull(connection);
        assertTrue(connection instanceof HttpClientConnection);

        assertSame("Connection must be a singleton", connection, injector
                .getInstance(ClientConnection.class));
    }

    @Test
    public void testDataChannel_NoChannelEvents() {

        Map<String, String> properties1 = new HashMap<>();
        properties1.put(ClientConstants.ROP_CHANNEL_EVENTS_PROPERTY, "true");
        ClientModule module = new ClientModule() {

            @Override
            public void configure(Binder binder) {
                super.configure(binder);

                // use a noop connection to prevent startup errors...
                binder.bind(ClientConnection.class).to(MockClientConnection.class);

                ServerModule.contributeProperties(binder).putAll(properties1);
            }
        };

        Injector injector = DIBootstrap.createInjector(module);

        DataChannel channel = injector.getInstance(DataChannel.class);
        ClientChannel clientChannel = (ClientChannel) channel;
        assertTrue(clientChannel.isChannelEventsEnabled());
    }
}