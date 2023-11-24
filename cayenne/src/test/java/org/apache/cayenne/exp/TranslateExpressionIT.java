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

package org.apache.cayenne.exp;

import java.util.List;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.ObjEntity;
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

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class TranslateExpressionIT extends RuntimeCase {

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

        TableHelper tPaintings = new TableHelper(dbHelper, "PAINTING");
        tPaintings.setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "GALLERY_ID");
        for (int i = 1; i <= 20; i++) {
            tPaintings.insert(i, "painting" + i, i % 5 + 1, 1);
        }
    }

    @Test
    public void testPrefetchWithTranslatedExp() {
        List<Painting> result = ObjectSelect.query(Painting.class)
                .where(Painting.TO_ARTIST
                        .dot(Artist.PAINTING_ARRAY)
                        .dot(Painting.PAINTING_TITLE).like("painting7"))
                .and(Painting.PAINTING_TITLE.like("painting2"))
                .prefetch(Painting.TO_ARTIST.disjoint())
                .select(context);
        assertEquals(1, result.size());
        assertEquals("artist3", result.get(0).getToArtist().getArtistName());
    }

    @Test
    public void testPrefetchWithTheSamePrefetchAndQualifier() {
        List<Painting> result = ObjectSelect.query(Painting.class)
                .where(Painting.TO_GALLERY
                        .dot(Gallery.PAINTING_ARRAY)
                        .dot(Painting.PAINTING_TITLE)
                        .eq("painting1"))
                .and(Painting.PAINTING_TITLE.like("painting2"))
                .prefetch(Painting.TO_GALLERY.disjoint())
                .prefetch(Painting.TO_GALLERY.dot(Gallery.PAINTING_ARRAY).disjoint())
                .select(context);
        assertEquals(1, result.size());
        assertEquals("painting2", result.get(0).getPaintingTitle());
    }

    @Test
    public void testTranslateExpression() {
        ObjEntity entity = context.getEntityResolver().getObjEntity("Painting");
        Expression expression = ExpressionFactory.pathExp("toArtist.paintingArray");
        Expression translatedExpression = entity
                .translateToRelatedEntity(expression, "toArtist");
        assertEquals(ExpressionFactory
                .dbPathExp("paintingArray.toArtist.paintingArray"),
                translatedExpression);
    }

    @Test
    public void testRelationshipPathEqualsToInput() {
        ObjEntity entity = context.getEntityResolver().getObjEntity("Painting");
        Expression expression = ExpressionFactory.pathExp("toArtist");
        Expression translatedExpression = entity
                .translateToRelatedEntity(expression, "toArtist");
        assertEquals(ExpressionFactory.dbPathExp("paintingArray.toArtist"),
                translatedExpression);
    }

    @Test
    public void testRelationshipNoneLeadingParts() {
        ObjEntity entity = context.getEntityResolver().getObjEntity("Painting");
        Expression expression = ExpressionFactory.pathExp("toGallery");
        Expression translatedExpression = entity
                .translateToRelatedEntity(expression, "toArtist");
        assertEquals(ExpressionFactory.dbPathExp("paintingArray.toGallery"),
                translatedExpression);
    }

    @Test
    public void testRelationshipSomeLeadingParts() {
        ObjEntity entity = context.getEntityResolver().getObjEntity("Painting");
        Expression expression = ExpressionFactory.pathExp("toGallery");
        Expression translatedExpression = entity
                .translateToRelatedEntity(expression, "toArtist.paintingArray.toGallery");
        assertEquals(ExpressionFactory.dbPathExp("paintingArray.toArtist.paintingArray.toGallery"),
                translatedExpression);
    }

    @Test
    public void testCompQualifier() {
        ObjEntity entity = context.getEntityResolver().getObjEntity("Painting");
        Expression expression = ExpressionFactory.pathExp("toArtist.artistExhibitArray.toExhibit");
        Expression translatedExpression = entity
                .translateToRelatedEntity(expression, "toGallery");
        assertEquals(ExpressionFactory.dbPathExp("paintingArray.toArtist.artistExhibitArray.toExhibit"),
                translatedExpression);
    }

    @Test
    public void testCompQualifierAndPref() {
        ObjEntity entity = context.getEntityResolver().getObjEntity("Artist");
        Expression expression = ExpressionFactory.pathExp("paintingArray.toGallery");
        Expression translatedExpression = entity
                .translateToRelatedEntity(expression, "artistExhibitArray.toExhibit");
        assertEquals(ExpressionFactory.dbPathExp("artistExhibitArray.toArtist.paintingArray.toGallery"),
                translatedExpression);
    }
}
