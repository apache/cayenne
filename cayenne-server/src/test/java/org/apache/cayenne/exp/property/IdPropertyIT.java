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

package org.apache.cayenne.exp.property;

import java.util.List;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @since 4.2
 */
@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class IdPropertyIT extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Before
    public void createArtistsDataSet() throws Exception {
        TableHelper tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");

        long dateBase = System.currentTimeMillis();
        int artistCount = 4;
        for (int i = 1; i <= artistCount; i++) {
            tArtist.insert(i, "artist" + i, new java.sql.Date(dateBase + 10000 * i));
        }

        TableHelper tGallery = new TableHelper(dbHelper, "GALLERY");
        tGallery.setColumns("GALLERY_ID", "GALLERY_NAME");
        tGallery.insert(1, "tate modern");

        TableHelper tPaintings = new TableHelper(dbHelper, "PAINTING");
        tPaintings.setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "GALLERY_ID");
        for (int i = 1; i <= 16; i++) {
            tPaintings.insert(i, "painting" + i, i % artistCount + 1, 1);
        }
    }

    @Test
    public void filterDb() {
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_ID_PK_PROPERTY.gt(2L))
                .select(context);
        assertEquals(2, artists.size());
    }

    @Test
    public void filterDbRelated() {
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_ID_PK_PROPERTY).gt(13))
                .select(context);
        assertEquals(3, artists.size());
    }

    @Test
    public void filterInMemory() {
        List<Artist> artists = Artist.ARTIST_ID_PK_PROPERTY.gt(2L)
                .filterObjects(ObjectSelect.query(Artist.class).select(context));
        assertEquals(2, artists.size());
    }

    @Test
    public void filterInMemoryRelated() {
        List<Artist> artists = Artist.PAINTING_ARRAY.dot(Painting.PAINTING_ID_PK_PROPERTY).gt(13)
                .filterObjects(ObjectSelect.query(Artist.class).select(context));
        assertEquals(3, artists.size());
    }

    @Test
    public void orderingDb() {
        List<Painting> paintings = ObjectSelect.query(Painting.class)
                .orderBy(Painting.PAINTING_ID_PK_PROPERTY.desc())
                .select(context);

        assertEquals(16, paintings.size());
        assertEquals(16L, Cayenne.longPKForObject(paintings.get(0)));
        assertEquals(1L, Cayenne.longPKForObject(paintings.get(15)));
    }

    @Test
    public void orderingDbRelated() {
        List<Painting> paintings = ObjectSelect.query(Painting.class)
                .orderBy(Painting.TO_ARTIST.dot(Artist.ARTIST_ID_PK_PROPERTY).asc())
                .select(context);

        assertEquals(16, paintings.size());
        assertEquals(1L, Cayenne.longPKForObject(paintings.get(0).getToArtist()));
        assertEquals(4L, Cayenne.longPKForObject(paintings.get(15).getToArtist()));
    }

    @Test
    public void orderingInMemory() {
        List<Painting> paintings = ObjectSelect.query(Painting.class)
                .select(context);
        Painting.PAINTING_ID_PK_PROPERTY.desc().orderList(paintings);

        assertEquals(16, paintings.size());
        assertEquals(16L, Cayenne.longPKForObject(paintings.get(0)));
        assertEquals(1L, Cayenne.longPKForObject(paintings.get(15)));
    }

    @Test
    public void orderingInMemoryRelated() {
        List<Painting> paintings = ObjectSelect.query(Painting.class)
                .select(context);
        Painting.TO_ARTIST.dot(Artist.ARTIST_ID_PK_PROPERTY).asc().orderList(paintings);

        assertEquals(16, paintings.size());
        assertEquals(1L, Cayenne.longPKForObject(paintings.get(0).getToArtist()));
        assertEquals(4L, Cayenne.longPKForObject(paintings.get(15).getToArtist()));
    }

    @Test
    public void columnQuery() {
        List<Long> ids = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_ID_PK_PROPERTY)
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.desc())
                .select(context);
        assertEquals(4, ids.size());
        assertEquals(Long.valueOf(1L), ids.get(3));
        assertEquals(Long.valueOf(2L), ids.get(2));
        assertEquals(Long.valueOf(3L), ids.get(1));
        assertEquals(Long.valueOf(4L), ids.get(0));
    }

    @Test
    public void columnQueryWithGroupBy() {
        List<Object[]> ids = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_ID_PK_PROPERTY, Artist.PAINTING_ARRAY.count())
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc())
                .select(context);
        assertEquals(4, ids.size());
        assertEquals(1L, ids.get(0)[0]);
        assertEquals(2L, ids.get(1)[0]);
        assertEquals(3L, ids.get(2)[0]);
        assertEquals(4L, ids.get(3)[0]);
    }

    @Test
    public void filterDirectExpression() {
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.matchAllExp("@id", 2))
                .select(context);
        assertEquals(1, artists.size());
    }

    @Test
    public void filterDirectAttributeExpression() {
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.matchAllExp("@id:ARTIST_ID", 2))
                .select(context);
        assertEquals(1, artists.size());
    }

    @Test
    public void filterByObjectId() {
        Artist artist = ObjectSelect.query(Artist.class).selectFirst(context);
        assertNotNull(artist);

        List<Painting> paintings = ObjectSelect.query(Painting.class)
                .where(Painting.TO_ARTIST.dot(Artist.ARTIST_ID_PK_PROPERTY).eq(artist.getObjectId()))
                .select(context);
        assertEquals(4, paintings.size());
    }
}
