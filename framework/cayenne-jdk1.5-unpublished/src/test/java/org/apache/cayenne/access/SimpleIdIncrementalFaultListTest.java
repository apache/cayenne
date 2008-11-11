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
import java.util.ListIterator;

import org.apache.art.Artist;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class SimpleIdIncrementalFaultListTest extends CayenneCase {

    protected SimpleIdIncrementalFaultList<?> list;
    protected Query query;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testRemoveDeleted() throws Exception {
        createTestData("testArtists");

        DataContext context = createDataContext();

        SelectQuery query = new SelectQuery(Artist.class);
        query.setPageSize(10);
        SimpleIdIncrementalFaultList<Artist> list = new SimpleIdIncrementalFaultList<Artist>(
                context,
                query);

        assertEquals(25, list.size());

        Artist a1 = list.get(0);
        context.deleteObject(a1);
        context.commitChanges();

        list.remove(0);
        assertEquals(24, list.size());
    }

    protected void prepareList(int pageSize) throws Exception {
        super.setUp();
        deleteTestData();
        createTestData("testArtists");

        SelectQuery q = new SelectQuery("Artist");

        // make sure total number of objects is not divisable
        // by the page size, to test the last smaller page
        q.setPageSize(pageSize);
        q.addOrdering("db:ARTIST_ID", Ordering.ASC);
        query = q;
        list = new SimpleIdIncrementalFaultList<Object>(createDataContext(), query);
    }

    public void testSize() throws Exception {
        prepareList(6);
        assertEquals(DataContextTest.artistCount, list.size());
    }

    public void testSmallList() throws Exception {
        prepareList(49);
        assertEquals(DataContextTest.artistCount, list.size());
    }

    public void testOnePageList() throws Exception {
        prepareList(DataContextTest.artistCount);
        assertEquals(DataContextTest.artistCount, list.size());
    }

    public void testIterator() throws Exception {
        prepareList(6);
        Iterator it = list.iterator();
        int counter = 0;
        while (it.hasNext()) {
            Object obj = it.next();
            assertNotNull(obj);
            assertTrue(obj instanceof DataObject);

            // iterator must be resolved page by page
            int expectedResolved = list.pageIndex(counter)
                    * list.getPageSize()
                    + list.getPageSize();
            if (expectedResolved > list.size()) {
                expectedResolved = list.size();
            }

            assertEquals(list.size() - expectedResolved, list.getUnfetchedObjects());

            counter++;
        }
    }

    public void testNewObject() throws Exception {

        deleteTestData();
        createTestData("testArtists");

        DataContext context = createDataContext();

        Artist newArtist = context.newObject(Artist.class);
        newArtist.setArtistName("X");
        context.commitChanges();

        SelectQuery q = new SelectQuery(Artist.class);
        q.setPageSize(6);
        q.addOrdering("db:ARTIST_ID", Ordering.DESC);

        SimpleIdIncrementalFaultList<?> list = new SimpleIdIncrementalFaultList<Object>(
                context,
                q);

        assertSame(newArtist, list.get(DataContextTest.artistCount));
    }

    public void testListIterator() throws Exception {
        prepareList(6);
        ListIterator it = list.listIterator();
        int counter = 0;
        while (it.hasNext()) {
            Object obj = it.next();
            assertNotNull(obj);
            assertTrue(obj instanceof DataObject);

            // iterator must be resolved page by page
            int expectedResolved = list.pageIndex(counter)
                    * list.getPageSize()
                    + list.getPageSize();
            if (expectedResolved > list.size()) {
                expectedResolved = list.size();
            }

            assertEquals(list.size() - expectedResolved, list.getUnfetchedObjects());

            counter++;
        }
    }

    public void testSort() throws Exception {
        prepareList(6);

        new Ordering(Artist.ARTIST_NAME_PROPERTY, Ordering.DESC).orderList(list);

        Iterator it = list.iterator();
        Artist previousArtist = null;
        while (it.hasNext()) {
            Artist artist = (Artist) it.next();
            if (previousArtist != null) {
                assertTrue(previousArtist.getArtistName().compareTo(
                        artist.getArtistName()) > 0);
            }
        }
    }

    public void testUnfetchedObjects() throws Exception {
        prepareList(6);
        assertEquals(DataContextTest.artistCount, list.getUnfetchedObjects());
        list.get(7);
        assertEquals(DataContextTest.artistCount - 6, list.getUnfetchedObjects());
        list.resolveAll();
        assertEquals(0, list.getUnfetchedObjects());
    }

    public void testPageIndex() throws Exception {
        prepareList(6);
        assertEquals(0, list.pageIndex(0));
        assertEquals(0, list.pageIndex(1));
        assertEquals(1, list.pageIndex(6));

        try {
            assertEquals(13, list.pageIndex(82));
            fail("Element index beyound array size must throw an IndexOutOfBoundsException.");
        }
        catch (IndexOutOfBoundsException ex) {
            // exception expercted
        }
    }

    public void testPagesRead1() throws Exception {
        prepareList(6);
        assertTrue(list.elements.get(0) instanceof Long);
        assertTrue(list.elements.get(8) instanceof Long);

        list.resolveInterval(5, 10);
        assertTrue(list.elements.get(7) instanceof Artist);

        list.resolveAll();
        assertTrue((list.elements.get(list.size() - 1)) instanceof Artist);
    }

    public void testGet1() throws Exception {
        prepareList(6);
        assertTrue(list.elements.get(0) instanceof Long);
        assertTrue(list.elements.get(8) instanceof Long);

        Object a = list.get(8);

        assertNotNull(a);
        assertTrue(a instanceof Artist);
        assertTrue(list.elements.get(8) instanceof Artist);
    }

    public void testGet2() throws Exception {
        prepareList(6);
        ((SelectQuery) query).setFetchingDataRows(true);
        assertTrue(list.elements.get(0) instanceof Long);
        assertTrue(list.elements.get(8) instanceof Long);

        Object a0 = list.get(0);

        assertNotNull(a0);
        assertTrue(list.elements.get(0) instanceof Artist);

        Object a = list.get(8);

        assertNotNull(a);
        assertTrue(list.elements.get(8) instanceof Artist);
    }

    public void testIndexOf() throws Exception {
        prepareList(6);
        Expression qual = ExpressionFactory.matchExp("artistName", "artist20");
        SelectQuery query = new SelectQuery(Artist.class, qual);
        List artists = list.dataContext.performQuery(query);

        assertEquals(1, artists.size());

        Artist row = (Artist) artists.get(0);
        assertEquals(19, list.indexOf(row));
        assertEquals(-1, list.indexOf(list.dataContext.newObject("Artist")));
    }

    public void testLastIndexOf() throws Exception {
        prepareList(6);
        Expression qual = ExpressionFactory.matchExp("artistName", "artist20");
        SelectQuery query = new SelectQuery(Artist.class, qual);
        List artists = list.dataContext.performQuery(query);

        assertEquals(1, artists.size());

        Artist row = (Artist) artists.get(0);
        assertEquals(19, list.lastIndexOf(row));
        assertEquals(-1, list.lastIndexOf(list.dataContext.newObject("Artist")));
    }
}
