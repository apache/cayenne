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
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;

public class ObjectDetachOperationTest extends CayenneCase {

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testDetachCommitted() {

        DataContext context = createDataContext();
        EntityResolver serverResover = context.getEntityResolver();
        EntityResolver clientResolver = serverResover.getClientEntityResolver();
        ObjectDetachOperation op = new ObjectDetachOperation(clientResolver);

        ObjectId oid = new ObjectId("MtTable1", MtTable1.TABLE1_ID_PK_COLUMN, 456);
        MtTable1 so = new MtTable1();
        so.setObjectId(oid);
        so.setGlobalAttribute1("gx");
        so.setPersistenceState(PersistenceState.COMMITTED);
        so.setObjectContext(context);
        context.getGraphManager().registerNode(oid, so);

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

        createTestData("testDetachHollow");

        DataContext context = createDataContext();
        EntityResolver serverResover = context.getEntityResolver();
        EntityResolver clientResolver = serverResover.getClientEntityResolver();
        ObjectDetachOperation op = new ObjectDetachOperation(clientResolver);

        ObjectId oid = new ObjectId("MtTable1", MtTable1.TABLE1_ID_PK_COLUMN, 1);
        MtTable1 so = new MtTable1();
        so.setObjectId(oid);
        so.setPersistenceState(PersistenceState.HOLLOW);
        so.setObjectContext(context);
        context.getGraphManager().registerNode(oid, so);

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
