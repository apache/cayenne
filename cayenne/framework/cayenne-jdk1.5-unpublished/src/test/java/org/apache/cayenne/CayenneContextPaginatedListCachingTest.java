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

import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.remote.ClientChannel;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;
import org.apache.cayenne.unit.UnitLocalConnection;

public class CayenneContextPaginatedListCachingTest extends CayenneCase {

    protected UnitLocalConnection connection;
    protected CayenneContext context;

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources.getResources().getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        ClientServerChannel serverChannel = new ClientServerChannel(getDomain());
        connection = new UnitLocalConnection(
                serverChannel,
                LocalConnection.HESSIAN_SERIALIZATION);
        ClientChannel clientChannel = new ClientChannel(connection);
        context = new CayenneContext(clientChannel);
    }

    public void testLocalCache() throws Exception {
        deleteTestData();
        createTestData("prepare");

        SelectQuery query = new SelectQuery(ClientMtTable1.class);
        query.addOrdering(ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY, Ordering.ASC);
        query.setPageSize(3);
        query.setCachePolicy(QueryMetadata.LOCAL_CACHE);

        List<?> result1 = context.performQuery(query);
        assertEquals(7, result1.size());

        // ensure we can resolve all objects without a failure...
        for(Object x : result1) {
            ((ClientMtTable1) x).getGlobalAttribute1();
        }
    }
}
