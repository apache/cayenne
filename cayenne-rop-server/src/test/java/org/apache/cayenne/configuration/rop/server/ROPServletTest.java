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
package org.apache.cayenne.configuration.rop.server;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.mockrunner.mock.web.MockServletConfig;
import com.mockrunner.mock.web.MockServletContext;
import org.apache.cayenne.configuration.CayenneRuntime;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.configuration.web.RequestHandler;
import org.apache.cayenne.configuration.web.WebModule;
import org.apache.cayenne.configuration.web.WebUtil;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.rop.ROPServlet;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ROPServletTest {

	private CayenneRuntime runtime;

	@After
	public void shutdownRuntime() {
		if(runtime != null) {
			runtime.shutdown();
		}
	}

	@Test
	public void testInitWithServletName() throws Exception {

		MockServletConfig config = new MockServletConfig();
		config.setServletName("cayenne-org.apache.cayenne.configuration.rop.server.test-config");

		MockServletContext context = new MockServletContext();
		config.setServletContext(context);

		ROPServlet servlet = new ROPServlet();

		assertNull(WebUtil.getCayenneRuntime(context));
		servlet.init(config);

		runtime = WebUtil.getCayenneRuntime(context);
		assertNotNull(runtime);

		List<String> locations = runtime.getInjector().getInstance(
				Key.getListOf(String.class, Constants.SERVER_PROJECT_LOCATIONS_LIST));
		assertEquals(Arrays.asList("cayenne-org.apache.cayenne.configuration.rop.server.test-config.xml"), locations);
	}

	@Test
	public void testInitWithLocation() throws Exception {

		String location = "cayenne-org.apache.cayenne.configuration.rop.server.test-config.xml";
		MockServletConfig config = new MockServletConfig();
		config.setServletName("abc");
		config.setInitParameter("configuration-location", location);

		MockServletContext context = new MockServletContext();
		config.setServletContext(context);

		ROPServlet servlet = new ROPServlet();
		servlet.init(config);

		runtime = WebUtil.getCayenneRuntime(context);
		assertNotNull(runtime);
		List<String> locations = runtime.getInjector().getInstance(
				Key.getListOf(String.class, Constants.SERVER_PROJECT_LOCATIONS_LIST));

		assertEquals(Arrays.asList(location), locations);
	}

	@Test
	public void testInitWithStandardModules() throws Exception {

		String name = "cayenne-org.apache.cayenne.configuration.rop.server.test-config";

		MockServletConfig config = new MockServletConfig();
		config.setServletName(name);

		MockServletContext context = new MockServletContext();
		config.setServletContext(context);

		ROPServlet servlet = new ROPServlet();
		servlet.init(config);

		runtime = WebUtil.getCayenneRuntime(context);
		assertNotNull(runtime);

		List<String> locations = runtime.getInjector().getInstance(
				Key.getListOf(String.class, Constants.SERVER_PROJECT_LOCATIONS_LIST));

		assertEquals(Arrays.asList(name + ".xml"), locations);

		Collection<Module> modules = runtime.getModules();
		assertEquals(5, modules.size());
		Object[] marray = modules.toArray();

		if(marray[0] instanceof ServerModule){
			assertTrue(marray[1] instanceof WebModule);
		}
		else{
			assertTrue(marray[0] instanceof WebModule);
		}
		assertTrue(marray[2] instanceof ROPServerModule);
	}

	@Test
	public void testInitWithExtraModules() throws Exception {

		String name = "cayenne-org.apache.cayenne.configuration.rop.server.test-config";

		MockServletConfig config = new MockServletConfig();
		config.setServletName(name);
		config.setInitParameter("extra-modules", MockModule1.class.getName() + "," + MockModule2.class.getName());

		MockServletContext context = new MockServletContext();
		config.setServletContext(context);

		ROPServlet servlet = new ROPServlet();
		servlet.init(config);

		runtime = WebUtil.getCayenneRuntime(context);
		assertNotNull(runtime);

		Collection<Module> modules = runtime.getModules();
		assertEquals(7, modules.size());

		Object[] marray = modules.toArray();
		if(marray[0] instanceof ServerModule){
			assertTrue(marray[1] instanceof WebModule);
		}
		else{
			assertTrue(marray[0] instanceof WebModule);
		}
		assertTrue(marray[2] instanceof ROPServerModule);
		assertTrue(marray[3] instanceof MockModule1);
		assertTrue(marray[4] instanceof MockModule2);

		RequestHandler handler = runtime.getInjector().getInstance(RequestHandler.class);
		assertTrue(handler instanceof MockRequestHandler);
	}

	@Test
	public void testInitHessianService() throws Exception {

		MockServletConfig config = new MockServletConfig();
		config.setServletName("abc");

		MockServletContext context = new MockServletContext();
		config.setServletContext(context);
		config.setInitParameter("extra-modules", ROPHessianServlet_ConfigModule.class.getName());

		ROPServlet servlet = new ROPServlet();

		servlet.init(config);
		runtime = WebUtil.getCayenneRuntime(context);
		Collection<Module> modules = runtime.getModules();
		assertEquals(6, modules.size());

		Object[] marray = modules.toArray();

		assertTrue(marray[3] instanceof ROPHessianServlet_ConfigModule);

		// TODO: mock servlet request to check that the right service instance
		// is invoked
	}
}
