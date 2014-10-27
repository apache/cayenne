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
package org.apache.cayenne.configuration.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.ModuleCollection;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.tx.BaseTransaction;
import org.apache.cayenne.tx.TransactionFactory;
import org.apache.cayenne.tx.TransactionalOperation;
import org.junit.Test;

public class ServerRuntimeTest {

	@Test
	public void testPerformInTransaction() {

		final BaseTransaction tx = mock(BaseTransaction.class);
		final TransactionFactory txFactory = mock(TransactionFactory.class);
		when(txFactory.createTransaction()).thenReturn(tx);

		Module module = new Module() {

			public void configure(Binder binder) {
				binder.bind(TransactionFactory.class).toInstance(txFactory);
			}
		};

		ServerRuntime runtime = new ServerRuntime("xxxx", module);
		try {

			final Object expectedResult = new Object();
			Object result = runtime.performInTransaction(new TransactionalOperation<Object>() {
				public Object perform() {
					assertSame(tx, BaseTransaction.getThreadTransaction());
					return expectedResult;
				}
			});

			assertSame(expectedResult, result);
		} finally {
			runtime.shutdown();
		}

	}

	@Test
	public void testDefaultConstructor_SingleLocation() {
		ServerRuntime runtime = new ServerRuntime("xxxx");

		List<?> locations = runtime.getInjector().getInstance(
				Key.get(List.class, Constants.SERVER_PROJECT_LOCATIONS_LIST));

		assertEquals(Arrays.asList("xxxx"), locations);

		Collection<Module> modules = ((ModuleCollection) runtime.getModule()).getModules();
		assertEquals(1, modules.size());
		Module m0 = modules.iterator().next();
		assertTrue(m0 instanceof ServerModule);
		assertEquals("xxxx", ((ServerModule) m0).configurationLocations[0]);
	}

	@Test
	public void testDefaultConstructor_MultipleLocations() {
		ServerRuntime runtime = new ServerRuntime(new String[] { "xxxx", "yyyy" });

		List<?> locations = runtime.getInjector().getInstance(
				Key.get(List.class, Constants.SERVER_PROJECT_LOCATIONS_LIST));

		assertEquals(Arrays.asList("xxxx", "yyyy"), locations);

		assertTrue(runtime.getModule() instanceof ModuleCollection);

		Collection<Module> modules = ((ModuleCollection) runtime.getModule()).getModules();
		assertEquals(1, modules.size());
		Module m0 = modules.iterator().next();
		assertTrue(m0 instanceof ServerModule);

		assertEquals("xxxx", ((ServerModule) m0).configurationLocations[0]);
		assertEquals("yyyy", ((ServerModule) m0).configurationLocations[1]);
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

		ServerRuntime runtime = new ServerRuntime("xxxx", m1, m2);

		Collection<Module> modules = ((ModuleCollection) runtime.getModule()).getModules();
		assertEquals(3, modules.size());

		assertTrue(configured[0]);
		assertTrue(configured[1]);
	}

	@Test
	public void testGetDataChannel_CustomModule() {
		final DataChannel channel = new DataChannel() {

			public EntityResolver getEntityResolver() {
				return null;
			}

			public EventManager getEventManager() {
				return null;
			}

			public QueryResponse onQuery(ObjectContext originatingContext, Query query) {
				return null;
			}

			public GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes, int syncType) {
				return null;
			}
		};

		Module module = new Module() {

			public void configure(Binder binder) {
				binder.bind(DataChannel.class).toInstance(channel);
			}
		};

		ServerRuntime runtime = new ServerRuntime("Yuis", module);
		assertSame(channel, runtime.getChannel());
	}

	@Test
	public void testGetObjectContext_CustomModule() {
		final ObjectContext context = new DataContext();
		final ObjectContextFactory factory = new ObjectContextFactory() {

			public ObjectContext createContext(DataChannel parent) {
				return context;
			}

			public ObjectContext createContext() {
				return context;
			}
		};

		Module module = new Module() {

			public void configure(Binder binder) {
				binder.bind(ObjectContextFactory.class).toInstance(factory);
			}
		};

		ServerRuntime runtime = new ServerRuntime("mnYw", module);
		assertSame(context, runtime.newContext());
		assertSame(context, runtime.newContext());
	}
}
