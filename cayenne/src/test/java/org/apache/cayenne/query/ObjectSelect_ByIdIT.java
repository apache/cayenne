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

package org.apache.cayenne.query;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ObjectSelect_ByIdIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private TableHelper tArtist;
    private TableHelper tPainting;

    @BeforeEach
    public void setUp() {
        tArtist = env.table("ARTIST").setColumns("ARTIST_ID", "ARTIST_NAME");
        tPainting = env.table("PAINTING").setColumns("PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE")
                .setColumnTypes(Types.INTEGER, Types.BIGINT, Types.VARCHAR);
    }

    private void createTwoArtists() throws Exception {
        tArtist.insert(2, "artist2");
        tArtist.insert(3, "artist3");
    }

    // --- scalar ids ---

    @Test
    public void intPk() throws Exception {
        createTwoArtists();

        Artist a3 = ObjectSelect.query(Artist.class).where(Artist.SELF.eqId(3)).selectOne(env.context());
        assertNotNull(a3);
        assertEquals("artist3", a3.getArtistName());

        Artist a2 = ObjectSelect.query(Artist.class).where(Artist.SELF.eqId(2)).selectOne(env.context());
        assertNotNull(a2);
        assertEquals("artist2", a2.getArtistName());
    }

    @Test
    public void intPkSelectFirst() throws Exception {
        createTwoArtists();

        Artist a3 = ObjectSelect.query(Artist.class).where(Artist.SELF.eqId(3)).selectFirst(env.context());
        assertNotNull(a3);
        assertEquals("artist3", a3.getArtistName());
    }

    @Test
    public void nullPk() {
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(Artist.SELF.eqId(null)).select(env.context());
        assertEquals(0, artists.size());
    }

    @Test
    public void intPkMulti() throws Exception {
        createTwoArtists();

        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(Artist.SELF.idsIn(2, 3)).orderBy(Artist.ARTIST_NAME.asc()).select(env.context());
        assertEquals(2, artists.size());
        assertEquals("artist2", artists.get(0).getArtistName());
        assertEquals("artist3", artists.get(1).getArtistName());
    }

    @Test
    public void intPkCollection() throws Exception {
        createTwoArtists();

        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(Artist.SELF.idsInCollection(Arrays.asList(2, 3)))
                .orderBy(Artist.ARTIST_NAME.asc()).select(env.context());
        assertEquals(2, artists.size());
    }

    @Test
    public void emptyPkCollection() {
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(Artist.SELF.idsInCollection(Collections.emptyList())).select(env.context());
        assertEquals(0, artists.size());
    }

    // --- map ids ---

    @Test
    public void mapPk() throws Exception {
        createTwoArtists();

        Artist a3 = ObjectSelect.query(Artist.class)
                .where(Artist.SELF.eqIdMap(singletonMap(Artist.ARTIST_ID_PK_COLUMN, 3)))
                .selectOne(env.context());
        assertNotNull(a3);
        assertEquals("artist3", a3.getArtistName());
    }

    @Test
    public void mapPkMulti() throws Exception {
        createTwoArtists();

        Map<String, ?> id2 = singletonMap(Artist.ARTIST_ID_PK_COLUMN, 2);
        Map<String, ?> id3 = singletonMap(Artist.ARTIST_ID_PK_COLUMN, 3);

        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(Artist.SELF.idMapsIn(id2, id3))
                .orderBy(Artist.ARTIST_NAME.asc()).select(env.context());
        assertEquals(2, artists.size());
        assertEquals("artist2", artists.get(0).getArtistName());
    }

    @Test
    public void mapPkCollection() throws Exception {
        createTwoArtists();

        List<Map<String, ?>> ids = Arrays.asList(
                singletonMap(Artist.ARTIST_ID_PK_COLUMN, 2),
                singletonMap(Artist.ARTIST_ID_PK_COLUMN, 3));

        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(Artist.SELF.idMapsInCollection(ids)).select(env.context());
        assertEquals(2, artists.size());
    }

    @Test
    public void emptyMapPkCollection() {
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(Artist.SELF.idMapsInCollection(Collections.emptyList())).select(env.context());
        assertEquals(0, artists.size());
    }

    // --- ObjectId ids ---

    @Test
    public void objectIdPk() throws Exception {
        createTwoArtists();

        ObjectId id = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 3);
        Artist a3 = ObjectSelect.query(Artist.class).where(Artist.SELF.eqId(id)).selectOne(env.context());
        assertNotNull(a3);
        assertEquals("artist3", a3.getArtistName());
    }

    @Test
    public void objectIdPkMulti() throws Exception {
        createTwoArtists();

        ObjectId id2 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 2);
        ObjectId id3 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 3);

        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(Artist.SELF.objectIdsIn(id2, id3))
                .orderBy(Artist.ARTIST_NAME.asc()).select(env.context());
        assertEquals(2, artists.size());
        assertEquals("artist2", artists.get(0).getArtistName());
    }

    @Test
    public void objectIdPkCollection() throws Exception {
        createTwoArtists();

        List<ObjectId> ids = Arrays.asList(
                ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 2),
                ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 3));

        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(Artist.SELF.objectIdsInCollection(ids)).select(env.context());
        assertEquals(2, artists.size());
    }

    // --- DataRows: the "dataRowQuery*" equivalent is chaining fetchDataRows() ---

    @Test
    public void dataRowIntPk() throws Exception {
        createTwoArtists();

        DataRow row = ObjectSelect.query(Artist.class).where(Artist.SELF.eqId(3))
                .fetchDataRows().selectOne(env.context());
        assertNotNull(row);
        assertEquals("artist3", row.get("ARTIST_NAME"));
    }

    @Test
    public void dataRowIntPkMulti() throws Exception {
        createTwoArtists();

        List<DataRow> rows = ObjectSelect.query(Artist.class).where(Artist.SELF.idsIn(2, 3))
                .fetchDataRows().select(env.context());
        assertEquals(2, rows.size());
    }

    @Test
    public void dataRowObjectIdPk() throws Exception {
        createTwoArtists();

        ObjectId id = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 3);
        DataRow row = ObjectSelect.query(Artist.class).where(Artist.SELF.eqId(id))
                .fetchDataRows().selectOne(env.context());
        assertEquals("artist3", row.get("ARTIST_NAME"));
    }

    // --- caching and prefetching still work off a by-id qualifier ---

    @Test
    public void localCache() throws Exception {
        createTwoArtists();

        final Artist[] a3 = new Artist[1];

        assertEquals(1, env.runWithQueryCounter(() -> {
            a3[0] = ObjectSelect.query(Artist.class).where(Artist.SELF.eqId(3))
                    .localCache("g1").selectOne(env.context());
            assertNotNull(a3[0]);
            assertEquals("artist3", a3[0].getArtistName());
        }));

        env.runWithQueriesBlocked(() -> {
            Artist a3cached = ObjectSelect.query(Artist.class).where(Artist.SELF.eqId(3))
                    .localCache("g1").selectOne(env.context());
            assertSame(a3[0], a3cached);
        });

        env.context().performGenericQuery(new RefreshQuery("g1"));

        assertEquals(1, env.runWithQueryCounter(() -> ObjectSelect.query(Artist.class)
                .where(Artist.SELF.eqId(3)).localCache("g1").selectOne(env.context())));
    }

    @Test
    public void prefetch() throws Exception {
        createTwoArtists();
        tPainting.insert(45, 3, "One");
        tPainting.insert(48, 3, "Two");

        Artist a3 = ObjectSelect.query(Artist.class).where(Artist.SELF.eqId(3))
                .prefetch(Artist.PAINTING_ARRAY.joint())
                .selectOne(env.context());

        env.runWithQueriesBlocked(() -> {
            assertNotNull(a3);
            assertEquals("artist3", a3.getArtistName());
            assertEquals(2, a3.getPaintingArray().size());

            a3.getPaintingArray().get(0).getPaintingTitle();
            a3.getPaintingArray().get(1).getPaintingTitle();
        });
    }

    /**
     * A disjoint prefetch rebases the query qualifier onto the prefetched entity. A bare "self"
     * reference has no path to rebase, so it must be resolved to the root PK first - otherwise it
     * silently reads as the *target* entity's PK.
     */
    @Test
    public void prefetchDisjoint() throws Exception {
        createTwoArtists();
        tPainting.insert(45, 3, "One");
        tPainting.insert(48, 3, "Two");

        Artist a3 = ObjectSelect.query(Artist.class).where(Artist.SELF.eqId(3))
                .prefetch(Artist.PAINTING_ARRAY.disjoint())
                .selectOne(env.context());

        env.runWithQueriesBlocked(() -> {
            assertNotNull(a3);
            assertEquals(2, a3.getPaintingArray().size());
        });
    }

    // --- iterated results, mirroring SelectByIdIteratedQueryIT ---

    @Test
    public void queryWithBatchIterator() throws Exception {
        createSixPaintings();

        try (ResultBatchIterator<Painting> iterator = ObjectSelect.query(Painting.class)
                .where(Painting.SELF.idsInCollection(Arrays.asList(1, 2, 3, 4, 5, 6)))
                .batchIterator(env.context(), 4)) {

            int count = 0;
            while (iterator.hasNext()) {
                count++;
                for (Painting painting : iterator.next()) {
                    assertEquals("painting" + Cayenne.longPKForObject(painting), painting.getPaintingTitle());
                }
            }
            assertEquals(2, count);
        }
    }

    @Test
    public void queryWithIterator() throws Exception {
        createSixPaintings();

        try (ResultIterator<Painting> iterator = ObjectSelect.query(Painting.class)
                .where(Painting.SELF.idsInCollection(Arrays.asList(1, 2, 3, 4, 5, 6)))
                .iterator(env.context())) {

            int count = 0;
            while (iterator.hasNextRow()) {
                count++;
                Painting painting = iterator.nextRow();
                assertEquals("painting" + Cayenne.longPKForObject(painting), painting.getPaintingTitle());
            }
            assertEquals(6, count);
        }
    }

    private void createSixPaintings() throws Exception {
        tArtist.insert(1, "artist1");
        for (int i = 1; i <= 6; i++) {
            tPainting.insert(i, 1, "painting" + i);
        }
    }
}
