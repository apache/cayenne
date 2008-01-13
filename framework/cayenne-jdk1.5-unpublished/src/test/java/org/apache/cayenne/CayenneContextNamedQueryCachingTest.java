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

import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.query.NamedQuery;
import org.apache.cayenne.remote.ClientChannel;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;
import org.apache.cayenne.unit.UnitLocalConnection;

public class CayenneContextNamedQueryCachingTest extends CayenneCase {

    protected UnitLocalConnection connection;
    protected CayenneContext context;

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
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

        NamedQuery q1 = new NamedQuery("MtQueryWithLocalCache");

        List result1 = context.performQuery(q1);
        assertEquals(3, result1.size());

        connection.setBlockingMessages(true);
        try {
            List result2 = context.performQuery(q1);
            assertSame(result1, result2);
        }
        finally {
            connection.setBlockingMessages(false);
        }

        // refresh
        q1.setForceNoCache(true);
        List result3 = context.performQuery(q1);
        assertNotSame(result1, result3);
        assertEquals(3, result3.size());
    }

    public void testLocalCacheParameterized() throws Exception {
        deleteTestData();
        createTestData("prepare");

        NamedQuery q1 = new NamedQuery("ParameterizedMtQueryWithLocalCache", Collections
                .singletonMap("g", "g1"));

        NamedQuery q2 = new NamedQuery("ParameterizedMtQueryWithLocalCache", Collections
                .singletonMap("g", "g2"));

        List result1 = context.performQuery(q1);
        assertEquals(1, result1.size());

        connection.setBlockingMessages(true);
        try {
            List result2 = context.performQuery(q1);
            assertSame(result1, result2);
        }
        finally {
            connection.setBlockingMessages(false);
        }

        List result3 = context.performQuery(q2);
        assertNotSame(result1, result3);
        assertEquals(1, result3.size());
        
        connection.setBlockingMessages(true);
        try {
            List result4 = context.performQuery(q2);
            assertSame(result3, result4);
            
            List result5 = context.performQuery(q1);
            assertSame(result1, result5);
        }
        finally {
            connection.setBlockingMessages(false);
        }
    }
}
