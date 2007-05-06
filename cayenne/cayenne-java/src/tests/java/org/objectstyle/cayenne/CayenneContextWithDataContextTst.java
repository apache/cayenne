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
package org.objectstyle.cayenne;

import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.access.ClientServerChannel;
import org.objectstyle.cayenne.query.ObjectIdQuery;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.remote.ClientChannel;
import org.objectstyle.cayenne.remote.ClientConnection;
import org.objectstyle.cayenne.remote.RemoteIncrementalFaultList;
import org.objectstyle.cayenne.remote.service.LocalConnection;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable1;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable2;
import org.objectstyle.cayenne.testdo.mt.MtTable1;
import org.objectstyle.cayenne.unit.AccessStack;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.CayenneTestResources;
import org.objectstyle.cayenne.unit.TestLocalConnection;
import org.objectstyle.cayenne.util.PersistentObjectHolder;
import org.objectstyle.cayenne.util.PersistentObjectList;

public class CayenneContextWithDataContextTst extends CayenneTestCase {

    protected AccessStack buildAccessStack() {
        return CayenneTestResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testNewObjectShouldInflateHolders() {

        CayenneContext context = new CayenneContext(new MockDataChannel());
        context.setEntityResolver(getDomain()
                .getEntityResolver()
                .getClientEntityResolver());

        // test that holders are present and that they are resolved... (new object has no
        // relationships by definition, so no need to keep holders as faults).

        // to one
        ClientMtTable2 o1 = (ClientMtTable2) context.newObject(ClientMtTable2.class);
        assertNotNull(o1.getTable1Direct());
        assertFalse(((PersistentObjectHolder) o1.getTable1Direct()).isFault());

        // to many
        ClientMtTable1 o2 = (ClientMtTable1) context.newObject(ClientMtTable1.class);
        assertNotNull(o2.getTable2ArrayDirect());

        assertFalse(((PersistentObjectList) o2.getTable2ArrayDirect()).isFault());
    }

    public void testCreateFault() throws Exception {
        createTestData("prepare");

        // must attach to the real channel...
        ClientConnection connection = new LocalConnection(new ClientServerChannel(
                getDomain()));
        ClientChannel channel = new ClientChannel(connection);

        CayenneContext context = new CayenneContext(channel);
        ObjectId id = new ObjectId("MtTable1", MtTable1.TABLE1_ID_PK_COLUMN, new Integer(
                1));

        Object fault = context.createFault(id);
        assertTrue(fault instanceof ClientMtTable1);

        ClientMtTable1 o = (ClientMtTable1) fault;
        assertEquals(PersistenceState.HOLLOW, o.getPersistenceState());
        assertSame(context, o.getObjectContext());
        assertNull(o.getGlobalAttribute1Direct());

        // make sure value holders are set but not resolved
        assertNotNull(o.getTable2ArrayDirect());
        assertTrue(((PersistentObjectList) o.getTable2ArrayDirect()).isFault());

        // make sure we haven't tripped the fault yet
        assertEquals(PersistenceState.HOLLOW, o.getPersistenceState());

        // try tripping fault
        assertEquals("g1", o.getGlobalAttribute1());
        assertEquals(PersistenceState.COMMITTED, o.getPersistenceState());
    }

    public void testCreateBadFault() throws Exception {
        createTestData("prepare");

        TestLocalConnection connection = new TestLocalConnection(new ClientServerChannel(
                getDomain()), LocalConnection.HESSIAN_SERIALIZATION);
        ClientChannel channel = new ClientChannel(connection);
        CayenneContext context = new CayenneContext(channel);

        ObjectId id = new ObjectId("MtTable1", MtTable1.TABLE1_ID_PK_COLUMN, new Integer(
                2));

        Object fault = context.createFault(id);
        assertTrue(fault instanceof ClientMtTable1);

        ClientMtTable1 o = (ClientMtTable1) fault;

        // try tripping fault
        try {
            o.getGlobalAttribute1();
            fail("resolving bad fault should've thrown");
        }
        catch (FaultFailureException e) {
            // expected
        }
    }

    public void testPrefetchingToOne() throws Exception {
        createTestData("testPrefetching");

        TestLocalConnection connection = new TestLocalConnection(new ClientServerChannel(
                getDomain()), LocalConnection.HESSIAN_SERIALIZATION);
        ClientChannel channel = new ClientChannel(connection);
        CayenneContext context = new CayenneContext(channel);

        ObjectId prefetchedId = new ObjectId(
                "MtTable1",
                MtTable1.TABLE1_ID_PK_COLUMN,
                new Integer(1));

        SelectQuery q = new SelectQuery(ClientMtTable2.class);
        q.addOrdering(ClientMtTable2.GLOBAL_ATTRIBUTE_PROPERTY, true);
        q.addPrefetch(ClientMtTable2.TABLE1_PROPERTY);

        List results = context.performQuery(q);

        connection.setBlockingMessages(true);
        try {

            assertEquals(2, results.size());
            Iterator it = results.iterator();
            while (it.hasNext()) {
                ClientMtTable2 o = (ClientMtTable2) it.next();
                assertEquals(PersistenceState.COMMITTED, o.getPersistenceState());
                assertSame(context, o.getObjectContext());

                ClientMtTable1 o1 = o.getTable1();
                assertNotNull(o1);
                assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
                assertSame(context, o1.getObjectContext());
                assertEquals(prefetchedId, o1.getObjectId());
            }
        }
        finally {
            connection.setBlockingMessages(false);
        }
    }

    public void testPrefetchingToOneNull() throws Exception {
        createTestData("testPrefetchingToOneNull");

        TestLocalConnection connection = new TestLocalConnection(new ClientServerChannel(
                getDomain()), LocalConnection.HESSIAN_SERIALIZATION);
        ClientChannel channel = new ClientChannel(connection);
        CayenneContext context = new CayenneContext(channel);

        SelectQuery q = new SelectQuery(ClientMtTable2.class);
        q.addPrefetch(ClientMtTable2.TABLE1_PROPERTY);

        List results = context.performQuery(q);

        connection.setBlockingMessages(true);
        try {

            assertEquals(1, results.size());

            ClientMtTable2 o = (ClientMtTable2) results.get(0);
            assertEquals(PersistenceState.COMMITTED, o.getPersistenceState());
            assertSame(context, o.getObjectContext());

            assertNull(o.getTable1());
        }
        finally {
            connection.setBlockingMessages(false);
        }
    }

    public void testPrefetchingToMany() throws Exception {
        createTestData("testPrefetching");

        TestLocalConnection connection = new TestLocalConnection(new ClientServerChannel(
                getDomain()), LocalConnection.HESSIAN_SERIALIZATION);
        ClientChannel channel = new ClientChannel(connection);
        CayenneContext context = new CayenneContext(channel);

        SelectQuery q = new SelectQuery(ClientMtTable1.class);
        q.addOrdering(ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY, true);
        q.addPrefetch(ClientMtTable1.TABLE2ARRAY_PROPERTY);

        List results = context.performQuery(q);

        connection.setBlockingMessages(true);
        try {

            ClientMtTable1 o1 = (ClientMtTable1) results.get(0);
            assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
            assertSame(context, o1.getObjectContext());

            List children1 = o1.getTable2Array();

            assertEquals(2, children1.size());
            Iterator it = children1.iterator();
            while (it.hasNext()) {
                ClientMtTable2 o = (ClientMtTable2) it.next();
                assertEquals(PersistenceState.COMMITTED, o.getPersistenceState());
                assertSame(context, o.getObjectContext());

                // TODO: fixme...
                // assertEquals(o1, o.getTable1());
            }
        }
        finally {
            connection.setBlockingMessages(false);
        }
    }

    public void testPerformPaginatedQuery() throws Exception {
        deleteTestData();
        createTestData("testPerformPaginatedQuery");

        TestLocalConnection connection = new TestLocalConnection(new ClientServerChannel(
                getDomain()), LocalConnection.HESSIAN_SERIALIZATION);
        ClientChannel channel = new ClientChannel(connection);
        CayenneContext context = new CayenneContext(channel);

        SelectQuery query = new SelectQuery(ClientMtTable1.class);
        query.setPageSize(5);
        List objects = context.performQuery(query);
        assertNotNull(objects);
        assertTrue(objects instanceof RemoteIncrementalFaultList);
    }

    public void testPrefetchingToManyEmpty() throws Exception {
        createTestData("testPrefetching");

        TestLocalConnection connection = new TestLocalConnection(new ClientServerChannel(
                getDomain()), LocalConnection.HESSIAN_SERIALIZATION);
        ClientChannel channel = new ClientChannel(connection);
        CayenneContext context = new CayenneContext(channel);

        SelectQuery q = new SelectQuery(ClientMtTable1.class);
        q.addOrdering(ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY, true);
        q.addPrefetch(ClientMtTable1.TABLE2ARRAY_PROPERTY);

        List results = context.performQuery(q);

        connection.setBlockingMessages(true);
        try {

            ClientMtTable1 o2 = (ClientMtTable1) results.get(1);
            assertEquals(PersistenceState.COMMITTED, o2.getPersistenceState());
            assertSame(context, o2.getObjectContext());

            List children2 = o2.getTable2Array();
            assertFalse(((ValueHolder) children2).isFault());
            assertEquals(0, children2.size());
        }
        finally {
            connection.setBlockingMessages(false);
        }
    }

    public void testOIDQueryInterception() throws Exception {

        deleteTestData();

        TestLocalConnection connection = new TestLocalConnection(new ClientServerChannel(
                getDomain()));
        ClientChannel channel = new ClientChannel(connection);
        CayenneContext context = new CayenneContext(channel);

        ClientMtTable1 o = (ClientMtTable1) context.newObject(ClientMtTable1.class);
        o.setGlobalAttribute1("aaa");

        // fetch new
        ObjectIdQuery q1 = new ObjectIdQuery(o.getObjectId(), false, ObjectIdQuery.CACHE);

        connection.setBlockingMessages(true);
        try {
            List objects = context.performQuery(q1);
            assertEquals(1, objects.size());
            assertSame(o, objects.get(0));
        }
        finally {
            connection.setBlockingMessages(false);
        }

        context.commitChanges();

        // fetch committed
        ObjectIdQuery q2 = new ObjectIdQuery(o.getObjectId(), false, ObjectIdQuery.CACHE);

        connection.setBlockingMessages(true);
        try {
            List objects = context.performQuery(q2);
            assertEquals(1, objects.size());
            assertSame(o, objects.get(0));
        }
        finally {
            connection.setBlockingMessages(false);
        }
    }
}
