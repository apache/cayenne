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

import java.util.List;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.testdo.mt.MtTable2;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.util.PersistentObjectHolder;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class PersistentObjectInContextTest extends ClientCase {

    @Inject
    private CayenneContext context;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tMtTable1;
    private TableHelper tMtTable2;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("MT_TABLE2");
        dbHelper.deleteAll("MT_TABLE1");

        tMtTable1 = new TableHelper(dbHelper, "MT_TABLE1");
        tMtTable1.setColumns("TABLE1_ID", "GLOBAL_ATTRIBUTE1", "SERVER_ATTRIBUTE1");

        tMtTable2 = new TableHelper(dbHelper, "MT_TABLE2");
        tMtTable2.setColumns("TABLE2_ID", "TABLE1_ID", "GLOBAL_ATTRIBUTE");
    }

    private void createTwoMtTable1sAnd2sDataSet() throws Exception {
        tMtTable1.insert(1, "g1", "s1");
        tMtTable1.insert(2, "g2", "s2");

        tMtTable2.insert(1, 1, "g1");
        tMtTable2.insert(2, 1, "g2");
    }

    public void testResolveToManyReverseResolved() throws Exception {
        createTwoMtTable1sAnd2sDataSet();

        ObjectId gid = new ObjectId(
                "MtTable1",
                MtTable1.TABLE1_ID_PK_COLUMN,
                new Integer(1));
        ClientMtTable1 t1 = (ClientMtTable1) Cayenne.objectForQuery(
                context,
                new ObjectIdQuery(gid));

        assertNotNull(t1);

        List<ClientMtTable2> t2s = t1.getTable2Array();
        assertEquals(2, t2s.size());

        for (ClientMtTable2 t2 : t2s) {

            PersistentObjectHolder holder = (PersistentObjectHolder) t2.getTable1Direct();
            assertFalse(holder.isFault());
            assertSame(t1, holder.getValue());
        }
    }

    public void testToOneRelationship() throws Exception {
        createTwoMtTable1sAnd2sDataSet();

        ObjectId gid = new ObjectId(
                "MtTable2",
                MtTable2.TABLE2_ID_PK_COLUMN,
                new Integer(1));
        ClientMtTable2 mtTable21 = (ClientMtTable2) Cayenne.objectForQuery(
                context,
                new ObjectIdQuery(gid));

        assertNotNull(mtTable21);

        ClientMtTable1 mtTable1 = mtTable21.getTable1();
        assertNotNull("To one relationship incorrectly resolved to null", mtTable1);
        assertEquals("g1", mtTable1.getGlobalAttribute1());
    }

    public void testResolveToOneReverseResolved() throws Exception {
        createTwoMtTable1sAnd2sDataSet();

        ObjectId gid = new ObjectId(
                "MtTable2",
                MtTable2.TABLE2_ID_PK_COLUMN,
                new Integer(1));
        ClientMtTable2 mtTable21 = (ClientMtTable2) Cayenne.objectForQuery(
                context,
                new ObjectIdQuery(gid));

        assertNotNull(mtTable21);

        ClientMtTable1 mtTable1 = mtTable21.getTable1();
        assertNotNull("To one relationship incorrectly resolved to null", mtTable1);

        List<ClientMtTable2> list = mtTable1.getTable2Array();
        assertNotNull(list);
        assertTrue(list instanceof ValueHolder);

        assertTrue(((ValueHolder) list).isFault());

        // resolve it here...
        assertEquals(2, list.size());
        for (ClientMtTable2 t2 : list) {
            PersistentObjectHolder holder = (PersistentObjectHolder) t2.getTable1Direct();
            assertFalse(holder.isFault());
            assertSame(mtTable1, holder.getValue());
        }

        assertEquals("g1", mtTable1.getGlobalAttribute1());
    }
}
