/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.cache.MapQueryCache;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextQueryCachingIT extends RuntimeCase {

    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tArtist;
    protected TableHelper tPainting;

    protected QueryCache oldCache;
    protected DataDomain domain;

    protected DataNode getNode() {
        return this.domain.getDataNodes().iterator().next();
    }

    @Before
    public void setUp() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns(
                "PAINTING_ID",
                "PAINTING_TITLE",
                "ARTIST_ID",
                "ESTIMATED_PRICE");

        domain = context.getParentDataDomain();
        oldCache = domain.getQueryCache();
        domain.setQueryCache(new MapQueryCache(50));
        context.setQueryCache(new MapQueryCache(50));
    }

    @After
    public void tearDown() throws Exception {
        domain.setQueryCache(oldCache);
    }

    protected void createInsertDataSet() throws Exception {
        tArtist.insert(33001, "aaa");
        tPainting.insert(33001, "P", 33001, 4000);
    }

    protected void createUpdateDataSet1() throws Exception {
        tArtist.update().set("ARTIST_NAME", "bbb").where("ARTIST_ID", 33001).execute();
    }

    protected void createUpdateDataSet2() throws Exception {
        tArtist.update().set("ARTIST_NAME", "ccc").where("ARTIST_ID", 33001).execute();
    }

    @Test
    public void testLocalCacheDataRowsRefresh() throws Exception {
        ObjectSelect<DataRow> select = ObjectSelect.dataRowQuery(Artist.class).localCache();

        MockDataNode engine = MockDataNode.interceptNode(domain, getNode());

        try {

            // first run, no cache yet
            List<?> rows1 = mockupDataRows(2);
            engine.reset();
            engine.addExpectedResult(select, rows1);
            List<?> resultRows = context.performQuery(select);
            assertEquals(1, engine.getRunCount());
            assertEquals(rows1, resultRows);

            QueryMetadata cacheKey = select.getMetaData(context.getEntityResolver());
            assertNull(context.getParentDataDomain().getQueryCache().get(cacheKey));

            assertEquals(rows1, context.getQueryCache().get(cacheKey));

            // second run, must refresh the cache
            List<?> rows2 = mockupDataRows(4);
            engine.reset();
            engine.addExpectedResult(select, rows2);
            select.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE_REFRESH);
            List<?> freshResultRows = context.performQuery(select);
            assertEquals(1, engine.getRunCount());
            assertEquals(rows2, freshResultRows);
            assertNull(context.getParentDataDomain().getQueryCache().get(cacheKey));
            assertEquals(rows2, context.getQueryCache().get(cacheKey));
        }
        finally {
            engine.stopInterceptNode();
        }
    }

    @Test
    public void testSharedCacheDataRowsRefresh() throws Exception {

        ObjectSelect<DataRow> select = ObjectSelect.dataRowQuery(Artist.class).sharedCache();

        MockDataNode engine = MockDataNode.interceptNode(domain, getNode());

        try {
            // first run, no cache yet
            List<?> rows1 = mockupDataRows(2);
            engine.reset();
            engine.addExpectedResult(select, rows1);
            List<?> resultRows = context.performQuery(select);
            assertEquals(1, engine.getRunCount());
            assertEquals(rows1, resultRows);

            QueryMetadata cacheKey = select.getMetaData(context.getEntityResolver());

            assertEquals(rows1, context.getParentDataDomain().getQueryCache().get(
                    cacheKey));

            assertNull(context.getQueryCache().get(cacheKey));

            // second run, must refresh the cache
            List<?> rows2 = mockupDataRows(5);
            engine.reset();
            engine.addExpectedResult(select, rows2);
            select.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE_REFRESH);
            List<?> freshResultRows = context.performQuery(select);
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

    @Test
    public void testLocalCachePersistentObjectsRefresh() throws Exception {

        ObjectSelect<Artist> select = ObjectSelect.query(Artist.class).localCache();

        MockDataNode engine = MockDataNode.interceptNode(domain, getNode());

        try {
            // first run, no cache yet
            List<?> rows1 = mockupDataRows(2);
            engine.reset();
            engine.addExpectedResult(select, rows1);
            List<?> resultRows = context.performQuery(select);
            assertEquals(1, engine.getRunCount());
            assertEquals(2, resultRows.size());
            assertTrue(resultRows.get(0) instanceof Persistent);
            QueryMetadata cacheKey = select.getMetaData(context.getEntityResolver());
            assertNull(context.getParentDataDomain().getQueryCache().get(cacheKey));

            assertEquals(resultRows, context.getQueryCache().get(cacheKey));

            // second run, must refresh the cache
            List<?> rows2 = mockupDataRows(4);
            engine.reset();
            engine.addExpectedResult(select, rows2);
            select.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE_REFRESH);
            List<?> freshResultRows = context.performQuery(select);

            assertEquals(1, engine.getRunCount());
            assertEquals(4, freshResultRows.size());
            assertTrue(resultRows.get(0) instanceof Persistent);
            assertNull(context.getParentDataDomain().getQueryCache().get(cacheKey));
            assertEquals(freshResultRows, context.getQueryCache().get(cacheKey));
        }
        finally {
            engine.stopInterceptNode();
        }
    }

    @Test
    public void testLocalCacheRefreshObjectsRefresh() throws Exception {
        createInsertDataSet();

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .cacheStrategy(QueryCacheStrategy.LOCAL_CACHE_REFRESH);

        // no cache yet...

        List<Artist> objects1 = query.select(context);
        assertEquals(1, objects1.size());
        Artist a1 = objects1.get(0);
        assertEquals("aaa", a1.getArtistName());

        // cache, but force refresh

        createUpdateDataSet1();

        List<?> objects2 = query.select(context);
        assertEquals(1, objects2.size());
        Artist a2 = (Artist) objects2.get(0);
        assertSame(a1, a2);
        assertEquals("bbb", a2.getArtistName());
    }

    private List<?> mockupDataRows(int len) {
        List<Object> rows = new ArrayList<>(len);

        for (int i = 0; i < len; i++) {
            DataRow a = new DataRow(3);
            a.put("ARTIST_ID", i + 1);
            a.put("ARTIST_NAME", "A-" + (i + 1));
            rows.add(a);
        }

        return rows;
    }
}
