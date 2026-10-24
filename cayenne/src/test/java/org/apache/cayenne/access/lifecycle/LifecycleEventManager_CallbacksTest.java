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
package org.apache.cayenne.access.lifecycle;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.MockCallingBackListener;
import org.apache.cayenne.map.MockCallingBackEntity;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LifecycleEventManager_CallbacksTest {

    @Test
    public void onEntity_Public() throws Exception {
        Consumer<Persistent> callback = LifecycleEventManager.entityCallback(
                MockCallingBackEntity.class.getDeclaredMethod("publicCallback"));

        MockCallingBackEntity e = new MockCallingBackEntity();
        callback.accept(e);
        assertTrue(e.publicCallbackInvoked);
        assertFalse(e.protectedCallbackInvoked);
        assertFalse(e.privateCallbackInvoked);
        assertFalse(e.defaultCallbackInvoked);
    }

    @Test
    public void onEntity_Protected() throws Exception {
        Consumer<Persistent> callback = LifecycleEventManager.entityCallback(
                MockCallingBackEntity.class.getDeclaredMethod("protectedCallback"));

        MockCallingBackEntity e = new MockCallingBackEntity();
        callback.accept(e);
        assertFalse(e.publicCallbackInvoked);
        assertTrue(e.protectedCallbackInvoked);
        assertFalse(e.privateCallbackInvoked);
        assertFalse(e.defaultCallbackInvoked);
    }

    @Test
    public void onEntity_Private() throws Exception {
        Consumer<Persistent> callback = LifecycleEventManager.entityCallback(
                MockCallingBackEntity.class.getDeclaredMethod("privateCallback"));

        MockCallingBackEntity e = new MockCallingBackEntity();
        callback.accept(e);
        assertFalse(e.publicCallbackInvoked);
        assertFalse(e.protectedCallbackInvoked);
        assertTrue(e.privateCallbackInvoked);
        assertFalse(e.defaultCallbackInvoked);
    }

    @Test
    public void onEntity_Default() throws Exception {
        Consumer<Persistent> callback = LifecycleEventManager.entityCallback(
                MockCallingBackEntity.class.getDeclaredMethod("defaultCallback"));

        MockCallingBackEntity e = new MockCallingBackEntity();
        callback.accept(e);
        assertFalse(e.publicCallbackInvoked);
        assertFalse(e.protectedCallbackInvoked);
        assertFalse(e.privateCallbackInvoked);
        assertTrue(e.defaultCallbackInvoked);
    }

    @Test
    public void onEntity_Static() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> LifecycleEventManager.entityCallback(MockCallingBackEntity.class.getDeclaredMethod("staticCallback")));
    }

    @Test
    public void onEntity_UncheckedExceptionPropagatesAsIs() throws Exception {
        Consumer<Persistent> callback = LifecycleEventManager.entityCallback(
                MockCallingBackEntity.class.getDeclaredMethod("uncheckedThrowingCallback"));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> callback.accept(new MockCallingBackEntity()));
        assertEquals("unchecked", e.getMessage());
    }

    @Test
    public void onEntity_CheckedExceptionIsWrapped() throws Exception {
        Consumer<Persistent> callback = LifecycleEventManager.entityCallback(
                MockCallingBackEntity.class.getDeclaredMethod("checkedThrowingCallback"));

        CayenneRuntimeException e = assertThrows(CayenneRuntimeException.class,
                () -> callback.accept(new MockCallingBackEntity()));
        assertEquals("checked", e.getCause().getMessage());
    }

    @Test
    public void onListener() throws Exception {
        MockCallingBackListener listener = new MockCallingBackListener();
        Consumer<Persistent> callback = LifecycleEventManager.listenerCallback(
                listener,
                MockCallingBackListener.class.getMethod("publicCallback", Object.class),
                Persistent.class);

        MockCallingBackEntity e = new MockCallingBackEntity();
        callback.accept(e);

        // entity itself should not be called back...
        assertFalse(e.publicCallbackInvoked);
        assertSame(e, listener.getPublicCalledbackEntity());
    }

    @Test
    public void onListener_EntityNotAssignableToParameter() throws Exception {
        EntityTypedListener listener = new EntityTypedListener();
        assertThrows(IllegalArgumentException.class, () -> LifecycleEventManager.listenerCallback(
                listener,
                EntityTypedListener.class.getMethod("callback", MockCallingBackEntity.class),
                Persistent.class));
    }

    public static class EntityTypedListener {

        public void callback(MockCallingBackEntity entity) {
        }
    }
}
