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

import static org.mockito.Mockito.mock;

import java.util.Collections;

import junit.framework.TestCase;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.remote.service.LocalConnection;

public class ClientLocalRuntimeTest extends TestCase {

    public void testDefaultConstructor() {

        Module serverModule = new Module() {

            public void configure(Binder binder) {
            }
        };

        ClientLocalRuntime runtime = new ClientLocalRuntime(
                DIBootstrap.createInjector(serverModule),
                Collections.EMPTY_MAP);
        assertEquals(2, runtime.getModules().length);

        Module m0 = runtime.getModules()[0];
        assertTrue(m0 instanceof ClientModule);
    }

    public void testGetConnection() {

        final DataContext serverContext = mock(DataContext.class);

        Module serverModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(ObjectContextFactory.class).toInstance(
                        new ObjectContextFactory() {

                            public ObjectContext createContext(DataChannel parent) {
                                return null;
                            }

                            public ObjectContext createContext() {
                                return serverContext;
                            }
                        });
            }
        };

        ClientLocalRuntime runtime = new ClientLocalRuntime(
                DIBootstrap.createInjector(serverModule),
                Collections.EMPTY_MAP);

        ClientConnection connection = runtime.getConnection();
        assertNotNull(connection);
        assertTrue(connection instanceof LocalConnection);

        LocalConnection localConnection = (LocalConnection) connection;
        assertTrue(localConnection.getChannel() instanceof ClientServerChannel);
        ClientServerChannel clientServerChannel = (ClientServerChannel) localConnection
                .getChannel();
        assertSame(serverContext, clientServerChannel.getParentChannel());
    }
}
