/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.remote;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.objectstyle.cayenne.CayenneContext;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.access.ClientServerChannel;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.remote.service.LocalConnection;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable1;
import org.objectstyle.cayenne.testdo.mt.MtTable1;
import org.objectstyle.cayenne.unit.AccessStack;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.CayenneTestResources;

public class RemoteIncrementalFaultListTst extends CayenneTestCase {

    static final int COUNT = 25;

    protected RemoteIncrementalFaultList list;
    protected SelectQuery query;

    protected AccessStack buildAccessStack() {
        return CayenneTestResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    protected void prepareList(int pageSize) throws Exception {

        deleteTestData();
        createTestData("testObjects");

        this.query = new SelectQuery(ClientMtTable1.class);

        // make sure total number of objects is not divisable
        // by the page size, to test the last smaller page
        query.setPageSize(pageSize);
        query.addOrdering("db:" + MtTable1.TABLE1_ID_PK_COLUMN, Ordering.ASC);

        ClientServerChannel serverChannel = new ClientServerChannel(getDomain());
        LocalConnection connection = new LocalConnection(
                serverChannel,
                LocalConnection.HESSIAN_SERIALIZATION);
        ClientChannel clientChannel = new ClientChannel(connection);

        this.list = new RemoteIncrementalFaultList(
                new CayenneContext(clientChannel),
                query);
    }

    public void testSize() throws Exception {
        prepareList(6);
        assertEquals(COUNT, list.size());
    }
    
    public void testIteratorPageSize1() throws Exception {
        doTestIterator(1);
    }
    
    public void testIteratorPageSize5() throws Exception {
        // size divisiable by page size
        doTestIterator(5);
    }

    public void testIteratorPageSize6() throws Exception {
        // size not divisable by page size
        doTestIterator(6);
    }
    
    public void testIteratorPageSize25() throws Exception {
        // size equals to page size
        doTestIterator(COUNT);
    }
    
    public void testIteratorPageSize26() throws Exception {
        // size exceeding page size
        doTestIterator(COUNT + 1);
    }

    public void testListIterator() throws Exception {
        prepareList(6);
        ListIterator it = list.listIterator();

        assertTrue(it.hasNext());

        int counter = 0;
        while (it.hasNext()) {
            Object obj = it.next();
            assertNotNull(obj);
            assertTrue(obj instanceof Persistent);

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

    public void testUnfetchedObjects() throws Exception {
        prepareList(6);
        assertEquals(COUNT - 6, list.getUnfetchedObjects());
        list.get(7);
        assertEquals(COUNT - 12, list.getUnfetchedObjects());
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
        assertTrue(list.elements.get(0) instanceof ClientMtTable1);
        assertSame(RemoteIncrementalFaultList.PLACEHOLDER, list.elements.get(8));

        list.resolveInterval(5, 10);
        assertTrue(list.elements.get(7) instanceof ClientMtTable1);

        list.resolveAll();
        assertTrue((list.elements.get(list.size() - 1)) instanceof ClientMtTable1);
    }

    public void testGet1() throws Exception {
        prepareList(6);
        assertTrue(list.elements.get(0) instanceof ClientMtTable1);
        assertSame(RemoteIncrementalFaultList.PLACEHOLDER, list.elements.get(8));

        Object a = list.get(8);

        assertNotNull(a);
        assertTrue(a instanceof ClientMtTable1);
        assertTrue(list.elements.get(8) instanceof ClientMtTable1);
    }

    public void testIndexOf() throws Exception {
        prepareList(6);

        Expression qual = ExpressionFactory.matchExp(
                ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY,
                "g20");
        SelectQuery query = new SelectQuery(ClientMtTable1.class, qual);
        List artists = list.context.performQuery(query);

        assertEquals(1, artists.size());

        ClientMtTable1 row = (ClientMtTable1) artists.get(0);
        assertEquals(19, list.indexOf(row));
        assertEquals(-1, list.indexOf(list.context.newObject(ClientMtTable1.class)));
    }

    public void testLastIndexOf() throws Exception {
        prepareList(6);
        Expression qual = ExpressionFactory.matchExp(
                ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY,
                "g20");
        SelectQuery query = new SelectQuery(ClientMtTable1.class, qual);
        List objects = list.context.performQuery(query);

        assertEquals(1, objects.size());

        ClientMtTable1 row = (ClientMtTable1) objects.get(0);
        assertEquals(19, list.lastIndexOf(row));
        assertEquals(-1, list.lastIndexOf(list.context.newObject(ClientMtTable1.class)));
    }
    
    private void doTestIterator(int size) throws Exception {
        prepareList(size);
        Iterator it = list.iterator();

        assertTrue(it.hasNext());

        int counter = 0;
        while (it.hasNext()) {
            Object obj = it.next();
            assertNotNull(obj);
            assertTrue(obj instanceof Persistent);

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
}
