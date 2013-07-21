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

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelListener;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.graph.GraphEvent;
import org.apache.cayenne.test.parallel.ParallelTestContainer;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.util.EventUtil;

/**
 * Tests that DataContext sends DataChannel events.
 */
@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextDataChannelEventsTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DataContext peer;

    @Inject
    private ServerRuntime runtime;

    public void testCommitEvent() throws Exception {
        Artist a = context.newObject(Artist.class);
        a.setArtistName("X");
        context.commitChanges();

        final MockChannelListener listener = new MockChannelListener();
        EventUtil.listenForChannelEvents(context, listener);

        a.setArtistName("Y");
        context.commitChanges();

        new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertTrue(listener.graphCommitted);
                assertFalse(listener.graphChanged);
                assertFalse(listener.graphRolledBack);
            }
        }.runTest(10000);

    }

    public void testRollbackEvent() throws Exception {
        Artist a = context.newObject(Artist.class);
        a.setArtistName("X");
        context.commitChanges();

        final MockChannelListener listener = new MockChannelListener();
        EventUtil.listenForChannelEvents(context, listener);

        a.setArtistName("Y");
        context.rollbackChanges();

        new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertFalse(listener.graphCommitted);
                assertFalse(listener.graphChanged);
                assertTrue(listener.graphRolledBack);
            }
        }.runTest(10000);
    }

    public void testChangeEventOnChildChange() throws Exception {
        Artist a = context.newObject(Artist.class);
        a.setArtistName("X");
        context.commitChanges();

        final MockChannelListener listener = new MockChannelListener();
        EventUtil.listenForChannelEvents(context, listener);

        ObjectContext childContext = runtime.newContext(context);

        Artist a1 = childContext.localObject(a);

        a1.setArtistName("Y");
        childContext.commitChangesToParent();

        new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertFalse(listener.graphCommitted);
                assertTrue(listener.graphChanged);
                assertFalse(listener.graphRolledBack);
            }
        }.runTest(10000);
    }

    public void testChangeEventOnPeerChange() throws Exception {
        Artist a = context.newObject(Artist.class);
        a.setArtistName("X");
        context.commitChanges();

        final MockChannelListener listener = new MockChannelListener();
        EventUtil.listenForChannelEvents(context, listener);

        Artist a1 = peer.localObject(a);

        a1.setArtistName("Y");
        peer.commitChangesToParent();

        new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertFalse(listener.graphCommitted);
                assertTrue(listener.graphChanged);
                assertFalse(listener.graphRolledBack);
            }
        }.runTest(10000);
    }

    public void testChangeEventOnPeerChangeSecondNestingLevel() throws Exception {
        ObjectContext childPeer1 = runtime.newContext(context);

        Artist a = childPeer1.newObject(Artist.class);
        a.setArtistName("X");
        childPeer1.commitChanges();

        final MockChannelListener listener = new MockChannelListener();
        EventUtil.listenForChannelEvents((DataChannel) childPeer1, listener);

        ObjectContext childPeer2 = runtime.newContext(context);

        Artist a1 = childPeer2.localObject(a);

        a1.setArtistName("Y");
        childPeer2.commitChangesToParent();

        new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertFalse(listener.graphCommitted);
                assertTrue(listener.graphChanged);
                assertFalse(listener.graphRolledBack);
            }
        }.runTest(10000);
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
