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

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.event.DefaultEventManager;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.remote.ClientChannel;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.remote.MockClientConnection;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("deprecation")
public class ClientRuntimeTest {

    @Test
	public void testGetObjectContext() {

		Map<String, String> properties = new HashMap<>();
		ClientModule extraModule = new ClientModule() {

			@Override
			public void configure(Binder binder) {
				super.configure(binder);

				// use a noop connection to prevent startup errors...
				binder.bind(ClientConnection.class).to(MockClientConnection.class);
			}
		};

		ClientRuntime runtime = ClientRuntime.builder()
								.properties(properties)
								.addModule(extraModule)
								.build();

		ObjectContext context = runtime.newContext();
		assertNotNull(context);
		assertTrue(context instanceof CayenneContext);
		assertNotSame("ObjectContext must not be a singleton", context, runtime.newContext());

		CayenneContext clientContext = (CayenneContext) context;
		assertNotNull(clientContext.getChannel());
		assertSame(runtime.getChannel(), clientContext.getChannel());
	}

    @Test
	public void testGetDataChannel() {

		Map<String, String> properties = new HashMap<>();

		Module extraModule = binder ->
            // use a noop connection to prevent hessian startup errors...
            binder.bind(ClientConnection.class).to(MockClientConnection.class);


		ClientRuntime runtime = ClientRuntime.builder()
								.properties(properties)
								.addModule(extraModule)
								.build();

		DataChannel channel = runtime.getChannel();
		assertNotNull(channel);
		assertTrue(channel instanceof ClientChannel);
	}

    @Test
	public void testShutdown() throws Exception {

		Map<String, String> properties = new HashMap<>();
		ClientRuntime runtime = ClientRuntime.builder()
								.properties(properties)
								.addModule(binder -> ServerModule.contributeProperties(binder)
										.put(Constants.SERVER_CONTEXTS_SYNC_PROPERTY, String.valueOf(true)))
								.build();

		// make sure objects to be shut down are resolved

		EventManager em = runtime.getInjector().getInstance(EventManager.class);
		assertNotNull(em);
		assertTrue(em instanceof DefaultEventManager);
		assertFalse(((DefaultEventManager) em).isStopped());

		runtime.getInjector().shutdown();

		assertTrue(((DefaultEventManager) em).isStopped());
	}
}
