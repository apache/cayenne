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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ObjectSelectIteratedQueryIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tPainting;

    private TableHelper tArtist;


    @Before
    public void before() throws Exception {
        tPainting = new TableHelper(dbHelper, "PAINTING")
                .setColumns("PAINTING_ID", "PAINTING_TITLE", "ESTIMATED_PRICE", "ARTIST_ID")
                .setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.DECIMAL, Types.INTEGER);

        tArtist = new TableHelper(dbHelper, "ARTIST")
                .setColumns("ARTIST_ID", "ARTIST_NAME");

        createArtistDataSet();
        createPaintingsDataSet();

    }

    private void createPaintingsDataSet() throws Exception {
        for (int i = 1; i <= 20; i++) {
            tPainting.insert(i, "painting" + i, 10000. * i, 1);
        }
    }

    private void createArtistDataSet() throws SQLException {
        tArtist.insert(1, "Test1");
        tArtist.insert(2, "Test2");
    }

    @Test
    public void prefetchWithBatchIterator() {
        try (ResultBatchIterator<Painting> iterator = ObjectSelect
                .query(Painting.class)
                .prefetch(Painting.TO_ARTIST.joint())
                .orderBy(Painting.PAINTING_ID_PK_PROPERTY.asc())
                .batchIterator(context, 10)) {
            int count = 0;
            while (iterator.hasNext()) {
                count++;
                List<Painting> paintings = iterator.next();
                for (Painting painting : paintings) {
                    //noinspection ConstantConditions
                    assertTrue(painting instanceof Painting);
                    assertEquals("Test1", painting.getToArtist().readPropertyDirectly("artistName"));
                }
            }
            assertEquals(2, count);
        }
    }

    @Test
    public void queryPrefetchJointWithIterator() {
        try (ResultIterator<Painting> iterator = ObjectSelect
                .query(Painting.class)
                .prefetch(Painting.TO_ARTIST.joint())
                .orderBy(Painting.PAINTING_ID_PK_PROPERTY.asc())
                .iterator(context)) {
            int count = 0;
            while (iterator.hasNextRow()) {
                count++;
                Painting painting = iterator.nextRow();
                //noinspection ConstantConditions
                assertTrue(painting instanceof Painting);
                assertEquals("Test1", painting.getToArtist().readPropertyDirectly("artistName"));
            }
            assertEquals(20, count);
        }
    }

    @Test(expected = CayenneRuntimeException.class)
    public void queryPrefetchDisjointWithIterator() {
        try (ResultIterator<Painting> iterator = ObjectSelect
                .query(Painting.class)
                .prefetch(Painting.TO_ARTIST.disjoint())
                .iterator(context)) {
            iterator.nextRow();
        }
    }


    @Test(expected = CayenneRuntimeException.class)
    public void queryPrefetchDisjointWithBatchIterator() {
        try (ResultBatchIterator<Painting> iterator = ObjectSelect
                .query(Painting.class)
                .prefetch(Painting.TO_ARTIST.disjoint())
                .batchIterator(context, 3)) {
            iterator.next();
        }
    }

    @Ignore
    @Test(expected = CayenneRuntimeException.class)
    public void queryPaginationWithBatchIterator() {
        try (ResultBatchIterator<Painting> iterator = ObjectSelect
                .query(Painting.class)
                .pageSize(2)
                .batchIterator(context, 3)) {
            iterator.next();
        }
    }

    @Test
    public void queryPrefetchDisjointByIdWithBIterator() {
        try (ResultIterator<Painting> iterator = ObjectSelect
                .query(Painting.class)
                .prefetch(Painting.TO_ARTIST.disjointById())
                .orderBy(Painting.PAINTING_ID_PK_PROPERTY.asc())
                .iterator(context)) {
            int count = 0;
            while (iterator.hasNextRow()) {
                count++;
                Painting painting = iterator.nextRow();
                //noinspection ConstantConditions
                assertTrue(painting instanceof Painting);
                assertEquals("Test1", painting.getToArtist().readPropertyDirectly("artistName"));
            }
            assertEquals(20, count);
        }
    }

    @Test
    public void queryPrefetchJointWithBatchIterator() {
        try (ResultBatchIterator<Painting> iterator = ObjectSelect
                .query(Painting.class, "Painting")
                .prefetch(Painting.TO_ARTIST.joint())
                .orderBy(Painting.PAINTING_ID_PK_PROPERTY.asc())
                .batchIterator(context, 5)) {
            int count = 0;
            int paintingCounter = 0;
            while (iterator.hasNext()) {
                count++;
                List<Painting> paintingList = iterator.next();
                for (Painting painting : paintingList) {
                    paintingCounter++;
                    assertEquals("painting" + paintingCounter, painting.getPaintingTitle());
                    assertEquals("Test1", painting.getToArtist().readPropertyDirectly("artistName"));
                }
            }
            assertEquals(4, count);
        }
    }

    @Test
    public void QueryWithIterator() {
        try (ResultIterator<Painting> iterator = ObjectSelect
                .query(Painting.class, "Painting")
                .prefetch(Painting.TO_ARTIST.joint())
                .orderBy(Painting.PAINTING_ID_PK_PROPERTY.asc())
                .iterator(context)) {
            int count = 0;
            int paintingCounter = 0;
            while (iterator.hasNextRow()) {
                count++;
                Painting painting = iterator.nextRow();
                paintingCounter++;
                assertEquals("painting" + paintingCounter, painting.getPaintingTitle());
                assertEquals("Test1", painting.getToArtist().readPropertyDirectly("artistName"));
            }
            assertEquals(20, count);
        }
    }

    @Test
    public void mappingWithBatchIterator() {
        try (ResultBatchIterator<DTO> iterator = ObjectSelect
                .columnQuery(Painting.class, Painting.PAINTING_TITLE, Painting.ESTIMATED_PRICE)
                .orderBy(Painting.PAINTING_ID_PK_PROPERTY.asc())
                .map(this::toDto)
                .batchIterator(context, 5)) {
            int count = 0;
            while (iterator.hasNext()) {
                count++;
                List<DTO> dtos = iterator.next();
                for (DTO dto : dtos) {
                    //noinspection ConstantConditions
                    assertTrue(dto instanceof DTO);
                    assertTrue(dto.getTitle().contains("dto_painting"));
                }
            }
            assertEquals(5, iterator.getBatchSize());
            assertEquals(4, count);
        }
    }

    @Test
    public void mappingWithIterator() {
        try (ResultIterator<DTO> iterator = ObjectSelect
                .columnQuery(Painting.class, Painting.PAINTING_TITLE, Painting.ESTIMATED_PRICE)
                .orderBy(Painting.PAINTING_ID_PK_PROPERTY.asc())
                .map(this::toDto)
                .iterator(context)) {
            int count = 0;
            while (iterator.hasNextRow()) {
                count++;
                DTO dto = iterator.nextRow();
                //noinspection ConstantConditions
                assertTrue(dto instanceof DTO);
                assertTrue(dto.getTitle().contains("dto_painting"));
            }
            assertEquals(20, count);
        }
    }

    @Test
    public void dataRowQueryWithBatchIterator() {
        try (ResultBatchIterator<?> iterator = ObjectSelect
                .dataRowQuery(Painting.class)
                .orderBy(Painting.PAINTING_ID_PK_PROPERTY.asc())
                .batchIterator(context, 5)) {
            int count = 0;
            int paintingCounter = 0;
            while (iterator.hasNext()) {
                count++;
                List<?> rows = iterator.next();
                for (Object row : rows) {
                    paintingCounter++;
                    Painting painting = context.objectFromDataRow(Painting.class, (DataRow) row);
                    assertEquals("painting" + paintingCounter, painting.getPaintingTitle());
                }
            }
            assertEquals(4, count);
        }
    }

    @Test
    public void dataRowQueryWithIterator() {
        try (ResultIterator<?> iterator = ObjectSelect
                .dataRowQuery(Painting.class)
                .orderBy(Painting.PAINTING_ID_PK_PROPERTY.asc())
                .iterator(context)) {
            int count = 0;
            int paintingCounter = 0;
            while (iterator.hasNextRow()) {
                count++;
                paintingCounter++;
                Object row = iterator.nextRow();
                Painting painting = context.objectFromDataRow(Painting.class, (DataRow) row);
                assertEquals("painting" + paintingCounter, painting.getPaintingTitle());
            }
            assertEquals(20, count);
        }
    }

    @Test
    public void dbQueryWithIterator() {
        try (ResultIterator<?> iterator = ObjectSelect
                .dbQuery("PAINTING")
                .orderBy("db:" + Painting.PAINTING_ID_PK_COLUMN)
                .iterator(context)) {
            int count = 0;
            int paintingCounter = 0;
            while (iterator.hasNextRow()) {
                count++;
                paintingCounter++;
                Object row = iterator.nextRow();
                Painting painting = context.objectFromDataRow(Painting.class, (DataRow) row);
                assertEquals("painting" + paintingCounter, painting.getPaintingTitle());
            }
            assertEquals(20, count);
        }
    }

    @Test
    public void dbQueryWithBatchIterator() {
        try (ResultBatchIterator<?> iterator = ObjectSelect
                .dbQuery("PAINTING")
                .orderBy("db:" + Painting.PAINTING_ID_PK_COLUMN)
                .batchIterator(context, 5)) {
            int count = 0;
            int paintingCounter = 0;
            while (iterator.hasNext()) {
                count++;
                List<?> rows = iterator.next();
                for (Object row : rows) {
                    paintingCounter++;
                    Painting painting = context.objectFromDataRow(Painting.class, (DataRow) row);
                    assertEquals("painting" + paintingCounter, painting.getPaintingTitle());
                }
            }
            assertEquals(4, count);
        }
    }


    @Test
    public void unclosedTransactionsTest() {
        for (int i = 0; i < 10; i++) {
            mappingWithBatchIterator();
        }
    }

    DTO toDto(Object[] data) {
        return new DTO(data);
    }


    static class DTO {
        private final String title;
        private final Long estimatedPrice;

        public DTO(Object[] data) {
            this.title = "dto_" + data[0];
            this.estimatedPrice = ((Number) data[1]).longValue();
        }

        public String getTitle() {
            return title;
        }

        public Long getEstimatedPrice() {
            return estimatedPrice;
        }

        @Override
        public String toString() {
            return "DTO{" +
                    "title='" + title + '\'' +
                    ", estimatedPrice=" + estimatedPrice +
                    '}';
        }
    }
}
