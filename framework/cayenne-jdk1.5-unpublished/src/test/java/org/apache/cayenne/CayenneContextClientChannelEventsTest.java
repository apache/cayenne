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

import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.rop.client.ClientRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.testdo.mt.ClientMtTable4;
import org.apache.cayenne.testdo.mt.ClientMtTable5;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.client.ClientRuntimeProperty;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

/**
 * Tests peer context synchronization via ClientChannel events.
 */
@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
@ClientRuntimeProperty({
        Constants.ROP_CHANNEL_EVENTS_PROPERTY, "true"
})
public class CayenneContextClientChannelEventsTest extends ClientCase {

    @Inject
    private DBHelper dbHelper;

    @Inject
    private ClientRuntime runtime;

    private TableHelper tMtTable1;
    private TableHelper tMtTable2;
    private TableHelper tMtTable4;
    private TableHelper tMtTable5;
    private TableHelper tMtJoin45;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("MT_TABLE2");
        dbHelper.deleteAll("MT_TABLE1");
        dbHelper.deleteAll("MT_JOIN45");
        dbHelper.deleteAll("MT_TABLE4");
        dbHelper.deleteAll("MT_TABLE5");

        tMtTable1 = new TableHelper(dbHelper, "MT_TABLE1");
        tMtTable1.setColumns("TABLE1_ID", "GLOBAL_ATTRIBUTE1", "SERVER_ATTRIBUTE1");

        tMtTable2 = new TableHelper(dbHelper, "MT_TABLE2");
        tMtTable2.setColumns("TABLE2_ID", "TABLE1_ID", "GLOBAL_ATTRIBUTE");

        tMtTable4 = new TableHelper(dbHelper, "MT_TABLE4");
        tMtTable4.setColumns("ID");

        tMtTable5 = new TableHelper(dbHelper, "MT_TABLE5");
        tMtTable5.setColumns("ID");

