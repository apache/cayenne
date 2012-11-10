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

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.testdo.mt.MtTable2;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class CayenneContextRelationshipsTest extends ClientCase {

    @Inject
    private DBHelper dbHelper;

    @Inject
    private CayenneContext context;

    @Inject
    private DataContext serverContext;

    private TableHelper tMtTable1;
    private TableHelper tMtTable2;

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
    }

    public void testLostUncommittedToOneModifications_Client() throws Exception {

        tMtTable1.insert(1, "G1", "S1");
        tMtTable1.insert(2, "G2", "S2");
        tMtTable2.insert(33, 1, "GX");

        ClientMtTable2 o = Cayenne.objectForPK(context, ClientMtTable2.class, 33);

        ClientMtTable1 r2 = Cayenne.objectForPK(context, ClientMtTable1.class, 2);
        ClientMtTable1 r1 = o.getTable1();

        o.setTable1(r2);

        assertSame(r2, o.getTable1());

        // see CAY-1757 - this used to reset our changes
        assertFalse(r1.getTable2Array().contains(o));
        assertSame(r2, o.getTable1());
    }

    public void testLostUncommittedToOneModifications_Server() throws Exception {

        tMtTable1.insert(1, "G1", "S1");
        tMtTable1.insert(2, "G2", "S2");
        tMtTable2.insert(33, 1, "GX");

        MtTable2 o = Cayenne.objectForPK(serverContext, MtTable2.class, 33);

        MtTable1 r2 = Cayenne.objectForPK(serverContext, MtTable1.class, 2);
        MtTable1 r1 = o.getTable1();

        o.setTable1(r2);

        assertSame(r2, o.getTable1());

        assertFalse(r1.getTable2Array().contains(o));
        assertSame(r2, o.getTable1());
    }
}
