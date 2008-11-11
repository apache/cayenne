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

import java.util.Collections;
import java.util.Date;

import org.apache.art.Artist;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.unit.CayenneCase;

/**
 * Tests objects registration in DataContext, transferring objects between contexts and
 * such.
 * 
 */
public class DataContextObjectTrackingTest extends CayenneCase {

    public void testUnregisterObject() {

        DataContext context = createDataContext();

        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(1));
        row.put("ARTIST_NAME", "ArtistXYZ");
        row.put("DATE_OF_BIRTH", new Date());
        DataObject obj = context.objectFromDataRow(Artist.class, row, false);
        ObjectId oid = obj.getObjectId();

        assertEquals(PersistenceState.COMMITTED, obj.getPersistenceState());
        assertSame(context, obj.getObjectContext());
        assertSame(obj, context.getGraphManager().getNode(oid));

        context.unregisterObjects(Collections.singletonList(obj));

        assertEquals(PersistenceState.TRANSIENT, obj.getPersistenceState());
        assertNull(obj.getObjectContext());
        assertNull(obj.getObjectId());
        assertNull(context.getGraphManager().getNode(oid));
        assertNull(context.getObjectStore().getCachedSnapshot(oid));
    }

    public void testInvalidateObject() {
        DataContext context = createDataContext();

        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(1));
        row.put("ARTIST_NAME", "ArtistXYZ");
        row.put("DATE_OF_BIRTH", new Date());
        DataObject obj = context.objectFromDataRow(Artist.class, row, false);
        ObjectId oid = obj.getObjectId();

        assertEquals(PersistenceState.COMMITTED, obj.getPersistenceState());
        assertSame(context, obj.getObjectContext());
        assertSame(obj, context.getGraphManager().getNode(oid));

        context.invalidateObjects(Collections.singletonList(obj));

        assertEquals(PersistenceState.HOLLOW, obj.getPersistenceState());
        assertSame(context, obj.getObjectContext());
        assertSame(oid, obj.getObjectId());
        assertNull(context.getObjectStore().getCachedSnapshot(oid));
        assertNotNull(context.getGraphManager().getNode(oid));
    }

    public void testLocalObjectPeerContextMap() throws Exception {
        deleteTestData();
        createTestData("testArtists");

        // must create both contexts before running the queries, as each call to
        // 'createDataContext' clears the cache.
        DataContext context = createDataContext();
        DataContext peerContext = createDataContext();

        Persistent _new = context.newObject(Artist.class);

        Persistent hollow = context.localObject(new ObjectId(
                "Artist",
                Artist.ARTIST_ID_PK_COLUMN,
                33001), null);
        DataObject committed = (DataObject) DataObjectUtils.objectForQuery(
                context,
                new ObjectIdQuery(new ObjectId(
                        "Artist",
                        Artist.ARTIST_ID_PK_COLUMN,
                        33002)));

        int modifiedId = 33003;
        Artist modified = (Artist) DataObjectUtils.objectForQuery(
                context,
                new ObjectIdQuery(new ObjectId(
                        "Artist",
                        Artist.ARTIST_ID_PK_COLUMN,
                        modifiedId)));
        modified.setArtistName("MODDED");
        DataObject deleted = (DataObject) DataObjectUtils.objectForQuery(
                context,
                new ObjectIdQuery(new ObjectId(
                        "Artist",
                        Artist.ARTIST_ID_PK_COLUMN,
                        33004)));
        context.deleteObject(deleted);

        assertEquals(PersistenceState.HOLLOW, hollow.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, committed.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, modified.getPersistenceState());
        assertEquals(PersistenceState.DELETED, deleted.getPersistenceState());
        assertEquals(PersistenceState.NEW, _new.getPersistenceState());

        // now check how objects in different state behave

        // on the one hand sticking an alien NEW object to a peer DataContext doesn't look
        // like a good idea, on the other hand the code detecting whether a given context
        // is a child of another context breaks DataChannel encapsulation (i.e. using
        // "instanceof" to check DataChannel type will make it impossible to use Proxies).

        // try {
        // peerContext.localObjects(Collections.singletonList(_new));
        // fail("A presence of new object should have triggered an exception");
        // }
        // catch (CayenneRuntimeException e) {
        // // expected
        // }

        blockQueries();

        try {

            Persistent hollowPeer = peerContext.localObject(hollow.getObjectId(), null);
            assertEquals(PersistenceState.HOLLOW, hollowPeer.getPersistenceState());
            assertEquals(hollow.getObjectId(), hollowPeer.getObjectId());
            assertSame(peerContext, hollowPeer.getObjectContext());
            assertSame(context, hollow.getObjectContext());

            Persistent committedPeer = peerContext.localObject(
                    committed.getObjectId(),
                    null);
            assertEquals(PersistenceState.HOLLOW, committedPeer.getPersistenceState());
            assertEquals(committed.getObjectId(), committedPeer.getObjectId());
            assertSame(peerContext, committedPeer.getObjectContext());
            assertSame(context, committed.getObjectContext());

            Persistent modifiedPeer = peerContext.localObject(
                    modified.getObjectId(),
                    null);
            assertEquals(PersistenceState.HOLLOW, modifiedPeer.getPersistenceState());
            assertEquals(modified.getObjectId(), modifiedPeer.getObjectId());
            assertSame(peerContext, modifiedPeer.getObjectContext());
            assertSame(context, modified.getObjectContext());

            Persistent deletedPeer = peerContext.localObject(deleted.getObjectId(), null);
            assertEquals(PersistenceState.HOLLOW, deletedPeer.getPersistenceState());
            assertEquals(deleted.getObjectId(), deletedPeer.getObjectId());
            assertSame(peerContext, deletedPeer.getObjectContext());
            assertSame(context, deleted.getObjectContext());
        }
        finally {
            unblockQueries();
        }
    }

    public void testLocalObjectPeerContextNoOverride() throws Exception {
        deleteTestData();
        createTestData("testArtists");

        // must create both contexts before running the queries, as each call to
        // 'createDataContext' clears the cache.
        DataContext context = createDataContext();
        DataContext peerContext = createDataContext();

        int modifiedId = 33003;
        Artist modified = (Artist) DataObjectUtils.objectForQuery(
                context,
                new ObjectIdQuery(new ObjectId(
                        "Artist",
                        Artist.ARTIST_ID_PK_COLUMN,
                        modifiedId)));
        Artist peerModified = (Artist) DataObjectUtils.objectForQuery(
                peerContext,
                new ObjectIdQuery(new ObjectId(
                        "Artist",
                        Artist.ARTIST_ID_PK_COLUMN,
                        modifiedId)));

        modified.setArtistName("M1");
        peerModified.setArtistName("M2");

        assertEquals(PersistenceState.MODIFIED, modified.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, peerModified.getPersistenceState());

        blockQueries();

        try {

            Persistent peerModified2 = peerContext.localObject(
                    modified.getObjectId(),
                    null);

            assertSame(peerModified, peerModified2);
            assertEquals(PersistenceState.MODIFIED, peerModified2.getPersistenceState());
            assertEquals("M2", peerModified.getArtistName());
            assertEquals("M1", modified.getArtistName());
        }
        finally {
            unblockQueries();
        }
    }
}
