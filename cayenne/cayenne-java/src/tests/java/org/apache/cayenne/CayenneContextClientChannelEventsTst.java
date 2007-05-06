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
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneTestCase;
import org.apache.cayenne.unit.CayenneTestResources;

/**
 * Tests peer context synchronization via ClientChannel events.
 * 
 * @author Andrus Adamchik
 */
public class CayenneContextClientChannelEventsTst extends CayenneTestCase {

    protected AccessStack buildAccessStack() {
        return CayenneTestResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testSyncSimpleProperty() throws Exception {
        // this resets snapshot cache...
        createDataContext();

        deleteTestData();
        createTestData("testSyncSimpleProperty");

        DataChannel serverChannel = new ClientServerChannel(getDomain(), false);
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

        DataChannel serverChannel = new ClientServerChannel(getDomain(), false);
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
        assertFalse(c1.internalGraphManager().hasChanges());
        assertFalse(c2.internalGraphManager().hasChanges());
    }

    public void testSyncToManyRelationship() throws Exception {
        // this resets snapshot cache...
        createDataContext();

        deleteTestData();
        createTestData("testSyncToManyRelationship");

        DataChannel serverChannel = new ClientServerChannel(getDomain(), false);
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

        DataChannel serverChannel = new ClientServerChannel(getDomain(), false);
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
}
