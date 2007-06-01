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

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class IncrementalFaultListTst extends CayenneTestCase {
    protected IncrementalFaultList list;
    protected GenericSelectQuery query;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
        createTestData("testArtists");

        SelectQuery q = new SelectQuery("Artist");

        // make sure total number of objects is not divisable
        // by the page size, to test the last smaller page
        q.setPageSize(6);
        q.addOrdering("db:ARTIST_ID", Ordering.ASC);
        query = q;
        list = new IncrementalFaultList(super.createDataContext(), query);
    }

    public void testSize() throws Exception {
        assertEquals(DataContextTst.artistCount, list.size());
    }

    public void testIterator() throws Exception {
        Iterator it = list.iterator();
        int counter = 0;
        while (it.hasNext()) {
            Object obj = it.next();
            assertNotNull(obj);
            assertTrue(obj instanceof DataObject);

            // iterator must be resolved page by page
            int expectedResolved =
                list.pageIndex(counter) * list.getPageSize() + list.getPageSize();
            if (expectedResolved > list.size()) {
                expectedResolved = list.size();
            }

            assertEquals(list.size() - expectedResolved, list.getUnfetchedObjects());

            counter++;
        }
    }

    public void testListIterator() throws Exception {
        ListIterator it = list.listIterator();
        int counter = 0;
        while (it.hasNext()) {
            Object obj = it.next();
            assertNotNull(obj);
            assertTrue(obj instanceof DataObject);

            // iterator must be resolved page by page
            int expectedResolved =
                list.pageIndex(counter) * list.getPageSize() + list.getPageSize();
            if (expectedResolved > list.size()) {
                expectedResolved = list.size();
            }

            assertEquals(list.size() - expectedResolved, list.getUnfetchedObjects());

            counter++;
        }
    }

    public void testUnfetchedObjects() throws Exception {
        assertEquals(DataContextTst.artistCount - 6, list.getUnfetchedObjects());
        list.get(7);
        assertEquals(DataContextTst.artistCount - 12, list.getUnfetchedObjects());
        list.resolveAll();
        assertEquals(0, list.getUnfetchedObjects());
    }

    public void testPageIndex() throws Exception {
        assertEquals(0, list.pageIndex(0));
        assertEquals(0, list.pageIndex(1));
        assertEquals(1, list.pageIndex(6));

        try {
            assertEquals(13, list.pageIndex(82));
            fail("Element index beyound array size must throw an IndexOutOfBoundsException.");
        } catch (IndexOutOfBoundsException ex) {
            // exception expercted
        }
    }

    public void testPagesRead1() throws Exception {
        assertTrue(list.elements.get(0) instanceof Artist);
        assertTrue(list.elements.get(8) instanceof Map);

        list.resolveInterval(5, 10);
        assertTrue(list.elements.get(7) instanceof Artist);

        list.resolveAll();
        assertTrue((list.elements.get(list.size() - 1)) instanceof Artist);
    }

    public void testGet1() throws Exception {
        assertTrue(list.elements.get(0) instanceof Artist);
        assertTrue(list.elements.get(8) instanceof Map);

        Object a = list.get(8);

        assertNotNull(a);
        assertTrue(a instanceof Artist);
        assertTrue(list.elements.get(8) instanceof Artist);
    }

    public void testGet2() throws Exception {
        ((SelectQuery) query).setFetchingDataRows(true);
        assertTrue(list.elements.get(0) instanceof Artist);
        assertTrue(list.elements.get(8) instanceof Map);

        Object a = list.get(8);

        assertNotNull(a);
        assertTrue(list.elements.get(8) instanceof Artist);
    }

    public void testIndexOf() throws Exception {

        Expression qual = ExpressionFactory.matchExp("artistName", "artist20");
        SelectQuery query = new SelectQuery(Artist.class, qual);
        List artists = list.dataContext.performQuery(query);

        assertEquals(1, artists.size());

        Artist row = (Artist) artists.get(0);
        assertEquals(19, list.indexOf(row));
        assertEquals(
            -1,
            list.indexOf(list.dataContext.createAndRegisterNewObject("Artist")));
    }

    public void testLastIndexOf() throws Exception {

        Expression qual = ExpressionFactory.matchExp("artistName", "artist20");
        SelectQuery query = new SelectQuery(Artist.class, qual);
        List artists = list.dataContext.performQuery(query);

        assertEquals(1, artists.size());

        Artist row = (Artist) artists.get(0);
        assertEquals(19, list.lastIndexOf(row));
        assertEquals(
            -1,
            list.lastIndexOf(list.dataContext.createAndRegisterNewObject("Artist")));
    }
}
