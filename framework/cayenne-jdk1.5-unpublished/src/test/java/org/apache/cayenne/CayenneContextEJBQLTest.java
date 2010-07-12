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
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class CayenneContextEJBQLTest extends ClientCase {

    @Inject
    private DBHelper dbHelper;

    @Inject
    private CayenneContext context;

    private TableHelper tMtTable1;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("MT_TABLE2");
        dbHelper.deleteAll("MT_TABLE1");

        tMtTable1 = new TableHelper(dbHelper, "MT_TABLE1");
        tMtTable1.setColumns("TABLE1_ID", "GLOBAL_ATTRIBUTE1", "SERVER_ATTRIBUTE1");
    }

    private void createTwoRecords() throws Exception {
        tMtTable1.insert(1, "g1", "s1");
        tMtTable1.insert(2, "g2", "s2");
    }

    public void testEJBQLSelect() throws Exception {
        createTwoRecords();

        EJBQLQuery query = new EJBQLQuery("SELECT a FROM MtTable1 a");
        List<ClientMtTable1> results = context.performQuery(query);

        assertEquals(2, results.size());
    }

    public void testEJBQLSelectScalar() throws Exception {
        createTwoRecords();

        EJBQLQuery query = new EJBQLQuery("SELECT COUNT(a) FROM MtTable1 a");

        List<Long> results = context.performQuery(query);
        assertEquals(Long.valueOf(2), results.get(0));
    }

    public void testEJBQLSelectMixed() throws Exception {
        createTwoRecords();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT COUNT(a), a, a.serverAttribute1 FROM MtTable1 a Group By a ORDER BY a.serverAttribute1");

        List<Object[]> results = context.performQuery(query);

        assertEquals(2, results.size());
        assertEquals(Long.valueOf(1), results.get(0)[0]);
        assertTrue(results.get(0)[1] instanceof ClientMtTable1);
        assertEquals("s1", results.get(0)[2]);
    }
}
