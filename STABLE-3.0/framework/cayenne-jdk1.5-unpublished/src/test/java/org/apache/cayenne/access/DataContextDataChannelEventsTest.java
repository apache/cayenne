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

package org.apache.cayenne.access;

import org.apache.art.Artist;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelListener;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.graph.GraphEvent;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.util.ThreadedTestHelper;
import org.apache.cayenne.util.EventUtil;

/**
 * Tests that DataContext sends DataChannel events.
 *
 */
public class DataContextDataChannelEventsTest extends CayenneCase {

    public void testCommitEvent() {
        DataContext context = createDataContext();

        Artist a = context.newObject(Artist.class);
        a.setArtistName("X");
        context.commitChanges();

        MockChannelListener listener = new MockChannelListener();
        EventUtil.listenForChannelEvents(context, listener);

        a.setArtistName("Y");
        context.commitChanges();

        assertTrue(listener.graphCommitted);
        assertFalse(listener.graphChanged);
        assertFalse(listener.graphRolledBack);
    }

    public void testRollbackEvent() {
        DataContext context = createDataContext();

        Artist a = context.newObject(Artist.class);
        a.setArtistName("X");
        context.commitChanges();

        MockChannelListener listener = new MockChannelListener();
        EventUtil.listenForChannelEvents(context, listener);

        a.setArtistName("Y");
        context.rollbackChanges();

        assertFalse(listener.graphCommitted);
        assertFalse(listener.graphChanged);
        assertTrue(listener.graphRolledBack);
    }

    public void testChangeEventOnChildChange() {
        DataContext context = createDataContext();

        Artist a = context.newObject(Artist.class);
        a.setArtistName("X");
        context.commitChanges();

        MockChannelListener listener = new MockChannelListener();
        EventUtil.listenForChannelEvents(context, listener);

        ObjectContext child = context.createChildContext();
        Artist a1 = (Artist) child.localObject(a.getObjectId(), a);

        a1.setArtistName("Y");
        child.commitChangesToParent();

        assertFalse(listener.graphCommitted);
        assertTrue(listener.graphChanged);
        assertFalse(listener.graphRolledBack);
    }

    public void testChangeEventOnPeerChange() throws Exception {
        DataContext context = createDataContext();

        Artist a = context.newObject(Artist.class);
        a.setArtistName("X");
        context.commitChanges();

        final MockChannelListener listener = new MockChannelListener();
        EventUtil.listenForChannelEvents(context, listener);

        DataContext peer = context.getParentDataDomain().createDataContext();
        Artist a1 = (Artist) peer.localObject(a.getObjectId(), a);

        a1.setArtistName("Y");
        peer.commitChangesToParent();

        new ThreadedTestHelper() {

            @Override
            protected void assertResult() throws Exception {
                assertFalse(listener.graphCommitted);
                assertTrue(listener.graphChanged);
                assertFalse(listener.graphRolledBack);
            }
        }.assertWithTimeout(1000);
    }

    public void testChangeEventOnPeerChangeSecondNestingLevel() throws Exception {
        DataContext context = createDataContext();

        ObjectContext childPeer1 = context.createChildContext();

        Artist a = childPeer1.newObject(Artist.class);
        a.setArtistName("X");
        childPeer1.commitChanges();

        final MockChannelListener listener = new MockChannelListener();
        EventUtil.listenForChannelEvents((DataChannel) childPeer1, listener);

        ObjectContext childPeer2 = context.createChildContext();

        Artist a1 = (Artist) childPeer2.localObject(a.getObjectId(), a);

        a1.setArtistName("Y");
        childPeer2.commitChangesToParent();

        new ThreadedTestHelper() {

            @Override
            protected void assertResult() throws Exception {
                assertFalse(listener.graphCommitted);
                assertTrue(listener.graphChanged);
                assertFalse(listener.graphRolledBack);
            }
        }.assertWithTimeout(1000);
    }

    class MockChannelListener implements DataChannelListener {

        boolean graphChanged;
        boolean graphCommitted;
        boolean graphRolledBack;

        public void graphChanged(GraphEvent event) {
            graphChanged = true;
        }

        public void graphFlushed(GraphEvent event) {
            graphCommitted = true;
        }

        public void graphRolledback(GraphEvent event) {
            graphRolledBack = true;
        }
    }
}
