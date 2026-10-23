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

import org.apache.cayenne.LifecycleListener;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.annotation.PostAdd;
import org.apache.cayenne.annotation.PostLoad;
import org.apache.cayenne.annotation.PostPersist;
import org.apache.cayenne.annotation.PostRemove;
import org.apache.cayenne.annotation.PostUpdate;
import org.apache.cayenne.annotation.PrePersist;
import org.apache.cayenne.annotation.PreRemove;
import org.apache.cayenne.annotation.PreUpdate;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class LifecycleEventManagerTest {

    LifecycleEventManager lifecycleEventManager;

    @BeforeEach
    public void createRegistry() {
        EntityResolver entityResolver = mock(EntityResolver.class);
        lifecycleEventManager = new LifecycleEventManager(entityResolver);
        for(LifecycleEvent event : LifecycleEvent.values()) {
            assertEquals(0, lifecycleEventManager.getHandler(event).defaultListenersSize());
            assertEquals(0, lifecycleEventManager.getHandler(event).listenersSize());
        }
    }

    @Test
    public void addDefaultListener() throws Exception {
        LifecycleListener listener = mock(LifecycleListener.class);
        lifecycleEventManager.addDefaultListener(listener);
        for(LifecycleEvent event : LifecycleEvent.values()) {
            assertEquals(1, lifecycleEventManager.getHandler(event).defaultListenersSize());
            assertEquals(0, lifecycleEventManager.getHandler(event).listenersSize());
        }
    }

    @Test
    public void addDefaultListenerSingleType() throws Exception {
        for(LifecycleEvent event : LifecycleEvent.values()) {
            assertEquals(0, lifecycleEventManager.getHandler(event).defaultListenersSize());
            LifecycleListener listener = mock(LifecycleListener.class);
            lifecycleEventManager.addDefaultListener(event, method(listener, event));
            assertEquals(1, lifecycleEventManager.getHandler(event).defaultListenersSize());
            assertEquals(0, lifecycleEventManager.getHandler(event).listenersSize());
        }
    }

    @Test
    public void addListenerWithEntityClass() throws Exception {
        LifecycleListener listener = mock(LifecycleListener.class);
        lifecycleEventManager.addListener(Persistent.class, listener);
        for(LifecycleEvent event : LifecycleEvent.values()) {
            assertEquals(1, lifecycleEventManager.getHandler(event).listenersSize());
            assertEquals(0, lifecycleEventManager.getHandler(event).defaultListenersSize());
        }
    }

    @Test
    public void addListenerWithEntityClassSingleType() throws Exception {
        for(LifecycleEvent event : LifecycleEvent.values()) {
            assertEquals(0, lifecycleEventManager.getHandler(event).listenersSize());
            LifecycleListener listener = mock(LifecycleListener.class);
            lifecycleEventManager.addListener(event, Persistent.class, method(listener, event));
            assertEquals(1, lifecycleEventManager.getHandler(event).listenersSize());
            assertEquals(0, lifecycleEventManager.getHandler(event).defaultListenersSize());
        }
    }

    @Test
    public void addDefaultListener_MethodReference() {
        PersistentParameterListener listener = new PersistentParameterListener();
        lifecycleEventManager.addDefaultListener(LifecycleEvent.POST_ADD, listener::postAdd);
        assertEquals(1, lifecycleEventManager.getHandler(LifecycleEvent.POST_ADD).defaultListenersSize());

        Persistent object = new Artist();
        lifecycleEventManager.performCallbacks(LifecycleEvent.POST_ADD, object);
        assertSame(object, listener.seen);
    }

    @Test
    public void addAnnotatedListener_PersistentParameter() {
        PersistentParameterListener listener = new PersistentParameterListener();
        lifecycleEventManager.addListener(listener);
        assertEquals(1, lifecycleEventManager.getHandler(LifecycleEvent.POST_ADD).defaultListenersSize());

        Persistent object = new Artist();
        lifecycleEventManager.performCallbacks(LifecycleEvent.POST_ADD, object);
        assertSame(object, listener.seen);
    }

    @Test
    public void addAnnotatedListener() {
        lifecycleEventManager.addListener(new AnnotatedListener());
        for(LifecycleEvent event : LifecycleEvent.values()) {
            assertEquals(1, lifecycleEventManager.getHandler(event).defaultListenersSize());
            assertEquals(0, lifecycleEventManager.getHandler(event).listenersSize());
        }
    }

    @Test
    public void addAnnotatedListenerWithEntityClass() {
        lifecycleEventManager.addListener(new AnnotatedListenerWithEntity());
        for(LifecycleEvent event : LifecycleEvent.values()) {
            assertEquals(0, lifecycleEventManager.getHandler(event).defaultListenersSize());
            assertEquals(1, lifecycleEventManager.getHandler(event).listenersSize());
        }
    }

    private static class PersistentParameterListener {

        Persistent seen;

        @PostAdd
        public void postAdd(Persistent object) {
            this.seen = object;
        }
    }

    private static class AnnotatedListener {
        @PostAdd
        public void postAdd(Object object) {}

        @PostLoad
        public void postLoad(Object object) {}

        @PostPersist
        public void postPersist(Object object) {}

        @PostRemove
        public void postRemove(Object object) {}

        @PostUpdate
        public void postUpdate(Object object) {}

        @PrePersist
        public void prePersist(Object object) {}

        @PreRemove
        public void preRemove(Object object) {}

        @PreUpdate
        public void preUpdate(Object object) {}
    }

    private static class AnnotatedListenerWithEntity {
        @PostAdd(Persistent.class)
        public void postAdd(Object object) {}

        @PostLoad(Persistent.class)
        public void postLoad(Object object) {}

        @PostPersist(Persistent.class)
        public void postPersist(Object object) {}

        @PostRemove(Persistent.class)
        public void postRemove(Object object) {}

        @PostUpdate(Persistent.class)
        public void postUpdate(Object object) {}

        @PrePersist(Persistent.class)
        public void prePersist(Object object) {}

        @PreRemove(Persistent.class)
        public void preRemove(Object object) {}

        @PreUpdate(Persistent.class)
        public void preUpdate(Object object) {}
    }

    private static Consumer<Persistent> method(LifecycleListener listener, LifecycleEvent event) {
        return switch (event) {
            case POST_ADD -> listener::postAdd;
            case PRE_PERSIST -> listener::prePersist;
            case POST_PERSIST -> listener::postPersist;
            case PRE_REMOVE -> listener::preRemove;
            case POST_REMOVE -> listener::postRemove;
            case PRE_UPDATE -> listener::preUpdate;
            case POST_UPDATE -> listener::postUpdate;
            case POST_LOAD -> listener::postLoad;
        };
    }
}