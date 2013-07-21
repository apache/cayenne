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

package org.apache.cayenne.event;

import java.util.Collection;

import junit.framework.TestCase;

import org.apache.cayenne.access.event.SnapshotEvent;
import org.apache.cayenne.test.parallel.ParallelTestContainer;

/**
 */
public class EventBridgeTest extends TestCase {

    public void testConstructor() throws Exception {
        EventSubject local = EventSubject
                .getSubject(EventBridgeTest.class, "testInstall");
        String external = "externalSubject";
        TestBridge bridge = new TestBridge(local, external);

        Collection subjects = bridge.getLocalSubjects();
        assertEquals(1, subjects.size());
        assertTrue(subjects.contains(local));
        assertEquals(external, bridge.getExternalSubject());
    }

    public void testStartup() throws Exception {
        EventSubject local = EventSubject
                .getSubject(EventBridgeTest.class, "testInstall");
        String external = "externalSubject";
        TestBridge bridge = new TestBridge(local, external);

        EventManager manager = new DefaultEventManager();
        bridge.startup(manager, EventBridge.RECEIVE_LOCAL_EXTERNAL);

        assertSame(manager, bridge.eventManager);
        assertEquals(1, bridge.startupCalls);
        assertEquals(0, bridge.shutdownCalls);

        // try startup again
        EventManager newManager = new DefaultEventManager();
        bridge.startup(newManager, EventBridge.RECEIVE_LOCAL_EXTERNAL);

        assertSame(newManager, bridge.eventManager);
        assertEquals(2, bridge.startupCalls);
        assertEquals(1, bridge.shutdownCalls);
    }

    public void testShutdown() throws Exception {
        EventSubject local = EventSubject
                .getSubject(EventBridgeTest.class, "testInstall");
        String external = "externalSubject";
        TestBridge bridge = new TestBridge(local, external);

        EventManager manager = new DefaultEventManager();
        bridge.startup(manager, EventBridge.RECEIVE_LOCAL_EXTERNAL);
        bridge.shutdown();

        assertNull(bridge.eventManager);
        assertEquals(1, bridge.startupCalls);
        assertEquals(1, bridge.shutdownCalls);
    }

    public void testSendExternalEvent() throws Exception {

        final EventSubject local = EventSubject.getSubject(
                EventBridgeTest.class,
                "testInstall");
        String external = "externalSubject";
        final TestBridge bridge = new TestBridge(local, external);

        EventManager manager = new DefaultEventManager(2);
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
