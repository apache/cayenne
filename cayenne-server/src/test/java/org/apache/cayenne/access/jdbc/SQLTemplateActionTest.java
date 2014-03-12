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

package org.apache.cayenne.access.jdbc;

import static org.mockito.Mockito.mock;

import java.sql.Connection;
import java.sql.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.MockOperationObserver;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.ServerCaseDataSourceFactory;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.unit.util.SQLTemplateCustomizer;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class SQLTemplateActionTest extends ServerCase {

    @Inject
    protected ServerCaseDataSourceFactory dataSourceFactory;

    @Inject
    protected DataNode node;

    @Inject
    protected JdbcAdapter adapter;

    @Inject
    protected ObjectContext objectContext;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected SQLTemplateCustomizer sqlTemplateCustomizer;

    protected TableHelper tArtist;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");

        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");
    }

    protected void createFourArtists() throws Exception {

        Date date = new Date(System.currentTimeMillis());

        tArtist.insert(11, "artist2", date);
        tArtist.insert(101, "artist3", date);
        tArtist.insert(201, "artist4", date);
        tArtist.insert(3001, "artist5", date);
    }

    public void testProperties() throws Exception {
        SQLTemplate template = new SQLTemplate(Object.class, "AAAAA");
        SQLTemplateAction action = new SQLTemplateAction(template, adapter, objectContext.getEntityResolver(),
                mock(RowReaderFactory.class));
        assertSame(adapter, action.getAdapter());
        assertSame(template, action.getQuery());
    }

    public void testExecuteSelect() throws Exception {
        createFourArtists();

        String templateString = "SELECT * FROM ARTIST WHERE ARTIST_ID = #bind($id)";
        SQLTemplate template = new SQLTemplate(Object.class, templateString);
        sqlTemplateCustomizer.updateSQLTemplate(template);

        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put("id", 201l);
        template.setParameters(bindings);

        // must ensure the right SQLTemplateAction is created

        SQLAction plan = adapter.getAction(template, node);
        assertTrue(plan instanceof SQLTemplateAction);

        MockOperationObserver observer = new MockOperationObserver();
        Connection c = dataSourceFactory.getSharedDataSource().getConnection();

        try {
            plan.performAction(c, observer);
        }
        finally {
            c.close();
        }

        List<DataRow> rows = observer.rowsForQuery(template);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        DataRow row = rows.get(0);

        // In the absence of ObjEntity most DB's return a Long here, except for Oracle
        // that has no BIGINT type and
        // returns BigDecimal, so do a Number comparison
        Number id = (Number) row.get("ARTIST_ID");
        assertNotNull(id);
        assertEquals(((Number) bindings.get("id")).longValue(), id.longValue());
        assertEquals("artist4", row.get("ARTIST_NAME"));
        assertTrue(row.containsKey("DATE_OF_BIRTH"));
    }

    public void testSelectUtilDate() throws Exception {
        createFourArtists();

        String templateString = "SELECT #result('DATE_OF_BIRTH' 'java.util.Date' 'DOB') "
                + "FROM ARTIST WHERE ARTIST_ID = #bind($id)";
        SQLTemplate template = new SQLTemplate(Object.class, templateString);
        sqlTemplateCustomizer.updateSQLTemplate(template);

        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put("id", 101);
        template.setParameters(bindings);

        SQLAction plan = adapter.getAction(template, node);

        MockOperationObserver observer = new MockOperationObserver();
        Connection c = dataSourceFactory.getSharedDataSource().getConnection();

        try {
            plan.performAction(c, observer);
        }
        finally {
            c.close();
        }

        List<DataRow> rows = observer.rowsForQuery(template);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        DataRow row = rows.get(0);

        assertNotNull(row.get("DOB"));
        assertEquals(java.util.Date.class, row.get("DOB").getClass());
    }

    public void testSelectSQLDate() throws Exception {
        createFourArtists();

        String templateString = "SELECT #result('DATE_OF_BIRTH' 'java.sql.Date' 'DOB') "
                + "FROM ARTIST WHERE ARTIST_ID = #bind($id)";
        SQLTemplate template = new SQLTemplate(Object.class, templateString);
        sqlTemplateCustomizer.updateSQLTemplate(template);

        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put("id", 101);
        template.setParameters(bindings);

        SQLAction plan = adapter.getAction(template, node);

        MockOperationObserver observer = new MockOperationObserver();
        Connection c = dataSourceFactory.getSharedDataSource().getConnection();

        try {
            plan.performAction(c, observer);
        }
        finally {
            c.close();
        }

        List<DataRow> rows = observer.rowsForQuery(template);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        DataRow row = rows.get(0);

        assertNotNull(row.get("DOB"));
        assertEquals(java.sql.Date.class, row.get("DOB").getClass());
    }

    public void testSelectSQLTimestamp() throws Exception {
        createFourArtists();

        String templateString = "SELECT #result('DATE_OF_BIRTH' 'java.sql.Timestamp' 'DOB') "
                + "FROM ARTIST WHERE ARTIST_ID = #bind($id)";
        SQLTemplate template = new SQLTemplate(Object.class, templateString);
        sqlTemplateCustomizer.updateSQLTemplate(template);

        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put("id", 201);
        template.setParameters(bindings);

        SQLAction plan = adapter.getAction(template, node);

        MockOperationObserver observer = new MockOperationObserver();
        Connection c = dataSourceFactory.getSharedDataSource().getConnection();

        try {
            plan.performAction(c, observer);
        }
        finally {
            c.close();
        }

        List<DataRow> rows = observer.rowsForQuery(template);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        DataRow row = rows.get(0);

        assertNotNull(row.get("DOB"));
        // Sybase returns a Timestamp subclass... so can't test equality
        assertTrue(java.sql.Timestamp.class.isAssignableFrom(row.get("DOB").getClass()));
    }

    public void testExecuteUpdate() throws Exception {
        String templateString = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) "
                + "VALUES (#bind($id), #bind($name), #bind($dob 'DATE'))";
        SQLTemplate template = new SQLTemplate(Object.class, templateString);

        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put("id", new Long(1));
        bindings.put("name", "a1");
        bindings.put("dob", new Date(System.currentTimeMillis()));
        template.setParameters(bindings);

        SQLAction action = adapter.getAction(template, node);

        Connection c = dataSourceFactory.getSharedDataSource().getConnection();
        try {
            MockOperationObserver observer = new MockOperationObserver();
            action.performAction(c, observer);

            int[] batches = observer.countsForQuery(template);
            assertNotNull(batches);
            assertEquals(1, batches.length);
            assertEquals(1, batches[0]);
        }
        finally {
            c.close();
        }

        assertEquals(1, tArtist.getRowCount());
        assertEquals(1l, tArtist.getLong("ARTIST_ID"));
        assertEquals("a1", tArtist.getString("ARTIST_NAME").trim());
    }

    public void testExecuteUpdateNoParameters() throws Exception {
        createFourArtists();

        SQLTemplate template = new SQLTemplate(
                Object.class,
                "delete from ARTIST where ARTIST_NAME like 'a%'");

        SQLAction action = adapter.getAction(template, node);

        Connection c = dataSourceFactory.getSharedDataSource().getConnection();
        try {
            MockOperationObserver observer = new MockOperationObserver();
            action.performAction(c, observer);

            int[] batches = observer.countsForQuery(template);
            assertNotNull(batches);
            assertEquals(1, batches.length);
            assertEquals(4, batches[0]);
        }
        finally {
            c.close();
        }
    }

    public void testExecuteUpdateBatch() throws Exception {
        String templateString = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) "
                + "VALUES (#bind($id), #bind($name), #bind($dob 'DATE'))";
        SQLTemplate template = new SQLTemplate(Object.class, templateString);

        Map<String, Object> bindings1 = new HashMap<String, Object>();
        bindings1.put("id", new Long(1));
        bindings1.put("name", "a1");
        bindings1.put("dob", new Date(System.currentTimeMillis()));

        Map<String, Object> bindings2 = new HashMap<String, Object>();
        bindings2.put("id", new Long(33));
        bindings2.put("name", "a$$$$$");
        bindings2.put("dob", new Date(System.currentTimeMillis()));
        template.setParameters(new Map[] {
                bindings1, bindings2
        });

        SQLAction genericAction = adapter.getAction(template, node);
        assertTrue(genericAction instanceof SQLTemplateAction);
        SQLTemplateAction action = (SQLTemplateAction) genericAction;

        assertSame(adapter, action.getAdapter());
        assertSame(template, action.getQuery());

        Connection c = dataSourceFactory.getSharedDataSource().getConnection();
        try {
            MockOperationObserver observer = new MockOperationObserver();
            action.performAction(c, observer);

            int[] batches = observer.countsForQuery(template);
            assertNotNull(batches);
            assertEquals(2, batches.length);
            assertEquals(1, batches[0]);
            assertEquals(1, batches[1]);
        }
        finally {
            c.close();
        }

        MockOperationObserver observer = new MockOperationObserver();
        SelectQuery query = new SelectQuery(Artist.class);
        query.addOrdering("db:ARTIST_ID", SortOrder.ASCENDING);
        node.performQueries(Collections.singletonList((Query) query), observer);

        List<DataRow> data = observer.rowsForQuery(query);
        assertEquals(2, data.size());
        DataRow row1 = data.get(0);
        assertEquals(bindings1.get("id"), row1.get("ARTIST_ID"));
        assertEquals(bindings1.get("name"), row1.get("ARTIST_NAME"));
        // to compare dates we need to create the binding correctly
        // assertEquals(bindings1.get("dob"), row.get("DATE_OF_BIRTH"));

        DataRow row2 = data.get(1);
        assertEquals(bindings2.get("id"), row2.get("ARTIST_ID"));
        assertEquals(bindings2.get("name"), row2.get("ARTIST_NAME"));
        // to compare dates we need to create the binding correctly
        // assertEquals(bindings2.get("dob"), row2.get("DATE_OF_BIRTH"));
    }

    public void testExtractTemplateString() throws Exception {
        SQLTemplate template = new SQLTemplate(Artist.class, "A\nBC");
        SQLTemplateAction action = new SQLTemplateAction(template, adapter, objectContext.getEntityResolver(),
                mock(RowReaderFactory.class));

        assertEquals("A BC", action.extractTemplateString());
    }

}
