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

package org.apache.cayenne.exp.property;

import java.util.Collections;
import java.util.List;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.ASTEqual;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.exp.parser.ASTPath;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @since 4.2
 */
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class PathAliasesIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Before
    public void createArtistsDataSet() throws Exception {
        TableHelper tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");

        long dateBase = System.currentTimeMillis();
        for (int i = 1; i <= 20; i++) {
            tArtist.insert(i, "artist" + i, new java.sql.Date(dateBase + 10000 * i));
        }

        TableHelper tGallery = new TableHelper(dbHelper, "GALLERY");
        tGallery.setColumns("GALLERY_ID", "GALLERY_NAME");
        tGallery.insert(1, "tate modern");

        TableHelper tGallery1 = new TableHelper(dbHelper, "GALLERY");
        tGallery1.setColumns("GALLERY_ID", "GALLERY_NAME");
        tGallery1.insert(2, "test gallery");

        TableHelper tPaintings = new TableHelper(dbHelper, "PAINTING");
        tPaintings.setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "GALLERY_ID");
        for (int i = 1; i <= 20; i++) {
            tPaintings.insert(i, "painting" + i,
                    i % 2 == 0 ? 4 : i % 5 + 1,
                    i % 2 == 0 ? 2 : 1);
        }
    }

    @Test
    public void testBeginAlias() {
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(Artist.PAINTING_ARRAY.alias("p1").dot(Painting.PAINTING_TITLE).eq("painting2"))
                .and(Artist.PAINTING_ARRAY.alias("p2").dot(Painting.PAINTING_TITLE).eq("painting4"))
                .select(context);
        assertEquals(1, artists.size());
        assertEquals("artist4", artists.get(0).getArtistName());
    }

    @Test
    public void testTheSameAliases() {
        List<Object[]> results = ObjectSelect.columnQuery(Artist.class,
                Artist.ARTIST_NAME,
                Artist.PAINTING_ARRAY.alias("p1").dot(Painting.PAINTING_TITLE),
                Artist.PAINTING_ARRAY.alias("p2").dot(Painting.PAINTING_TITLE))
                .where(Artist.PAINTING_ARRAY.alias("p1").dot(Painting.PAINTING_TITLE).eq("painting2"))
                .and(Artist.PAINTING_ARRAY.alias("p2").dot(Painting.PAINTING_TITLE).eq("painting4"))
                .select(context);
        assertEquals(1, results.size());
        assertEquals("artist4", results.get(0)[0]);
        assertEquals("painting2", results.get(0)[1]);
        assertEquals("painting4", results.get(0)[2]);
    }

    @Test
    public void testMiddleAlias() {
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).dot(Gallery.PAINTING_ARRAY).alias("p1").dot(Painting.PAINTING_TITLE).eq("painting2"))
                .and(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).dot(Gallery.PAINTING_ARRAY).alias("p2").dot(Painting.PAINTING_TITLE).eq("painting4"))
                .select(context);
        assertEquals(1, artists.size());
        assertEquals("artist4", artists.get(0).getArtistName());
    }

    @Test
    public void testEntityPropertyAliases() {
        Artist artist = Cayenne.objectForPK(context, Artist.class, 1);

        List<Painting> paintings = ObjectSelect.query(Painting.class)
                .where(Painting.TO_ARTIST.alias("p1").eq(artist))
                .select(context);
        assertEquals(2, paintings.size());
        assertEquals("artist1", paintings.get(0).getToArtist().getArtistName());
        assertEquals("artist1", paintings.get(1).getToArtist().getArtistName());
    }

    @Test
    public void testAliases() {
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(Artist.PAINTING_ARRAY.alias("p1").dot(Painting.PAINTING_TITLE).eq("painting2"))
                .and(Artist.PAINTING_ARRAY.alias("p2").dot(Painting.PAINTING_TITLE).eq("painting4"))
                .select(context);
        assertEquals(1, artists.size());
        assertNotNull(artists.get(0));
        assertEquals("artist4", artists.get(0).getArtistName());
    }

    @Test
    public void testAliasForPath() {
        ASTPath astPath = new ASTObjPath("paintingArray.a.galleryName");
        astPath.setPathAliases(Collections.singletonMap("a", "toGallery"));
        ASTEqual astEqual = new ASTEqual(astPath, "tate modern");
        List<Object[]> artists = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME, PropertyFactory.createBase(astPath, String.class))
                .where(astEqual)
                .orderBy(Artist.ARTIST_NAME.asc())
                .select(context);
        assertEquals(5, artists.size());
        assertEquals("artist1", artists.get(0)[0]);
    }

    @Test
    public void testAggregationWithAliases() {
        List<Object[]> artistAndPaintingCount = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.count())
                .having(Artist.PAINTING_ARRAY.alias("p1").count().lt(5L))
                .select(context);
        assertEquals(4, artistAndPaintingCount.size());
        assertTrue((Long)artistAndPaintingCount.get(0)[1] < 5);
    }

    @Test
    public void testOrderWithAlias() {
        ObjectSelect<Painting> query = ObjectSelect.query(Painting.class)
                .orderBy(Painting.TO_ARTIST.alias("p1").dot(Artist.ARTIST_NAME).asc())
                .prefetch(Painting.TO_ARTIST.disjoint());

        List<Painting> paintings = query.select(context);
        assertEquals(20, paintings.size());
        assertEquals("artist1", paintings.get(0).getToArtist().getArtistName());
    }

    @Test(expected = CayenneRuntimeException.class)
    public void testPrefetchWithAliases() {
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class);
        query.prefetch(Artist.PAINTING_ARRAY.alias("p1").disjoint());
        query.select(context);
    }

    @Test(expected = CayenneRuntimeException.class)
    public void testTheSameAliasesToDifferentProperties() {
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class);
        query.where(Artist.PAINTING_ARRAY.alias("p1").dot(Painting.PAINTING_TITLE).eq("p1"));
        query.and(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).alias("p1").dot(Gallery.GALLERY_NAME).eq("g1"));
        query.select(context);
    }

    @Test
    public void testExpWithAliases() {
        Expression e1 = ExpressionFactory.exp("paintingArray#p1.paintingTitle = 'painting2'");
        Expression e2 = ExpressionFactory.exp("paintingArray#p2.paintingTitle = 'painting4'");
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(e1)
                .and(e2)
                .select(context);
        assertEquals(1, artists.size());
        assertEquals("artist4", artists.get(0).getArtistName());
    }

    @Test
    public void testExpWithSeveralAliases() {
        Expression e1 = ExpressionFactory.exp("paintingArray#p1.toGallery#g1.galleryName = 'tate modern'");
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(e1)
                .orderBy(Artist.ARTIST_NAME.asc())
                .select(context);
        assertEquals(5, artists.size());
        assertEquals("artist1", artists.get(0).getArtistName());
    }

    @Test
    public void testMiddleAliasForExp() {
        Expression e1 = ExpressionFactory.exp("paintingArray.toGallery.paintingArray#p1.paintingTitle = 'painting2'");
        Expression e2 = ExpressionFactory.exp("paintingArray.toGallery.paintingArray#p2.paintingTitle = 'painting4'");
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(e1)
                .and(e2)
                .select(context);
        assertEquals(1, artists.size());
        assertEquals("artist4", artists.get(0).getArtistName());
    }

    @Test
    public void testEntityPropertyAliasesInExp() {
        Painting painting2 = Cayenne.objectForPK(context, Painting.class, 2);
        Painting painting4 = Cayenne.objectForPK(context, Painting.class, 4);

        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.exp("paintingArray#p1 = $painting1", painting2))
                .and(ExpressionFactory.exp("paintingArray#p2 = $painting2", painting4))
                .select(context);
        assertEquals(1, artists.size());
        assertEquals("artist4", artists.get(0).getArtistName());
    }

    @Test
    public void testAliasForPathExp() {
        ASTPath astPath = new ASTObjPath("paintingArray.p1.galleryName");
        astPath.setPathAliases(Collections.singletonMap("a", "toGallery"));
        ASTEqual astEqual = new ASTEqual(astPath, "test gallery");
        Expression e1 = ExpressionFactory.exp("paintingArray.toGallery#p1.galleryName");
        List<Object[]> artists = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME, PropertyFactory.createBase(e1, String.class))
                .where(astEqual)
                .select(context);
        assertEquals(1, artists.size());
        assertEquals("artist4", artists.get(0)[0]);
        assertEquals("test gallery", artists.get(0)[1]);
    }

    @Test
    public void testTheSameAliasesForExp() {
        Expression e1 = ExpressionFactory.exp("paintingArray#p1.paintingTitle");
        Expression e2 = ExpressionFactory.exp("paintingArray#p2.paintingTitle");
        Expression e3 = ExpressionFactory.exp("paintingArray#p1.paintingTitle = 'painting2'");
        Expression e4 = ExpressionFactory.exp("paintingArray#p2.paintingTitle = 'painting4'");
        List<Object[]> results = ObjectSelect.columnQuery(Artist.class,
                Artist.ARTIST_NAME,
                PropertyFactory.createBase(e1, String.class),
                PropertyFactory.createBase(e2, String.class))
                .where(e3)
                .and(e4)
                .select(context);
        assertEquals(1, results.size());
        assertEquals("artist4", results.get(0)[0]);
        assertEquals("painting2", results.get(0)[1]);
        assertEquals("painting4", results.get(0)[2]);
    }

    @Test
    public void testOrderWithAliasForExp() {
        Expression e1 = ExpressionFactory.exp("toArtist#p1.artistName");
        ObjectSelect<Painting> query = ObjectSelect.query(Painting.class)
                .orderBy(PropertyFactory.createBase(e1, String.class).asc())
                .prefetch(Painting.TO_ARTIST.disjoint());

        List<Painting> paintings = query.select(context);
        assertEquals(20, paintings.size());
        assertEquals("artist1", paintings.get(0).getToArtist().getArtistName());
    }

    @Test
    public void testAggregationWithAliasesForExp() {
        Expression e1 = ExpressionFactory.exp("paintingArray#p1");
        List<Object[]> artistAndPaintingCount = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.count())
                .having(PropertyFactory.createBase(e1, Number.class).count().lt(5L))
                .select(context);
        assertEquals(4, artistAndPaintingCount.size());
        assertTrue((Long)artistAndPaintingCount.get(0)[1] < 5);
    }

    @Test(expected = CayenneRuntimeException.class)
    public void testPrefetchWithAliasesForExp() {
        Expression e1 = ExpressionFactory.exp("paintingArray#p1");
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class);
        query.prefetch(PropertyFactory.createList("p1", e1, Painting.class).disjoint());
        query.select(context);
    }

    @Test(expected = CayenneRuntimeException.class)
    public void testExpWithAliasNotToRelSegment() {
        Expression e1 = ExpressionFactory.exp("paintingArray.paintingTitle#p1 = 'painting2'");
        ObjectSelect.query(Artist.class)
               .where(e1)
               .select(context);
    }
}
