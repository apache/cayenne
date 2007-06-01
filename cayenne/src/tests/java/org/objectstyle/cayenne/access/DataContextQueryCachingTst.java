/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.access;

import java.util.ArrayList;
import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.util.MockDataDomain;
import org.objectstyle.cayenne.unit.util.MockQueryEngine;

/**
 * @author Andrei Adamchik
 */
public class DataContextQueryCachingTst extends CayenneTestCase {

    protected DataRowStore dataRowCache;
    protected DataContext context;
    protected MockQueryEngine engine;

    protected void setUp() throws Exception {
        super.setUp();

        // assemble mockup context
        this.engine = new MockQueryEngine();
        this.engine.setEntityResolver(getDomain().getEntityResolver());

        this.dataRowCache = new DataRowStore("test");
        this.dataRowCache.setNotifyingRemoteListeners(false);

        this.context = mockupDataContext();
    }

    public void testLocalCacheDataRowsNoRefresh() throws Exception {
        SelectQuery select = new SelectQuery(Artist.class);
        select.setName("c");
        select.setRefreshingObjects(false);
        select.setFetchingDataRows(true);
        select.setCachePolicy(GenericSelectQuery.LOCAL_CACHE);

        List rows = mockupDataRows(2);
        engine.reset();
        engine.addExpectedResult(select, rows);

        // first run, no cache yet
        List resultRows = context.performQuery(select);
        assertEquals(1, engine.getRunCount());
        assertEquals(rows, resultRows);
        assertNull(dataRowCache.getCachedSnapshots("c"));
        assertEquals(rows, context.getObjectStore().getCachedQueryResult("c"));

        // now the query with the same name must run from cache
        engine.reset();
        List cachedResultRows = context.performQuery(select);
        assertEquals(0, engine.getRunCount());
        assertEquals(rows, cachedResultRows);
    }

    public void testLocalCacheDataRowsRefresh() throws Exception {
        SelectQuery select = new SelectQuery(Artist.class);
        select.setName("c");
        select.setRefreshingObjects(true);
        select.setFetchingDataRows(true);
        select.setCachePolicy(GenericSelectQuery.LOCAL_CACHE);

        // first run, no cache yet
        List rows1 = mockupDataRows(2);
        engine.reset();
        engine.addExpectedResult(select, rows1);
        List resultRows = context.performQuery(select);
        assertEquals(1, engine.getRunCount());
        assertEquals(rows1, resultRows);
        assertNull(dataRowCache.getCachedSnapshots("c"));
        assertEquals(rows1, context.getObjectStore().getCachedQueryResult("c"));

        // second run, must refresh the cache
        List rows2 = mockupDataRows(4);
        engine.reset();
        engine.addExpectedResult(select, rows2);
        List freshResultRows = context.performQuery(select);
        assertEquals(1, engine.getRunCount());
        assertEquals(rows2, freshResultRows);
        assertNull(dataRowCache.getCachedSnapshots("c"));
        assertEquals(rows2, context.getObjectStore().getCachedQueryResult("c"));
    }

    public void testSharedCacheDataRowsNoRefresh() throws Exception {
        SelectQuery select = new SelectQuery(Artist.class);
        select.setName("c");
        select.setRefreshingObjects(false);
        select.setFetchingDataRows(true);
        select.setCachePolicy(GenericSelectQuery.SHARED_CACHE);

        List rows = mockupDataRows(2);
        engine.reset();
        engine.addExpectedResult(select, rows);

        // first run, no cache yet
        List resultRows = context.performQuery(select);
        assertEquals(1, engine.getRunCount());
        assertEquals(rows, resultRows);
        assertNull(context.getObjectStore().getCachedQueryResult("c"));
        assertEquals(rows, dataRowCache.getCachedSnapshots("c"));

        // now the query with the same name must run from cache
        engine.reset();
        List cachedResultRows = context.performQuery(select);
        assertEquals(0, engine.getRunCount());
        assertEquals(rows, cachedResultRows);

        // query from an alt DataContext must run from cache
        DataContext altContext = mockupDataContext();
        engine.reset();
        List altResultRows = altContext.performQuery(select);
        assertEquals(0, engine.getRunCount());
        assertEquals(rows, altResultRows);
    }

