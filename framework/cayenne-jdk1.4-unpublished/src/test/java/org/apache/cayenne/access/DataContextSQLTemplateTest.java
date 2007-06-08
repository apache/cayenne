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

package org.apache.cayenne.access;

import java.util.List;
import java.util.Map;

import org.apache.art.Artist;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.CayenneCase;

/**
 * @author Andrus Adamchik
 */
public class DataContextSQLTemplateTest extends CayenneCase {

    protected DataContext context;

    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
        context = createDataContext();
    }

    public void testColumnNamesCapitalization() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = new SQLTemplate(Artist.class, template);
        query.setColumnNamesCapitalization(SQLTemplate.LOWERCASE_COLUMN_NAMES);
        query.setFetchingDataRows(true);

        List rows = context.performQuery(query);

        DataRow row1 = (DataRow) rows.get(0);
        assertFalse(row1.containsKey("ARTIST_ID"));
        assertTrue(row1.containsKey("artist_id"));

        DataRow row2 = (DataRow) rows.get(1);
        assertFalse(row2.containsKey("ARTIST_ID"));
        assertTrue(row2.containsKey("artist_id"));
    
        query.setColumnNamesCapitalization(SQLTemplate.UPPERCASE_COLUMN_NAMES);
        
        List rowsUpper = context.performQuery(query);

        DataRow row3 = (DataRow) rowsUpper.get(0);
        assertFalse(row3.containsKey("artist_id"));
        assertTrue(row3.containsKey("ARTIST_ID"));

        DataRow row4 = (DataRow) rowsUpper.get(1);
        assertFalse(row4.containsKey("artist_id"));
        assertTrue(row4.containsKey("ARTIST_ID"));
    }

    public void testFetchDataRows() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = new SQLTemplate(Artist.class, template);

        getSQLTemplateBuilder().updateSQLTemplate(query);

        query.setFetchingDataRows(true);

        List rows = context.performQuery(query);
        assertEquals(DataContextCase.artistCount, rows.size());
        assertTrue(
                "Expected DataRow, got this: " + rows.get(1),
                rows.get(1) instanceof DataRow);

        DataRow row2 = (DataRow) rows.get(1);
        assertEquals(3, row2.size());
        assertEquals(new Integer(33002), row2.get("ARTIST_ID"));
    }

    public void testFetchObjects() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = getSQLTemplateBuilder().createSQLTemplate(
                Artist.class,
                template);

        query.setFetchingDataRows(false);

        List objects = context.performQuery(query);
        assertEquals(DataContextCase.artistCount, objects.size());
        assertTrue(objects.get(1) instanceof Artist);

        Artist artist2 = (Artist) objects.get(1);
        assertEquals("artist2", artist2.getArtistName());
    }

    public void testFetchLimit() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        int fetchLimit = 3;

        // sanity check
        assertTrue(fetchLimit < DataContextCase.artistCount);
        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = getSQLTemplateBuilder().createSQLTemplate(
                Artist.class,
                template);
        query.setFetchLimit(fetchLimit);

        List objects = context.performQuery(query);
        assertEquals(fetchLimit, objects.size());
        assertTrue(objects.get(0) instanceof Artist);
    }

    public void testPageSize() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        int pageSize = 3;

        // sanity check
        assertTrue(pageSize < DataContextCase.artistCount);

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = getSQLTemplateBuilder().createSQLTemplate(
                Artist.class,
                template);

        query.setPageSize(pageSize);

        List objects = context.performQuery(query);
        assertEquals(DataContextCase.artistCount, objects.size());
        assertTrue(objects.get(0) instanceof Artist);

        assertTrue(objects instanceof IncrementalFaultList);
        IncrementalFaultList pagedList = (IncrementalFaultList) objects;
        assertEquals(DataContextCase.artistCount - pageSize, pagedList
                .getUnfetchedObjects());

        // check if we can resolve subsequent pages
        Artist artist = (Artist) objects.get(pageSize);

        int expectUnresolved = DataContextCase.artistCount - pageSize - pageSize;
        if (expectUnresolved < 0) {
            expectUnresolved = 0;
        }
        assertEquals(expectUnresolved, pagedList.getUnfetchedObjects());
        assertEquals("artist" + (pageSize + 1), artist.getArtistName());
    }

    public void testIteratedQuery() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = getSQLTemplateBuilder().createSQLTemplate(
                Artist.class,
                template);

        ResultIterator it = context.performIteratedQuery(query);

        try {
            int i = 0;

            while (it.hasNextRow()) {
                i++;

                Map row = it.nextDataRow();
                assertEquals(3, row.size());
                assertEquals(new Integer(33000 + i), row.get("ARTIST_ID"));
            }

            assertEquals(DataContextCase.artistCount, i);
        }
        finally {
            it.close();
        }
    }

    public void testQueryWithLineBreakAfterMacroCAY726() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        // see CAY-726 for details
        String template = "SELECT #result('count(*)' 'int' 'X')"
                + System.getProperty("line.separator")
                + "FROM ARTIST";
        SQLTemplate query = getSQLTemplateBuilder().createSQLTemplate(
                Artist.class,
                template);
        query.setFetchingDataRows(true);

        List result = context.performQuery(query);

        assertEquals(new Integer(25), ((Map) result.get(0)).get("X"));
    }
}
