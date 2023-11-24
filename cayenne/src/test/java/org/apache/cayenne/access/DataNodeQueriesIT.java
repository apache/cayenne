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

package org.apache.cayenne.access;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.apache.cayenne.unit.util.SQLTemplateCustomizer;
import org.junit.Before;
import org.junit.Test;

import java.sql.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataNodeQueriesIT extends RuntimeCase {

    @Inject
    protected DataNode node;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected SQLTemplateCustomizer sqlTemplateCustomizer;

    protected TableHelper tArtist;

    @Before
    public void setUp() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");
    }

    protected void createFourArtists() throws Exception {
        tArtist.insert(11, "artist2");
        tArtist.insert(101, "artist3");
        tArtist.insert(201, "artist4");
        tArtist.insert(3001, "artist5");
    }

    @Test
    public void testCreatePkSupportForMapEntities() throws Exception {

        DbEntity artistEnt = node.getEntityResolver().getDbEntity("ARTIST");
        assertNotNull(node.getAdapter().getPkGenerator().generatePk(
                node,
                artistEnt.getPrimaryKeys().iterator().next()));

        DbEntity exhibitEnt = node.getEntityResolver().getDbEntity("EXHIBIT");
        assertNotNull(node.getAdapter().getPkGenerator().generatePk(
                node,
                exhibitEnt.getPrimaryKeys().iterator().next()));
    }

    @Test
    public void testPerfomQueriesSQLTemplate() throws Exception {
        String template = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) "
                + "VALUES (#bind($id), #bind($name), #bind($dob 'DATE'))";
        SQLTemplate query = new SQLTemplate(Object.class, template);

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("id", 1L);
        bindings.put("name", "a1");
        bindings.put("dob", new Date(System.currentTimeMillis()));
        query.setParameters(bindings);

        MockOperationObserver observer = new MockOperationObserver();
        node.performQueries(Collections.singletonList((Query) query), observer);
        assertNotNull(observer.countsForQuery(query));
        assertEquals(1, observer.countsForQuery(query)[0]);

        // check the data

        assertEquals(1, tArtist.getRowCount());
        assertEquals(1L, tArtist.getLong("ARTIST_ID"));
        assertEquals("a1", tArtist.getString("ARTIST_NAME").trim());
    }

    @Test
    public void testPerfomQueriesSelectingSQLTemplate1() throws Exception {
        createFourArtists();

        String template = "SELECT #result('ARTIST_ID' 'int') FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = new SQLTemplate(Object.class, template);

        MockOperationObserver observer = new MockOperationObserver();
        node.performQueries(Collections.singletonList((Query) query), observer);

        List<DataRow> data = observer.rowsForQuery(query);
        assertEquals(4, data.size());
        DataRow row = data.get(2);
        assertEquals(1, row.size());
        assertEquals(201, row.get("ARTIST_ID"));
    }

    @Test
    public void testPerfomQueriesSelectingSQLTemplate2() throws Exception {
        createFourArtists();

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = new SQLTemplate(Object.class, template);
        sqlTemplateCustomizer.updateSQLTemplate(query);

        MockOperationObserver observer = new MockOperationObserver();
        node.performQueries(Collections.singletonList((Query) query), observer);

        List<DataRow> data = observer.rowsForQuery(query);
        assertEquals(4, data.size());
        DataRow row = data.get(2);
        assertEquals(3, row.size());

        Number id = (Number) row.get("ARTIST_ID");
        assertNotNull(id);
        assertEquals(201, id.intValue());
    }

    @Test
    public void testPerfomQueriesSelectingSQLTemplateAlias() throws Exception {
        createFourArtists();

        String template = "SELECT #result('ARTIST_ID' 'int' 'A') FROM ARTIST ORDER BY ARTIST_ID";
        Query query = new SQLTemplate(Object.class, template);

        MockOperationObserver observer = new MockOperationObserver();
        node.performQueries(Collections.singletonList(query), observer);

        List<DataRow> data = observer.rowsForQuery(query);
        assertEquals(4, data.size());
        DataRow row = data.get(2);
        assertEquals(1, row.size());
        assertEquals(201, row.get("A"));
    }

    @Test
    public void testRunMultiLineSQLTemplateUNIX() throws Exception {
        String templateString = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME)"
                + "\n"
                + "VALUES (1, 'A')";
        Query template = new SQLTemplate(Object.class, templateString);
        node.performQueries(
                Collections.singletonList(template),
                new MockOperationObserver());
    }

    @Test
    public void testRunMultiLineSQLTemplateWindows() throws Exception {
        String templateString = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME)"
                + "\r\n"
                + "VALUES (1, 'A')";
        Query template = new SQLTemplate(Object.class, templateString);
        node.performQueries(
                Collections.singletonList(template),
                new MockOperationObserver());
    }

    @Test
    public void testRunMultiLineSQLTemplateMac() throws Exception {
        String templateString = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME)"
                + "\r"
                + "VALUES (1, 'A')";
        Query template = new SQLTemplate(Object.class, templateString);
        node.performQueries(
                Collections.singletonList(template),
                new MockOperationObserver());
    }
}
