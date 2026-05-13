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
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.apache.cayenne.exp.ExpressionFactory.*;
import static org.junit.jupiter.api.Assertions.*;

public class AggregateExpInMemoryEvaluationIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    // Format: d/m/YY
    private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);

    private DataContext context;

    @BeforeEach
    public void createArtistsDataSet() throws Exception {
        context = env.dataContext();
        TableHelper tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");
        tArtist.setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.DATE);

        java.sql.Date[] dates = new java.sql.Date[5];
        for(int i=1; i<=5; i++) {
            dates[i-1] = new java.sql.Date(DATE_FORMAT.parse("1/" + i + "/17").getTime());
        }
        for (int i = 1; i <= 20; i++) {
            tArtist.insert(i, "artist" + i, dates[i % 5]);
        }

        TableHelper tGallery = env.table("GALLERY", "GALLERY_ID", "GALLERY_NAME");
        tGallery.insert(1, "tate modern");

        TableHelper tPaintings = env.table("PAINTING", "PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "GALLERY_ID", "ESTIMATED_PRICE");
        for (int i = 1; i <= 20; i++) {
            tPaintings.insert(i, "painting" + i, i % 5 + 1, 1, i * 10);
        }
        tPaintings.insert(21, "painting21", 2, 1, 1000);
    }

    @AfterEach
    public void clearArtistsDataSet() throws Exception {
        for(String table : Arrays.asList("PAINTING", "ARTIST", "GALLERY")) {
            TableHelper tHelper = env.table(table);
            tHelper.deleteAll();
        }
    }

    @Test
    public void count() {
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
    public void max() {
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
    public void min() {
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
    public void avg() {
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

    @Test
    public void caseWhenFirstCondition() {
        Artist artist = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTING_ARRAY.disjoint())
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc())
                .selectFirst(context);

        Expression caseWhenFirstCondition = caseWhen(
                List.of(Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE).between(BigDecimal.ZERO, BigDecimal.valueOf(50)),
                        Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE).between(BigDecimal.valueOf(51), BigDecimal.valueOf(100))),
                List.of(Artist.ARTIST_NAME.getExpression(),
                        Artist.DATE_OF_BIRTH.getExpression()),
                Artist.ARTIST_ID_PK_PROPERTY.getExpression());

        Object resultFirstCondition = caseWhenFirstCondition.evaluate(artist);
        assertTrue(resultFirstCondition instanceof String);
        assertEquals("artist1", resultFirstCondition);
    }

    @Test
    public void caseWhenSecondCondition() {
        Artist artist = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTING_ARRAY.disjoint())
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc())
                .selectFirst(context);

        Expression caseWhenFirstCondition = caseWhen(
                List.of(Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE).between(BigDecimal.ZERO, BigDecimal.valueOf(49)),
                        Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE).between(BigDecimal.valueOf(51), BigDecimal.valueOf(100))),
                List.of(Artist.ARTIST_NAME.getExpression(),
                        Artist.DATE_OF_BIRTH.getExpression()),
                Artist.ARTIST_ID_PK_PROPERTY.getExpression());

        Object resultSecondCondition = caseWhenFirstCondition.evaluate(artist);
        assertTrue(resultSecondCondition instanceof Date);
    }

    @Test
    public void caseWhenDefaultCondition() {
        Artist artist = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTING_ARRAY.disjoint())
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc())
                .selectFirst(context);

        Expression caseWhenDefaultCondition = caseWhen(
                List.of(Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE).between(BigDecimal.ZERO, BigDecimal.valueOf(1)),
                        Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE).between(BigDecimal.valueOf(2), BigDecimal.valueOf(3))),
                List.of(Artist.ARTIST_NAME.getExpression(),
                        Artist.DATE_OF_BIRTH.getExpression()),
                Artist.ARTIST_ID_PK_PROPERTY.getExpression());

        Object resultDefaultCondition = caseWhenDefaultCondition.evaluate(artist);
        assertTrue(resultDefaultCondition instanceof Long);
        assertEquals(1L, (long)resultDefaultCondition);
    }

    @Test
    public void caseWhenNoResultNoDefault() {
        Artist artist = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTING_ARRAY.disjoint())
                .orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc())
                .selectFirst(context);

        Expression caseWhenNoResultNoDefault = caseWhen(
                List.of(Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE).between(BigDecimal.ZERO, BigDecimal.valueOf(1))),
                List.of(Artist.DATE_OF_BIRTH.getExpression())
        );

        Object resultNoResultNoDefault = caseWhenNoResultNoDefault.evaluate(artist);
        assertNull(resultNoResultNoDefault);
    }

}
