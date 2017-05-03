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

package org.apache.cayenne.query;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Types;
import java.text.DateFormat;
import java.util.List;
import java.util.Locale;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.Fault;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIteratorCallback;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.FunctionExpressionFactory;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.PostgresUnitDbAdapter;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @since 4.0
 */
@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ColumnSelectIT extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Inject
    private UnitDbAdapter unitDbAdapter;

    // Format: d/m/YY
    private static final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);

    private TableHelper tArtist, tPaintings;

    @Before
    public void createArtistsDataSet() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");
        tArtist.setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.DATE);

        java.sql.Date[] dates = new java.sql.Date[5];
        for(int i=1; i<=5; i++) {
            dates[i-1] = new java.sql.Date(dateFormat.parse("1/" + i + "/17").getTime());
        }
        for (int i = 1; i <= 20; i++) {
            tArtist.insert(i, "artist" + i, dates[i % 5]);
        }

        TableHelper tGallery = new TableHelper(dbHelper, "GALLERY");
        tGallery.setColumns("GALLERY_ID", "GALLERY_NAME");
        tGallery.insert(1, "tate modern");

        tPaintings = new TableHelper(dbHelper, "PAINTING");
        tPaintings.setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "GALLERY_ID", "ESTIMATED_PRICE");
        for (int i = 1; i <= 20; i++) {
            tPaintings.insert(i, "painting" + i, i % 5 + 1, 1, 22 - i);
        }
        tPaintings.insert(21, "painting21", 2, 1, 30);
    }

    @Test
    public void testSelectGroupBy() throws Exception {
        Object[] result = ObjectSelect.query(Artist.class)
                .columns(Artist.DATE_OF_BIRTH, Property.COUNT)
                .orderBy(Artist.DATE_OF_BIRTH.asc())
                .selectFirst(context);

        assertEquals(dateFormat.parse("1/1/17"), result[0]);
        assertEquals(4L, result[1]);
    }

    @Test
    public void testSelectSimpleHaving() throws Exception {
        Object[] result = ObjectSelect.query(Artist.class)
                .columns(Artist.DATE_OF_BIRTH, Property.COUNT)
                .orderBy(Artist.DATE_OF_BIRTH.asc())
                .having(Artist.DATE_OF_BIRTH.eq(dateFormat.parse("1/2/17")))
                .selectOne(context);

        assertEquals(dateFormat.parse("1/2/17"), result[0]);
        assertEquals(4L, result[1]);
    }

    @Test(expected = Exception.class)
    public void testHavingOnNonGroupByColumn() throws Exception {
        Property<String> nameSubstr = Artist.ARTIST_NAME.substring(1, 6);

        Object[] q = ObjectSelect.columnQuery(Artist.class, nameSubstr, Property.COUNT)
                .having(Artist.ARTIST_NAME.like("artist%"))
                .selectOne(context);
        assertEquals("artist", q[0]);
        assertEquals(20L, q[1]);
    }

    @Test
    public void testSelectRelationshipCount() throws Exception {
        Object[] result = ObjectSelect.query(Artist.class)
                .columns(Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY.count())
                .orderBy(Artist.DATE_OF_BIRTH.asc())
                .selectFirst(context);
        assertEquals(dateFormat.parse("1/1/17"), result[0]);
        assertEquals(4L, result[1]);
    }

    @Test
    public void testSelectHavingWithExpressionAlias() throws Exception {

        Object[] q = null;
        try {
            q = ObjectSelect
                    .columnQuery(Artist.class, Artist.ARTIST_NAME.substring(1, 6).alias("name_substr"), Property.COUNT)
                    .having(Property.COUNT.gt(10L))
                    .selectOne(context);
        } catch (CayenneRuntimeException ex) {
            if(unitDbAdapter.supportsExpressionInHaving()) {
                fail();
            } else {
                return;
            }
        }
        assertEquals("artist", q[0]);
        assertEquals(20L, q[1]);
    }

    @Ignore("Need to figure out a better way to handle alias / no alias case for expression in having")
    @Test
    public void testSelectHavingWithExpressionNoAlias() throws Exception {

        Object[] q = null;
        try {
            q = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME.substring(1, 6), Property.COUNT)
                    .having(Property.COUNT.gt(10L))
                    .selectOne(context);
        } catch (CayenneRuntimeException ex) {
            if(unitDbAdapter.supportsExpressionInHaving()) {
                fail();
            } else {
                return;
            }
        }
        assertEquals("artist", q[0]);
        assertEquals(20L, q[1]);
    }

    @Test
    public void testSelectWhereAndHaving() throws Exception {
        Object[] q = null;
        try {
            q = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME.substring(1, 6).alias("name_substr"), Property.COUNT)
                    .where(Artist.ARTIST_NAME.substring(1, 1).eq("a"))
                    .having(Property.COUNT.gt(10L))
                    .selectOne(context);
        } catch (CayenneRuntimeException ex) {
            if(unitDbAdapter.supportsExpressionInHaving()) {
                fail();
            } else {
                return;
            }
        }
        assertEquals("artist", q[0]);
        assertEquals(20L, q[1]);
    }

    @Test
    public void testHavingWithoutAggregate() throws Exception {
        Object date = ObjectSelect.columnQuery(Artist.class, Artist.DATE_OF_BIRTH, Artist.ARTIST_NAME)
                .having(Artist.ARTIST_NAME.like("a%"))
                .selectFirst(context);
        assertNotNull(date);
    }

    /**
     * This test will fail as ARTIST_NAME wouldn't be in GROUP BY,
     * but potentially we can detect this case (e.g. add all fields in HAVING clause to GROUP BY).
     * This just doesn't seem right as in this case WHERE a better choice.
     *
     * Current workaround for this is the method above, i.e. just adding field used
     * in a HAVING qualifier into select.
     */
    @Ignore
    @Test
    public void testHavingWithoutSelect() throws Exception {
        Object date = ObjectSelect.columnQuery(Artist.class, Artist.DATE_OF_BIRTH)
                .having(Artist.ARTIST_NAME.like("a%"))
                .selectFirst(context);
        assertNotNull(date);
    }

    /**
     * Test using field in HAVING clause without using it in SELECT
     * i.e. something like this:
     *      SELECT a.name FROM artist a JOIN painting p ON (..) HAVING COUNT(p.id) > 4
     */
    @Test
    public void testSelectRelationshipCountHavingWithoutFieldSelect() throws Exception {
        Object[] result = null;
        try {
            result = ObjectSelect.query(Artist.class)
                    .columns(Artist.ARTIST_NAME)
                    .having(Artist.PAINTING_ARRAY.count().gt(4L))
                    .selectOne(context);
        } catch (CayenneRuntimeException ex) {
            if(unitDbAdapter.supportsExpressionInHaving()) {
                fail();
            } else {
                return;
            }
        }

        assertEquals("artist2", result[0]);
    }

    @Test
    public void testSelectRelationshipCountHaving() throws Exception {
        Property<Long> paintingCount = Artist.PAINTING_ARRAY.count();

        Object[] result = null;
        try {
            result = ObjectSelect.query(Artist.class)
                .columns(Artist.ARTIST_NAME, paintingCount)
                .having(paintingCount.gt(4L))
                .selectOne(context);
        } catch (CayenneRuntimeException ex) {
            if(unitDbAdapter.supportsExpressionInHaving()) {
                fail();
            } else {
                return;
            }
        }
        assertEquals("artist2", result[0]);
        assertEquals(5L, result[1]);
    }

    @Test
    public void testSelectWithQuoting() throws Exception {
        if(unitDbAdapter instanceof PostgresUnitDbAdapter) {
            // we need to convert somehow all names to lowercase on postgres, so skip it for now
            return;
        }

        Property<Long> paintingCount = Artist.PAINTING_ARRAY.count();
        context.getEntityResolver().getDataMap("testmap").setQuotingSQLIdentifiers(true);

        Object[] result = null;
        try {
            result = ObjectSelect.query(Artist.class)
                    .columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, paintingCount)
                    .having(paintingCount.gt(4L))
                    .selectOne(context);
        } catch (CayenneRuntimeException ex) {
            if(unitDbAdapter.supportsExpressionInHaving()) {
                fail();
            } else {
                return;
            }
        } finally {
            context.getEntityResolver().getDataMap("testmap").setQuotingSQLIdentifiers(false);
        }
        assertEquals("artist2", result[0]);
        assertEquals(5L, result[2]);
    }

    @Test
    public void testSelectGroupByWithQuoting() throws Exception {
        if(unitDbAdapter instanceof PostgresUnitDbAdapter) {
            // we need to convert somehow all names to lowercase on postgres, so skip it for now
            return;
        }

        context.getEntityResolver().getDataMap("testmap").setQuotingSQLIdentifiers(true);
        try {
            Object[] result = ObjectSelect.query(Artist.class)
                    .columns(Artist.DATE_OF_BIRTH, Property.COUNT)
                    .orderBy(Artist.DATE_OF_BIRTH.asc())
                    .selectFirst(context);

            assertEquals(dateFormat.parse("1/1/17"), result[0]);
            assertEquals(4L, result[1]);
        } finally {
            context.getEntityResolver().getDataMap("testmap").setQuotingSQLIdentifiers(false);
        }
    }

    @Test
    public void testAgregateOnRelation() throws Exception {
        BigDecimal min = new BigDecimal(3);
        BigDecimal max = new BigDecimal(30);
        BigDecimal avg = new BigDecimal(BigInteger.valueOf(1290L), 2);
        BigDecimal sum = new BigDecimal(258);

        Property<BigDecimal> estimatedPrice = Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE);
        Object[] minMaxAvgPrice = ObjectSelect.query(Artist.class)
                .where(estimatedPrice.gte(min))
                .min(estimatedPrice).max(estimatedPrice)
                .avg(estimatedPrice)
                .sum(estimatedPrice)
                .count()
                .selectOne(context);

        assertEquals(0, min.compareTo((BigDecimal)minMaxAvgPrice[0]));
        assertEquals(0, max.compareTo((BigDecimal)minMaxAvgPrice[1]));
        assertEquals(0, avg.compareTo((BigDecimal)minMaxAvgPrice[2]));
        assertEquals(0, sum.compareTo((BigDecimal)minMaxAvgPrice[3]));
        assertEquals(20L, minMaxAvgPrice[4]);
    }

    @Test
    public void testQueryCount() throws Exception {
        long count = ObjectSelect
                .columnQuery(Artist.class, Property.COUNT)
                .selectOne(context);

        assertEquals(20, count);

        long count2 = ObjectSelect
                .query(Artist.class)
                .count()
                .selectOne(context);

        assertEquals(count, count2);

        long count3 = ObjectSelect
                .query(Artist.class)
                .selectCount(context);

        assertEquals(count, count3);
    }

    @Test
    public void testQueryCountWithProperty() throws Exception {
        tArtist.insert(21, "artist_21", null);
        tArtist.insert(22, "artist_21", null);

        long count = ObjectSelect
                .columnQuery(Artist.class, Property.COUNT)
                .selectOne(context);
        assertEquals(22, count);

        // COUNT(attribute) should return count of non null values of attribute
        long count2 = ObjectSelect
                .columnQuery(Artist.class, Artist.DATE_OF_BIRTH.count())
                .selectOne(context);
        assertEquals(20, count2);

        long count3 = ObjectSelect
                .query(Artist.class)
                .count(Artist.DATE_OF_BIRTH)
                .selectOne(context);
        assertEquals(count2, count3);
    }

    @Test
    public void testSelectFirst_MultiColumns() throws Exception {
        Object[] a = ObjectSelect.query(Artist.class)
                .columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH)
                .columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH)
                .columns(Artist.ARTIST_NAME.alias("newName"))
                .where(Artist.ARTIST_NAME.like("artist%"))
                .orderBy("db:ARTIST_ID")
                .selectFirst(context);
        assertNotNull(a);
        assertEquals("artist1", a[0]);
        assertEquals("artist1", a[4]);
    }

    @Test
    public void testSelectFirst_SingleValueInColumns() throws Exception {
        Object[] a = ObjectSelect.query(Artist.class)
                .columns(Artist.ARTIST_NAME)
                .where(Artist.ARTIST_NAME.like("artist%"))
                .orderBy("db:ARTIST_ID")
                .selectFirst(context);
        assertNotNull(a);
        assertEquals("artist1", a[0]);
    }

    @Test
    public void testSelectFirst_SubstringName() throws Exception {
        Expression exp = FunctionExpressionFactory.substringExp(Artist.ARTIST_NAME.path(), 5, 3);
        Property<String> substrName = Property.create("substrName", exp, String.class);
        Object[] a = ObjectSelect.query(Artist.class)
                .columns(Artist.ARTIST_NAME, substrName)
                .where(substrName.eq("st3"))
                .selectFirst(context);

        assertNotNull(a);
        assertEquals("artist3", a[0]);
        assertEquals("st3", a[1]);
    }

    @Test
    public void testSelectFirst_RelColumns() throws Exception {
        // set shorter than painting_array.paintingTitle alias as some DBs doesn't support dot in alias
        Property<String> paintingTitle = Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).alias("paintingTitle");

        Object[] a = ObjectSelect.query(Artist.class)
                .columns(Artist.ARTIST_NAME, paintingTitle)
                .orderBy(paintingTitle.asc())
                .selectFirst(context);
        assertNotNull(a);
        assertEquals("painting1", a[1]);
    }

    @Test
    public void testSelectFirst_RelColumn() throws Exception {
        // set shorter than painting_array.paintingTitle alias as some DBs doesn't support dot in alias
        Property<String> paintingTitle = Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).alias("paintingTitle");

        String a = ObjectSelect.query(Artist.class)
                .column(paintingTitle)
                .orderBy(paintingTitle.asc())
                .selectFirst(context);
        assertNotNull(a);
        assertEquals("painting1", a);
    }

    @Test
    public void testSelectFirst_RelColumnWithFunction() throws Exception {
        Property<String> altTitle = Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE)
                .substring(7, 3).concat(" ", Artist.ARTIST_NAME)
                .alias("altTitle");

        String a = ObjectSelect.query(Artist.class)
                .column(altTitle)
                .where(altTitle.like("ng1%"))
                .and(Artist.ARTIST_NAME.like("%ist1"))
