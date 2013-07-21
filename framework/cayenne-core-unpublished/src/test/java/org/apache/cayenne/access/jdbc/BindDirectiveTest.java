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
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.MockOperationObserver;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.ServerCaseDataSourceFactory;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

/**
 * Tests BindDirective for passed null parameters and for not passed parameters
 */
@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class BindDirectiveTest extends ServerCase {

    @Inject
    private ServerCaseDataSourceFactory dataSourceFactory;

    @Inject
    private JdbcAdapter adapter;

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    @Inject
    private JdbcEventLogger logger;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
    }

    public void testBindTimestamp() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("id", new Integer(1));
        parameters.put("name", "ArtistWithDOB");
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2010, 2, 8);
        parameters.put("dob", new Timestamp(cal.getTime().getTime()));

        // without JDBC usage
        Map<String, ?> row = performInsertForParameters(parameters, false, 1);
        assertEquals(parameters.get("name"), row.get("ARTIST_NAME"));
        assertEquals(cal.getTime(), row.get("DATE_OF_BIRTH"));
        assertNotNull(row.get("DATE_OF_BIRTH"));
        assertEquals(Date.class, row.get("DATE_OF_BIRTH").getClass());
    }

    public void testBindSQLDate() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("id", new Integer(1));
        parameters.put("name", "ArtistWithDOB");
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2010, 2, 8);
        parameters.put("dob", new java.sql.Date(cal.getTime().getTime()));

        // without JDBC usage
        Map<String, ?> row = performInsertForParameters(parameters, false, 1);
        assertEquals(parameters.get("name"), row.get("ARTIST_NAME"));
        assertEquals(parameters.get("dob"), row.get("DATE_OF_BIRTH"));
        assertNotNull(row.get("DATE_OF_BIRTH"));
        assertEquals(Date.class, row.get("DATE_OF_BIRTH").getClass());
    }

    public void testBindUtilDate() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("id", new Integer(1));
        parameters.put("name", "ArtistWithDOB");
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2010, 2, 8);
        parameters.put("dob", cal.getTime());

        // without JDBC usage
        Map<String, ?> row = performInsertForParameters(parameters, false, 1);
        assertEquals(parameters.get("name"), row.get("ARTIST_NAME"));
        assertEquals(parameters.get("dob"), row.get("DATE_OF_BIRTH"));
        assertNotNull(row.get("DATE_OF_BIRTH"));
        assertEquals(Date.class, row.get("DATE_OF_BIRTH").getClass());
    }

    public void testBindingForCollection() throws Exception {

        // insert 3 artists
        for (int i = 1; i < 4; i++) {
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("id", new Long(i));
            parameters.put("name", "Artist" + i);
            performInsertForParameters(parameters, true, i);
        }

        // now select only with names: Artist1 and Artist3
        Set<String> artistNames = new HashSet<String>();
        artistNames.add("Artist1");
        artistNames.add("Artist3");
        String sql = "SELECT * FROM ARTIST WHERE ARTIST_NAME in (#bind($ARTISTNAMES))";
        SQLTemplate query = new SQLTemplate(Artist.class, sql);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("ARTISTNAMES", artistNames));
        List<DataRow> result = context.performQuery(query);
        assertEquals(2, result.size());
    }

    public void testBindForPassedNullParam() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("id", new Long(1));
        parameters.put("name", "ArtistWithoutDOB");
        // passing null in parameter
        parameters.put("dob", null);

        // without JDBC usage
        Map<String, ?> row = performInsertForParameters(parameters, false, 1);
        assertEquals(parameters.get("id"), row.get("ARTIST_ID"));
        assertEquals(parameters.get("name"), row.get("ARTIST_NAME"));
        assertEquals(parameters.get("dob"), row.get("DATE_OF_BIRTH"));
        assertNull(row.get("DATE_OF_BIRTH"));
    }

    public void testBindWithJDBCForPassedNullParam() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("id", new Long(1));
        parameters.put("name", "ArtistWithoutDOB");
        // passing null in parameter
        parameters.put("dob", null);

        // use JDBC
        Map<String, ?> row = performInsertForParameters(parameters, true, 1);
        assertEquals(parameters.get("id"), row.get("ARTIST_ID"));
        assertEquals(parameters.get("name"), row.get("ARTIST_NAME"));
        assertEquals(parameters.get("dob"), row.get("DATE_OF_BIRTH"));
        assertNull(row.get("DATE_OF_BIRTH"));
    }

    public void testBindForNotPassedParam() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("id", new Long(1));
        parameters.put("name", "ArtistWithoutDOB");
        // not passing parameter parameters.put("dob", not passed!);

        // without JDBC usage
        Map<String, ?> row = performInsertForParameters(parameters, false, 1);
        assertEquals(parameters.get("id"), row.get("ARTIST_ID"));
        assertEquals(parameters.get("name"), row.get("ARTIST_NAME"));
        // parameter should be passed as null
        assertNull(row.get("DATE_OF_BIRTH"));
    }

    public void testBindWithJDBCForNotPassedParam() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("id", new Long(1));
        parameters.put("name", "ArtistWithoutDOB");
        // not passing parameter parameters.put("dob", not passed!);

        // use JDBC
        Map<String, ?> row = performInsertForParameters(parameters, true, 1);
        assertEquals(parameters.get("id"), row.get("ARTIST_ID"));
        assertEquals(parameters.get("name"), row.get("ARTIST_NAME"));
        // parameter should be passed as null
        assertNull(row.get("DATE_OF_BIRTH"));
    }

    /**
     * Inserts row for given parameters
     * 
     * @return inserted row
     */
    private Map<String, ?> performInsertForParameters(
            Map<String, Object> parameters,
            boolean useJDBCType,
            int expectedRowCount) throws Exception {

        String templateString;
        if (useJDBCType) {
            templateString = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) "
                    + "VALUES (#bind($id), #bind($name), #bind($dob 'DATE'))";
        }
        else {
            templateString = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) "
                    + "VALUES (#bind($id), #bind($name), #bind($dob))";
        }
        SQLTemplate template = new SQLTemplate(Object.class, templateString);

        template.setParameters(parameters);

        SQLTemplateAction action = new SQLTemplateAction(
                template,
                adapter,
                context.getEntityResolver());

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

        SelectQuery query = new SelectQuery(Artist.class);
        query.setFetchingDataRows(true);

        List<DataRow> data = context.performQuery(query);
        assertEquals(expectedRowCount, data.size());
        return data.get(0);
    }
}
