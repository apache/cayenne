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

import java.sql.Types;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.exp.property.NumericProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.apache.cayenne.exp.FunctionExpressionFactory.*;
import static org.junit.Assert.assertEquals;

/**
 * @since 4.0
 */
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ObjectSelect_AggregateIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Inject
    private UnitDbAdapter dbAdapter;

    // Format: d/m/YY
    DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);

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
        tPaintings.setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "GALLERY_ID", "ESTIMATED_PRICE");
        for (int i = 1; i <= 20; i++) {
            tPaintings.insert(i, "painting" + i, i % 5 + 1, 1, i * 10);
        }
        tPaintings.insert(21, "painting21", 2, 1, 1000);
    }

    @After
    public void clearArtistsDataSet() throws Exception {
        for(String table : Arrays.asList("PAINTING", "ARTIST", "GALLERY")) {
            TableHelper tHelper = new TableHelper(dbHelper, table);
            tHelper.deleteAll();
        }
    }

    @Test
    public void testCount() {
        long count = ObjectSelect.query(Artist.class)
                .column(PropertyFactory.COUNT)
                .selectOne(context);
        assertEquals(20L, count);
    }

    @Test
    public void testCountDistinct() throws Exception {
    	List<Artist> artists = ObjectSelect.query(Artist.class).select(context);
    	for (Artist artist : artists) {
			artist.setArtistName("Duplicate");
		}
    	context.commitChanges();
    	
        NumericProperty<Long> countDistinctProp = Artist.ARTIST_NAME.countDistinct();

        long count = ObjectSelect.query(Artist.class)
                .column(countDistinctProp)
                .selectOne(context);
        assertEquals(1L, count);
    }

    @Test
    @Ignore("Not all databases support AVG(DATE) aggregation")
    public void testAvg() throws Exception {
        BaseProperty<Date> avgProp = PropertyFactory.createBase(avgExp(Artist.DATE_OF_BIRTH.getExpression()), Date.class);

        Date avg = ObjectSelect.query(Artist.class)
                .column(avgProp)
                .selectOne(context);
        Date date = dateFormat.parse("1/3/17");
        assertEquals(date, avg);
    }

    @Test
    public void testMin() throws Exception {
        Date avg = ObjectSelect.query(Artist.class)
                .column(Artist.DATE_OF_BIRTH.min())
                .selectOne(context);
        Date date = dateFormat.parse("1/1/17");
        assertEquals(date, avg);
    }

    @Test
    public void testMax() throws Exception {
        Date avg = ObjectSelect.query(Artist.class)
                .column(Artist.DATE_OF_BIRTH.max())
                .selectOne(context);
        Date date = dateFormat.parse("1/5/17");
        assertEquals(date, avg);
    }

    @Test
    public void testCountGroupBy() throws Exception {
        List<Object[]> count = ObjectSelect.query(Artist.class)
                .columns(Artist.ARTIST_NAME.count(), Artist.DATE_OF_BIRTH)
                .orderBy(Artist.DATE_OF_BIRTH.asc())
                .select(context);
        Date date = dateFormat.parse("1/2/17");
        assertEquals(5L, count.size());
        assertEquals(4L, count.get(1)[0]);
        assertEquals(date, count.get(1)[1]);
    }

    @Test
    public void testGroupByOp() throws Exception {
        if(!dbAdapter.supportsExpressionInHaving()) {
            return;
        }
        List<Object[]> count = ObjectSelect.query(Artist.class)
                .columns(
                        Artist.ARTIST_NAME.count(),
                        Artist.DATE_OF_BIRTH.dayOfMonth().add(1).alias("op")
                )
                .orderBy(Artist.DATE_OF_BIRTH.asc())
                .select(context);
        assertEquals(5L, count.size());
        assertEquals(4L, count.get(1)[0]);
    }

    @Test
    public void testSelectRelationshipCount() throws Exception {
        long count = ObjectSelect.query(Artist.class)
                .column(Artist.PAINTING_ARRAY.count())
                .where(Artist.ARTIST_NAME.eq("artist1"))
                .selectOne(context);
        assertEquals(4L, count);
    }

    @Test
    public void testSelectRelationshipCountWithAnotherField() throws Exception {
        Object[] result = ObjectSelect.query(Artist.class)
                .columns(Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.count())
                .where(Artist.ARTIST_NAME.eq("artist1"))
                .selectOne(context);
        assertEquals("artist1", result[0]);
        assertEquals(4L, result[1]);
    }

    @Test
    public void testOrderByCount() {
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .orderBy(Artist.PAINTING_ARRAY.outer().count().desc())
                .prefetch(Artist.PAINTING_ARRAY.disjoint())
                .select(context);

        assertEquals(20, artists.size());

        assertEquals(5, artists.get(0).getPaintingArray().size());
        assertEquals("artist2", artists.get(0).getArtistName());
        assertEquals(4, artists.get(1).getPaintingArray().size());
        assertEquals(4, artists.get(2).getPaintingArray().size());

        assertEquals(0, artists.get(17).getPaintingArray().size());
        assertEquals(0, artists.get(18).getPaintingArray().size());
        assertEquals(0, artists.get(19).getPaintingArray().size());
    }

    @Test
    public void testOrderByAvg() {
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .orderBy(Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE).avg().asc())
                .prefetch(Artist.PAINTING_ARRAY.disjoint())
                .select(context);

        assertEquals(5, artists.size());

        assertEquals("artist3", artists.get(0).getArtistName());
        assertEquals("artist4", artists.get(1).getArtistName());
        assertEquals("artist5", artists.get(2).getArtistName());
        assertEquals("artist1", artists.get(3).getArtistName());
        assertEquals("artist2", artists.get(4).getArtistName());

    }
}