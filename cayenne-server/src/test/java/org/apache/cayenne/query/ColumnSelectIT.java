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
import java.util.Locale;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.PostgresUnitDbAdapter;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.apache.cayenne.exp.FunctionExpressionFactory.substringExp;
import static org.junit.Assert.assertEquals;
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

    private TableHelper tArtist;

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

        TableHelper tPaintings = new TableHelper(dbHelper, "PAINTING");
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
}