//				.orderBy(altTitle.asc()) // unsupported for now
                .selectFirst(context);
        assertNotNull(a);
        assertEquals("ng1 artist1", a);
    }

    /*
     *  Test iterated select
     */

    @Test
    public void testIterationSingleColumn() throws Exception {
        ColumnSelect<String> columnSelect = ObjectSelect.query(Artist.class).column(Artist.ARTIST_NAME);

        final int[] count = new int[1];
        columnSelect.iterate(context, new ResultIteratorCallback<String>() {
            @Override
            public void next(String object) {
                count[0]++;
                assertTrue(object.startsWith("artist"));
            }
        });

        assertEquals(20, count[0]);
    }

    @Test
    public void testBatchIterationSingleColumn() throws Exception {
        ColumnSelect<String> columnSelect = ObjectSelect.query(Artist.class).column(Artist.ARTIST_NAME);

        try(ResultBatchIterator<String> it = columnSelect.batchIterator(context, 10)) {
            List<String> next = it.next();
            assertEquals(10, next.size());
            assertTrue(next.get(0).startsWith("artist"));
        }
    }

    @Test
    public void testIterationMultiColumns() throws Exception {
        ColumnSelect<Object[]> columnSelect = ObjectSelect.query(Artist.class).columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH);

        final int[] count = new int[1];
        columnSelect.iterate(context, new ResultIteratorCallback<Object[]>() {
            @Override
            public void next(Object[] object) {
                count[0]++;
                assertTrue(object[0] instanceof String);
                assertTrue(object[1] instanceof java.util.Date);
            }
        });

        assertEquals(20, count[0]);
    }

    @Test
    public void testBatchIterationMultiColumns() throws Exception {
        ColumnSelect<Object[]> columnSelect = ObjectSelect.query(Artist.class).columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH);

        try(ResultBatchIterator<Object[]> it = columnSelect.batchIterator(context, 10)) {
            List<Object[]> next = it.next();
            assertEquals(10, next.size());
            assertTrue(next.get(0)[0] instanceof String);
            assertTrue(next.get(0)[1] instanceof java.util.Date);
        }
    }

    /*
     *  Test select with page size
     */

    @Test
    public void testPageSizeOneScalar() {
        List<String> a = ObjectSelect.query(Artist.class)
                .column(Artist.ARTIST_NAME.trim())
                .pageSize(10)
                .select(context);
        assertNotNull(a);
        assertEquals(20, a.size());
        int idx = 0;
        for(String next : a) {
            assertNotNull(""+idx, next);
            idx++;
        }
    }

    @Test
    public void testPageSizeScalars() {
        List<Object[]> a = ObjectSelect.query(Artist.class)
                .columns(Artist.ARTIST_NAME.trim(), Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY.count())
                .pageSize(10)
                .select(context);
        assertNotNull(a);
        assertEquals(5, a.size());
        int idx = 0;
        for(Object[] next : a) {
            assertNotNull(next);
            assertTrue("" + idx, next[0] instanceof String);
            assertTrue("" + idx, next[1] instanceof java.util.Date);
            assertTrue("" + idx, next[2] instanceof Long);
            idx++;
        }
    }

    @Test
    public void testPageSizeOneObject() {
        Property<Artist> artistFull = Property.createSelf(Artist.class);
        List<Artist> a = ObjectSelect.query(Artist.class)
                .column(artistFull)
                .pageSize(10)
                .select(context);
        assertNotNull(a);
        assertEquals(20, a.size());
        for(Artist next : a){
            assertNotNull(next);
        }
    }

    @Test
    public void testPageSizeObjectAndScalars() {
        Property<Artist> artistFull = Property.createSelf(Artist.class);
        List<Object[]> a = ObjectSelect.query(Artist.class)
                .columns(Artist.ARTIST_NAME, artistFull, Artist.PAINTING_ARRAY.count())
                .pageSize(10)
                .select(context);
        assertNotNull(a);
        assertEquals(5, a.size());
        int idx = 0;
        for(Object[] next : a) {
            assertNotNull(next);
            assertEquals("" + idx, String.class, next[0].getClass());
            assertEquals("" + idx, Artist.class, next[1].getClass());
            assertEquals("" + idx, Long.class, next[2].getClass());
            idx++;
        }
    }

    @Test
    public void testPageSizeObjects() {
        Property<Artist> artistFull = Property.createSelf(Artist.class);
        List<Object[]> a = ObjectSelect.query(Artist.class)
                .columns(Artist.ARTIST_NAME, artistFull, Artist.PAINTING_ARRAY.flat(Painting.class))
                .pageSize(10)
                .select(context);
        assertNotNull(a);
        assertEquals(21, a.size());
        int idx = 0;
        for(Object[] next : a) {
            assertNotNull(next);
            assertEquals("" + idx, String.class, next[0].getClass());
            assertEquals("" + idx, Artist.class, next[1].getClass());
            assertEquals("" + idx, Painting.class, next[2].getClass());
            idx++;
        }
    }

    /*
     *  Test prefetch
     */

    @Test
    public void testObjectColumnWithJointPrefetch() {
        Property<Artist> artistFull = Property.createSelf(Artist.class);

        List<Object[]> result = ObjectSelect.query(Artist.class)
                .columns(artistFull, Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE))
                .prefetch(Artist.PAINTING_ARRAY.joint())
                .select(context);

        checkPrefetchResults(result);
    }

    @Test
    public void testObjectColumnWithDisjointPrefetch() {
        Property<Artist> artistFull = Property.createSelf(Artist.class);

        List<Object[]> result = ObjectSelect.query(Artist.class)
                .columns(artistFull, Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE))
                .prefetch(Artist.PAINTING_ARRAY.disjoint())
                .select(context);

        checkPrefetchResults(result);
    }

    @Test
    public void testObjectColumnWithDisjointByIdPrefetch() {
        Property<Artist> artistFull = Property.createSelf(Artist.class);

        List<Object[]> result = ObjectSelect.query(Artist.class)
                .columns(artistFull, Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE))
                .prefetch(Artist.PAINTING_ARRAY.disjointById())
                .select(context);

        checkPrefetchResults(result);
    }

    private void checkPrefetchResults(List<Object[]> result) {
        assertEquals(21, result.size());
        for(Object[] next : result) {
            assertTrue(next[0] instanceof Artist);
            assertTrue(next[1] instanceof java.util.Date);
            assertTrue(next[2] instanceof String);
            Artist artist = (Artist)next[0];
            assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());

            Object paintingsArr = artist.readPropertyDirectly(Artist.PAINTING_ARRAY.getName());
            assertFalse(paintingsArr instanceof Fault);
            assertTrue(((List)paintingsArr).size() > 0);
        }
    }

    @Test
    public void testAggregateColumnWithJointPrefetch() {
        Property<Artist> artistFull = Property.createSelf(Artist.class);

        List<Object[]> result = ObjectSelect.query(Artist.class)
                .columns(artistFull, Artist.PAINTING_ARRAY.count())
                .prefetch(Artist.PAINTING_ARRAY.joint())
                .select(context);

        checkAggregatePrefetchResults(result);
    }

    @Test
    public void testAggregateColumnWithDisjointPrefetch() {
        Property<Artist> artistFull = Property.createSelf(Artist.class);

        List<Object[]> result = ObjectSelect.query(Artist.class)
                .columns(artistFull, Artist.PAINTING_ARRAY.count())
                .prefetch(Artist.PAINTING_ARRAY.disjoint())
                .select(context);

        checkAggregatePrefetchResults(result);
    }

    @Test
    public void testAggregateColumnWithDisjointByIdPrefetch() {
        Property<Artist> artistFull = Property.createSelf(Artist.class);

        List<Object[]> result = ObjectSelect.query(Artist.class)
                .columns(artistFull, Artist.PAINTING_ARRAY.count())
                .prefetch(Artist.PAINTING_ARRAY.disjointById())
                .select(context);

        checkAggregatePrefetchResults(result);
    }

    private void checkAggregatePrefetchResults(List<Object[]> result) {
        assertEquals(5, result.size());
        for(Object[] next : result) {
            assertTrue(next[0] instanceof Artist);
            assertTrue(next[1] instanceof Long);
            Artist artist = (Artist)next[0];
            assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());

            Object paintingsArr = artist.readPropertyDirectly(Artist.PAINTING_ARRAY.getName());
            assertFalse(paintingsArr instanceof Fault);
            assertTrue(((List)paintingsArr).size() == (long)next[1]);
        }
    }

    @Test
    public void testObjectSelectWithJointPrefetch() {
        List<Artist> result = ObjectSelect.query(Artist.class)
                .column(Property.createSelf(Artist.class))
                .prefetch(Artist.PAINTING_ARRAY.joint())
                .select(context);
        assertEquals(20, result.size());

        for(Artist artist : result) {
            assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());

            Object paintingsArr = artist.readPropertyDirectly(Artist.PAINTING_ARRAY.getName());
            assertFalse(paintingsArr instanceof Fault);
        }
    }

    @Test
    public void testObjectWithDisjointPrefetch() {
        List<Artist> result = ObjectSelect.query(Artist.class)
                .column(Property.createSelf(Artist.class))
                .prefetch(Artist.PAINTING_ARRAY.disjoint())
                .select(context);
        assertEquals(20, result.size());
        for(Artist artist : result) {
            assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());

            Object paintingsArr = artist.readPropertyDirectly(Artist.PAINTING_ARRAY.getName());
            assertFalse(paintingsArr instanceof Fault);
        }
    }

    @Test
    public void testObjectWithDisjointByIdPrefetch() {
        List<Artist> result = ObjectSelect.query(Artist.class)
                .column(Property.createSelf(Artist.class))
                .prefetch(Artist.PAINTING_ARRAY.disjointById())
                .select(context);
        assertEquals(20, result.size());
        for(Artist artist : result) {
            assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());

            Object paintingsArr = artist.readPropertyDirectly(Artist.PAINTING_ARRAY.getName());
            assertFalse(paintingsArr instanceof Fault);
        }
    }

    /*
     *  Test Persistent object select
     */

    @Test
    public void testObjectColumn() {
        Property<Artist> artistFull = Property.createSelf(Artist.class);

        List<Object[]> result = ObjectSelect.query(Artist.class)
                .columns(artistFull, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.count())
                .select(context);
        assertEquals(5, result.size());

        for(Object[] next : result) {
            assertTrue(next[0] instanceof Artist);
            assertTrue(next[1] instanceof String);
            assertTrue(next[2] instanceof Long);
            assertEquals(PersistenceState.COMMITTED, ((Artist)next[0]).getPersistenceState());
        }
    }

    @Test
    public void testObjectColumnToOne() {
        Property<Artist> artistFull = Property.create(ExpressionFactory.fullObjectExp(Painting.TO_ARTIST.getExpression()), Artist.class);
        Property<Gallery> galleryFull = Property.create(ExpressionFactory.fullObjectExp(Painting.TO_GALLERY.getExpression()), Gallery.class);

        List<Object[]> result = ObjectSelect.query(Painting.class)
                .columns(Painting.PAINTING_TITLE, artistFull, galleryFull)
                .select(context);
        assertEquals(21, result.size());

        for(Object[] next : result) {
            assertTrue(next[0] instanceof String);
            assertTrue(next[1] instanceof Artist);
            assertTrue(next[2] instanceof Gallery);
            assertEquals(PersistenceState.COMMITTED, ((Artist)next[1]).getPersistenceState());
        }
    }

    @Test
    public void testObjectColumnToOneAsObjPath() {

        List<Object[]> result = ObjectSelect.query(Painting.class)
                .columns(Painting.PAINTING_TITLE, Painting.TO_ARTIST, Painting.TO_GALLERY)
                .select(context);
        assertEquals(21, result.size());

        for(Object[] next : result) {
            assertTrue(next[0] instanceof String);
            assertTrue(next[1] instanceof Artist);
            assertTrue(next[2] instanceof Gallery);
            assertEquals(PersistenceState.COMMITTED, ((Artist)next[1]).getPersistenceState());
        }
    }

    @Test
    public void testObjectColumnToMany() throws Exception {
        Property<Artist> artist = Property.createSelf(Artist.class);

        List<Object[]> result = ObjectSelect.query(Artist.class)
                .columns(artist, Artist.PAINTING_ARRAY.flat(Painting.class), Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY))
                .select(context);
        assertEquals(21, result.size());

        for(Object[] next : result) {
            assertTrue(next[0] instanceof Artist);
            assertTrue(next[1] instanceof Painting);
            assertTrue(next[2] instanceof Gallery);
            assertEquals(PersistenceState.COMMITTED, ((Artist)next[0]).getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, ((Painting)(next[1])).getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, ((Gallery)(next[2])).getPersistenceState());
        }
    }

    @Test(expected = CayenneRuntimeException.class)
    public void testDirectRelationshipSelect() {
        // We should fail here as actual result will be just distinct paintings' ids.
        List<List<Painting>> result = ObjectSelect.query(Artist.class)
                .column(Artist.PAINTING_ARRAY).select(context);
        assertEquals(21, result.size());
    }

    @Test(expected = CayenneRuntimeException.class)
    public void testSelfPropertyInOrderBy() {
        Property<Artist> artistProperty = Property.createSelf(Artist.class);
        ObjectSelect.query(Artist.class)
                .column(artistProperty)
                .orderBy(artistProperty.desc())
                .select(context);
    }

    @Test(expected = CayenneRuntimeException.class)
    public void testSelfPropertyInWhere() {
        Artist artist = ObjectSelect.query(Artist.class).selectFirst(context);
        Property<Artist> artistProperty = Property.createSelf(Artist.class);
        List<Artist> result = ObjectSelect.query(Artist.class)
                .column(artistProperty)
                .where(artistProperty.eq(artist))
                .select(context);
    }

    @Test
    public void testObjPropertyInWhere() {
        Artist artist = ObjectSelect.query(Artist.class, Artist.ARTIST_NAME.eq("artist1"))
                .selectFirst(context);
        Property<Painting> paintingProperty = Property.createSelf(Painting.class);
        List<Painting> result = ObjectSelect.query(Painting.class)
                .column(paintingProperty)
                .where(Painting.TO_ARTIST.eq(artist))
                .select(context);
        assertEquals(4, result.size());
    }

    /*
     * Test distinct() / suppressDistinct() methods
     */

    @Test
    public void testExplicitDistinct() throws Exception {
        tArtist.insert(21, "artist1", null);

        List<String> result = ObjectSelect
                .columnQuery(Artist.class, Artist.ARTIST_NAME)
                .select(context);
        assertEquals(21, result.size());

        List<String> result2 = ObjectSelect
                .columnQuery(Artist.class, Artist.ARTIST_NAME)
                .suppressDistinct()
                .select(context);
        assertEquals(result, result2);

        result = ObjectSelect
                .columnQuery(Artist.class, Artist.ARTIST_NAME)
                .distinct()
                .select(context);
        assertEquals(20, result.size());
    }


    @Test
    public void testSuppressDistinct() throws Exception {
        // create non unique artist name / painting name pair
        tArtist.insert(21, "artist1", null);
        tPaintings.insert(22, "painting10", 21, 1, 23);

        List<Object[]> result = ObjectSelect
                .columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE))
                .select(context);
        assertEquals(21, result.size());

        List<Object[]> result2 = ObjectSelect
                .columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE))
                .distinct()
                .select(context);
        assertEquals(result.size(), result2.size());
        for(int i=0; i<result.size(); i++) {
            assertArrayEquals(result.get(i), result2.get(i));
        }

        result = ObjectSelect
                .columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE))
                .suppressDistinct()
                .select(context);
        assertEquals(22, result.size());
    }
}
