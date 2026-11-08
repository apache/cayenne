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
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.query.ObjectSelect;
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

public class TranslateExpressionIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private DataContext context;

    @BeforeEach
    public void createArtistsDataSet() throws Exception {
        context = env.context();
        TableHelper tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");

        long dateBase = System.currentTimeMillis();
        for (int i = 1; i <= 20; i++) {
            tArtist.insert(i, "artist" + i, new java.sql.Date(dateBase + 10000 * i));
        }

        TableHelper tGallery = env.table("GALLERY", "GALLERY_ID", "GALLERY_NAME");
        tGallery.insert(1, "tate modern");

        TableHelper tPaintings = env.table("PAINTING", "PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "GALLERY_ID");
        for (int i = 1; i <= 20; i++) {
            tPaintings.insert(i, "painting" + i, i % 5 + 1, 1);
        }
    }

    @Test
    public void prefetchWithTranslatedExp() {
        List<Painting> result = ObjectSelect.query(Painting.class)
                .where(Painting.ARTIST
                        .dot(Artist.PAINTINGS)
                        .dot(Painting.PAINTING_TITLE).like("painting7"))
                .and(Painting.PAINTING_TITLE.like("painting2"))
                .prefetch(Painting.ARTIST.disjoint())
                .select(context);
        assertEquals(1, result.size());
        assertEquals("artist3", result.get(0).getArtist().getArtistName());
    }

    @Test
    public void prefetchWithTheSamePrefetchAndQualifier() {
        List<Painting> result = ObjectSelect.query(Painting.class)
                .where(Painting.GALLERY
                        .dot(Gallery.PAINTINGS)
                        .dot(Painting.PAINTING_TITLE)
                        .eq("painting1"))
                .and(Painting.PAINTING_TITLE.like("painting2"))
                .prefetch(Painting.GALLERY.disjoint())
                .prefetch(Painting.GALLERY.dot(Gallery.PAINTINGS).disjoint())
                .select(context);
        assertEquals(1, result.size());
        assertEquals("painting2", result.get(0).getPaintingTitle());
    }

    @Test
    public void translateExpression() {
        CompiledEntity entity = context.getMapping().entity("Painting");
        Expression expression = ExpressionFactory.pathExp("artist.paintings");
        Expression translatedExpression = entity
                .translateToRelatedTable(expression, CayennePath.of("artist"));
        assertEquals(ExpressionFactory
                .dbPathExp("paintings.artist.paintings"),
                translatedExpression);
    }

    @Test
    public void relationshipPathEqualsToInput() {
        CompiledEntity entity = context.getMapping().entity("Painting");
        Expression expression = ExpressionFactory.pathExp("artist");
        Expression translatedExpression = entity
                .translateToRelatedTable(expression, CayennePath.of("artist"));
        assertEquals(ExpressionFactory.dbPathExp("paintings.artist"),
                translatedExpression);
    }

    @Test
    public void relationshipNoneLeadingParts() {
        CompiledEntity entity = context.getMapping().entity("Painting");
        Expression expression = ExpressionFactory.pathExp("gallery");
        Expression translatedExpression = entity
                .translateToRelatedTable(expression, CayennePath.of("artist"));
        assertEquals(ExpressionFactory.dbPathExp("paintings.gallery"),
                translatedExpression);
    }

    @Test
    public void relationshipSomeLeadingParts() {
        CompiledEntity entity = context.getMapping().entity("Painting");
        Expression expression = ExpressionFactory.pathExp("gallery");
        Expression translatedExpression = entity
                .translateToRelatedTable(expression, CayennePath.of("artist.paintings.gallery"));
        assertEquals(ExpressionFactory.dbPathExp("paintings.artist.paintings.gallery"),
                translatedExpression);
    }

    @Test
    public void compQualifier() {
        CompiledEntity entity = context.getMapping().entity("Painting");
        Expression expression = ExpressionFactory.pathExp("artist.artistExhibits.exhibit");
        Expression translatedExpression = entity
                .translateToRelatedTable(expression, CayennePath.of("gallery"));
        assertEquals(ExpressionFactory.dbPathExp("paintings.artist.artistExhibits.exhibit"),
                translatedExpression);
    }

    @Test
    public void compQualifierAndPref() {
        CompiledEntity entity = context.getMapping().entity("Artist");
        Expression expression = ExpressionFactory.pathExp("paintings.gallery");
        Expression translatedExpression = entity
                .translateToRelatedTable(expression, CayennePath.of("artistExhibits.exhibit"));
        assertEquals(ExpressionFactory.dbPathExp("artistExhibits.artist.paintings.gallery"),
                translatedExpression);
    }
}
