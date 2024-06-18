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

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLSelect;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.PaintingInfo;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextDisjointByIdPrefetchIT extends RuntimeCase {

    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected DataChannelInterceptor queryInterceptor;

    private TableHelper tArtist;
    private TableHelper tPainting;
    private TableHelper tPaintingInfo;

    @Before
    public void setUp() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns("PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE").setColumnTypes(Types.INTEGER, Types.BIGINT,
                Types.VARCHAR);

        tPaintingInfo = new TableHelper(dbHelper, "PAINTING_INFO");
        tPaintingInfo.setColumns("PAINTING_ID", "TEXT_REVIEW");
    }

    private void createArtistWithTwoPaintingsDataSet() throws Exception {
        tArtist.insert(1, "X");

        for (int i = 1; i <= 2; i++) {
            tPainting.insert(i, 1, "Y" + i);
        }
    }

    private void createThreeArtistsWithPlentyOfPaintingsDataSet() throws Exception {
        tArtist.insert(1, "bag1");
        tArtist.insert(2, "bag2");
        tArtist.insert(3, "bag3");

        tPainting.insert(1, 1, "box1");
        tPainting.insert(2, 1, "box2");
        tPainting.insert(3, 1, "box3");
        tPainting.insert(4, 1, "box4");
        tPainting.insert(5, 1, "box5");

        tPainting.insert(6, 2, "box6");
        tPainting.insert(7, 2, "box7");

        tPainting.insert(8, 3, "box8");
        tPainting.insert(9, 3, "box9");
        tPainting.insert(10, 3, "box10");
    }

    private void createTwoPaintingsWithInfosDataSet() throws Exception {
        tArtist.insert(1, "bag1");

        tPainting.insert(1, 1, "big");
        tPaintingInfo.insert(1, "red");
        tPainting.insert(2, 1, "small");
        tPaintingInfo.insert(2, "green");
    }

    @Test
    public void testOneToMany() throws Exception {
        createArtistWithTwoPaintingsDataSet();

        List<Artist> result = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTING_ARRAY.disjointById())
                .select(context);

        queryInterceptor.runWithQueriesBlocked(() -> {
            assertFalse(result.isEmpty());
            Artist b1 = result.get(0);
            @SuppressWarnings("unchecked")
            List<Painting> toMany = (List<Painting>) b1.readPropertyDirectly(Artist.PAINTING_ARRAY.getName());
            assertNotNull(toMany);
            assertFalse(((ValueHolder) toMany).isFault());
            assertEquals(2, toMany.size());

            List<String> names = new ArrayList<>();
            for (Painting b : toMany) {
                assertEquals(PersistenceState.COMMITTED, b.getPersistenceState());
                names.add(b.getPaintingTitle());
            }

            assertTrue(names.contains("Y1"));
            assertTrue(names.contains("Y2"));
        });
    }

    @Test
    public void testOneToMany_SQLSelect() throws Exception {
        createArtistWithTwoPaintingsDataSet();

        List<Artist> result = SQLSelect.query(Artist.class, "SELECT "
                + "#result('ARTIST_NAME' 'String'), "
                + "#result('DATE_OF_BIRTH' 'java.util.Date'), "
                + "#result('t0.ARTIST_ID' 'int' '' 'ARTIST_ID') "
                + "FROM ARTIST t0")
                .addPrefetch(Artist.PAINTING_ARRAY.disjointById())
                .select(context);

        queryInterceptor.runWithQueriesBlocked(() -> {
            assertFalse(result.isEmpty());
            Artist b1 = result.get(0);

            @SuppressWarnings("unchecked")
            List<Painting> toMany = (List<Painting>) b1.readPropertyDirectly(Artist.PAINTING_ARRAY.getName());
            assertNotNull(toMany);
            assertFalse(((ValueHolder) toMany).isFault());
            assertEquals(2, toMany.size());

            List<String> names = new ArrayList<>();
            for (Painting b : toMany) {
                assertEquals(PersistenceState.COMMITTED, b.getPersistenceState());
                names.add(b.getPaintingTitle());
            }

            assertTrue(names.contains("Y1"));
            assertTrue(names.contains("Y2"));
        });
    }

    @Test
    public void testManyToOne() throws Exception {
        createArtistWithTwoPaintingsDataSet();

        final List<Painting> result = ObjectSelect.query(Painting.class)
                .prefetch(Painting.TO_ARTIST.disjointById())
                .select(context);

        queryInterceptor.runWithQueriesBlocked(() -> {
            assertFalse(result.isEmpty());
            Painting b1 = result.get(0);
            assertNotNull(b1.getToArtist());
            assertEquals(PersistenceState.COMMITTED, b1.getToArtist().getPersistenceState());
            assertEquals("X", b1.getToArtist().getArtistName());
        });
    }

    @Test
    public void testManyToOne_SQLSelect() throws Exception {
        createArtistWithTwoPaintingsDataSet();

        List<Painting> result = SQLSelect.query(Painting.class, "SELECT "
                + "#result('ESTIMATED_PRICE' 'BigDecimal'), "
                + "#result('PAINTING_TITLE' 'String'), "
                + "#result('PAINTING_DESCRIPTION' 'String'), "
                + "#result('GALLERY_ID' 'int'), "
                + "#result('PAINTING_ID' 'int'), "
                + "#result('ARTIST_ID' 'int') "
                + "FROM PAINTING")
                .addPrefetch(Painting.TO_ARTIST.disjointById())
                .select(context);

        queryInterceptor.runWithQueriesBlocked(() -> {
            assertFalse(result.isEmpty());
            Painting p1 = result.get(0);
            assertEquals(PersistenceState.COMMITTED, p1.getPersistenceState());

            assertNotNull(p1.getToArtist());
            assertEquals(PersistenceState.COMMITTED, p1.getToArtist().getPersistenceState());
            assertEquals("X", p1.getToArtist().getArtistName());
        });
    }

    @Test
    public void testFetchLimit() throws Exception {
        createThreeArtistsWithPlentyOfPaintingsDataSet();

        // There will be only 2 bags in a result. The first bag has 5 boxes and
        // the second has 2. So we are expecting exactly 9 snapshots in the data
        // row store after performing the query.
        final List<Artist> bags = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTING_ARRAY.disjointById())
                .orderBy("db:" + Artist.ARTIST_ID_PK_COLUMN, SortOrder.ASCENDING)
                .limit(2)
                .select(context);

        queryInterceptor.runWithQueriesBlocked(() -> {

            assertEquals(2, bags.size());

            assertEquals(5, bags.get(0).getPaintingArray().size());
            assertEquals(2, bags.get(1).getPaintingArray().size());

            for (Artist b : bags) {
                b.getArtistName();
                for (Painting bx : b.getPaintingArray()) {
                    bx.getPaintingTitle();
                }
            }
        });
    }

    @Test
    public void testOneToOneRelationship() throws Exception {
        createTwoPaintingsWithInfosDataSet();

        final List<Painting> result = ObjectSelect.query(Painting.class)
                .prefetch(Painting.TO_PAINTING_INFO.disjointById())
                .select(context);
        queryInterceptor.runWithQueriesBlocked(() -> {
            assertFalse(result.isEmpty());
            List<String> boxColors = new ArrayList<>();
            for (Painting box : result) {
                PaintingInfo info = (PaintingInfo) box.readPropertyDirectly(Painting.TO_PAINTING_INFO.getName());
                assertNotNull(info);
                boxColors.add(info.getTextReview());
                assertEquals(PersistenceState.COMMITTED, info.getPersistenceState());
            }
            assertTrue(boxColors.containsAll(Arrays.asList("red", "green")));
        });
    }

}
