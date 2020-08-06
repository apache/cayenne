/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.query;

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.remote.RemoteIncrementalFaultList;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@UseServerRuntime(CayenneProjects.MULTI_TIER_PROJECT)
public class ClientObjectSelectIT extends ClientCase {

    @Inject
    private CayenneContext context;

    @Inject
    DataChannelInterceptor serverCaseDataChannelInterceptor;

    @Inject
    private DBHelper dbHelper;

    private TableHelper mtTable;

    @Before
    public void setUp() throws Exception {
        mtTable = new TableHelper(dbHelper, "MT_TABLE1");
        mtTable.setColumns("TABLE1_ID", "GLOBAL_ATTRIBUTE1", "SERVER_ATTRIBUTE1");

        for (int i = 1; i <= 20; i++) {
            mtTable.insert(i, "globalAttr" + i, "serverAttr" + i);
        }
    }

    @Test
    public void testSelect() throws Exception{
        List<ClientMtTable1> list = ObjectSelect.query(ClientMtTable1.class)
                .select(context);

        assertNotNull(list);
        assertEquals(20, list.size());
    }

    @Test
    public void testCacheSelect() throws Exception{
        final ObjectSelect<ClientMtTable1> objectSelect = ObjectSelect.query(ClientMtTable1.class).
                cacheStrategy(QueryCacheStrategy.SHARED_CACHE);

        final List<ClientMtTable1> list1 = objectSelect.select(context);
        assertNotNull(list1);
        assertFalse(list1.isEmpty());

        serverCaseDataChannelInterceptor.runWithQueriesBlocked(new UnitTestClosure() {
            @Override
            public void execute() {
                List<ClientMtTable1> list2 = objectSelect.select(context);
                assertNotNull(list2);
                assertFalse(list2.isEmpty());
                assertEquals(list1, list2);
            }
        });
    }

    @Test
    public void testLimitSelect() throws Exception{
        List<ClientMtTable1> list = ObjectSelect.query(ClientMtTable1.class)
                .offset(5)
                .limit(10)
                .select(context);

        assertNotNull(list);
        assertEquals(10, list.size());
    }

    @Test
    public void testCacheLimitSelect() throws Exception {
        final ObjectSelect<ClientMtTable1> objectSelect = ObjectSelect.query(ClientMtTable1.class)
                .cacheStrategy(QueryCacheStrategy.SHARED_CACHE)
                .offset(5)
                .limit(10);

        final List<ClientMtTable1> list1 = objectSelect.select(context);
        assertEquals(10, list1.size());

        serverCaseDataChannelInterceptor.runWithQueriesBlocked(new UnitTestClosure() {
            @Override
            public void execute() {
                List<ClientMtTable1> list2 = objectSelect.select(context);
                assertNotNull(list2);
                assertEquals(10, list2.size());
                assertEquals(list1, list2);
            }
        });
    }

    @Test
    public void testPageSelect() throws Exception{
        final ObjectSelect<ClientMtTable1> objectSelect = ObjectSelect.query(ClientMtTable1.class)
                .pageSize(5);

        final List<ClientMtTable1> list = objectSelect.select(context);
        assertNotNull(list);
        assertEquals(RemoteIncrementalFaultList.class, list.getClass());

        int count = serverCaseDataChannelInterceptor.runWithQueryCounter(new UnitTestClosure() {
            @Override
            public void execute() {
                assertNotNull(list.get(0));
                assertNotNull(list.get(4));
                assertNotNull(list.get(5));
                assertNotNull(list.get(6));
            }
        });

        assertEquals(1, count);
    }

    @Test
    public void testCAY_2094() {
        ClientMtTable1 clientMtTable1 = SelectById.query(ClientMtTable1.class, 1).selectOne(context);
        assertNotNull(clientMtTable1);
        assertEquals(1, clientMtTable1.getObjectId().getIdSnapshot().get("TABLE1_ID"));
    }

}
