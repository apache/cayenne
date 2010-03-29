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

import java.sql.Connection;
import java.sql.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.art.Artist;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

/**
 * DataNode test cases.
 * 
 */
public class DataNodeQueriesTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testCreatePkSupportForMapEntities() throws Exception {
        getAccessStack().createPKSupport();

        DataNode node = getNode();

        DbEntity artistEnt = node.getEntityResolver().getDbEntity("ARTIST");
        assertNotNull(node.getAdapter().getPkGenerator().generatePk(
                node,
                artistEnt.getPrimaryKeys().iterator().next()));

        DbEntity exhibitEnt = node.getEntityResolver().getDbEntity("EXHIBIT");
        assertNotNull(node.getAdapter().getPkGenerator().generatePk(
                node,
                exhibitEnt.getPrimaryKeys().iterator().next()));
    }

    public void testPerfomQueriesSQLTemplate() throws Exception {
        String template = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) "
                + "VALUES (#bind($id), #bind($name), #bind($dob 'DATE'))";
        SQLTemplate query = new SQLTemplate(Object.class, template);

        Map bindings = new HashMap();
        bindings.put("id", 1l);
        bindings.put("name", "a1");
        bindings.put("dob", new Date(System.currentTimeMillis()));
        query.setParameters(bindings);

        MockOperationObserver observer = new MockOperationObserver();
        getNode().performQueries(Collections.singletonList((Query) query), observer);
        assertNotNull(observer.countsForQuery(query));
        assertEquals(1, observer.countsForQuery(query)[0]);

        // check the data
        MockOperationObserver checkObserver = new MockOperationObserver();
        SelectQuery checkQuery = new SelectQuery(Artist.class);
        getDomain().performQueries(Collections.singletonList(checkQuery), checkObserver);
        List data = checkObserver.rowsForQuery(checkQuery);
        assertEquals(1, data.size());
        Map row = (Map) data.get(0);
        assertEquals(bindings.get("id"), row.get("ARTIST_ID"));
        assertEquals(bindings.get("name"), row.get("ARTIST_NAME"));
        // to compare dates we need to create the binding correctly
        // assertEquals(bindings.get("dob"), row.get("DATE_OF_BIRTH"));
    }

    public void testPerfomQueriesSelectingSQLTemplate1() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        String template = "SELECT #result('ARTIST_ID' 'int') FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = new SQLTemplate(Object.class, template);

        MockOperationObserver observer = new MockOperationObserver();
        getNode().performQueries(Collections.singletonList((Query) query), observer);

        List data = observer.rowsForQuery(query);
        assertEquals(DataContextCase.artistCount, data.size());
        Map row = (Map) data.get(2);
        assertEquals(1, row.size());
        assertEquals(new Integer(33003), row.get("ARTIST_ID"));
    }

    public void testPerfomQueriesSelectingSQLTemplate2() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = new SQLTemplate(Object.class, template);
        getSQLTemplateBuilder().updateSQLTemplate(query);

        MockOperationObserver observer = new MockOperationObserver();
        getNode().performQueries(Collections.singletonList((Query) query), observer);

        List data = observer.rowsForQuery(query);
        assertEquals(DataContextCase.artistCount, data.size());
        Map row = (Map) data.get(2);
        assertEquals(3, row.size());

        Number id = (Number) row.get("ARTIST_ID");
        assertNotNull(id);
        assertEquals("Can't find ARTIST_ID: " + row, 33003, id.intValue());
    }

    public void testPerfomQueriesSelectingSQLTemplateAlias() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        String template = "SELECT #result('ARTIST_ID' 'int' 'A') FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = new SQLTemplate(Object.class, template);

        MockOperationObserver observer = new MockOperationObserver();
        getNode().performQueries(Collections.singletonList((Query) query), observer);

        List data = observer.rowsForQuery(query);
        assertEquals(DataContextCase.artistCount, data.size());
        Map row = (Map) data.get(2);
        assertEquals(1, row.size());
        assertEquals(new Integer(33003), row.get("A"));
    }

    public void testRunMultiLineSQLTemplateUNIX() throws Exception {
        String templateString = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME)"
                + "\n"
                + "VALUES (1, 'A')";
        runSQL(templateString);
    }

    public void testRunMultiLineSQLTemplateWindows() throws Exception {
        String templateString = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME)"
                + "\r\n"
                + "VALUES (1, 'A')";
        runSQL(templateString);
    }

    public void testRunMultiLineSQLTemplateMac() throws Exception {
        String templateString = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME)"
                + "\r"
                + "VALUES (1, 'A')";
        runSQL(templateString);
    }

    /**
     * Testing that SQLTemplate that is entered on multiple lines can be executed. CAY-269
     * shows that some databases are very picky about it.
     */
    private void runSQL(String templateString) throws Exception {
        SQLTemplate template = new SQLTemplate(Object.class, templateString);
        SQLAction action = getNode().getAdapter().getAction(template, getNode());

        Connection c = getConnection();
        try {
            action.performAction(c, new MockOperationObserver());
        }
        finally {
            c.close();
        }
    }

}
