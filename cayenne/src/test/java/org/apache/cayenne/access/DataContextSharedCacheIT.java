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

package org.apache.cayenne.access;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.util.RuntimeCaseSyncModule;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.test.parallel.ParallelTestContainer;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.ExtraModules;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.apache.cayenne.unit.util.SQLTemplateCustomizer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test suite for testing behavior of multiple DataContexts that share the same underlying
 * DataDomain.
 */
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
@ExtraModules(RuntimeCaseSyncModule.class)
public class DataContextSharedCacheIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Inject
    private DataContext context1;

    @Inject
    private SQLTemplateCustomizer sqlTemplateCustomizer;

    private Artist artist;

    @Before
    public void setUp() throws Exception {
        // prepare a single artist record
        artist = (Artist) context.newObject("Artist");
        artist.setArtistName("version1");
        artist.setDateOfBirth(new Date());
        context.commitChanges();
    }

    /**
     * Test case to prove that refreshing snapshots as a result of the database fetch will
     * be propagated across DataContexts.
     */
    @Test
    public void testSnapshotChangePropagationOnSelect() throws Exception {
        String originalName = artist.getArtistName();
        final String newName = "version2";

        // update artist using raw SQL
        SQLTemplate query = sqlTemplateCustomizer.createSQLTemplate(
                Artist.class,
                "UPDATE ARTIST SET ARTIST_NAME = #bind($newName) "
                        + "WHERE ARTIST_NAME = #bind($oldName)");

        Map<String, Object> map = new HashMap<>(3);
        map.put("newName", newName);
        map.put("oldName", originalName);
        query.setParams(map);
        context.performNonSelectingQuery(query);

        // fetch updated artist into the new context, and see if the original
        // one gets updated
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq(newName))
                .select(context);
        assertEquals(1, artists.size());
        Artist altArtist = artists.get(0);

        // check underlying cache
        DataRow freshSnapshot = context
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(altArtist.getObjectId());
        assertNotNull(freshSnapshot);
        assertEquals(newName, freshSnapshot.get("ARTIST_NAME"));

        // check both artists
        assertEquals(newName, altArtist.getArtistName());

        ParallelTestContainer helper = new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertEquals(
                        "Peer object state wasn't refreshed on fetch",
                        newName,
                        artist.getArtistName());
            }
        };
        helper.runTest(3000);
    }

    /**
     * Test case to prove that changes made to an object in one ObjectStore and committed
     * to the database will be reflected in the peer ObjectStore using the same
     * DataRowCache.
     */
    @Test
    public void testSnapshotChangePropagation() throws Exception {
        String originalName = artist.getArtistName();
        final String newName = "version2";

        // make sure we have a fully resolved copy of an artist object
        // in the second context
        final Artist altArtist = context1.localObject(artist);
        assertFalse(altArtist == artist);
        assertEquals(originalName, altArtist.getArtistName());
        assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());

        // Update Artist
        artist.setArtistName(newName);

        // no changes propagated till commit...
        assertEquals(originalName, altArtist.getArtistName());
        context.commitChanges();

        // check underlying cache
        DataRow freshSnapshot = context
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(altArtist.getObjectId());
        assertEquals(newName, freshSnapshot.get("ARTIST_NAME"));

        // check peer artist
        ParallelTestContainer helper = new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertEquals(newName, altArtist.getArtistName());
            }
        };
        helper.runTest(3000);
    }

    /**
     * Test case to prove that changes made to an object in one ObjectStore and committed
     * to the database will be correctly merged in the peer ObjectStore using the same
     * DataRowCache. E.g. modified objects will be merged so that no new changes are lost.
     */
    @Test
    public void testSnapshotChangePropagationToModifiedObjects() throws Exception {
        String originalName = artist.getArtistName();
        Date originalDate = artist.getDateOfBirth();
        String newName = "version2";
        final Date newDate = new Date(originalDate.getTime() - 10000);
        final String newAltName = "version3";

        // make sure we have a fully resolved copy of an artist object
        // in the second context
        final Artist altArtist = context1.localObject(artist);
        assertNotNull(altArtist);
        assertFalse(altArtist == artist);
        assertEquals(originalName, altArtist.getArtistName());
        assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());

        // Update Artist peers independently
        artist.setArtistName(newName);
        artist.setDateOfBirth(newDate);
        altArtist.setArtistName(newAltName);

        context.commitChanges();

        // check underlying cache
        DataRow freshSnapshot = context
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(altArtist.getObjectId());
        assertEquals(newName, freshSnapshot.get("ARTIST_NAME"));
        assertEquals(newDate, freshSnapshot.get("DATE_OF_BIRTH"));

        // check peer artist
        ParallelTestContainer helper = new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertEquals(newAltName, altArtist.getArtistName());
                assertEquals(newDate, altArtist.getDateOfBirth());
                assertEquals(PersistenceState.MODIFIED, altArtist.getPersistenceState());
            }
        };
        helper.runTest(3000);

    }

    /**
     * Test case to prove that deleting an object in one ObjectStore and committing to the
     * database will be reflected in the peer ObjectStore using the same DataRowCache. By
     * default COMMITTED objects will be changed to TRANSIENT.
     */
    @Test
    public void testSnapshotDeletePropagationToCommitted() throws Exception {

        // make sure we have a fully resolved copy of an artist object
        // in the second context
        final Artist altArtist = context1.localObject(artist);
        assertNotNull(altArtist);
        assertFalse(altArtist == artist);
        assertEquals(artist.getArtistName(), altArtist.getArtistName());
        assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());

        // Update Artist
        context.deleteObjects(artist);
        context.commitChanges();

        // check underlying cache
        assertNull(context
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(altArtist.getObjectId()));

        // check peer artist
        ParallelTestContainer helper = new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertEquals(PersistenceState.TRANSIENT, altArtist.getPersistenceState());
                assertNull(altArtist.getObjectContext());
            }
        };
        helper.runTest(3000);
    }

    /**
     * Test case to prove that deleting an object in one ObjectStore and committed to the
     * database will be reflected in the peer ObjectStore using the same DataRowCache. By
     * default HOLLOW objects will be changed to TRANSIENT.
     */
    @Test
    public void testSnapshotDeletePropagationToHollow() throws Exception {

        final Artist altArtist = context1.localObject(artist);

        assertNotNull(altArtist);
        assertFalse(altArtist == artist);
        assertEquals(PersistenceState.HOLLOW, altArtist.getPersistenceState());

        // Update Artist
        context.deleteObjects(artist);
        context.commitChanges();

        // check underlying cache
        assertNull(context
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(altArtist.getObjectId()));

        // check peer artist
        ParallelTestContainer helper = new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertEquals(PersistenceState.TRANSIENT, altArtist.getPersistenceState());
                assertNull(altArtist.getObjectContext());
            }
        };
        helper.runTest(3000);
    }

    /**
     * Test case to prove that deleting an object in one ObjectStore and committed to the
     * database will be reflected in the peer ObjectStore using the same DataRowCache. By
     * default MODIFIED objects will be changed to NEW.
     */
    @Test
    public void testSnapshotDeletePropagationToModified() throws Exception {

        // make sure we have a fully resolved copy of an artist object
        // in the second context
        final Artist altArtist = context1.localObject(artist);
        altArtist.getArtistName();
        assertNotNull(altArtist);
        assertFalse(altArtist == artist);

        // modify peer
        altArtist.setArtistName("version2");
        assertEquals(PersistenceState.MODIFIED, altArtist.getPersistenceState());

        // Update Artist
        context.deleteObjects(artist);
        context.commitChanges();

        // check underlying cache
        assertNull(context
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(altArtist.getObjectId()));

        // check peer artist
        ParallelTestContainer helper = new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertEquals(PersistenceState.NEW, altArtist.getPersistenceState());
            }
        };
        helper.runTest(3000);

        // check if now we can save this object again, and with the original
        // ObjectId
        ObjectId id = altArtist.getObjectId();
        assertNotNull(id);
        assertNotNull(id.getIdSnapshot().get(Artist.ARTIST_ID_PK_COLUMN));
        assertFalse(id.isTemporary());

        context1.commitChanges();

        assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());
    }

    /**
     * Test case to prove that deleting an object in one ObjectStore and committing to the
     * database will be reflected in the peer ObjectStore using the same DataRowCache. By
     * default DELETED objects will be changed to TRANSIENT.
     */
    @Test
    public void testSnapshotDeletePropagationToDeleted() throws Exception {

        // make sure we have a fully resolved copy of an artist object
        // in the second context
        final Artist altArtist = context1.localObject(artist);
        altArtist.getArtistName();
        assertNotNull(altArtist);
        assertFalse(altArtist == artist);

        // delete peer
        context1.deleteObjects(altArtist);

        // Update Artist
        context.deleteObjects(artist);
        context.commitChanges();

        // check underlying cache
        assertNull(context
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(altArtist.getObjectId()));

        // check peer artist
        ParallelTestContainer helper = new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertEquals(PersistenceState.TRANSIENT, altArtist.getPersistenceState());
                assertNull(altArtist.getObjectContext());
            }
        };
        helper.runTest(3000);

        assertFalse(context1.hasChanges());
    }

    /**
     * Test case to prove that deleting an object in one ObjectStore and committing to the
     * database will be reflected in the peer ObjectStore using the same DataRowCache,
     * including proper processing of deleted object being held in to-many collections.
     */
    @Test
    public void testSnapshotDeletePropagationToManyRefresh() throws Exception {

        Painting painting1 = (Painting) context.newObject("Painting");
        painting1.setPaintingTitle("p1");
        painting1.setToArtist(artist);

        Painting painting2 = (Painting) context.newObject("Painting");
        painting2.setPaintingTitle("p2");
        painting2.setToArtist(artist);

        context.commitChanges();

        // make sure we have a fully resolved copy of an artist and painting
        // objects
        // in the second context

        final Artist altArtist = context1.localObject(artist);
        final Painting altPainting1 = context1.localObject(painting1);
        final Painting altPainting2 = context1.localObject(painting2);

        assertEquals(artist.getArtistName(), altArtist.getArtistName());
        assertEquals(painting1.getPaintingTitle(), altPainting1.getPaintingTitle());
        assertEquals(painting2.getPaintingTitle(), altPainting2.getPaintingTitle());
        assertEquals(2, altArtist.getPaintingArray().size());
        assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, altPainting1.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, altPainting2.getPersistenceState());

        // make sure toOne relationships from Paintings
        // are resolved...
        altPainting1.getToArtist();
        altPainting2.getToArtist();
        assertSame(altArtist, altPainting1.readPropertyDirectly("toArtist"));
        assertSame(altArtist, altPainting2.readPropertyDirectly("toArtist"));

        // delete painting
        context.deleteObjects(painting1);
        context.commitChanges();

        // check underlying cache
        assertNull(context
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(painting1.getObjectId()));

        // check peer artist
        ParallelTestContainer helper = new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertEquals(
                        PersistenceState.TRANSIENT,
                        altPainting1.getPersistenceState());
                assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());

                Collection<Painting> list = altArtist.getPaintingArray();
                assertEquals(1, list.size());
                assertFalse(list.contains(altPainting1));
            }
        };
        helper.runTest(3000);
    }

    /**
     * Test case to prove that inserting an object in one ObjectStore and committing to
     * the database will be reflected in the peer ObjectStore using the same DataRowCache.
     * This would mean refreshing to-many collections.
     */
    @Test
    public void testSnapshotInsertPropagationToManyRefresh() throws Exception {

        Painting painting1 = (Painting) context.newObject("Painting");
        painting1.setPaintingTitle("p1");
        painting1.setToArtist(artist);

        context.commitChanges();

        // make sure we have a fully resolved copy of an artist and painting
        // objects
        // in the second context

        final Artist altArtist = context1.localObject(artist);
        final Painting altPainting1 = context1.localObject(painting1);

        assertEquals(artist.getArtistName(), altArtist.getArtistName());
        assertEquals(painting1.getPaintingTitle(), altPainting1.getPaintingTitle());
        assertEquals(1, altArtist.getPaintingArray().size());
        assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, altPainting1.getPersistenceState());

        // insert new painting and add to artist
        Painting painting2 = (Painting) context.newObject("Painting");
        painting2.setPaintingTitle("p2");
        painting2.setToArtist(artist);
        context.commitChanges();

        // check peer artist

        // use threaded helper as a barrier, to avoid triggering faults earlier than
        // needed
        ParallelTestContainer helper = new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                Object value = altArtist.readPropertyDirectly("paintingArray");
                assertTrue("Unexpected: " + value, value instanceof ToManyList);
                assertTrue(((ToManyList) value).isFault());
            }
        };
        helper.runTest(2000);
        List<Painting> list = altArtist.getPaintingArray();
        assertEquals(2, list.size());
    }

    /**
     * Checks that cache is refreshed when a query "refreshingObjects" property is set to
     * true.
     */
    @Test
    public void testCacheRefreshingOnSelect() throws Exception {
        String originalName = artist.getArtistName();
        final String newName = "version2";

        DataContext context = (DataContext) artist.getObjectContext();

        DataRow oldSnapshot = context
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(artist.getObjectId());
        assertNotNull(oldSnapshot);
        assertEquals(originalName, oldSnapshot.get("ARTIST_NAME"));

        // update artist using raw SQL
        SQLTemplate update = sqlTemplateCustomizer
                .createSQLTemplate(
                        Artist.class,
                        "UPDATE ARTIST SET ARTIST_NAME = #bind($newName) WHERE ARTIST_NAME = #bind($oldName)");
        Map<String, Object> map = new HashMap<>(3);
        map.put("newName", newName);
        map.put("oldName", originalName);
        update.setParams(map);
        context.performNonSelectingQuery(update);

        // fetch updated artist without refreshing
        List artists = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq(newName))
                .select(context);
        assertEquals(1, artists.size());
        artist = (Artist) artists.get(0);

        // check underlying cache
        DataRow freshSnapshot = context
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(artist.getObjectId());
        assertNotSame(oldSnapshot, freshSnapshot);
        assertEquals(newName, freshSnapshot.get("ARTIST_NAME"));

        // check an artist
        assertEquals(newName, artist.getArtistName());
    }

    @Test
    public void testSnapshotEvictedForHollow() throws Exception {
        String originalName = artist.getArtistName();

        context.invalidateObjects(artist);
        assertEquals(PersistenceState.HOLLOW, artist.getPersistenceState());
        assertNull(context
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(artist.getObjectId()));

        // resolve object
        assertEquals(originalName, artist.getArtistName());
        DataRow freshSnapshot = context
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(artist.getObjectId());
        assertNotNull(freshSnapshot);
        assertEquals(originalName, freshSnapshot.get("ARTIST_NAME"));
    }

    @Test
    public void testSnapshotEvictedAndObjectsHollowedForInvalidate() throws Exception {
        String originalName = artist.getArtistName();

        // make sure we have a fully resolved copy of an artist object
        // in the second context
        final Artist altArtist = context1.localObject(artist);
        context1.prepareForAccess(altArtist, null, false);
        assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());

        context.invalidateObjects(artist);

        // original context
        assertEquals(PersistenceState.HOLLOW, artist.getPersistenceState());
        assertNull(context
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(artist.getObjectId()));

        // alternate context
        new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertEquals(PersistenceState.HOLLOW, altArtist.getPersistenceState());
                assertNull(context1
                        .getObjectStore()
                        .getDataRowCache()
                        .getCachedSnapshot(altArtist.getObjectId()));
            }
        }.runTest(5000);

        // resolve object
        assertEquals(originalName, altArtist.getArtistName());
        DataRow altFreshSnapshot = context1
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(altArtist.getObjectId());
        assertNotNull(altFreshSnapshot);
        assertEquals(originalName, altFreshSnapshot.get("ARTIST_NAME"));

    }

    @Test
    public void testSnapshotEvictedForCommitted() throws Exception {
        String newName = "version2";

        assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());

        context.getObjectStore().getDataRowCache().forgetSnapshot(artist.getObjectId());
        assertNull(context
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(artist.getObjectId()));

        // modify object and try to save
        artist.setArtistName(newName);
        context.commitChanges();

        assertEquals(newName, artist.getArtistName());
        DataRow freshSnapshot = context
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(artist.getObjectId());
        assertNotNull(freshSnapshot);
        assertEquals(newName, freshSnapshot.get("ARTIST_NAME"));
    }

    @Test
    public void testSnapshotEvictedForModified() throws Exception {
        String newName = "version2";

        assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());

        // modify object PRIOR to killing the snapshot
        artist.setArtistName(newName);

        context.getObjectStore().getDataRowCache().forgetSnapshot(artist.getObjectId());
        assertNull(context
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(artist.getObjectId()));

        context.commitChanges();

        assertEquals(newName, artist.getArtistName());
        DataRow freshSnapshot = context
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(artist.getObjectId());
        assertNotNull(freshSnapshot);
        assertEquals(newName, freshSnapshot.get("ARTIST_NAME"));
    }

    @Test
    public void testSnapshotEvictedAndChangedForModified() throws Exception {
        String originalName = artist.getArtistName();
        String newName = "version2";
        String backendName = "version3";

        assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());

        // modify object PRIOR to killing the snapshot
        artist.setArtistName(newName);

        context.getObjectStore().getDataRowCache().forgetSnapshot(artist.getObjectId());
        assertNull(context
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(artist.getObjectId()));

        // now replace the row in the database
        String template = "UPDATE ARTIST SET ARTIST_NAME = #bind($newName) WHERE ARTIST_NAME = #bind($oldName)";
        SQLTemplate update = new SQLTemplate(Artist.class, template);

        Map<String, Object> map = new HashMap<>(3);
        map.put("newName", backendName);
        map.put("oldName", originalName);
        update.setParams(map);
        context.performNonSelectingQuery(update);

        context.commitChanges();

        assertEquals(newName, artist.getArtistName());
        DataRow freshSnapshot = context
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(artist.getObjectId());
        assertNotNull(freshSnapshot);
        assertEquals(newName, freshSnapshot.get("ARTIST_NAME"));
    }

    @Test
    public void testSnapshotEvictedForDeleted() throws Exception {
        // remember ObjectId
        ObjectId id = artist.getObjectId();

        assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());

        // delete object PRIOR to killing the snapshot
        context.deleteObjects(artist);

        context.getObjectStore().getDataRowCache().forgetSnapshot(id);
        assertNull(context.getObjectStore().getDataRowCache().getCachedSnapshot(id));

        context.commitChanges();

        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
        assertNull(context.getObjectStore().getDataRowCache().getCachedSnapshot(id));
    }
}
