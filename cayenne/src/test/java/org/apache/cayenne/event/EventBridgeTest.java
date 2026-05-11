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

package org.apache.cayenne.event;

import org.apache.cayenne.access.event.SnapshotEvent;
import org.apache.cayenne.test.parallel.ParallelTestContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 */
public class EventBridgeTest {

    private List<DefaultEventManager> managersToClean = new ArrayList<>();

    @AfterEach
    public void cleanEventManagers() {
        for(DefaultEventManager manager : managersToClean) {
            manager.shutdown();
        }
        managersToClean.clear();
    }

    @Test
    public void constructor() throws Exception {
        EventSubject local = EventSubject
                .getSubject(EventBridgeTest.class, "testInstall");
        String external = "externalSubject";
        TestBridge bridge = new TestBridge(local, external);

        Collection subjects = bridge.getLocalSubjects();
        assertEquals(1, subjects.size());
        assertTrue(subjects.contains(local));
        assertEquals(external, bridge.getExternalSubject());
    }

    @Test
    public void startup() throws Exception {
        EventSubject local = EventSubject
                .getSubject(EventBridgeTest.class, "testInstall");
        String external = "externalSubject";
        TestBridge bridge = new TestBridge(local, external);

        DefaultEventManager manager = new DefaultEventManager();
        managersToClean.add(manager);
        bridge.startup(manager, EventBridge.RECEIVE_LOCAL_EXTERNAL);

        assertSame(manager, bridge.eventManager);
        assertEquals(1, bridge.startupCalls);
        assertEquals(0, bridge.shutdownCalls);

        // try startup again
        DefaultEventManager newManager = new DefaultEventManager();
        managersToClean.add(newManager);
        bridge.startup(newManager, EventBridge.RECEIVE_LOCAL_EXTERNAL);

        assertSame(newManager, bridge.eventManager);
        assertEquals(2, bridge.startupCalls);
        assertEquals(1, bridge.shutdownCalls);
    }

    @Test
    public void shutdown() throws Exception {
        EventSubject local = EventSubject
                .getSubject(EventBridgeTest.class, "testInstall");
        String external = "externalSubject";
        TestBridge bridge = new TestBridge(local, external);

        DefaultEventManager manager = new DefaultEventManager();
        managersToClean.add(manager);
        bridge.startup(manager, EventBridge.RECEIVE_LOCAL_EXTERNAL);
        bridge.shutdown();

        assertNull(bridge.eventManager);
        assertEquals(1, bridge.startupCalls);
        assertEquals(1, bridge.shutdownCalls);
    }

    @Test
    public void sendExternalEvent() throws Exception {

        final EventSubject local = EventSubject.getSubject(
                EventBridgeTest.class,
                "testInstall");
        String external = "externalSubject";
        final TestBridge bridge = new TestBridge(local, external);

        DefaultEventManager manager = new DefaultEventManager(2);
        managersToClean.add(manager);
        bridge.startup(manager, EventBridge.RECEIVE_LOCAL_EXTERNAL);

        final SnapshotEvent eventWithNoSubject = new SnapshotEvent(
                this,
                this,
                null,
                null,
                null,
                null);

        manager.postEvent(eventWithNoSubject, local);

        // check that event was received and that subject was injected...

        // since bridge is notified asynchronously by default,
        // we must wait till notification is received
        ParallelTestContainer helper = new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertTrue(bridge.lastLocalEvent instanceof SnapshotEvent);
                assertEquals(local, bridge.lastLocalEvent.getSubject());
            }
        };

        helper.runTest(5000);

        final SnapshotEvent eventWithSubject = new SnapshotEvent(
                this,
                this,
                null,
                null,
                null,
                null);
        eventWithSubject.setSubject(local);
        manager.postEvent(eventWithNoSubject, local);

        // check that event was received and that subject was injected...

        // since bridge is notified asynchronously by default,
        // we must wait till notification is received
        ParallelTestContainer helper1 = new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertTrue(bridge.lastLocalEvent instanceof SnapshotEvent);
                assertEquals(local, bridge.lastLocalEvent.getSubject());
            }
        };

        helper1.runTest(5000);
    }

    class TestBridge extends EventBridge {

        CayenneEvent lastLocalEvent;
        int startupCalls;
        int shutdownCalls;

        public TestBridge(EventSubject localSubject, String externalSubject) {
            super(localSubject, externalSubject);
        }

        @Override
        public void sendExternalEvent(CayenneEvent event) {
            lastLocalEvent = event;
        }

        @Override
        protected void shutdownExternal() throws Exception {
            shutdownCalls++;
        }

        @Override
        protected void startupExternal() throws Exception {
            startupCalls++;
        }
    }
}
