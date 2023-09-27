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

import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class IteratedQueryIT extends ServerCase {

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


    //SingleObjectConversation
    @Test
    public void test_prefetchWithBatchIterator() {
        Painting painting;
        try (ResultBatchIterator<Painting> iterator = ObjectSelect.query(Painting.class)
                .prefetch(Painting.TO_ARTIST.joint())
                .batchIterator(context, 5)) {
            painting = iterator.next().get(0);
        }
        assertTrue(painting instanceof Painting);
    }

    @Test
    public void test_prefetchWithIterator() {
        Painting painting;
        try (ResultIterator<Painting> iterator = ObjectSelect.query(Painting.class)
                .prefetch(Painting.TO_ARTIST.joint())
                .iterator(context)) {
            painting = iterator.nextRow();
        }
        assertTrue(painting instanceof Painting);
    }

    //SingleScalarConversationStrategy
    @Test
    public void test_ScalarQueryWithBatchIterator() {
        int id;
        try (ResultBatchIterator<Integer> iterator = SQLSelect.scalarQuery("SELECT PAINTING_ID FROM PAINTING ORDER BY PAINTING_ID",
                Integer.class).batchIterator(context, 2)) {
            id = iterator.next().get(0);
        }
        assertEquals(1, id);
    }

    //SingleScalarConversationStrategy
    @Test
    public void test_ScalarQueryWithIterator() {
        int id;
        try (ResultIterator<Integer> iterator = SQLSelect.scalarQuery("SELECT PAINTING_ID FROM PAINTING ORDER BY PAINTING_ID",
                Integer.class).iterator(context)) {
            id = iterator.nextRow();
        }
        assertEquals(1, id);
    }

    //MapperConversationStrategy
    //MixedConversationStrategy
    @Test
    public void test_MappingWithBatchIterator() {
        try (ResultBatchIterator<DTO> batchIterator = ObjectSelect.columnQuery(Painting.class, Painting.PAINTING_TITLE, Painting.ESTIMATED_PRICE)
                .map(this::toDto)
                .batchIterator(context, 2)) {

            assertTrue(batchIterator.iterator().next().get(0) instanceof DTO);
        }
        context.commitChanges();
    }

    //MapperConversationStrategy
    //MixedConversationStrategy
    @Test
    public void test_MappingWithIterator() {
        try (ResultIterator<DTO> iterator = ObjectSelect.columnQuery(Painting.class, Painting.PAINTING_TITLE, Painting.ESTIMATED_PRICE)
                .map(this::toDto)
                .iterator(context)) {

            assertTrue(iterator.nextRow() instanceof DTO);
        }
        context.commitChanges();
    }


    //SingleObjectConversation
    @Test
    public void UnclosedTransactionsTest() {
        for (int i = 0; i < 5; i++) {
            test_MappingWithBatchIterator();
        }
    }


    DTO toDto(Object[] data) {
        return new DTO(data);
    }


    static class DTO {
        private final String title;
        private final Long estimatedPrice;

        public DTO(Object[] data) {
            this.title = (String) data[0];
            this.estimatedPrice = ((Number) data[1]).longValue();
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
