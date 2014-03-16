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
import org.apache.cayenne.di.mock.MockImplementation1;
import org.apache.cayenne.di.mock.MockInterface1;
import org.apache.cayenne.di.mock.MockInterface1_Decorator1;
import org.apache.cayenne.di.mock.MockInterface1_Decorator2;
import org.apache.cayenne.di.mock.MockInterface1_Decorator3;

public class DefaultInjectorDecorationTest extends TestCase {

    public void testSingleDecorator_After() {

        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).to(MockImplementation1.class);
                binder.decorate(MockInterface1.class).after(MockInterface1_Decorator1.class);
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertNotNull(service);
        assertEquals("[MyName]", service.getName());
    }

    public void testSingleDecorator_Before() {

        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).to(MockImplementation1.class);
                binder.decorate(MockInterface1.class).before(MockInterface1_Decorator1.class);
            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertNotNull(service);
        assertEquals("[MyName]", service.getName());
    }

    public void testDecoratorChain() {

        Module module = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(MockInterface1.class).to(MockImplementation1.class);
                binder.decorate(MockInterface1.class).before(MockInterface1_Decorator1.class);
                binder.decorate(MockInterface1.class).before(MockInterface1_Decorator2.class);
                binder.decorate(MockInterface1.class).after(MockInterface1_Decorator3.class);

            }
        };

        DefaultInjector injector = new DefaultInjector(module);

        MockInterface1 service = injector.getInstance(MockInterface1.class);
        assertNotNull(service);
        assertEquals("<[{MyName}]>", service.getName());
    }

}
