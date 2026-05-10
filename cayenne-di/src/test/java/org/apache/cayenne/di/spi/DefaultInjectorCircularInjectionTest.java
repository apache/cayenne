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
package org.apache.cayenne.di.spi;

import org.apache.cayenne.di.DIRuntimeException;
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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefaultInjectorCircularInjectionTest {

    @Test
    public void fieldInjection_CircularDependency() {

        Module module = binder -> {
            binder.bind(MockInterface1.class).to(MockImplementation1_DepOn2.class);
            binder.bind(MockInterface2.class).to(MockImplementation2.class);
        };

        DefaultInjector injector = new DefaultInjector(module);

        assertThrows(DIRuntimeException.class, () -> injector.getInstance(MockInterface1.class),
                "Circular dependency is not detected.");
    }

    @Test
    public void providerInjection_CircularDependency() {

        Module module = binder -> {
            binder.bind(MockInterface1.class).to(
                    MockImplementation1_DepOn2Provider.class);
            binder.bind(MockInterface2.class).to(MockImplementation2.class);
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertEquals("MockImplementation2Name", service.getName());
    }

    @Test
    public void constructorInjection_CircularDependency() {

        Module module = binder -> {
            binder.bind(MockInterface1.class).to(
                    MockImplementation1_DepOn2Constructor.class);
            binder.bind(MockInterface2.class).to(
                    MockImplementation2_Constructor.class);
        };

        DefaultInjector injector = new DefaultInjector(module);

        assertThrows(DIRuntimeException.class, () -> injector.getInstance(MockInterface1.class),
                "Circular dependency is not detected.");
    }

    @Test
    public void constructorInjection_WithFieldInjectionDeps() {

        Module module = binder -> {
            binder.bind(MockInterface1.class).to(
                    MockImplementation1_DepOn2Constructor.class);
            binder.bind(MockInterface2.class).to(
                    MockImplementation2_I3Dependency.class);
            binder.bind(MockInterface3.class).to(MockImplementation3.class);
        };

        DefaultInjector injector = new DefaultInjector(module);

        // Should not throw - no circular dependency
        injector.getInstance(MockInterface1.class);
    }
}