        tMtJoin45 = new TableHelper(dbHelper, "MT_JOIN45");
        tMtJoin45.setColumns("TABLE4_ID", "TABLE5_ID");
    }

    public void testSyncNewObject() throws Exception {

        CayenneContext c1 = (CayenneContext) runtime.newContext();
        CayenneContext c2 = (CayenneContext) runtime.newContext();
        assertNotSame(c1, c2);

        ClientMtTable1 o1 = c1.newObject(ClientMtTable1.class);
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

        CayenneContext c1 = (CayenneContext) runtime.newContext();
        CayenneContext c2 = (CayenneContext) runtime.newContext();
        assertNotSame(c1, c2);

        // insert, then delete - this shouldn't propagate via an event.
        ClientMtTable1 o1 = c1.newObject(ClientMtTable1.class);
        o1.setGlobalAttribute1("X");
        c1.deleteObjects(o1);

        // introduce some other change so that commit can go ahead...
        ClientMtTable1 o1x = c1.newObject(ClientMtTable1.class);
        o1x.setGlobalAttribute1("Y");
        c1.commitChanges();

        ClientMtTable1 o2 = (ClientMtTable1) c2.getGraphManager().getNode(
                o1.getObjectId());

        assertNull(o2);

        assertFalse(c1.internalGraphManager().hasChanges());
        assertFalse(c2.internalGraphManager().hasChanges());
    }

    public void testSyncNewObjectIntoDirtyContext() throws Exception {

        CayenneContext c1 = (CayenneContext) runtime.newContext();
        CayenneContext c2 = (CayenneContext) runtime.newContext();
        assertNotSame(c1, c2);

        // make sure c2 has uncommitted changes
        c2.newObject(ClientMtTable1.class);

        ClientMtTable1 o1 = c1.newObject(ClientMtTable1.class);
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

        tMtTable1.insert(1, "g1", "s1");

        CayenneContext c1 = (CayenneContext) runtime.newContext();
        CayenneContext c2 = (CayenneContext) runtime.newContext();
        assertNotSame(c1, c2);

        ClientMtTable1 o1 = (ClientMtTable1) Cayenne.objectForQuery(
                c1,
                new ObjectIdQuery(new ObjectId("MtTable1", "TABLE1_ID", 1)));

        ClientMtTable1 o2 = (ClientMtTable1) Cayenne.objectForQuery(
                c2,
                new ObjectIdQuery(new ObjectId("MtTable1", "TABLE1_ID", 1)));

        assertEquals("g1", o1.getGlobalAttribute1());
        assertEquals("g1", o2.getGlobalAttribute1());

        o1.setGlobalAttribute1("X");
        c1.commitChanges();
        
        // let the events propagate to peers
        Thread.sleep(500);

        assertEquals("X", o2.getGlobalAttribute1());
        assertFalse(c1.internalGraphManager().hasChanges());
        assertFalse(c2.internalGraphManager().hasChanges());
    }

    public void testSyncToOneRelationship() throws Exception {

        tMtTable1.insert(1, "g1", "s1");
        tMtTable1.insert(2, "g2", "s2");
        tMtTable2.insert(1, 1, "g1");

        CayenneContext c1 = (CayenneContext) runtime.newContext();
        CayenneContext c2 = (CayenneContext) runtime.newContext();

        ClientMtTable2 o1 = (ClientMtTable2) Cayenne.objectForQuery(
                c1,
                new ObjectIdQuery(new ObjectId("MtTable2", "TABLE2_ID", 1)));

        ClientMtTable2 o2 = (ClientMtTable2) Cayenne.objectForQuery(
                c2,
                new ObjectIdQuery(new ObjectId("MtTable2", "TABLE2_ID", 1)));

        assertEquals("g1", o1.getTable1().getGlobalAttribute1());
        assertEquals("g1", o2.getTable1().getGlobalAttribute1());

        ClientMtTable1 o1r = (ClientMtTable1) Cayenne.objectForQuery(
                c1,
                new ObjectIdQuery(new ObjectId("MtTable1", "TABLE1_ID", 2)));
        o1.setTable1(o1r);
        c1.commitChanges();
        
        // let the events propagate to peers
        Thread.sleep(500);

        assertEquals("g2", o2.getTable1().getGlobalAttribute1());
        assertEquals(o1r.getObjectId(), o2.getTable1().getObjectId());
        assertFalse(c1.internalGraphManager().hasChanges());
        assertFalse(c2.internalGraphManager().hasChanges());
    }

    public void testSyncToManyRelationship() throws Exception {
        tMtTable1.insert(1, "g1", "s1");
        tMtTable2.insert(1, 1, "g1");

        CayenneContext c1 = (CayenneContext) runtime.newContext();
        CayenneContext c2 = (CayenneContext) runtime.newContext();

        ClientMtTable1 o1 = (ClientMtTable1) Cayenne.objectForQuery(
                c1,
                new ObjectIdQuery(new ObjectId("MtTable1", "TABLE1_ID", 1)));

        ClientMtTable1 o2 = (ClientMtTable1) Cayenne.objectForQuery(
                c2,
                new ObjectIdQuery(new ObjectId("MtTable1", "TABLE1_ID", 1)));

        assertEquals(1, o1.getTable2Array().size());
        assertEquals(1, o2.getTable2Array().size());

        ClientMtTable2 o1r = c1.newObject(ClientMtTable2.class);
        o1r.setGlobalAttribute("X");
        o1.addToTable2Array(o1r);

        c1.commitChanges();
        
        // let the events propagate to peers
        Thread.sleep(500);

        assertEquals(2, o1.getTable2Array().size());
        assertEquals(2, o2.getTable2Array().size());
        assertFalse(c1.internalGraphManager().hasChanges());
        assertFalse(c2.internalGraphManager().hasChanges());
    }

    public void testSyncToManyRelationship1() throws Exception {
        tMtTable1.insert(1, "g1", "s1");
        tMtTable2.insert(1, 1, "g1");

        CayenneContext c1 = (CayenneContext) runtime.newContext();
        CayenneContext c2 = (CayenneContext) runtime.newContext();

        ClientMtTable1 o1 = (ClientMtTable1) Cayenne.objectForQuery(
                c1,
                new ObjectIdQuery(new ObjectId("MtTable1", "TABLE1_ID", 1)));

        // do not resolve objects in question in the second context and see if the merge
        // causes any issues...

        assertEquals(1, o1.getTable2Array().size());

        ClientMtTable2 o1r = c1.newObject(ClientMtTable2.class);
        o1r.setGlobalAttribute("X");
        o1.addToTable2Array(o1r);

        c1.commitChanges();

        assertEquals(2, o1.getTable2Array().size());

        assertFalse(c1.internalGraphManager().hasChanges());
        assertFalse(c2.internalGraphManager().hasChanges());

        ClientMtTable1 o2 = (ClientMtTable1) Cayenne.objectForQuery(
                c2,
                new ObjectIdQuery(new ObjectId("MtTable1", "TABLE1_ID", 1)));
        assertEquals(2, o2.getTable2Array().size());
    }

    public void testSyncManyToManyRelationship() throws Exception {
        tMtTable4.insert(1);
        tMtTable5.insert(1);
        tMtTable5.insert(2);
        tMtJoin45.insert(1, 1);
        tMtJoin45.insert(1, 2);

        CayenneContext c1 = (CayenneContext) runtime.newContext();
        CayenneContext c2 = (CayenneContext) runtime.newContext();

        ClientMtTable4 o1 = (ClientMtTable4) Cayenne.objectForQuery(
                c1,
                new ObjectIdQuery(new ObjectId("MtTable4", "ID", 1)));

        ClientMtTable4 o2 = (ClientMtTable4) Cayenne.objectForQuery(
                c2,
                new ObjectIdQuery(new ObjectId("MtTable4", "ID", 1)));

        assertEquals(2, o1.getTable5s().size());
        assertEquals(2, o2.getTable5s().size());

        ClientMtTable5 o1r = (ClientMtTable5) Cayenne.objectForQuery(
                c1,
                new ObjectIdQuery(new ObjectId("MtTable5", "ID", 1)));
        o1.removeFromTable5s(o1r);

        c1.commitChanges();
        // let the events propagate to peers
        Thread.sleep(500);

        assertEquals(1, o1.getTable5s().size());
        assertEquals(1, o2.getTable5s().size());
        assertFalse(c1.internalGraphManager().hasChanges());
        assertFalse(c2.internalGraphManager().hasChanges());
    }

    public void testSyncManyToManyRelationship1() throws Exception {

        CayenneContext c1 = (CayenneContext) runtime.newContext();
        CayenneContext c2 = (CayenneContext) runtime.newContext();

        ClientMtTable4 o1 = c1.newObject(ClientMtTable4.class);
        ClientMtTable5 o1r = c1.newObject(ClientMtTable5.class);
        c1.commitChanges();

        ClientMtTable4 o2 = c2.localObject(o1);
        ClientMtTable5 o2r = c2.localObject(o1r);

        o2.addToTable5s(o2r);
        c2.commitChanges();

        assertEquals(1, o1.getTable5s().size());
        assertEquals(1, o2.getTable5s().size());
        assertFalse(c1.internalGraphManager().hasChanges());
        assertFalse(c2.internalGraphManager().hasChanges());
    }
}
