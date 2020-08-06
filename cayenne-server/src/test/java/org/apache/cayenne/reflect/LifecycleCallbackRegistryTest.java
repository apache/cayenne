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

package org.apache.cayenne.reflect;

import org.apache.cayenne.LifecycleListener;
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
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * @since 4.0
 */
public class LifecycleCallbackRegistryTest {

    LifecycleCallbackRegistry registry;

    @Before
    public void createRegistry() {
        EntityResolver entityResolver = mock(EntityResolver.class);
        registry = new LifecycleCallbackRegistry(entityResolver);
        for(LifecycleEvent event : LifecycleEvent.values()) {
            assertEquals(0, registry.getHandler(event).defaultListenersSize());
            assertEquals(0, registry.getHandler(event).listenersSize());
        }
    }

    @Test
    public void addDefaultListener() throws Exception {
        LifecycleListener listener = mock(LifecycleListener.class);
        registry.addDefaultListener(listener);
        for(LifecycleEvent event : LifecycleEvent.values()) {
            assertEquals(1, registry.getHandler(event).defaultListenersSize());
            assertEquals(0, registry.getHandler(event).listenersSize());
        }
    }

    @Test
    public void addDefaultListenerSingleType() throws Exception {
        for(LifecycleEvent event : LifecycleEvent.values()) {
            assertEquals(0, registry.getHandler(event).defaultListenersSize());
            LifecycleListener listener = mock(LifecycleListener.class);
            registry.addDefaultListener(event, listener, nameToCamelCase(event.name()));
            assertEquals(1, registry.getHandler(event).defaultListenersSize());
            assertEquals(0, registry.getHandler(event).listenersSize());
        }
    }

    @Test
    public void addListenerWithEntityClass() throws Exception {
        LifecycleListener listener = mock(LifecycleListener.class);
        registry.addListener(Object.class, listener);
        for(LifecycleEvent event : LifecycleEvent.values()) {
            assertEquals(1, registry.getHandler(event).listenersSize());
            assertEquals(0, registry.getHandler(event).defaultListenersSize());
        }
    }

    @Test
    public void addListenerWithEntityClassSingleType() throws Exception {
        for(LifecycleEvent event : LifecycleEvent.values()) {
            assertEquals(0, registry.getHandler(event).listenersSize());
            LifecycleListener listener = mock(LifecycleListener.class);
            registry.addListener(event, Object.class, listener, nameToCamelCase(event.name()));
            assertEquals(1, registry.getHandler(event).listenersSize());
            assertEquals(0, registry.getHandler(event).defaultListenersSize());
        }
    }

    @Test
    public void addAnnotatedListener() {
        registry.addListener(new AnnotatedListener());
        for(LifecycleEvent event : LifecycleEvent.values()) {
            assertEquals(1, registry.getHandler(event).defaultListenersSize());
            assertEquals(0, registry.getHandler(event).listenersSize());
        }
    }

    @Test
    public void addAnnotatedListenerWithEntityClass() {
        registry.addListener(new AnnotatedListenerWithEntity());
        for(LifecycleEvent event : LifecycleEvent.values()) {
            assertEquals(0, registry.getHandler(event).defaultListenersSize());
            assertEquals(1, registry.getHandler(event).listenersSize());
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
        @PostAdd(Object.class)
        public void postAdd(Object object) {}

        @PostLoad(Object.class)
        public void postLoad(Object object) {}

        @PostPersist(Object.class)
        public void postPersist(Object object) {}

        @PostRemove(Object.class)
        public void postRemove(Object object) {}

        @PostUpdate(Object.class)
        public void postUpdate(Object object) {}

        @PrePersist(Object.class)
        public void prePersist(Object object) {}

        @PreRemove(Object.class)
        public void preRemove(Object object) {}

        @PreUpdate(Object.class)
        public void preUpdate(Object object) {}
    }

    private static String nameToCamelCase(String functionName) {
        String[] parts = functionName.split("_");
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(String part : parts) {
            if(first) {
                sb.append(part.toLowerCase());
                first = false;
            } else {
                char[] chars = part.toLowerCase().toCharArray();
                chars[0] = Character.toTitleCase(chars[0]);
                sb.append(chars);
            }
        }
        return sb.toString();
    }
}