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

import java.util.List;

import org.apache.art.Artist;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

/**
 */
public class SimpleIdIncrementalFaultListPrefetchTest extends DataContextCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createTestData("testPaintings");
    }
    
    public void testListType() {
        Expression e = ExpressionFactory.likeExp("artistName", "artist1%");
        SelectQuery q = new SelectQuery("Artist", e);
        q.setPageSize(4);

        List<?> result = context.performQuery(q);
        assertTrue(result instanceof SimpleIdIncrementalFaultList);
    }

    /**
     * Test that all queries specified in prefetch are executed with a single prefetch
     * path.
     */
    public void testPrefetch1() {

        Expression e = ExpressionFactory.likeExp("artistName", "artist1%");
        SelectQuery q = new SelectQuery("Artist", e);
        q.addPrefetch("paintingArray");
        q.setPageSize(4);

        IncrementalFaultList result = (IncrementalFaultList) context.performQuery(q);

        assertEquals(11, result.size());

        // currently queries with prefetch do not resolve their first page
        assertEquals(result.size(), result.getUnfetchedObjects());

        // go through the second page objects and count queries
        getDomain().restartQueryCounter();
        for (int i = 4; i < 8; i++) {
            result.get(i);

            // within the same page only one query should've been executed
            assertEquals(1, getDomain().getQueryCount());
        }
    }

    /**
     * Test that a to-many relationship is initialized.
     */
    public void testPrefetch3() {

        Expression e = ExpressionFactory.likeExp("artistName", "artist1%");
        SelectQuery q = new SelectQuery("Artist", e);
        q.addPrefetch("paintingArray");
        q.setPageSize(4);

        IncrementalFaultList result = (IncrementalFaultList) context.performQuery(q);

        assertEquals(11, result.size());

        // currently queries with prefetch do not resolve their first page
        assertEquals(result.size(), result.getUnfetchedObjects());

        // go through the second page objects and check their to many
        for (int i = 4; i < 8; i++) {
            Artist a = (Artist) result.get(i);

            List paintings = a.getPaintingArray();
            assertFalse(((ValueHolder) paintings).isFault());
            assertEquals(1, paintings.size());
        }
    }

    /**
     * Test that a to-one relationship is initialized.
     */
    public void testPrefetch4() {

        SelectQuery q = new SelectQuery("Painting");
        q.setPageSize(4);
        q.addPrefetch("toArtist");

        IncrementalFaultList result = (IncrementalFaultList) context.performQuery(q);

        // get an objects from the second page
        DataObject p1 = (DataObject) result.get(q.getPageSize());

        blockQueries();

        try {

            Object toOnePrefetch = p1.readNestedProperty("toArtist");
            assertNotNull(toOnePrefetch);
            assertTrue(
                    "Expected DataObject, got: " + toOnePrefetch.getClass().getName(),
                    toOnePrefetch instanceof DataObject);

            DataObject a1 = (DataObject) toOnePrefetch;
            assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
        }
        finally {
            unblockQueries();
        }
    }

}
