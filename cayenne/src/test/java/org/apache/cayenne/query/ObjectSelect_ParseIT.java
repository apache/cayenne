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

import java.math.BigDecimal;
import java.util.List;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Runs the queries created with {@link ObjectSelect#parse(String, Object...)}.
 */
public class ObjectSelect_ParseIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private DataContext context;

    /**
     * Artists 1 to 4 have two paintings each, artist 5 has none:
     * <pre>
     * artist1: painting4 (40), painting8 (80)
     * artist2: painting1 (10), painting5 (50)
     * artist3: painting2 (20), painting6 (60)
     * artist4: painting3 (30), painting7 (70)
     * </pre>
     * Odd paintings are in "gallery1", even paintings have no gallery.
     */
    @BeforeEach
    public void createDataSet() throws Exception {
        context = env.context();

        TableHelper tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");
        for (int i = 1; i <= 5; i++) {
            tArtist.insert(i, "artist" + i);
        }

        env.table("GALLERY", "GALLERY_ID", "GALLERY_NAME").insert(1, "gallery1");

        TableHelper tPainting = env.table("PAINTING", "PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "GALLERY_ID",
                "ESTIMATED_PRICE");
        for (int i = 1; i <= 8; i++) {
            tPainting.insert(i, "painting" + i, i % 4 + 1, i % 2 == 1 ? 1 : null, 10 * i);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> select(String query, Object... params) {
        return (List<T>) ObjectSelect.parse(query, params).select(context);
    }

    private static List<String> names(List<Artist> artists) {
        return artists.stream().map(Artist::getArtistName).toList();
    }

    @Test
    public void objects() {
        List<Artist> artists = select("from Artist order by artistName desc");
        assertEquals(List.of("artist5", "artist4", "artist3", "artist2", "artist1"), names(artists));
    }

    @Test
    public void whereOrderLimitOffset() {
        List<Artist> artists = select("from Artist where artistName like 'artist%' and artistName != $name "
                + "order by artistName limit 2 offset 1", "artist2");
        assertEquals(List.of("artist3", "artist4"), names(artists));
    }

    @Test
    public void typedParse() {
        List<Artist> artists = ObjectSelect.parse(Artist.class, "from Artist where artistName = $n", "artist3")
                .select(context);
        assertEquals(List.of("artist3"), names(artists));

        List<DataRow> rows = ObjectSelect.parse(DataRow.class, "from Artist where artistName = 'artist3'")
                .select(context);
        assertEquals(1, rows.size());
        assertEquals("artist3", rows.get(0).get("ARTIST_NAME").toString().trim());

        List<String> names = ObjectSelect.parseColumn(String.class, "select artistName from Artist order by artistName")
                .select(context);
        assertEquals(5, names.size());
        assertEquals("artist1", names.get(0));

        List<Object[]> columns = ObjectSelect.parseColumns("select artistName, count(paintingArray) from Artist "
                + "order by artistName").select(context);
        assertEquals(4, columns.size());
        assertEquals(2L, columns.get(0)[1]);
    }

    @Test
    public void persistentParameter() {
        Artist artist2 = Cayenne.objectForPK(context, Artist.class, 2);

        List<Painting> paintings = select(
                "from Painting where toArtist = $artist and estimatedPrice between $low and $high", artist2, 20, 100);
        assertEquals(1, paintings.size());
        assertEquals("painting5", paintings.get(0).getPaintingTitle());
    }

    @Test
    public void collectionParameter() {
        List<Artist> artists = select("from Artist where db:ARTIST_ID in $ids order by artistName", List.of(1, 3));
        assertEquals(List.of("artist1", "artist3"), names(artists));
    }

    @Test
    public void outerJoinAndNull() {
        List<Artist> artists = select("from Artist where paintingArray+ = null");
        assertEquals(List.of("artist5"), names(artists));
    }

    @Test
    public void splitAliases() {
        List<Artist> artists = select("from Artist where paintingArray#p1.paintingTitle = 'painting4' "
                + "and paintingArray#p2.paintingTitle = 'painting8'");
        assertEquals(List.of("artist1"), names(artists));

        // the same condition over a single join matches nothing
        List<Artist> none = select("from Artist where paintingArray.paintingTitle = 'painting4' "
                + "and paintingArray.paintingTitle = 'painting8'");
        assertEquals(0, none.size());
    }

    @Test
    public void scalarColumns() {
        List<Object[]> rows = select("select paintingTitle, estimatedPrice from Painting order by estimatedPrice desc");

        assertEquals(8, rows.size());
        assertEquals("painting8", rows.get(0)[0]);
        assertEquals(new BigDecimal("80"), assertInstanceOf(BigDecimal.class, rows.get(0)[1]).setScale(0));
    }

    @Test
    public void singleDistinctColumn() {
        List<String> names = select("select distinct toGallery.galleryName from Painting");
        assertEquals(List.of("gallery1"), names);
    }

    @Test
    public void aggregatesWithHaving() {
        List<Object[]> rows = select("select toArtist.artistName, sum(estimatedPrice), max(estimatedPrice) "
                + "from Painting having sum(estimatedPrice) > $min order by toArtist.artistName", 70);

        assertEquals(3, rows.size());
        assertEquals("artist1", rows.get(0)[0]);
        assertEquals(new BigDecimal("120"), ((BigDecimal) rows.get(0)[1]).setScale(0));
        assertEquals(new BigDecimal("80"), ((BigDecimal) rows.get(0)[2]).setScale(0));
        assertEquals("artist3", rows.get(1)[0]);
        assertEquals("artist4", rows.get(2)[0]);
    }

    @Test
    public void countAll() {
        List<Long> count = select("select count(*) from Painting where estimatedPrice > 35");
        assertEquals(List.of(5L), count);
    }

    @Test
    public void selfWithAggregate() {
        List<Object[]> rows = select("select self, count(paintingArray+) as n from Artist "
                + "order by count(paintingArray+), artistName desc");

        assertEquals(5, rows.size());
        assertEquals("artist5", assertInstanceOf(Artist.class, rows.get(0)[0]).getArtistName());
        assertEquals(0L, rows.get(0)[1]);
        assertEquals("artist4", assertInstanceOf(Artist.class, rows.get(1)[0]).getArtistName());
        assertEquals(2L, rows.get(1)[1]);
    }

    @Test
    public void relatedEntityColumns() {
        List<Object[]> rows = select("select toGallery+, toArtist, self from Painting order by paintingTitle");

        assertEquals(8, rows.size());

        // painting1
        assertEquals("gallery1", assertInstanceOf(Gallery.class, rows.get(0)[0]).getGalleryName());
        assertEquals("artist2", assertInstanceOf(Artist.class, rows.get(0)[1]).getArtistName());
        assertEquals("painting1", assertInstanceOf(Painting.class, rows.get(0)[2]).getPaintingTitle());

        // painting2
        assertNull(rows.get(1)[0]);
        assertEquals("artist3", assertInstanceOf(Artist.class, rows.get(1)[1]).getArtistName());
    }

    @Test
    public void flatToManyColumn() {
        List<Object[]> rows = select("select artistName, paintingArray from Artist where artistName = 'artist1' "
                + "order by paintingArray.paintingTitle");

        assertEquals(2, rows.size());
        assertEquals("artist1", rows.get(0)[0]);
        assertEquals("painting4", assertInstanceOf(Painting.class, rows.get(0)[1]).getPaintingTitle());
        assertEquals("painting8", assertInstanceOf(Painting.class, rows.get(1)[1]).getPaintingTitle());
    }

    @Test
    public void flatToManyColumnWithNoRelatedObjects() {
        // an entity column is an outer join, so an artist with no paintings is not filtered out
        List<Object[]> rows = select("select artistName, paintingArray from Artist where artistName = 'artist5'");

        assertEquals(1, rows.size());
        assertEquals("artist5", rows.get(0)[0]);
        assertNull(rows.get(0)[1]);

        // ... unless the qualifier says so
        List<Object[]> none = select("select artistName, paintingArray from Artist "
                + "where artistName = 'artist5' and exists paintingArray");
        assertEquals(0, none.size());
    }

    @Test
    public void functions() {
        List<Object[]> rows = select("select upper(artistName), length(trim(artistName)) from Artist "
                + "where locate('5', artistName) = 7");

        assertEquals(1, rows.size());
        assertEquals("ARTIST5", rows.get(0)[0]);
        assertEquals(7, ((Number) rows.get(0)[1]).intValue());
    }

    @Test
    public void prefetch() {
        List<Artist> artists = select("from Artist where artistName = $name "
                + "prefetch paintingArray joint, paintingArray.toGallery", "artist2");

        assertEquals(1, artists.size());

        env.runWithQueriesBlocked(() -> {
            List<Painting> paintings = artists.get(0).getPaintingArray();
            assertEquals(2, paintings.size());
            for (Painting painting : paintings) {
                assertEquals(PersistenceState.COMMITTED, painting.getPersistenceState());
                assertEquals("gallery1", painting.getToGallery().getGalleryName());
            }
        });
    }

    @Test
    public void existsSubquery() {
        List<Artist> artists = select("from Artist where exists "
                + "(select self from Painting where toArtist = enclosing(self) and estimatedPrice > $price) "
                + "order by artistName", 65);
        assertEquals(List.of("artist1", "artist4"), names(artists));
    }

    @Test
    public void notExistsSubquery() {
        List<Artist> artists = select(
                "from Artist where not exists (from Painting where toArtist = enclosing(self))");
        assertEquals(List.of("artist5"), names(artists));
    }

    @Test
    public void existsSubqueryWithHaving() {
        List<Artist> artists = select("from Artist where exists (select self from Painting "
                + "where toArtist = enclosing(self) and estimatedPrice < 45 having count(*) > 1)");
        assertEquals(0, artists.size());
    }

    @Test
    public void existsPath() {
        List<Artist> artists = select("from Artist where not exists paintingArray");
        assertEquals(List.of("artist5"), names(artists));
    }

    @Test
    public void inSubquery() {
        List<Artist> artists = select("from Artist where artistName in "
                + "(select toArtist.artistName from Painting where estimatedPrice < $price) order by artistName", 25);
        assertEquals(List.of("artist2", "artist3"), names(artists));

        List<Artist> notIn = select("from Artist where artistName not in "
                + "(select toArtist.artistName from Painting where estimatedPrice < 75) order by artistName");
        assertEquals(List.of("artist5"), names(notIn));
    }

    @Test
    public void allAndAnySubqueries() {
        List<Painting> all = select("from Painting where estimatedPrice > all "
                + "(select estimatedPrice from Painting where toArtist.artistName = 'artist4')");
        assertEquals(1, all.size());
        assertEquals("painting8", all.get(0).getPaintingTitle());

        List<Painting> any = select("from Painting where estimatedPrice = any "
                + "(select max(estimatedPrice) from Painting)");
        assertEquals(1, any.size());
        assertEquals("painting8", any.get(0).getPaintingTitle());
    }

    @Test
    public void subqueryInExpression() {
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where("exists (from Painting where toArtist = enclosing(self) and paintingTitle = $t)", "painting3")
                .select(context);
        assertEquals(List.of("artist4"), names(artists));
    }

    @Test
    public void cachedSubqueryParameters() {
        // a parameter of a nested select is a part of the cache key, so the two queries don't share the cached result
        String query = "from Artist where exists "
                + "(from Painting where toArtist = enclosing(self) and estimatedPrice > $price) order by artistName";

        List<Artist> over65 = ObjectSelect.parse(Artist.class, query, 65).localCache().select(context);
        assertEquals(List.of("artist1", "artist4"), names(over65));

        List<Artist> over75 = ObjectSelect.parse(Artist.class, query, 75).localCache().select(context);
        assertEquals(List.of("artist1"), names(over75));
    }

    @Test
    public void dbRoot() {
        List<DataRow> rows = select("from db:ARTIST where ARTIST_NAME like 'artist%' order by ARTIST_ID desc limit 2");

        assertEquals(2, rows.size());
        assertEquals(5, ((Number) rows.get(0).get("ARTIST_ID")).intValue());
        assertEquals(4, ((Number) rows.get(1).get("ARTIST_ID")).intValue());
    }
}
