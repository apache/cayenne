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

import org.apache.cayenne.Persistent;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LifecycleEventsManagerTest {

    static final List<Function<LifecycleEventsManager, LifecycleEventManager>> EVENTS = List.of(
            LifecycleEventsManager::postAdd,
            LifecycleEventsManager::prePersist,
            LifecycleEventsManager::postPersist,
            LifecycleEventsManager::preUpdate,
            LifecycleEventsManager::postUpdate,
            LifecycleEventsManager::preRemove,
            LifecycleEventsManager::postRemove,
            LifecycleEventsManager::postLoad);

    LifecycleEventsManager lifecycleEventsManager;

    @BeforeEach
    public void createManager() {
        lifecycleEventsManager = new LifecycleEventsManager();
        for (Function<LifecycleEventsManager, LifecycleEventManager> event : EVENTS) {
            assertFalse(event.apply(lifecycleEventsManager).hasListeners());
        }
    }

    @Test
    public void eventManagersAreDistinct() {
        List<LifecycleEventManager> managers = EVENTS.stream().map(e -> e.apply(lifecycleEventsManager)).toList();
        assertEquals(8, managers.stream().distinct().count());
    }

    @Test
    public void addListener_AllEntities() {
        Listener listener = new Listener();
        for (Function<LifecycleEventsManager, LifecycleEventManager> event : EVENTS) {
            LifecycleEventManager eventManager = event.apply(lifecycleEventsManager);
            eventManager.addListener(listener::callback);
            assertTrue(eventManager.hasListeners());

            // any entity is dispatched
            eventManager.onEvent(List.of(new Artist(), new Painting()));
        }
        assertEquals(2 * EVENTS.size(), listener.seen.size());
    }

    @Test
    public void addListener() {
        Listener listener = new Listener();
        for (Function<LifecycleEventsManager, LifecycleEventManager> event : EVENTS) {
            LifecycleEventManager eventManager = event.apply(lifecycleEventsManager);
            eventManager.addListener(listener::artistCallback, Artist.class);
            assertTrue(eventManager.hasListeners());

            // only the entity and its subclasses are dispatched
            eventManager.onEvent(List.of(new Artist(), new Painting()));
        }
        assertEquals(EVENTS.size(), listener.seen.size());
    }

    @Test
    public void addListener_AllEntities_MethodReference() {
        Listener listener = new Listener();
        lifecycleEventsManager.postAdd().addListener(listener::callback);

        Persistent object = new Artist();
        lifecycleEventsManager.postAdd().onEvent(List.of(object));
        assertEquals(1, listener.seen.size());
        assertSame(object, listener.seen.get(0));
    }

    @Test
    public void addListener_MethodReference() {
        Listener listener = new Listener();
        lifecycleEventsManager.postAdd().addListener(listener::artistCallback, Artist.class);

        Artist artist = new Artist();
        lifecycleEventsManager.postAdd().onEvent(List.of(artist, new Painting()));
        assertEquals(1, listener.seen.size());
        assertSame(artist, listener.seen.get(0));
    }

    static class Listener {

        List<Persistent> seen = new ArrayList<>();

        void callback(Persistent object) {
            seen.add(object);
        }

        void artistCallback(Artist artist) {
            seen.add(artist);
        }
    }
}
