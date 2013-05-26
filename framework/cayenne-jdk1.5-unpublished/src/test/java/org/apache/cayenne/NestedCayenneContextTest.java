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

import org.apache.cayenne.configuration.rop.client.ClientRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.remote.RemoteCayenneCase;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.testdo.mt.ClientMtTooneDep;
import org.apache.cayenne.testdo.mt.ClientMtTooneMaster;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

/**
 * Tests nested object contexts
 */
@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class NestedCayenneContextTest extends RemoteCayenneCase {

    @Inject
    private ClientRuntime runtime;
    
    @Inject
    private DBHelper dbHelper;

    @Inject
    private DataChannelInterceptor queryInterceptor;

    @Override
    public void setUpAfterInjection() throws Exception {
        super.setUpAfterInjection();

        dbHelper.deleteAll("MT_TABLE2");
        dbHelper.deleteAll("MT_TABLE1");
    }

    public void testChannels() {
        ObjectContext child = runtime.newContext(clientContext);

        assertNotNull(child);
        assertSame(clientContext, child.getChannel());

        // second level of nesting
        ObjectContext grandchild = runtime.newContext((DataChannel) child);

        assertNotNull(grandchild);
        assertSame(child, grandchild.getChannel());
    }

    public void testSelect() throws Exception {
        ObjectContext child = runtime.newContext(clientContext);

        ClientMtTable1 committed = clientContext.newObject(ClientMtTable1.class);
        ClientMtTable1 deleted = clientContext.newObject(ClientMtTable1.class);
        ClientMtTable1 modified = clientContext.newObject(ClientMtTable1.class);

        clientContext.commitChanges();
        int modifiedid = Cayenne.intPKForObject(modified);

        // test how different object states appear in the child on select

        clientContext.deleteObjects(deleted);
        modified.setGlobalAttribute1("a");

        ClientMtTable1 _new = clientContext.newObject(ClientMtTable1.class);

        assertEquals(PersistenceState.COMMITTED, committed.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, modified.getPersistenceState());
        assertEquals(PersistenceState.DELETED, deleted.getPersistenceState());
        assertEquals(PersistenceState.NEW, _new.getPersistenceState());

        List<?> objects = child.performQuery(new SelectQuery(ClientMtTable1.class));
        assertEquals("All but NEW object must have been included", 3, objects.size());

        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            ClientMtTable1 next = (ClientMtTable1) it.next();
            assertEquals(PersistenceState.COMMITTED, next.getPersistenceState());

            int id = Cayenne.intPKForObject(next);
            if (id == modifiedid) {
                assertEquals("a", next.getGlobalAttribute1());
            }
        }
    }

    public void testPrefetchingToOne() throws Exception {
        final ClientMtTable1 mt11 = clientContext.newObject(ClientMtTable1.class);
        clientContext.newObject(ClientMtTable1.class);
        ClientMtTable2 mt21 = clientContext.newObject(ClientMtTable2.class);
        ClientMtTable2 mt22 = clientContext.newObject(ClientMtTable2.class);

        mt21.setTable1(mt11);
        mt22.setTable1(mt11);

        clientContext.commitChanges();

        final ObjectContext child = runtime.newContext(clientContext);

        SelectQuery q = new SelectQuery(ClientMtTable2.class);
        q.addPrefetch(ClientMtTable2.TABLE1_PROPERTY);

        final List<?> results = child.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(2, results.size());
                Iterator<?> it = results.iterator();
                while (it.hasNext()) {
                    ClientMtTable2 o = (ClientMtTable2) it.next();
                    assertEquals(PersistenceState.COMMITTED, o.getPersistenceState());
                    assertSame(child, o.getObjectContext());

                    ClientMtTable1 o1 = o.getTable1();
                    assertNotNull(o1);
                    assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
                    assertSame(child, o1.getObjectContext());
                    assertEquals(mt11.getObjectId(), o1.getObjectId());
                }
            }
        });
    }

    public void testPrefetchingToMany() throws Exception {
        ClientMtTable1 mt11 = clientContext.newObject(ClientMtTable1.class);
        mt11.setGlobalAttribute1("1");

        ClientMtTable1 mt12 = clientContext.newObject(ClientMtTable1.class);
        mt12.setGlobalAttribute1("2");

        ClientMtTable2 mt21 = clientContext.newObject(ClientMtTable2.class);
        ClientMtTable2 mt22 = clientContext.newObject(ClientMtTable2.class);

        mt21.setTable1(mt11);
        mt22.setTable1(mt11);

        clientContext.commitChanges();

        final ObjectContext child = runtime.newContext(clientContext);

        SelectQuery q = new SelectQuery(ClientMtTable1.class);
        q.addOrdering("globalAttribute1", SortOrder.ASCENDING);
        q.addPrefetch(ClientMtTable1.TABLE2ARRAY_PROPERTY);

        final List<?> results = child.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                ClientMtTable1 o1 = (ClientMtTable1) results.get(0);
                assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
                assertSame(child, o1.getObjectContext());

                List<ClientMtTable2> children1 = o1.getTable2Array();

                assertEquals(2, children1.size());
                Iterator<ClientMtTable2> it = children1.iterator();
                while (it.hasNext()) {
                    ClientMtTable2 o = it.next();
                    assertEquals(PersistenceState.COMMITTED, o.getPersistenceState());
                    assertSame(child, o.getObjectContext());

                    assertEquals(o1, o.getTable1());
                }

                ClientMtTable1 o2 = (ClientMtTable1) results.get(1);
                assertEquals(PersistenceState.COMMITTED, o2.getPersistenceState());
                assertSame(child, o2.getObjectContext());

                List<?> children2 = o2.getTable2Array();

                assertEquals(0, children2.size());
            }
        });
    }

    public void testDeleteNew() throws Exception {
        ObjectContext child = runtime.newContext(clientContext);

        ClientMtTable1 a = clientContext.newObject(ClientMtTable1.class);
        clientContext.commitChanges();

        ClientMtTable2 p = child.newObject(ClientMtTable2.class);
        ClientMtTable1 aChild = (ClientMtTable1) Cayenne.objectForPK(
                child,
                a.getObjectId());
        p.setGlobalAttribute("X");
        aChild.addToTable2Array(p);

        child.commitChangesToParent();

        child.deleteObjects(p);
        aChild.removeFromTable2Array(p);

        child.commitChangesToParent();
    }

    /**
     * A test case for CAY-698 bug.
     */
    public void testNullifyToOne() throws Exception {
        ClientMtTable1 a = clientContext.newObject(ClientMtTable1.class);
        ClientMtTable2 b = clientContext.newObject(ClientMtTable2.class);
        a.addToTable2Array(b);

        clientContext.commitChanges();

        final ObjectContext child = runtime.newContext(clientContext);
        ObjectContext childPeer = runtime.newContext(clientContext);

        final ClientMtTable2 childP1 = (ClientMtTable2) Cayenne.objectForPK(
                child,
                b.getObjectId());

        // trigger object creation in the peer nested DC
        Cayenne.objectForPK(childPeer, b.getObjectId());
        childP1.setTable1(null);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                child.commitChangesToParent();
                assertEquals(PersistenceState.COMMITTED, childP1.getPersistenceState());

                ClientMtTable2 parentP1 = (ClientMtTable2) clientContext
                        .getGraphManager()
                        .getNode(childP1.getObjectId());

                assertNotNull(parentP1);
                assertEquals(PersistenceState.MODIFIED, parentP1.getPersistenceState());
                assertNull(parentP1.getTable1());

                // check that arc changes got recorded in the parent context
                GraphDiff diffs = clientContext.internalGraphManager().getDiffs();
                final int[] arcDiffs = new int[1];

                diffs.apply(new GraphChangeHandler() {

                    public void arcCreated(
                            Object nodeId,
                            Object targetNodeId,
                            Object arcId) {
                        arcDiffs[0]++;
                    }

                    public void arcDeleted(
                            Object nodeId,
                            Object targetNodeId,
                            Object arcId) {
                        arcDiffs[0]--;
                    }

                    public void nodeCreated(Object nodeId) {

                    }

                    public void nodeIdChanged(Object nodeId, Object newId) {
                    }

                    public void nodePropertyChanged(
                            Object nodeId,
                            String property,
                            Object oldValue,
                            Object newValue) {
                    }

                    public void nodeRemoved(Object nodeId) {

                    }
                });

                assertEquals(-2, arcDiffs[0]);
            }
        });
    }

    public void testCommitChangesToParent() throws Exception {
        clientContext.newObject(ClientMtTable1.class);
        clientContext.newObject(ClientMtTable1.class);
        clientContext.newObject(ClientMtTable1.class);
        clientContext.newObject(ClientMtTable1.class);
        clientContext.commitChanges();

        final ObjectContext child = runtime.newContext(clientContext);

        SelectQuery query = new SelectQuery(ClientMtTable1.class);
        List<?> objects = child.performQuery(query);

        assertEquals(4, objects.size());

        final ClientMtTable1 childNew = child.newObject(ClientMtTable1.class);
        childNew.setGlobalAttribute1("NNN");

        final ClientMtTable1 childModified = (ClientMtTable1) objects.get(0);
        childModified.setGlobalAttribute1("MMM");

        final ClientMtTable1 childCommitted = (ClientMtTable1) objects.get(1);

        final ClientMtTable1 childHollow = (ClientMtTable1) objects.get(3);
        child.invalidateObjects(childHollow);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                child.commitChangesToParent();

                // * all modified child objects must be in committed state now
                // * all modifications should be propagated to the parent
                // * no actual commit should occur.

                assertEquals(PersistenceState.COMMITTED, childNew.getPersistenceState());
                assertEquals(
                        PersistenceState.COMMITTED,
                        childModified.getPersistenceState());
                assertEquals(
                        PersistenceState.COMMITTED,
                        childCommitted.getPersistenceState());
                assertEquals(PersistenceState.HOLLOW, childHollow.getPersistenceState());

                ClientMtTable1 parentNew = (ClientMtTable1) clientContext
                        .getGraphManager()
                        .getNode(childNew.getObjectId());
                final ClientMtTable1 parentModified = (ClientMtTable1) clientContext
                        .getGraphManager()
                        .getNode(childModified.getObjectId());
                ClientMtTable1 parentCommitted = (ClientMtTable1) clientContext
                        .getGraphManager()
                        .getNode(childCommitted.getObjectId());
                ClientMtTable1 parentHollow = (ClientMtTable1) clientContext
                        .getGraphManager()
                        .getNode(childHollow.getObjectId());

                assertNotNull(parentNew);
                assertEquals(PersistenceState.NEW, parentNew.getPersistenceState());
                assertEquals("NNN", parentNew.getGlobalAttribute1());

                assertNotNull(parentModified);
                assertEquals(
                        PersistenceState.MODIFIED,
                        parentModified.getPersistenceState());
                assertEquals("MMM", parentModified.getGlobalAttribute1());

                assertNotNull(parentCommitted);
                assertEquals(
                        PersistenceState.COMMITTED,
                        parentCommitted.getPersistenceState());

                assertNotNull(parentHollow);

                // check that arc changes got recorded in the parent context
                GraphDiff diffs = clientContext.internalGraphManager().getDiffs();

                final int[] modifiedProperties = new int[1];

                diffs.apply(new GraphChangeHandler() {

                    public void arcCreated(
                            Object nodeId,
                            Object targetNodeId,
                            Object arcId) {

                    }

                    public void arcDeleted(
                            Object nodeId,
                            Object targetNodeId,
                            Object arcId) {

                    }

                    public void nodeCreated(Object nodeId) {

                    }

                    public void nodeIdChanged(Object nodeId, Object newId) {
                    }

                    public void nodePropertyChanged(
                            Object nodeId,
                            String property,
                            Object oldValue,
                            Object newValue) {

                        if (nodeId.equals(parentModified.getObjectId())) {
                            modifiedProperties[0]++;
                        }
                    }

                    public void nodeRemoved(Object nodeId) {

                    }
                });

                assertEquals(1, modifiedProperties[0]);
            }
        });
    }

    public void testCommitChangesToParentDeleted() throws Exception {
        clientContext.newObject(ClientMtTable1.class);
        clientContext.newObject(ClientMtTable1.class);
        clientContext.newObject(ClientMtTable1.class);
        clientContext.newObject(ClientMtTable1.class);
        clientContext.commitChanges();

        ObjectContext child = runtime.newContext(clientContext);

        // make sure we fetch in predictable order
        SelectQuery query = new SelectQuery(ClientMtTable1.class);
        List<?> objects = child.performQuery(query);

        assertEquals(4, objects.size());

        // delete AND modify
        ClientMtTable1 childDeleted = (ClientMtTable1) objects.get(2);
        child.deleteObjects(childDeleted);
        childDeleted.setGlobalAttribute1("DDD");

        // don't block queries - on delete Cayenne may need to resolve delete rules via
        // fetch
        child.commitChangesToParent();

        // * all modified child objects must be in committed state now
        // * all modifications should be propagated to the parent
        // * no actual commit should occur.

        assertEquals(PersistenceState.TRANSIENT, childDeleted.getPersistenceState());

        ClientMtTable1 parentDeleted = (ClientMtTable1) clientContext
                .getGraphManager()
                .getNode(childDeleted.getObjectId());

        assertNotNull(parentDeleted);
        assertEquals(PersistenceState.DELETED, parentDeleted.getPersistenceState());
        assertEquals("DDD", parentDeleted.getGlobalAttribute1());
    }

    /*
     * was added for CAY-1636
     */
    public void testCAY1636() throws Exception {

        ClientMtTooneMaster A = clientContext.newObject(ClientMtTooneMaster.class);
        clientContext.commitChanges();

        ClientMtTooneDep B = clientContext.newObject(ClientMtTooneDep.class);
        A.setToDependent(B);
        clientContext.commitChanges();

        ObjectContext child = runtime.newContext(clientContext);

        SelectQuery query = new SelectQuery(ClientMtTooneMaster.class);
        List<?> objects = child.performQuery(query);

        assertEquals(1, objects.size());

        ClientMtTooneMaster childDeleted = (ClientMtTooneMaster) objects.get(0);

        child.deleteObjects(childDeleted);

        child.commitChangesToParent();

        ClientMtTooneMaster parentDeleted = (ClientMtTooneMaster) clientContext
                .getGraphManager()
                .getNode(childDeleted.getObjectId());

        assertNotNull(parentDeleted);
        assertEquals(PersistenceState.DELETED, parentDeleted.getPersistenceState());

        clientContext.commitChanges();

        SelectQuery query2 = new SelectQuery(ClientMtTooneMaster.class);
        List<?> objects2 = child.performQuery(query2);

        assertEquals(0, objects2.size());

    }

    public void testCAY1636_2() throws Exception {

        ClientMtTooneMaster A = clientContext.newObject(ClientMtTooneMaster.class);
        clientContext.commitChanges();

        ClientMtTooneDep B = clientContext.newObject(ClientMtTooneDep.class);
        A.setToDependent(B);
        clientContext.commitChanges();

        ObjectContext child = runtime.newContext(clientContext);

        SelectQuery queryB = new SelectQuery(ClientMtTooneDep.class);
        List<?> objectsB = child.performQuery(queryB);

        assertEquals(1, objectsB.size());

        ClientMtTooneDep childBDeleted = (ClientMtTooneDep) objectsB.get(0);
        child.deleteObjects(childBDeleted);

        SelectQuery query = new SelectQuery(ClientMtTooneMaster.class);
        List<?> objects = child.performQuery(query);

        assertEquals(1, objects.size());

        ClientMtTooneMaster childDeleted = (ClientMtTooneMaster) objects.get(0);

        child.deleteObjects(childDeleted);

        child.commitChangesToParent();

        ClientMtTooneMaster parentDeleted = (ClientMtTooneMaster) clientContext
                .getGraphManager()
                .getNode(childDeleted.getObjectId());

        assertNotNull(parentDeleted);
        assertEquals(PersistenceState.DELETED, parentDeleted.getPersistenceState());

        clientContext.commitChanges();

        SelectQuery query2 = new SelectQuery(ClientMtTooneMaster.class);
        List<?> objects2 = child.performQuery(query2);

        assertEquals(0, objects2.size());

    }

    public void testCommitChanges() throws Exception {
        clientContext.newObject(ClientMtTable1.class);
        clientContext.newObject(ClientMtTable1.class);
        clientContext.newObject(ClientMtTable1.class);
        clientContext.newObject(ClientMtTable1.class);
        clientContext.commitChanges();

        ObjectContext child = runtime.newContext(clientContext);

        // make sure we fetch in predictable order
        SelectQuery query = new SelectQuery(ClientMtTable1.class);
        List<?> objects = child.performQuery(query);

        assertEquals(4, objects.size());

        ClientMtTable1 childNew = child.newObject(ClientMtTable1.class);
        childNew.setGlobalAttribute1("NNN");

        ClientMtTable1 childModified = (ClientMtTable1) objects.get(0);
        childModified.setGlobalAttribute1("MMM");

        ClientMtTable1 childCommitted = (ClientMtTable1) objects.get(1);

        // delete AND modify
        ClientMtTable1 childDeleted = (ClientMtTable1) objects.get(2);
        child.deleteObjects(childDeleted);
        childDeleted.setGlobalAttribute1("DDD");

        ClientMtTable1 childHollow = (ClientMtTable1) objects.get(3);
        child.invalidateObjects(childHollow);

        child.commitChanges();

        assertEquals(PersistenceState.COMMITTED, childNew.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, childModified.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, childCommitted.getPersistenceState());
        assertEquals(PersistenceState.TRANSIENT, childDeleted.getPersistenceState());
        assertEquals(PersistenceState.HOLLOW, childHollow.getPersistenceState());

        ClientMtTable1 parentNew = (ClientMtTable1) clientContext
                .getGraphManager()
                .getNode(childNew.getObjectId());
        ClientMtTable1 parentModified = (ClientMtTable1) clientContext
                .getGraphManager()
                .getNode(childModified.getObjectId());
        ClientMtTable1 parentCommitted = (ClientMtTable1) clientContext
                .getGraphManager()
                .getNode(childCommitted.getObjectId());
        ClientMtTable1 parentDeleted = (ClientMtTable1) clientContext
                .getGraphManager()
                .getNode(childDeleted.getObjectId());
        ClientMtTable1 parentHollow = (ClientMtTable1) clientContext
                .getGraphManager()
                .getNode(childHollow.getObjectId());

        assertNotNull(parentNew);
        assertEquals(PersistenceState.COMMITTED, parentNew.getPersistenceState());
        assertEquals("NNN", parentNew.getGlobalAttribute1());

        assertNotNull(parentModified);
        assertEquals(PersistenceState.COMMITTED, parentModified.getPersistenceState());
        assertEquals("MMM", parentModified.getGlobalAttribute1());

        assertNull("Deleted object should not be registered.", parentDeleted);

        assertNotNull(parentCommitted);
        assertEquals(PersistenceState.COMMITTED, parentCommitted.getPersistenceState());

        assertNotNull(parentHollow);
    }

    public void testAddRemove() throws Exception {
        ObjectContext child = runtime.newContext(clientContext);

        ClientMtTable1 a = child.newObject(ClientMtTable1.class);
        a.setGlobalAttribute1("X");
        child.commitChanges();

        ClientMtTable2 p1 = child.newObject(ClientMtTable2.class);
        p1.setGlobalAttribute("P1");
        a.addToTable2Array(p1);

        ClientMtTable2 p2 = child.newObject(ClientMtTable2.class);
        p2.setGlobalAttribute("P2");
        a.addToTable2Array(p2);

        a.removeFromTable2Array(p2);

        // this causes an error on commit
        child.deleteObjects(p2);

        child.commitChangesToParent();

    }

    public void testChangeRel() throws Exception {
        ObjectContext child = runtime.newContext(clientContext);

        ClientMtTable1 a = child.newObject(ClientMtTable1.class);
        ClientMtTable2 b = child.newObject(ClientMtTable2.class);
        child.commitChanges();

        assertEquals(PersistenceState.COMMITTED, a.getPersistenceState());

        a.addToTable2Array(b);
        assertEquals(PersistenceState.MODIFIED, a.getPersistenceState());

        child.commitChangesToParent();
        ClientMtTable1 parentA = (ClientMtTable1) clientContext
                .getGraphManager()
                .getNode(a.getObjectId());
        assertEquals(PersistenceState.COMMITTED, a.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, parentA.getPersistenceState());
        assertEquals(1, parentA.getTable2Array().size());

        clientContext.commitChanges();
        assertEquals(PersistenceState.COMMITTED, parentA.getPersistenceState());

        a.removeFromTable2Array(b);
        assertEquals(PersistenceState.MODIFIED, a.getPersistenceState());

        child.commitChangesToParent();
        assertEquals(PersistenceState.COMMITTED, a.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, parentA.getPersistenceState());
        assertEquals(0, parentA.getTable2Array().size());
    }

    public void testCAY1183() throws Exception {
        ClientMtTable1 parentMt = clientContext.newObject(ClientMtTable1.class);
        clientContext.commitChanges();

        ObjectContext child = runtime.newContext(clientContext);
        ClientMtTable1 childMt = (ClientMtTable1) Cayenne.objectForPK(
                child,
                parentMt.getObjectId());
        childMt.setGlobalAttribute1("1183");
        ClientMtTable2 childMt2 = child.newObject(ClientMtTable2.class);
        childMt2.setGlobalAttribute("1183");
        childMt2.setTable1(childMt);

        child.commitChangesToParent();

        // fetching other relationship... this fails per CAY-1183
        childMt2.getTable3();
    }
    
    /**
     * CAY1714
     */
    public void testQueriesOnTemporaryObject() throws Exception {
        ObjectContext clientContext = runtime.newContext((DataChannel) this.clientContext);
        ClientMtTable1 parentMt = clientContext.newObject(ClientMtTable1.class);

        ObjectContext childContext = runtime.newContext((DataChannel) clientContext);
        ClientMtTable1 childMt = (ClientMtTable1) Cayenne.objectForPK(childContext, parentMt.getObjectId());
        childMt.setGlobalAttribute1("1183");
        ClientMtTable2 childMt2 = childContext.newObject(ClientMtTable2.class);
        childMt2.setGlobalAttribute("1183");
        childMt2.setTable1(childMt);

        childContext.commitChangesToParent();

        assertNull(childMt2.getTable3());
    }

    public void testCAY1194() throws Exception {
        ClientMtTable1 parentMt = clientContext.newObject(ClientMtTable1.class);
        ObjectContext child = runtime.newContext(clientContext);

        ClientMtTable2 childMt2 = child.newObject(ClientMtTable2.class);
        childMt2.setGlobalAttribute("222");

        ClientMtTable1 localParentMt = child.localObject(parentMt);
        assertEquals(0, parentMt.getTable2Array().size());
        assertEquals(0, localParentMt.getTable2Array().size());

        childMt2.setTable1(localParentMt);

        assertEquals(0, parentMt.getTable2Array().size());
        assertEquals(1, localParentMt.getTable2Array().size());

        assertEquals(localParentMt.getTable2Array().get(0).getObjectContext(), child);

        child.commitChangesToParent();
        assertEquals(1, parentMt.getTable2Array().size());
        assertEquals(parentMt.getTable2Array().get(0).getObjectContext(), clientContext);
    }

    public void testCommitChangesToParentOneToMany() throws Exception {
        ObjectContext child = runtime.newContext(clientContext);

        ClientMtTable1 master = child.newObject(ClientMtTable1.class);
        ClientMtTable2 dep = child.newObject(ClientMtTable2.class);
        master.addToTable2Array(dep);

        child.commitChangesToParent();

        ClientMtTable1 masterParent = (ClientMtTable1) clientContext
                .getGraphManager()
                .getNode(master.getObjectId());
        ClientMtTable2 depParent = (ClientMtTable2) clientContext
                .getGraphManager()
                .getNode(dep.getObjectId());

        assertNotNull(masterParent);
        assertNotNull(depParent);

        assertSame(masterParent, depParent.getTable1());
        assertTrue(masterParent.getTable2Array().contains(depParent));

        // check that arc changes got recorded in the parent context
        GraphDiff diffs = clientContext.internalGraphManager().getDiffs();

        final int[] arcDiffs = new int[1];
        final int[] newNodes = new int[1];

        diffs.apply(new GraphChangeHandler() {

            public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
                arcDiffs[0]++;
            }

            public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
                arcDiffs[0]--;
            }

            public void nodeCreated(Object nodeId) {
                newNodes[0]++;
            }

            public void nodeIdChanged(Object nodeId, Object newId) {
            }

            public void nodePropertyChanged(
                    Object nodeId,
                    String property,
                    Object oldValue,
                    Object newValue) {
            }

            public void nodeRemoved(Object nodeId) {
                newNodes[0]--;
            }
        });

        assertEquals(2, newNodes[0]);
        assertEquals(2, arcDiffs[0]);
    }

    public void testCommitChangesToParentOneToOne() throws Exception {
        ObjectContext child = runtime.newContext(clientContext);

        ClientMtTooneMaster master = child.newObject(ClientMtTooneMaster.class);
        ClientMtTooneDep dep = child.newObject(ClientMtTooneDep.class);
        master.setToDependent(dep);

        child.commitChangesToParent();

        ClientMtTooneMaster masterParent = (ClientMtTooneMaster) clientContext
                .getGraphManager()
                .getNode(master.getObjectId());
        ClientMtTooneDep depParent = (ClientMtTooneDep) clientContext
                .getGraphManager()
                .getNode(dep.getObjectId());

        assertNotNull(masterParent);
        assertNotNull(depParent);

        assertSame(masterParent, depParent.getToMaster());
        assertSame(depParent, masterParent.getToDependent());

        // check that arc changes got recorded in the parent context
        GraphDiff diffs = clientContext.internalGraphManager().getDiffs();

        final int[] arcDiffs = new int[1];
        final int[] newNodes = new int[1];

        diffs.apply(new GraphChangeHandler() {

            public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
                arcDiffs[0]++;
            }

            public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
                arcDiffs[0]--;
            }

            public void nodeCreated(Object nodeId) {
                newNodes[0]++;
            }

            public void nodeIdChanged(Object nodeId, Object newId) {
            }

            public void nodePropertyChanged(
                    Object nodeId,
                    String property,
                    Object oldValue,
                    Object newValue) {
            }

            public void nodeRemoved(Object nodeId) {
                newNodes[0]--;
            }
        });

        assertEquals(2, newNodes[0]);
        assertEquals(2, arcDiffs[0]);
    }
}
