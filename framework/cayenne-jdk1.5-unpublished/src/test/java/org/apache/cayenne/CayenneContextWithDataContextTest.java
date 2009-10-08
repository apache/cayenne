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

package org.apache.cayenne;

import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;
import org.apache.cayenne.remote.ClientChannel;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.remote.RemoteIncrementalFaultList;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.testdo.mt.ClientMtMeaningfulPk;
import org.apache.cayenne.testdo.mt.ClientMtReflexive;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.testdo.mt.ClientMtTableBool;
import org.apache.cayenne.testdo.mt.MtReflexive;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.testdo.mt.MtTableBool;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;
import org.apache.cayenne.unit.UnitLocalConnection;

public class CayenneContextWithDataContextTest extends CayenneCase {
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources.getResources().getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testLocalCacheStaysLocal() {

        DataContext context = createDataContext();
        ClientServerChannel clientServerChannel = new ClientServerChannel(context);
        UnitLocalConnection connection = new UnitLocalConnection(
                clientServerChannel,
                LocalConnection.HESSIAN_SERIALIZATION);
        ClientChannel channel = new ClientChannel(connection);
        CayenneContext clientContext = new CayenneContext(channel);

        SelectQuery query = new SelectQuery(ClientMtTable1.class);
        query.setCachePolicy(QueryMetadata.LOCAL_CACHE);

        assertEquals(0, clientContext.getQueryCache().size());
        assertEquals(0, context.getQueryCache().size());

        List<?> results = clientContext.performQuery(query);

        assertEquals(1, clientContext.getQueryCache().size());
        assertSame(results, clientContext.getQueryCache().get(
                query.getMetaData(clientContext.getEntityResolver())));

        assertEquals(0, context.getQueryCache().size());
    }

    public void testSelectPrimitives() {
        insertValue();
        DataContext context = createDataContext();
        ClientServerChannel clientServerChannel = new ClientServerChannel(context);
        UnitLocalConnection connection = new UnitLocalConnection(
                clientServerChannel,
                LocalConnection.HESSIAN_SERIALIZATION);
        ClientChannel channel = new ClientChannel(connection);
        CayenneContext clientContext = new CayenneContext(channel);

        SelectQuery query = new SelectQuery(ClientMtTableBool.class);
        query.addOrdering("db:" + MtTableBool.ID_PK_COLUMN, true);

        List<ClientMtTableBool> results = clientContext.performQuery(query);
        assertTrue(results.get(1).isBlablacheck());
        assertFalse(results.get(4).isBlablacheck());

        assertEquals(1, results.get(1).getNumber());
        assertEquals(5, results.get(5).getNumber());
    }

    public void testCommitChangesPrimitives() {

        DataContext dataContext = createDataContext();

        ClientConnection connection = new LocalConnection(new ClientServerChannel(
                getDomain()));
        ClientChannel channel = new ClientChannel(connection);
        CayenneContext context = new CayenneContext(channel);
        ClientMtTableBool obj = context.newObject(ClientMtTableBool.class);

        obj.setBlablacheck(true);
        obj.setNumber(3);

        context.commitChanges();

        SelectQuery query = new SelectQuery(MtTableBool.class);
        List<MtTableBool> results = dataContext.performQuery(query);

        assertTrue(results.get(0).isBlablacheck());
        assertEquals(3, results.get(0).getNumber());

        obj.setBlablacheck(false);
        obj.setNumber(8);
        context.commitChanges();

        query = new SelectQuery(MtTableBool.class);
        results = dataContext.performQuery(query);

        assertFalse(results.get(0).isBlablacheck());
        assertEquals(8, results.get(0).getNumber());

    }

    public void insertValue() {
        DataContext context = createDataContext();

        MtTableBool obj;

        for (int i = 0; i < 6; i++) {
            if (i < 3) {
                obj = context.newObject(MtTableBool.class);
                obj.setBlablacheck(true);
                obj.setNumber(i);
                context.commitChanges();
            }
            else {
                obj = context.newObject(MtTableBool.class);
                obj.setBlablacheck(false);
                obj.setNumber(i);
                context.commitChanges();
            }
        }
    }

