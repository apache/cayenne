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

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ColumnSelect_EntityColumnJoinsIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private DataContext context;

    /**
     * Artists 1 to 4 have two paintings each, artist 5 has none. Paintings 1 to 8 have artists, painting 9 has none.
     * Odd paintings are in a gallery, even ones are not.
     */
    @BeforeEach
    public void createDataSet() throws Exception {
        context = env.context();

        TableHelper tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");
        for (int i = 1; i <= 5; i++) {
            tArtist.insert(i, "artist" + i);
        }

        env.table("GALLERY", "GALLERY_ID", "GALLERY_NAME").insert(1, "gallery1");

        TableHelper tPainting = env.table("PAINTING", "PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "GALLERY_ID");
        for (int i = 1; i <= 8; i++) {
            tPainting.insert(i, "painting" + i, i % 4 + 1, i % 2 == 1 ? 1 : null);
        }
        tPainting.insert(9, "painting9", null, 1);
    }

    private static <T> BaseProperty<T> untyped(String path) {
        return PropertyFactory.createBase(path, null);
    }

    // checks that there is a single [name, null] row for a root with no related objects
    private static void assertRootWithNoRelated(List<Object[]> rows, String rootName) {
        List<Object[]> matched = rows.stream().filter(r -> rootName.equals(r[0])).toList();
        assertEquals(1, matched.size(), () -> "Expected a single row for " + rootName);
        assertNull(matched.get(0)[1]);
    }

    @Test
    public void toMany() {
        List<Object[]> rows = ObjectSelect
                .columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.flat())
                .select(context);

        assertEquals(9, rows.size());
        assertRootWithNoRelated(rows, "artist5");
    }

    @Test
    public void toMany_Untyped() {
        List<Object[]> rows = ObjectSelect
                .columnQuery(Artist.class, untyped("artistName"), untyped("paintingArray"))
                .select(context);

        assertEquals(9, rows.size());
        assertRootWithNoRelated(rows, "artist5");
    }

    @Test
    public void toMany_SingleColumn() {
        List<Painting> paintings = ObjectSelect
                .columnQuery(Artist.class, Artist.PAINTING_ARRAY.flat())
                .select(context);

        assertEquals(9, paintings.size());
        assertEquals(8, paintings.stream().filter(Objects::nonNull).count());
    }

    @Test
    public void toMany_Paginated() {
        List<Object[]> rows = ObjectSelect
                .columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.flat())
                .pageSize(3)
                .select(context);

        assertEquals(9, rows.size());
        assertRootWithNoRelated(rows, "artist5");
    }

    @Test
    public void toMany_QualifierOnRoot() {
        List<Object[]> rows = ObjectSelect
                .columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.flat())
                .where(Artist.ARTIST_NAME.in("artist1", "artist5"))
                .select(context);

        assertEquals(3, rows.size());
        assertRootWithNoRelated(rows, "artist5");
    }

    @Test
    public void toMany_OrderingOnTheSamePath() {
        List<Object[]> rows = ObjectSelect
                .columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.flat())
                .orderBy(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).asc())
                .select(context);

        assertEquals(9, rows.size());
        assertRootWithNoRelated(rows, "artist5");
    }

    @Test
    public void toMany_QualifierOnTheSamePath() {
        // the join is shared with the qualifier, that is the one to filter the rows
        List<Object[]> rows = ObjectSelect
                .columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.flat())
                .where(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).like("painting%"))
                .select(context);

        assertEquals(8, rows.size());
    }

    @Test
    public void toMany_QualifierOnTheSamePath_InnerJoinWins() {
        // Unlike "like" above, "is null" gives different results for inner and outer joins. No painting has a null
        // title, so the inner join of the qualifier matches nothing, while an outer join would've matched "artist5".
        List<Object[]> rows = ObjectSelect
                .columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.flat())
                .where(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).isNull())
                .select(context);

        assertEquals(0, rows.size());
    }

    @Test
    public void toMany_OuterQualifierOnTheSamePath() {
        // an explicitly outer path in the qualifier is a join of its own, and it does match "artist5"
        List<Object[]> rows = ObjectSelect
                .columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.flat())
                .where(Artist.PAINTING_ARRAY.outer().dot(Painting.PAINTING_TITLE).isNull())
                .select(context);

        assertEquals(1, rows.size());
        assertRootWithNoRelated(rows, "artist5");
    }

    @Test
    public void toMany_ExistsQualifier() {
        List<Object[]> rows = ObjectSelect
                .columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.flat())
                .where(Artist.PAINTING_ARRAY.exists())
                .select(context);

        assertEquals(8, rows.size());
    }

    @Test
    public void toManyThenToOne() {
        // DISTINCT is in effect, so there's a row per artist: artists 2 and 4 have paintings in a gallery
        List<Object[]> rows = ObjectSelect
                .columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY))
                .select(context);

        assertEquals(5, rows.size());
        assertRootWithNoRelated(rows, "artist5");
        assertRootWithNoRelated(rows, "artist1");
        assertEquals(2, rows.stream().filter(r -> r[1] instanceof Gallery).count());
    }

    @Test
    public void toOneThenToMany() {
        // 5 paintings in a gallery, each paired with the 5 paintings of that gallery, plus 4 paintings with no gallery
        List<Object[]> rows = ObjectSelect
                .columnQuery(Painting.class, Painting.PAINTING_TITLE, untyped("toGallery.paintingArray"))
                .select(context);

        assertEquals(29, rows.size());
        assertRootWithNoRelated(rows, "painting2");
    }

    @Test
    public void toOne() {
        List<Object[]> rows = ObjectSelect
                .columnQuery(Painting.class, Painting.PAINTING_TITLE, Painting.TO_ARTIST)
                .select(context);

        assertEquals(9, rows.size());
        assertRootWithNoRelated(rows, "painting9");
    }

    @Test
    public void toOne_QualifierOnTheSamePath() {
        List<Object[]> rows = ObjectSelect
                .columnQuery(Painting.class, Painting.PAINTING_TITLE, Painting.TO_ARTIST)
                .where(Painting.TO_ARTIST.dot(Artist.ARTIST_NAME).like("artist%"))
                .select(context);

        assertEquals(8, rows.size());
    }

    @Test
    public void toOne_QualifierOnTheSamePath_InnerJoinWins() {
        // no artist has a null name, so the inner join of the qualifier matches nothing, while an outer join
        // would've matched "painting9"
        List<Object[]> rows = ObjectSelect
                .columnQuery(Painting.class, Painting.PAINTING_TITLE, Painting.TO_ARTIST)
                .where(Painting.TO_ARTIST.dot(Artist.ARTIST_NAME).isNull())
                .select(context);

        assertEquals(0, rows.size());
    }

    @Test
    public void scalarColumnsAndAggregatesUseInnerJoins() {
        List<Object[]> scalars = ObjectSelect
                .columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE))
                .select(context);
        assertEquals(8, scalars.size());

        List<Object[]> counts = ObjectSelect
                .columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.count())
                .select(context);
        assertEquals(4, counts.size());
    }
}
