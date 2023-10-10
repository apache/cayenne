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

import org.apache.cayenne.DataRow;
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
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class SelectByIdIteratedQueryIT extends ServerCase {

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
    public void queryWithButchIterator() {
        try (ResultBatchIterator<Painting> iterator = SelectById
                .query(Painting.class, Arrays.asList(1,2,3,4,5,6))
                .batchIterator(context, 3)){
            int count = 0;
            int paintingCounter = 0;
            while (iterator.hasNext()) {
                count++;
                List<Painting> paintingList = iterator.next();
                for (Painting painting : paintingList) {
                    paintingCounter++;
                    assertEquals("painting" + paintingCounter, painting.getPaintingTitle());
                }
            }
            assertEquals(2, count);
        }
    }

    @Test
    public void queryWithIterator() {
        try (ResultIterator<Painting> iterator = SelectById
                .query(Painting.class, Arrays.asList(1,2,3,4,5,6))
                .iterator(context)){
            int count = 0;
            int paintingCounter = 0;
            while (iterator.hasNextRow()) {
                count++;
                Painting painting = iterator.nextRow();
                paintingCounter++;
                assertEquals("painting" + paintingCounter, painting.getPaintingTitle());
            }
            assertEquals(6, count);
        }
    }

    @Test
    public void dataRowQueryWithBatchIterator() {
        try (ResultBatchIterator<?> iterator = SelectById
                .dataRowQuery(Painting.class, 1, 2,3,4,5,6)
                .batchIterator(context, 3)) {
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
            assertEquals(2, count);
        }
    }

    @Test
    public void dataRowQueryWithIterator() {
        try (ResultIterator<?> iterator = SelectById
                .dataRowQuery(Painting.class, 1, 2,3,4,5,6)
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
            assertEquals(6, count);
        }
    }

}
