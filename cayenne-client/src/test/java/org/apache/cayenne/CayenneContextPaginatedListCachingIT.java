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
package org.apache.cayenne;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

@UseServerRuntime(CayenneProjects.MULTI_TIER_PROJECT)
public class CayenneContextPaginatedListCachingIT extends ClientCase {

    @Inject
    private DBHelper dbHelper;

    @Inject
    private CayenneContext context;

    private TableHelper tMtTable1;

    @Before
    public void setUp() throws Exception {
        tMtTable1 = new TableHelper(dbHelper, "MT_TABLE1");
        tMtTable1.setColumns("TABLE1_ID", "GLOBAL_ATTRIBUTE1", "SERVER_ATTRIBUTE1");
    }

    private void createSevenMtTable1sDataSet() throws Exception {
        for (int i = 1; i <= 7; i++) {
            tMtTable1.insert(i, "g" + i, "s" + i);
        }
    }

    @Test
    public void testLocalCache() throws Exception {
        createSevenMtTable1sDataSet();

        List<ClientMtTable1> result1 = ObjectSelect.query(ClientMtTable1.class)
                .orderBy(ClientMtTable1.GLOBAL_ATTRIBUTE1.asc())
                .pageSize(3)
                .localCache()
                .select(context);
        assertEquals(7, result1.size());

        // ensure we can resolve all objects without a failure...
        for (ClientMtTable1 x : result1) {
            x.getGlobalAttribute1();
        }
    }
}
