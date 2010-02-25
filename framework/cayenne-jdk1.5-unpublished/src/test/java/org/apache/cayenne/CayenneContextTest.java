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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.MockGraphDiff;
import org.apache.cayenne.graph.NodeIdChangeOperation;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.remote.MockClientConnection;
import org.apache.cayenne.remote.ClientChannel;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;
import org.apache.cayenne.util.GenericResponse;

/**
 */
public class CayenneContextTest extends CayenneCase {

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources.getResources().getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testConstructor() {

        CayenneContext context = new CayenneContext();

        // test default property parameters
        assertNotNull(context.getGraphManager());
        assertNull(context.getChannel());

        MockDataChannel channel = new MockDataChannel();
        context.setChannel(channel);
        assertSame(channel, context.getChannel());
    }

    public void testLocalObject() {

        MockDataChannel channel = new MockDataChannel();
        CayenneContext src = new CayenneContext(channel);
        src.setEntityResolver(getDomain().getEntityResolver().getClientEntityResolver());

        List sources = new ArrayList();

        ClientMtTable1 s1 = new ClientMtTable1();
        s1.setPersistenceState(PersistenceState.COMMITTED);
        s1.setObjectId(new ObjectId("MtTable1", MtTable1.TABLE1_ID_PK_COLUMN, 1));
        s1.setGlobalAttribute1("abc");
        s1.setObjectContext(src);
        src.getGraphManager().registerNode(s1.getObjectId(), s1);
        sources.add(s1);

        ClientMtTable1 s2 = new ClientMtTable1();
        s2.setPersistenceState(PersistenceState.COMMITTED);
        s2.setObjectId(new ObjectId("MtTable1", MtTable1.TABLE1_ID_PK_COLUMN, 2));
        s2.setGlobalAttribute1("xyz");
        s2.setObjectContext(src);
        src.getGraphManager().registerNode(s2.getObjectId(), s2);
        sources.add(s2);

        ClientMtTable1 s3 = new ClientMtTable1();
        s3.setPersistenceState(PersistenceState.HOLLOW);
        s3.setObjectId(new ObjectId("MtTable1", MtTable1.TABLE1_ID_PK_COLUMN, 3));
        s3.setObjectContext(src);
        src.getGraphManager().registerNode(s3.getObjectId(), s3);
        sources.add(s3);

        CayenneContext target = new CayenneContext(channel);
        target.setEntityResolver(getDomain()
                .getEntityResolver()
                .getClientEntityResolver());

        for (int i = 0; i < sources.size(); i++) {
            Persistent srcObject = (Persistent) sources.get(i);
            Persistent targetObject = target.localObject(
                    srcObject.getObjectId(),
                    srcObject);

            assertSame(target, targetObject.getObjectContext());
            assertSame(src, srcObject.getObjectContext());
            assertEquals(srcObject.getObjectId(), targetObject.getObjectId());
            assertSame(targetObject, target.getGraphManager().getNode(
                    targetObject.getObjectId()));
        }
    }

    public void testChannel() {
        MockDataChannel channel = new MockDataChannel();
        CayenneContext context = new CayenneContext(channel);

        assertSame(channel, context.getChannel());
    }

    public void testCommitUnchanged() {

        MockDataChannel channel = new MockDataChannel();
        CayenneContext context = new CayenneContext(channel);

        // no context changes so no connector access is expected
        context.commitChanges();
        assertTrue(channel.getRequestObjects().isEmpty());
    }

    public void testCommitCommandExecuted() {

        MockDataChannel channel = new MockDataChannel(new MockGraphDiff());
        channel.setEntityResolver(getDomain()
                .getEntityResolver()
                .getClientEntityResolver());
        CayenneContext context = new CayenneContext(channel);

        // test that a command is being sent via connector on commit...

        context.internalGraphManager().nodePropertyChanged(
                new ObjectId("MtTable1"),
                "x",
                "y",
                "z");

        context.commitChanges();
        assertEquals(1, channel.getRequestObjects().size());

        // expect a sync/commit chain
        Object mainMessage = channel.getRequestObjects().iterator().next();
        assertTrue(mainMessage instanceof GraphDiff);
    }

