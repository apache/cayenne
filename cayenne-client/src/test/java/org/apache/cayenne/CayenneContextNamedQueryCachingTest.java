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

import java.util.Collections;
import java.util.List;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.NamedQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class CayenneContextNamedQueryCachingTest extends ClientCase {

    @Inject
    private DBHelper dbHelper;

    @Inject
    private CayenneContext context;

    @Inject(ClientCase.ROP_CLIENT_KEY)
    private DataChannelInterceptor clientServerInterceptor;

    private TableHelper tMtTable1;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("MT_TABLE2");
        dbHelper.deleteAll("MT_TABLE1");

        tMtTable1 = new TableHelper(dbHelper, "MT_TABLE1");
        tMtTable1.setColumns("TABLE1_ID", "GLOBAL_ATTRIBUTE1", "SERVER_ATTRIBUTE1");
    }

    protected void createThreeMtTable1sDataSet() throws Exception {
        tMtTable1.insert(1, "g1", "s1");
        tMtTable1.insert(2, "g2", "s2");
        tMtTable1.insert(3, "g3", "s3");
    }

    public void testLocalCache() throws Exception {
        createThreeMtTable1sDataSet();

        final NamedQuery q1 = new NamedQuery("MtQueryWithLocalCache");

        final List<?> result1 = context.performQuery(q1);
        assertEquals(3, result1.size());

        clientServerInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                List<?> result2 = context.performQuery(q1);
                assertSame(result1, result2);
            }
        });

        // refresh
        q1.setForceNoCache(true);
        List<?> result3 = context.performQuery(q1);
        assertNotSame(result1, result3);
        assertEquals(3, result3.size());
    }

    public void testLocalCacheParameterized() throws Exception {
        createThreeMtTable1sDataSet();

        final NamedQuery q1 = new NamedQuery(
                "ParameterizedMtQueryWithLocalCache",
                Collections.singletonMap("g", "g1"));

        final NamedQuery q2 = new NamedQuery(
                "ParameterizedMtQueryWithLocalCache",
                Collections.singletonMap("g", "g2"));

        final List<?> result1 = context.performQuery(q1);
        assertEquals(1, result1.size());

        clientServerInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                List<?> result2 = context.performQuery(q1);
                assertSame(result1, result2);
            }
        });
        
        final List<?> result3 = context.performQuery(q2);
        assertNotSame(result1, result3);
        assertEquals(1, result3.size());

        clientServerInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                List<?> result4 = context.performQuery(q2);
                assertSame(result3, result4);

                List<?> result5 = context.performQuery(q1);
                assertSame(result1, result5);
            }
        });
    
    }
    
    public void testParameterizedMappedToEJBQLQueries() throws Exception {
        
        createThreeMtTable1sDataSet();
        NamedQuery query = new NamedQuery("ParameterizedEJBQLMtQuery", Collections.singletonMap("g", "g1"));
        
        List<?> r1 = context.performQuery(query);
        assertEquals(1, r1.size());
    }
}
