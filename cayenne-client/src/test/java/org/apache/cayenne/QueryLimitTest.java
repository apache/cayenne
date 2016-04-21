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
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.*;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @since 4.0
 */

@UseServerRuntime(CayenneProjects.MULTI_TIER_PROJECT)
public class QueryLimitTest extends ClientCase {

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
        mtTable.setColumns("TABLE1_ID", "GLOBAL_ATTRIBUTE1", "SERVER_ATTRIBUTE1", "SUBCLASS_ATTRIBUTE1");
    }

    protected void createDataSet() throws Exception{

        for (int i = 1; i <= 10; i++){
            mtTable.insert(i, "sub2", "sub2_" + i, "sub2attr");
        }

        for (int i = 11; i <= 20; i++){
            mtTable.insert(i, "sub1", "sub1_" + i, "sub1attr");
        }

    }

    //setting limit and cache strategy
    @Test
    public void testLimitWhenCacheSetted() throws Exception {
        createDataSet();

        final ObjectSelect objectSelect = ObjectSelect.query(MtTable1.class).
                limit(2).
                cacheStrategy(QueryCacheStrategy.SHARED_CACHE);

        final List<MtTable1> artistList1 = objectSelect.select(context);
        final List<MtTable1> artistList2 = new ArrayList<>();

        serverCaseDataChannelInterceptor.runWithQueriesBlocked(new UnitTestClosure() {
            public void execute() {

                artistList2.addAll(objectSelect.select(context));
                assertEquals(artistList1, artistList2);
            }
        });
    }
}