    public void testCommitChangesNew() {
        final CompoundDiff diff = new CompoundDiff();
        final Object newObjectId = new ObjectId("test", "key", "generated");
        final EventManager eventManager = new EventManager(0);

        // test that ids that are passed back are actually propagated to the right
        // objects...

        MockDataChannel channel = new MockDataChannel() {

            @Override
            public GraphDiff onSync(
                    ObjectContext originatingContext,
                    GraphDiff changes,
                    int syncType) {

                return diff;
            }

            // must provide a channel with working event manager
            @Override
            public EventManager getEventManager() {
                return eventManager;
            }
        };

        CayenneContext context = new CayenneContext(channel);
        ObjEntity entity = new ObjEntity("test_entity");
        entity.setClassName(MockPersistentObject.class.getName());

        DataMap dataMap = new DataMap("test");
        dataMap.addObjEntity(entity);
        Collection entities = Collections.singleton(dataMap);
        context.setEntityResolver(new EntityResolver(entities));
        Persistent object = context.newObject(MockPersistentObject.class);

        // record change here to make it available to the anonymous connector method..
        diff.add(new NodeIdChangeOperation(object.getObjectId(), newObjectId));

        // check that a generated object ID is assigned back to the object...
        assertNotSame(newObjectId, object.getObjectId());
        context.commitChanges();
        assertSame(newObjectId, object.getObjectId());
        assertSame(object, context.graphManager.getNode(newObjectId));
    }

    public void testNewObject() {

        CayenneContext context = new CayenneContext(new MockDataChannel());

        ObjEntity entity = new ObjEntity("test_entity");
        entity.setClassName(MockPersistentObject.class.getName());

        DataMap dataMap = new DataMap("test");
        dataMap.addObjEntity(entity);
        Collection entities = Collections.singleton(dataMap);
        context.setEntityResolver(new EntityResolver(entities));

        Persistent object = context.newObject(MockPersistentObject.class);
        assertNotNull(object);
        assertTrue(object instanceof MockPersistentObject);
        assertEquals(PersistenceState.NEW, object.getPersistenceState());
        assertSame(context, object.getObjectContext());
        assertTrue(context
                .internalGraphManager()
                .dirtyNodes(PersistenceState.NEW)
                .contains(object));
        assertNotNull(object.getObjectId());
        assertTrue(object.getObjectId().isTemporary());
    }

    public void testDeleteObject() {

        CayenneContext context = new CayenneContext(new MockDataChannel());
        ObjEntity entity = new ObjEntity("test_entity");
        entity.setClassName(MockPersistentObject.class.getName());

        DataMap dataMap = new DataMap("test");
        dataMap.addObjEntity(entity);
        Collection entities = Collections.singleton(dataMap);
        context.setEntityResolver(new EntityResolver(entities));

        // TRANSIENT ... should quietly ignore it
        Persistent transientObject = new MockPersistentObject();
        context.deleteObject(transientObject);
        assertEquals(PersistenceState.TRANSIENT, transientObject.getPersistenceState());

        // NEW ... should make it TRANSIENT
        // create via context to make sure that object store would register it
        Persistent newObject = context.newObject(MockPersistentObject.class);
        assertNotNull(newObject.getObjectContext());
        context.deleteObject(newObject);
        assertNull(newObject.getObjectContext());
        assertEquals(PersistenceState.TRANSIENT, newObject.getPersistenceState());
        assertFalse(context.internalGraphManager().dirtyNodes().contains(
                newObject.getObjectId()));

        // see CAY-547 for details...
        assertFalse(context.internalGraphManager().dirtyNodes().contains(null));

        // COMMITTED
        Persistent committed = new MockPersistentObject();
        committed.setPersistenceState(PersistenceState.COMMITTED);
        committed.setObjectId(new ObjectId("test_entity", "key", "value1"));
        committed.setObjectContext(context);
        context.deleteObject(committed);
        assertEquals(PersistenceState.DELETED, committed.getPersistenceState());

        // MODIFIED
        Persistent modified = new MockPersistentObject();
        modified.setPersistenceState(PersistenceState.MODIFIED);
        modified.setObjectId(new ObjectId("test_entity", "key", "value2"));
        modified.setObjectContext(context);
        context.deleteObject(modified);
        assertEquals(PersistenceState.DELETED, modified.getPersistenceState());

        // DELETED
        Persistent deleted = new MockPersistentObject();
        deleted.setPersistenceState(PersistenceState.DELETED);
        deleted.setObjectId(new ObjectId("test_entity", "key", "value3"));
        deleted.setObjectContext(context);
        context.deleteObject(deleted);
        assertEquals(PersistenceState.DELETED, committed.getPersistenceState());
    }

