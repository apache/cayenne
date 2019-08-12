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

package org.apache.cayenne;

import java.sql.Types;
import java.util.List;

import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;
import org.apache.cayenne.remote.RemoteIncrementalFaultList;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.test.parallel.ParallelTestContainer;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

@UseServerRuntime(CayenneProjects.MULTI_TIER_PROJECT)
public class CayenneContextWithDataContextIT extends ClientCase {

    @Inject
    private CayenneContext clientContext;

    @Inject
    private DBHelper dbHelper;

    @Inject(ClientCase.ROP_CLIENT_KEY)
    private DataChannelInterceptor clientServerInterceptor;

    @Inject
    private ClientServerChannel clientServerChannel;

    private TableHelper tMtTable1;
    private TableHelper tMtTable2;

    @Before
    public void setUp() throws Exception {
        tMtTable1 = new TableHelper(dbHelper, "MT_TABLE1");
        tMtTable1.setColumns("TABLE1_ID", "GLOBAL_ATTRIBUTE1", "SERVER_ATTRIBUTE1");

        tMtTable2 = new TableHelper(dbHelper, "MT_TABLE2");
        tMtTable2.setColumns("TABLE2_ID", "TABLE1_ID", "GLOBAL_ATTRIBUTE").setColumnTypes(
                Types.INTEGER, Types.INTEGER, Types.VARCHAR);
    }

    private void createTwoMtTable1sAnd2sDataSet() throws Exception {
        tMtTable1.insert(1, "g1", "s1");
        tMtTable1.insert(2, "g2", "s2");

        tMtTable2.insert(1, 1, "g1");
        tMtTable2.insert(2, 1, "g2");
    }

    private void createEightMtTable1s() throws Exception {
        for (int i = 1; i <= 8; i++) {
            tMtTable1.insert(i, "g" + i, "s" + i);
        }
    }

    @Test
    public void testLocalCacheStaysLocal() {
        ObjectSelect<ClientMtTable1> query = ObjectSelect.query(ClientMtTable1.class).localCache();
        List<?> results = query.select(clientContext);

        assertSame(results, clientContext.getQueryCache().get(query.getMetaData(clientContext.getEntityResolver())));
    }

    @Test
    public void testAddToList() {

        ClientMtTable1 t1 = clientContext.newObject(ClientMtTable1.class);
        ClientMtTable2 t2 = clientContext.newObject(ClientMtTable2.class);

        t1.addToTable2Array(t2);
        assertEquals(1, t1.getTable2Array().size());
        assertSame(t1, t2.getTable1());

        // do it again to make sure action can handle series of changes
        ClientMtTable1 t3 = clientContext.newObject(ClientMtTable1.class);
        ClientMtTable2 t4 = clientContext.newObject(ClientMtTable2.class);

        t3.addToTable2Array(t4);
        assertEquals(1, t3.getTable2Array().size());
        assertSame(t3, t4.getTable1());
    }

    @Test
    public void testSetValueHolder() {

        ClientMtTable1 t1 = clientContext.newObject(ClientMtTable1.class);
        ClientMtTable2 t2 = clientContext.newObject(ClientMtTable2.class);

        t2.setTable1(t1);
        assertEquals(1, t1.getTable2Array().size());
        assertSame(t1, t2.getTable1());
    }

