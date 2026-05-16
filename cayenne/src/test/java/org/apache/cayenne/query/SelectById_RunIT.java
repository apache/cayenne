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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.EntityResolver;
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
import static org.junit.jupiter.api.Assertions.*;

public class SelectById_RunIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private TableHelper tArtist;
    private TableHelper tPainting;

    private EntityResolver resolver;

    @BeforeEach
    public void setUp() throws Exception {
        resolver = env.entityResolver();
        tArtist = env.table("ARTIST").setColumns("ARTIST_ID", "ARTIST_NAME");
        tPainting = env.table("PAINTING").setColumns("PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE")
                .setColumnTypes(Types.INTEGER, Types.BIGINT, Types.VARCHAR);
    }

    private void createTwoArtists() throws Exception {
        tArtist.insert(2, "artist2");
        tArtist.insert(3, "artist3");
    }

    @Test
    public void intPk() throws Exception {
        createTwoArtists();

        Artist a3 = SelectById.queryId(Artist.class, 3).selectOne(env.context());
        assertNotNull(a3);
        assertEquals("artist3", a3.getArtistName());

        Artist a2 = SelectById.queryId(Artist.class, 2).selectOne(env.context());
        assertNotNull(a2);
        assertEquals("artist2", a2.getArtistName());
    }

    @Test
    public void nullPk() {
        List<Artist> artists = SelectById.queryId(Artist.class, null).select(env.context());
        assertEquals(0, artists.size());
    }

    @Test
    public void dataRowNullPk() {
        List<DataRow> artists = SelectById.dataRowQueryId(Artist.class, null).select(env.context());
        assertEquals(0, artists.size());
    }

    @Test
    public void emptyPkMulti() {
        List<Artist> artists = SelectById.queryIds(Artist.class).select(env.context());
        assertEquals(0, artists.size());
    }

    @Test
    public void emptyPkCollection() {
        List<Artist> artists = SelectById.queryIdsCollection(Artist.class, Collections.emptyList()).select(env.context());
        assertEquals(0, artists.size());
    }

    @Test
    public void emptyMapPkMulti() {
        List<Artist> artists = SelectById.queryMaps(Artist.class).select(env.context());
        assertEquals(0, artists.size());
    }

    @Test
    public void emptyMapPkCollection() {
        List<Artist> artists = SelectById.queryMapsCollection(Artist.class, Collections.emptyList()).select(env.context());
        assertEquals(0, artists.size());
    }

    @Test
    public void dataRowEmptyPkMulti() {
        List<DataRow> artists = SelectById.dataRowQueryIds(Artist.class).select(env.context());
        assertEquals(0, artists.size());
    }

    @Test
    public void dataRowEmptyPkCollection() {
        List<DataRow> artists = SelectById.dataRowQueryIdsCollection(Artist.class, Collections.emptyList()).select(env.context());
        assertEquals(0, artists.size());
    }

    @Test
    public void dataRowEmptyMapPkMulti() {
        List<DataRow> artists = SelectById.dataRowQueryMaps(Artist.class).select(env.context());
        assertEquals(0, artists.size());
    }

    @Test
    public void dataRowEmptyMapPkCollection() {
        List<DataRow> artists = SelectById.dataRowQueryMapsCollection(Artist.class, Collections.emptyList()).select(env.context());
        assertEquals(0, artists.size());
    }

    @Test
    public void intPkMulti() throws Exception {
        createTwoArtists();

        List<Artist> artists = SelectById.queryIds(Artist.class, 2, 3)
                .select(env.context());
        assertEquals(2, artists.size());
        assertInstanceOf(Artist.class, artists.get(0));
    }

    @Test
    public void intPkCollection() throws Exception {
        createTwoArtists();

        List<Artist> artists = SelectById.queryIdsCollection(Artist.class, Arrays.asList(1, 2, 3, 4, 5))
                .select(env.context());
        assertEquals(2, artists.size());
        assertInstanceOf(Artist.class, artists.get(0));
    }

    @Test
    public void mapPk() throws Exception {
        createTwoArtists();

        Artist a3 = SelectById.queryMap(Artist.class, singletonMap(Artist.ARTIST_ID_PK_COLUMN, 3)).selectOne(env.context());
        assertNotNull(a3);
        assertEquals("artist3", a3.getArtistName());

        Artist a2 = SelectById.queryMap(Artist.class, singletonMap(Artist.ARTIST_ID_PK_COLUMN, 2)).selectOne(env.context());
        assertNotNull(a2);
        assertEquals("artist2", a2.getArtistName());
    }

    @Test
    public void mapPkMulti() throws Exception {
        createTwoArtists();

        Map<String, ?> id2 = Collections.singletonMap(Artist.ARTIST_ID_PK_COLUMN, 2);
        Map<String, ?> id3 = Collections.singletonMap(Artist.ARTIST_ID_PK_COLUMN, 3);

        List<Artist> artists = SelectById.queryMaps(Artist.class, id2, id3)
                .select(env.context());
        assertEquals(2, artists.size());
        assertInstanceOf(Artist.class, artists.get(0));
    }

    @Test
    public void mapPkCollection() throws Exception {
        createTwoArtists();

        Map<String, ?> id2 = Collections.singletonMap(Artist.ARTIST_ID_PK_COLUMN, 2);
        Map<String, ?> id3 = Collections.singletonMap(Artist.ARTIST_ID_PK_COLUMN, 3);

        List<Artist> artists = SelectById.queryMapsCollection(Artist.class, Arrays.asList(id2, id3))
                .select(env.context());
        assertEquals(2, artists.size());
        assertInstanceOf(Artist.class, artists.get(0));
    }

    @Test
    public void objectIdPk() throws Exception {
        createTwoArtists();

        ObjectId oid3 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 3);
        Artist a3 = SelectById.queryObjectId(Artist.class, oid3).selectOne(env.context());
        assertNotNull(a3);
        assertEquals("artist3", a3.getArtistName());

        ObjectId oid2 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 2);
        Artist a2 = SelectById.queryObjectId(Artist.class, oid2).selectOne(env.context());
        assertNotNull(a2);
        assertEquals("artist2", a2.getArtistName());
    }

    @Test
    public void objectIdPkMulti() throws Exception {
        createTwoArtists();

        ObjectId oid2 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 2);
        ObjectId oid3 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 3);

        List<Artist> artists = SelectById.queryObjectIds(Artist.class, oid2, oid3)
                .select(env.context());
        assertEquals(2, artists.size());
        assertInstanceOf(Artist.class, artists.get(0));
    }

    @Test
    public void objectIdPkCollection() throws Exception {
        createTwoArtists();

        ObjectId oid2 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 2);
        ObjectId oid3 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 3);

        List<Artist> artists = SelectById.queryObjectIdsCollection(Artist.class, Arrays.asList(oid2, oid3))
                .select(env.context());
        assertEquals(2, artists.size());
        assertInstanceOf(Artist.class, artists.get(0));
    }

    @Test
    public void dataRowIntPk() throws Exception {
        createTwoArtists();

        DataRow a3 = SelectById.dataRowQueryId(Artist.class, 3).selectOne(env.context());
        assertNotNull(a3);
        assertEquals("artist3", a3.get("ARTIST_NAME"));

        DataRow a2 = SelectById.dataRowQueryId(Artist.class, 2).selectOne(env.context());
        assertNotNull(a2);
        assertEquals("artist2", a2.get("ARTIST_NAME"));
    }

    @Test
    public void dataRowMapPk() throws Exception {
        createTwoArtists();

        Map<String, ?> id3 = Collections.singletonMap(Artist.ARTIST_ID_PK_COLUMN, 3);
        DataRow a3 = SelectById.dataRowQueryMap(Artist.class, id3).selectOne(env.context());
        assertNotNull(a3);
        assertEquals("artist3", a3.get("ARTIST_NAME"));

        Map<String, ?> id2 = Collections.singletonMap(Artist.ARTIST_ID_PK_COLUMN, 2);
        DataRow a2 = SelectById.dataRowQueryMap(Artist.class, id2).selectOne(env.context());
        assertNotNull(a2);
        assertEquals("artist2", a2.get("ARTIST_NAME"));
    }

    @Test
    public void dataRowObjectIdPk() throws Exception {
        createTwoArtists();

        ObjectId oid3 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 3);
        DataRow a3 = SelectById.dataRowQueryObjectId(oid3).selectOne(env.context());
        assertNotNull(a3);
        assertEquals("artist3", a3.get("ARTIST_NAME"));

        ObjectId oid2 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 2);
        DataRow a2 = SelectById.dataRowQueryObjectId(oid2).selectOne(env.context());
        assertNotNull(a2);
        assertEquals("artist2", a2.get("ARTIST_NAME"));
    }

    @Test
    public void dataRowIntPkMulti() throws Exception {
        createTwoArtists();

        List<DataRow> artists = SelectById.dataRowQueryIds(Artist.class, 2, 3)
                .select(env.context());
        assertEquals(2, artists.size());
        assertInstanceOf(DataRow.class, artists.get(0));
    }

    @Test
    public void dataRowObjectIdPkMulti() throws Exception {
        createTwoArtists();

        ObjectId oid2 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 2);
        ObjectId oid3 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 3);

        List<DataRow> artists = SelectById.dataRowQueryObjectIds(oid2, oid3)
                .select(env.context());
        assertEquals(2, artists.size());
        assertInstanceOf(DataRow.class, artists.get(0));
    }

    @Test
    public void dataRowMapPkMulti() throws Exception {
        createTwoArtists();

        Map<String, ?> id2 = Collections.singletonMap(Artist.ARTIST_ID_PK_COLUMN, 2);
        Map<String, ?> id3 = Collections.singletonMap(Artist.ARTIST_ID_PK_COLUMN, 3);

        List<DataRow> artists = SelectById.dataRowQueryMaps(Artist.class, id2, id3)
                .select(env.context());
        assertEquals(2, artists.size());
        assertInstanceOf(DataRow.class, artists.get(0));
    }

    @Test
    public void dataRowIntPkCollection() throws Exception {
        createTwoArtists();

        List<DataRow> artists = SelectById.dataRowQueryIdsCollection(Artist.class, Arrays.asList(2, 3))
                .select(env.context());
        assertEquals(2, artists.size());
        assertInstanceOf(DataRow.class, artists.get(0));
    }

    @Test
    public void dataRowObjectIdPkCollection() throws Exception {
        createTwoArtists();

        ObjectId oid2 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 2);
        ObjectId oid3 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 3);

        List<DataRow> artists = SelectById.dataRowQueryObjectIdsCollection(Arrays.asList(oid2, oid3))
                .select(env.context());
        assertEquals(2, artists.size());
        assertInstanceOf(DataRow.class, artists.get(0));
    }

    @Test
    public void dataRowMapPkCollection() throws Exception {
        createTwoArtists();

        Map<String, ?> id2 = Collections.singletonMap(Artist.ARTIST_ID_PK_COLUMN, 2);
        Map<String, ?> id3 = Collections.singletonMap(Artist.ARTIST_ID_PK_COLUMN, 3);

        List<DataRow> artists = SelectById.dataRowQueryMapsCollection(Artist.class, Arrays.asList(id2, id3))
                .select(env.context());
        assertEquals(2, artists.size());
        assertInstanceOf(DataRow.class, artists.get(0));
    }

    @Test
    public void intPk_SelectFirst() throws Exception {
        createTwoArtists();

        Artist a3 = SelectById.queryId(Artist.class, 3).selectFirst(env.context());
        assertNotNull(a3);
        assertEquals("artist3", a3.getArtistName());

        Artist a2 = SelectById.queryId(Artist.class, 2).selectFirst(env.context());
        assertNotNull(a2);
        assertEquals("artist2", a2.getArtistName());
    }

    @Test
    public void metadataCacheKey() {
        SelectById<Painting> q1 = SelectById.queryId(Painting.class, 4).localCache();
        QueryMetadata md1 = q1.getMetaData(resolver);
        assertNotNull(md1);
        assertNotNull(md1.getCacheKey());

        SelectById<Painting> q2 = SelectById.queryMap(Painting.class, singletonMap(Painting.PAINTING_ID_PK_COLUMN, 4))
                .localCache();
        QueryMetadata md2 = q2.getMetaData(resolver);
        assertNotNull(md2);
        assertNotNull(md2.getCacheKey());

        // this query is just a different form of q1, so should hit the same
        // cache entry
        assertEquals(md1.getCacheKey(), md2.getCacheKey());

        SelectById<Painting> q3 = SelectById.queryId(Painting.class, 5).localCache();
        QueryMetadata md3 = q3.getMetaData(resolver);
        assertNotNull(md3);
        assertNotNull(md3.getCacheKey());
        assertNotEquals(md1.getCacheKey(), md3.getCacheKey());

        SelectById<Artist> q4 = SelectById.queryId(Artist.class, 4).localCache();
        QueryMetadata md4 = q4.getMetaData(resolver);
        assertNotNull(md4);
        assertNotNull(md4.getCacheKey());
        assertNotEquals(md1.getCacheKey(), md4.getCacheKey());

        SelectById<Painting> q5 = SelectById
                .queryObjectId(Painting.class, ObjectId.of("Painting", Painting.PAINTING_ID_PK_COLUMN, 4))
                .localCache();
        QueryMetadata md5 = q5.getMetaData(resolver);
        assertNotNull(md5);
        assertNotNull(md5.getCacheKey());

        // this query is just a different form of q1, so should hit the same cache entry
        assertEquals(md1.getCacheKey(), md5.getCacheKey());
    }

    @Test
    public void localCache() throws Exception {
        createTwoArtists();

        final Artist[] a3 = new Artist[1];

        assertEquals(1, env.runWithQueryCounter(() -> {
            a3[0] = SelectById.queryId(Artist.class, 3).localCache("g1").selectOne(env.context());
            assertNotNull(a3[0]);
            assertEquals("artist3", a3[0].getArtistName());
        }));

        env.runWithQueriesBlocked(() -> {
            Artist a3cached = SelectById.queryId(Artist.class, 3).localCache("g1").selectOne(env.context());
            assertSame(a3[0], a3cached);
        });

        env.context().performGenericQuery(new RefreshQuery("g1"));

        assertEquals(1, env.runWithQueryCounter(() ->
                SelectById.queryId(Artist.class, 3).localCache("g1").selectOne(env.context())));
    }

    @Test
    public void prefetch() throws Exception {
        createTwoArtists();
        tPainting.insert(45, 3, "One");
        tPainting.insert(48, 3, "Two");

        Artist a3 = SelectById.queryId(Artist.class, 3)
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

    /* deprecated methods tests */

    @SuppressWarnings("removal")
    @Test
    public void intPkDeprecated() throws Exception {
        createTwoArtists();

        Artist a3 = SelectById.query(Artist.class, 3).selectOne(env.context());
        assertNotNull(a3);
        assertEquals("artist3", a3.getArtistName());

        Artist a2 = SelectById.query(Artist.class, 2).selectOne(env.context());
        assertNotNull(a2);
        assertEquals("artist2", a2.getArtistName());
    }

    @SuppressWarnings("removal")
    @Test
    public void intPkMultiDeprecated() throws Exception {
        createTwoArtists();

        List<Artist> artists = SelectById.query(Artist.class, 2, 3)
                .select(env.context());
        assertEquals(2, artists.size());
        assertInstanceOf(Artist.class, artists.get(0));
    }

    @SuppressWarnings("removal")
    @Test
    public void intPkCollectionDeprecation() throws Exception {
        createTwoArtists();

        List<Artist> artists = SelectById.query(Artist.class, Arrays.asList(1, 2, 3, 4, 5))
                .select(env.context());
        assertEquals(2, artists.size());
        assertInstanceOf(Artist.class, artists.get(0));
    }

    @SuppressWarnings("removal")
    @Test
    public void mapPkDeprecation() throws Exception {
        createTwoArtists();

        Artist a3 = SelectById.query(Artist.class, singletonMap(Artist.ARTIST_ID_PK_COLUMN, 3)).selectOne(env.context());
        assertNotNull(a3);
        assertEquals("artist3", a3.getArtistName());

        Artist a2 = SelectById.query(Artist.class, singletonMap(Artist.ARTIST_ID_PK_COLUMN, 2)).selectOne(env.context());
        assertNotNull(a2);
        assertEquals("artist2", a2.getArtistName());
    }

    @SuppressWarnings("removal")
    @Test
    public void mapPkMultiDeprecation() throws Exception {
        createTwoArtists();

        Map<String, ?> id2 = Collections.singletonMap(Artist.ARTIST_ID_PK_COLUMN, 2);
        Map<String, ?> id3 = Collections.singletonMap(Artist.ARTIST_ID_PK_COLUMN, 3);

        List<Artist> artists = SelectById.query(Artist.class, id2, id3)
                .select(env.context());
        assertEquals(2, artists.size());
        assertInstanceOf(Artist.class, artists.get(0));
    }

    @SuppressWarnings("removal")
    @Test
    public void objectIdPkDeprecation() throws Exception {
        createTwoArtists();

        ObjectId oid3 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 3);
        Artist a3 = SelectById.query(Artist.class, oid3).selectOne(env.context());
        assertNotNull(a3);
        assertEquals("artist3", a3.getArtistName());

        ObjectId oid2 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 2);
        Artist a2 = SelectById.query(Artist.class, oid2).selectOne(env.context());
        assertNotNull(a2);
        assertEquals("artist2", a2.getArtistName());
    }

    @SuppressWarnings("removal")
    @Test
    public void objectIdPkMultiDeprecation() throws Exception {
        createTwoArtists();

        ObjectId oid2 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 2);
        ObjectId oid3 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 3);

        List<Artist> artists = SelectById.query(Artist.class, oid2, oid3)
                .select(env.context());
        assertEquals(2, artists.size());
        assertInstanceOf(Artist.class, artists.get(0));
    }

    @SuppressWarnings("removal")
    @Test
    public void dataRowIntPkDeprecation() throws Exception {
        createTwoArtists();

        DataRow a3 = SelectById.dataRowQuery(Artist.class, 3).selectOne(env.context());
        assertNotNull(a3);
        assertEquals("artist3", a3.get("ARTIST_NAME"));

        DataRow a2 = SelectById.dataRowQuery(Artist.class, 2).selectOne(env.context());
        assertNotNull(a2);
        assertEquals("artist2", a2.get("ARTIST_NAME"));
    }

    @SuppressWarnings("removal")
    @Test
    public void dataRowMapPkDeprecation() throws Exception {
        createTwoArtists();

        Map<String, ?> id3 = Collections.singletonMap(Artist.ARTIST_ID_PK_COLUMN, 3);
        DataRow a3 = SelectById.dataRowQuery(Artist.class, id3).selectOne(env.context());
        assertNotNull(a3);
        assertEquals("artist3", a3.get("ARTIST_NAME"));

        Map<String, ?> id2 = Collections.singletonMap(Artist.ARTIST_ID_PK_COLUMN, 2);
        DataRow a2 = SelectById.dataRowQuery(Artist.class, id2).selectOne(env.context());
        assertNotNull(a2);
        assertEquals("artist2", a2.get("ARTIST_NAME"));
    }

    @SuppressWarnings("removal")
    @Test
    public void dataRowObjectIdPkDeprecation() throws Exception {
        createTwoArtists();

        ObjectId oid3 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 3);
        DataRow a3 = SelectById.dataRowQuery(oid3).selectOne(env.context());
        assertNotNull(a3);
        assertEquals("artist3", a3.get("ARTIST_NAME"));

        ObjectId oid2 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 2);
        DataRow a2 = SelectById.dataRowQuery(oid2).selectOne(env.context());
        assertNotNull(a2);
        assertEquals("artist2", a2.get("ARTIST_NAME"));
    }

    @SuppressWarnings("removal")
    @Test
    public void dataRowIntPkMultiDeprecation() throws Exception {
        createTwoArtists();

        List<DataRow> artists = SelectById.dataRowQuery(Artist.class, 2, 3)
                .select(env.context());
        assertEquals(2, artists.size());
        assertInstanceOf(DataRow.class, artists.get(0));
    }

    @SuppressWarnings("removal")
    @Test
    public void dataRowMapPkMultiDeprecation() throws Exception {
        createTwoArtists();

        ObjectId oid2 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 2);
        ObjectId oid3 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 3);

        List<DataRow> artists = SelectById.dataRowQuery(oid2, oid3)
                .select(env.context());
        assertEquals(2, artists.size());
        assertInstanceOf(DataRow.class, artists.get(0));
    }

    @SuppressWarnings("removal")
    @Test
    public void dataRowObjectIdPkMultiDeprecation() throws Exception {
        createTwoArtists();

        Map<String, ?> id2 = Collections.singletonMap(Artist.ARTIST_ID_PK_COLUMN, 2);
        Map<String, ?> id3 = Collections.singletonMap(Artist.ARTIST_ID_PK_COLUMN, 3);

        List<DataRow> artists = SelectById.dataRowQuery(Artist.class, id2, id3)
                .select(env.context());
        assertEquals(2, artists.size());
        assertInstanceOf(DataRow.class, artists.get(0));
    }
}
