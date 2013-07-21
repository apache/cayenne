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

import org.apache.cayenne.DataObject;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class SimpleIdIncrementalFaultListTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tArtist;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");

        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");
    }

    protected void createArtistsDataSet() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
        tArtist.insert(33003, "artist3");
        tArtist.insert(33004, "artist4");
        tArtist.insert(33005, "artist5");
        tArtist.insert(33006, "artist6");
        tArtist.insert(33007, "artist7");
        tArtist.insert(33008, "artist8");
        tArtist.insert(33009, "artist9");
        tArtist.insert(33010, "artist10");
        tArtist.insert(33011, "artist11");
        tArtist.insert(33012, "artist12");
        tArtist.insert(33013, "artist13");
        tArtist.insert(33014, "artist14");
        tArtist.insert(33015, "artist15");
        tArtist.insert(33016, "artist16");
        tArtist.insert(33017, "artist17");
        tArtist.insert(33018, "artist18");
        tArtist.insert(33019, "artist19");
        tArtist.insert(33020, "artist20");
        tArtist.insert(33021, "artist21");
        tArtist.insert(33022, "artist22");
        tArtist.insert(33023, "artist23");
        tArtist.insert(33024, "artist24");
        tArtist.insert(33025, "artist25");
    }

    public void testRemoveDeleted() throws Exception {
        createArtistsDataSet();

        // DataContext context = createDataContext();

        SelectQuery query = new SelectQuery(Artist.class);
        query.setPageSize(10);
        SimpleIdIncrementalFaultList<Artist> list = new SimpleIdIncrementalFaultList<Artist>(
                context,
                query, 10000);

        assertEquals(25, list.size());

        Artist a1 = list.get(0);
        context.deleteObjects(a1);
        context.commitChanges();

        list.remove(0);
        assertEquals(24, list.size());
    }

    private SimpleIdIncrementalFaultList<?> prepareList(int pageSize) throws Exception {
        createArtistsDataSet();

        SelectQuery query = new SelectQuery(Artist.class);

        // make sure total number of objects is not divisable
        // by the page size, to test the last smaller page
        query.setPageSize(pageSize);
        query.addOrdering("db:ARTIST_ID", SortOrder.ASCENDING);
        return new SimpleIdIncrementalFaultList<Object>(context, query, 10000);
    }

    public void testSize() throws Exception {
        SimpleIdIncrementalFaultList<?> list = prepareList(6);
        assertEquals(25, list.size());
    }

    public void testSmallList() throws Exception {
        SimpleIdIncrementalFaultList<?> list = prepareList(49);
        assertEquals(25, list.size());
    }

    public void testOnePageList() throws Exception {
        SimpleIdIncrementalFaultList<?> list = prepareList(25);
        assertEquals(25, list.size());
    }

    public void testIterator() throws Exception {
        SimpleIdIncrementalFaultList<?> list = prepareList(6);
        Iterator<?> it = list.iterator();
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

        createArtistsDataSet();

        Artist newArtist = context.newObject(Artist.class);
        newArtist.setArtistName("x");
        context.commitChanges();

        SelectQuery<Artist> q = new SelectQuery<Artist>(Artist.class);
        q.setPageSize(6);
        q.addOrdering(Artist.ARTIST_NAME.asc());

        SimpleIdIncrementalFaultList<?> list = new SimpleIdIncrementalFaultList<Object>(
                context,
                q, 10000);

        assertSame(newArtist, list.get(25));
    }

    public void testListIterator() throws Exception {
        SimpleIdIncrementalFaultList<?> list = prepareList(6);
        ListIterator<?> it = list.listIterator();
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
        SimpleIdIncrementalFaultList<?> list = prepareList(6);

        new Ordering(Artist.ARTIST_NAME_PROPERTY, SortOrder.DESCENDING).orderList(list);

        Iterator<?> it = list.iterator();
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
        SimpleIdIncrementalFaultList<?> list = prepareList(6);
        assertEquals(25, list.getUnfetchedObjects());
        list.get(7);
        assertEquals(25 - 6, list.getUnfetchedObjects());
        list.resolveAll();
        assertEquals(0, list.getUnfetchedObjects());
    }

    public void testPageIndex() throws Exception {
        SimpleIdIncrementalFaultList<?> list = prepareList(6);
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
        SimpleIdIncrementalFaultList<?> list = prepareList(6);
        assertTrue(list.elements.get(0) instanceof Long);
        assertTrue(list.elements.get(8) instanceof Long);

        list.resolveInterval(5, 10);
        assertTrue(list.elements.get(7) instanceof Artist);

        list.resolveAll();
        assertTrue((list.elements.get(list.size() - 1)) instanceof Artist);
    }

    public void testGet1() throws Exception {
        SimpleIdIncrementalFaultList<?> list = prepareList(6);
        assertTrue(list.elements.get(0) instanceof Long);
        assertTrue(list.elements.get(8) instanceof Long);

        Object a = list.get(8);

        assertNotNull(a);
        assertTrue(a instanceof Artist);
        assertTrue(list.elements.get(8) instanceof Artist);
    }

    public void testIndexOf() throws Exception {
        SimpleIdIncrementalFaultList<?> list = prepareList(6);
        Expression qual = ExpressionFactory.matchExp("artistName", "artist20");
        SelectQuery query = new SelectQuery(Artist.class, qual);
        List<?> artists = list.dataContext.performQuery(query);

        assertEquals(1, artists.size());

        Artist row = (Artist) artists.get(0);
        assertEquals(19, list.indexOf(row));
        assertEquals(-1, list.indexOf(list.dataContext.newObject("Artist")));
    }

    public void testLastIndexOf() throws Exception {
        SimpleIdIncrementalFaultList<?> list = prepareList(6);
        Expression qual = ExpressionFactory.matchExp("artistName", "artist20");
        SelectQuery query = new SelectQuery(Artist.class, qual);
        List<?> artists = list.dataContext.performQuery(query);

        assertEquals(1, artists.size());

        Artist row = (Artist) artists.get(0);
        assertEquals(19, list.lastIndexOf(row));
        assertEquals(-1, list.lastIndexOf(list.dataContext.newObject("Artist")));
    }
}
