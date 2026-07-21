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
package org.apache.cayenne.runtime;

import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.runtime.CoreModule;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class CayenneRuntimeBuilderTest {

	private CayenneRuntime runtime;

	@AfterEach
	public void stopRuntime() {

		// even though we don't supply real configs here, we sometimes access
		// DataDomain, and this starts EventManager threads that need to be
		// shutdown
		if (runtime != null) {
			runtime.shutdown();
		}
	}

	@Test
	public void noLocation() {

		// this is meaningless (no DataSource), but should work...
		runtime = new CayenneRuntimeBuilder(null).build();

		List<String> locations = runtime.getInjector().getInstance(
				Key.getListOf(String.class, Constants.PROJECT_LOCATIONS_LIST));

		assertEquals(List.of(), locations);

		assertEquals(2, runtime.modules.size());
		assertInstanceOf(CoreModule.class, runtime.modules.iterator().next());
	}

	@Test
	public void singleLocation() {

		runtime = new CayenneRuntimeBuilder(null).addConfig("xxxx").build();

		List<String> locations = runtime.getInjector().getInstance(
				Key.getListOf(String.class, Constants.PROJECT_LOCATIONS_LIST));

		assertEquals(List.of("xxxx"), locations);

		assertEquals(2, runtime.modules.size());
		assertInstanceOf(CoreModule.class, runtime.modules.iterator().next());

	}

	@Test
	public void multipleLocations() {

		runtime = new CayenneRuntimeBuilder(null).addConfigs("xxxx", "yyyy").build();

		List<String> locations = runtime.getInjector().getInstance(
				Key.getListOf(String.class, Constants.PROJECT_LOCATIONS_LIST));

		assertEquals(Arrays.asList("xxxx", "yyyy"), locations);

		assertEquals(3, runtime.modules.size());
		assertInstanceOf(CoreModule.class, runtime.modules.iterator().next());
	}

	@Test
	public void extraModules() {

		Module m = mock(Module.class);

		runtime = new CayenneRuntimeBuilder(null).addModule(m).build();

		assertEquals(3, runtime.modules.size());
		Module[] array = runtime.modules.toArray(new Module[3]);
		assertInstanceOf(CoreModule.class, array[0]);
		assertSame(m, array[1]);
	}

	@Test
	public void unnamedDomain_NoLocation() {
		runtime = new CayenneRuntimeBuilder(null).build();
		assertEquals("cayenne", runtime.getDataDomain().getName());
	}

	@Test
	public void namedDomain_NoLocation() {
		runtime = new CayenneRuntimeBuilder("myd").build();
		assertEquals("myd", runtime.getDataDomain().getName());
	}
}
