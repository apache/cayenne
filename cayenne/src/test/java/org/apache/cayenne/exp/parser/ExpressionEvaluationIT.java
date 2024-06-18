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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Here we compare Expression evaluation in-memory vs execution in database.
 * Results should be same for both cases.
 * Here is primary collection of complex expressions:
 *  - To-Many relationships comparisons
 *  - Null comparisons
 *  - Null in AND and OR expressions
 *
 * @since 4.0
 */
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ExpressionEvaluationIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tArtist, tGallery, tPaintings;

    @Before
    public void createArtistsDataSet() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");

        long dateBase = System.currentTimeMillis();
        for (int i = 1; i <= 6; i++) {
            tArtist.insert(i, "artist" + i, new java.sql.Date(dateBase + 10000 * i));
        }

        tGallery = new TableHelper(dbHelper, "GALLERY");
        tGallery.setColumns("GALLERY_ID", "GALLERY_NAME");
        tGallery.insert(1, "tate modern");

        tPaintings = new TableHelper(dbHelper, "PAINTING");
        tPaintings.setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "GALLERY_ID", "ESTIMATED_PRICE");
        for (int i = 1; i <= 10; i++) {
            tPaintings.insert(i, "painting" + i, i % 5 + 1, 1, i * 100);
        }

        tPaintings.insert(11, "painting11", null, 1, 10000);
        tPaintings.insert(12, "painting12", 1, 1, null);
    }

    @Test
    public void testSimpleLike() {
        Expression exp = Artist.ARTIST_NAME
                .like("artist%");

        compareSqlAndEval(exp, 6);
    }

    @Test
    public void testSimpleNotLike() {
        Expression exp = Artist.ARTIST_NAME
                .nlike("artist%");

        compareSqlAndEval(exp, 0);
    }

    @Test
    public void testSimpleEqual() {
        Expression exp = Artist.ARTIST_NAME
                .eq("artist2");

        compareSqlAndEval(exp, 1);
    }

    @Test
    public void testSimpleNotEqual() {
        Expression exp = Artist.ARTIST_NAME
                .ne("artist2");

        compareSqlAndEval(exp, 5);
    }

    @Test
    public void testLikeIgnoreCase() {
        Expression exp = Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE)
                .likeIgnoreCase("painting%");

        compareSqlAndEval(exp, 5);
    }

    @Test
    public void testNotLikeIgnoreCase() {
        Expression exp = Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE)
                .likeIgnoreCase("PaInTing%");

        compareSqlAndEval(exp, 5);
    }

    @Test
    public void testLike() {
        Expression exp = Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE)
                .like("painting%");

        compareSqlAndEval(exp, 5);
    }

    @Test
    public void testNotLike() {
        Expression exp = Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE)
                .like("painting%");

        compareSqlAndEval(exp, 5);
    }

    @Test
    public void testEqual() {
        Expression exp = Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE)
                .eq("painting1");

        compareSqlAndEval(exp, 1);
    }

    @Test
    public void testNotEqual1() {
        Expression exp = Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE)
                .ne("painting1");

        compareSqlAndEval(exp, 5);
    }

    @Test
    public void testNotEqual2() {
        Expression exp = Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE)
                .ne("painting11");

        compareSqlAndEval(exp, 5);
    }

    @Test
    public void testNotEqual3() {
        Expression exp = Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE)
                .ne("zyz");

        compareSqlAndEval(exp, 5);
    }

    @Test
    public void testBetween() {
        Expression exp = Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE)
                .between(new BigDecimal(300), new BigDecimal(600));

        compareSqlAndEval(exp, 4);
    }

    @Test
    public void testNotBetween() {
        Expression exp = ExpressionFactory.notBetweenExp(
                "paintingArray.estimatedPrice",
                new BigDecimal(300), new BigDecimal(600));

        compareSqlAndEval(exp, 5);
    }

    @Test
    public void testGreater() {
        Expression exp = Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE)
                .gt(new BigDecimal(799));

        compareSqlAndEval(exp, 3);
    }

    @Test
    public void testGreaterEqual() {
        Expression exp = Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE)
                .gte(new BigDecimal(800));

        compareSqlAndEval(exp, 3);
    }

    @Test
    public void testIn() {
        Expression exp = Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE)
                .in(new BigDecimal(800), new BigDecimal(300), new BigDecimal(700));

        compareSqlAndEval(exp, 2);
    }

    @Test
    public void testInEmpty() {
        Expression exp = Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE)
                .in(Collections.emptyList());

        compareSqlAndEval(exp, 0);
    }

    @Test
    public void testNotIn() {
        Expression exp = Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE)
                .nin(new BigDecimal(800), new BigDecimal(200), new BigDecimal(300), new BigDecimal(400), new BigDecimal(700));

        compareSqlAndEval(exp, 3);
    }

    @Test
    public void testNotInEmpty() {
        Expression exp = Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE)
                .nin(Collections.emptyList());

        compareSqlAndEval(exp, 6);
    }

    @Test
    public void testLess() {
        Expression exp = Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE)
                .lt(new BigDecimal(801));

        compareSqlAndEval(exp, 5);
    }

    @Test
    public void testLessOrEqual() {
        Expression exp = Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE)
                .lte(new BigDecimal(800));

        compareSqlAndEval(exp, 5);
    }

    @Test
    public void testCollectionWithNull() {
        Expression exp = Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE)
                .lt(new BigDecimal(200));

        compareSqlAndEval(exp, 1);
    }

    @Test
    public void testGreaterWithNull() throws Exception {
        tPaintings.deleteAll();
        tArtist.deleteAll();
        tArtist.insert(7, "artist7", null);

        Expression expression = Artist.DATE_OF_BIRTH
                .gt(new java.sql.Date(System.currentTimeMillis()));

        compareSqlAndEval(expression, 0);

        Expression expression1 = expression.notExp();
        compareSqlAndEval(expression1, 0);
    }

    @Test
    public void testGreaterEqualWithNull() throws Exception {
        tPaintings.deleteAll();
        tArtist.deleteAll();
        tArtist.insert(7, "artist7", null);

        Expression expression = Artist.DATE_OF_BIRTH
                .gte(new java.sql.Date(System.currentTimeMillis()));

        compareSqlAndEval(expression, 0);

        Expression expression1 = expression.notExp();
        compareSqlAndEval(expression1, 0);
    }

    @Test
    public void testLessWithNull() throws Exception {
        tPaintings.deleteAll();
        tArtist.deleteAll();
        tArtist.insert(7, "artist7", null);

        Expression expression = Artist.DATE_OF_BIRTH
                .lt(new java.sql.Date(System.currentTimeMillis()));

        compareSqlAndEval(expression, 0);

        Expression expression1 = expression.notExp();
        compareSqlAndEval(expression1, 0);
    }

    @Test
    public void testLessEqualWithNull() throws Exception {
        tPaintings.deleteAll();
        tArtist.deleteAll();
        tArtist.insert(7, "artist7", null);

        Expression expression = Artist.DATE_OF_BIRTH
                .lte(new java.sql.Date(System.currentTimeMillis()));

        compareSqlAndEval(expression, 0);

        Expression expression1 = expression.notExp();
        compareSqlAndEval(expression1, 0);
    }

    @Test
    public void testAndWithNull() throws Exception {
        tPaintings.deleteAll();
        tArtist.deleteAll();
        tArtist.insert(7, "artist7", null);
        tArtist.insert(8, "artist8", null);
        tArtist.insert(9, "artist9", null);

        Expression nullExp = Artist.DATE_OF_BIRTH.lt(new java.sql.Date(System.currentTimeMillis()));
        Expression and = ExpressionFactory.and(nullExp, Artist.ARTIST_NAME.eq("artist7"));

        compareSqlAndEval(and, 0);
        compareSqlAndEval(and.notExp(), 2);
    }

    @Test
    public void testAndWithNull2() throws Exception {
        tPaintings.deleteAll();
        tArtist.deleteAll();
        tArtist.insert(7, "artist7", null);
        tArtist.insert(8, "artist8", null);
        tArtist.insert(9, "artist9", null);

        Expression nullExp = Artist.DATE_OF_BIRTH.lt(new java.sql.Date(System.currentTimeMillis()));
        Expression and = ExpressionFactory.and(nullExp, Artist.ARTIST_NAME.eq("artist10"));

        compareSqlAndEval(and, 0);
        compareSqlAndEval(and.notExp(), 3);
    }

    @Test
    public void testOrWithNull() throws Exception {
        tPaintings.deleteAll();
        tArtist.deleteAll();
        tArtist.insert(7, "artist7", null);
        tArtist.insert(8, "artist8", null);
        tArtist.insert(9, "artist9", null);

        Expression nullExp = Artist.DATE_OF_BIRTH.lt(new java.sql.Date(System.currentTimeMillis()));
        Expression and = ExpressionFactory.or(nullExp, Artist.ARTIST_NAME.eq("artist7"));

        compareSqlAndEval(and, 1);
        compareSqlAndEval(and.notExp(), 0);
    }

    private void compareSqlAndEval(Expression exp, int expectedCount) {
        // apply exp in SQL
        Ordering ordering = new Ordering("db:ARTIST_ID");
        List<Artist> filteredInSQL = ObjectSelect.query(Artist.class, exp).orderBy(ordering).select(context);
        // apply exp to in-memory collection
        List<Artist> filteredInMemory = exp.filterObjects(
                ObjectSelect.query(Artist.class).prefetch(Artist.PAINTING_ARRAY.disjoint()).select(context)
        );
        ordering.orderList(filteredInMemory);

        assertEquals(expectedCount, filteredInMemory.size());
        assertEquals(filteredInSQL, filteredInMemory);
    }
}
