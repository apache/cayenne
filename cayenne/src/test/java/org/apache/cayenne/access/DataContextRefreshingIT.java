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

import org.apache.cayenne.Fault;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.PaintingInfo;
import org.apache.cayenne.testdo.testmap.ROArtist;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Types;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite covering possible scenarios of refreshing updated objects. This includes
 * refreshing relationships and attributes changed outside of Cayenne with and without
 * prefetching.
 */
public class DataContextRefreshingIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    protected DataContext context;
    protected TableHelper tArtist;
    protected TableHelper tPainting;
    protected TableHelper tPaintingInfo;

    @BeforeEach
    public void setUp() throws Exception {
        context = env.context();
        tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");
        tPaintingInfo = env.table("PAINTING_INFO", "PAINTING_ID", "TEXT_REVIEW");

        tPainting = env.table("PAINTING").setColumns(
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

    @Test
    public void refetchRootWithUpdatedAttributes() throws Exception {

        createSingleArtistDataSet();

        String nameBefore = "artist2";
        String nameAfter = "not an artist";

        ObjectSelect<Artist> queryBefore = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq(nameBefore));

        Artist artist = (Artist) context.select(queryBefore).get(0);
        assertEquals(nameBefore, artist.getArtistName());

        assertEquals(1, tArtist.update().set("ARTIST_NAME", nameAfter).execute());

        // fetch into the same context
        List<Artist> artists = queryBefore.select(context);
        assertEquals(0, artists.size());

        ObjectSelect<Artist> queryAfter = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq(nameAfter));

        artist = (Artist) context.select(queryAfter).get(0);
        assertNotNull(artist);
        assertEquals(nameAfter, artist.getArtistName());
    }

    @Test
    public void refetchRootWithNullifiedToOne() throws Exception {
        createSingleArtistAndPaintingDataSet();

        Painting painting = (Painting) context.select(
                ObjectSelect.query(Painting.class)).get(0);

        assertNotNull(painting.getToArtist());
        assertEquals("artist2", painting.getToArtist().getArtistName());

        assertEquals(1, tPainting.update().set("ARTIST_ID", null, Types.BIGINT).execute());

        // select without prefetch
        painting = (Painting) context
                .select(ObjectSelect.query(Painting.class))
                .get(0);
        assertNotNull(painting);
        assertNull(painting.getToArtist());
    }

    @Test
    public void refetchRootWithChangedToOneTarget() throws Exception {
        createTwoArtistsAndPaintingDataSet();

        Painting painting = (Painting) context.select(
                ObjectSelect.query(Painting.class)).get(0);

        Artist artistBefore = painting.getToArtist();
        assertNotNull(artistBefore);
        assertEquals("artist2", artistBefore.getArtistName());

        assertEquals(1, tPainting.update().set("ARTIST_ID", 6).execute());

        // select without prefetch
        painting = (Painting) context
                .select(ObjectSelect.query(Painting.class))
                .get(0);
        assertNotNull(painting);
        assertEquals("artist3", painting.getToArtist().getArtistName());
    }

    @Test
    public void refetchRootWithNullToOneTargetChangedToNotNull() throws Exception {
        createSingleArtistAndUnrelatedPaintingDataSet();

        Painting painting = (Painting) context.select(
                ObjectSelect.query(Painting.class)).get(0);

        assertNull(painting.getToArtist());

        assertEquals(1, tPainting.update().set("ARTIST_ID", 5).execute());

        // select without prefetch
        painting = (Painting) context
                .select(ObjectSelect.query(Painting.class))
                .get(0);
        assertNotNull(painting);
        assertEquals("artist2", painting.getToArtist().getArtistName());
    }

    @Test
    public void refetchRootKeepsUnchangedToOne() throws Exception {
        createSingleArtistAndPaintingDataSet();

        Painting painting = ObjectSelect.query(Painting.class).selectFirst(context);
        Artist artist = painting.getToArtist();
        assertNotNull(artist);

        // select without prefetch: the FK did not change, so the resolved target must survive the refresh
        painting = ObjectSelect.query(Painting.class).selectFirst(context);
        assertSame(artist, painting.readPropertyDirectly(Painting.TO_ARTIST.getName()));
    }

    @Test
    public void invalidateRootRefaultsUnchangedToOne() throws Exception {
        createSingleArtistAndPaintingDataSet();

        Painting painting = ObjectSelect.query(Painting.class).selectFirst(context);
        Artist artist = painting.getToArtist();
        assertNotNull(artist);

        // unlike a plain refetch, invalidation forgets the arcs even when the FK did not change
        context.invalidateObjects(painting);
        painting = ObjectSelect.query(Painting.class).selectFirst(context);
        assertEquals(PersistenceState.COMMITTED, painting.getPersistenceState());
        assertInstanceOf(Fault.class, painting.readPropertyDirectly(Painting.TO_ARTIST.getName()));
        assertSame(artist, painting.getToArtist());
    }

    @Test
    public void refetchRootRefaultsToDependentPkToOne() throws Exception {
        createSingleArtistAndPaintingDataSet();
        tPaintingInfo.insert(4, "review");

        Painting painting = ObjectSelect.query(Painting.class).selectFirst(context);
        PaintingInfo info = painting.getToPaintingInfo();
        assertNotNull(info);

        // the source row says nothing about the presence of a dependent row, so the arc must be refaulted
        painting = ObjectSelect.query(Painting.class).selectFirst(context);
        assertInstanceOf(Fault.class, painting.readPropertyDirectly(Painting.TO_PAINTING_INFO.getName()));
    }

    @Test
    public void refetchReadOnlyRootKeepsToMany() throws Exception {
        createSingleArtistAndPaintingDataSet();

        ROArtist artist = ObjectSelect.query(ROArtist.class)
                .prefetch(ROArtist.PAINTING_ARRAY.disjoint())
                .selectFirst(context);
        ToManyHolder<?> paintings = (ToManyHolder<?>) artist.readPropertyDirectly(ROArtist.PAINTING_ARRAY.getName());
        assertFalse(paintings.isFault());

        // select without prefetch: a COMMITTED object keeps its to-many lists, read-only or not
        artist = ObjectSelect.query(ROArtist.class).selectFirst(context);
        assertSame(paintings, artist.readPropertyDirectly(ROArtist.PAINTING_ARRAY.getName()));
        assertFalse(paintings.isFault());
    }

    @Test
    public void invalidateReadOnlyRootRefaultsToMany() throws Exception {
        createSingleArtistAndPaintingDataSet();

        ROArtist artist = ObjectSelect.query(ROArtist.class)
                .prefetch(ROArtist.PAINTING_ARRAY.disjoint())
                .selectFirst(context);
        ToManyHolder<?> paintings = (ToManyHolder<?>) artist.readPropertyDirectly(ROArtist.PAINTING_ARRAY.getName());
        assertFalse(paintings.isFault());

        context.invalidateObjects(artist);
        artist = ObjectSelect.query(ROArtist.class).selectFirst(context);
        paintings = (ToManyHolder<?>) artist.readPropertyDirectly(ROArtist.PAINTING_ARRAY.getName());
        assertTrue(paintings.isFault());
    }

    @Test
    public void refetchRootWithDeletedToMany() throws Exception {
        createSingleArtistAndPaintingDataSet();

        Artist artist = (Artist) context.select(ObjectSelect.query(Artist.class)).get(
                0);
        assertEquals(artist.getPaintingArray().size(), 1);

        assertEquals(1, tPainting
                .delete()
                .where(Painting.PAINTING_ID_PK_COLUMN, 4)
                .execute());

        // select without prefetch
        artist = (Artist) context.select(ObjectSelect.query(Artist.class)).get(0);
        assertEquals(artist.getPaintingArray().size(), 1);

        // select using relationship prefetching
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTING_ARRAY.disjoint());
        artist = (Artist) context.select(query).get(0);
        assertEquals(0, artist.getPaintingArray().size());
    }

    @Test
    public void refetchRootWithAddedToMany() throws Exception {

        createSingleArtistDataSet();

        Artist artist = (Artist) context.select(ObjectSelect.query(Artist.class)).get(
                0);
        assertEquals(artist.getPaintingArray().size(), 0);

        tPainting.insert(5, "p", 5, 1000);

        // select without prefetch
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class);
        artist = (Artist) context.select(query).get(0);
        assertEquals(artist.getPaintingArray().size(), 0);

        // select using relationship prefetching
        query.prefetch(Artist.PAINTING_ARRAY.disjoint());
        artist = (Artist) context.select(query).get(0);
        assertEquals(artist.getPaintingArray().size(), 1);
    }

    @Test
    public void invalidateRootWithUpdatedAttributes() throws Exception {
        createSingleArtistDataSet();

        String nameBefore = "artist2";
        String nameAfter = "not an artist";

        Artist artist = (Artist) context.select(ObjectSelect.query(Artist.class)).get(0);
        assertNotNull(artist);
        assertEquals(nameBefore, artist.getArtistName());

        // update via DataNode directly
        assertEquals(1, tArtist.update().set("ARTIST_NAME", nameAfter).execute());

        context.invalidateObjects(artist);
        assertEquals(nameAfter, artist.getArtistName());
    }

    @Test
    public void invalidateRootWithNullifiedToOne() throws Exception {

        createSingleArtistAndPaintingDataSet();

        Painting painting = (Painting) context.select(ObjectSelect.query(Painting.class)).get(0);

        assertNotNull(painting.getToArtist());
        assertEquals("artist2", painting.getToArtist().getArtistName());

        assertEquals(1, tPainting.update().set("ARTIST_ID", null, Types.BIGINT).execute());

        context.invalidateObjects(painting);
        assertNull(painting.getToArtist());
    }

    @Test
    public void invalidateRootWithChangedToOneTarget() throws Exception {
        createTwoArtistsAndPaintingDataSet();

        Painting painting = (Painting) context.select(
                ObjectSelect.query(Painting.class)).get(0);
        Artist artistBefore = painting.getToArtist();
        assertNotNull(artistBefore);
        assertEquals("artist2", artistBefore.getArtistName());

        assertEquals(1, tPainting.update().set("ARTIST_ID", 6).execute());

        context.invalidateObjects(painting);
        assertNotSame(artistBefore, painting.getToArtist());
        assertEquals("artist3", painting.getToArtist().getArtistName());
    }

    @Test
    public void invalidateRootWithNullToOneTargetChangedToNotNull() throws Exception {
        createSingleArtistAndUnrelatedPaintingDataSet();

        Painting painting = (Painting) context.select(
                ObjectSelect.query(Painting.class)).get(0);
        assertNull(painting.getToArtist());

        assertEquals(1, tPainting.update().set("ARTIST_ID", 5).execute());

        context.invalidateObjects(painting);
        assertNotNull(painting.getToArtist());
        assertEquals("artist2", painting.getToArtist().getArtistName());
    }

    @Test
    public void invalidateRootWithDeletedToMany() throws Exception {
        createSingleArtistAndPaintingDataSet();

        Artist artist = (Artist) context.select(ObjectSelect.query(Artist.class)).get(0);
        assertEquals(artist.getPaintingArray().size(), 1);

        assertEquals(1, tPainting.delete().execute());

        context.invalidateObjects(artist);
        assertEquals(artist.getPaintingArray().size(), 0);
    }

    @Test
    public void invaliateRootWithAddedToMany() throws Exception {

        createSingleArtistDataSet();

        Artist artist = (Artist) context.select(ObjectSelect.query(Artist.class)).get(0);
        assertEquals(artist.getPaintingArray().size(), 0);

        tPainting.insert(4, "p", 5, 1000);

        assertEquals(artist.getPaintingArray().size(), 0);
        context.invalidateObjects(artist);
        assertEquals(artist.getPaintingArray().size(), 1);
    }

    @Test
    public void invalidateThenModify() throws Exception {

        createSingleArtistDataSet();

        final Artist artist = (Artist) context
                .select(ObjectSelect.query(Artist.class))
                .get(0);
        assertNotNull(artist);

        context.invalidateObjects(artist);
        assertEquals(PersistenceState.HOLLOW, artist.getPersistenceState());

        int queries = env.runWithQueryCounter(() -> {
            // this must trigger a fetch
            artist.setArtistName("new name");
        });

        assertEquals(1, queries);
        assertEquals(PersistenceState.MODIFIED, artist.getPersistenceState());
    }

    @Test
    public void modifyHollow() throws Exception {

        createSingleArtistAndPaintingDataSet();

        Painting painting = (Painting) context
                .select(ObjectSelect.query(Painting.class)).get(0);
        final Artist artist = painting.getToArtist();
        assertEquals(PersistenceState.HOLLOW, artist.getPersistenceState());
        assertNull(artist.readPropertyDirectly("artistName"));

        int queries = env.runWithQueryCounter(() -> {
            // this must trigger a fetch
            artist.setDateOfBirth(new Date());
        });

        assertEquals(1, queries);

        assertEquals(PersistenceState.MODIFIED, artist.getPersistenceState());
        assertNotNull(artist.readPropertyDirectly("artistName"));
    }
}
