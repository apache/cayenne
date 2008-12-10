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
import org.apache.art.Painting;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.util.ThreadedTestHelper;

public class NestedDataContextParentPeerEventsTest extends CayenneCase {

    public void testPeerObjectUpdatedSimpleProperty() throws Exception {
        DataContext context = createDataContext();

        Artist a = context.newObject(Artist.class);
        a.setArtistName("X");
        context.commitChanges();

        DataContext parentPeer = context.getParentDataDomain().createDataContext();
        Artist a1 = (Artist) parentPeer.localObject(a.getObjectId(), a);

        final ObjectContext peer2 = context.createChildContext();
        final Artist a2 = (Artist) peer2.localObject(a.getObjectId(), a);

        a1.setArtistName("Y");
        assertEquals("X", a2.getArtistName());
        parentPeer.commitChangesToParent();

        new ThreadedTestHelper() {

            @Override
            protected void assertResult() throws Exception {
                assertEquals("Y", a2.getArtistName());

                assertFalse("Peer data context became dirty on event processing", peer2
                        .hasChanges());
            }
        }.assertWithTimeout(2000);
    }

    public void testPeerObjectUpdatedToOneRelationship() throws Exception {

        DataContext context = createDataContext();

        Artist a = context.newObject(Artist.class);
        Artist altA = context.newObject(Artist.class);

        Painting p = context.newObject(Painting.class);
        p.setToArtist(a);
        p.setPaintingTitle("PPP");
        a.setArtistName("X");
        altA.setArtistName("Y");
        context.commitChanges();

        DataContext parentPeer = context.getParentDataDomain().createDataContext();
        Painting p1 = (Painting) parentPeer.localObject(p.getObjectId(), p);
        Artist altA1 = (Artist) parentPeer.localObject(altA.getObjectId(), altA);

        final ObjectContext peer2 = context.createChildContext();
        final Painting p2 = (Painting) peer2.localObject(p.getObjectId(), p);
        final Artist altA2 = (Artist) peer2.localObject(altA.getObjectId(), altA);
        Artist a2 = (Artist) peer2.localObject(a.getObjectId(), a);

        p1.setToArtist(altA1);
        assertSame(a2, p2.getToArtist());
        assertNotSame(altA2, p2.getToArtist());
        parentPeer.commitChangesToParent();

        new ThreadedTestHelper() {

            @Override
            protected void assertResult() throws Exception {
                assertSame(altA2, p2.getToArtist());
                assertFalse("Peer data context became dirty on event processing", peer2
                        .hasChanges());
            }
        }.assertWithTimeout(2000);
    }

    public void testPeerObjectUpdatedToManyRelationship() throws Exception {

        DataContext context = createDataContext();

        Artist a = context.newObject(Artist.class);
        a.setArtistName("X");

        Painting px = context.newObject(Painting.class);
        px.setToArtist(a);
        px.setPaintingTitle("PX");

        Painting py = context.newObject(Painting.class);
        py.setPaintingTitle("PY");

        context.commitChanges();

        DataContext parentPeer = context.getParentDataDomain().createDataContext();
        Painting py1 = (Painting) parentPeer.localObject(py.getObjectId(), py);
        Artist a1 = (Artist) parentPeer.localObject(a.getObjectId(), a);

        final ObjectContext peer2 = context.createChildContext();
        final Painting py2 = (Painting) peer2.localObject(py.getObjectId(), py);
        final Artist a2 = (Artist) peer2.localObject(a.getObjectId(), a);

        a1.addToPaintingArray(py1);
        assertEquals(1, a2.getPaintingArray().size());
        assertFalse(a2.getPaintingArray().contains(py2));
        parentPeer.commitChangesToParent();

        new ThreadedTestHelper() {

            @Override
            protected void assertResult() throws Exception {
                assertEquals(2, a2.getPaintingArray().size());
                assertTrue(a2.getPaintingArray().contains(py2));

                assertFalse("Peer data context became dirty on event processing", peer2
                        .hasChanges());
            }
        }.assertWithTimeout(2000);
    }
}
