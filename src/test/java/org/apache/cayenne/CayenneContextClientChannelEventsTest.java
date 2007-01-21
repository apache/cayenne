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

import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.remote.ClientChannel;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.testdo.mt.ClientMtTable4;
import org.apache.cayenne.testdo.mt.ClientMtTable5;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;

/**
 * Tests peer context synchronization via ClientChannel events.
 * 
 * @author Andrus Adamchik
 */
public class CayenneContextClientChannelEventsTest extends CayenneCase {

    protected AccessStack buildAccessStack() {
        return CayenneResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testSyncNewObject() throws Exception {
        // this resets snapshot cache...
        createDataContext();
        deleteTestData();

        DataChannel serverChannel = new ClientServerChannel(getDomain());
        ClientChannel clientChannel = new ClientChannel(
                new LocalConnection(serverChannel),
                true);

        CayenneContext c1 = new CayenneContext(clientChannel);
        CayenneContext c2 = new CayenneContext(clientChannel);

        ClientMtTable1 o1 = (ClientMtTable1) c1.newObject(ClientMtTable1.class);
        o1.setGlobalAttribute1("X");
        c1.commitChanges();

        ClientMtTable1 o2 = (ClientMtTable1) c2.getGraphManager().getNode(
                o1.getObjectId());

        assertNull(o2);
        // now fetch it fresh

        o2 = (ClientMtTable1) c2.performQuery(new ObjectIdQuery(o1.getObjectId())).get(0);
        assertNotNull(o2);

        assertEquals("X", o2.getGlobalAttribute1());
        assertEquals(PersistenceState.COMMITTED, o2.getPersistenceState());
        assertFalse(c1.internalGraphManager().hasChanges());
        assertFalse(c2.internalGraphManager().hasChanges());
    }

    public void testSyncNewDeletedObject() throws Exception {
        // this resets snapshot cache...
        createDataContext();
        deleteTestData();

        DataChannel serverChannel = new ClientServerChannel(getDomain());
        ClientChannel clientChannel = new ClientChannel(
                new LocalConnection(serverChannel),
                true);

        CayenneContext c1 = new CayenneContext(clientChannel);
        CayenneContext c2 = new CayenneContext(clientChannel);

        // insert, then delete - this shouldn't propagate via an event.
        ClientMtTable1 o1 = (ClientMtTable1) c1.newObject(ClientMtTable1.class);
        o1.setGlobalAttribute1("X");
        c1.deleteObject(o1);

        // introduce some other change so that commit can go ahead...
        ClientMtTable1 o1x = (ClientMtTable1) c1.newObject(ClientMtTable1.class);
        o1x.setGlobalAttribute1("Y");
        c1.commitChanges();

        ClientMtTable1 o2 = (ClientMtTable1) c2.getGraphManager().getNode(
                o1.getObjectId());

        assertNull(o2);

        assertFalse(c1.internalGraphManager().hasChanges());
        assertFalse(c2.internalGraphManager().hasChanges());
    }

    public void testSyncNewObjectIntoDirtyContext() throws Exception {
        // this resets snapshot cache...
        createDataContext();
        deleteTestData();

        DataChannel serverChannel = new ClientServerChannel(getDomain());
        ClientChannel clientChannel = new ClientChannel(
                new LocalConnection(serverChannel),
                true);

        CayenneContext c1 = new CayenneContext(clientChannel);
        CayenneContext c2 = new CayenneContext(clientChannel);

        // make sure c2 has uncommitted changes
        c2.newObject(ClientMtTable1.class);

        ClientMtTable1 o1 = (ClientMtTable1) c1.newObject(ClientMtTable1.class);
        o1.setGlobalAttribute1("X");
        c1.commitChanges();

        ClientMtTable1 o2 = (ClientMtTable1) c2.getGraphManager().getNode(
                o1.getObjectId());
        assertNull(o2);

        // now fetch it fresh

        o2 = (ClientMtTable1) c2.performQuery(new ObjectIdQuery(o1.getObjectId())).get(0);
        assertNotNull(o2);
        assertEquals("X", o2.getGlobalAttribute1());
        assertEquals(PersistenceState.COMMITTED, o2.getPersistenceState());
        assertFalse(c1.internalGraphManager().hasChanges());
        assertTrue(c2.internalGraphManager().hasChanges());
    }

    public void testSyncSimpleProperty() throws Exception {
        // this resets snapshot cache...
        createDataContext();

        deleteTestData();
        createTestData("testSyncSimpleProperty");

        DataChannel serverChannel = new ClientServerChannel(getDomain());
        ClientChannel clientChannel = new ClientChannel(
                new LocalConnection(serverChannel),
                true);

        CayenneContext c1 = new CayenneContext(clientChannel);
        CayenneContext c2 = new CayenneContext(clientChannel);

        ClientMtTable1 o1 = (ClientMtTable1) DataObjectUtils.objectForQuery(
                c1,
                new ObjectIdQuery(new ObjectId("MtTable1", "TABLE1_ID", 1)));

        ClientMtTable1 o2 = (ClientMtTable1) DataObjectUtils.objectForQuery(
                c2,
                new ObjectIdQuery(new ObjectId("MtTable1", "TABLE1_ID", 1)));

        assertEquals("g1", o1.getGlobalAttribute1());
        assertEquals("g1", o2.getGlobalAttribute1());

        o1.setGlobalAttribute1("X");
        c1.commitChanges();

        assertEquals("X", o2.getGlobalAttribute1());
        assertFalse(c1.internalGraphManager().hasChanges());
        assertFalse(c2.internalGraphManager().hasChanges());
    }

    public void testSyncToOneRelationship() throws Exception {
        // this resets snapshot cache...
        createDataContext();

        deleteTestData();
        createTestData("testSyncToOneRelationship");

        DataChannel serverChannel = new ClientServerChannel(getDomain());
        ClientChannel clientChannel = new ClientChannel(
                new LocalConnection(serverChannel),
                true);

        CayenneContext c1 = new CayenneContext(clientChannel);
        CayenneContext c2 = new CayenneContext(clientChannel);

        ClientMtTable2 o1 = (ClientMtTable2) DataObjectUtils.objectForQuery(
                c1,
                new ObjectIdQuery(new ObjectId("MtTable2", "TABLE2_ID", 1)));

        ClientMtTable2 o2 = (ClientMtTable2) DataObjectUtils.objectForQuery(
                c2,
                new ObjectIdQuery(new ObjectId("MtTable2", "TABLE2_ID", 1)));

        assertEquals("g1", o1.getTable1().getGlobalAttribute1());
        assertEquals("g1", o2.getTable1().getGlobalAttribute1());

        ClientMtTable1 o1r = (ClientMtTable1) DataObjectUtils.objectForQuery(
                c1,
                new ObjectIdQuery(new ObjectId("MtTable1", "TABLE1_ID", 2)));
        o1.setTable1(o1r);
        c1.commitChanges();

        assertEquals("g2", o2.getTable1().getGlobalAttribute1());
        assertEquals(o1r.getObjectId(), o2.getTable1().getObjectId());
        assertFalse(c1.internalGraphManager().hasChanges());
        assertFalse(c2.internalGraphManager().hasChanges());
    }

    public void testSyncToManyRelationship() throws Exception {
        // this resets snapshot cache...
        createDataContext();

        deleteTestData();
        createTestData("testSyncToManyRelationship");

        DataChannel serverChannel = new ClientServerChannel(getDomain());
        ClientChannel clientChannel = new ClientChannel(
                new LocalConnection(serverChannel),
                true);

        CayenneContext c1 = new CayenneContext(clientChannel);
        CayenneContext c2 = new CayenneContext(clientChannel);

        ClientMtTable1 o1 = (ClientMtTable1) DataObjectUtils.objectForQuery(
                c1,
                new ObjectIdQuery(new ObjectId("MtTable1", "TABLE1_ID", 1)));

        ClientMtTable1 o2 = (ClientMtTable1) DataObjectUtils.objectForQuery(
                c2,
                new ObjectIdQuery(new ObjectId("MtTable1", "TABLE1_ID", 1)));

        assertEquals(1, o1.getTable2Array().size());
        assertEquals(1, o2.getTable2Array().size());

        ClientMtTable2 o1r = (ClientMtTable2) c1.newObject(ClientMtTable2.class);
        o1r.setGlobalAttribute("X");
        o1.addToTable2Array(o1r);

        c1.commitChanges();

        assertEquals(2, o1.getTable2Array().size());
        assertEquals(2, o2.getTable2Array().size());
        assertFalse(c1.internalGraphManager().hasChanges());
        assertFalse(c2.internalGraphManager().hasChanges());
    }

    public void testSyncToManyRelationship1() throws Exception {
        // this resets snapshot cache...
        createDataContext();

        deleteTestData();
        createTestData("testSyncToManyRelationship");

        DataChannel serverChannel = new ClientServerChannel(getDomain());
        ClientChannel clientChannel = new ClientChannel(
                new LocalConnection(serverChannel),
                true);

        CayenneContext c1 = new CayenneContext(clientChannel);
        CayenneContext c2 = new CayenneContext(clientChannel);

        ClientMtTable1 o1 = (ClientMtTable1) DataObjectUtils.objectForQuery(
                c1,
                new ObjectIdQuery(new ObjectId("MtTable1", "TABLE1_ID", 1)));

        // do not resolve objects in question in the second context and see if the merge
        // causes any issues...

        assertEquals(1, o1.getTable2Array().size());

        ClientMtTable2 o1r = (ClientMtTable2) c1.newObject(ClientMtTable2.class);
        o1r.setGlobalAttribute("X");
        o1.addToTable2Array(o1r);

        c1.commitChanges();

        assertEquals(2, o1.getTable2Array().size());

        assertFalse(c1.internalGraphManager().hasChanges());
        assertFalse(c2.internalGraphManager().hasChanges());

        ClientMtTable1 o2 = (ClientMtTable1) DataObjectUtils.objectForQuery(
                c2,
                new ObjectIdQuery(new ObjectId("MtTable1", "TABLE1_ID", 1)));
        assertEquals(2, o2.getTable2Array().size());
    }

    public void testSyncManyToManyRelationship() throws Exception {
        // this resets snapshot cache...
        createDataContext();

        deleteTestData();
        createTestData("testSyncManyToManyRelationship");

        DataChannel serverChannel = new ClientServerChannel(getDomain());
        ClientChannel clientChannel = new ClientChannel(
                new LocalConnection(serverChannel),
                true);

        CayenneContext c1 = new CayenneContext(clientChannel);
        CayenneContext c2 = new CayenneContext(clientChannel);

        ClientMtTable4 o1 = (ClientMtTable4) DataObjectUtils.objectForQuery(
                c1,
                new ObjectIdQuery(new ObjectId("MtTable4", "ID", 1)));

        ClientMtTable4 o2 = (ClientMtTable4) DataObjectUtils.objectForQuery(
                c2,
                new ObjectIdQuery(new ObjectId("MtTable4", "ID", 1)));

        assertEquals(2, o1.getTable5s().size());
        assertEquals(2, o2.getTable5s().size());

        ClientMtTable5 o1r = (ClientMtTable5) DataObjectUtils.objectForQuery(
                c1,
                new ObjectIdQuery(new ObjectId("MtTable5", "ID", 1)));
        o1.removeFromTable5s(o1r);

        c1.commitChanges();

        assertEquals(1, o1.getTable5s().size());
        assertEquals(1, o2.getTable5s().size());
        assertFalse(c1.internalGraphManager().hasChanges());
        assertFalse(c2.internalGraphManager().hasChanges());
    }

    public void testSyncManyToManyRelationship1() throws Exception {
        // this resets snapshot cache...
        createDataContext();

        deleteTestData();

        DataChannel serverChannel = new ClientServerChannel(getDomain());
        ClientChannel clientChannel = new ClientChannel(
                new LocalConnection(serverChannel),
                true);

        CayenneContext c1 = new CayenneContext(clientChannel);
        CayenneContext c2 = new CayenneContext(clientChannel);

        ClientMtTable4 o1 = (ClientMtTable4) c1.newObject(ClientMtTable4.class);
        ClientMtTable5 o1r = (ClientMtTable5) c1.newObject(ClientMtTable5.class);
        c1.commitChanges();

        ClientMtTable4 o2 = (ClientMtTable4) c2.localObject(o1.getObjectId(), null);
        ClientMtTable5 o2r = (ClientMtTable5) c2.localObject(o1r.getObjectId(), null);

        o2.addToTable5s(o2r);
        c2.commitChanges();

        assertEquals(1, o1.getTable5s().size());
        assertEquals(1, o2.getTable5s().size());
        assertFalse(c1.internalGraphManager().hasChanges());
        assertFalse(c2.internalGraphManager().hasChanges());
    }
}
