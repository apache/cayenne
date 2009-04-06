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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.remote.RemoteCayenneCase;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.testdo.mt.ClientMtTooneDep;
import org.apache.cayenne.testdo.mt.ClientMtTooneMaster;

/**
 * Tests nested object contexts
 */
public class NestedCayenneContextTest extends RemoteCayenneCase {

    public void testChannels() {
        ObjectContext child = context.createChildContext();

        assertNotNull(child);
        assertSame(context, child.getChannel());

        // second level of nesting
        ObjectContext grandchild = child.createChildContext();

        assertNotNull(grandchild);
        assertSame(child, grandchild.getChannel());
    }

    public void testLocalObjectSynchronize() throws Exception {
        deleteTestData();

        ObjectContext child = context.createChildContext();

        ClientMtTable1 committed = context.newObject(ClientMtTable1.class);
        ClientMtTable1 deleted = context.newObject(ClientMtTable1.class);
        ClientMtTable1 modified = context.newObject(ClientMtTable1.class);

        context.commitChanges();

        context.deleteObject(deleted);
        modified.setGlobalAttribute1("a");

        ClientMtTable1 _new = context.newObject(ClientMtTable1.class);

        ClientMtTable1 hollow = (ClientMtTable1) context.localObject(new ObjectId(
                "MtTable1"), null);

        assertEquals(PersistenceState.HOLLOW, hollow.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, committed.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, modified.getPersistenceState());
        assertEquals(PersistenceState.DELETED, deleted.getPersistenceState());
        assertEquals(PersistenceState.NEW, _new.getPersistenceState());

        blockQueries();

        try {
            Persistent newPeer = child.localObject(_new.getObjectId(), _new);

            assertEquals(_new.getObjectId(), newPeer.getObjectId());
            assertEquals(PersistenceState.COMMITTED, newPeer.getPersistenceState());

            assertSame(child, newPeer.getObjectContext());
            assertSame(context, _new.getObjectContext());

            Persistent hollowPeer = child.localObject(hollow.getObjectId(), hollow);
            assertEquals(PersistenceState.HOLLOW, hollowPeer.getPersistenceState());
            assertEquals(hollow.getObjectId(), hollowPeer.getObjectId());
            assertSame(child, hollowPeer.getObjectContext());
            assertSame(context, hollow.getObjectContext());

            Persistent committedPeer = child.localObject(
                    committed.getObjectId(),
                    committed);
            assertEquals(PersistenceState.COMMITTED, committedPeer.getPersistenceState());
            assertEquals(committed.getObjectId(), committedPeer.getObjectId());
            assertSame(child, committedPeer.getObjectContext());
            assertSame(context, committed.getObjectContext());

            ClientMtTable1 modifiedPeer = (ClientMtTable1) child.localObject(modified
                    .getObjectId(), modified);
            assertEquals(PersistenceState.COMMITTED, modifiedPeer.getPersistenceState());
            assertEquals(modified.getObjectId(), modifiedPeer.getObjectId());
            assertEquals("a", modifiedPeer.getGlobalAttribute1());
            assertSame(child, modifiedPeer.getObjectContext());
            assertSame(context, modified.getObjectContext());

            Persistent deletedPeer = child.localObject(deleted.getObjectId(), deleted);
            assertEquals(PersistenceState.COMMITTED, deletedPeer.getPersistenceState());
            assertEquals(deleted.getObjectId(), deletedPeer.getObjectId());
            assertSame(child, deletedPeer.getObjectContext());
            assertSame(context, deleted.getObjectContext());
        }
        finally {
            unblockQueries();
        }
    }

    public void testLocalObjectsNoOverride() throws Exception {
        deleteTestData();

        ObjectContext child = context.createChildContext();

        ClientMtTable1 modified = context.newObject(ClientMtTable1.class);
        context.commitChanges();

        ClientMtTable1 peerModified = (ClientMtTable1) DataObjectUtils.objectForQuery(
                child,
                new ObjectIdQuery(modified.getObjectId()));

        modified.setGlobalAttribute1("M1");
        peerModified.setGlobalAttribute1("M2");

        assertEquals(PersistenceState.MODIFIED, modified.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, peerModified.getPersistenceState());

        blockQueries();

        try {

            Persistent peerModified2 = child
                    .localObject(modified.getObjectId(), modified);
            assertSame(peerModified, peerModified2);
            assertEquals(PersistenceState.MODIFIED, peerModified2.getPersistenceState());
            assertEquals("M2", peerModified.getGlobalAttribute1());
            assertEquals("M1", modified.getGlobalAttribute1());
        }
        finally {
            unblockQueries();
        }
    }

