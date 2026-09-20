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
package org.apache.cayenne.docs;

import org.apache.cayenne.configuration.runtime.CoreModule;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.docs.customize.DoubleArrayType;
import org.apache.cayenne.docs.customize.MoneyValueObjectType;
import org.apache.cayenne.docs.customize.MyDbAdapterDetector;
import org.apache.cayenne.docs.customize.MyExtensionsModule;
import org.apache.cayenne.docs.di.Module1;
import org.apache.cayenne.docs.di.Service1;
import org.apache.cayenne.docs.di.Service1Provider;
import org.apache.cayenne.docs.di.Service2;
import org.apache.cayenne.docs.di.Service2Impl;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class CustomizeTest {

    @Test
    public void injector() {
        // tag::injector[]
        Injector injector = DIBootstrap.createInjector(new Module1());
        // end::injector[]

        // tag::getInstance[]
        Service1 s1 = injector.getInstance(Service1.class);
        for (int i = 0; i < 5; i++) {
            System.out.println("S1 String: " + s1.getString());
        }
        // end::getInstance[]

        // the 5 calls above must have incremented the counter in the injected Service2
        assertEquals("5_Service1Impl", s1.getString());
    }

    @Test
    public void constructorInjection() {
        Injector injector = DIBootstrap.createInjector(b -> {
            b.bind(Service1.class).to(org.apache.cayenne.docs.di.constructor.Service1Impl.class);
            b.bind(Service2.class).to(Service2Impl.class);
        });

        assertEquals("0_Service1Impl", injector.getInstance(Service1.class).getString());
    }

    @Test
    public void bindings() {
        Module providers = binder -> {
            // tag::bindings[]
            // binding to instance - allowing user to create and configure instance
            // inside the module class
            binder.bind(Service2.class).toInstance(new Service2Impl());

            // binding to provider - delegating instance creation to a special
            // provider class
            binder.bind(Service1.class).toProvider(Service1Provider.class);

            // binding to provider instance
            binder.bind(Service1.class).toProviderInstance(new Service1Provider());

            // multiple bindings of the same type using Key
            // injection can reference the key name in annotation:
            // @Inject("i1")
            // private Service2 service2;
            binder.bind(Key.get(Service2.class, "i1")).to(Service2Impl.class);
            binder.bind(Key.get(Service2.class, "i2")).to(Service2Impl.class);
            // end::bindings[]
        };

        Injector injector = DIBootstrap.createInjector(providers);
        assertEquals("Service1Provider", injector.getInstance(Service1.class).getString());
        assertNotSame(
                injector.getInstance(Key.get(Service2.class, "i1")),
                injector.getInstance(Key.get(Service2.class, "i2")));
    }

    @Test
    public void withoutScope() {
        Injector injector = DIBootstrap.createInjector(binder -> {
            // tag::withoutScope[]
            binder.bind(Service2.class).to(Service2Impl.class).withoutScope();
            // end::withoutScope[]
        });

        assertNotSame(injector.getInstance(Service2.class), injector.getInstance(Service2.class));
    }

    @Test
    public void extensionsModule() {
        // tag::extensionsModule[]
        Module extensions = new MyExtensionsModule();
        CayenneRuntime runtime = CayenneRuntime.of()
                .addConfig("com/example/cayenne-mydomain.xml")
                .addModule(extensions)
                .build();
        // end::extensionsModule[]

        assertNotNull(runtime.getDataDomain());
        runtime.shutdown();
    }

    @Test
    public void adapterDetector() {
        CayenneRuntime runtime = CayenneRuntime.of()
                .addConfig("cayenne-project.xml")
                .addModule(this::adapterDetector)
                .build();
        assertNotNull(runtime.getDataDomain());
        runtime.shutdown();
    }

    private void adapterDetector(Binder binder) {
        // tag::adapterDetector[]
        CoreModule.extend(binder)
                .addAdapterDetector(MyDbAdapterDetector.class);
        // end::adapterDetector[]
    }

    @Test
    public void valueObjectType() {
        // tag::valueObjectType[]
        CayenneRuntime runtime = CayenneRuntime.of()
                .addConfig("cayenne-project.xml")
                .addModule(binder ->
                        CoreModule.extend(binder)
                                .addValueObjectType(MoneyValueObjectType.class))
                .build();
        // end::valueObjectType[]

        assertNotNull(runtime.getDataDomain());
        runtime.shutdown();
    }

    @Test
    public void extendedType() {
        // tag::extendedType[]
        // add DoubleArrayType to list of user types
        CayenneRuntime runtime = CayenneRuntime.of()
                .addConfig("cayenne-project.xml")
                .addModule(binder ->
                        CoreModule.extend(binder)
                                .addUserExtendedType(new DoubleArrayType()))
                .build();
        // end::extendedType[]

        assertNotNull(runtime.getDataDomain());
        runtime.shutdown();
    }
}
