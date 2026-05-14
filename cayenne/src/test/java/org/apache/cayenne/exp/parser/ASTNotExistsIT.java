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

package org.apache.cayenne.exp.parser;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ASTNotExistsIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @BeforeEach
    public void createArtistsDataSet() throws Exception {
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

    public void evaluateInMemoryNotExistsSubquery() {
        assertThrows(ExpressionException.class, () -> {

            ObjectSelect<Painting> subQuery = ObjectSelect.query(Painting.class)
                    .where(Painting.TO_ARTIST.eq(Artist.ARTIST_ID_PK_PROPERTY.enclosing()));

            doEvaluateWithQuery(ExpressionFactory.notExists(subQuery));
    
        });
    }

    @Test
    public void evaluateInMemoryNotExistsExpression() {
        doEvaluateNoQuery(Artist.PAINTING_ARRAY.notExists());

        doEvaluateNoQuery(Artist.ARTIST_ID_PK_PROPERTY.eq(6L).andExp(Artist.PAINTING_ARRAY.notExists()));

        doEvaluateNoQuery(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).like("p%").notExists());

        doEvaluateNoQuery(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).like("not_exists%").notExists());

        doEvaluateNoQuery(Artist.PAINTING_ARRAY.dot(Painting.TO_PAINTING_INFO).notExists());

        doEvaluateNoQuery(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).notExists());

        doEvaluateNoQuery(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).dot(Gallery.GALLERY_NAME).like("g%").notExists());

        doEvaluateNoQuery(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).dot(Gallery.GALLERY_NAME).like("not_exists%").notExists());

        doEvaluateNoQuery(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).dot(Gallery.GALLERY_NAME).like("g%")
                .andExp(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).like("p%"))
                .notExists());

        doEvaluateNoQuery(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).dot(Gallery.GALLERY_NAME).like("not_exists%")
                .andExp(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).like("p%"))
                .notExists());

        doEvaluateNoQuery(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).dot(Gallery.GALLERY_NAME).like("g%")
                .andExp(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).like("not_exists%"))
                .notExists());

        doEvaluateNoQuery(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).dot(Gallery.GALLERY_NAME).like("not_exists%")
                .andExp(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).like("not_exists%"))
                .notExists());

    }

    private void doEvaluateNoQuery(Expression exp) {
        List<Artist> artistSelected = ObjectSelect.query(Artist.class, exp)
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc())
                .select(env.context());

        List<Artist> artists = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTING_ARRAY.outer().disjoint())
                .prefetch(Artist.PAINTING_ARRAY.outer().dot(Painting.TO_PAINTING_INFO).disjoint())
                .prefetch(Artist.PAINTING_ARRAY.outer().dot(Painting.TO_GALLERY).disjoint())
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc())
                .select(env.context());

        env.runWithQueriesBlocked(() -> {
            List<Artist> artistsFiltered = exp.filterObjects(artists);
            assertEquals(artistSelected, artistsFiltered, exp.toString());
        });
    }

    private void doEvaluateWithQuery(Expression exp) {
        List<Artist> artistSelected = ObjectSelect.query(Artist.class, exp).select(env.context());

        List<Artist> artists = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTING_ARRAY.disjoint())
                .prefetch(Artist.PAINTING_ARRAY.dot(Painting.TO_PAINTING_INFO).disjoint())
                .prefetch(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).disjoint())
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc())
                .select(env.context());

        List<Artist> artistsFiltered = exp.filterObjects(artists);
        assertEquals(artistSelected, artistsFiltered, exp.toString());
    }
}