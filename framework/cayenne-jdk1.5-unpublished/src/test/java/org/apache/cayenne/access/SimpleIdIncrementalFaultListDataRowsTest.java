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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.art.Artist;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

/**
 * Tests IncrementalFaultList behavior when fetching data rows.
 * 
 */
public class SimpleIdIncrementalFaultListDataRowsTest extends CayenneCase {

    protected SimpleIdIncrementalFaultList list;
    protected Query query;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        createTestData("testArtists");

        SelectQuery q = new SelectQuery("Artist");
        q.setPageSize(6);
        q.setFetchingDataRows(true);
        q.addOrdering("db:ARTIST_ID", Ordering.ASC);

        query = q;
        list = new SimpleIdIncrementalFaultList(super.createDataContext(), query);
    }

    public void testGet1() throws Exception {
        assertEquals(1, list.idWidth);
        assertFalse(list.elements.get(0) instanceof Map);
        assertTrue(list.elements.get(19) instanceof Long);

        Object a = list.get(19);

        assertNotNull(a);
        assertTrue(a instanceof Map);
        assertEquals(3, ((Map) a).size());
        assertEquals("artist20", ((Map) a).get("ARTIST_NAME"));
    }

    public void testIndexOf1() throws Exception {
        DataContext parallelContext = createDataContext();

        Expression qual = ExpressionFactory.matchExp("artistName", "artist20");
        SelectQuery query = new SelectQuery(Artist.class, qual);
        query.setFetchingDataRows(true);
        List artists = parallelContext.performQuery(query);

        assertEquals(1, artists.size());

        Map row = (Map) artists.get(0);
        assertEquals(19, list.indexOf(row));
        
        DataRow clone = new DataRow(row);
        assertEquals(19, list.indexOf(clone));

        row.remove("ARTIST_ID");
        assertEquals(-1, list.indexOf(row));
    }

    public void testIndexOf2() throws Exception {
        DataContext parallelContext = createDataContext();
        
        // resolve first page
        list.get(0);

        Expression qual = ExpressionFactory.matchExp("artistName", "artist2");
        SelectQuery query = new SelectQuery(Artist.class, qual);
        query.setFetchingDataRows(true);
        List artists = parallelContext.performQuery(query);

        assertEquals(1, artists.size());

        Map row = (Map) artists.get(0);
        assertEquals(1, list.indexOf(row));

        row.remove("ARTIST_NAME");
        assertEquals(-1, list.indexOf(row));
    }

    public void testLastIndexOf1() throws Exception {
        DataContext parallelContext = createDataContext();
       
        // resolve first page
        list.get(0);

        Expression qual = ExpressionFactory.matchExp("artistName", "artist3");
        SelectQuery query = new SelectQuery(Artist.class, qual);
        query.setFetchingDataRows(true);
        List artists = parallelContext.performQuery(query);

        assertEquals(1, artists.size());

        Map row = (Map) artists.get(0);
        assertEquals(2, list.lastIndexOf(row));

        row.remove("ARTIST_NAME");
        assertEquals(-1, list.lastIndexOf(row));
    }

    public void testLastIndexOf2() throws Exception {
        DataContext parallelContext = createDataContext();

        Expression qual = ExpressionFactory.matchExp("artistName", "artist20");
        SelectQuery query = new SelectQuery(Artist.class, qual);
        query.setFetchingDataRows(true);
        List artists = parallelContext.performQuery(query);

        assertEquals(1, artists.size());

        Map row = (Map) artists.get(0);
        assertEquals(19, list.lastIndexOf(row));

        row.remove("ARTIST_ID");
        assertEquals(-1, list.lastIndexOf(row));
    }

    public void testIterator() throws Exception {
        assertEquals(1, list.idWidth);

        Iterator it = list.iterator();
        int counter = 0;
        while (it.hasNext()) {
            Object obj = it.next();
            assertNotNull(obj);
            assertTrue(
                    "Unexpected object class: " + obj.getClass().getName(),
                    obj instanceof Map);
            assertEquals(3, ((Map) obj).size());

            // iterator must be resolved page by page
            int expectedResolved = list.pageIndex(counter)
                    * list.getPageSize()
                    + list.getPageSize();
            if (expectedResolved > list.size()) {
                expectedResolved = list.size();
            }

            assertEquals(list.size() - expectedResolved, list.getUnfetchedObjects());

            if (list.getUnfetchedObjects() >= list.getPageSize()) {
                assertTrue(list.elements.get(list.size() - 1) instanceof Long);
            }

            counter++;
        }
    }
}
