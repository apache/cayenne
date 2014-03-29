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

import java.util.List;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class SQLSelectTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
    }

    protected void createArtistsDataSet() throws Exception {
        TableHelper tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");

        long dateBase = System.currentTimeMillis();

        for (int i = 1; i <= 20; i++) {
            tArtist.insert(i, "artist" + i, new java.sql.Date(dateBase + 10000 * i));
        }
    }

    public void test_DataRows_DataMapNameRoot() throws Exception {

        createArtistsDataSet();

        SQLSelect<DataRow> q1 = SQLSelect.dataRowQuery("tstmap", "SELECT * FROM ARTIST");
        assertTrue(q1.isFetchingDataRows());

        List<DataRow> result = context.select(q1);
        assertEquals(20, result.size());
        assertTrue(result.get(0) instanceof DataRow);
    }

    public void test_DataRows_DefaultRoot() throws Exception {

        createArtistsDataSet();

        SQLSelect<DataRow> q1 = SQLSelect.dataRowQuery("SELECT * FROM ARTIST");
        assertTrue(q1.isFetchingDataRows());

        List<DataRow> result = context.select(q1);
        assertEquals(20, result.size());
        assertTrue(result.get(0) instanceof DataRow);
    }

    public void test_DataRows_ClassRoot() throws Exception {

        createArtistsDataSet();

        SQLSelect<Artist> q1 = SQLSelect.query(Artist.class, "SELECT * FROM ARTIST");
        assertFalse(q1.isFetchingDataRows());
        List<Artist> result = context.select(q1);
        assertEquals(20, result.size());
        assertTrue(result.get(0) instanceof Artist);
    }

    public void test_DataRows_ClassRoot_Parameters() throws Exception {

        createArtistsDataSet();

        SQLSelect<Artist> q1 = SQLSelect.query(Artist.class, "SELECT * FROM ARTIST WHERE ARTIST_NAME = #bind($a)");
        q1.getParameters().put("a", "artist3");

        assertFalse(q1.isFetchingDataRows());
        Artist a = context.selectOne(q1);
        assertEquals("artist3", a.getArtistName());
    }

    public void test_DataRows_ClassRoot_Bind() throws Exception {

        createArtistsDataSet();

        SQLSelect<Artist> q1 = SQLSelect.query(Artist.class,
                "SELECT * FROM ARTIST WHERE ARTIST_NAME = #bind($a) OR ARTIST_NAME = #bind($b)");
        q1.bind("a", "artist3").bind("b", "artist4");

        List<Artist> result = context.select(q1);
        assertEquals(2, result.size());
    }

    public void test_DataRows_ColumnNameCaps() throws Exception {

        SQLSelect<DataRow> q1 = SQLSelect.dataRowQuery("SELECT * FROM ARTIST WHERE ARTIST_NAME = 'artist2'");
        q1.upperColumnNames();

        SQLTemplate r1 = (SQLTemplate) q1.getReplacementQuery(context.getEntityResolver());
        assertEquals(CapsStrategy.UPPER, r1.getColumnNamesCapitalization());

        q1.lowerColumnNames();
        SQLTemplate r2 = (SQLTemplate) q1.getReplacementQuery(context.getEntityResolver());
        assertEquals(CapsStrategy.LOWER, r2.getColumnNamesCapitalization());
    }

    public void test_DataRows_FetchLimit() throws Exception {

        createArtistsDataSet();

        SQLSelect<DataRow> q1 = SQLSelect.dataRowQuery("SELECT * FROM ARTIST");
        q1.limit(5);

        assertEquals(5, context.select(q1).size());
    }

    public void test_DataRows_FetchOffset() throws Exception {

        createArtistsDataSet();

        SQLSelect<DataRow> q1 = SQLSelect.dataRowQuery("SELECT * FROM ARTIST");
        q1.offset(4);

        assertEquals(16, context.select(q1).size());
    }

    public void test_Append() throws Exception {

        createArtistsDataSet();

        SQLSelect<Artist> q1 = SQLSelect.query(Artist.class, "SELECT * FROM ARTIST")
                .append(" WHERE ARTIST_NAME = #bind($a)").bind("a", "artist3");

        List<Artist> result = context.select(q1);
        assertEquals(1, result.size());
    }

    public void test_Select() throws Exception {

        createArtistsDataSet();

        List<Artist> result = SQLSelect.query(Artist.class, "SELECT * FROM ARTIST WHERE ARTIST_NAME = #bind($a)")
                .bind("a", "artist3").select(context);

        assertEquals(1, result.size());
    }

    public void test_SelectOne() throws Exception {

        createArtistsDataSet();

        Artist a = SQLSelect.query(Artist.class, "SELECT * FROM ARTIST WHERE ARTIST_NAME = #bind($a)")
                .bind("a", "artist3").selectOne(context);

        assertEquals("artist3", a.getArtistName());
    }

    public void test_SelectLong() throws Exception {

        createArtistsDataSet();

        long id = SQLSelect.scalarQuery(Long.class, "SELECT ARTIST_ID FROM ARTIST WHERE ARTIST_NAME = #bind($a)")
                .bind("a", "artist3").selectOne(context);

        assertEquals(3l, id);
    }

    public void test_SelectLongArray() throws Exception {

        createArtistsDataSet();

        List<Long> ids = SQLSelect.scalarQuery(Long.class, "SELECT ARTIST_ID FROM ARTIST ORDER BY ARTIST_ID").select(
                context);

        assertEquals(20, ids.size());
        assertEquals(2l, ids.get(1).longValue());
    }

    public void test_SelectCount() throws Exception {

        createArtistsDataSet();

        int c = SQLSelect.scalarQuery(Integer.class, "SELECT #result('COUNT(*)' 'int') FROM ARTIST").selectOne(context);

        assertEquals(20, c);
    }
}
