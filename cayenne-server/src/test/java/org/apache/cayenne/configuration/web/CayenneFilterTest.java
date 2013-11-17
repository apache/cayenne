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

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.cayenne.configuration.CayenneRuntime;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.di.Key;

import com.mockrunner.mock.web.MockFilterChain;
import com.mockrunner.mock.web.MockFilterConfig;
import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import com.mockrunner.mock.web.MockServletContext;

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

        List<?> locations = runtime.getInjector().getInstance(
                Key.get(List.class, Constants.SERVER_PROJECT_LOCATIONS_LIST));

        assertEquals(Arrays.asList("abc.xml"), locations);
    }

    public void testInitWithLocation() throws Exception {

        MockFilterConfig config = new MockFilterConfig();
        config.setFilterName("abc");
        config.setInitParameter(WebConfiguration.CONFIGURATION_LOCATION_PARAMETER, "xyz");

        MockServletContext context = new MockServletContext();
        config.setupServletContext(context);

        CayenneFilter filter = new CayenneFilter();
        filter.init(config);

        CayenneRuntime runtime = WebUtil.getCayenneRuntime(context);
        assertNotNull(runtime);
        List<?> locations = runtime.getInjector().getInstance(
                Key.get(List.class, Constants.SERVER_PROJECT_LOCATIONS_LIST));

        assertEquals(Arrays.asList("xyz"), locations);
    }

    public void testInitWithStandardModules() throws Exception {

        MockFilterConfig config = new MockFilterConfig();
        config.setFilterName("cayenne-abc");

        MockServletContext context = new MockServletContext();
        config.setupServletContext(context);

        CayenneFilter filter = new CayenneFilter();

        assertNull(WebUtil.getCayenneRuntime(context));
        filter.init(config);

        CayenneRuntime runtime = WebUtil.getCayenneRuntime(context);
        assertNotNull(runtime);
        List<?> locations = runtime.getInjector().getInstance(
                Key.get(List.class, Constants.SERVER_PROJECT_LOCATIONS_LIST));

        assertEquals(Arrays.asList("cayenne-abc.xml"), locations);
        assertEquals(2, runtime.getModules().length);
        assertTrue(runtime.getModules()[0] instanceof ServerModule);
        assertTrue(runtime.getModules()[1] instanceof WebModule);

        RequestHandler handler = runtime.getInjector().getInstance(RequestHandler.class);
        assertTrue(handler instanceof SessionContextRequestHandler);
    }

    public void testInitWithExtraModules() throws Exception {

        MockFilterConfig config = new MockFilterConfig();
        config.setFilterName("abc");
        config.setInitParameter(
                WebConfiguration.EXTRA_MODULES_PARAMETER,
                MockModule1.class.getName() + "," + MockModule2.class.getName());

        MockServletContext context = new MockServletContext();
        config.setupServletContext(context);

        CayenneFilter filter = new CayenneFilter();
        filter.init(config);

        CayenneRuntime runtime = WebUtil.getCayenneRuntime(context);
        assertNotNull(runtime);

        assertEquals(4, runtime.getModules().length);

        assertTrue(runtime.getModules()[0] instanceof ServerModule);
        assertTrue(runtime.getModules()[1] instanceof WebModule);
        assertTrue(runtime.getModules()[2] instanceof MockModule1);
        assertTrue(runtime.getModules()[3] instanceof MockModule2);

        RequestHandler handler = runtime.getInjector().getInstance(RequestHandler.class);
        assertTrue(handler instanceof MockRequestHandler);
    }

    public void testDoFilter() throws Exception {
        MockFilterConfig config = new MockFilterConfig();
        config.setFilterName("abc");
        config.setInitParameter(
                WebConfiguration.EXTRA_MODULES_PARAMETER,
                CayenneFilter_DispatchModule.class.getName());

        MockServletContext context = new MockServletContext();
        config.setupServletContext(context);

        CayenneFilter filter = new CayenneFilter();
        filter.init(config);

        CayenneRuntime runtime = WebUtil.getCayenneRuntime(context);
        CayenneFilter_DispatchRequestHandler handler = (CayenneFilter_DispatchRequestHandler) runtime
                .getInjector()
                .getInstance(RequestHandler.class);

        assertEquals(0, handler.getStarted());
        assertEquals(0, handler.getEnded());

        filter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                new MockFilterChain());
        assertEquals(1, handler.getStarted());
        assertEquals(1, handler.getEnded());

        filter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                new MockFilterChain());
        assertEquals(2, handler.getStarted());
        assertEquals(2, handler.getEnded());
    }
}