    public void testPostAddCallback() throws Exception {

        ClientServerChannel csChannel = new ClientServerChannel(getDomain());

        // an exception was triggered within POST_LOAD callback
        LifecycleCallbackRegistry callbackRegistry = csChannel
                .getEntityResolver()
                .getCallbackRegistry();

        final boolean[] flag = new boolean[1];

        try {
            callbackRegistry.addListener(MtTable1.class, new LifecycleListener() {

                public void postLoad(Object entity) {
                }

                public void postPersist(Object entity) {
                }

                public void postRemove(Object entity) {
                }

                public void postUpdate(Object entity) {
                }

                public void postAdd(Object entity) {
                    flag[0] = true;
                }

                public void preRemove(Object entity) {
                }

                public void preUpdate(Object entity) {
                }

				public void prePersist(Object entity) {
				}
            });

            ClientConnection connection = new LocalConnection(csChannel);
            ClientChannel channel = new ClientChannel(connection);

            CayenneContext context = new CayenneContext(channel);

            context.newObject(ClientMtTable1.class);

            assertFalse(flag[0]);
            context.commitChanges();
            assertTrue(flag[0]);
        }
        finally {
            callbackRegistry.clear();
        }
    }

    class TestClientServerChannel extends ClientServerChannel {

        TestClientServerChannel(DataDomain domain) {
            super(domain);
        }

        public ObjectContext getServerContext() {
            return serverContext;
        }
    }

    public void testPostAddOnObjectCallback() throws Exception {

        TestClientServerChannel csChannel = new TestClientServerChannel(getDomain());

        // an exception was triggered within POST_LOAD callback
        LifecycleCallbackRegistry callbackRegistry = csChannel
                .getEntityResolver()
                .getCallbackRegistry();

        try {
            callbackRegistry.addListener(
                    LifecycleEvent.POST_ADD,
                    MtTable1.class,
                    "prePersistMethod");

            ClientConnection connection = new LocalConnection(csChannel);
            ClientChannel channel = new ClientChannel(connection);

            CayenneContext context = new CayenneContext(channel);

            Persistent clientObject = context.newObject(ClientMtTable1.class);

            context.commitChanges();

            // find peer
            MtTable1 peer = (MtTable1) csChannel
                    .getServerContext()
                    .getGraphManager()
                    .getNode(clientObject.getObjectId());

            assertTrue(peer.isPrePersisted());
        }
        finally {
            callbackRegistry.clear();
        }
    }

    public void testPreRemoveCallback() throws Exception {

        ClientServerChannel csChannel = new ClientServerChannel(getDomain());

        // an exception was triggered within POST_LOAD callback
        LifecycleCallbackRegistry callbackRegistry = csChannel
                .getEntityResolver()
                .getCallbackRegistry();

        final boolean[] flag = new boolean[1];

        try {
            callbackRegistry.addListener(MtTable1.class, new LifecycleListener() {

                public void postLoad(Object entity) {
                }

                public void postPersist(Object entity) {
                }

                public void postRemove(Object entity) {
                }

                public void postUpdate(Object entity) {
                }

                public void postAdd(Object entity) {
                }

                public void preRemove(Object entity) {
                    flag[0] = true;
                }

                public void preUpdate(Object entity) {
                }

				public void prePersist(Object entity) {
				}
            });

            ClientConnection connection = new LocalConnection(csChannel);
            ClientChannel channel = new ClientChannel(connection);

            CayenneContext context = new CayenneContext(channel);

            ClientMtTable1 object = context.newObject(ClientMtTable1.class);

            assertFalse(flag[0]);
            context.commitChanges();
            assertFalse(flag[0]);

            context.deleteObject(object);
            context.commitChanges();
            assertTrue(flag[0]);
        }
        finally {
            callbackRegistry.clear();
        }
    }

