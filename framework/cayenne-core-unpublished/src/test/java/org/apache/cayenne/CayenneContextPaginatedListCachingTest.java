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
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class CayenneContextPaginatedListCachingTest extends ClientCase {

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

    protected void createSevenMtTable1sDataSet() throws Exception {
        for (int i = 1; i <= 7; i++) {
            tMtTable1.insert(i, "g" + i, "s" + i);
        }
    }

    public void testLocalCache() throws Exception {
        createSevenMtTable1sDataSet();

        SelectQuery query = new SelectQuery(ClientMtTable1.class);
        query.addOrdering(ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY, SortOrder.ASCENDING);
        query.setPageSize(3);
        query.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

        List<ClientMtTable1> result1 = context.performQuery(query);
        assertEquals(7, result1.size());

        // ensure we can resolve all objects without a failure...
        for (ClientMtTable1 x : result1) {
            x.getGlobalAttribute1();
        }
    }
}
