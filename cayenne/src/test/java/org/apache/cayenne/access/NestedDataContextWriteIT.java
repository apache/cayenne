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

import java.sql.Types;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.ArtGroup;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.PaintingInfo;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class NestedDataContextWriteIT extends RuntimeCase {

    @Inject
    private CayenneRuntime runtime;

    @Inject
    private DataContext context;

    @Inject
    private DataChannelInterceptor queryInterceptor;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tArtist;
    private TableHelper tPainting;

    @Before
    public void setUp() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns(
                "PAINTING_ID",
                "PAINTING_TITLE",
                "ARTIST_ID",
                "ESTIMATED_PRICE").setColumnTypes(
                Types.INTEGER,
                Types.VARCHAR,
                Types.BIGINT,
                Types.DECIMAL);

        TableHelper tPaintingInfo = new TableHelper(dbHelper, "PAINTING_INFO");
        tPaintingInfo.setColumns("PAINTING_ID", "TEXT_REVIEW", "IMAGE_BLOB");
    }

    private void createArtistsDataSet() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
        tArtist.insert(33003, "artist3");
        tArtist.insert(33004, "artist4");
    }

    private void createMixedDataSet() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
        tArtist.insert(33003, "artist3");
        tArtist.insert(33004, "artist4");
        tPainting.insert(33001, "P_artist1", 33001, 3000);
        tPainting.insert(33002, "P_artist2", 33002, 3000);
        tPainting.insert(33003, "P_artist3", 33003, 3000);
        tPainting.insert(33004, "P_artist4", 33004, 3000);
        tPainting.insert(33005, "P_artist5", null, 3000);
        tPainting.insert(33006, "P_artist6", 33001, 3000);
    }

    private void createNullifyToOneDataSet() throws Exception {
        tArtist.insert(33001, "artist1");
        tPainting.insert(33001, "P_artist1", 33001, 3000);
    }

    private void createSingleArtistDataSet() throws Exception {
        tArtist.insert(33001, "artist1");
    }

    /**
     * Ensures that created DataContext's ObjectStore retains unreferenced registered
     * objects.
     */
    // TODO : pluggable retain strategy
    private DataContext createDataContext() {
        context.getObjectStore().objectMap = new HashMap<>();
        return context;
    }

    @Test
    public void testDeleteNew() throws Exception {
        createSingleArtistDataSet();

        DataContext context = createDataContext();
        ObjectContext childContext = runtime.newContext(context);

        Artist a = Cayenne.objectForPK(childContext, Artist.class, 33001);
        Painting p = childContext.newObject(Painting.class);
        p.setPaintingTitle("X");
        a.addToPaintingArray(p);

        childContext.commitChangesToParent();

        childContext.deleteObjects(p);
        a.removeFromPaintingArray(p);

        childContext.commitChangesToParent();
    }

    /**
     * A test case for CAY-698 bug.
     */
    @Test
    public void testNullifyToOne() throws Exception {
        createNullifyToOneDataSet();

        final DataContext context = createDataContext();
        final ObjectContext childContext = runtime.newContext(context);
        ObjectContext childContextPeer = runtime.newContext(context);

        final Painting childP1 = Cayenne.objectForPK(childContext, Painting.class, 33001);

        // trigger object creation in the peer nested DC
        Cayenne.objectForPK(childContextPeer, Painting.class, 33001);
        childP1.setToArtist(null);

        queryInterceptor.runWithQueriesBlocked(() -> {
            childContext.commitChangesToParent();
            assertEquals(PersistenceState.COMMITTED, childP1.getPersistenceState());

            Painting parentP1 = (Painting) context.getGraphManager().getNode(
                    childP1.getObjectId());

            assertNotNull(parentP1);
            assertEquals(PersistenceState.MODIFIED, parentP1.getPersistenceState());
            assertNull(parentP1.getToArtist());
        });
    }

    @Test
    public void testCommitChangesToParent() throws Exception {
        createArtistsDataSet();

        final DataContext context = createDataContext();
        final ObjectContext childContext = runtime.newContext(context);

        // make sure we fetch in predictable order
        List<Artist> objects = ObjectSelect.query(Artist.class)
                .orderBy(Artist.ARTIST_NAME.asc())
                .select(childContext);

        assertEquals(4, objects.size());

        final Artist childNew = childContext.newObject(Artist.class);
        childNew.setArtistName("NNN");

        final Artist childModified = objects.get(0);
        childModified.setArtistName("MMM");

        final Artist childCommitted = objects.get(1);

        final Artist childHollow = objects.get(3);
        childContext.invalidateObjects(childHollow);

        queryInterceptor.runWithQueriesBlocked(() -> {
            childContext.commitChangesToParent();

            // * all modified child objects must be in committed state now
            // * all modifications should be propagated to the parent
            // * no actual commit should occur.

            assertEquals(PersistenceState.COMMITTED, childNew.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childModified
                    .getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childCommitted
                    .getPersistenceState());
            assertEquals(PersistenceState.HOLLOW, childHollow.getPersistenceState());

            Artist parentNew = (Artist) context.getGraphManager().getNode(
                    childNew.getObjectId());
            Artist parentModified = (Artist) context.getGraphManager().getNode(
                    childModified.getObjectId());
            Artist parentCommitted = (Artist) context.getGraphManager().getNode(
                    childCommitted.getObjectId());
            Artist parentHollow = (Artist) context.getGraphManager().getNode(
                    childHollow.getObjectId());

            assertNotNull(parentNew);
            assertEquals(PersistenceState.NEW, parentNew.getPersistenceState());
            assertEquals("NNN", parentNew.getArtistName());

            assertNotNull(parentModified);
            assertEquals(PersistenceState.MODIFIED, parentModified
                    .getPersistenceState());
            assertEquals("MMM", parentModified.getArtistName());
            assertNotNull(context.getObjectStore().getChangesByObjectId().get(
                    parentModified.getObjectId()));

            assertNotNull(parentCommitted);
            assertEquals(PersistenceState.COMMITTED, parentCommitted
                    .getPersistenceState());

            assertNotNull(parentHollow);
            // TODO: we can assert that when we figure out how nested "invalidate"
            // should
            // work
            // assertEquals(PersistenceState.HOLLOW,
            // parentHollow.getPersistenceState());
        });
    }

    @Test
    public void testCommitChangesToParentDeleted() throws Exception {
        createArtistsDataSet();

        DataContext context = createDataContext();
        ObjectContext childContext = runtime.newContext(context);

        // make sure we fetch in predictable order
        List<Artist> objects = ObjectSelect.query(Artist.class)
                .orderBy(Artist.ARTIST_NAME.asc())
                .select(childContext);

        assertEquals(4, objects.size());

        // delete AND modify
        Artist childDeleted = objects.get(2);
        childContext.deleteObjects(childDeleted);
        childDeleted.setArtistName("DDD");

        // don't block queries - on delete Cayenne may need to resolve delete rules via
        // fetch
        childContext.commitChangesToParent();

        // * all modified child objects must be in committed state now
        // * all modifications should be propagated to the parent
        // * no actual commit should occur.

        assertEquals(PersistenceState.TRANSIENT, childDeleted.getPersistenceState());

        Artist parentDeleted = (Artist) context.getGraphManager().getNode(
                childDeleted.getObjectId());

        assertNotNull(parentDeleted);
        assertEquals(PersistenceState.DELETED, parentDeleted.getPersistenceState());
        assertEquals("DDD", parentDeleted.getArtistName());
    }

    @Test
    public void testCommitChanges() throws Exception {
        createArtistsDataSet();

        DataContext context = createDataContext();
        ObjectContext childContext = runtime.newContext(context);

        // make sure we fetch in predictable order
        List<Artist> objects = ObjectSelect.query(Artist.class)
                .orderBy(Artist.ARTIST_NAME.asc())
                .select(childContext);

        assertEquals(4, objects.size());

        Artist childNew = childContext.newObject(Artist.class);
        childNew.setArtistName("NNN");

        Artist childModified = objects.get(0);
        childModified.setArtistName("MMM");

        Artist childCommitted = objects.get(1);

        // delete AND modify
        Artist childDeleted = objects.get(2);
        childContext.deleteObjects(childDeleted);
        childDeleted.setArtistName("DDD");

        Artist childHollow = objects.get(3);
        childContext.invalidateObjects(childHollow);

        childContext.commitChanges();

        assertEquals(PersistenceState.COMMITTED, childNew.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, childModified.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, childCommitted.getPersistenceState());
        assertEquals(PersistenceState.TRANSIENT, childDeleted.getPersistenceState());
        assertEquals(PersistenceState.HOLLOW, childHollow.getPersistenceState());

        Artist parentNew = (Artist) context.getGraphManager().getNode(
                childNew.getObjectId());
        Artist parentModified = (Artist) context.getGraphManager().getNode(
                childModified.getObjectId());
        Artist parentCommitted = (Artist) context.getGraphManager().getNode(
                childCommitted.getObjectId());
        Artist parentDeleted = (Artist) context.getGraphManager().getNode(
                childDeleted.getObjectId());
        Artist parentHollow = (Artist) context.getGraphManager().getNode(
                childHollow.getObjectId());

        assertNotNull(parentNew);
        assertEquals(PersistenceState.COMMITTED, parentNew.getPersistenceState());
        assertEquals("NNN", parentNew.getArtistName());

        assertNotNull(parentModified);
        assertEquals(PersistenceState.COMMITTED, parentModified.getPersistenceState());
        assertEquals("MMM", parentModified.getArtistName());
        assertNull(context.getObjectStore().getChangesByObjectId().get(
                parentModified.getObjectId()));

        assertNull("Deleted object should not be registered.", parentDeleted);

        assertNotNull(parentCommitted);
        assertEquals(PersistenceState.COMMITTED, parentCommitted.getPersistenceState());

        assertNotNull(parentHollow);
    }

    @Test
    public void testCommitChangesToParent_MergeProperties() throws Exception {
        createMixedDataSet();

        final DataContext context = createDataContext();
        final ObjectContext childContext = runtime.newContext(context);

        // make sure we fetch in predictable order
        List<Painting> objects = ObjectSelect.query(Painting.class)
                .orderBy(Painting.PAINTING_TITLE.asc())
                .select(childContext);

        assertEquals(6, objects.size());

        final Painting childModifiedSimple = objects.get(0);
        childModifiedSimple.setPaintingTitle("C_PT");

        final Painting childModifiedToOne = objects.get(1);
        childModifiedToOne.setToArtist(childModifiedSimple.getToArtist());

        final Artist childModifiedToMany = objects.get(2).getToArtist();

        // ensure painting array is fully resolved...
        childModifiedToMany.getPaintingArray().size();
        childModifiedToMany.addToPaintingArray(objects.get(3));

        queryInterceptor.runWithQueriesBlocked(() -> {
            Painting parentModifiedSimple;
            Artist parentModifiedToMany;

            childContext.commitChangesToParent();

            assertEquals(PersistenceState.COMMITTED, childModifiedSimple
                    .getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childModifiedToOne
                    .getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childModifiedToMany
                    .getPersistenceState());

            parentModifiedSimple = (Painting) context.getGraphManager().getNode(
                    childModifiedSimple.getObjectId());

            Painting parentModifiedToOne = (Painting) context
                    .getGraphManager()
                    .getNode(childModifiedToOne.getObjectId());

            parentModifiedToMany = (Artist) context.getGraphManager().getNode(
                    childModifiedToMany.getObjectId());

            assertNotNull(parentModifiedSimple);
            assertEquals(PersistenceState.MODIFIED, parentModifiedSimple
                    .getPersistenceState());
            assertEquals("C_PT", parentModifiedSimple.getPaintingTitle());
            assertNotNull(context.getObjectStore().getChangesByObjectId().get(
                    parentModifiedSimple.getObjectId()));

            assertNotNull(parentModifiedToOne);
            assertEquals(PersistenceState.MODIFIED, parentModifiedToOne
                    .getPersistenceState());
            assertNotNull(parentModifiedToOne.getToArtist());
            assertEquals(33001, Cayenne.intPKForObject(parentModifiedToOne
                    .getToArtist()));
            assertNotNull(context.getObjectStore().getChangesByObjectId().get(
                    parentModifiedToOne.getObjectId()));

            // indirectly modified....
            assertNotNull(parentModifiedToMany);
            assertEquals(PersistenceState.MODIFIED, parentModifiedToMany
                    .getPersistenceState());

            // here query is expected, as the parent was hollow and its to-many
            // relationship
            // is unresolved
            List<?> paintings = parentModifiedToMany.getPaintingArray();
            assertEquals(2, paintings.size());
        });
    }

    @Test
    public void testCommitChangesToParentPropagatedKey() {
        final DataContext context = createDataContext();
        final ObjectContext childContext = runtime.newContext(context);

        final Painting childMaster = childContext.newObject(Painting.class);
        childMaster.setPaintingTitle("Master");

        final PaintingInfo childDetail1 = childContext.newObject(PaintingInfo.class);
        childDetail1.setTextReview("Detail1");
        childDetail1.setPainting(childMaster);

        queryInterceptor.runWithQueriesBlocked(() -> {
            childContext.commitChangesToParent();

            assertEquals(PersistenceState.COMMITTED, childMaster
                    .getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childDetail1
                    .getPersistenceState());

            Painting parentMaster = (Painting) context.getGraphManager().getNode(
                    childMaster.getObjectId());

            assertNotNull(parentMaster);
            assertEquals(PersistenceState.NEW, parentMaster.getPersistenceState());

            PaintingInfo parentDetail1 = (PaintingInfo) context
                    .getGraphManager()
                    .getNode(childDetail1.getObjectId());

            assertNotNull(parentDetail1);
            assertEquals(PersistenceState.NEW, parentDetail1.getPersistenceState());

            assertSame(parentMaster, parentDetail1.getPainting());
            assertSame(parentDetail1, parentMaster.getToPaintingInfo());
        });
    }

    @Test
    public void testCommitChangesToParentFlattened() {

        final DataContext context = createDataContext();
        final ObjectContext childContext = runtime.newContext(context);

        final Artist childO1 = childContext.newObject(Artist.class);
        childO1.setArtistName("Master");

        final ArtGroup childO2 = childContext.newObject(ArtGroup.class);
        childO2.setName("Detail1");
        childO2.addToArtistArray(childO1);

        ObjEntity ent = childContext.getEntityResolver().getObjEntity("ArtGroup");
        Collection<ObjRelationship> rels = ent.getDeclaredRelationships();
        assertEquals(3, rels.size());

        assertEquals(1, childO1.getGroupArray().size());
        assertEquals(1, childO2.getArtistArray().size());

        queryInterceptor.runWithQueriesBlocked(() -> {
            childContext.commitChangesToParent();

            assertEquals(PersistenceState.COMMITTED, childO1.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childO2.getPersistenceState());

            Artist parentO1 = (Artist) context.getGraphManager().getNode(
                    childO1.getObjectId());

            assertNotNull(parentO1);
            assertEquals(PersistenceState.NEW, parentO1.getPersistenceState());

            ArtGroup parentO2 = (ArtGroup) context.getGraphManager().getNode(
                    childO2.getObjectId());

            assertNotNull(parentO2);
            assertEquals(PersistenceState.NEW, parentO2.getPersistenceState());

            assertEquals(1, parentO1.getGroupArray().size());
            assertEquals(1, parentO2.getArtistArray().size());
            assertTrue(parentO2.getArtistArray().contains(parentO1));
            assertTrue(parentO1.getGroupArray().contains(parentO2));
        });
    }

    @Test
    public void testCommitChangesToParentFlattenedMultipleFlush() {
        final DataContext context = createDataContext();
        final ObjectContext childContext = runtime.newContext(context);

        final Artist childO1 = childContext.newObject(Artist.class);
        childO1.setArtistName("o1");

        final ArtGroup childO2 = childContext.newObject(ArtGroup.class);
        childO2.setName("o2");
        childO2.addToArtistArray(childO1);

        childContext.commitChangesToParent();

        final ArtGroup childO3 = childContext.newObject(ArtGroup.class);
        childO3.setName("o3");
        childO1.addToGroupArray(childO3);

        assertEquals(2, childO1.getGroupArray().size());
        assertEquals(1, childO2.getArtistArray().size());
        assertEquals(1, childO3.getArtistArray().size());

        queryInterceptor.runWithQueriesBlocked(() -> {
            childContext.commitChangesToParent();

            assertEquals(PersistenceState.COMMITTED, childO1.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childO2.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childO3.getPersistenceState());

            Artist parentO1 = (Artist) context.getGraphManager().getNode(
                    childO1.getObjectId());

            assertNotNull(parentO1);
            assertEquals(PersistenceState.NEW, parentO1.getPersistenceState());

            ArtGroup parentO2 = (ArtGroup) context.getGraphManager().getNode(
                    childO2.getObjectId());

            assertNotNull(parentO2);
            assertEquals(PersistenceState.NEW, parentO2.getPersistenceState());

            ArtGroup parentO3 = (ArtGroup) context.getGraphManager().getNode(
                    childO3.getObjectId());

            assertNotNull(parentO3);
            assertEquals(PersistenceState.NEW, parentO3.getPersistenceState());

            assertEquals(2, parentO1.getGroupArray().size());
            assertEquals(1, parentO2.getArtistArray().size());
            assertEquals(1, parentO3.getArtistArray().size());
            assertTrue(parentO2.getArtistArray().contains(parentO1));
            assertTrue(parentO3.getArtistArray().contains(parentO1));
            assertTrue(parentO1.getGroupArray().contains(parentO2));
            assertTrue(parentO1.getGroupArray().contains(parentO3));
        });

        childO1.removeFromGroupArray(childO2);

        queryInterceptor.runWithQueriesBlocked(() -> {
            childContext.commitChangesToParent();

            assertEquals(PersistenceState.COMMITTED, childO1.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childO2.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childO3.getPersistenceState());

            Artist parentO1 = (Artist) context.getGraphManager().getNode(
                    childO1.getObjectId());

            assertNotNull(parentO1);
            assertEquals(PersistenceState.NEW, parentO1.getPersistenceState());

            ArtGroup parentO2 = (ArtGroup) context.getGraphManager().getNode(
                    childO2.getObjectId());

            assertNotNull(parentO2);
            assertEquals(PersistenceState.NEW, parentO2.getPersistenceState());

            ArtGroup parentO3 = (ArtGroup) context.getGraphManager().getNode(
                    childO3.getObjectId());

            assertNotNull(parentO3);
            assertEquals(PersistenceState.NEW, parentO3.getPersistenceState());

            assertEquals(1, parentO1.getGroupArray().size());
            assertEquals(0, parentO2.getArtistArray().size());
            assertEquals(1, parentO3.getArtistArray().size());

            assertTrue(parentO3.getArtistArray().contains(parentO1));
            assertTrue(parentO1.getGroupArray().contains(parentO3));
        });
    }

    @Test
    public void testAddRemove() {

        DataContext context = createDataContext();
        ObjectContext child = runtime.newContext(context);

        Artist a = child.newObject(Artist.class);
        a.setArtistName("X");
        child.commitChanges();

        Painting p1 = child.newObject(Painting.class);
        p1.setPaintingTitle("P1");
        a.addToPaintingArray(p1);

        Painting p2 = child.newObject(Painting.class);
        p2.setPaintingTitle("P2");
        a.addToPaintingArray(p2);

        a.removeFromPaintingArray(p2);

        // this causes an error on commit
        child.deleteObjects(p2);

        child.commitChangesToParent();

    }

    @Test
    public void testCAY1194() {
        DataContext context = createDataContext();

        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("111");
        ObjectContext child = runtime.newContext(context);

        Painting painting = child.newObject(Painting.class);
        painting.setPaintingTitle("222");

        Artist localParentMt = child.localObject(artist);
        assertEquals(0, artist.getPaintingArray().size());
        assertEquals(0, localParentMt.getPaintingArray().size());

        painting.setToArtist(localParentMt);

        assertEquals(0, artist.getPaintingArray().size());
        assertEquals(1, localParentMt.getPaintingArray().size());
        assertEquals(localParentMt.getPaintingArray().get(0).getObjectContext(), child);

        child.commitChangesToParent();
        assertEquals(1, artist.getPaintingArray().size());
        assertEquals(artist.getPaintingArray().get(0).getObjectContext(), context);

    }
    
    @Test
    @Ignore("Waiting for a fix")
    public void testTwoStageCommit() {
        DataContext parent = createDataContext();
        ObjectContext child = runtime.newContext(parent);

        Painting painting = child.newObject(Painting.class);
        painting.setPaintingTitle("222");
        
        child.commitChangesToParent();
        parent.commitChanges();
        assertTrue(!painting.getObjectId().isTemporary());
    }
}
