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

package org.apache.cayenne;

import java.util.Collections;
import java.util.List;

import org.apache.art.Artist;
import org.apache.art.Painting;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CaseDataFactory;

public class CayenneDataObjectInCtxtTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testDoubleRegistration() {
        DataContext context = createDataContext();

        DataObject object = new Artist();
        assertNull(object.getObjectId());

        context.registerNewObject(object);
        ObjectId tempID = object.getObjectId();
        assertNotNull(tempID);
        assertTrue(tempID.isTemporary());
        assertSame(context, object.getObjectContext());

        // double registration in the same context should be quietly ignored
        context.registerNewObject(object);
        assertSame(tempID, object.getObjectId());
        assertSame(object, context.getGraphManager().getNode(tempID));

        // registering in another context should throw an exception
        DataContext anotherContext = createDataContext();
        try {
            anotherContext.registerNewObject(object);
            fail("registerNewObject should've failed - object is already in another context");
        }
        catch (IllegalStateException e) {
            // expected
        }
    }

    public void testObjEntity() {
        DataContext context = createDataContext();

        Artist a = new Artist();
        assertNull(a.getObjEntity());

        context.registerNewObject(a);
        ObjEntity e = a.getObjEntity();
        assertNotNull(e);
        assertEquals("Artist", e.getName());

        Painting p = new Painting();
        assertNull(p.getObjEntity());

        context.registerNewObject(p);
        ObjEntity e1 = p.getObjEntity();
        assertNotNull(e1);
        assertEquals("Painting", e1.getName());
    }

    public void testCommitChangesInBatch() {
        DataContext context = createDataContext();

        Artist a1 = (Artist) context.newObject("Artist");
        a1.setArtistName("abc1");

        Artist a2 = (Artist) context.newObject("Artist");
        a2.setArtistName("abc2");

        Artist a3 = (Artist) context.newObject("Artist");
        a3.setArtistName("abc3");

        context.commitChanges();

        List artists = context.performQuery(new SelectQuery(Artist.class));
        assertEquals(3, artists.size());
    }

    public void testSetObjectId() {
        DataContext context = createDataContext();

        Artist o1 = new Artist();
        assertNull(o1.getObjectId());

        context.registerNewObject(o1);
        assertNotNull(o1.getObjectId());
    }

    public void testStateTransToNew() {
        DataContext context = createDataContext();
        Artist o1 = new Artist();
        assertEquals(PersistenceState.TRANSIENT, o1.getPersistenceState());

        context.registerNewObject(o1);
        assertEquals(PersistenceState.NEW, o1.getPersistenceState());
    }

    public void testStateNewToCommitted() {
        DataContext context = createDataContext();

        Artist o1 = new Artist();
        o1.setArtistName("a");

        context.registerNewObject(o1);
        assertEquals(PersistenceState.NEW, o1.getPersistenceState());

        context.commitChanges();
        assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
    }

    public void testStateCommittedToModified() {
        DataContext context = createDataContext();
        Artist o1 = new Artist();
        o1.setArtistName("a");
        context.registerNewObject(o1);
        context.commitChanges();
        assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());

        o1.setArtistName(o1.getArtistName() + "_1");
        assertEquals(PersistenceState.MODIFIED, o1.getPersistenceState());
    }

    public void testStateModifiedToCommitted() {
        DataContext context = createDataContext();

        Artist o1 = newSavedArtist(context);
        o1.setArtistName(o1.getArtistName() + "_1");
        assertEquals(PersistenceState.MODIFIED, o1.getPersistenceState());

        context.commitChanges();
        assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
    }

    public void testStateCommittedToDeleted() {
        DataContext context = createDataContext();

        Artist o1 = new Artist();
        o1.setArtistName("a");
        context.registerNewObject(o1);
        context.commitChanges();
        assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());

        context.deleteObject(o1);
        assertEquals(PersistenceState.DELETED, o1.getPersistenceState());
    }

    public void testStateDeletedToTransient() {
        DataContext context = createDataContext();

        Artist o1 = newSavedArtist(context);
        context.deleteObject(o1);
        assertEquals(PersistenceState.DELETED, o1.getPersistenceState());

        context.commitChanges();
        assertEquals(PersistenceState.TRANSIENT, o1.getPersistenceState());
        assertFalse(context.getGraphManager().registeredNodes().contains(o1));
        assertNull(o1.getObjectContext());
    }

    public void testSetDataContext() {
        DataContext context = createDataContext();

        Artist o1 = new Artist();
        assertNull(o1.getObjectContext());

        context.registerNewObject(o1);
        assertSame(context, o1.getObjectContext());
    }

    public void testFetchByAttr() throws Exception {
        DataContext context = createDataContext();

        String artistName = "artist with one painting";
        CaseDataFactory.createArtistWithPainting(artistName, new String[] {}, false);

        SelectQuery q = new SelectQuery("Artist", ExpressionFactory.matchExp(
                "artistName",
                artistName));

        List artists = context.performQuery(q);
        assertEquals(1, artists.size());
        Artist o1 = (Artist) artists.get(0);
        assertNotNull(o1);
        assertEquals(artistName, o1.getArtistName());
    }

    public void testUniquing() throws Exception {
        DataContext context = createDataContext();

        String artistName = "unique artist with no paintings";
        CaseDataFactory.createArtistWithPainting(artistName, new String[] {}, false);

        Artist a1 = fetchArtist(context, artistName);
        Artist a2 = fetchArtist(context, artistName);

        assertNotNull(a1);
        assertNotNull(a2);
        assertEquals(1, context.getGraphManager().registeredNodes().size());
        assertSame(a1, a2);
    }

    public void testSnapshotVersion1() {
        DataContext context = createDataContext();

        Artist artist = (Artist) context.newObject("Artist");
        assertEquals(DataObject.DEFAULT_VERSION, artist.getSnapshotVersion());

        // test versions set on commit

        artist.setArtistName("abc");
        context.commitChanges();

        DataRow cachedSnapshot = context.getObjectStore().getCachedSnapshot(
                artist.getObjectId());

        assertNotNull(cachedSnapshot);
        assertEquals(cachedSnapshot.getVersion(), artist.getSnapshotVersion());
    }

    public void testSnapshotVersion2() {
        DataContext context = createDataContext();

        newSavedArtist(context);

        // test versions assigned on fetch... clean up domain cache
        // before doing it
        getDomain().getEventManager().removeAllListeners(
                getDomain().getSharedSnapshotCache().getSnapshotEventSubject());
        getDomain().getSharedSnapshotCache().clear();
        context = getDomain().createDataContext();

        List artists = context.performQuery(new SelectQuery(Artist.class));
        Artist artist = (Artist) artists.get(0);

        assertFalse(DataObject.DEFAULT_VERSION == artist.getSnapshotVersion());
        assertEquals(context
                .getObjectStore()
                .getCachedSnapshot(artist.getObjectId())
                .getVersion(), artist.getSnapshotVersion());
    }

    public void testSnapshotVersion3() {
        DataContext context = createDataContext();

        Artist artist = newSavedArtist(context);

        // test versions assigned after update
        long oldVersion = artist.getSnapshotVersion();

        artist.setArtistName(artist.getArtistName() + "---");
        context.commitChanges();

        assertFalse(oldVersion == artist.getSnapshotVersion());
        assertEquals(context
                .getObjectStore()
                .getCachedSnapshot(artist.getObjectId())
                .getVersion(), artist.getSnapshotVersion());
    }

    /**
     * Tests a condition when user substitutes object id of a new object instead of
     * setting replacement. This is demonstrated here -
     * http://objectstyle.org/cayenne/lists/cayenne-user/2005/01/0210.html
     */
    public void testObjectsCommittedManualOID() {
        DataContext context = createDataContext();

        Artist object = context.newObject(Artist.class);
        object.setArtistName("ABC1");
        assertEquals(PersistenceState.NEW, object.getPersistenceState());

        // do a manual id substitution
        object.setObjectId(new ObjectId(
                "Artist",
                Artist.ARTIST_ID_PK_COLUMN,
                new Integer(3)));

        context.commitChanges();
        assertEquals(PersistenceState.COMMITTED, object.getPersistenceState());

        // refetch
        context.invalidateObjects(Collections.singleton(object));

        Artist object2 = DataObjectUtils.objectForPK(context, Artist.class, 3);
        assertNotNull(object2);
        assertEquals("ABC1", object2.getArtistName());
    }

    private Artist newSavedArtist(DataContext context) {
        Artist o1 = new Artist();
        o1.setArtistName("a");
        o1.setDateOfBirth(new java.sql.Date(System.currentTimeMillis()));
        context.registerNewObject(o1);
        context.commitChanges();
        return o1;
    }

    private Artist fetchArtist(DataContext context, String name) {
        SelectQuery q = new SelectQuery("Artist", ExpressionFactory.matchExp(
                "artistName",
                name));
        List ats = context.performQuery(q);
        return (ats.size() > 0) ? (Artist) ats.get(0) : null;
    }
}
