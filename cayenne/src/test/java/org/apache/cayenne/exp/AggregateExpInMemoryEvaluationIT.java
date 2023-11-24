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

import java.math.BigDecimal;
import java.sql.Types;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class AggregateExpInMemoryEvaluationIT extends RuntimeCase {

    // Format: d/m/YY
    private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);

    @Inject
    private DBHelper dbHelper;

    @Inject
    private DataContext context;

    @Before
    public void createArtistsDataSet() throws Exception {
        TableHelper tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");
        tArtist.setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.DATE);

        java.sql.Date[] dates = new java.sql.Date[5];
        for(int i=1; i<=5; i++) {
            dates[i-1] = new java.sql.Date(DATE_FORMAT.parse("1/" + i + "/17").getTime());
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
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc())
                .prefetch(Artist.PAINTING_ARRAY.disjoint())
                .select(context);

        Expression countExp = Artist.PAINTING_ARRAY.count().getExpression();

        for (Artist artist : artists) {
            assertEquals(artist.getPaintingArray().size(), countExp.evaluate(artist));
        }
    }

    @Test
    public void testMax() {
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc())
                .prefetch(Artist.PAINTING_ARRAY.disjoint())
                .select(context);

        Expression maxExp = Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE).max().getExpression();

        BigDecimal max0 = (BigDecimal) maxExp.evaluate(artists.get(0));
        BigDecimal expected0 = BigDecimal.valueOf(20000, 2);
        assertEquals(0, expected0.compareTo(max0));

        BigDecimal max1 = (BigDecimal) maxExp.evaluate(artists.get(1));
        BigDecimal expected1 = BigDecimal.valueOf(100000, 2);
        assertEquals(0, expected1.compareTo(max1));

        BigDecimal max4 = (BigDecimal) maxExp.evaluate(artists.get(4));
        BigDecimal expected4 = BigDecimal.valueOf(19000, 2);
        assertEquals(0, expected4.compareTo(max4));
    }

    @Test
    public void testMin() {
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc())
                .prefetch(Artist.PAINTING_ARRAY.disjoint())
                .select(context);

        Expression minExp = Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE).min().getExpression();

        BigDecimal min0 = (BigDecimal) minExp.evaluate(artists.get(0));
        BigDecimal expected0 = BigDecimal.valueOf(5000, 2);
        assertEquals(0, expected0.compareTo(min0));

        BigDecimal min3 = (BigDecimal) minExp.evaluate(artists.get(3));
        BigDecimal expected1 = BigDecimal.valueOf(3000, 2);
        assertEquals(0, expected1.compareTo(min3));

        BigDecimal min4 = (BigDecimal) minExp.evaluate(artists.get(4));
        BigDecimal expected4 = BigDecimal.valueOf(4000, 2);
        assertEquals(0, expected4.compareTo(min4));
    }

    @Test
    public void testAvg() {
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTING_ARRAY.disjoint())
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc())
                .select(context);

        Expression avgExp = Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE).avg().getExpression();

        Object avg0 = avgExp.evaluate(artists.get(0));
        assertEquals(125.0, avg0);

        Object avg2 = avgExp.evaluate(artists.get(2));
        assertEquals(95.0, avg2);
    }

}
