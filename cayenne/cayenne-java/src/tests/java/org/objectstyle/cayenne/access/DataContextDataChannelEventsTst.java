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
package org.objectstyle.cayenne.access;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.DataChannelListener;
import org.objectstyle.cayenne.graph.GraphEvent;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.util.ThreadedTestHelper;
import org.objectstyle.cayenne.util.EventUtil;

/**
 * Tests that DataContext sends DataChannel events.
 * 
 * @author Andrus Adamchik
 */
public class DataContextDataChannelEventsTst extends CayenneTestCase {

    public void testCommitEvent() {
        DataContext context = createDataContext();

        Artist a = (Artist) context.newObject(Artist.class);
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

        Artist a = (Artist) context.newObject(Artist.class);
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

        Artist a = (Artist) context.newObject(Artist.class);
        a.setArtistName("X");
        context.commitChanges();

        MockChannelListener listener = new MockChannelListener();
        EventUtil.listenForChannelEvents(context, listener);

        DataContext child = context.createChildDataContext();
        Artist a1 = (Artist) child.localObject(a.getObjectId(), a);

        a1.setArtistName("Y");
        child.commitChangesToParent();

        assertFalse(listener.graphCommitted);
        assertTrue(listener.graphChanged);
        assertFalse(listener.graphRolledBack);
    }

    public void testChangeEventOnPeerChange() throws Exception {
        DataContext context = createDataContext();

        Artist a = (Artist) context.newObject(Artist.class);
        a.setArtistName("X");
        context.commitChanges();

        final MockChannelListener listener = new MockChannelListener();
        EventUtil.listenForChannelEvents(context, listener);

        DataContext peer = context.getParentDataDomain().createDataContext();
        Artist a1 = (Artist) peer.localObject(a.getObjectId(), a);

        a1.setArtistName("Y");
        peer.commitChangesToParent();

        new ThreadedTestHelper() {

            protected void assertResult() throws Exception {
                assertFalse(listener.graphCommitted);
                assertTrue(listener.graphChanged);
                assertFalse(listener.graphRolledBack);
            }
        }.assertWithTimeout(1000);
    }

    public void testChangeEventOnPeerChangeSecondNestingLevel() throws Exception {
        DataContext context = createDataContext();
        DataContext childPeer1 = context.createChildDataContext();

        Artist a = (Artist) childPeer1.newObject(Artist.class);
        a.setArtistName("X");
        childPeer1.commitChanges();

        final MockChannelListener listener = new MockChannelListener();
        EventUtil.listenForChannelEvents(childPeer1, listener);

        DataContext childPeer2 = context.createChildDataContext();

        Artist a1 = (Artist) childPeer2.localObject(a.getObjectId(), a);

        a1.setArtistName("Y");
        childPeer2.commitChangesToParent();

        new ThreadedTestHelper() {

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
