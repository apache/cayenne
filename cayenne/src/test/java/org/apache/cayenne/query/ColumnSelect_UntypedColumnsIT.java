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
import java.util.Date;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.FunctionExpressionFactory;
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Columns declared without a Java type: the type and the column kind (scalar, entity, embeddable) are resolved
 * against the model when the query is executed.
 */
public class ColumnSelect_UntypedColumnsIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private DataContext context;

    @BeforeEach
    public void createDataSet() throws Exception {
        context = env.context();

        TableHelper tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");
        long dateBase = System.currentTimeMillis() - 10L * 24 * 3600 * 1000;
        for (int i = 1; i <= 4; i++) {
            tArtist.insert(i, "artist" + i, new java.sql.Date(dateBase + 24L * 3600 * 1000 * i));
        }

        TableHelper tGallery = env.table("GALLERY", "GALLERY_ID", "GALLERY_NAME");
        tGallery.insert(1, "tate modern");

        TableHelper tPaintings = env.table("PAINTING", "PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "GALLERY_ID",
                "ESTIMATED_PRICE");
        for (int i = 1; i <= 8; i++) {
            tPaintings.insert(i, "painting" + i, i % 4 + 1, 1, 10 * i);
        }
    }

    private static <T> BaseProperty<T> untyped(String path) {
        return PropertyFactory.createBase(path, null);
    }

    @Test
    public void attributeColumnsUseMappedTypes() {
        List<Object[]> rows = ObjectSelect.columnQuery(Artist.class, untyped("artistName"), untyped("dateOfBirth"))
                .orderBy(Artist.ARTIST_NAME.asc())
                .select(context);

        assertEquals(4, rows.size());
        assertEquals("artist1", rows.get(0)[0]);
        // "dateOfBirth" is mapped as java.util.Date; a driver would report java.sql.Date or Timestamp
        assertEquals(Date.class, rows.get(0)[1].getClass());
    }

    @Test
    public void singleAttributeColumn() {
        BaseProperty<BigDecimal> price = untyped("estimatedPrice");
        List<BigDecimal> prices = ObjectSelect.columnQuery(Painting.class, price)
                .orderBy(Painting.ESTIMATED_PRICE.asc())
                .select(context);

        assertEquals(8, prices.size());
        assertEquals(new BigDecimal("10"), prices.get(0).setScale(0));
    }

    @Test
    public void toOnePathIsAnEntityColumn() {
        List<Object[]> rows = ObjectSelect.columnQuery(Painting.class, untyped("paintingTitle"), untyped("toArtist"))
                .orderBy(Painting.PAINTING_TITLE.asc())
                .select(context);

        assertEquals(8, rows.size());
        assertEquals("painting1", rows.get(0)[0]);
        Artist artist = assertInstanceOf(Artist.class, rows.get(0)[1]);
        assertEquals("artist2", artist.getArtistName());
    }

    @Test
    public void toManyPathIsAFlatEntityColumn() {
        List<Object[]> rows = ObjectSelect.columnQuery(Artist.class, untyped("artistName"), untyped("paintingArray"))
                .where(Artist.ARTIST_NAME.eq("artist1"))
                .select(context);

        assertEquals(2, rows.size());
        for (Object[] row : rows) {
            assertEquals("artist1", row[0]);
            Painting painting = assertInstanceOf(Painting.class, row[1]);
            assertEquals("artist1", painting.getToArtist().getArtistName());
        }
    }

    @Test
    public void toManyPathWithNoRelatedObjects() throws Exception {
        env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME").insert(5, "artist5");

        // inner join: the root row is dropped
        List<Object[]> inner = ObjectSelect.columnQuery(Artist.class, untyped("artistName"), untyped("paintingArray"))
                .where(Artist.ARTIST_NAME.eq("artist5"))
                .select(context);
        assertEquals(0, inner.size());

        // outer join: the root row is preserved with a null related object
        List<Object[]> outer = ObjectSelect.columnQuery(Artist.class, untyped("artistName"), untyped("paintingArray+"))
                .where(Artist.ARTIST_NAME.eq("artist5"))
                .select(context);
        assertEquals(1, outer.size());
        assertEquals("artist5", outer.get(0)[0]);
        assertNull(outer.get(0)[1]);
    }

    @Test
    public void toOnePathWithNoRelatedObject() throws Exception {
        env.table("PAINTING", "PAINTING_ID", "PAINTING_TITLE").insert(9, "painting9");

        // unlike to-many, a to-one column is an outer join even with no "+": the root row is preserved
        List<Object[]> rows = ObjectSelect.columnQuery(Painting.class, untyped("paintingTitle"), untyped("toArtist"))
                .where(Painting.PAINTING_TITLE.eq("painting9"))
                .select(context);
        assertEquals(1, rows.size());
        assertEquals("painting9", rows.get(0)[0]);
        assertNull(rows.get(0)[1]);
    }

    @Test
    public void typedToManyPathIsAFlatEntityColumn() {
        List<Object[]> rows = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY)
                .where(Artist.ARTIST_NAME.eq("artist1"))
                .select(context);

        assertEquals(2, rows.size());
        for (Object[] row : rows) {
            assertEquals("artist1", row[0]);
            Painting painting = assertInstanceOf(Painting.class, row[1]);
            assertEquals("artist1", painting.getToArtist().getArtistName());
        }
    }

    @Test
    public void typedToManyPathIsRejectedAsASingleColumn() {
        // the declared result type would be a List<Painting> per row, that the query can't deliver
        assertThrows(CayenneRuntimeException.class, () -> ObjectSelect.columnQuery(Artist.class, Artist.PAINTING_ARRAY));
        assertThrows(CayenneRuntimeException.class, () -> ObjectSelect.query(Artist.class).column(Artist.PAINTING_ARRAY));

        List<Painting> paintings = ObjectSelect.columnQuery(Artist.class, Artist.PAINTING_ARRAY.flat())
                .where(Artist.ARTIST_NAME.eq("artist1"))
                .select(context);
        assertEquals(2, paintings.size());
    }

    @Test
    public void selfIsTheRootEntity() {
        BaseProperty<Object> self = PropertyFactory.createBase(ExpressionFactory.fullObjectExp(), null);
        List<Object[]> rows = ObjectSelect.columnQuery(Artist.class, self, untyped("artistName"))
                .orderBy(Artist.ARTIST_NAME.asc())
                .select(context);

        assertEquals(4, rows.size());
        Artist artist = assertInstanceOf(Artist.class, rows.get(0)[0]);
        assertEquals("artist1", artist.getArtistName());
        assertEquals("artist1", rows.get(0)[1]);
    }

    @Test
    public void aggregatesInferTypes() {
        BaseProperty<Object> count = PropertyFactory.createBase(FunctionExpressionFactory.countExp(), null);
        BaseProperty<Object> sum = PropertyFactory.createBase(
                FunctionExpressionFactory.sumExp(ExpressionFactory.pathExp("estimatedPrice")), null);
        BaseProperty<Object> max = PropertyFactory.createBase(
                FunctionExpressionFactory.maxExp(ExpressionFactory.pathExp("paintingTitle")), null);

        Object[] row = ObjectSelect.columnQuery(Painting.class, count, sum, max).selectFirst(context);

        assertEquals(8L, row[0]);
        assertEquals(new BigDecimal("360"), assertInstanceOf(BigDecimal.class, row[1]).setScale(0));
        assertEquals("painting8", row[2]);
    }

    @Test
    public void mixedTypedAndUntypedColumns() {
        List<Object[]> rows = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME, untyped("dateOfBirth"),
                        Artist.PAINTING_ARRAY.count())
                .orderBy(Artist.ARTIST_NAME.asc())
                .select(context);

        assertEquals(4, rows.size());
        assertEquals("artist1", rows.get(0)[0]);
        assertEquals(Date.class, rows.get(0)[1].getClass());
        assertEquals(2L, rows.get(0)[2]);
    }

    @Test
    public void resultSetMappingCarriesResolvedTypes() {
        ColumnSelect<Object[]> query = ObjectSelect.columnQuery(Painting.class,
                untyped("paintingTitle"),
                untyped("toArtist"),
                Painting.ESTIMATED_PRICE,
                PropertyFactory.createBase(FunctionExpressionFactory.countExp(), null),
                PropertyFactory.createBase(FunctionExpressionFactory.currentTimestamp(), null));

        List<ResultSegment> segments = query.getMetaData(context.getEntityResolver()).getResultSetMapping();
        assertEquals(5, segments.size());

        ScalarResultSegment title = assertInstanceOf(ScalarResultSegment.class, segments.get(0));
        assertEquals(String.class, title.type());

        EntityResultSegment artist = assertInstanceOf(EntityResultSegment.class, segments.get(1));
        assertEquals("Artist", artist.classDescriptor().getEntity().getName());

        ScalarResultSegment price = assertInstanceOf(ScalarResultSegment.class, segments.get(2));
        assertEquals(BigDecimal.class, price.type());

        ScalarResultSegment count = assertInstanceOf(ScalarResultSegment.class, segments.get(3));
        assertEquals(Long.class, count.type());

        // nothing to infer for a function with no path operand: the driver type is used
        ScalarResultSegment now = assertInstanceOf(ScalarResultSegment.class, segments.get(4));
        assertNull(now.type());
    }
}
