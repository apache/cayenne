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

import java.util.Date;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
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

/**
 * @since 4.2
 */
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ObjectSelect_SubqueryIT extends RuntimeCase {

    @Inject
    DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Before
    public void createArtistsDataSet() throws Exception {
        TableHelper tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");

        long dateBase = System.currentTimeMillis() - 2 * 24 * 3600 * 1000;
        for (int i = 1; i <= 20; i++) {
            tArtist.insert(i, "artist" + i, new java.sql.Date(dateBase + 24 * 3600 * 1000 * i));
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
    public void selectQuery_simpleExists() {
        long count = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.exists(ObjectSelect.query(Painting.class, Painting.PAINTING_TITLE.like("painting%"))))
                .selectCount(context);
        assertEquals(20L, count);
    }

    @Test
    public void selectQuery_existsWithExpressionFromParentQuery() {
        Expression exp = Painting.TO_ARTIST.eq(Artist.ARTIST_ID_PK_PROPERTY.enclosing())
                .andExp(Painting.PAINTING_TITLE.like("painting%"))
                .andExp(Artist.ARTIST_NAME.enclosing().like("art%"));

        ColumnSelect<String> subQuery = ObjectSelect.columnQuery(Painting.class, Painting.PAINTING_TITLE).where(exp);
        long count = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.exists(subQuery))
                .selectCount(context);
        assertEquals(5L, count);
    }

    @Test
    public void selectQuery_notExistsWithExpressionFromParentQuery() {
        ObjectSelect<Painting> subQuery = ObjectSelect.query(Painting.class)
                .where(Painting.TO_ARTIST.eq(Artist.ARTIST_ID_PK_PROPERTY.enclosing()));
        long count = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.notExists(subQuery))
                .selectCount(context);
        assertEquals(15L, count);
    }

    @Test
    public void selectQuery_twoLevelExists() {
        Expression exp = Painting.PAINTING_TITLE.like("painting%")
                .andExp(ExpressionFactory.exists(ObjectSelect.query(Gallery.class)));
        long count = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.exists(ObjectSelect.query(Painting.class, exp)))
                .selectCount(context);
        assertEquals(20L, count);
    }

    @Test
    public void selectQuery_twoLevelExistsWithExpressionFromParentQuery() {
        Expression deepNestedExp = Artist.ARTIST_NAME.enclosing().enclosing().like("art%")
                .andExp(Painting.TO_GALLERY.enclosing().eq(Gallery.SELF));

        Expression exp = Painting.PAINTING_TITLE.like("painting%")
                .andExp(ExpressionFactory.exists(ObjectSelect.query(Gallery.class, deepNestedExp)))
                .andExp(Painting.TO_ARTIST.eq(Artist.SELF.enclosing()));

        long count = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.exists(ObjectSelect.query(Painting.class, exp)))
                .selectCount(context);
        assertEquals(5L, count);
    }

    @Test
    public void objectSelect_twoLevelExistsWithExpressionFromParentQuery() {
        ObjectSelect<Gallery> deepSubquery = ObjectSelect.query(Gallery.class)
                .where(Artist.ARTIST_NAME.enclosing().enclosing().like("art%"))
                .and(Painting.TO_GALLERY.enclosing().eq(Gallery.SELF));

        ObjectSelect<Painting> subquery = ObjectSelect.query(Painting.class)
                .where(Painting.PAINTING_TITLE.like("painting%"))
                .and(Painting.TO_ARTIST.eq(Artist.SELF.enclosing()))
                .and(ExpressionFactory.exists(deepSubquery));

        long count = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.exists(subquery))
                .selectCount(context);

        assertEquals(5L, count);
    }

    @Test
    public void columnSelect_simpleInSubquery() {

        ColumnSelect<String> subquery = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME)
                .where(Artist.DATE_OF_BIRTH.lt(new Date()));

        long count = ObjectSelect.query(Painting.class)
                .where(Painting.TO_ARTIST.dot(Artist.ARTIST_NAME).in(subquery))
                .selectCount(context);

        assertEquals(4L, count);
    }

    @Test
    public void columnSelect_simpleNotInSubquery() {

        ColumnSelect<String> subquery = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME)
                .where(Artist.DATE_OF_BIRTH.lt(new Date()));

        long count = ObjectSelect.query(Painting.class)
                .where(Painting.TO_ARTIST.dot(Artist.ARTIST_NAME).nin(subquery))
                .selectCount(context);

        assertEquals(16L, count);
    }

    @Test
    public void selectQuery_ltAll() {
        long count = ObjectSelect.query(Artist.class)
                .where(Artist.DATE_OF_BIRTH.year().ltAll(ObjectSelect.columnQuery(Artist.class, Artist.DATE_OF_BIRTH.year())))
                .selectCount(context);
        assertEquals(0L, count);
    }

}
