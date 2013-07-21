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
package org.apache.cayenne.unit.di;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultScope;

public class DICaseSelfTest extends DICase {

    private static final Injector injector;

    static {
        Module selfTestModule = new Module() {

            public void configure(Binder binder) {
                DefaultScope testScope = new DefaultScope();

                binder.bind(UnitTestLifecycleManager.class).toInstance(
                        new DefaultUnitTestLifecycleManager(testScope));

                binder.bind(Key.get(Object.class, "test-scope")).to(Object.class).in(
                        testScope);
                binder
                        .bind(Key.get(Object.class, "singleton-scope"))
                        .to(Object.class)
                        .inSingletonScope();
            }
        };

        injector = DIBootstrap.createInjector(selfTestModule);
    }

    @Inject("test-scope")
    protected Object testScoped;

    @Inject("singleton-scope")
    protected Object singletonScoped;

    @Override
    protected Injector getUnitTestInjector() {
        return injector;
    }

    public void testInjection() throws Exception {

        Object testScoped = this.testScoped;
        assertNotNull(testScoped);

        Object singletonScoped = this.singletonScoped;
        assertNotNull(singletonScoped);

        tearDown();
        setUp();

        assertNotSame(testScoped, this.testScoped);
        assertNotNull(this.testScoped);
        assertSame(singletonScoped, this.singletonScoped);
    }

}