    public void testLocalObjectRelationship() throws Exception {
        deleteTestData();

        ObjectContext child = context.createChildContext();

        ClientMtTable1 _new = context.newObject(ClientMtTable1.class);
        ClientMtTable2 _new2 = context.newObject(ClientMtTable2.class);
        _new.addToTable2Array(_new2);

        blockQueries();

        try {
            ClientMtTable2 child2 = (ClientMtTable2) child.localObject(_new2
                    .getObjectId(), _new2);
            assertEquals(PersistenceState.COMMITTED, child2.getPersistenceState());
            assertNotNull(child2.getTable1());
            assertEquals(PersistenceState.COMMITTED, child2
                    .getTable1()
                    .getPersistenceState());
        }
        finally {
            unblockQueries();
        }
    }

    public void testSelect() throws Exception {
        deleteTestData();

        ObjectContext child = context.createChildContext();

        ClientMtTable1 committed = context.newObject(ClientMtTable1.class);
        ClientMtTable1 deleted = context.newObject(ClientMtTable1.class);
        ClientMtTable1 modified = context.newObject(ClientMtTable1.class);

        context.commitChanges();
        int modifiedid = DataObjectUtils.intPKForObject(modified);

        // test how different object states appear in the child on select

        context.deleteObject(deleted);
        modified.setGlobalAttribute1("a");

        ClientMtTable1 _new = context.newObject(ClientMtTable1.class);

        assertEquals(PersistenceState.COMMITTED, committed.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, modified.getPersistenceState());
        assertEquals(PersistenceState.DELETED, deleted.getPersistenceState());
        assertEquals(PersistenceState.NEW, _new.getPersistenceState());

        List objects = child.performQuery(new SelectQuery(ClientMtTable1.class));
        assertEquals("All but NEW object must have been included", 3, objects.size());

        Iterator it = objects.iterator();
        while (it.hasNext()) {
            ClientMtTable1 next = (ClientMtTable1) it.next();
            assertEquals(PersistenceState.COMMITTED, next.getPersistenceState());

            int id = DataObjectUtils.intPKForObject(next);
            if (id == modifiedid) {
                assertEquals("a", next.getGlobalAttribute1());
            }
        }
    }

