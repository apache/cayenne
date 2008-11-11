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

import java.sql.Connection;
import java.sql.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.art.Artist;
import org.apache.cayenne.access.DataContextCase;
import org.apache.cayenne.access.MockOperationObserver;
import org.apache.cayenne.access.QueryResult;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class SQLTemplateActionTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testProperties() throws Exception {
        DbAdapter adapter = new JdbcAdapter();
        SQLTemplate template = new SQLTemplate(Object.class, "AAAAA");
        SQLTemplateAction action = new SQLTemplateAction(template, adapter, getDomain()
                .getEntityResolver());
        assertSame(adapter, action.getAdapter());
        assertSame(template, action.getQuery());
    }

    public void testExecuteSelect() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        String templateString = "SELECT * FROM ARTIST WHERE ARTIST_ID = #bind($id)";
        SQLTemplate template = new SQLTemplate(Object.class, templateString);
        getSQLTemplateBuilder().updateSQLTemplate(template);

        Map bindings = new HashMap();
        bindings.put("id", new Long(33005l));
        template.setParameters(bindings);

        // must ensure the right SQLTemplateAction is created
        DbAdapter adapter = getAccessStackAdapter().getAdapter();
        SQLAction plan = adapter.getAction(template, getNode());
        assertTrue(plan instanceof SQLTemplateAction);

        MockOperationObserver observer = new MockOperationObserver();
        Connection c = getConnection();

        try {
            plan.performAction(c, observer);
        }
        finally {
            c.close();
        }

        List rows = observer.rowsForQuery(template);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        Map row = (Map) rows.get(0);

        // In the absence of ObjEntity most DB's return a Long here, except for Oracle
        // that has no BIGINT type and
        // returns BigDecimal, so do a Number comparison
        Number id = (Number) row.get("ARTIST_ID");
        assertNotNull(id);
        assertEquals(((Number) bindings.get("id")).longValue(), id.longValue());
        assertEquals("artist5", row.get("ARTIST_NAME"));
        assertTrue(row.containsKey("DATE_OF_BIRTH"));
    }

    public void testSelectUtilDate() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);
        // update data set to include dates....
        setDate(new java.util.Date(), 33006);

        String templateString = "SELECT #result('DATE_OF_BIRTH' 'java.util.Date' 'DOB') "
                + "FROM ARTIST WHERE ARTIST_ID = #bind($id)";
        SQLTemplate template = new SQLTemplate(Object.class, templateString);
        getSQLTemplateBuilder().updateSQLTemplate(template);

        Map bindings = new HashMap();
        bindings.put("id", new Integer(33006));
        template.setParameters(bindings);

        DbAdapter adapter = getAccessStackAdapter().getAdapter();
        SQLAction plan = adapter.getAction(template, getNode());

        MockOperationObserver observer = new MockOperationObserver();
        Connection c = getConnection();

        try {
            plan.performAction(c, observer);
        }
        finally {
            c.close();
        }

        List rows = observer.rowsForQuery(template);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        Map row = (Map) rows.get(0);

        assertNotNull(row.get("DOB"));
        assertEquals(java.util.Date.class, row.get("DOB").getClass());
    }

    public void testSelectSQLDate() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);
        // update data set to include dates....
        setDate(new java.util.Date(), 33006);

        String templateString = "SELECT #result('DATE_OF_BIRTH' 'java.sql.Date' 'DOB') "
                + "FROM ARTIST WHERE ARTIST_ID = #bind($id)";
        SQLTemplate template = new SQLTemplate(Object.class, templateString);
        getSQLTemplateBuilder().updateSQLTemplate(template);

        Map bindings = new HashMap();
        bindings.put("id", new Integer(33006));
        template.setParameters(bindings);

        DbAdapter adapter = getAccessStackAdapter().getAdapter();
        SQLAction plan = adapter.getAction(template, getNode());

        MockOperationObserver observer = new MockOperationObserver();
        Connection c = getConnection();

        try {
            plan.performAction(c, observer);
        }
        finally {
            c.close();
        }

        List rows = observer.rowsForQuery(template);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        Map row = (Map) rows.get(0);

        assertNotNull(row.get("DOB"));
        assertEquals(java.sql.Date.class, row.get("DOB").getClass());
    }

    public void testSelectSQLTimestamp() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);
        // update data set to include dates....
        setDate(new java.util.Date(), 33006);

        String templateString = "SELECT #result('DATE_OF_BIRTH' 'java.sql.Timestamp' 'DOB') "
                + "FROM ARTIST WHERE ARTIST_ID = #bind($id)";
        SQLTemplate template = new SQLTemplate(Object.class, templateString);
        getSQLTemplateBuilder().updateSQLTemplate(template);

        Map bindings = new HashMap();
        bindings.put("id", new Integer(33006));
        template.setParameters(bindings);

        DbAdapter adapter = getAccessStackAdapter().getAdapter();
        SQLAction plan = adapter.getAction(template, getNode());

        MockOperationObserver observer = new MockOperationObserver();
        Connection c = getConnection();

        try {
            plan.performAction(c, observer);
        }
        finally {
            c.close();
        }

        List rows = observer.rowsForQuery(template);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        Map row = (Map) rows.get(0);

        assertNotNull(row.get("DOB"));
        // Sybase returns a Timestamp subclass... so can't test equality
        assertTrue(java.sql.Timestamp.class.isAssignableFrom(row.get("DOB").getClass()));
    }

    public void testExecuteUpdate() throws Exception {
        String templateString = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) "
                + "VALUES (#bind($id), #bind($name), #bind($dob 'DATE'))";
        SQLTemplate template = new SQLTemplate(Object.class, templateString);

        Map bindings = new HashMap();
        bindings.put("id", new Long(1));
        bindings.put("name", "a1");
        bindings.put("dob", new Date(System.currentTimeMillis()));
        template.setParameters(bindings);

        DbAdapter adapter = getAccessStackAdapter().getAdapter();
        SQLAction action = adapter.getAction(template, getNode());

        Connection c = getConnection();
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

        MockOperationObserver observer = new MockOperationObserver();
        SelectQuery query = new SelectQuery(Artist.class);
        getDomain().performQueries(Collections.singletonList(query), observer);

        List data = observer.rowsForQuery(query);
        assertEquals(1, data.size());
        Map row = (Map) data.get(0);
        assertEquals(bindings.get("id"), row.get("ARTIST_ID"));
        assertEquals(bindings.get("name"), row.get("ARTIST_NAME"));
        // to compare dates we need to create the binding correctly
        // assertEquals(bindings.get("dob"), row.get("DATE_OF_BIRTH"));
    }

    public void testExecuteUpdateNoParameters() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        SQLTemplate template = new SQLTemplate(
                Object.class,
                "delete from ARTIST where ARTIST_NAME like 'a%'");

        DbAdapter adapter = getAccessStackAdapter().getAdapter();
        SQLAction action = adapter.getAction(template, getNode());

        Connection c = getConnection();
        try {
            MockOperationObserver observer = new MockOperationObserver();
            action.performAction(c, observer);

            int[] batches = observer.countsForQuery(template);
            assertNotNull(batches);
            assertEquals(1, batches.length);
            assertEquals(DataContextCase.artistCount, batches[0]);
        }
        finally {
            c.close();
        }
    }

    public void testExecuteUpdateBatch() throws Exception {
        String templateString = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) "
                + "VALUES (#bind($id), #bind($name), #bind($dob 'DATE'))";
        SQLTemplate template = new SQLTemplate(Object.class, templateString);

        Map bindings1 = new HashMap();
        bindings1.put("id", new Long(1));
        bindings1.put("name", "a1");
        bindings1.put("dob", new Date(System.currentTimeMillis()));

        Map bindings2 = new HashMap();
        bindings2.put("id", new Long(33));
        bindings2.put("name", "a$$$$$");
        bindings2.put("dob", new Date(System.currentTimeMillis()));
        template.setParameters(new Map[] {
                bindings1, bindings2
        });

        DbAdapter adapter = getAccessStackAdapter().getAdapter();
        SQLAction genericAction = adapter.getAction(template, getNode());
        assertTrue(genericAction instanceof SQLTemplateAction);
        SQLTemplateAction action = (SQLTemplateAction) genericAction;

        assertSame(getAccessStackAdapter().getAdapter(), action.getAdapter());
        assertSame(template, action.getQuery());

        Connection c = getConnection();
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
        query.addOrdering("db:ARTIST_ID", true);
        getDomain().performQueries(Collections.singletonList(query), observer);

        List data = observer.rowsForQuery(query);
        assertEquals(2, data.size());
        Map row1 = (Map) data.get(0);
        assertEquals(bindings1.get("id"), row1.get("ARTIST_ID"));
        assertEquals(bindings1.get("name"), row1.get("ARTIST_NAME"));
        // to compare dates we need to create the binding correctly
        // assertEquals(bindings1.get("dob"), row.get("DATE_OF_BIRTH"));

        Map row2 = (Map) data.get(1);
        assertEquals(bindings2.get("id"), row2.get("ARTIST_ID"));
        assertEquals(bindings2.get("name"), row2.get("ARTIST_NAME"));
        // to compare dates we need to create the binding correctly
        // assertEquals(bindings2.get("dob"), row2.get("DATE_OF_BIRTH"));
    }

    public void testExtractTemplateString() throws Exception {
        SQLTemplate template = new SQLTemplate(Artist.class, "A\nBC");
        SQLTemplateAction action = new SQLTemplateAction(
                template,
                getAccessStackAdapter().getAdapter(),
                getDomain().getEntityResolver());

        assertEquals("A BC", action.extractTemplateString());
    }

    private void setDate(java.util.Date date, int artistId) {
        String templateString = "UPDATE ARTIST SET DATE_OF_BIRTH #bindEqual($date 'DATE') "
                + "WHERE ARTIST_ID = #bind($id)";
        SQLTemplate template = new SQLTemplate(Object.class, templateString);

        Map map = new HashMap();
        map.put("date", date);
        map.put("id", new Integer(artistId));

        template.setParameters(map);
        getNode().performQueries(
                Collections.singleton((Query) template),
                new QueryResult());
    }
}
