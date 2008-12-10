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
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.unit.CayenneCase;

public class NestedDataContextPeerEventsTest extends CayenneCase {

    public void testPeerObjectUpdatedTempOID() {
        DataContext context = createDataContext();

        ObjectContext peer1 = context.createChildContext();
        Artist a1 = peer1.newObject(Artist.class);
        a1.setArtistName("Y");
        ObjectId a1TempId = a1.getObjectId();

        ObjectContext peer2 = context.createChildContext();
        Artist a2 = (Artist) peer2.localObject(a1TempId, a1);

        assertEquals(a1TempId, a2.getObjectId());

        peer1.commitChanges();
        assertFalse(a1.getObjectId().isTemporary());
        assertFalse(a2.getObjectId().isTemporary());
        assertEquals(a2.getObjectId(), a1.getObjectId());
    }

    public void testPeerObjectUpdatedSimpleProperty() {
        DataContext context = createDataContext();

        Artist a = context.newObject(Artist.class);
        a.setArtistName("X");
        context.commitChanges();

        ObjectContext peer1 = context.createChildContext();
        Artist a1 = (Artist) peer1.localObject(a.getObjectId(), a);

        ObjectContext peer2 = context.createChildContext();
        Artist a2 = (Artist) peer2.localObject(a.getObjectId(), a);

        a1.setArtistName("Y");
        assertEquals("X", a2.getArtistName());
        peer1.commitChangesToParent();
        assertEquals("Y", a2.getArtistName());

        assertFalse("Peer data context became dirty on event processing", peer2
                .hasChanges());
    }

    public void testPeerObjectUpdatedToOneRelationship() {

        DataContext context = createDataContext();

        Artist a = context.newObject(Artist.class);
        Artist altA = context.newObject(Artist.class);

        Painting p = context.newObject(Painting.class);
        p.setToArtist(a);
        p.setPaintingTitle("PPP");
        a.setArtistName("X");
        altA.setArtistName("Y");
        context.commitChanges();

        ObjectContext peer1 = context.createChildContext();
        Painting p1 = (Painting) peer1.localObject(p.getObjectId(), p);
        Artist altA1 = (Artist) peer1.localObject(altA.getObjectId(), altA);

        ObjectContext peer2 = context.createChildContext();
        Painting p2 = (Painting) peer2.localObject(p.getObjectId(), p);
        Artist altA2 = (Artist) peer2.localObject(altA.getObjectId(), altA);
        Artist a2 = (Artist) peer2.localObject(a.getObjectId(), a);

        p1.setToArtist(altA1);
        assertSame(a2, p2.getToArtist());
        peer1.commitChangesToParent();
        assertEquals(altA2, p2.getToArtist());

        assertFalse("Peer data context became dirty on event processing", peer2
                .hasChanges());
    }

    public void testPeerObjectUpdatedToManyRelationship() {

        DataContext context = createDataContext();

        Artist a = context.newObject(Artist.class);
        a.setArtistName("X");

        Painting px = context.newObject(Painting.class);
        px.setToArtist(a);
        px.setPaintingTitle("PX");

        Painting py = context.newObject(Painting.class);
        py.setPaintingTitle("PY");

        context.commitChanges();

        ObjectContext peer1 = context.createChildContext();
        Painting py1 = (Painting) peer1.localObject(py.getObjectId(), py);
        Artist a1 = (Artist) peer1.localObject(a.getObjectId(), a);

        ObjectContext peer2 = context.createChildContext();
        Painting py2 = (Painting) peer2.localObject(py.getObjectId(), py);
        Artist a2 = (Artist) peer2.localObject(a.getObjectId(), a);

        a1.addToPaintingArray(py1);
        assertEquals(1, a2.getPaintingArray().size());
        assertFalse(a2.getPaintingArray().contains(py2));
        peer1.commitChangesToParent();
        assertEquals(2, a2.getPaintingArray().size());
        assertTrue(a2.getPaintingArray().contains(py2));

        assertFalse("Peer data context became dirty on event processing", peer2
                .hasChanges());
    }
}
