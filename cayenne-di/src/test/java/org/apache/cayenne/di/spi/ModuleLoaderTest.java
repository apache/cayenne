/*
 *    Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.di.spi;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ModuleLoaderTest {

    @Test
    public void testLoad() {

        List<Module> modules = new ModuleLoader().load(ModuleProvider.class);
        assertEquals(4, modules.size());
        assertTrue(String.valueOf(modules.get(0)), modules.get(0) instanceof Module3);
        assertTrue(String.valueOf(modules.get(1)), modules.get(1) instanceof Module4);
        assertTrue(String.valueOf(modules.get(2)), modules.get(2) instanceof Module2);
        assertTrue(String.valueOf(modules.get(3)), modules.get(3) instanceof Module1);

        Injector i = DIBootstrap.createInjector(modules);
        assertEquals("a", i.getInstance(String.class));
        assertEquals(Integer.valueOf(56), i.getInstance(Integer.class));
    }

    @Test
    public void testLoadCustom() {
        List<Module> modules = new ModuleLoader().load(CustomModuleProvider.class);
        assertEquals(2, modules.size());
        assertTrue(String.valueOf(modules.get(0)), modules.get(0) instanceof Module5);
        assertTrue(String.valueOf(modules.get(1)), modules.get(1) instanceof Module6);

        Injector i = DIBootstrap.createInjector(modules);
        assertEquals(Integer.valueOf(66), i.getInstance(Integer.class));
    }

    public static class Module1 implements Module {

        @Override
        public void configure(Binder binder) {
            binder.bind(String.class).toInstance("a");
        }
    }

    public static class Module2 implements Module {

        @Override
        public void configure(Binder binder) {
            binder.bind(String.class).toInstance("b");
        }
    }

    public static class Module3 implements Module {

        @Override
        public void configure(Binder binder) {
            binder.bind(Integer.class).toInstance(66);
        }
    }

    public static class Module4 implements Module {

        @Override
        public void configure(Binder binder) {
            binder.bind(Integer.class).toInstance(56);
        }
    }

    public static class Module5 implements Module {
        @Override
        public void configure(Binder binder) {
            binder.bind(Integer.class).toInstance(56);
        }
    }

    public static class Module6 implements Module {
        @Override
        public void configure(Binder binder) {
            binder.bind(Integer.class).toInstance(66);
        }
    }

    public static class ModuleProvider1 implements ModuleProvider {

        @Override
        public Module module() {
            return new Module1();
        }

        @Override
        public Class<? extends Module> moduleType() {
            return Module1.class;
        }

        @Override
        public Collection<Class<? extends Module>> overrides() {
            return Collections.singletonList(Module2.class);
        }
    }

    public static class ModuleProvider2 implements ModuleProvider {

        @Override
        public Module module() {
            return new Module2();
        }

        @Override
        public Class<? extends Module> moduleType() {
            return Module2.class;
        }

        @Override
        public Collection<Class<? extends Module>> overrides() {
            return Collections.singletonList(Module4.class);
        }
    }

    public static class ModuleProvider3 implements ModuleProvider {

        @Override
        public Module module() {
            return new Module3();
        }

        @Override
        public Class<? extends Module> moduleType() {
            return Module3.class;
        }

        @Override
        public Collection<Class<? extends Module>> overrides() {
            return Collections.emptyList();
        }
    }

    public static class ModuleProvider4 implements ModuleProvider {

        @Override
        public Module module() {
            return new Module4();
        }

        @Override
        public Class<? extends Module> moduleType() {
            return Module4.class;
        }

        @Override
        public Collection<Class<? extends Module>> overrides() {
            return Collections.singletonList(Module3.class);
        }
    }

    public static class ModuleProvider5 implements CustomModuleProvider {

        @Override
        public Module module() {
            return new Module5();
        }

        @Override
        public Class<? extends Module> moduleType() {
            return Module5.class;
        }

        @Override
        public Collection<Class<? extends Module>> overrides() {
            return Collections.emptyList();
        }
    }

    public static class ModuleProvider6 implements CustomModuleProvider {

        @Override
        public Module module() {
            return new Module6();
        }

        @Override
        public Class<? extends Module> moduleType() {
            return Module6.class;
        }

        @Override
        public Collection<Class<? extends Module>> overrides() {
            return Collections.singletonList(Module5.class);
        }
    }
}
