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

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.event.DefaultEventManager;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.remote.ClientChannel;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.remote.MockClientConnection;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("deprecation")
public class ClientRuntimeTest {

    @Test
	public void testDefaultConstructor() {
		ClientRuntime runtime = new ClientRuntime(Collections.<String, String> emptyMap());

		Collection<Module> modules = runtime.getModules();
		assertEquals(1, modules.size());
		Object[] marray = modules.toArray();

		assertTrue(marray[0] instanceof ClientModule);
	}

    @Test
	public void testConstructor_Modules() {

		final boolean[] configured = new boolean[2];

		Module m1 = new Module() {

			public void configure(Binder binder) {
				configured[0] = true;
			}
		};

		Module m2 = new Module() {

			public void configure(Binder binder) {
				configured[1] = true;
			}
		};

		Map<String, String> properties = new HashMap<>();

		ClientRuntime runtime = new ClientRuntime(properties, m1, m2);
		Collection<Module> modules = runtime.getModules();
		assertEquals(3, modules.size());

		assertTrue(configured[0]);
		assertTrue(configured[1]);
	}

    @Test
	public void testConstructor_ModulesCollection() {

		final boolean[] configured = new boolean[2];

		Collection<Module> modules = new ArrayList<Module>();

		modules.add(new Module() {

			public void configure(Binder binder) {
				configured[0] = true;
			}
		});

		modules.add(new Module() {

			public void configure(Binder binder) {
				configured[1] = true;
			}
		});

		Map<String, String> properties = new HashMap<>();

		ClientRuntime runtime = new ClientRuntime(properties, modules);
		Collection<Module> cmodules = runtime.getModules();
		assertEquals(3, cmodules.size());

		assertTrue(configured[0]);
		assertTrue(configured[1]);
	}

    @Test
	public void testGetObjectContext() {

		Map<String, String> properties = new HashMap<>();
		ClientModule extraModule = new ClientModule(properties) {

			@Override
			public void configure(Binder binder) {
				super.configure(binder);

				// use a noop connection to prevent startup errors...
				binder.bind(ClientConnection.class).to(MockClientConnection.class);
			}
		};

		ClientRuntime runtime = new ClientRuntime(properties, extraModule);

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

		Module extraModule = new Module() {

			public void configure(Binder binder) {

				// use a noop connection to prevent hessian startup errors...
				binder.bind(ClientConnection.class).to(MockClientConnection.class);
			}
		};

		ClientRuntime runtime = new ClientRuntime(properties, extraModule);

		DataChannel channel = runtime.getChannel();
		assertNotNull(channel);
		assertTrue(channel instanceof ClientChannel);
	}

    @Test
	public void testShutdown() throws Exception {

		Map<String, String> properties = new HashMap<>();
		ClientRuntime runtime = new ClientRuntime(properties);

		// make sure objects to be shut down are resolved

		EventManager em = runtime.getInjector().getInstance(EventManager.class);
		assertNotNull(em);
		assertTrue(em instanceof DefaultEventManager);
		assertFalse(((DefaultEventManager) em).isStopped());

		runtime.getInjector().shutdown();

		assertTrue(((DefaultEventManager) em).isStopped());
	}
}
