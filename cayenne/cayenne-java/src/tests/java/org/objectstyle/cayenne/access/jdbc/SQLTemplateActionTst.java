/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access.jdbc;

import java.sql.Connection;
import java.sql.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.access.DataContextTestBase;
import org.objectstyle.cayenne.access.MockOperationObserver;
import org.objectstyle.cayenne.access.QueryResult;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.query.SQLTemplate;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class SQLTemplateActionTst extends CayenneTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testProperties() throws Exception {
        DbAdapter adapter = new JdbcAdapter();
        SQLTemplate template = new SQLTemplate(Object.class, "AAAAA");
        SQLTemplateAction action = new SQLTemplateAction(template, adapter);
        assertSame(adapter, action.getAdapter());
        assertSame(template, action.getQuery());
    }

    public void testExecuteSelect() throws Exception {
        getAccessStack().createTestData(DataContextTestBase.class, "testArtists", null);

        String templateString = "SELECT * FROM ARTIST WHERE ARTIST_ID = #bind($id)";
        SQLTemplate template = new SQLTemplate(Object.class, templateString);
        getSQLTemplateBuilder().updateSQLTemplate(template);

        Map bindings = new HashMap();
        bindings.put("id", new Integer(33005));
        template.setParameters(bindings);

        DbAdapter adapter = getAccessStackAdapter().getAdapter();
        SQLTemplateAction plan = new SQLTemplateAction(template, adapter);

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

        assertEquals(bindings.get("id"), row.get("ARTIST_ID"));
        assertEquals("artist5", row.get("ARTIST_NAME"));
        assertTrue(row.containsKey("DATE_OF_BIRTH"));
    }

    public void testSelectUtilDate() throws Exception {
        getAccessStack().createTestData(DataContextTestBase.class, "testArtists", null);
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
        SQLTemplateAction plan = new SQLTemplateAction(template, adapter);

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
        getAccessStack().createTestData(DataContextTestBase.class, "testArtists", null);
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
        SQLTemplateAction plan = new SQLTemplateAction(template, adapter);

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
        getAccessStack().createTestData(DataContextTestBase.class, "testArtists", null);
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
        SQLTemplateAction action = new SQLTemplateAction(template, adapter);

        MockOperationObserver observer = new MockOperationObserver();
        Connection c = getConnection();

        try {
            action.performAction(c, observer);
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
        bindings.put("id", new Integer(1));
        bindings.put("name", "a1");
        bindings.put("dob", new Date(System.currentTimeMillis()));
        template.setParameters(bindings);

        SQLTemplateAction action = new SQLTemplateAction(
                template,
                getAccessStackAdapter().getAdapter());
        assertSame(getAccessStackAdapter().getAdapter(), action.getAdapter());

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
        getAccessStack().createTestData(DataContextTestBase.class, "testArtists", null);

        SQLTemplate template = new SQLTemplate(Object.class, "delete from ARTIST");

        SQLTemplateAction plan = new SQLTemplateAction(template, getAccessStackAdapter()
                .getAdapter());
        assertSame(getAccessStackAdapter().getAdapter(), plan.getAdapter());

        Connection c = getConnection();
        try {
            MockOperationObserver observer = new MockOperationObserver();
            plan.performAction(c, observer);

            int[] batches = observer.countsForQuery(template);
            assertNotNull(batches);
            assertEquals(1, batches.length);
            assertEquals(DataContextTestBase.artistCount, batches[0]);
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
        bindings1.put("id", new Integer(1));
        bindings1.put("name", "a1");
        bindings1.put("dob", new Date(System.currentTimeMillis()));

        Map bindings2 = new HashMap();
        bindings2.put("id", new Integer(33));
        bindings2.put("name", "a$$$$$");
        bindings2.put("dob", new Date(System.currentTimeMillis()));
        template.setParameters(new Map[] {
                bindings1, bindings2
        });

        SQLTemplateAction action = new SQLTemplateAction(
                template,
                getAccessStackAdapter().getAdapter());
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
                getAccessStackAdapter().getAdapter());

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
        getNode().performQueries(Collections.singleton(template), new QueryResult());
    }
}