    public void testBeforePropertyReadShouldInflateHollow() {

        ObjectId gid = new ObjectId("MtTable1", "a", "b");
        final ClientMtTable1 inflated = new ClientMtTable1();
        inflated.setPersistenceState(PersistenceState.COMMITTED);
        inflated.setObjectId(gid);
        inflated.setGlobalAttribute1("abc");

        MockClientConnection connection = new MockClientConnection(new GenericResponse(
                Arrays.asList(inflated)));
        ClientChannel channel = new ClientChannel(connection);

        // check that a HOLLOW object is infalted on "beforePropertyRead"
        ClientMtTable1 hollow = new ClientMtTable1();
        hollow.setPersistenceState(PersistenceState.HOLLOW);
        hollow.setObjectId(gid);

        final boolean[] selectExecuted = new boolean[1];
        CayenneContext context = new CayenneContext(channel) {

            @Override
            public List performQuery(Query query) {
                selectExecuted[0] = true;
                return super.performQuery(query);
            }
        };

        context.setEntityResolver(getDomain()
                .getEntityResolver()
                .getClientEntityResolver());

        context.graphManager.registerNode(hollow.getObjectId(), hollow);

        // testing this...
        context
                .prepareForAccess(
                        hollow,
                        ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY,
                        false);
        assertTrue(selectExecuted[0]);
        assertSame(hollow, context.getGraphManager().getNode(gid));
        assertEquals(inflated.getGlobalAttribute1Direct(), hollow
                .getGlobalAttribute1Direct());
        assertEquals(PersistenceState.COMMITTED, hollow.getPersistenceState());
    }
    
    public void testBeforeHollowDeleteShouldChangeStateToCommited() {

        ObjectId gid = new ObjectId("MtTable1", "a", "b");
        final ClientMtTable1 inflated = new ClientMtTable1();
        inflated.setPersistenceState(PersistenceState.COMMITTED);
        inflated.setObjectId(gid);
        inflated.setGlobalAttribute1("abc");

        MockClientConnection connection = new MockClientConnection(new GenericResponse(
                Arrays.asList(inflated)));
        ClientChannel channel = new ClientChannel(connection);

        CayenneContext context = new CayenneContext(channel);
        context.setEntityResolver(getDomain()
                .getEntityResolver()
                .getClientEntityResolver());
        ClientMtTable1 hollow = (ClientMtTable1) context.localObject(gid, null);
        assertEquals(PersistenceState.HOLLOW, hollow.getPersistenceState());

        // testing this...
        context.deleteObject(hollow);
        assertSame(hollow, context.getGraphManager().getNode(gid));
        assertEquals(inflated.getGlobalAttribute1Direct(), hollow
                .getGlobalAttribute1Direct());
        assertEquals(PersistenceState.DELETED, hollow.getPersistenceState());
    }

}
