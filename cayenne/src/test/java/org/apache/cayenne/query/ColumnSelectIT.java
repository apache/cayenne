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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.Fault;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.property.NumericProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.ArtistExhibit;
import org.apache.cayenne.testdo.testmap.Exhibit;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.PaintingInfo;
import org.apache.cayenne.unit.PostgresUnitDbAdapter;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;

public class ColumnSelectIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private DataContext context;
    private CayenneRuntime runtime;
    private UnitDbAdapter unitDbAdapter;

    // Format: d/m/YY
    private static final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);

    private TableHelper tArtist, tPaintings;

    @BeforeEach
    public void createArtistsDataSet() throws Exception {
        context = env.context();
        runtime = env.runtime();
        unitDbAdapter = env.getInstance(UnitDbAdapter.class);
        tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");
        tArtist.setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.DATE);

        java.sql.Date[] dates = new java.sql.Date[5];
        for(int i=1; i<=5; i++) {
            dates[i-1] = new java.sql.Date(dateFormat.parse("1/" + i + "/17").getTime());
        }
        for (int i = 1; i <= 20; i++) {
            tArtist.insert(i, "artist" + i, dates[i % 5]);
        }

        TableHelper tGallery = env.table("GALLERY", "GALLERY_ID", "GALLERY_NAME");
        tGallery.insert(1, "tate modern");

        tPaintings = env.table("PAINTING", "PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "GALLERY_ID", "ESTIMATED_PRICE");
        for (int i = 1; i <= 20; i++) {
            tPaintings.insert(i, "painting" + i, i % 5 + 1, 1, 22 - i);
        }
        tPaintings.insert(21, "painting21", 2, 1, 30);
    }

    @Test
    public void selectGroupBy() throws Exception {
        Object[] result = Artist.SELF.columnQuery(Artist.DATE_OF_BIRTH, PropertyFactory.COUNT)
                .orderBy(Artist.DATE_OF_BIRTH.asc())
                .selectFirst(context);

        assertEquals(dateFormat.parse("1/1/17"), result[0]);
        assertEquals(4L, result[1]);
    }

    @Test
    public void selectSimpleHaving() throws Exception {
        Object[] result = Artist.SELF.columnQuery(Artist.DATE_OF_BIRTH, PropertyFactory.COUNT)
                .having(Artist.DATE_OF_BIRTH.eq(dateFormat.parse("1/2/17")))
                .selectOne(context);

        assertEquals(dateFormat.parse("1/2/17"), result[0]);
        assertEquals(4L, result[1]);
    }

    @Test
    public void havingOnNonGroupByColumn() {
        StringProperty<String> nameSubstr = Artist.ARTIST_NAME.substring(1, 6);

        assertThrows(Exception.class, () -> Artist.SELF.columnQuery(nameSubstr, PropertyFactory.COUNT)
                .having(Artist.ARTIST_NAME.like("artist%"))
                .selectOne(context));
    }

    @Test
    public void selectRelationshipCount() throws Exception {
        Object[] result = Artist.SELF.columnQuery(Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY.count())
                .orderBy(Artist.DATE_OF_BIRTH.asc())
                .selectFirst(context);
        assertEquals(dateFormat.parse("1/1/17"), result[0]);
        assertEquals(4L, result[1]);
    }

    @Test
    public void selectHavingWithExpressionAlias() {

        Object[] q = null;
        try {
            q = Artist.SELF.columnQuery(Artist.ARTIST_NAME.substring(1, 6).alias("name_substr"), PropertyFactory.COUNT)
                    .having(PropertyFactory.COUNT.gt(10L))
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

    @Disabled("Need to figure out a better way to handle alias / no alias case for expression in having")
    @Test
    public void selectHavingWithExpressionNoAlias() {

        Object[] q = null;
        try {
            q = Artist.SELF.columnQuery(Artist.ARTIST_NAME.substring(1, 6), PropertyFactory.COUNT)
                    .having(PropertyFactory.COUNT.gt(10L))
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
    public void selectWhereAndHaving() {
        Object[] q = null;
        try {
            q = Artist.SELF.columnQuery(Artist.ARTIST_NAME.substring(1, 6).alias("name_substr"), PropertyFactory.COUNT)
                    .where(Artist.ARTIST_NAME.substring(1, 1).eq("a"))
                    .having(PropertyFactory.COUNT.gt(10L))
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
    public void havingWithoutAggregate() {
        Object date = Artist.SELF.columnQuery(Artist.DATE_OF_BIRTH, Artist.ARTIST_NAME)
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
    @Disabled
    @Test
    public void havingWithoutSelect() {
        Object date = Artist.SELF.columnQuery(Artist.DATE_OF_BIRTH)
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
    public void selectRelationshipCountHavingWithoutFieldSelect() {
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
    public void selectRelationshipCountHaving() {
        NumericProperty<Long> paintingCount = Artist.PAINTING_ARRAY.count();

        Object[] result = null;
        try {
            result = Artist.SELF.columnQuery(Artist.ARTIST_NAME, paintingCount)
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
    public void selectWithQuoting() {
        if(unitDbAdapter instanceof PostgresUnitDbAdapter) {
            // we need to convert somehow all names to lowercase on postgres, so skip it for now
            return;
        }

        NumericProperty<Long> paintingCount = Artist.PAINTING_ARRAY.count();
        context.getEntityResolver().getDataMap("testmap").setQuotingSQLIdentifiers(true);

        Object[] result = null;
        try {
            result = Artist.SELF.columnQuery(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, paintingCount)
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
    public void selectGroupByWithQuoting() throws Exception {
        if(unitDbAdapter instanceof PostgresUnitDbAdapter) {
            // we need to convert somehow all names to lowercase on postgres, so skip it for now
            return;
        }

        context.getEntityResolver().getDataMap("testmap").setQuotingSQLIdentifiers(true);
        try {
            Object[] result = Artist.SELF.columnQuery(Artist.DATE_OF_BIRTH, PropertyFactory.COUNT)
                    .orderBy(Artist.DATE_OF_BIRTH.asc())
                    .selectFirst(context);

            assertEquals(dateFormat.parse("1/1/17"), result[0]);
            assertEquals(4L, result[1]);
        } finally {
            context.getEntityResolver().getDataMap("testmap").setQuotingSQLIdentifiers(false);
        }
    }

    @Test
    public void aggregateOnRelation() {
        BigDecimal min = new BigDecimal(3);
        BigDecimal max = new BigDecimal(30);
        BigDecimal avg = new BigDecimal(BigInteger.valueOf(1290L), 2);
        BigDecimal sum = new BigDecimal(258);

        NumericProperty<BigDecimal> estimatedPrice = Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE);
        Object[] minMaxAvgPrice = ObjectSelect.query(Artist.class)
                .where(estimatedPrice.gte(min))
                .min(estimatedPrice)
                .max(estimatedPrice)
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
    public void aggregateOnRelation_withCustomAggregates() {
        BigDecimal min = new BigDecimal(3);
        BigDecimal max = new BigDecimal(30);
        BigDecimal avg = new BigDecimal(BigInteger.valueOf(1290L), 2);
        BigDecimal sum = new BigDecimal(258);
        BigDecimal count = new BigDecimal(20);

        NumericProperty<BigDecimal> estimatedPrice = Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE);
        Object[] minMaxAvgPrice = ObjectSelect.query(Artist.class)
                .where(estimatedPrice.gte(min))
                .aggregate(estimatedPrice, "min", BigDecimal.class)
                .aggregate(estimatedPrice, "max", BigDecimal.class)
                .aggregate(estimatedPrice, "avg", BigDecimal.class)
                .aggregate(estimatedPrice, "sum", BigDecimal.class)
                .aggregate(estimatedPrice, "count", BigDecimal.class)
                .selectOne(context);

        assertEquals(0, min.compareTo((BigDecimal)minMaxAvgPrice[0]));
        assertEquals(0, max.compareTo((BigDecimal)minMaxAvgPrice[1]));
        assertEquals(0, avg.compareTo((BigDecimal)minMaxAvgPrice[2]));
        assertEquals(0, sum.compareTo((BigDecimal)minMaxAvgPrice[3]));
        assertEquals(0, count.compareTo((BigDecimal)minMaxAvgPrice[4]));
    }

    @Test
    public void queryCount() {
        long count = ObjectSelect
                .columnQuery(Artist.class, PropertyFactory.COUNT)
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
    public void queryCountWithProperty() throws Exception {
        tArtist.insert(21, "artist_21", null);
        tArtist.insert(22, "artist_21", null);

        long count = ObjectSelect
                .columnQuery(Artist.class, PropertyFactory.COUNT)
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
    public void selectFirst_MultiColumns() {
        Object[] a = ObjectSelect.query(Artist.class)
                .columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH)
                .columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH)
                .columns(Artist.ARTIST_NAME.alias("newName"))
                .where(Artist.ARTIST_NAME.like("artist%"))
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc())
                .selectFirst(context);
        assertNotNull(a);
        assertEquals("artist1", a[0]);
        assertEquals("artist1", a[4]);
    }

    @Test
    public void selectFirst_SingleValueInColumns() {
        Object[] a = ObjectSelect.query(Artist.class)
                .columns(Artist.ARTIST_NAME)
                .where(Artist.ARTIST_NAME.like("artist%"))
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc())
                .selectFirst(context);
        assertNotNull(a);
        assertEquals("artist1", a[0]);
    }

    @Test
    public void selectFirst_SubstringName() {
        StringProperty<String> substrName = Artist.ARTIST_NAME.substring(5, 3);
        Object[] a = Artist.SELF.columnQuery(Artist.ARTIST_NAME, substrName)
                .where(substrName.eq("st3"))
                .selectFirst(context);

        assertNotNull(a);
        assertEquals("artist3", a[0]);
        assertEquals("st3", a[1]);
    }

    @Test
    public void selectFirst_RelColumns() {
        // set shorter than painting_array.paintingTitle alias as some DBs doesn't support dot in alias
        StringProperty<String> paintingTitle = Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).alias("paintingTitle");

        Object[] a = Artist.SELF.columnQuery(Artist.ARTIST_NAME, paintingTitle)
                .orderBy(paintingTitle.asc())
                .selectFirst(context);
        assertNotNull(a);
        assertEquals("painting1", a[1]);
    }

    @Test
    public void selectFirst_RelColumn() {
        // set shorter than painting_array.paintingTitle alias as some DBs doesn't support dot in alias
        StringProperty<String> paintingTitle = Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).alias("paintingTitle");

        String a = Artist.SELF.columnQuery(paintingTitle)
                .orderBy(paintingTitle.asc())
                .selectFirst(context);
        assertNotNull(a);
        assertEquals("painting1", a);
    }

    @Test
    public void selectFirst_RelColumnWithFunction() {
        StringProperty<String> altTitle = Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE)
                .substring(7, 3).concat(" ", Artist.ARTIST_NAME)
                .alias("altTitle");

        String a = Artist.SELF.columnQuery(altTitle)
                .where(altTitle.like("ng1%"))
                .and(Artist.ARTIST_NAME.like("%ist1"))
//				.orderBy(altTitle.asc()) // unsupported for now
                .selectFirst(context);
        assertNotNull(a);
        assertEquals("ng1 artist1", a);
    }

    @Test
    public void aliasOrder() {
        // test that all table aliases are correct
        List<Object[]> result = Artist.SELF.columnQuery(
                Artist.PAINTING_ARRAY.outer().count(),
                Artist.SELF,
                Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE),
                Artist.SELF,
                Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).dot(Gallery.GALLERY_NAME),
                Artist.ARTIST_NAME,
                Artist.SELF
        ).select(context);
        assertEquals(21, result.size());

        for(Object[] next : result) {
            long count = (Long)next[0];
            Artist artist = (Artist)next[1];
            String paintingTitle = (String)next[2];
            Artist artist2 = (Artist)next[3];
            String galleryName = (String)next[4];
            String artistName = (String)next[5];
            Artist artist3 = (Artist)next[6];

            assertTrue(paintingTitle.startsWith("painting"));
            assertEquals("tate modern", galleryName);
            assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, artist2.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, artist3.getPersistenceState());
            assertEquals(artistName, artist.getArtistName());
            assertTrue(count == 4L || count == 5L);
        }
    }

    /*
     *  Test iterated select
     */

    @Test
    public void iterationSingleColumn() {
        ColumnSelect<String> columnSelect = Artist.SELF.columnQuery(Artist.ARTIST_NAME);

        final int[] count = new int[1];
        columnSelect.iterate(context, object -> {
            count[0]++;
            assertTrue(object.startsWith("artist"));
        });

        assertEquals(20, count[0]);
    }

    @Test
    public void batchIterationSingleColumn() {
        ColumnSelect<String> columnSelect = Artist.SELF.columnQuery(Artist.ARTIST_NAME);

        try(ResultBatchIterator<String> it = columnSelect.batchIterator(context, 10)) {
            List<String> next = it.next();
            assertEquals(10, next.size());
            assertTrue(next.get(0).startsWith("artist"));
        }
    }

    @Test
    public void iterationMultiColumns() {
        ColumnSelect<Object[]> columnSelect = Artist.SELF.columnQuery(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH);

        final int[] count = new int[1];
        columnSelect.iterate(context, object -> {
            count[0]++;
            assertTrue(object[0] instanceof String);
            assertTrue(object[1] instanceof Date);
        });

        assertEquals(20, count[0]);
    }

    @Test
    public void batchIterationMultiColumns() {
        ColumnSelect<Object[]> columnSelect = Artist.SELF.columnQuery(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH);

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
    public void pageSizeOneScalar() {
        List<String> a = Artist.SELF
                .columnQuery(Artist.ARTIST_NAME.trim())
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
    public void pageSizeOneScalarAsArray() {
        List<Object[]> a = ObjectSelect.query(Artist.class)
                .columns(Artist.ARTIST_NAME.trim())
                .pageSize(10)
                .select(context);
        assertNotNull(a);
        assertEquals(20, a.size());
        int idx = 0;
        for(Object[] next : a) {
            assertNotNull(next[0], ""+idx);
            assertTrue(next[0] instanceof String);
            idx++;
        }
    }

    @Test
    public void pageSizeScalars() {
        List<Object[]> a = Artist.SELF
                .columnQuery(Artist.ARTIST_NAME.trim(), Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY.count())
                .pageSize(10)
                .select(context);
        assertNotNull(a);
        assertEquals(5, a.size());
        int idx = 0;
        for(Object[] next : a) {
            assertNotNull(next);
            assertTrue(next[0] instanceof String, "" + idx);
            assertTrue(next[1] instanceof java.util.Date, "" + idx);
            assertTrue(next[2] instanceof Long, "" + idx);
            idx++;
        }
    }

    @Test
    public void pageSizeOneObject() {
        List<Artist> a = Artist.SELF
                .columnQuery(Artist.SELF)
                .pageSize(10)
                .select(context);
        assertNotNull(a);
        assertEquals(20, a.size());
        for(Artist next : a){
            assertNotNull(next);
        }
    }

    @Test
    public void pageSizeOneObjectAsArray() {
        List<Object[]> a = ObjectSelect.query(Artist.class)
                .columns(Artist.SELF)
                .pageSize(10)
                .select(context);
        assertNotNull(a);
        assertEquals(20, a.size());
        for(Object[] next : a){
            assertNotNull(next[0]);
            assertTrue(next[0] instanceof Artist);
        }
    }

    @Test
    public void pageSizeObjectAndScalars() {
        List<Object[]> a = Artist.SELF
                .columnQuery(Artist.ARTIST_NAME, Artist.SELF, Artist.PAINTING_ARRAY.count())
                .pageSize(10)
                .select(context);
        assertNotNull(a);
        assertEquals(5, a.size());
        int idx = 0;
        for(Object[] next : a) {
            assertNotNull(next);
            assertEquals(String.class, next[0].getClass(), "" + idx);
            assertEquals(Artist.class, next[1].getClass(), "" + idx);
            assertEquals(Long.class, next[2].getClass(), "" + idx);
            idx++;
        }
    }

    @Test
    public void pageSizeObjects() {
        List<Object[]> a = Artist.SELF
                .columnQuery(Artist.ARTIST_NAME, Artist.SELF, Artist.PAINTING_ARRAY.flat())
                .pageSize(10)
                .select(context);

        assertNotNull(a);
        assertEquals(21, a.size());
        int idx = 0;
        for(Object[] next : a) {
            assertNotNull(next);
            assertEquals(String.class, next[0].getClass(), "" + idx);
            assertEquals(Artist.class, next[1].getClass(), "" + idx);
            if(next[2] != null) {
                assertEquals(Painting.class, next[2].getClass(), "" + idx);
            }
            idx++;
        }
    }

    /*
     *  Test prefetch
     */

    @Test
    public void objectColumnWithJointPrefetch() {
        List<Object[]> result = Artist.SELF
                .columnQuery(Artist.SELF, Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE))
                .prefetch(Artist.PAINTING_ARRAY.joint())
                .select(context);

        checkPrefetchResults(result);
    }

    @Test
    public void objectColumnWithDisjointPrefetch() {
        List<Object[]> result = Artist.SELF
                .columnQuery(Artist.SELF, Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE))
                .prefetch(Artist.PAINTING_ARRAY.disjoint())
                .select(context);

        checkPrefetchResults(result);
    }

    @Test
    public void objectColumnWithDisjointByIdPrefetch() {
        List<Object[]> result = Artist.SELF
                .columnQuery(Artist.SELF, Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE))
                .prefetch(Artist.PAINTING_ARRAY.disjointById())
                .select(context);

        checkPrefetchResults(result);
    }

    private void checkPrefetchResults(List<Object[]> result) {
        assertEquals(21, result.size());
        for(Object[] next : result) {
            assertInstanceOf(Artist.class, next[0]);
            assertInstanceOf(java.util.Date.class, next[1]);
            assertInstanceOf(String.class, next[2]);
            Artist artist = (Artist)next[0];
            assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());

            Object paintingsArr = artist.readPropertyDirectly(Artist.PAINTING_ARRAY.getName());
            assertFalse(paintingsArr instanceof Fault);
            assertTrue(((List)paintingsArr).size() > 0);
        }
    }

    @Test
    public void aggregateColumnWithJointPrefetch() {
        List<Object[]> result = Artist.SELF.columnQuery(Artist.SELF, Artist.PAINTING_ARRAY.count())
                .prefetch(Artist.PAINTING_ARRAY.joint())
                .select(context);

        checkAggregatePrefetchResults(result);
    }

    @Test
    public void aggregateColumnWithDisjointPrefetch() {
        List<Object[]> result = Artist.SELF.columnQuery(Artist.SELF, Artist.PAINTING_ARRAY.count())
                .prefetch(Artist.PAINTING_ARRAY.disjoint())
                .select(context);

        checkAggregatePrefetchResults(result);
    }

    @Test
    public void aggregateColumnWithDisjointByIdPrefetch() {
        List<Object[]> result = Artist.SELF.columnQuery(Artist.SELF, Artist.PAINTING_ARRAY.count())
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
            assertEquals(((List) paintingsArr).size(), (long) next[1]);
        }
    }

    @Test
    public void objectSelectWithJointPrefetch() {
        List<Artist> result = Artist.SELF.columnQuery(Artist.SELF)
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
    public void objectWithDisjointPrefetch() {
        List<Artist> result = Artist.SELF.columnQuery(Artist.SELF)
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
    public void objectWithDisjointByIdPrefetch() {
        List<Artist> result = Artist.SELF.columnQuery(Artist.SELF)
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
    public void objectColumn() {
        List<Object[]> result = ObjectSelect.query(Artist.class)
                .columns(Artist.SELF, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.count())
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
    public void objectColumnToOne() {
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
    public void objectColumnToOneAsObjPath() {

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
    public void objectColumnToMany() {
        List<Object[]> result = ObjectSelect.query(Artist.class)
                .columns(Artist.SELF, Artist.PAINTING_ARRAY.flat(), Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY))
                .select(context);
        assertEquals(21, result.size());

        for(Object[] next : result) {
            assertTrue(next[0] instanceof Artist);
            assertEquals(PersistenceState.COMMITTED, ((Artist)next[0]).getPersistenceState());
            if(next[1] != null) {
                assertTrue(next[1] instanceof Painting);
                assertEquals(PersistenceState.COMMITTED, ((Painting)(next[1])).getPersistenceState());
            }
            if(next[2] != null) {
                assertTrue(next[2] instanceof Gallery);
                assertEquals(PersistenceState.COMMITTED, ((Gallery) (next[2])).getPersistenceState());
            }
        }
    }

    @Test
    public void directRelationshipSelect() {
        // We should fail here as actual result will be just distinct paintings' ids.
        assertThrows(CayenneRuntimeException.class, () -> {
            List<List<Painting>> result = ObjectSelect.query(Artist.class)
                    .column(Artist.PAINTING_ARRAY).select(context);
            assertEquals(21, result.size());
        });
    }

    @Test
    public void selfPropertyInOrderBy() {
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .column(Artist.SELF)
                .orderBy(Artist.SELF.desc())
                .select(context);
        assertEquals(20, artists.size());
        assertEquals("artist20", artists.get(0).getArtistName());
        assertEquals("artist1", artists.get(19).getArtistName());
    }

    @Test
    public void selfPropertyInWhere() {
        Artist artist = Artist.SELF.query().selectFirst(context);
        Artist selectedArtist = ObjectSelect.query(Artist.class)
                .column(Artist.SELF)
                .where(Artist.SELF.eq(artist))
                .orderBy(Artist.SELF.asc())
                .selectOne(context);
        assertNotNull(selectedArtist);
        assertEquals(artist.getArtistName(), selectedArtist.getArtistName());
    }

    @Test
    public void objPropertyInWhere() {
        Artist artist = ObjectSelect.query(Artist.class, Artist.ARTIST_NAME.eq("artist1"))
                .selectFirst(context);
        List<Painting> result = ObjectSelect.query(Painting.class)
                .column(Painting.SELF)
                .where(Painting.TO_ARTIST.eq(artist))
                .select(context);
        assertEquals(4, result.size());
    }

    /*
     * Test distinct() / suppressDistinct() methods
     */

    @Test
    public void explicitDistinct() throws Exception {
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
    public void suppressDistinct() throws Exception {
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

    /*
     * Test selection from nested context
     */

    @Test
    public void nestedContextScalarResult() {
        ObjectContext childContext = runtime.newContext(context);

        List<String> names = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME)
                .select(childContext);
        assertEquals(20, names.size());
        for(String name : names) {
            assertNotNull(name);
        }
    }
    @Test
    public void nestedContextObjectResult() {
        ObjectContext childContext = runtime.newContext(context);
        List<Artist> artists = ObjectSelect.columnQuery(Artist.class, Artist.SELF)
                .select(childContext);
        assertEquals(20, artists.size());
        for(Artist artist : artists) {
            assertNotNull(artist);
        }
    }

    @Test
    public void nestedContextScalarArrayResult() {
        ObjectContext childContext = runtime.newContext(context);

        List<Object[]> data = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH)
                .select(childContext);
        assertEquals(20, data.size());
        for(Object[] next : data) {
            assertTrue(next[0] instanceof String);
            assertTrue(next[1] instanceof Date);
        }
    }

    @Test
    public void nestedContextMixedResult() {
        ObjectContext childContext = runtime.newContext(context);

        List<Object[]> data = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.SELF)
                .select(childContext);
        assertEquals(20, data.size());
        for(Object[] next : data) {
            assertTrue(next[0] instanceof String);
            assertTrue(next[1] instanceof Artist);
        }
    }

    @Test
    public void byteArraySelect() throws SQLException {
        env.table("PAINTING_INFO")
                .setColumns("IMAGE_BLOB", "PAINTING_ID")
                .setColumnTypes(Types.LONGVARBINARY, Types.INTEGER)
                .insert(new byte[]{(byte)1, (byte)2, (byte)3, (byte)4, (byte)5}, 1)
                .insert(new byte[]{(byte)5, (byte)4, (byte)3, (byte)2}, 2);

        List<byte[]> blobs = ObjectSelect.columnQuery(PaintingInfo.class, PaintingInfo.IMAGE_BLOB)
                .orderBy("db:" + PaintingInfo.PAINTING_ID_PK_COLUMN)
                .select(context);

        assertEquals(2, blobs.size());
        assertArrayEquals(new byte[]{(byte)1, (byte)2, (byte)3, (byte)4, (byte)5}, blobs.get(0));
        assertArrayEquals(new byte[]{(byte)5, (byte)4, (byte)3, (byte)2}, blobs.get(1));
    }

    @Test
    public void collectionProperty() {
        Painting painting = ObjectSelect.query(Painting.class).selectFirst(context);

        Artist artist = ObjectSelect.query(Artist.class)
                .where(Artist.PAINTING_ARRAY.contains(painting))
                .and(Artist.DATE_OF_BIRTH.year().gt(1950))
                .and(Artist.ARTIST_NAME.like("artist%"))
                .selectOne(context);
        assertNotNull(artist);
        assertTrue(artist.getArtistName().startsWith("artist"));
    }

    @Test
    public void pkSelect2() {
        List<Object[]> results = ObjectSelect.columnQuery(Artist.class,
                        Artist.SELF,
                        Artist.PAINTING_ARRAY.dot(Painting.TO_ARTIST).dot(Artist.ARTIST_ID_PK_PROPERTY))
                .where(Artist.ARTIST_ID_PK_PROPERTY.eq(1L))
                .pageSize(1)
                .select(context);
        assertEquals(1, results.size());
        assertEquals("artist1", ((Artist)results.get(0)[0]).getArtistName());
        assertEquals(1L, results.get(0)[1]);
    }

    @Test
    public void objects2Pk() {
        Artist artist = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_ID_PK_PROPERTY.eq(1L))
                .selectFirst(context);
        ArtistExhibit artistExhibit = context.newObject(ArtistExhibit.class);
        Exhibit exhibit = context.newObject(Exhibit.class);
        exhibit.setOpeningDate(new Date());
        exhibit.setClosingDate(new Date());
        artistExhibit.setToArtist(artist);
        artistExhibit.setToExhibit(exhibit);
        Gallery gallery = context.newObject(Gallery.class);
        gallery.setGalleryName("Test");
        exhibit.setToGallery(gallery);
        context.commitChanges();

        List<Object[]> results = ObjectSelect.columnQuery(Artist.class,
                        Artist.ARTIST_NAME,
                        Artist.SELF,
                        Artist.ARTIST_EXHIBIT_ARRAY.dot(ArtistExhibit.ARTIST_ID_PK_PROPERTY))
                .where(Artist.ARTIST_ID_PK_PROPERTY.eq(1L))
                .pageSize(1)
                .select(context);
        assertEquals(1, results.size());
        assertEquals("artist1", results.get(0)[0]);
        assertEquals("artist1", ((Artist)results.get(0)[1]).getArtistName());
        assertEquals(1L, results.get(0)[2]);
    }

    @Test
    public void mapToPojo() {
        List<TestPojo> result = ObjectSelect.query(Artist.class)
                .columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, Artist.ARTIST_NAME.trim().length())
                .where(Artist.ARTIST_NAME.like("artist%"))
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc())
                .map(TestPojo::new)
                .select(context);

        assertEquals(20, result.size());

        TestPojo testPojo0 = result.get(0);
        assertNotNull(testPojo0);
        assertEquals("artist1", testPojo0.name);
        assertNotNull(testPojo0.date);
        assertEquals(7, testPojo0.length);

        TestPojo testPojo19 = result.get(19);
        assertNotNull(testPojo19);
        assertEquals("artist20", testPojo19.name);
        assertEquals(8, testPojo19.length);
        assertNotNull(testPojo19.date);
    }

    @Test
    public void doubleMapToPojo() {
        List<TestPojo2> result = ObjectSelect.query(Artist.class)
                .columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, Artist.ARTIST_NAME.trim().length())
                .where(Artist.ARTIST_NAME.like("artist%"))
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc())
                .map(TestPojo::new)
                .map(TestPojo2::new)
                .select(context);
        assertEquals(20, result.size());

        TestPojo2 testPojo0 = result.get(0);
        assertNotNull(testPojo0);
        assertEquals("artist1", testPojo0.pojo.name);
        assertNotNull(testPojo0.pojo.date);
        assertEquals(7, testPojo0.pojo.length);
    }

    @Test
    public void sharedCache() {
        ColumnSelect<Object[]> query = ObjectSelect.query(Artist.class)
                .columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, Artist.SELF)
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc())
                .cacheStrategy(QueryCacheStrategy.SHARED_CACHE);

        List<Object[]> result = query.select(context);
        assertEquals(20, result.size());
        assertInstanceOf(Artist.class, result.get(0)[2], "Should be an instance of Artist");

        List<Object[]> result2 = query.select(context);
        assertEquals(20, result2.size());
        assertInstanceOf(Artist.class, result.get(0)[2], "Should be an instance of Artist");
    }

    static class TestPojo {
        String name;
        Date date;
        int length;
        TestPojo(Object[] data) {
            name = (String)data[0];
            date = (Date)data[1];
            length = (Integer)data[2];
        }
    }

    static class TestPojo2 {
        TestPojo pojo;
        TestPojo2(TestPojo pojo) {
            this.pojo = pojo;
        }
    }
}
