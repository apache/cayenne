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
import java.util.List;

import org.apache.art.Artist;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.query.MockQueryMetadata;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

/**
 * @author Andrus Adamchik
 */
public class DataContextQueryCachingTest extends CayenneCase {

    protected DataContext context;

    protected void setUp() throws Exception {
        super.setUp();

        context = createDataContextWithSharedCache();
    }

    protected DataContext createDataContextNoCacheClear() {
        return getDomain().createDataContext();
    }

    public void testLocalCacheDataRowsNoRefresh() throws Exception {
        SelectQuery select = new SelectQuery(Artist.class);
        select.setName("c");
        select.setRefreshingObjects(false);
        select.setFetchingDataRows(true);
        select.setCachePolicy(QueryMetadata.LOCAL_CACHE);

        MockDataNode engine = MockDataNode.interceptNode(getDomain(), getNode());

        try {
            List rows = mockupDataRows(2);
            engine.reset();
            engine.addExpectedResult(select, rows);

            // first run, no cache yet
            List resultRows = context.performQuery(select);
            assertEquals(1, engine.getRunCount());
            assertEquals(rows, resultRows);

            QueryMetadata cacheKey = new MockQueryMetadata() {

                public String getCacheKey() {
                    return "c";
                }
            };

            assertNull(context.getParentDataDomain().getQueryCache().get(cacheKey));
            assertEquals(rows, context.getQueryCache().get(cacheKey));

            // now the query with the same name must run from cache
            engine.reset();
            List cachedResultRows = context.performQuery(select);
            assertEquals(0, engine.getRunCount());
            assertEquals(rows, cachedResultRows);
        }
        finally {
            engine.stopInterceptNode();
        }
    }

    public void testLocalCacheDataRowsRefresh() throws Exception {
        SelectQuery select = new SelectQuery(Artist.class);
        select.setName("c");
        select.setRefreshingObjects(true);
        select.setFetchingDataRows(true);
        select.setCachePolicy(QueryMetadata.LOCAL_CACHE);

        MockDataNode engine = MockDataNode.interceptNode(getDomain(), getNode());

        try {

            // first run, no cache yet
            List rows1 = mockupDataRows(2);
            engine.reset();
            engine.addExpectedResult(select, rows1);
            List resultRows = context.performQuery(select);
            assertEquals(1, engine.getRunCount());
            assertEquals(rows1, resultRows);

            QueryMetadata cacheKey = new MockQueryMetadata() {

                public String getCacheKey() {
                    return "c";
                }
            };
            assertNull(context.getParentDataDomain().getQueryCache().get(cacheKey));

            assertEquals(rows1, context.getQueryCache().get(cacheKey));

            // second run, must refresh the cache
            List rows2 = mockupDataRows(4);
            engine.reset();
            engine.addExpectedResult(select, rows2);
            select.setCachePolicy(QueryMetadata.LOCAL_CACHE_REFRESH);
            List freshResultRows = context.performQuery(select);
            assertEquals(1, engine.getRunCount());
            assertEquals(rows2, freshResultRows);
            assertNull(context.getParentDataDomain().getQueryCache().get(cacheKey));
            assertEquals(rows2, context.getQueryCache().get(cacheKey));
        }
        finally {
            engine.stopInterceptNode();
        }
    }

    public void testSharedCacheDataRowsNoRefresh() throws Exception {
        SelectQuery select = new SelectQuery(Artist.class);
        select.setName("c");
        select.setRefreshingObjects(false);
        select.setFetchingDataRows(true);
        select.setCachePolicy(QueryMetadata.SHARED_CACHE);

        MockDataNode engine = MockDataNode.interceptNode(getDomain(), getNode());

        try {
            List rows = mockupDataRows(2);
            engine.reset();
            engine.addExpectedResult(select, rows);

            // first run, no cache yet
            List resultRows = context.performQuery(select);
            assertEquals(1, engine.getRunCount());
            assertEquals(rows, resultRows);
            QueryMetadata cacheKey = new MockQueryMetadata() {

                public String getCacheKey() {
                    return "c";
                }
            };

            assertNull(context.getQueryCache().get(cacheKey));
            assertEquals(rows, context
                    .getParentDataDomain()
                    .getQueryCache()
                    .get(cacheKey));

            // now the query with the same name must run from cache
            engine.reset();
            List cachedResultRows = context.performQuery(select);
            assertEquals(0, engine.getRunCount());
            assertEquals(rows, cachedResultRows);

            // query from an alt DataContext must run from cache
            DataContext altContext = createDataContextNoCacheClear();
            engine.reset();
            List altResultRows = altContext.performQuery(select);
            assertEquals(0, engine.getRunCount());
            assertEquals(rows, altResultRows);
        }
        finally {
            engine.stopInterceptNode();
        }
    }

