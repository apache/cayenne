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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.ModuleCollection;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.junit.Test;

public class ServerRuntimeBuilderTest {

	@Test
	public void test_NoLocation() {

		// this is meaningless (no DataSource), but should work...
		ServerRuntime runtime = new ServerRuntimeBuilder().build();

		List<?> locations = runtime.getInjector().getInstance(
				Key.get(List.class, Constants.SERVER_PROJECT_LOCATIONS_LIST));

		assertEquals(Arrays.asList(), locations);
		assertTrue(runtime.getModule() instanceof ModuleCollection);

		Collection<Module> modules = ((ModuleCollection) runtime.getModule()).getModules();
		assertEquals(1, modules.size());
		assertTrue(modules.iterator().next() instanceof ServerModule);
	}

	@Test
	public void test_SingleLocation() {

		ServerRuntime runtime = new ServerRuntimeBuilder("xxxx").build();

		List<?> locations = runtime.getInjector().getInstance(
				Key.get(List.class, Constants.SERVER_PROJECT_LOCATIONS_LIST));

		assertEquals(Arrays.asList("xxxx"), locations);

		Collection<Module> modules = ((ModuleCollection) runtime.getModule()).getModules();
		assertEquals(1, modules.size());
		assertTrue(modules.iterator().next() instanceof ServerModule);

	}

	@Test
	public void test_MultipleLocations() {

		ServerRuntime runtime = new ServerRuntimeBuilder("xxxx").addConfig("yyyy").build();

		List<?> locations = runtime.getInjector().getInstance(
				Key.get(List.class, Constants.SERVER_PROJECT_LOCATIONS_LIST));

		assertEquals(Arrays.asList("xxxx", "yyyy"), locations);

		Collection<Module> modules = ((ModuleCollection) runtime.getModule()).getModules();
		assertEquals(1, modules.size());
		assertTrue(modules.iterator().next() instanceof ServerModule);
	}

	@Test
	public void test_ExtraModules() {

		Module m = mock(Module.class);

		ServerRuntime runtime = new ServerRuntimeBuilder("xxxx").addModule(m).build();

		Collection<Module> modules = ((ModuleCollection) runtime.getModule()).getModules();
		assertEquals(2, modules.size());

		Iterator<Module> it = modules.iterator();

		assertTrue(it.next() instanceof ServerModule);
		assertSame(m, it.next());
	}

}
