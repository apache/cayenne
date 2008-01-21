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

import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.remote.ClientChannel;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.testdo.mt.MtTable2;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;
import org.apache.cayenne.util.PersistentObjectHolder;

public class PersistentObjectInContextTest extends CayenneCase {

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    protected ObjectContext createObjectContext() {
        // wrap ClientServerChannel in LocalConnection to enable logging...
        ClientConnection connector = new LocalConnection(new ClientServerChannel(
                getDomain()), LocalConnection.HESSIAN_SERIALIZATION);
        return new CayenneContext(new ClientChannel(connector));
    }

    public void testResolveToManyReverseResolved() throws Exception {
        createTestData("prepare");

        ObjectContext context = createObjectContext();

        ObjectId gid = new ObjectId(
                "MtTable1",
                MtTable1.TABLE1_ID_PK_COLUMN,
                new Integer(1));
        ClientMtTable1 t1 = (ClientMtTable1) DataObjectUtils.objectForQuery(
                context,
                new ObjectIdQuery(gid));

        assertNotNull(t1);

        List t2s = t1.getTable2Array();
        assertEquals(2, t2s.size());
        Iterator it = t2s.iterator();
        while (it.hasNext()) {
            ClientMtTable2 t2 = (ClientMtTable2) it.next();

            PersistentObjectHolder holder = (PersistentObjectHolder) t2.getTable1Direct();
            assertFalse(holder.isFault());
            assertSame(t1, holder.getValue());
        }
    }

    public void testToOneRelationship() throws Exception {
        createTestData("prepare");

        ObjectContext context = createObjectContext();

        ObjectId gid = new ObjectId(
                "MtTable2",
                MtTable2.TABLE2_ID_PK_COLUMN,
                new Integer(1));
        ClientMtTable2 mtTable21 = (ClientMtTable2) DataObjectUtils.objectForQuery(
                context,
                new ObjectIdQuery(gid));

        assertNotNull(mtTable21);

        ClientMtTable1 mtTable1 = mtTable21.getTable1();
        assertNotNull("To one relationship incorrectly resolved to null", mtTable1);
        assertEquals("g1", mtTable1.getGlobalAttribute1());
    }

    public void testResolveToOneReverseResolved() throws Exception {
        createTestData("prepare");

        ObjectContext context = createObjectContext();

        ObjectId gid = new ObjectId(
                "MtTable2",
                MtTable2.TABLE2_ID_PK_COLUMN,
                new Integer(1));
        ClientMtTable2 mtTable21 = (ClientMtTable2) DataObjectUtils.objectForQuery(
                context,
                new ObjectIdQuery(gid));

        assertNotNull(mtTable21);

        ClientMtTable1 mtTable1 = mtTable21.getTable1();
        assertNotNull("To one relationship incorrectly resolved to null", mtTable1);

        List list = mtTable1.getTable2Array();
        assertNotNull(list);
        assertTrue(list instanceof ValueHolder);

        assertTrue(((ValueHolder) list).isFault());

        // resolve it here...
        assertEquals(2, list.size());
        Iterator it = list.iterator();
        while (it.hasNext()) {
            ClientMtTable2 t2 = (ClientMtTable2) it.next();

            PersistentObjectHolder holder = (PersistentObjectHolder) t2.getTable1Direct();
            assertFalse(holder.isFault());
            assertSame(mtTable1, holder.getValue());
        }

        assertEquals("g1", mtTable1.getGlobalAttribute1());
    }
}