    public void testCAY830() throws Exception {

        deleteTestData();

        ClientServerChannel csChannel = new ClientServerChannel(getDomain());

        // an exception was triggered within POST_LOAD callback
        LifecycleCallbackRegistry callbackRegistry = csChannel
                .getEntityResolver()
                .getCallbackRegistry();

        try {
            callbackRegistry.addListener(MtReflexive.class, new LifecycleListener() {

                public void postLoad(Object entity) {
                }

                public void postPersist(Object entity) {
                }

                public void postRemove(Object entity) {
                }

                public void postUpdate(Object entity) {
                }

                public void postAdd(Object entity) {
                }

                public void preRemove(Object entity) {
                }

                public void preUpdate(Object entity) {
                }

				public void prePersist(Object entity) {
				}
            });

            ClientConnection connection = new LocalConnection(csChannel);
            ClientChannel channel = new ClientChannel(connection);

            CayenneContext context = new CayenneContext(channel);

            ClientMtReflexive o1 = context.newObject(ClientMtReflexive.class);
            o1.setName("parent");

            ClientMtReflexive o2 = context.newObject(ClientMtReflexive.class);
            o2.setName("child");
            o2.setToParent(o1);
            context.commitChanges();

            context.deleteObject(o1);
            context.deleteObject(o2);
            context.commitChanges();
            // per CAY-830 an exception is thrown here
        }
        finally {
            callbackRegistry.clear();
        }
    }

    public void testRollbackChanges() throws Exception {
        ClientConnection connection = new LocalConnection(new ClientServerChannel(
                getDomain()));
        ClientChannel channel = new ClientChannel(connection);

        CayenneContext context = new CayenneContext(channel);

        ClientMtTable1 o = context.newObject(ClientMtTable1.class);
        o.setGlobalAttribute1("1");
        context.commitChanges();

        assertEquals("1", o.getGlobalAttribute1());
        o.setGlobalAttribute1("2");
        assertEquals("2", o.getGlobalAttribute1());
        context.rollbackChanges();

        // CAY-1103 - uncommenting this assertion demonstrates the problem
        // assertEquals("1", o.getGlobalAttribute1());

        assertTrue(context.modifiedObjects().isEmpty());
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

        // make sure we haven't tripped the fault yet
        assertEquals(PersistenceState.HOLLOW, o.getPersistenceState());

        // try tripping fault
        assertEquals("g1", o.getGlobalAttribute1());
        assertEquals(PersistenceState.COMMITTED, o.getPersistenceState());
    }

    public void testCreateBadFault() throws Exception {
        deleteTestData();
        createTestData("prepare");

        // this clears domain cache
        createDataContext();

        UnitLocalConnection connection = new UnitLocalConnection(new ClientServerChannel(
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

    public void testMeaningfulPK() throws Exception {
        createTestData("testMeaningfulPK");

        SelectQuery query = new SelectQuery(ClientMtMeaningfulPk.class);
        query.addOrdering(ClientMtMeaningfulPk.PK_PROPERTY, Ordering.DESC);

        UnitLocalConnection connection = new UnitLocalConnection(new ClientServerChannel(
                getDomain()), LocalConnection.HESSIAN_SERIALIZATION);
        ClientChannel channel = new ClientChannel(connection);
        CayenneContext context = new CayenneContext(channel);

        List results = context.performQuery(query);
        assertEquals(2, results.size());
    }

    public void testPrefetchingToOne() throws Exception {
        createTestData("testPrefetching");

        UnitLocalConnection connection = new UnitLocalConnection(new ClientServerChannel(
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

        UnitLocalConnection connection = new UnitLocalConnection(new ClientServerChannel(
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

        UnitLocalConnection connection = new UnitLocalConnection(new ClientServerChannel(
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

        UnitLocalConnection connection = new UnitLocalConnection(new ClientServerChannel(
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

        UnitLocalConnection connection = new UnitLocalConnection(new ClientServerChannel(
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

        UnitLocalConnection connection = new UnitLocalConnection(new ClientServerChannel(
                getDomain()));
        ClientChannel channel = new ClientChannel(connection);
        CayenneContext context = new CayenneContext(channel);

        ClientMtTable1 o = context.newObject(ClientMtTable1.class);
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
