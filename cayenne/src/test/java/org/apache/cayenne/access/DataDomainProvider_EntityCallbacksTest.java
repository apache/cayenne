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

package org.apache.cayenne.access;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.access.lifecycle.LifecycleEventsManager;
import org.apache.cayenne.lifecycle.PostAddAware;
import org.apache.cayenne.lifecycle.PostLoadAware;
import org.apache.cayenne.lifecycle.PrePersistAware;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the registration of the callbacks declared by the entity classes, when the DataDomainProvider creates the
 * LifecycleEventsManager.
 */
public class DataDomainProvider_EntityCallbacksTest {

    @Test
    public void entityCallbacks() {
        LifecycleEventsManager manager = createManager(E1.class, E2.class);

        assertTrue(manager.postAdd().hasListeners());
        assertTrue(manager.prePersist().hasListeners());
        assertFalse(manager.postLoad().hasListeners());

        E1 e1 = new E1();
        manager.postAdd().onEvent(List.of(e1));
        manager.prePersist().onEvent(List.of(e1));
        manager.postLoad().onEvent(List.of(e1));
        assertEquals(List.of("E1.onPostAdd"), e1.callbacks);

        E2 e2 = new E2();
        manager.postAdd().onEvent(List.of(e2));
        manager.prePersist().onEvent(List.of(e2));
        assertEquals(List.of("E2.onPrePersist"), e2.callbacks);
    }

    @Test
    public void inheritedByMappedSubclass() {
        // a callback implemented by a mapped superclass must be invoked once on its subclasses, whether overridden
        // or not
        LifecycleEventsManager manager = createManager(E1.class, E1Sub.class, E1SubOverride.class);

        E1Sub sub = new E1Sub();
        manager.postAdd().onEvent(List.of(sub));
        assertEquals(List.of("E1.onPostAdd"), sub.callbacks);

        E1SubOverride subOverride = new E1SubOverride();
        manager.postAdd().onEvent(List.of(subOverride));
        assertEquals(List.of("E1SubOverride.onPostAdd"), subOverride.callbacks);
    }

    @Test
    public void inheritedFromUnmappedSuperclass() {
        // a callback implemented by an unmapped superclass shared by several entities is invoked once per object
        LifecycleEventsManager manager = createManager(E3.class, E4.class);

        E3 e3 = new E3();
        manager.postLoad().onEvent(List.of(e3));
        assertEquals(List.of("Base.onPostLoad"), e3.callbacks);

        E4 e4 = new E4();
        manager.postLoad().onEvent(List.of(e4));
        assertEquals(List.of("Base.onPostLoad"), e4.callbacks);
    }

    @Test
    public void invokedBeforeListeners() {
        LifecycleEventsManager manager = createManager(E1.class);

        E1 e1 = new E1();
        manager.postAdd().addListener(e -> e.callbacks.add("listener"), E1.class);
        manager.postAdd().onEvent(List.of(e1));
        assertEquals(List.of("E1.onPostAdd", "listener"), e1.callbacks);
    }

    /**
     * Creates a manager over an EntityResolver mock with entities mapped to the given classes.
     */
    private static LifecycleEventsManager createManager(Class<?>... entityClasses) {
        List<ObjEntity> entities = new ArrayList<>();
        for (Class<?> entityClass : entityClasses) {
            // the class name doubles as the entity name, to look up the class in the descriptor mock
            entities.add(new ObjEntity(entityClass.getName()));
        }

        EntityResolver resolver = mock(EntityResolver.class);
        when(resolver.getObjEntities()).thenReturn(entities);
        when(resolver.getClassDescriptor(anyString())).thenAnswer(i -> {
            ClassDescriptor descriptor = mock(ClassDescriptor.class);
            when(descriptor.getObjectClass()).thenAnswer(a -> Class.forName(i.getArgument(0)));
            return descriptor;
        });

        return new DataDomainProvider().createLifecycleEventsManager(resolver);
    }

    public static class E1 extends PersistentObject implements PostAddAware {

        List<String> callbacks = new ArrayList<>();

        @Override
        public void onPostAdd() {
            callbacks.add("E1.onPostAdd");
        }
    }

    public static class E1Sub extends E1 {
    }

    public static class E1SubOverride extends E1 {

        @Override
        public void onPostAdd() {
            callbacks.add("E1SubOverride.onPostAdd");
        }
    }

    public static class E2 extends PersistentObject implements PrePersistAware {

        List<String> callbacks = new ArrayList<>();

        @Override
        public void onPrePersist() {
            callbacks.add("E2.onPrePersist");
        }
    }

    public abstract static class Base extends PersistentObject implements PostLoadAware {

        List<String> callbacks = new ArrayList<>();

        @Override
        public void onPostLoad() {
            callbacks.add("Base.onPostLoad");
        }
    }

    public static class E3 extends Base {
    }

    public static class E4 extends Base {
    }
}
