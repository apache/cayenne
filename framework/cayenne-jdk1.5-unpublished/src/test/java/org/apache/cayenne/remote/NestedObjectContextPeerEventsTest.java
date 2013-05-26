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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.configuration.rop.client.ClientRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class NestedObjectContextPeerEventsTest extends RemoteCayenneCase {

    @Inject
    private ClientRuntime runtime;
    
    @Inject
    private DBHelper dbHelper;

    @Override
    public void setUpAfterInjection() throws Exception {
        super.setUpAfterInjection();

        dbHelper.deleteAll("MT_TABLE2");
        dbHelper.deleteAll("MT_TABLE1");
    }

    public void testPeerObjectUpdatedTempOID() throws Exception {
        ObjectContext peer1 = runtime.newContext(clientContext);
        ClientMtTable1 a1 = peer1.newObject(ClientMtTable1.class);
        a1.setGlobalAttribute1("Y");
        ObjectId a1TempId = a1.getObjectId();

        ObjectContext peer2 = runtime.newContext(clientContext);
        ClientMtTable1 a2 = peer2.localObject(a1);

        assertEquals(a1TempId, a2.getObjectId());

        peer1.commitChanges();
        assertFalse(a1.getObjectId().isTemporary());
        assertFalse(a2.getObjectId().isTemporary());
        assertEquals(a2.getObjectId(), a1.getObjectId());
    }

    public void testPeerObjectUpdatedSimpleProperty() throws Exception {
        ClientMtTable1 a = clientContext.newObject(ClientMtTable1.class);
        a.setGlobalAttribute1("X");
        clientContext.commitChanges();

        ObjectContext peer1 = runtime.newContext(clientContext);
        ClientMtTable1 a1 = peer1.localObject(a);

        ObjectContext peer2 = runtime.newContext(clientContext);
        ClientMtTable1 a2 = peer2.localObject(a);

        a1.setGlobalAttribute1("Y");
        assertEquals("X", a2.getGlobalAttribute1());
        peer1.commitChangesToParent();
        assertEquals("Y", a2.getGlobalAttribute1());

        assertFalse(
                "Peer data context became dirty on event processing",
                peer2.hasChanges());
    }

    public void testPeerObjectUpdatedToOneRelationship() throws Exception {
        ClientMtTable1 a = clientContext.newObject(ClientMtTable1.class);
        ClientMtTable1 altA = clientContext.newObject(ClientMtTable1.class);

        ClientMtTable2 p = clientContext.newObject(ClientMtTable2.class);
        p.setTable1(a);
        p.setGlobalAttribute("PPP");
        a.setGlobalAttribute1("X");
        altA.setGlobalAttribute1("Y");
        clientContext.commitChanges();

        ObjectContext peer1 = runtime.newContext(clientContext);
        ClientMtTable2 p1 = peer1.localObject(p);
        ClientMtTable1 altA1 = peer1.localObject(altA);

        ObjectContext peer2 = runtime.newContext(clientContext);
        ClientMtTable2 p2 = peer2.localObject(p);
        ClientMtTable1 altA2 = peer2.localObject(altA);
        ClientMtTable1 a2 = peer2.localObject(a);

        p1.setTable1(altA1);
        assertSame(a2, p2.getTable1());
        peer1.commitChangesToParent();
        assertEquals(altA2, p2.getTable1());

        assertFalse(
                "Peer data context became dirty on event processing",
                peer2.hasChanges());
    }

    public void testPeerObjectUpdatedToManyRelationship() throws Exception {
        ClientMtTable1 a = clientContext.newObject(ClientMtTable1.class);
        a.setGlobalAttribute1("X");

        ClientMtTable2 px = clientContext.newObject(ClientMtTable2.class);
        px.setTable1(a);
        px.setGlobalAttribute("PX");

        ClientMtTable2 py = clientContext.newObject(ClientMtTable2.class);
        py.setGlobalAttribute("PY");

        clientContext.commitChanges();

        ObjectContext peer1 = runtime.newContext(clientContext);
        ClientMtTable2 py1 = peer1.localObject(py);
        ClientMtTable1 a1 = peer1.localObject(a);

        ObjectContext peer2 = runtime.newContext(clientContext);
        ClientMtTable2 py2 = peer2.localObject(py);
        ClientMtTable1 a2 = peer2.localObject(a);

        a1.addToTable2Array(py1);
        assertEquals(1, a2.getTable2Array().size());
        assertFalse(a2.getTable2Array().contains(py2));
        peer1.commitChangesToParent();
        assertEquals(2, a2.getTable2Array().size());
        assertTrue(a2.getTable2Array().contains(py2));

        assertFalse(
                "Peer data context became dirty on event processing",
                peer2.hasChanges());
    }
}
