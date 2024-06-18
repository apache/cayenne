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

package org.apache.cayenne;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class CayennePersistentObjectInContextIT extends RuntimeCase {

    @Inject
    protected CayenneRuntime runtime;

    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tArtist;

    @Before
    public void setUp() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");
    }

    @Test
    public void testDoubleRegistration() {

        Persistent object = new Artist();
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
        ObjectContext anotherContext = runtime.newContext();
        try {
            anotherContext.registerNewObject(object);
            fail("registerNewObject should've failed - object is already in another context");
        }
        catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void testCommitChangesInBatch() {

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("abc1");

        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("abc2");

        Artist a3 = context.newObject(Artist.class);
        a3.setArtistName("abc3");

        context.commitChanges();

        List<Artist> artists = ObjectSelect.query(Artist.class).select(context);
        assertEquals(3, artists.size());
    }

    @Test
    public void testSetObjectId() {

        Artist o1 = new Artist();
        assertNull(o1.getObjectId());

        context.registerNewObject(o1);
        assertNotNull(o1.getObjectId());
    }

    @Test
    public void testStateTransToNew() {

        Artist o1 = new Artist();
        assertEquals(PersistenceState.TRANSIENT, o1.getPersistenceState());

        context.registerNewObject(o1);
        assertEquals(PersistenceState.NEW, o1.getPersistenceState());
    }

    @Test
    public void testStateNewToCommitted() {

        Artist o1 = new Artist();
        o1.setArtistName("a");

        context.registerNewObject(o1);
        assertEquals(PersistenceState.NEW, o1.getPersistenceState());

        context.commitChanges();
        assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
    }

    @Test
    public void testStateCommittedToModified() {

        Artist o1 = new Artist();
        o1.setArtistName("a");
        context.registerNewObject(o1);
        context.commitChanges();
        assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());

        o1.setArtistName(o1.getArtistName() + "_1");
        assertEquals(PersistenceState.MODIFIED, o1.getPersistenceState());
    }

    @Test
    public void testStateModifiedToCommitted() {

        Artist o1 = context.newObject(Artist.class);
        o1.setArtistName("qY");
        context.commitChanges();

        o1.setArtistName(o1.getArtistName() + "_1");
        assertEquals(PersistenceState.MODIFIED, o1.getPersistenceState());

        context.commitChanges();
        assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
    }

    @Test
    public void testStateCommittedToDeleted() {

        Artist o1 = new Artist();
        o1.setArtistName("a");
        context.registerNewObject(o1);
        context.commitChanges();
        assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());

        context.deleteObjects(o1);
        assertEquals(PersistenceState.DELETED, o1.getPersistenceState());
    }

    @Test
    public void testStateDeletedToTransient() {

        Artist o1 = context.newObject(Artist.class);
        o1.setArtistName("qY");
        context.commitChanges();

        context.deleteObjects(o1);
        assertEquals(PersistenceState.DELETED, o1.getPersistenceState());

        context.commitChanges();
        assertEquals(PersistenceState.TRANSIENT, o1.getPersistenceState());
        assertFalse(context.getGraphManager().registeredNodes().contains(o1));
        assertNull(o1.getObjectContext());
    }

    @Test
    public void testSetContext() {

        Artist o1 = new Artist();
        assertNull(o1.getObjectContext());

        context.registerNewObject(o1);
        assertSame(context, o1.getObjectContext());
    }

    @Test
    public void testFetchByAttribute() throws Exception {

        tArtist.insert(7, "m6");

        List<Artist> artists = ObjectSelect.query(Artist.class, Artist.ARTIST_NAME.eq("m6")).select(context);
        assertEquals(1, artists.size());
        Artist o1 = artists.get(0);
        assertNotNull(o1);
        assertEquals("m6", o1.getArtistName());
    }

    @Test
    public void testUniquing() throws Exception {

        tArtist.insert(7, "m6");

        Artist a1 = (Artist) Cayenne.objectForQuery(context, ObjectSelect.query(Artist.class, Artist.ARTIST_NAME.eq("m6")));
        Artist a2 = (Artist) Cayenne.objectForQuery(context, ObjectSelect.query(Artist.class, Artist.ARTIST_NAME.eq("m6")));

        assertNotNull(a1);
        assertNotNull(a2);
        assertEquals(1, context.getGraphManager().registeredNodes().size());
        assertSame(a1, a2);
    }

    @Test
    public void testSnapshotVersion1() {

        Artist artist = context.newObject(Artist.class);
        assertEquals(Persistent.DEFAULT_VERSION, artist.getSnapshotVersion());

        // test versions set on commit

        artist.setArtistName("abc");
        context.commitChanges();

        DataRow cachedSnapshot = context.getObjectStore().getCachedSnapshot(
                artist.getObjectId());

        assertNotNull(cachedSnapshot);
        assertEquals(cachedSnapshot.getVersion(), artist.getSnapshotVersion());
    }

    @Test
    public void testSnapshotVersion2() throws Exception {

        tArtist.insert(7, "m6");

        // test versions assigned on fetch... clean up domain cache
        Artist artist = ObjectSelect.query(Artist.class).selectFirst(context);

        assertNotEquals(Persistent.DEFAULT_VERSION, artist.getSnapshotVersion());
        assertEquals(context
                .getObjectStore()
                .getCachedSnapshot(artist.getObjectId())
                .getVersion(), artist.getSnapshotVersion());
    }

    @Test
    public void testSnapshotVersion3() {

        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("qY");
        context.commitChanges();

        // test versions assigned after update
        long oldVersion = artist.getSnapshotVersion();

        artist.setArtistName(artist.getArtistName() + "---");
        context.commitChanges();

        assertNotEquals(oldVersion, artist.getSnapshotVersion());
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
    @Test
    public void testObjectsCommittedManualOID() {

        Artist object = context.newObject(Artist.class);
        object.setArtistName("ABC1");
        assertEquals(PersistenceState.NEW, object.getPersistenceState());

        // do a manual id substitution
        object.setObjectId(ObjectId.of(
                "Artist",
                Artist.ARTIST_ID_PK_COLUMN,
                3));

        context.commitChanges();
        assertEquals(PersistenceState.COMMITTED, object.getPersistenceState());

        // refetch
        context.invalidateObjects(object);

        Artist object2 = Cayenne.objectForPK(context, Artist.class, 3);
        assertNotNull(object2);
        assertEquals("ABC1", object2.getArtistName());
    }

}
