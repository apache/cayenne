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

package org.apache.cayenne.util;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class ObjectDetachOperationTest extends ClientCase {

    @Inject
    private DataContext serverContext;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tMtTable1;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("MT_TABLE2");
        dbHelper.deleteAll("MT_TABLE1");

        tMtTable1 = new TableHelper(dbHelper, "MT_TABLE1");
        tMtTable1.setColumns("TABLE1_ID", "GLOBAL_ATTRIBUTE1", "SERVER_ATTRIBUTE1");
    }

    public void testDetachCommitted() {

        EntityResolver serverResover = serverContext.getEntityResolver();
        EntityResolver clientResolver = serverResover.getClientEntityResolver();
        ObjectDetachOperation op = new ObjectDetachOperation(clientResolver);

        ObjectId oid = new ObjectId("MtTable1", MtTable1.TABLE1_ID_PK_COLUMN, 456);
        MtTable1 so = new MtTable1();
        so.setObjectId(oid);
        so.setGlobalAttribute1("gx");
        so.setPersistenceState(PersistenceState.COMMITTED);
        so.setObjectContext(serverContext);
        serverContext.getGraphManager().registerNode(oid, so);

        Object detached = op.detach(
                so,
                serverResover.getClassDescriptor("MtTable1"),
                null);
        assertNotNull(detached);
        assertNotSame(so, detached);
        assertTrue(detached instanceof ClientMtTable1);

        ClientMtTable1 co = (ClientMtTable1) detached;
        assertEquals(oid, co.getObjectId());
        assertEquals("gx", co.getGlobalAttribute1());
        assertEquals(PersistenceState.TRANSIENT, co.getPersistenceState());
        assertNull(co.getObjectContext());
    }

    public void testDetachHollow() throws Exception {

        tMtTable1.insert(4, "g1", "s1");

        EntityResolver serverResover = serverContext.getEntityResolver();
        EntityResolver clientResolver = serverResover.getClientEntityResolver();
        ObjectDetachOperation op = new ObjectDetachOperation(clientResolver);

        ObjectId oid = new ObjectId("MtTable1", MtTable1.TABLE1_ID_PK_COLUMN, 4);
        MtTable1 so = new MtTable1();
        so.setObjectId(oid);
        so.setPersistenceState(PersistenceState.HOLLOW);
        so.setObjectContext(serverContext);
        serverContext.getGraphManager().registerNode(oid, so);

        Object detached = op.detach(
                so,
                serverResover.getClassDescriptor("MtTable1"),
                null);
        assertNotNull(detached);
        assertNotSame(so, detached);
        assertTrue(detached instanceof ClientMtTable1);

        ClientMtTable1 co = (ClientMtTable1) detached;
        assertEquals(oid, co.getObjectId());
        assertEquals("g1", co.getGlobalAttribute1());
        assertEquals(PersistenceState.TRANSIENT, co.getPersistenceState());
        assertNull(co.getObjectContext());
    }
}
