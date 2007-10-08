/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.event;

import java.util.Collection;

import junit.framework.TestCase;

import org.objectstyle.cayenne.access.event.SnapshotEvent;
import org.objectstyle.cayenne.unit.util.ThreadedTestHelper;

/**
 * @author Andrei Adamchik
 */
public class EventBridgeTst extends TestCase {

    /**
     * @deprecated since 1.2
     */
    public void testConstructorOld() throws Exception {
        EventSubject local = EventSubject.getSubject(EventBridgeTst.class, "testInstall");
        String external = "externalSubject";
        TestBridge bridge = new TestBridge(local, external);

        assertEquals(local, bridge.getLocalSubject());
        assertEquals(external, bridge.getExternalSubject());
    }

    public void testConstructor() throws Exception {
        EventSubject local = EventSubject.getSubject(EventBridgeTst.class, "testInstall");
        String external = "externalSubject";
        TestBridge bridge = new TestBridge(local, external);

        Collection subjects = bridge.getLocalSubjects();
        assertEquals(1, subjects.size());
        assertTrue(subjects.contains(local));
        assertEquals(external, bridge.getExternalSubject());
    }

    public void testStartup() throws Exception {
        EventSubject local = EventSubject.getSubject(EventBridgeTst.class, "testInstall");
        String external = "externalSubject";
        TestBridge bridge = new TestBridge(local, external);

        EventManager manager = new EventManager();
        bridge.startup(manager, EventBridge.RECEIVE_LOCAL_EXTERNAL);

        assertSame(manager, bridge.eventManager);
        assertEquals(1, bridge.startupCalls);
        assertEquals(0, bridge.shutdownCalls);

        // try startup again
        EventManager newManager = new EventManager();
        bridge.startup(newManager, EventBridge.RECEIVE_LOCAL_EXTERNAL);

        assertSame(newManager, bridge.eventManager);
        assertEquals(2, bridge.startupCalls);
        assertEquals(1, bridge.shutdownCalls);
    }

    public void testShutdown() throws Exception {
        EventSubject local = EventSubject.getSubject(EventBridgeTst.class, "testInstall");
        String external = "externalSubject";
        TestBridge bridge = new TestBridge(local, external);

        EventManager manager = new EventManager();
        bridge.startup(manager, EventBridge.RECEIVE_LOCAL_EXTERNAL);
        bridge.shutdown();

        assertNull(bridge.eventManager);
        assertEquals(1, bridge.startupCalls);
        assertEquals(1, bridge.shutdownCalls);
    }

    public void testSendExternalEvent() throws Exception {

        final EventSubject local = EventSubject.getSubject(EventBridgeTst.class, "testInstall");
        String external = "externalSubject";
        final TestBridge bridge = new TestBridge(local, external);

        EventManager manager = new EventManager(2);
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
        ThreadedTestHelper helper = new ThreadedTestHelper() {

            protected void assertResult() throws Exception {
                assertTrue(bridge.lastLocalEvent instanceof SnapshotEvent);
                assertEquals(local, bridge.lastLocalEvent.getSubject());
            }
        };

        helper.assertWithTimeout(5000);
        
        
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
        ThreadedTestHelper helper1 = new ThreadedTestHelper() {

            protected void assertResult() throws Exception {
                assertTrue(bridge.lastLocalEvent instanceof SnapshotEvent);
                assertEquals(local, bridge.lastLocalEvent.getSubject());
            }
        };

        helper1.assertWithTimeout(5000);
    }

    class TestBridge extends EventBridge {

        CayenneEvent lastLocalEvent;
        int startupCalls;
        int shutdownCalls;

        public TestBridge(EventSubject localSubject, String externalSubject) {
            super(localSubject, externalSubject);
        }

        public void sendExternalEvent(CayenneEvent event) {
            lastLocalEvent = event;
        }

        protected void shutdownExternal() throws Exception {
            shutdownCalls++;
        }

        protected void startupExternal() throws Exception {
            startupCalls++;
        }
    }
}
