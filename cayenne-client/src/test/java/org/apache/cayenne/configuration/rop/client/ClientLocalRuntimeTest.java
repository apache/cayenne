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

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.remote.service.LocalConnection;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class ClientLocalRuntimeTest {

    @Test
	public void testDefaultConstructor() {

		Module serverModule = binder -> {
        };

		ClientRuntime runtime = ClientRuntime.builder()
				.disableModulesAutoLoading()
				.local(DIBootstrap.createInjector(serverModule))
				.build();
		Collection<Module> cmodules = runtime.getModules();
		assertEquals(2, cmodules.size());

		assertTrue(cmodules.toArray()[0] instanceof ClientModule);
	}

    @Test
	public void testGetConnection() {

		final DataContext serverContext = mock(DataContext.class);

		Module serverModule = binder -> binder.bind(ObjectContextFactory.class).toInstance(new ObjectContextFactory() {

            public ObjectContext createContext(DataChannel parent) {
                return null;
            }

            public ObjectContext createContext() {
                return serverContext;
            }
        });

		ClientRuntime runtime = ClientRuntime.builder()
				.local(DIBootstrap.createInjector(serverModule))
				.build();

		ClientConnection connection = runtime.getConnection();
		assertNotNull(connection);
		assertTrue(connection instanceof LocalConnection);

		LocalConnection localConnection = (LocalConnection) connection;
		assertTrue(localConnection.getChannel() instanceof ClientServerChannel);
		ClientServerChannel clientServerChannel = (ClientServerChannel) localConnection.getChannel();
		assertSame(serverContext, clientServerChannel.getParentChannel());
	}
}
