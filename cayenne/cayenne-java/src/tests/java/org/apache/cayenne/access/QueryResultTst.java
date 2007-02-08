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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.apache.cayenne.query.DeleteQuery;
import org.apache.cayenne.query.MockQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.UpdateQuery;

/**
 * @deprecated since 1.2
 * @author Andrei Adamchik
 */
public class QueryResultTst extends TestCase {

    protected QueryResult result;
    protected Query[] queries;

    protected void setUp() throws Exception {
        super.setUp();

        result = new QueryResult();
        queries = new Query[] {
                new SelectQuery(), new UpdateQuery(), new DeleteQuery()
        };
    }

    public void testAllQueries() {

        for (int i = 0; i < queries.length; i++) {
            result.nextCount(queries[i], i);
        }

        int ind = 0;
        Iterator it = result.getQueries();
        while (it.hasNext()) {
            assertSame(queries[ind], it.next());
            ind++;
        }

        assertEquals(queries.length, ind);
    }

    public void testAllQueriesIterationOrder() {

        Query q1 = new MockQuery();
        Query q2 = new MockQuery();
        Query q3 = new MockQuery();
        Query q4 = new MockQuery();
        Query q5 = new MockQuery();
        Query q6 = new MockQuery();

        QueryResult result = new QueryResult();

        result.nextCount(q1, 1);
        result.nextCount(q2, 1);
        result.nextBatchCount(q3, new int[] {
            1
        });
        result.nextDataRows(q4, new ArrayList());
        result.nextCount(q5, 1);
        result.nextCount(q6, 1);

        Query[] orderedArray = new Query[] {
                q1, q2, q3, q4, q5, q6
        };

        Iterator it = result.getQueries();
        for (int i = 0; i < orderedArray.length; i++) {
            assertTrue(it.hasNext());
            assertSame("Unexpected query at index " + i, orderedArray[i], it.next());
        }

        assertFalse(it.hasNext());
    }

    public void testResults() throws Exception {
        // add a mix of counts and rows
        result.nextCount(queries[0], 1);
        result.nextDataRows(queries[0], Collections.EMPTY_LIST);
        result.nextDataRows(queries[0], Collections.EMPTY_LIST);
        result.nextDataRows(queries[0], Collections.EMPTY_LIST);
        result.nextCount(queries[0], 5);

        Iterator it = result.getQueries();
        Query q = (Query) it.next();

        List rows = result.getRows(q);
        assertNotNull(rows);
        assertEquals(3, rows.size());

        List counts = result.getUpdates(q);
        assertNotNull(counts);
        assertEquals(2, counts.size());

        List all = result.getResults(q);
        assertNotNull(all);
        assertEquals(5, all.size());
    }
}
