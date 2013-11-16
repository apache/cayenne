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
package org.apache.cayenne.di.spi;

import junit.framework.TestCase;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.mock.MockImplementation1_DepOn2;
import org.apache.cayenne.di.mock.MockImplementation1_DepOn2Constructor;
import org.apache.cayenne.di.mock.MockImplementation1_DepOn2Provider;
import org.apache.cayenne.di.mock.MockImplementation2;
import org.apache.cayenne.di.mock.MockImplementation2_Constructor;
import org.apache.cayenne.di.mock.MockImplementation2_I3Dependency;
import org.apache.cayenne.di.mock.MockImplementation3;
import org.apache.cayenne.di.mock.MockInterface1;
import org.apache.cayenne.di.mock.MockInterface2;
import org.apache.cayenne.di.mock.MockInterface3;

public class DefaultInjectorCircularInjectionTest extends TestCase {

    public void testFieldInjection_CircularDependency() {

        Module module = new Module() {

            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).to(MockImplementation1_DepOn2.class);
                binder.bind(MockInterface2.class).to(MockImplementation2.class);
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        try {
            injector.getInstance(MockInterface1.class);
            fail("Circular dependency is not detected.");
        }
        catch (ConfigurationException e) {
            // expected
        }
        catch (StackOverflowError e) {
            fail("Circular dependency is not detected, causing stack overflow");
        }
    }

    public void testProviderInjection_CircularDependency() {

        Module module = new Module() {

            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).to(
                        MockImplementation1_DepOn2Provider.class);
                binder.bind(MockInterface2.class).to(MockImplementation2.class);
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertEquals("MockImplementation2Name", service.getName());
    }

    public void testConstructorInjection_CircularDependency() {

        Module module = new Module() {

            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).to(
                        MockImplementation1_DepOn2Constructor.class);
                binder.bind(MockInterface2.class).to(
                        MockImplementation2_Constructor.class);
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        try {
            injector.getInstance(MockInterface1.class);
            fail("Circular dependency is not detected.");
        }
        catch (ConfigurationException e) {
            // expected
        }
        catch (StackOverflowError e) {
            fail("Circular dependency is not detected, causing stack overflow");
        }
    }

    public void testConstructorInjection_WithFieldInjectionDeps() {

        Module module = new Module() {

            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).to(
                        MockImplementation1_DepOn2Constructor.class);
                binder.bind(MockInterface2.class).to(
                        MockImplementation2_I3Dependency.class);
                binder.bind(MockInterface3.class).to(MockImplementation3.class);
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        try {
            injector.getInstance(MockInterface1.class);
        }
        catch (ConfigurationException e) {
            fail("Circular dependency is detected incorrectly: " + e.getMessage());
        }
    }
}