    public void testSharedCacheDataRowsRefresh() throws Exception {
        SelectQuery select = new SelectQuery(Artist.class);
        select.setName("c");
        select.setRefreshingObjects(true);
        select.setFetchingDataRows(true);
        select.setCachePolicy(QueryMetadata.SHARED_CACHE);

        MockDataNode engine = MockDataNode.interceptNode(getDomain(), getNode());

        try {
            // first run, no cache yet
            List rows1 = mockupDataRows(2);
            engine.reset();
            engine.addExpectedResult(select, rows1);
            List resultRows = context.performQuery(select);
            assertEquals(1, engine.getRunCount());
            assertEquals(rows1, resultRows);

            QueryMetadata cacheKey = new MockQueryMetadata() {

                public String getCacheKey() {
                    return "c";
                }
            };

            assertEquals(rows1, context.getParentDataDomain().getQueryCache().get(
                    cacheKey));

            assertNull(context.getQueryCache().get(cacheKey));

            // second run, must refresh the cache
            List rows2 = mockupDataRows(5);
            engine.reset();
            engine.addExpectedResult(select, rows2);
            select.setCachePolicy(QueryMetadata.SHARED_CACHE_REFRESH);
            List freshResultRows = context.performQuery(select);
            assertEquals(1, engine.getRunCount());
            assertEquals(rows2, freshResultRows);
            assertEquals(rows2, context.getParentDataDomain().getQueryCache().get(
                    cacheKey));
            assertNull(context.getQueryCache().get(cacheKey));
        }
        finally {
            engine.stopInterceptNode();
        }
    }

    public void testLocalCacheDataObjectsRefresh() throws Exception {
        SelectQuery select = new SelectQuery(Artist.class);
        select.setName("c");
        select.setRefreshingObjects(true);
        select.setFetchingDataRows(false);
        select.setCachePolicy(QueryMetadata.LOCAL_CACHE);

        MockDataNode engine = MockDataNode.interceptNode(getDomain(), getNode());

        try {
            // first run, no cache yet
            List rows1 = mockupDataRows(2);
            engine.reset();
            engine.addExpectedResult(select, rows1);
            List resultRows = context.performQuery(select);
            assertEquals(1, engine.getRunCount());
            assertEquals(2, resultRows.size());
            assertTrue(resultRows.get(0) instanceof DataObject);
            QueryMetadata cacheKey = new MockQueryMetadata() {

                public String getCacheKey() {
                    return "c";
                }
            };
            assertNull(context.getParentDataDomain().getQueryCache().get(cacheKey));

            assertEquals(resultRows, context.getQueryCache().get(cacheKey));

            // second run, must refresh the cache
            List rows2 = mockupDataRows(4);
            engine.reset();
            engine.addExpectedResult(select, rows2);
            select.setCachePolicy(QueryMetadata.LOCAL_CACHE_REFRESH);
            List freshResultRows = context.performQuery(select);

            assertEquals(1, engine.getRunCount());
            assertEquals(4, freshResultRows.size());
            assertTrue(resultRows.get(0) instanceof DataObject);
            assertNull(context.getParentDataDomain().getQueryCache().get(cacheKey));
            assertEquals(freshResultRows, context.getQueryCache().get(cacheKey));
        }
        finally {
            engine.stopInterceptNode();
        }
    }

