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
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.apache.cayenne.exp.FunctionExpressionFactory.countExp;
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
    private DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);


    @Before
    public void createArtistsDataSet() throws Exception {
        TableHelper tArtist = new TableHelper(dbHelper, "ARTIST");
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
        tPaintings.setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "GALLERY_ID");
        for (int i = 1; i <= 20; i++) {
            tPaintings.insert(i, "painting" + i, i % 5 + 1, 1);
        }
        tPaintings.insert(21, "painting21", 2, 1);
    }

    @Test
    public void testSelectGroupBy() throws Exception {
        Property<Long> count = Property.create(countExp(), Long.class);

        Object[] result = ColumnSelect.query(Artist.class)
                .columns(Artist.DATE_OF_BIRTH, count)
                .orderBy(Artist.DATE_OF_BIRTH.asc())
                .selectFirst(context);

        assertEquals(dateFormat.parse("1/1/17"), result[0]);
        assertEquals(4L, result[1]);
    }

    @Test
    public void testSelectSimpleHaving() throws Exception {
        Property<Long> count = Property.create(countExp(), Long.class);

        Object[] result = ColumnSelect.query(Artist.class)
                .columns(Artist.DATE_OF_BIRTH, count)
                .orderBy(Artist.DATE_OF_BIRTH.asc())
                .having(Artist.DATE_OF_BIRTH.eq(dateFormat.parse("1/2/17")))
                .selectOne(context);

        assertEquals(dateFormat.parse("1/2/17"), result[0]);
        assertEquals(4L, result[1]);
    }

    @Test(expected = Exception.class)
    public void testHavingOnNonGroupByColumn() throws Exception {
        Property<String> nameSubstr = Property.create(substringExp(Artist.ARTIST_NAME.path(), 1, 6), String.class);
        Property<Long> count = Property.create(countExp(), Long.class);

        Object[] q = ColumnSelect.query(Artist.class, nameSubstr, count)
                .having(Artist.ARTIST_NAME.like("artist%"))
                .selectOne(context);
        assertEquals("artist", q[0]);
        assertEquals(20L, q[1]);
    }

    @Test
    public void testSelectRelationshipCount() throws Exception {
        Property<Long> paintingCount = Property.create(countExp(Artist.PAINTING_ARRAY.path()), Long.class);

        Object[] result = ColumnSelect.query(Artist.class)
                .columns(Artist.DATE_OF_BIRTH, paintingCount)
                .orderBy(Artist.DATE_OF_BIRTH.asc())
                .selectFirst(context);
        assertEquals(dateFormat.parse("1/1/17"), result[0]);
        assertEquals(4L, result[1]);
    }

    @Test
    public void testSelectHavingWithExpressionAlias() throws Exception {

        Property<String> nameSubstr = Property.create("name_substr", substringExp(Artist.ARTIST_NAME.path(), 1, 6), String.class);
        Property<Long> count = Property.create(countExp(), Long.class);

        Object[] q = null;
        try {
            q = ColumnSelect.query(Artist.class, nameSubstr, count)
                    .having(count.gt(10L))
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

        Property<String> nameSubstr = Property.create(substringExp(Artist.ARTIST_NAME.path(), 1, 6), String.class);
        Property<Long> count = Property.create(countExp(), Long.class);

        Object[] q = null;
        try {
            q = ColumnSelect.query(Artist.class, nameSubstr, count)
                    .having(count.gt(10L))
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
        Property<String> nameFirstLetter = Property.create(substringExp(Artist.ARTIST_NAME.path(), 1, 1), String.class);
        Property<String> nameSubstr = Property.create("name_substr", substringExp(Artist.ARTIST_NAME.path(), 1, 6), String.class);
        Property<Long> count = Property.create(countExp(), Long.class);

        Object[] q = null;
        try {
            q = ColumnSelect.query(Artist.class, nameSubstr, count)
                    .where(nameFirstLetter.eq("a"))
                    .having(count.gt(10L))
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
        Property<Long> paintingCount = Property.create(countExp(Artist.PAINTING_ARRAY.path()), Long.class);

        Object[] result = null;
        try {
            result = ColumnSelect.query(Artist.class)
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


}
