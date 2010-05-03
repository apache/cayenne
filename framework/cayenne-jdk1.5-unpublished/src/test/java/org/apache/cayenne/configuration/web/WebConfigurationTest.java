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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;

import com.mockrunner.mock.web.MockFilterConfig;
import com.mockrunner.mock.web.MockServletConfig;

public class WebConfigurationTest extends TestCase {

    public void testFilterCreateModules_Standard() throws Exception {

        MockFilterConfig config = new MockFilterConfig();
        WebConfiguration configuration = new WebConfiguration(config);

        Module m1 = new Module() {

            public void configure(Binder binder) {
            }
        };

        Module m2 = new Module() {

            public void configure(Binder binder) {
            }
        };

        Collection<Module> modules = configuration.createModules(m1, m2);
        assertEquals(2, modules.size());

        Iterator<Module> it = modules.iterator();
        assertSame(m1, it.next());
        assertSame(m2, it.next());
    }

    public void testFilterCreateModules_Extra() throws Exception {

        MockFilterConfig config = new MockFilterConfig();
        String exra = String.format(
                "%s, \n%s",
                MockModule1.class.getName(),
                MockModule2.class.getName());
        config.setInitParameter(WebConfiguration.EXTRA_MODULES_PARAMETER, exra);

        WebConfiguration configuration = new WebConfiguration(config);

        Module m1 = new Module() {

            public void configure(Binder binder) {
            }
        };

        Module m2 = new Module() {

            public void configure(Binder binder) {
            }
        };

        Collection<Module> modules = configuration.createModules(m1, m2);
        assertEquals(4, modules.size());

        Iterator<Module> it = modules.iterator();
        assertSame(m1, it.next());
        assertSame(m2, it.next());
        assertTrue(it.next() instanceof MockModule1);
        assertTrue(it.next() instanceof MockModule2);
    }

    public void testServletCreateModules_Extra() throws Exception {

        MockServletConfig config = new MockServletConfig();
        String exra = String.format(
                "%s, \n%s",
                MockModule1.class.getName(),
                MockModule2.class.getName());
        config.setInitParameter(WebConfiguration.EXTRA_MODULES_PARAMETER, exra);

        WebConfiguration configuration = new WebConfiguration(config);

        Module m1 = new Module() {

            public void configure(Binder binder) {
            }
        };

        Module m2 = new Module() {

            public void configure(Binder binder) {
            }
        };

        Collection<Module> modules = configuration.createModules(m1, m2);
        assertEquals(4, modules.size());

        Iterator<Module> it = modules.iterator();
        assertSame(m1, it.next());
        assertSame(m2, it.next());
        assertTrue(it.next() instanceof MockModule1);
        assertTrue(it.next() instanceof MockModule2);
    }

    public void testFilterConfigurationLocation_Name() {
        MockFilterConfig config1 = new MockFilterConfig();
        config1.setFilterName("cayenne-x");

        WebConfiguration configuration1 = new WebConfiguration(config1);
        assertEquals("cayenne-x.xml", configuration1.getConfigurationLocation());

        MockFilterConfig config2 = new MockFilterConfig();
        config2.setFilterName("cayenne-y.xml");

        WebConfiguration configuration2 = new WebConfiguration(config2);
        assertEquals("cayenne-y.xml", configuration2.getConfigurationLocation());

        MockFilterConfig config3 = new MockFilterConfig();
        config3.setFilterName("a/b/c/cayenne-z.xml");

        WebConfiguration configuration3 = new WebConfiguration(config3);
        assertEquals("a/b/c/cayenne-z.xml", configuration3.getConfigurationLocation());
    }

    public void testServletConfigurationLocation_Name() {
        MockServletConfig config1 = new MockServletConfig();
        config1.setServletName("cayenne-x");

        WebConfiguration configuration1 = new WebConfiguration(config1);
        assertEquals("cayenne-x.xml", configuration1.getConfigurationLocation());

        MockServletConfig config2 = new MockServletConfig();
        config2.setServletName("cayenne-y.xml");

        WebConfiguration configuration2 = new WebConfiguration(config2);
        assertEquals("cayenne-y.xml", configuration2.getConfigurationLocation());

        MockServletConfig config3 = new MockServletConfig();
        config3.setServletName("a/b/c/cayenne-z.xml");

        WebConfiguration configuration3 = new WebConfiguration(config3);
        assertEquals("a/b/c/cayenne-z.xml", configuration3.getConfigurationLocation());
    }

    public void testFilterConfigurationLocation_Parameter() {
        MockFilterConfig config1 = new MockFilterConfig();
        config1.setFilterName("cayenne-x");
        config1.setInitParameter(
                WebConfiguration.CONFIGURATION_LOCATION_PARAMETER,
                "cayenne-y.xml");

        WebConfiguration configuration1 = new WebConfiguration(config1);
        assertEquals("cayenne-y.xml", configuration1.getConfigurationLocation());
    }

    public void testServletConfigurationLocation_Parameter() {
        MockServletConfig config1 = new MockServletConfig();
        config1.setServletName("cayenne-x");
        config1.setInitParameter(
                WebConfiguration.CONFIGURATION_LOCATION_PARAMETER,
                "cayenne-y.xml");

        WebConfiguration configuration1 = new WebConfiguration(config1);
        assertEquals("cayenne-y.xml", configuration1.getConfigurationLocation());
    }

    public void testFilterParameters() {
        MockFilterConfig config1 = new MockFilterConfig();
        config1.setFilterName("cayenne-x");
        config1.setInitParameter(
                WebConfiguration.CONFIGURATION_LOCATION_PARAMETER,
                "cayenne-y.xml");
        config1.setInitParameter("test", "xxx");

        WebConfiguration configuration1 = new WebConfiguration(config1);
        Map<String, String> parameters = configuration1.getParameters();
        assertNotSame(parameters, configuration1.getParameters());
        assertEquals(parameters, configuration1.getParameters());

        assertEquals(2, parameters.size());
        assertEquals("cayenne-y.xml", parameters
                .get(WebConfiguration.CONFIGURATION_LOCATION_PARAMETER));
        assertEquals("xxx", parameters.get("test"));
    }

    public void testFilterOtherParameters() {
        MockFilterConfig config1 = new MockFilterConfig();
        config1.setFilterName("cayenne-x");
        config1.setInitParameter(
                WebConfiguration.CONFIGURATION_LOCATION_PARAMETER,
                "cayenne-y.xml");
        config1.setInitParameter(WebConfiguration.EXTRA_MODULES_PARAMETER, "M1,M2");
        config1.setInitParameter("test", "xxx");

        WebConfiguration configuration1 = new WebConfiguration(config1);
        Map<String, String> parameters = configuration1.getOtherParameters();
        assertNotSame(parameters, configuration1.getOtherParameters());
        assertEquals(parameters, configuration1.getOtherParameters());

        assertEquals(1, parameters.size());
        assertEquals("xxx", parameters.get("test"));
    }
}
