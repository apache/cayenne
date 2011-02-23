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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class CayenneContextRefreshQueryTest extends ClientCase {

    @Inject
    private DBHelper dbHelper;

    @Inject
    private CayenneContext context;

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

    private void createM1AndTwoM2sDataSet() throws Exception {
        tMtTable1.insert(1, "g1", "s1");
        tMtTable2.insert(1, 1, "g1").insert(2, 1, "g1");
    }

    private void delete1M2DataSet() throws Exception {
        tMtTable2.delete().where("TABLE2_ID", 1).execute();
    }

    public void testRefreshToMany() throws Exception {

        createM1AndTwoM2sDataSet();

        ClientMtTable1 a = Cayenne.objectForPK(context, ClientMtTable1.class, 1);
        assertEquals(2, a.getTable2Array().size());

        delete1M2DataSet();

        RefreshQuery refresh = new RefreshQuery(a);
        context.performGenericQuery(refresh);
        assertEquals(PersistenceState.HOLLOW, a.getPersistenceState());
        assertEquals(1, a.getTable2Array().size());
    }
}
