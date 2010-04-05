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
package org.apache.cayenne.configuration.web;

import org.apache.cayenne.configuration.CayenneRuntime;
import org.apache.cayenne.configuration.server.CayenneServerModule;

import com.mockrunner.mock.web.MockFilterConfig;
import com.mockrunner.mock.web.MockServletContext;

import junit.framework.TestCase;

public class CayenneFilterTest extends TestCase {

    public void testInitWithFilterName() throws Exception {

        MockFilterConfig config = new MockFilterConfig();
        config.setFilterName("abc");

        MockServletContext context = new MockServletContext();
        config.setupServletContext(context);

        CayenneFilter filter = new CayenneFilter();

        assertNull(WebUtil.getCayenneRuntime(context));
        filter.init(config);

        CayenneRuntime runtime = WebUtil.getCayenneRuntime(context);
        assertNotNull(runtime);

        assertEquals("abc", runtime.getName());
    }

    public void testInitWithRuntimeName() throws Exception {

        MockFilterConfig config = new MockFilterConfig();
        config.setFilterName("abc");
        config.setInitParameter(CayenneFilter.RUNTIME_NAME_PARAMETER, "xyz");

        MockServletContext context = new MockServletContext();
        config.setupServletContext(context);

        CayenneFilter filter = new CayenneFilter();
        filter.init(config);

        CayenneRuntime runtime = WebUtil.getCayenneRuntime(context);
        assertNotNull(runtime);

        assertEquals("xyz", runtime.getName());
    }

    public void testInitWithStarndardModules() throws Exception {

        MockFilterConfig config = new MockFilterConfig();
        config.setFilterName("abc");

        MockServletContext context = new MockServletContext();
        config.setupServletContext(context);

        CayenneFilter filter = new CayenneFilter();

        assertNull(WebUtil.getCayenneRuntime(context));
        filter.init(config);

        CayenneRuntime runtime = WebUtil.getCayenneRuntime(context);
        assertNotNull(runtime);

        assertEquals("abc", runtime.getName());
        assertEquals(2, runtime.getModules().length);
        assertTrue(runtime.getModules()[0] instanceof CayenneServerModule);
        assertTrue(runtime.getModules()[1] instanceof CayenneWebModule);

        RequestHandler handler = runtime.getInjector().getInstance(RequestHandler.class);
        assertTrue(handler instanceof DefaultRequestHandler);
    }

    public void testInitWithExtraModules() throws Exception {

        MockFilterConfig config = new MockFilterConfig();
        config.setFilterName("abc");
        config.setInitParameter(CayenneFilter.EXTRA_MODULES_PARAMETER, MockModule1.class
                .getName()
                + ","
                + MockModule2.class.getName());

        MockServletContext context = new MockServletContext();
        config.setupServletContext(context);

        CayenneFilter filter = new CayenneFilter();
        filter.init(config);

        CayenneRuntime runtime = WebUtil.getCayenneRuntime(context);
        assertNotNull(runtime);

        assertEquals(4, runtime.getModules().length);

        assertTrue(runtime.getModules()[0] instanceof CayenneServerModule);
        assertTrue(runtime.getModules()[1] instanceof CayenneWebModule);
        assertTrue(runtime.getModules()[2] instanceof MockModule1);
        assertTrue(runtime.getModules()[3] instanceof MockModule2);

        RequestHandler handler = runtime.getInjector().getInstance(RequestHandler.class);
        assertTrue(handler instanceof MockRequestHandler);
    }
}
