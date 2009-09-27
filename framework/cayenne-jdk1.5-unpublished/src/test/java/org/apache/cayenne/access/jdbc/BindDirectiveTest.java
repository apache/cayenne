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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.art.Artist;
import org.apache.cayenne.access.MockOperationObserver;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

/**
 * Tests BindDirective for passed null parameters and for not passed parameters
 */
public class BindDirectiveTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testBindForPassedNullParam() throws Exception {
        Map parameters = new HashMap();
        parameters.put("id", new Long(1));
        parameters.put("name", "ArtistWithoutDOB");
        // passing null in parameter
        parameters.put("dob", null);

        // without JDBC usage
        Map row = performInsertForParameters(parameters, false);
        assertEquals(parameters.get("id"), row.get("ARTIST_ID"));
        assertEquals(parameters.get("name"), row.get("ARTIST_NAME"));
        assertEquals(parameters.get("dob"), row.get("DATE_OF_BIRTH"));
        assertNull(row.get("DATE_OF_BIRTH"));
    }

    public void testBindWithJDBCForPassedNullParam() throws Exception {
        Map parameters = new HashMap();
        parameters.put("id", new Long(1));
        parameters.put("name", "ArtistWithoutDOB");
        // passing null in parameter
        parameters.put("dob", null);

        // use JDBC
        Map row = performInsertForParameters(parameters, true);
        assertEquals(parameters.get("id"), row.get("ARTIST_ID"));
        assertEquals(parameters.get("name"), row.get("ARTIST_NAME"));
        assertEquals(parameters.get("dob"), row.get("DATE_OF_BIRTH"));
        assertNull(row.get("DATE_OF_BIRTH"));
    }

    public void testBindForNotPassedParam() throws Exception {
        Map parameters = new HashMap();
        parameters.put("id", new Long(1));
        parameters.put("name", "ArtistWithoutDOB");
        // not passing parameter parameters.put("dob", not passed!);

        // without JDBC usage
        Map row = performInsertForParameters(parameters, false);
        assertEquals(parameters.get("id"), row.get("ARTIST_ID"));
        assertEquals(parameters.get("name"), row.get("ARTIST_NAME"));
        // parameter should be passed as null
        assertNull(row.get("DATE_OF_BIRTH"));
    }

    public void testBindWithJDBCForNotPassedParam() throws Exception {
        Map parameters = new HashMap();
        parameters.put("id", new Long(1));
        parameters.put("name", "ArtistWithoutDOB");
        // not passing parameter parameters.put("dob", not passed!);

        // use JDBC
        Map row = performInsertForParameters(parameters, true);
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
    private Map performInsertForParameters(Map parameters, boolean useJDBCType)
            throws Exception {
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
                getAccessStackAdapter().getAdapter(), getDomain().getEntityResolver());
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
        return row;
    }
}