    public void testLocalCacheDataObjectsNoRefresh() throws Exception {
        SelectQuery select = new SelectQuery(Artist.class);
        select.setName("c");
        select.setRefreshingObjects(false);
        select.setFetchingDataRows(false);
        select.setCachePolicy(QueryMetadata.LOCAL_CACHE);

        MockDataNode engine = MockDataNode.interceptNode(getDomain(), getNode());

        try {
            List rows = mockupDataRows(2);
            engine.reset();
            engine.addExpectedResult(select, rows);

            // first run, no cache yet
            List resultRows = context.performQuery(select);
            assertEquals(1, engine.getRunCount());
            assertEquals(2, resultRows.size());
            assertTrue(resultRows.get(0) instanceof DataObject);

            QueryMetadata cacheKey = new MockQueryMetadata() {

                public String getCacheKey() {
                    return "c";
                }
            };
            assertNull(context.getParentDataDomain().getQueryCache().get(cacheKey));

            assertEquals(resultRows, context.getQueryCache().get(cacheKey));

            // now the query with the same name must run from cache
            engine.reset();
            List cachedResultRows = context.performQuery(select);
            assertEquals(0, engine.getRunCount());
            assertEquals(resultRows, cachedResultRows);
        }
        finally {
            engine.stopInterceptNode();
        }
    }

    public void testSharedCacheDataObjectsNoRefresh() throws Exception {
        SelectQuery select = new SelectQuery(Artist.class);
        select.setName("c");
        select.setRefreshingObjects(false);
        select.setFetchingDataRows(false);
        select.setCachePolicy(QueryMetadata.SHARED_CACHE);

        MockDataNode engine = MockDataNode.interceptNode(getDomain(), getNode());

        try {
            List rows = mockupDataRows(2);
            engine.reset();
            engine.addExpectedResult(select, rows);

            // first run, no cache yet
            List resultRows = context.performQuery(select);
            assertEquals(1, engine.getRunCount());
            assertEquals(2, resultRows.size());
            assertTrue(resultRows.get(0) instanceof DataObject);
            QueryMetadata cacheKey = new MockQueryMetadata() {

                public String getCacheKey() {
                    return "c";
                }
            };
            assertNull(context.getQueryCache().get(cacheKey));
            assertEquals(rows, context
                    .getParentDataDomain()
                    .getQueryCache()
                    .get(cacheKey));

            // now the query with the same name must run from cache
            engine.reset();
            List cachedResultRows = context.performQuery(select);
            assertEquals(0, engine.getRunCount());
            assertEquals(2, cachedResultRows.size());
            assertTrue(cachedResultRows.get(0) instanceof DataObject);

            // query from an alt DataContext must run from cache
            DataContext altContext = createDataContextNoCacheClear();
            engine.reset();
            List altResultRows = altContext.performQuery(select);
            assertEquals(0, engine.getRunCount());
            assertEquals(2, altResultRows.size());
            assertTrue(altResultRows.get(0) instanceof DataObject);
        }
        finally {
            engine.stopInterceptNode();
        }
    }

    public void testLocalCacheRefreshObjectsRefresh() throws Exception {

        deleteTestData();
        createTestData("testLocalCacheRefreshObjectsRefresh_Insert");

        SelectQuery select = new SelectQuery(Artist.class);
        select.setName("c");
        select.setRefreshingObjects(true);
        select.setCachePolicy(QueryMetadata.LOCAL_CACHE_REFRESH);

        // no cache yet...

        List objects1 = context.performQuery(select);
        assertEquals(1, objects1.size());
        Artist a1 = (Artist) objects1.get(0);
        assertEquals("aaa", a1.getArtistName());

        // cache, but force refresh

        createTestData("testLocalCacheRefreshObjectsRefresh_Update1");
        List objects2 = context.performQuery(select);
        assertEquals(1, objects2.size());
        Artist a2 = (Artist) objects2.get(0);
        assertSame(a1, a2);
        assertEquals("bbb", a2.getArtistName());

        // cache, no refresh
        select.setRefreshingObjects(false);
        createTestData("testLocalCacheRefreshObjectsRefresh_Update1");

        List objects3 = context.performQuery(select);
        assertEquals(1, objects3.size());
        Artist a3 = (Artist) objects3.get(0);
        assertSame(a1, a3);
        assertEquals("bbb", a3.getArtistName());
    }

    private List mockupDataRows(int len) {
        List rows = new ArrayList(len);

        for (int i = 0; i < len; i++) {
            DataRow a = new DataRow(3);
            a.put("ARTIST_ID", new Integer(i + 1));
            a.put("ARTIST_NAME", "A-" + (i + 1));
            rows.add(a);
        }

        return rows;
    }
}
