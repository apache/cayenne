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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ASTExistsIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    @Inject
    private DataChannelInterceptor queryInterceptor;

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

    @Test(expected = ExpressionException.class)
    public void testEvaluateInMemoryExistsSubquery() {
        ObjectSelect<Painting> subQuery = ObjectSelect.query(Painting.class)
                .where(Painting.TO_ARTIST.eq(Artist.ARTIST_ID_PK_PROPERTY.enclosing()));

        doEvaluateWithQuery(ExpressionFactory.notExists(subQuery));
    }

    @Test
    public void testEvaluateInMemoryExistsExpression() {
        doEvaluateNoQuery(Artist.PAINTING_ARRAY.exists());

        doEvaluateNoQuery(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).like("p%").exists());

        doEvaluateNoQuery(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).like("not_exists%").exists());

        doEvaluateNoQuery(Artist.PAINTING_ARRAY.dot(Painting.TO_PAINTING_INFO).exists());

        doEvaluateNoQuery(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).exists());

        doEvaluateNoQuery(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).dot(Gallery.GALLERY_NAME).like("g%").exists());

        doEvaluateNoQuery(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).dot(Gallery.GALLERY_NAME).like("not_exists%").exists());

        doEvaluateNoQuery(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).dot(Gallery.GALLERY_NAME).like("g%")
                .andExp(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).like("p%"))
                .exists());

        doEvaluateNoQuery(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).dot(Gallery.GALLERY_NAME).like("not_exists%")
                .andExp(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).like("p%"))
                .exists());

        doEvaluateNoQuery(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).dot(Gallery.GALLERY_NAME).like("g%")
                .andExp(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).like("not_exists%"))
                .exists());

        doEvaluateNoQuery(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).dot(Gallery.GALLERY_NAME).like("not_exists%")
                .andExp(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).like("not_exists%"))
                .exists());

    }

    private void doEvaluateNoQuery(Expression exp) {
        List<Artist> artistSelected = ObjectSelect.query(Artist.class, exp)
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc())
                .select(context);

        List<Artist> artists = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTING_ARRAY.disjoint())
                .prefetch(Artist.PAINTING_ARRAY.dot(Painting.TO_PAINTING_INFO).disjoint())
                .prefetch(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).disjoint())
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc())
                .select(context);

        queryInterceptor.runWithQueriesBlocked(() -> {
            List<Artist> artistsFiltered = exp.filterObjects(artists);
            assertEquals(exp.toString(), artistSelected, artistsFiltered);
        });
    }

    private void doEvaluateWithQuery(Expression exp) {
        List<Artist> artistSelected = ObjectSelect.query(Artist.class, exp).select(context);

        List<Artist> artists = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTING_ARRAY.disjoint())
                .prefetch(Artist.PAINTING_ARRAY.dot(Painting.TO_PAINTING_INFO).disjoint())
                .prefetch(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).disjoint())
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc())
                .select(context);

        List<Artist> artistsFiltered = exp.filterObjects(artists);
        assertEquals(exp.toString(), artistSelected, artistsFiltered);
    }
}