    @Test
    public void testPostAddCallback() {

        LifecycleCallbackRegistry callbackRegistry = clientServerChannel
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

            clientContext.newObject(ClientMtTable1.class);

            assertFalse(flag[0]);
            clientContext.commitChanges();
            assertTrue(flag[0]);
        }
        finally {
            callbackRegistry.clear();
        }
    }

    @Test
    public void testPostAddOnObjectCallback() throws Exception {

        final DataContext serverContext = (DataContext) clientServerChannel.getParentChannel();

        LifecycleCallbackRegistry callbackRegistry = serverContext
                .getEntityResolver()
                .getCallbackRegistry();

        try {
            callbackRegistry.addCallback(
                    LifecycleEvent.POST_ADD,
                    MtTable1.class,
                    "prePersistMethod");

            final Persistent clientObject = clientContext.newObject(ClientMtTable1.class);
            clientContext.commitChanges();

        new ParallelTestContainer() {

            @Override
            protected void assertResult() {
            	// find peer
            	MtTable1 peer = (MtTable1) serverContext.getGraphManager().getNode(clientObject.getObjectId());
            	assertNotNull(peer);
            	assertTrue(peer.isPrePersisted());
            }
        }.runTest(1000);


        }
        finally {
            callbackRegistry.clear();
        }
    }

    @Test
    public void testPreRemoveCallback() {

        // an exception was triggered within POST_LOAD callback
        LifecycleCallbackRegistry callbackRegistry = clientServerChannel
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

            ClientMtTable1 object = clientContext.newObject(ClientMtTable1.class);

            assertFalse(flag[0]);
            clientContext.commitChanges();
            assertFalse(flag[0]);

            clientContext.deleteObjects(object);
            clientContext.commitChanges();
            assertTrue(flag[0]);
        }
        finally {
            callbackRegistry.clear();
        }
    }

    @Test
    public void testRollbackChanges() {

        ClientMtTable1 o = clientContext.newObject(ClientMtTable1.class);
        o.setGlobalAttribute1("1");
        clientContext.commitChanges();

        assertEquals("1", o.getGlobalAttribute1());
        o.setGlobalAttribute1("2");
        assertEquals("2", o.getGlobalAttribute1());
        clientContext.rollbackChanges();

        assertEquals("1", o.getGlobalAttribute1());
        assertTrue(clientContext.modifiedObjects().isEmpty());
    }

    @Test
    public void testCreateFault() throws Exception {
        tMtTable1.insert(1, "g1", "s1");

        ObjectId id = ObjectId.of("MtTable1", MtTable1.TABLE1_ID_PK_COLUMN, 1);

        Object fault = clientContext.createFault(id);
        assertTrue(fault instanceof ClientMtTable1);

        ClientMtTable1 o = (ClientMtTable1) fault;
        assertEquals(PersistenceState.HOLLOW, o.getPersistenceState());
        assertSame(clientContext, o.getObjectContext());
        assertNull(o.getGlobalAttribute1Direct());

        // make sure we haven't tripped the fault yet
        assertEquals(PersistenceState.HOLLOW, o.getPersistenceState());

        // try tripping fault
        assertEquals("g1", o.getGlobalAttribute1());
        assertEquals(PersistenceState.COMMITTED, o.getPersistenceState());
    }

    @Test
    public void testCreateBadFault() throws Exception {
        tMtTable1.insert(1, "g1", "s1");

        ObjectId id = ObjectId.of("MtTable1", MtTable1.TABLE1_ID_PK_COLUMN, 2);

        Object fault = clientContext.createFault(id);
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

    @Test
    public void testPrefetchingToOne() throws Exception {
        createTwoMtTable1sAnd2sDataSet();

        final ObjectId prefetchedId = ObjectId.of(
                "MtTable1",
                MtTable1.TABLE1_ID_PK_COLUMN,
                1);

        ObjectSelect<ClientMtTable2> q = ObjectSelect.query(ClientMtTable2.class)
                .orderBy(ClientMtTable2.GLOBAL_ATTRIBUTE.asc())
                .prefetch(ClientMtTable2.TABLE1.disjoint());

        final List<ClientMtTable2> results = q.select(clientContext);

        clientServerInterceptor.runWithQueriesBlocked(() -> {
            assertEquals(2, results.size());

            for (ClientMtTable2 o : results) {
                assertEquals(PersistenceState.COMMITTED, o.getPersistenceState());
                assertSame(clientContext, o.getObjectContext());

                ClientMtTable1 o1 = o.getTable1();
                assertNotNull(o1);
                assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
                assertSame(clientContext, o1.getObjectContext());
                assertEquals(prefetchedId, o1.getObjectId());
            }
        });
    }

    @Test
    public void testPrefetchingToOneNull() throws Exception {
        tMtTable2.insert(15, null, "g3");

        ObjectSelect<ClientMtTable2> q = ObjectSelect.query(ClientMtTable2.class)
                .prefetch(ClientMtTable2.TABLE1.disjoint());

        final List<ClientMtTable2> results = q.select(clientContext);

        clientServerInterceptor.runWithQueriesBlocked(() -> {
            assertEquals(1, results.size());

            ClientMtTable2 o = results.get(0);
            assertEquals(PersistenceState.COMMITTED, o.getPersistenceState());
            assertSame(clientContext, o.getObjectContext());

            assertNull(o.getTable1());
        });
    }

    @Test
    public void testPrefetchingToMany() throws Exception {
        createTwoMtTable1sAnd2sDataSet();

        ObjectSelect<ClientMtTable1> q = ObjectSelect.query(ClientMtTable1.class)
                .orderBy(ClientMtTable1.GLOBAL_ATTRIBUTE1.asc())
                .prefetch(ClientMtTable1.TABLE2ARRAY.joint());

        final List<ClientMtTable1> results = q.select(clientContext);

        clientServerInterceptor.runWithQueriesBlocked(() -> {

            ClientMtTable1 o1 = results.get(0);
            assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
            assertSame(clientContext, o1.getObjectContext());

            List<ClientMtTable2> children1 = o1.getTable2Array();

            assertEquals(2, children1.size());
            for (ClientMtTable2 o : children1) {
                assertEquals(PersistenceState.COMMITTED, o.getPersistenceState());
                assertSame(clientContext, o.getObjectContext());

                // TODO: fixme... reverse relationship is not connected and will
                // cause a fetch
                // assertEquals(o1, o.getTable1());
            }
        });
    }

    @Test
    public void testPerformPaginatedQuery() throws Exception {
        createEightMtTable1s();

        ObjectSelect<ClientMtTable1> query = ObjectSelect.query(ClientMtTable1.class)
                .pageSize(5);
        List<ClientMtTable1> objects = query.select(clientContext);
        assertNotNull(objects);
        assertTrue(objects instanceof RemoteIncrementalFaultList);
    }

    @Test
    public void testPrefetchingToManyEmpty() throws Exception {
        createTwoMtTable1sAnd2sDataSet();
        
        ObjectSelect<ClientMtTable1> q = ObjectSelect.query(ClientMtTable1.class)
                .orderBy(ClientMtTable1.GLOBAL_ATTRIBUTE1.asc())
                .prefetch(ClientMtTable1.TABLE2ARRAY.joint());

        final List<ClientMtTable1> results = q.select(clientContext);

        clientServerInterceptor.runWithQueriesBlocked(() -> {
            ClientMtTable1 o2 = results.get(1);
            assertEquals(PersistenceState.COMMITTED, o2.getPersistenceState());
            assertSame(clientContext, o2.getObjectContext());

            List<ClientMtTable2> children2 = o2.getTable2Array();
            assertFalse(((ValueHolder) children2).isFault());
            assertEquals(0, children2.size());
        });
    }

    @Test
    public void testOIDQueryInterception() {

        final ClientMtTable1 o = clientContext.newObject(ClientMtTable1.class);
        o.setGlobalAttribute1("aaa");

        // fetch new
        final ObjectIdQuery q1 = new ObjectIdQuery(o.getObjectId(), false, ObjectIdQuery.CACHE);

        clientServerInterceptor.runWithQueriesBlocked(() -> {
            List<?> objects = clientContext.performQuery(q1);
            assertEquals(1, objects.size());
            assertSame(o, objects.get(0));
        });

        clientContext.commitChanges();

        // fetch committed
        final ObjectIdQuery q2 = new ObjectIdQuery(o.getObjectId(), false, ObjectIdQuery.CACHE);

        clientServerInterceptor.runWithQueriesBlocked(() -> {
            List<?> objects = clientContext.performQuery(q2);
            assertEquals(1, objects.size());
            assertSame(o, objects.get(0));
        });
    }
}
