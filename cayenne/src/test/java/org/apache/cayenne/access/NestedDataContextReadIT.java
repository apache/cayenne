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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class NestedDataContextReadIT extends RuntimeCase {

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
    }

    private void createArtistsDataSet() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
        tArtist.insert(33003, "artist3");
        tArtist.insert(33004, "artist4");
    }

    private void createRelationshipDataSet() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
        tArtist.insert(33003, "artist3");
        tArtist.insert(33004, "artist4");
        tPainting.insert(33001, "P_artist1", 33001, 3000);
        tPainting.insert(33002, "P_artist2", 33002, 3000);
        tPainting.insert(33003, "P_artist3", 33003, 3000);
        tPainting.insert(33004, "P_artist4", 33004, 3000);
        tPainting.insert(33005, "P_artist5", null, 3000);
    }

    private void createPrefetchingDataSet() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
        tPainting.insert(33001, "P_artist1", 33001, 3000);
        tPainting.insert(33006, "P_artist6", 33001, 3000);
    }

    @Test
    public void testCreateChildDataContext() {
        context.setValidatingObjectsOnCommit(true);

        ObjectContext child1 = runtime.newContext(context);

        assertNotNull(child1);
        assertSame(context, child1.getChannel());
        assertTrue(((DataContext) child1).isValidatingObjectsOnCommit());

        context.setValidatingObjectsOnCommit(false);

        ObjectContext child2 = runtime.newContext(context);

        assertNotNull(child2);
        assertSame(context, child2.getChannel());
        assertFalse(((DataContext) child2).isValidatingObjectsOnCommit());

        // second level of nesting
        ObjectContext child21 = runtime.newContext(child2);

        assertNotNull(child21);
        assertSame(child2, child21.getChannel());
        assertFalse(((DataContext) child2).isValidatingObjectsOnCommit());
    }

    @Test
    public void testSelect() throws Exception {
        createArtistsDataSet();

        ObjectContext child = runtime.newContext(context);

        // test how different object states appear in the child on select

        Persistent _new = context.newObject(Artist.class);

        Persistent hollow = Cayenne.objectForPK(context, Artist.class, 33001);
        context.invalidateObjects(hollow);

        Persistent committed = Cayenne.objectForPK(context, Artist.class, 33002);

        Artist modified = Cayenne.objectForPK(context, Artist.class, 33003);
        modified.setArtistName("MODDED");

        Persistent deleted = Cayenne.objectForPK(context, Artist.class, 33004);
        context.deleteObjects(deleted);

        assertEquals(PersistenceState.HOLLOW, hollow.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, committed.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, modified.getPersistenceState());
        assertEquals(PersistenceState.DELETED, deleted.getPersistenceState());
        assertEquals(PersistenceState.NEW, _new.getPersistenceState());

        List<Artist> objects = ObjectSelect.query(Artist.class).select(child);
        assertEquals("All but NEW object must have been included", 4, objects.size());

        for (Artist next : objects) {
            assertEquals(PersistenceState.COMMITTED, next.getPersistenceState());

            int id = Cayenne.intPKForObject(next);
            if (id == 33003) {
                assertEquals("MODDED", next.getArtistName());
            }
        }

    }

    @Test
    public void testPageableSelect() throws Exception {
        createArtistsDataSet();
        ObjectContext child = runtime.newContext(context);

        @SuppressWarnings("unchecked")
        IncrementalFaultList<Artist> records = (IncrementalFaultList) ObjectSelect.query(Artist.class)
                .orderBy(Artist.ARTIST_NAME.desc())
                .pageSize(1)
                .select(child);

        assertEquals(4, records.size());
        assertEquals(1, records.getPageSize());
    }

    @Test
    public void testReadToOneRelationship() throws Exception {
        createRelationshipDataSet();

        final ObjectContext child = runtime.newContext(context);

        // test how different object states appear in the child on select

        Painting hollowTargetSrc = Cayenne.objectForPK(context, Painting.class, 33001);
        Artist hollowTarget = hollowTargetSrc.getToArtist();

        Painting modifiedTargetSrc = Cayenne.objectForPK(context, Painting.class, 33002);
        Artist modifiedTarget = modifiedTargetSrc.getToArtist();
        modifiedTarget.setArtistName("M1");

        final Painting deletedTargetSrc = Cayenne.objectForPK(
                context,
                Painting.class,
                33003);
        Artist deletedTarget = deletedTargetSrc.getToArtist();
        deletedTargetSrc.setToArtist(null);
        context.deleteObjects(deletedTarget);

        Painting committedTargetSrc = Cayenne.objectForPK(context, Painting.class, 33004);
        Artist committedTarget = committedTargetSrc.getToArtist();
        committedTarget.getArtistName();

        final Painting newTargetSrc = Cayenne.objectForPK(context, Painting.class, 33005);
        Artist newTarget = context.newObject(Artist.class);
        newTarget.setArtistName("N1");
        newTargetSrc.setToArtist(newTarget);

        assertEquals(PersistenceState.COMMITTED, hollowTargetSrc.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, modifiedTargetSrc.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, deletedTargetSrc.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, committedTargetSrc.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, newTargetSrc.getPersistenceState());

        assertEquals(PersistenceState.HOLLOW, hollowTarget.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, modifiedTarget.getPersistenceState());
        assertEquals(PersistenceState.DELETED, deletedTarget.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, committedTarget.getPersistenceState());
        assertEquals(PersistenceState.NEW, newTarget.getPersistenceState());

        // run an ordered query, so we can address specific objects directly by index
        final List<Painting> childSources = ObjectSelect.query(Painting.class)
                .orderBy(Painting.PAINTING_TITLE.asc())
                .select(child);
        assertEquals(5, childSources.size());

        queryInterceptor.runWithQueriesBlocked(() -> {
            Painting childHollowTargetSrc = childSources.get(0);
            assertSame(child, childHollowTargetSrc.getObjectContext());

            Artist childHollowTarget = childHollowTargetSrc.getToArtist();
            assertNotNull(childHollowTarget);
            assertEquals(PersistenceState.HOLLOW, childHollowTarget.getPersistenceState());
            assertSame(child, childHollowTarget.getObjectContext());

            Artist childModifiedTarget = childSources.get(1).getToArtist();
            assertEquals(PersistenceState.COMMITTED, childModifiedTarget.getPersistenceState());
            assertSame(child, childModifiedTarget.getObjectContext());
            assertEquals("M1", childModifiedTarget.getArtistName());

            Painting childDeletedTargetSrc = childSources.get(2);
            // make sure we got the right object...
            assertEquals(deletedTargetSrc.getObjectId(), childDeletedTargetSrc.getObjectId());

            Artist childDeletedTarget = childDeletedTargetSrc.getToArtist();
            assertNull(childDeletedTarget);

            Artist childCommittedTarget = childSources.get(3).getToArtist();
            assertEquals(PersistenceState.COMMITTED, childCommittedTarget.getPersistenceState());
            assertSame(child, childCommittedTarget.getObjectContext());

            Painting childNewTargetSrc = childSources.get(4);
            // make sure we got the right object...
            assertEquals(newTargetSrc.getObjectId(), childNewTargetSrc.getObjectId());

            Artist childNewTarget = childNewTargetSrc.getToArtist();
            assertNotNull(childNewTarget);
            assertEquals(PersistenceState.COMMITTED, childNewTarget.getPersistenceState());
            assertSame(child, childNewTarget.getObjectContext());
            assertEquals("N1", childNewTarget.getArtistName());
        });
    }

    @Test
    public void testPrefetchingToOne() throws Exception {
        createPrefetchingDataSet();

        final ObjectContext child = runtime.newContext(context);

        final ObjectId prefetchedId = ObjectId.of(
                "Artist",
                Artist.ARTIST_ID_PK_COLUMN,
                33001);

        final List<Painting> results = ObjectSelect.query(Painting.class)
                .orderBy(Painting.PAINTING_TITLE.asc())
                .prefetch(Painting.TO_ARTIST.disjoint())
                .select(child);

        // blockQueries();

        queryInterceptor.runWithQueriesBlocked(() -> {
            assertEquals(2, results.size());
            for (Painting o : results) {
                assertEquals(PersistenceState.COMMITTED, o.getPersistenceState());
                assertSame(child, o.getObjectContext());

                Artist o1 = o.getToArtist();
                assertNotNull(o1);
                assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
                assertSame(child, o1.getObjectContext());
                assertEquals(prefetchedId, o1.getObjectId());
            }
        });
    }

    @Test
    public void testPrefetchingToMany() throws Exception {
        createPrefetchingDataSet();

        final ObjectContext child = runtime.newContext(context);

        final List<Artist> results = ObjectSelect.query(Artist.class)
                .orderBy(Artist.ARTIST_NAME.asc())
                .prefetch(Artist.PAINTING_ARRAY.disjoint())
                .select(child);

        queryInterceptor.runWithQueriesBlocked(() -> {
            Artist o1 = results.get(0);
            assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
            assertSame(child, o1.getObjectContext());

            List<Painting> children1 = o1.getPaintingArray();
            assertEquals(ToManyList.class, children1.getClass());
            assertEquals(2, children1.size());

            for (Painting o : children1) {
                assertEquals(PersistenceState.COMMITTED, o.getPersistenceState());
                assertSame(child, o.getObjectContext());
                assertEquals(o1, o.getToArtist());
            }

            Artist o2 = results.get(1);
            assertEquals(PersistenceState.COMMITTED, o2.getPersistenceState());
            assertSame(child, o2.getObjectContext());

            List<Painting> children2 = o2.getPaintingArray();
            assertEquals(0, children2.size());
        });
    }

    @Test
    public void testObjectFromDataRow() {

        DataContext childContext = (DataContext) runtime.newContext(context);

        DataRow row = new DataRow(8);
        row.put("ARTIST_ID", 5L);
        row.put("ARTIST_NAME", "A");
        row.put("DATE_OF_BIRTH", new Date());

        Artist artist = childContext.objectFromDataRow(Artist.class, row);
        assertNotNull(artist);
        assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());
        assertSame(childContext, artist.getObjectContext());

        Object parentArtist = context.getObjectStore().getNode(artist.getObjectId());
        assertNotNull(parentArtist);
        assertNotSame(artist, parentArtist);
    }
}
