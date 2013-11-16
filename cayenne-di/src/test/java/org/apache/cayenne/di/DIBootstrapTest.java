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
package org.apache.cayenne.di;

import junit.framework.TestCase;


public class DIBootstrapTest extends TestCase {

	public void testCreateInjector_Empty() {
		Injector emptyInjector = DIBootstrap.createInjector();
		assertNotNull(emptyInjector);
	}

	public void testCreateInjector_SingleModule() {
		final boolean[] configureCalled = new boolean[1];

		Module module = new Module() {
			public void configure(Binder binder) {
				configureCalled[0] = true;
			}
		};

		Injector injector = DIBootstrap.createInjector(module);
		assertNotNull(injector);

		assertTrue(configureCalled[0]);
	}

	public void testCreateInjector_MultiModule() {

		final boolean[] configureCalled = new boolean[2];

		Module module1 = new Module() {
			public void configure(Binder binder) {
				configureCalled[0] = true;
			}
		};

		Module module2 = new Module() {
			public void configure(Binder binder) {
				configureCalled[1] = true;
			}
		};

		Injector injector = DIBootstrap.createInjector(module1, module2);
		assertNotNull(injector);

		assertTrue(configureCalled[0]);
		assertTrue(configureCalled[1]);
	}
}
