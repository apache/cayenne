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

import java.sql.Types;
import java.util.Date;
import java.util.List;

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

/**
 * Test suite covering possible scenarios of refreshing updated objects. This includes
 * refreshing relationships and attributes changed outside of Cayenne with and without
 * prefetching.
 */
@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextRefreshingTest extends ServerCase {

    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected DataChannelInterceptor queryInterceptor;

    protected TableHelper tArtist;
    protected TableHelper tPainting;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");

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

    protected void createSingleArtistDataSet() throws Exception {
        tArtist.insert(5, "artist2");
    }

    protected void createSingleArtistAndPaintingDataSet() throws Exception {
        createSingleArtistDataSet();
        tPainting.insert(4, "p", 5, 1000);
    }

    protected void createSingleArtistAndUnrelatedPaintingDataSet() throws Exception {
        createSingleArtistDataSet();
        tPainting.insert(4, "p", null, 1000);
    }

    protected void createTwoArtistsAndPaintingDataSet() throws Exception {
        tArtist.insert(5, "artist2");
        tArtist.insert(6, "artist3");
        tPainting.insert(4, "p", 5, 1000);
    }

    public void testRefetchRootWithUpdatedAttributes() throws Exception {

        createSingleArtistDataSet();

        String nameBefore = "artist2";
        String nameAfter = "not an artist";

        SelectQuery queryBefore = new SelectQuery(
                Artist.class,
                ExpressionFactory.matchExp("artistName", nameBefore));

        Artist artist = (Artist) context.performQuery(queryBefore).get(0);
        assertEquals(nameBefore, artist.getArtistName());

        assertEquals(1, tArtist.update().set("ARTIST_NAME", nameAfter).execute());

        // fetch into the same context
        List<Artist> artists = context.performQuery(queryBefore);
        assertEquals(0, artists.size());

        SelectQuery queryAfter = new SelectQuery(
                Artist.class,
                ExpressionFactory.matchExp("artistName", nameAfter));

        artist = (Artist) context.performQuery(queryAfter).get(0);
        assertNotNull(artist);
        assertEquals(nameAfter, artist.getArtistName());
    }

    public void testRefetchRootWithNullifiedToOne() throws Exception {
        createSingleArtistAndPaintingDataSet();

        Painting painting = (Painting) context.performQuery(
                new SelectQuery(Painting.class)).get(0);

        assertNotNull(painting.getToArtist());
        assertEquals("artist2", painting.getToArtist().getArtistName());

        assertEquals(1, tPainting.update().set("ARTIST_ID", null, Types.BIGINT).execute());

        // select without prefetch
        painting = (Painting) context
                .performQuery(new SelectQuery(Painting.class))
                .get(0);
        assertNotNull(painting);
        assertNull(painting.getToArtist());
    }

    public void testRefetchRootWithChangedToOneTarget() throws Exception {
        createTwoArtistsAndPaintingDataSet();

        Painting painting = (Painting) context.performQuery(
                new SelectQuery(Painting.class)).get(0);

        Artist artistBefore = painting.getToArtist();
        assertNotNull(artistBefore);
        assertEquals("artist2", artistBefore.getArtistName());

        assertEquals(1, tPainting.update().set("ARTIST_ID", 6).execute());

        // select without prefetch
        painting = (Painting) context
                .performQuery(new SelectQuery(Painting.class))
                .get(0);
        assertNotNull(painting);
        assertEquals("artist3", painting.getToArtist().getArtistName());
    }

    public void testRefetchRootWithNullToOneTargetChangedToNotNull() throws Exception {
        createSingleArtistAndUnrelatedPaintingDataSet();

        Painting painting = (Painting) context.performQuery(
                new SelectQuery(Painting.class)).get(0);

        assertNull(painting.getToArtist());

        assertEquals(1, tPainting.update().set("ARTIST_ID", 5).execute());

        // select without prefetch
        painting = (Painting) context
                .performQuery(new SelectQuery(Painting.class))
                .get(0);
        assertNotNull(painting);
        assertEquals("artist2", painting.getToArtist().getArtistName());
    }

    public void testRefetchRootWithDeletedToMany() throws Exception {
        createSingleArtistAndPaintingDataSet();

        Artist artist = (Artist) context.performQuery(new SelectQuery(Artist.class)).get(
                0);
        assertEquals(artist.getPaintingArray().size(), 1);

        assertEquals(1, tPainting
                .delete()
                .where(Painting.PAINTING_ID_PK_COLUMN, 4)
                .execute());

        // select without prefetch
        artist = (Artist) context.performQuery(new SelectQuery(Artist.class)).get(0);
        assertEquals(artist.getPaintingArray().size(), 1);

        // select using relationship prefetching
        SelectQuery query = new SelectQuery(Artist.class);
        query.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY);
        artist = (Artist) context.performQuery(query).get(0);
        assertEquals(0, artist.getPaintingArray().size());
    }

    public void testRefetchRootWithAddedToMany() throws Exception {

        createSingleArtistDataSet();

        Artist artist = (Artist) context.performQuery(new SelectQuery(Artist.class)).get(
                0);
        assertEquals(artist.getPaintingArray().size(), 0);

        tPainting.insert(5, "p", 5, 1000);

        // select without prefetch
        SelectQuery query = new SelectQuery(Artist.class);
        artist = (Artist) context.performQuery(query).get(0);
        assertEquals(artist.getPaintingArray().size(), 0);

        // select using relationship prefetching
        query.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY);
        artist = (Artist) context.performQuery(query).get(0);
        assertEquals(artist.getPaintingArray().size(), 1);
    }

    public void testInvalidateRootWithUpdatedAttributes() throws Exception {
        createSingleArtistDataSet();

        String nameBefore = "artist2";
        String nameAfter = "not an artist";

        Artist artist = (Artist) context.performQuery(new SelectQuery(Artist.class)).get(
                0);
        assertNotNull(artist);
        assertEquals(nameBefore, artist.getArtistName());

        // update via DataNode directly
        assertEquals(1, tArtist.update().set("ARTIST_NAME", nameAfter).execute());

        context.invalidateObjects(artist);
        assertEquals(nameAfter, artist.getArtistName());
    }

    public void testInvalidateRootWithNullifiedToOne() throws Exception {

        createSingleArtistAndPaintingDataSet();

        Painting painting = (Painting) context.performQuery(
                new SelectQuery(Painting.class)).get(0);

        assertNotNull(painting.getToArtist());
        assertEquals("artist2", painting.getToArtist().getArtistName());

        assertEquals(1, tPainting.update().set("ARTIST_ID", null, Types.BIGINT).execute());

        context.invalidateObjects(painting);
        assertNull(painting.getToArtist());
    }

    public void testInvalidateRootWithChangedToOneTarget() throws Exception {
        createTwoArtistsAndPaintingDataSet();

        Painting painting = (Painting) context.performQuery(
                new SelectQuery(Painting.class)).get(0);
        Artist artistBefore = painting.getToArtist();
        assertNotNull(artistBefore);
        assertEquals("artist2", artistBefore.getArtistName());

        assertEquals(1, tPainting.update().set("ARTIST_ID", 6).execute());

        context.invalidateObjects(painting);
        assertNotSame(artistBefore, painting.getToArtist());
        assertEquals("artist3", painting.getToArtist().getArtistName());
    }

    public void testInvalidateRootWithNullToOneTargetChangedToNotNull() throws Exception {
        createSingleArtistAndUnrelatedPaintingDataSet();

        Painting painting = (Painting) context.performQuery(
                new SelectQuery(Painting.class)).get(0);
        assertNull(painting.getToArtist());

        assertEquals(1, tPainting.update().set("ARTIST_ID", 5).execute());

        context.invalidateObjects(painting);
        assertNotNull(painting.getToArtist());
        assertEquals("artist2", painting.getToArtist().getArtistName());
    }

    public void testInvalidateRootWithDeletedToMany() throws Exception {
        createSingleArtistAndPaintingDataSet();

        Artist artist = (Artist) context.performQuery(new SelectQuery(Artist.class)).get(
                0);
        assertEquals(artist.getPaintingArray().size(), 1);

        assertEquals(1, tPainting.delete().execute());

        context.invalidateObjects(artist);
        assertEquals(artist.getPaintingArray().size(), 0);
    }

    public void testInvaliateRootWithAddedToMany() throws Exception {

        createSingleArtistDataSet();

        Artist artist = (Artist) context.performQuery(new SelectQuery(Artist.class)).get(
                0);
        assertEquals(artist.getPaintingArray().size(), 0);

        tPainting.insert(4, "p", 5, 1000);

        assertEquals(artist.getPaintingArray().size(), 0);
        context.invalidateObjects(artist);
        assertEquals(artist.getPaintingArray().size(), 1);
    }

    public void testInvalidateThenModify() throws Exception {

        createSingleArtistDataSet();

        final Artist artist = (Artist) context
                .performQuery(new SelectQuery(Artist.class))
                .get(0);
        assertNotNull(artist);

        context.invalidateObjects(artist);
        assertEquals(PersistenceState.HOLLOW, artist.getPersistenceState());

        int queries = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                // this must trigger a fetch
                artist.setArtistName("new name");
            }
        });

        assertEquals(1, queries);
        assertEquals(PersistenceState.MODIFIED, artist.getPersistenceState());
    }

    public void testModifyHollow() throws Exception {

        createSingleArtistAndPaintingDataSet();

        Painting painting = (Painting) context.performQuery(
                new SelectQuery(Painting.class)).get(0);
        final Artist artist = painting.getToArtist();
        assertEquals(PersistenceState.HOLLOW, artist.getPersistenceState());
        assertNull(artist.readPropertyDirectly("artistName"));

        int queries = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                // this must trigger a fetch
                artist.setDateOfBirth(new Date());
            }
        });

        assertEquals(1, queries);

        assertEquals(PersistenceState.MODIFIED, artist.getPersistenceState());
        assertNotNull(artist.readPropertyDirectly("artistName"));
    }
}
