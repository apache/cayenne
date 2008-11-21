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

package org.apache.cayenne.access;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.MockDataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.graph.MockGraphDiff;
import org.apache.cayenne.graph.NodeCreateOperation;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.MockQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.remote.QueryMessage;
import org.apache.cayenne.remote.hessian.service.HessianUtil;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable1Subclass;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.testdo.mt.ClientMtTable3;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;
import org.apache.cayenne.util.EqualsBuilder;

/**
 */
public class ClientServerChannelTest extends CayenneCase {

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources.getResources().getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testGetEntityResolver() throws Exception {
        EntityResolver resolver = new ClientServerChannel(getDomain())
                .getEntityResolver();
        assertNotNull(resolver);
        assertNull(resolver.lookupObjEntity(ClientMtTable1.class));
        assertNotNull(resolver.getClientEntityResolver().lookupObjEntity(
                ClientMtTable1.class));
    }

    public void testSynchronizeCommit() throws Exception {

        deleteTestData();
        SelectQuery query = new SelectQuery(MtTable1.class);

        DataContext context = createDataContext();

        assertEquals(0, context.performQuery(query).size());

        // no changes...
        ClientServerChannel channel = new ClientServerChannel(context);
        channel.onSync(context, new MockGraphDiff(), DataChannel.FLUSH_CASCADE_SYNC);

        assertEquals(0, context.performQuery(query).size());

        // introduce changes
        channel.onSync(
                context,
                new NodeCreateOperation(new ObjectId("MtTable1")),
                DataChannel.FLUSH_CASCADE_SYNC);

        assertEquals(1, context.performQuery(query).size());
    }

    public void testPerformQueryObjectIDInjection() throws Exception {
        createTestData("testOnSelectQueryObjectIDInjection");

        DataContext context = createDataContext();

        Query query = new SelectQuery("MtTable1");
        QueryResponse response = new ClientServerChannel(context).onQuery(null, query);

        assertNotNull(response);

        List results = response.firstList();

        assertNotNull(results);
        assertEquals(1, results.size());

        Object result = results.get(0);
        assertTrue(result instanceof ClientMtTable1);
        ClientMtTable1 clientObject = (ClientMtTable1) result;
        assertNotNull(clientObject.getObjectId());

        assertEquals(
                new ObjectId("MtTable1", MtTable1.TABLE1_ID_PK_COLUMN, 55),
                clientObject.getObjectId());
    }

    public void testPerformQueryValuePropagation() throws Exception {

        byte[] bytes = new byte[] {
                1, 2, 3
        };

        String chars = "abc";

        Map parameters = new HashMap();
        parameters.put("bytes", bytes);
        parameters.put("chars", chars);

        createTestData("testOnSelectQueryValuePropagation", parameters);

        DataContext context = createDataContext();

        Query query = new SelectQuery("MtTable3");
        QueryResponse response = new ClientServerChannel(context).onQuery(null, query);

        assertNotNull(response);

        List results = response.firstList();

        assertNotNull(results);
        assertEquals(1, results.size());

        Object result = results.get(0);
        assertTrue("Result is of wrong type: " + result, result instanceof ClientMtTable3);
        ClientMtTable3 clientObject = (ClientMtTable3) result;

        assertEquals(chars, clientObject.getCharColumn());
        assertEquals(new Integer(4), clientObject.getIntColumn());
        assertTrue(new EqualsBuilder()
                .append(clientObject.getBinaryColumn(), bytes)
                .isEquals());
    }

    public void testPerformQueryPropagationInheritance() throws Exception {

        Map parameters = new HashMap();
        parameters.put("GLOBAL_ATTRIBUTE1", "sub1");
        parameters.put("SERVER_ATTRIBUTE1", "xyz");
        createTestData("testOnSelectQueryValuePropagationInheritance", parameters);

        DataContext context = createDataContext();

        // must use real SelectQuery instead of mockup as root overriding depends on the
        // fact that Query inherits from AbstractQuery.
        SelectQuery query = new SelectQuery(ClientMtTable1.class);

        // must pass through the serialization pipe before running query as
        // HessianSerializer has needed preprocessing hooks...
        Query preprocessedQuery = (Query) HessianUtil.cloneViaClientServerSerialization(
                query,
                context.getEntityResolver());

        QueryResponse response = new ClientServerChannel(context).onQuery(
                null,
                preprocessedQuery);

        assertNotNull(response);

        List results = response.firstList();

        assertNotNull(results);
        assertEquals(1, results.size());

        Object result = results.get(0);
        assertTrue(
                "Result is of wrong type: " + result,
                result instanceof ClientMtTable1Subclass);
        ClientMtTable1Subclass clientObject = (ClientMtTable1Subclass) result;

        assertEquals("sub1", clientObject.getGlobalAttribute1());
    }

    public void testOnQuery() {

        final boolean[] genericDone = new boolean[1];
        MockDataChannel parent = new MockDataChannel(new EntityResolver()) {

            @Override
            public QueryResponse onQuery(ObjectContext context, Query query) {
                genericDone[0] = true;
                return super.onQuery(context, query);
            }
        };
        DataContext context = new DataContext(parent, new ObjectStore(
                new MockDataRowStore()));

        QueryMessage message = new QueryMessage(new MockQuery());
        new ClientServerChannel(context).onQuery(null, message.getQuery());
        assertTrue(genericDone[0]);
    }

    public void testOnQueryPrefetchingToMany() throws Exception {
        createTestData("testPrefetching");

        DataContext context = createDataContext();
        ClientServerChannel channel = new ClientServerChannel(context);

        SelectQuery q = new SelectQuery(ClientMtTable1.class);
        q.addOrdering(ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY, true);
        q.addPrefetch(ClientMtTable1.TABLE2ARRAY_PROPERTY);

        // must pass through the serialization pipe before running query as
        // HessianSerializer has needed preprocessing hooks...
        Query preprocessedQuery = (Query) HessianUtil.cloneViaClientServerSerialization(
                q,
                context.getEntityResolver());

        List results = channel.onQuery(null, preprocessedQuery).firstList();

        blockQueries();
        try {

            ClientMtTable1 o1 = (ClientMtTable1) results.get(0);
            assertNull(o1.getObjectContext());

            List children1 = o1.getTable2Array();

            assertEquals(2, children1.size());
            Iterator it = children1.iterator();
            while (it.hasNext()) {
                ClientMtTable2 o = (ClientMtTable2) it.next();
                assertNull(o.getObjectContext());
            }
        }
        finally {
            unblockQueries();
        }
    }

    public void testOnQueryPrefetchingToManyEmpty() throws Exception {
        createTestData("testPrefetching");

        DataContext context = createDataContext();
        ClientServerChannel channel = new ClientServerChannel(context);

        SelectQuery q = new SelectQuery(ClientMtTable1.class);
        q.addOrdering(ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY, true);
        q.addPrefetch(ClientMtTable1.TABLE2ARRAY_PROPERTY);

        // must pass through the serialization pipe before running query as
        // HessianSerializer has needed preprocessing hooks...
        Query preprocessedQuery = (Query) HessianUtil.cloneViaClientServerSerialization(
                q,
                context.getEntityResolver());

        List results = channel.onQuery(null, preprocessedQuery).firstList();

        blockQueries();
        try {

            ClientMtTable1 o2 = (ClientMtTable1) results.get(1);
            assertNull(o2.getObjectContext());

            List children2 = o2.getTable2Array();
            assertNotNull(children2);
            assertFalse(((ValueHolder) children2).isFault());
            assertEquals(0, children2.size());
        }
        finally {
            unblockQueries();
        }
    }
}