    public void testSharedCacheDataRowsRefresh() throws Exception {
        SelectQuery select = new SelectQuery(Artist.class);
        select.setName("c");
        select.setRefreshingObjects(true);
        select.setFetchingDataRows(true);
        select.setCachePolicy(GenericSelectQuery.SHARED_CACHE);

        // first run, no cache yet
        List rows1 = mockupDataRows(2);
        engine.reset();
        engine.addExpectedResult(select, rows1);
        List resultRows = context.performQuery(select);
        assertEquals(1, engine.getRunCount());
        assertEquals(rows1, resultRows);
        assertEquals(rows1, dataRowCache.getCachedSnapshots("c"));
        assertNull(context.getObjectStore().getCachedQueryResult("c"));

        // second run, must refresh the cache
        List rows2 = mockupDataRows(5);
        engine.reset();
        engine.addExpectedResult(select, rows2);
        List freshResultRows = context.performQuery(select);
        assertEquals(1, engine.getRunCount());
        assertEquals(rows2, freshResultRows);
        assertEquals(rows2, dataRowCache.getCachedSnapshots("c"));
        assertNull(context.getObjectStore().getCachedQueryResult("c"));
    }

    public void testLocalCacheDataObjectsRefresh() throws Exception {
        SelectQuery select = new SelectQuery(Artist.class);
        select.setName("c");
        select.setRefreshingObjects(true);
        select.setFetchingDataRows(false);
        select.setCachePolicy(GenericSelectQuery.LOCAL_CACHE);

        // first run, no cache yet
        List rows1 = mockupDataRows(2);
        engine.reset();
        engine.addExpectedResult(select, rows1);
        List resultRows = context.performQuery(select);
        assertEquals(1, engine.getRunCount());
        assertEquals(2, resultRows.size());
        assertTrue(resultRows.get(0) instanceof DataObject);
        assertNull(dataRowCache.getCachedSnapshots("c"));
        assertEquals(resultRows, context.getObjectStore().getCachedQueryResult("c"));

        // second run, must refresh the cache
        List rows2 = mockupDataRows(4);
        engine.reset();
        engine.addExpectedResult(select, rows2);
        List freshResultRows = context.performQuery(select);
        assertEquals(1, engine.getRunCount());
        assertEquals(4, freshResultRows.size());
        assertTrue(resultRows.get(0) instanceof DataObject);
        assertNull(dataRowCache.getCachedSnapshots("c"));
        assertEquals(freshResultRows, context.getObjectStore().getCachedQueryResult("c"));
    }

    public void testLocalCacheDataObjectsNoRefresh() throws Exception {
        SelectQuery select = new SelectQuery(Artist.class);
        select.setName("c");
        select.setRefreshingObjects(false);
        select.setFetchingDataRows(false);
        select.setCachePolicy(GenericSelectQuery.LOCAL_CACHE);

        List rows = mockupDataRows(2);
        engine.reset();
        engine.addExpectedResult(select, rows);

        // first run, no cache yet
        List resultRows = context.performQuery(select);
        assertEquals(1, engine.getRunCount());
        assertEquals(2, resultRows.size());
        assertTrue(resultRows.get(0) instanceof DataObject);
        assertNull(dataRowCache.getCachedSnapshots("c"));
        assertEquals(resultRows, context.getObjectStore().getCachedQueryResult("c"));

        // now the query with the same name must run from cache
        engine.reset();
        List cachedResultRows = context.performQuery(select);
        assertEquals(0, engine.getRunCount());
        assertEquals(resultRows, cachedResultRows);
    }

    public void testSharedCacheDataObjectsNoRefresh() throws Exception {
        SelectQuery select = new SelectQuery(Artist.class);
        select.setName("c");
        select.setRefreshingObjects(false);
        select.setFetchingDataRows(false);
        select.setCachePolicy(GenericSelectQuery.SHARED_CACHE);

        List rows = mockupDataRows(2);
        engine.reset();
        engine.addExpectedResult(select, rows);

        // first run, no cache yet
        List resultRows = context.performQuery(select);
        assertEquals(1, engine.getRunCount());
        assertEquals(2, resultRows.size());
        assertTrue(resultRows.get(0) instanceof DataObject);
        assertNull(context.getObjectStore().getCachedQueryResult("c"));
        assertEquals(rows, dataRowCache.getCachedSnapshots("c"));

        // now the query with the same name must run from cache
        engine.reset();
        List cachedResultRows = context.performQuery(select);
        assertEquals(0, engine.getRunCount());
        assertEquals(2, cachedResultRows.size());
        assertTrue(cachedResultRows.get(0) instanceof DataObject);

        // query from an alt DataContext must run from cache
        DataContext altContext = mockupDataContext();
        engine.reset();
        List altResultRows = altContext.performQuery(select);
        assertEquals(0, engine.getRunCount());
        assertEquals(2, altResultRows.size());
        assertTrue(altResultRows.get(0) instanceof DataObject);
    }

    private DataContext mockupDataContext() {
        DataContext context = new DataContext();
        context.objectStore = new ObjectStore(dataRowCache);
        context.usingSharedSnaphsotCache = true;
        context.setParent(new MockDataDomain(engine));

        return context;
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