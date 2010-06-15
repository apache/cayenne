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

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.BeforeScopeEnd;
import org.apache.cayenne.di.mock.MockImplementation1;
import org.apache.cayenne.di.mock.MockImplementation1_EventAnnotations;
import org.apache.cayenne.di.mock.MockImplementation1_Provider;
import org.apache.cayenne.di.mock.MockInterface1;

public class DefaultInjectorScopeTest extends TestCase {

    public void testDefaultScope_IsSingleton() {

        Module module = new Module() {

            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).to(MockImplementation1.class);
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 instance1 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance2 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance3 = injector.getInstance(MockInterface1.class);

        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNotNull(instance3);

        assertSame(instance1, instance2);
        assertSame(instance2, instance3);
    }

    public void testNoScope() {

        Module module = new Module() {

            public void configure(Binder binder) {
                binder
                        .bind(MockInterface1.class)
                        .to(MockImplementation1.class)
                        .withoutScope();
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 instance1 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance2 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance3 = injector.getInstance(MockInterface1.class);

        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNotNull(instance3);

        assertNotSame(instance1, instance2);
        assertNotSame(instance2, instance3);
        assertNotSame(instance3, instance1);
    }

    public void testSingletonScope() {

        Module module = new Module() {

            public void configure(Binder binder) {
                binder
                        .bind(MockInterface1.class)
                        .to(MockImplementation1.class)
                        .inSingletonScope();
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 instance1 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance2 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance3 = injector.getInstance(MockInterface1.class);

        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNotNull(instance3);

        assertSame(instance1, instance2);
        assertSame(instance2, instance3);
    }

    public void testSingletonScope_AnnotatedEvents() {

        MockImplementation1_EventAnnotations.reset();

        Module module = new Module() {

            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).to(
                        MockImplementation1_EventAnnotations.class).inSingletonScope();
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 instance1 = injector.getInstance(MockInterface1.class);
        assertEquals("XuI", instance1.getName());

        assertFalse(MockImplementation1_EventAnnotations.shutdown1);
        assertFalse(MockImplementation1_EventAnnotations.shutdown2);
        assertFalse(MockImplementation1_EventAnnotations.shutdown3);

        injector.getSingletonScope().postScopeEvent(BeforeScopeEnd.class);

        assertTrue(MockImplementation1_EventAnnotations.shutdown1);
        assertTrue(MockImplementation1_EventAnnotations.shutdown2);
        assertTrue(MockImplementation1_EventAnnotations.shutdown3);
    }

    public void testSingletonScope_WithProvider() {

        Module module = new Module() {

            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).toProvider(
                        MockImplementation1_Provider.class).inSingletonScope();
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 instance1 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance2 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance3 = injector.getInstance(MockInterface1.class);

        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNotNull(instance3);

        assertSame(instance1, instance2);
        assertSame(instance2, instance3);
    }

    public void testNoScope_WithProvider() {

        Module module = new Module() {

            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).toProvider(
                        MockImplementation1_Provider.class).withoutScope();
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 instance1 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance2 = injector.getInstance(MockInterface1.class);
        MockInterface1 instance3 = injector.getInstance(MockInterface1.class);

        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNotNull(instance3);

        assertNotSame(instance1, instance2);
        assertNotSame(instance2, instance3);
    }
}