    public void testPrefetchingToOne() throws Exception {
        deleteTestData();

        ClientMtTable1 mt11 = context.newObject(ClientMtTable1.class);
        ClientMtTable1 mt12 = context.newObject(ClientMtTable1.class);
        ClientMtTable2 mt21 = context.newObject(ClientMtTable2.class);
        ClientMtTable2 mt22 = context.newObject(ClientMtTable2.class);

        mt21.setTable1(mt11);
        mt22.setTable1(mt11);

        context.commitChanges();

        ObjectContext child = context.createChildContext();

        SelectQuery q = new SelectQuery(ClientMtTable2.class);
        q.addPrefetch(ClientMtTable2.TABLE1_PROPERTY);

        List results = child.performQuery(q);

        blockQueries();
        try {
            assertEquals(2, results.size());
            Iterator it = results.iterator();
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
        finally {
            unblockQueries();
        }
    }

    public void testPrefetchingToMany() throws Exception {
        deleteTestData();

        ClientMtTable1 mt11 = context.newObject(ClientMtTable1.class);
        mt11.setGlobalAttribute1("1");

        ClientMtTable1 mt12 = context.newObject(ClientMtTable1.class);
        mt12.setGlobalAttribute1("2");

        ClientMtTable2 mt21 = context.newObject(ClientMtTable2.class);
        ClientMtTable2 mt22 = context.newObject(ClientMtTable2.class);

        mt21.setTable1(mt11);
        mt22.setTable1(mt11);

        context.commitChanges();

        ObjectContext child = context.createChildContext();

        SelectQuery q = new SelectQuery(ClientMtTable1.class);
        q.addOrdering("globalAttribute1", true);
        q.addPrefetch(ClientMtTable1.TABLE2ARRAY_PROPERTY);

        List results = child.performQuery(q);

        blockQueries();
        try {

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

            List children2 = o2.getTable2Array();

            assertEquals(0, children2.size());
        }
        finally {
            unblockQueries();
        }
    }

    public void testDeleteNew() throws Exception {
        deleteTestData();
        ObjectContext child = context.createChildContext();

        ClientMtTable1 a = context.newObject(ClientMtTable1.class);
        context.commitChanges();

        ClientMtTable2 p = child.newObject(ClientMtTable2.class);
        ClientMtTable1 aChild = (ClientMtTable1) DataObjectUtils.objectForPK(child, a
                .getObjectId());
        p.setGlobalAttribute("X");
        aChild.addToTable2Array(p);

        child.commitChangesToParent();

        child.deleteObject(p);
        aChild.removeFromTable2Array(p);

        child.commitChangesToParent();
    }

    /**
     * A test case for CAY-698 bug.
     */
    public void testNullifyToOne() throws Exception {
        deleteTestData();

        ClientMtTable1 a = context.newObject(ClientMtTable1.class);
        ClientMtTable2 b = context.newObject(ClientMtTable2.class);
        a.addToTable2Array(b);

        context.commitChanges();

        ObjectContext child = context.createChildContext();
        ObjectContext childPeer = context.createChildContext();

        ClientMtTable2 childP1 = (ClientMtTable2) DataObjectUtils.objectForPK(child, b
                .getObjectId());

        // trigger object creation in the peer nested DC
        DataObjectUtils.objectForPK(childPeer, b.getObjectId());
        childP1.setTable1(null);

        blockQueries();

        try {
            child.commitChangesToParent();
            assertEquals(PersistenceState.COMMITTED, childP1.getPersistenceState());

            ClientMtTable2 parentP1 = (ClientMtTable2) context.getGraphManager().getNode(
                    childP1.getObjectId());

            assertNotNull(parentP1);
            assertEquals(PersistenceState.MODIFIED, parentP1.getPersistenceState());
            assertNull(parentP1.getTable1());

            // check that arc changes got recorded in the parent context
            GraphDiff diffs = context.internalGraphManager().getDiffs();
            final int[] arcDiffs = new int[1];

            diffs.apply(new GraphChangeHandler() {

                public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
                    arcDiffs[0]++;
                }

                public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
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
        finally {
            unblockQueries();
        }
    }

    public void testCommitChangesToParent() throws Exception {
        deleteTestData();

        context.newObject(ClientMtTable1.class);
        context.newObject(ClientMtTable1.class);
        context.newObject(ClientMtTable1.class);
        context.newObject(ClientMtTable1.class);
        context.commitChanges();

        ObjectContext child = context.createChildContext();

        SelectQuery query = new SelectQuery(ClientMtTable1.class);
        List objects = child.performQuery(query);

        assertEquals(4, objects.size());

        ClientMtTable1 childNew = child.newObject(ClientMtTable1.class);
        childNew.setGlobalAttribute1("NNN");

        ClientMtTable1 childModified = (ClientMtTable1) objects.get(0);
        childModified.setGlobalAttribute1("MMM");

        ClientMtTable1 childCommitted = (ClientMtTable1) objects.get(1);

        ClientMtTable1 childHollow = (ClientMtTable1) objects.get(3);
        child.invalidateObjects(Collections.singleton(childHollow));

        blockQueries();

        try {
            child.commitChangesToParent();

            // * all modified child objects must be in committed state now
            // * all modifications should be propagated to the parent
            // * no actual commit should occur.

            assertEquals(PersistenceState.COMMITTED, childNew.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childModified.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childCommitted.getPersistenceState());
            assertEquals(PersistenceState.HOLLOW, childHollow.getPersistenceState());

            ClientMtTable1 parentNew = (ClientMtTable1) context
                    .getGraphManager()
                    .getNode(childNew.getObjectId());
            final ClientMtTable1 parentModified = (ClientMtTable1) context
                    .getGraphManager()
                    .getNode(childModified.getObjectId());
            ClientMtTable1 parentCommitted = (ClientMtTable1) context
                    .getGraphManager()
                    .getNode(childCommitted.getObjectId());
            ClientMtTable1 parentHollow = (ClientMtTable1) context
                    .getGraphManager()
                    .getNode(childHollow.getObjectId());

            assertNotNull(parentNew);
            assertEquals(PersistenceState.NEW, parentNew.getPersistenceState());
            assertEquals("NNN", parentNew.getGlobalAttribute1());

            assertNotNull(parentModified);
            assertEquals(PersistenceState.MODIFIED, parentModified.getPersistenceState());
            assertEquals("MMM", parentModified.getGlobalAttribute1());

            assertNotNull(parentCommitted);
            assertEquals(PersistenceState.COMMITTED, parentCommitted
                    .getPersistenceState());

            assertNotNull(parentHollow);

            // check that arc changes got recorded in the parent context
            GraphDiff diffs = context.internalGraphManager().getDiffs();

            final int[] modifiedProperties = new int[1];

            diffs.apply(new GraphChangeHandler() {

                public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {

                }

                public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {

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
        finally {
            unblockQueries();
        }
    }

    public void testCommitChangesToParentDeleted() throws Exception {
        deleteTestData();

        context.newObject(ClientMtTable1.class);
        context.newObject(ClientMtTable1.class);
        context.newObject(ClientMtTable1.class);
        context.newObject(ClientMtTable1.class);
        context.commitChanges();

        ObjectContext child = context.createChildContext();

        // make sure we fetch in predictable order
        SelectQuery query = new SelectQuery(ClientMtTable1.class);
        List objects = child.performQuery(query);

        assertEquals(4, objects.size());

        // delete AND modify
        ClientMtTable1 childDeleted = (ClientMtTable1) objects.get(2);
        child.deleteObject(childDeleted);
        childDeleted.setGlobalAttribute1("DDD");

        // don't block queries - on delete Cayenne may need to resolve delete rules via
        // fetch
        child.commitChangesToParent();

        // * all modified child objects must be in committed state now
        // * all modifications should be propagated to the parent
        // * no actual commit should occur.

        assertEquals(PersistenceState.TRANSIENT, childDeleted.getPersistenceState());

        ClientMtTable1 parentDeleted = (ClientMtTable1) context
                .getGraphManager()
                .getNode(childDeleted.getObjectId());

        assertNotNull(parentDeleted);
        assertEquals(PersistenceState.DELETED, parentDeleted.getPersistenceState());
        assertEquals("DDD", parentDeleted.getGlobalAttribute1());
    }

    public void testCommitChanges() throws Exception {
        deleteTestData();

        context.newObject(ClientMtTable1.class);
        context.newObject(ClientMtTable1.class);
        context.newObject(ClientMtTable1.class);
        context.newObject(ClientMtTable1.class);
        context.commitChanges();

        ObjectContext child = context.createChildContext();

        // make sure we fetch in predictable order
        SelectQuery query = new SelectQuery(ClientMtTable1.class);
        List objects = child.performQuery(query);

        assertEquals(4, objects.size());

        ClientMtTable1 childNew = child.newObject(ClientMtTable1.class);
        childNew.setGlobalAttribute1("NNN");

        ClientMtTable1 childModified = (ClientMtTable1) objects.get(0);
        childModified.setGlobalAttribute1("MMM");

        ClientMtTable1 childCommitted = (ClientMtTable1) objects.get(1);

        // delete AND modify
        ClientMtTable1 childDeleted = (ClientMtTable1) objects.get(2);
        child.deleteObject(childDeleted);
        childDeleted.setGlobalAttribute1("DDD");

        ClientMtTable1 childHollow = (ClientMtTable1) objects.get(3);
        child.invalidateObjects(Collections.singleton(childHollow));

        child.commitChanges();

        assertEquals(PersistenceState.COMMITTED, childNew.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, childModified.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, childCommitted.getPersistenceState());
        assertEquals(PersistenceState.TRANSIENT, childDeleted.getPersistenceState());
        assertEquals(PersistenceState.HOLLOW, childHollow.getPersistenceState());

        ClientMtTable1 parentNew = (ClientMtTable1) context.getGraphManager().getNode(
                childNew.getObjectId());
        ClientMtTable1 parentModified = (ClientMtTable1) context
                .getGraphManager()
                .getNode(childModified.getObjectId());
        ClientMtTable1 parentCommitted = (ClientMtTable1) context
                .getGraphManager()
                .getNode(childCommitted.getObjectId());
        ClientMtTable1 parentDeleted = (ClientMtTable1) context
                .getGraphManager()
                .getNode(childDeleted.getObjectId());
        ClientMtTable1 parentHollow = (ClientMtTable1) context.getGraphManager().getNode(
                childHollow.getObjectId());

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
        deleteTestData();
        ObjectContext child = context.createChildContext();

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
        child.deleteObject(p2);

        child.commitChangesToParent();

    }

    public void testChangeRel() throws Exception {
        deleteTestData();
        ObjectContext child = context.createChildContext();

        ClientMtTable1 a = child.newObject(ClientMtTable1.class);
        ClientMtTable2 b = child.newObject(ClientMtTable2.class);
        child.commitChanges();

        assertEquals(PersistenceState.COMMITTED, a.getPersistenceState());

        a.addToTable2Array(b);
        assertEquals(PersistenceState.MODIFIED, a.getPersistenceState());

        child.commitChangesToParent();
        ClientMtTable1 parentA = (ClientMtTable1) context.getGraphManager().getNode(
                a.getObjectId());
        assertEquals(PersistenceState.COMMITTED, a.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, parentA.getPersistenceState());
        assertEquals(1, parentA.getTable2Array().size());

        context.commitChanges();
        assertEquals(PersistenceState.COMMITTED, parentA.getPersistenceState());

        a.removeFromTable2Array(b);
        assertEquals(PersistenceState.MODIFIED, a.getPersistenceState());

        child.commitChangesToParent();
        assertEquals(PersistenceState.COMMITTED, a.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, parentA.getPersistenceState());
        assertEquals(0, parentA.getTable2Array().size());
    }

    public void testCAY1183() throws Exception {
        deleteTestData();

        ClientMtTable1 parentMt = context.newObject(ClientMtTable1.class);
        context.commitChanges();

        ObjectContext child = context.createChildContext();
        ClientMtTable1 childMt = (ClientMtTable1) DataObjectUtils.objectForPK(
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

    public void testCAY1194() throws Exception {
        deleteTestData();

        ClientMtTable1 parentMt = context.newObject(ClientMtTable1.class);
        ObjectContext child = context.createChildContext();

        ClientMtTable2 childMt2 = child.newObject(ClientMtTable2.class);
        childMt2.setGlobalAttribute("222");

        ClientMtTable1 localParentMt = (ClientMtTable1) child.localObject(parentMt
                .getObjectId(), null);
        assertEquals(0, parentMt.getTable2Array().size());
        assertEquals(0, localParentMt.getTable2Array().size());

        childMt2.setTable1(localParentMt);

        assertEquals(0, parentMt.getTable2Array().size());
        assertEquals(1, localParentMt.getTable2Array().size());

        assertEquals(localParentMt.getTable2Array().get(0).getObjectContext(), child);

        child.commitChangesToParent();
        assertEquals(1, parentMt.getTable2Array().size());
        assertEquals(parentMt.getTable2Array().get(0).getObjectContext(), context);
    }

    public void testCommitChangesToParentOneToMany() throws Exception {
        deleteTestData();

        ObjectContext child = context.createChildContext();

        ClientMtTable1 master = child.newObject(ClientMtTable1.class);
        ClientMtTable2 dep = child.newObject(ClientMtTable2.class);
        master.addToTable2Array(dep);

        child.commitChangesToParent();

        ClientMtTable1 masterParent = (ClientMtTable1) context.getGraphManager().getNode(
                master.getObjectId());
        ClientMtTable2 depParent = (ClientMtTable2) context.getGraphManager().getNode(
                dep.getObjectId());

        assertNotNull(masterParent);
        assertNotNull(depParent);

        assertSame(masterParent, depParent.getTable1());
        assertTrue(masterParent.getTable2Array().contains(depParent));

        // check that arc changes got recorded in the parent context
        GraphDiff diffs = context.internalGraphManager().getDiffs();

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
        deleteTestData();

        ObjectContext child = context.createChildContext();

        ClientMtTooneMaster master = child.newObject(ClientMtTooneMaster.class);
        ClientMtTooneDep dep = child.newObject(ClientMtTooneDep.class);
        master.setToDependent(dep);

        child.commitChangesToParent();

        ClientMtTooneMaster masterParent = (ClientMtTooneMaster) context
                .getGraphManager()
                .getNode(master.getObjectId());
        ClientMtTooneDep depParent = (ClientMtTooneDep) context
                .getGraphManager()
                .getNode(dep.getObjectId());

        assertNotNull(masterParent);
        assertNotNull(depParent);

        assertSame(masterParent, depParent.getToMaster());
        assertSame(depParent, masterParent.getToDependent());

        // check that arc changes got recorded in the parent context
        GraphDiff diffs = context.internalGraphManager().getDiffs();

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
