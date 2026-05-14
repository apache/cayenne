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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLSelect;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests joint prefetch handling by Cayenne access stack.
 */
public class JointPrefetchIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    protected DataContext context;
    protected CayenneRuntime runtime;

    protected TableHelper tArtist;
    protected TableHelper tGallery;
    protected TableHelper tPainting;

    
    @BeforeEach
    public void setUp() throws Exception {
        context = env.context();
        runtime = env.runtime();
        tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");

        tGallery = env.table("GALLERY", "GALLERY_ID", "GALLERY_NAME");

        tPainting = env.table("PAINTING", "PAINTING_ID",
                "PAINTING_TITLE",
                "ARTIST_ID",
                "ESTIMATED_PRICE",
                "GALLERY_ID");
    }

    private void createJointPrefetchDataSet() throws Exception {
        tGallery.insert(33001, "G1");
        tGallery.insert(33002, "G2");
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
        tArtist.insert(33003, "artist3");
        tPainting.insert(33001, "P_artist11", 33001, 1000, 33001);
        tPainting.insert(33002, "P_artist12", 33001, 2000, 33001);
        tPainting.insert(33003, "P_artist21", 33002, 3000, 33002);
    }

    @Test
    public void jointPrefetch_ToOne_FetchLimit() throws Exception {
        createJointPrefetchDataSet();

        final List<Painting> objects = ObjectSelect.query(Painting.class)
                .limit(2).offset(0)
                .orderBy("db:PAINTING_ID", SortOrder.ASCENDING)
                .prefetch(Painting.TO_ARTIST.joint())
                .select(context);

        env.runWithQueriesBlocked(() -> {
            assertEquals(2, objects.size());

            for (Painting p : objects) {
                Artist target = p.getToArtist();
                assertNotNull(target);
                assertEquals(PersistenceState.COMMITTED, target.getPersistenceState());
            }
        });
    }

    @Test
    public void jointPrefetch_ToMany_FetchLimit() throws Exception {
        createJointPrefetchDataSet();

        final List<Artist> objects = ObjectSelect.query(Artist.class)
                .limit(2).offset(0)
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc())
                .prefetch(Artist.PAINTING_ARRAY.joint())
                .select(context);

        env.runWithQueriesBlocked(() -> {
            // herein lies the limitation of prefetching combined with fetch limit -
            // we got fewer artists than we wanted
            assertEquals(1, objects.size());

            for (Artist a : objects) {
                List<Painting> targets = a.getPaintingArray();
                assertNotNull(targets);
                for (Painting p : targets) {
                    assertEquals(PersistenceState.COMMITTED, p.getPersistenceState());
                }
            }
        });
    }

    @Test
    public void jointPrefetchDataRows() throws Exception {
        createJointPrefetchDataSet();

        // query with to-many joint prefetches
        final List<DataRow> rows = ObjectSelect.dataRowQuery(Painting.class)
                .orderBy("db:PAINTING_ID", SortOrder.ASCENDING)
                .prefetch(Painting.TO_ARTIST.joint())
                .select(context);

        env.runWithQueriesBlocked(() -> {
            assertEquals(3, rows.size());

            // row should contain columns from both entities minus those duplicated in a join...
            int rowWidth = context.getEntityResolver().getDbEntity("ARTIST").getAttributes().size()
                    + context.getEntityResolver().getDbEntity("PAINTING").getAttributes().size();
            for (DataRow row : rows) {
                assertEquals(rowWidth, row.size(), "" + row);

                // assert columns presence
                assertTrue(row.containsKey("PAINTING_ID"), row + "");
                assertTrue(row.containsKey("ARTIST_ID"), row + "");
                assertTrue(row.containsKey("GALLERY_ID"), row + "");
                assertTrue(row.containsKey("PAINTING_TITLE"), row + "");
                assertTrue(row.containsKey("ESTIMATED_PRICE"), row + "");
                assertTrue(row.containsKey("toArtist.ARTIST_NAME"), row + "");
                assertTrue(row.containsKey("toArtist.DATE_OF_BIRTH"), row + "");
            }
        });
    }

    @Test
    public void jointPrefetchSQLTemplate() throws Exception {
        createJointPrefetchDataSet();

        // correctly naming columns is the key..
        SQLTemplate q = new SQLTemplate(
                Artist.class,
                "SELECT distinct "
                        + "#result('ESTIMATED_PRICE' 'BigDecimal' '' 'paintingArray.ESTIMATED_PRICE'), "
                        + "#result('PAINTING_TITLE' 'String' '' 'paintingArray.PAINTING_TITLE'), "
                        + "#result('GALLERY_ID' 'int' '' 'paintingArray.GALLERY_ID'), "
                        + "#result('PAINTING_ID' 'int' '' 'paintingArray.PAINTING_ID'), "
                        + "#result('ARTIST_NAME' 'String'), "
                        + "#result('DATE_OF_BIRTH' 'java.util.Date'), "
                        + "#result('t0.ARTIST_ID' 'int' '' 'ARTIST_ID') "
                        + "FROM ARTIST t0, PAINTING t1 "
                        + "WHERE t0.ARTIST_ID = t1.ARTIST_ID");

        q.addPrefetch(Artist.PAINTING_ARRAY.joint());
        q.setFetchingDataRows(false);

        @SuppressWarnings("unchecked")
        final List<Artist> objects = (List<Artist>)context.performQuery(q);

        env.runWithQueriesBlocked(() -> {
            // without OUTER join we will get fewer objects...
            assertEquals(2, objects.size());

            for (Artist a : objects) {
                List<Painting> list = a.getPaintingArray();

                assertNotNull(list);
                assertFalse(((ValueHolder) list).isFault());
                assertFalse(list.isEmpty());

                for (Painting p : list) {
                    assertEquals(PersistenceState.COMMITTED, p.getPersistenceState());
                    // make sure properties are not null..
                    assertNotNull(p.getPaintingTitle());
                }
            }
        });
    }

    @Test
    public void jointPrefetchToOne() throws Exception {
        createJointPrefetchDataSet();

        // query with to-many joint prefetches
        final List<Painting> objects = ObjectSelect.query(Painting.class)
                .orderBy("db:PAINTING_ID", SortOrder.ASCENDING)
                .prefetch(Painting.TO_ARTIST.joint())
                .select(context);

        env.runWithQueriesBlocked(() -> {
            assertEquals(3, objects.size());
            for (Painting p : objects) {
                Artist target = p.getToArtist();
                assertNotNull(target);
                assertEquals(PersistenceState.COMMITTED, target.getPersistenceState());
            }
        });
    }

    /**
     * Tests that joined entities can have non-standard type mappings.
     */
    @Test
    public void jointPrefetchDataTypes() {
        // prepare... can't load from XML, as it doesn't yet support dates..
        SQLTemplate artistSQL = new SQLTemplate(
                Artist.class,
                "insert into ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) "
                        + "values (33001, 'a1', #bind($date 'DATE'))");
        artistSQL.setParams(Collections.singletonMap(
                "date",
                new Date(System.currentTimeMillis())));
        SQLTemplate paintingSQL = new SQLTemplate(
                Painting.class,
                "INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ARTIST_ID, ESTIMATED_PRICE) "
                        + "VALUES (33001, 'p1', 33001, 1000)");

        context.performNonSelectingQuery(artistSQL);
        context.performNonSelectingQuery(paintingSQL);

        // test
        ObjEntity artistE = context.getEntityResolver().getObjEntity("Artist");
        ObjAttribute dateOfBirth = artistE.getAttribute("dateOfBirth");
        assertEquals("java.util.Date", dateOfBirth.getType());
        dateOfBirth.setType("java.sql.Date");
        try {
             List<Painting> objects = ObjectSelect.query(Painting.class)
                    .prefetch(Painting.TO_ARTIST.joint())
                    .select(context);

            env.runWithQueriesBlocked(() -> {
                assertEquals(1, objects.size());
                for (Painting p : objects) {
                    Artist a = p.getToArtist();
                    assertNotNull(a);
                    assertNotNull(a.getDateOfBirth());
                    assertTrue(Date.class.isAssignableFrom(a.getDateOfBirth().getClass()), a.getDateOfBirth().getClass().getName());
                }
            });
        } finally {
            dateOfBirth.setType("java.util.Date");
        }
    }

    @Test
    public void jointPrefetchToMany() throws Exception {
        createJointPrefetchDataSet();

        // query with to-many joint prefetches
         List<Artist> objects = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTING_ARRAY.joint())
                .select(context);

        env.runWithQueriesBlocked(() -> {
            assertEquals(3, objects.size());

            for (Artist a : objects) {
                List<Painting> list = a.getPaintingArray();

                assertNotNull(list);
                assertFalse(((ValueHolder) list).isFault());

                for (Painting p : list) {
                    assertEquals(PersistenceState.COMMITTED, p.getPersistenceState());
                    // make sure properties are not null..
                    assertNotNull(p.getPaintingTitle());
                }
            }
        });
    }

    @Test
    public void jointPrefetchToManyNonConflictingQualifier() throws Exception {
        createJointPrefetchDataSet();

        // query with to-many joint prefetches and qualifier that doesn't match prefetch....
         List<Artist> objects = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq("artist1"))
                .prefetch(Artist.PAINTING_ARRAY.joint())
                .select(context);

        env.runWithQueriesBlocked(() -> {
            assertEquals(1, objects.size());

            Artist a = objects.getFirst();
            List<Painting> list = a.getPaintingArray();

            assertNotNull(list);
            assertFalse(((ValueHolder) list).isFault());
            assertEquals(2, list.size());

            for (Painting p : list) {
                assertEquals(PersistenceState.COMMITTED, p.getPersistenceState());
                // make sure properties are not null..
                assertNotNull(p.getPaintingTitle());
            }

            // assert no duplicates
            Set<Painting> s = new HashSet<>(list);
            assertEquals(s.size(), list.size());
        });
    }

    @Test
    public void jointPrefetchMultiStep() throws Exception {
        createJointPrefetchDataSet();

        final DataContext context = this.context;

        // make sure phantomly prefetched objects are not deallocated
        context.getObjectStore().objectMap = new HashMap<>();

        // sanity check...
        Persistent g1 = (Persistent) context.getGraphManager().getNode(
                ObjectId.of("Gallery", Gallery.GALLERY_ID_PK_COLUMN, 33001));
        assertNull(g1);

        // query with to-many joint prefetches
         List<Artist> objects = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).joint())
                .select(context);

        env.runWithQueriesBlocked(() -> {
            assertEquals(3, objects.size());

            for (Artist a : objects) {
                ValueHolder list = (ValueHolder) a.getPaintingArray();

                assertNotNull(list);

                // intermediate relationship is not fetched...
                assertTrue(list.isFault());
            }

            // however both galleries must be in memory...
            Persistent g11 = (Persistent) context.getGraphManager().getNode(
                    ObjectId.of("Gallery", Gallery.GALLERY_ID_PK_COLUMN, 33001));
            assertNotNull(g11);
            assertEquals(PersistenceState.COMMITTED, g11.getPersistenceState());
            Persistent g2 = (Persistent) context.getGraphManager().getNode(
                    ObjectId.of("Gallery", Gallery.GALLERY_ID_PK_COLUMN, 33002));
            assertNotNull(g2);
            assertEquals(PersistenceState.COMMITTED, g2.getPersistenceState());
        });
    }

    @Test
    public void jointPrefetchSQLSelectToMany() throws Exception {
        createJointPrefetchDataSet();

        List<Artist> objects = SQLSelect.query(Artist.class, "SELECT "
                + "#result('ESTIMATED_PRICE' 'BigDecimal' '' 'paintingArray.ESTIMATED_PRICE'), "
                + "#result('PAINTING_TITLE' 'String' '' 'paintingArray.PAINTING_TITLE'), "
                + "#result('GALLERY_ID' 'int' '' 'paintingArray.GALLERY_ID'), "
                + "#result('PAINTING_ID' 'int' '' 'paintingArray.PAINTING_ID'), "
                + "#result('ARTIST_NAME' 'String'), "
                + "#result('DATE_OF_BIRTH' 'java.util.Date'), "
                + "#result('t0.ARTIST_ID' 'int' '' 'ARTIST_ID') "
                + "FROM ARTIST t0, PAINTING t1 "
                + "WHERE t0.ARTIST_ID = t1.ARTIST_ID")
                .addPrefetch(Artist.PAINTING_ARRAY.joint())
                .select(context);

        env.runWithQueriesBlocked(() -> {
            assertEquals(2, objects.size());

            for (Artist artist : objects) {
                List<Painting> paintings = artist.getPaintingArray();
                assertFalse(paintings.isEmpty());
                for (Painting painting : paintings) {
                    assertEquals(PersistenceState.COMMITTED, painting.getPersistenceState());
                    assertNotNull(painting.getPaintingTitle());
                }
            }
        });
    }

    @Test
    public void jointPrefetchSQLSelectNestedJoint() throws Exception {
        createJointPrefetchDataSet();
        SQLSelect.query(Artist.class, "SELECT "
                + "#result('GALLERY_ID' 'int' '' 'paintingArray.toGallery.GALLERY_ID'),"
                + "#result('GALLERY_NAME' 'String' '' 'paintingArray.toGallery.GALLERY_NAME'),"
                + "#result('t0.ARTIST_ID' 'int' '' 'ARTIST_ID') "
                + "FROM ARTIST t0, GALLERY t2 ")
                .addPrefetch(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).joint())
                .select(context);
        env.runWithQueriesBlocked(() -> {
            Persistent g1 = (Persistent) context.getGraphManager().getNode(
                    ObjectId.of("Gallery", Gallery.GALLERY_ID_PK_COLUMN, 33001)
            );
            assertNotNull(g1);
            assertEquals("G1", g1.readProperty("galleryName"));
        });
    }

    @Test
    public void jointPrefetchPreservesPendingToOneArcDiff() throws Exception {
        createJointPrefetchDataSet();

        Artist artist = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq("artist1"))
                .selectFirst(context);

        Painting painting = ObjectSelect.query(Painting.class)
                .where(Painting.PAINTING_TITLE.eq("P_artist21"))
                .selectFirst(context);

        // create pending arc diff
        painting.setToArtist(artist);

        // refresh the painting (should preserve pending arc diff)
        ObjectSelect.query(Painting.class)
                .where(Painting.PAINTING_TITLE.eq("P_artist21"))
                .selectFirst(context);
        assertEquals(artist, painting.getToArtist());

        // refresh the artist (should preserve pending arc diff)
        ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq("artist1"))
                .selectFirst(context);
        assertEquals(artist, painting.getToArtist());

        // refresh them both together (should preserve pending arc diff)
        ObjectSelect.query(Painting.class)
                .where(Painting.PAINTING_TITLE.eq("P_artist21"))
                .prefetch(Painting.TO_ARTIST.joint())
                .selectFirst(context);

        assertEquals(artist, painting.getToArtist());
    }

    @Test
    public void jointPrefetchPreservesPendingToManyArcDiff() throws Exception {
        createJointPrefetchDataSet();

        Artist artist = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq("artist1"))
                .selectFirst(context);

        Painting painting = ObjectSelect.query(Painting.class)
                .where(Painting.PAINTING_TITLE.eq("P_artist21"))
                .selectFirst(context);

        // create pending arc diff
        artist.addToPaintingArray(painting);

        // refresh the painting (should preserve pending arc diff)
        ObjectSelect.query(Painting.class)
                .where(Painting.PAINTING_TITLE.eq("P_artist21"))
                .selectFirst(context);
        assertEquals(3, artist.getPaintingArray().size());
        assertTrue(artist.getPaintingArray().contains(painting));

        // refresh the artist (should preserve pending arc diff)
        ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq("artist1"))
                .selectFirst(context);
        assertEquals(3, artist.getPaintingArray().size());
        assertTrue(artist.getPaintingArray().contains(painting));

        // refresh them both together (should preserve pending arc diff)
        ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq("artist1"))
                .prefetch(Artist.PAINTING_ARRAY.joint())
                .select(context);
        assertEquals(3, artist.getPaintingArray().size());
        assertTrue(artist.getPaintingArray().contains(painting));
    }
}
