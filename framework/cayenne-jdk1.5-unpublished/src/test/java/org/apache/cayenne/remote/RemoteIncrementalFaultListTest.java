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

package org.apache.cayenne.remote;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;

public class RemoteIncrementalFaultListTest extends CayenneCase {

    static final int COUNT = 25;

    protected RemoteIncrementalFaultList list;
    protected SelectQuery query;

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources
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
