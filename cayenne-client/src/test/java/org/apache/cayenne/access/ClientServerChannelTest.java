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

import java.util.List;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.MockDataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.graph.MockGraphDiff;
import org.apache.cayenne.graph.NodeCreateOperation;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.MockQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.remote.QueryMessage;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable1Subclass1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.testdo.mt.ClientMtTable3;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.util.EqualsBuilder;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class ClientServerChannelTest extends ClientCase {

    @Inject
    protected DataContext serverContext;

    @Inject
    protected ClientServerChannel clientServerChannel;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected DataChannelInterceptor queryInterceptor;

    @Inject
    protected JdbcEventLogger logger;

    @Inject
    private ServerRuntime runtime;

    private TableHelper tMtTable1;
    private TableHelper tMtTable2;
    private TableHelper tMtTable3;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("MT_TABLE2");
        dbHelper.deleteAll("MT_TABLE1");
        dbHelper.deleteAll("MT_TABLE3");

        tMtTable1 = new TableHelper(dbHelper, "MT_TABLE1");
        tMtTable1.setColumns("TABLE1_ID", "GLOBAL_ATTRIBUTE1", "SERVER_ATTRIBUTE1");

        tMtTable2 = new TableHelper(dbHelper, "MT_TABLE2");
        tMtTable2.setColumns("TABLE2_ID", "TABLE1_ID", "GLOBAL_ATTRIBUTE");

        tMtTable3 = new TableHelper(dbHelper, "MT_TABLE3");
        tMtTable3.setColumns("TABLE3_ID", "BINARY_COLUMN", "CHAR_COLUMN", "INT_COLUMN");
    }

    protected void createTwoMtTable1sAnd2sDataSet() throws Exception {

        tMtTable1.insert(1, "g1", "s1");
        tMtTable1.insert(2, "g2", "s2");

        tMtTable2.insert(1, 1, "g1");
        tMtTable2.insert(2, 1, "g2");
    }

    public void testGetEntityResolver() throws Exception {
        EntityResolver resolver = clientServerChannel.getEntityResolver();
        assertNotNull(resolver);
        assertNull(resolver.getObjEntity(ClientMtTable1.class));
        assertNotNull(resolver.getClientEntityResolver().getObjEntity(ClientMtTable1.class));
    }

    public void testSynchronizeCommit() throws Exception {

        SelectQuery query = new SelectQuery(MtTable1.class);

        // no changes...
        clientServerChannel.onSync(serverContext, new MockGraphDiff(), DataChannel.FLUSH_CASCADE_SYNC);

        assertEquals(0, serverContext.performQuery(query).size());

        // introduce changes
        clientServerChannel.onSync(serverContext, new NodeCreateOperation(new ObjectId("MtTable1")),
                DataChannel.FLUSH_CASCADE_SYNC);

        assertEquals(1, serverContext.performQuery(query).size());
    }

    public void testPerformQueryObjectIDInjection() throws Exception {
        tMtTable1.insert(55, "g1", "s1");

        Query query = new SelectQuery("MtTable1");
        QueryResponse response = clientServerChannel.onQuery(null, query);

        assertNotNull(response);

        List<?> results = response.firstList();

        assertNotNull(results);
        assertEquals(1, results.size());

        Object result = results.get(0);
        assertTrue(result instanceof ClientMtTable1);
        ClientMtTable1 clientObject = (ClientMtTable1) result;
        assertNotNull(clientObject.getObjectId());

        assertEquals(new ObjectId("MtTable1", MtTable1.TABLE1_ID_PK_COLUMN, 55), clientObject.getObjectId());
    }

    public void testPerformQueryValuePropagation() throws Exception {

        byte[] bytes = new byte[] { 1, 2, 3 };

        tMtTable3.insert(1, bytes, "abc", 4);

        Query query = new SelectQuery("MtTable3");
        QueryResponse response = clientServerChannel.onQuery(null, query);

        assertNotNull(response);

        List<?> results = response.firstList();

        assertNotNull(results);
        assertEquals(1, results.size());

        Object result = results.get(0);
        assertTrue("Result is of wrong type: " + result, result instanceof ClientMtTable3);
        ClientMtTable3 clientObject = (ClientMtTable3) result;

        assertEquals("abc", clientObject.getCharColumn());
        assertEquals(new Integer(4), clientObject.getIntColumn());
        assertTrue(new EqualsBuilder().append(clientObject.getBinaryColumn(), bytes).isEquals());
    }

    public void testPerformQueryPropagationInheritance() throws Exception {

        tMtTable1.insert(65, "sub1", "xyz");

        SelectQuery query = new SelectQuery(ClientMtTable1.class);
        QueryResponse response = clientServerChannel.onQuery(null, query);

        assertNotNull(response);

        List<?> results = response.firstList();

        assertNotNull(results);
        assertEquals(1, results.size());

        Object result = results.get(0);
        assertTrue("Result is of wrong type: " + result, result instanceof ClientMtTable1Subclass1);
        ClientMtTable1Subclass1 clientObject = (ClientMtTable1Subclass1) result;

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
        DataContext context = (DataContext) runtime.newContext(parent);

        QueryMessage message = new QueryMessage(new MockQuery());
        new ClientServerChannel(context).onQuery(null, message.getQuery());
        assertTrue(genericDone[0]);
    }

    public void testOnQueryPrefetchingToMany() throws Exception {
        createTwoMtTable1sAnd2sDataSet();

        SelectQuery query = new SelectQuery(ClientMtTable1.class);
        query.addOrdering(ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY, SortOrder.ASCENDING);
        query.addPrefetch(ClientMtTable1.TABLE2ARRAY_PROPERTY);

        final List<?> results = clientServerChannel.onQuery(null, query).firstList();

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                ClientMtTable1 o1 = (ClientMtTable1) results.get(0);
                assertNull(o1.getObjectContext());

                List<ClientMtTable2> children1 = o1.getTable2Array();

                assertEquals(2, children1.size());
                for (ClientMtTable2 o : children1) {
                    assertNull(o.getObjectContext());
                }
            }
        });
    }

    public void testOnQueryPrefetchingToManyEmpty() throws Exception {
        createTwoMtTable1sAnd2sDataSet();

        SelectQuery q = new SelectQuery(ClientMtTable1.class);
        q.addOrdering(ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY, SortOrder.ASCENDING);
        q.addPrefetch(ClientMtTable1.TABLE2ARRAY_PROPERTY);

        final List<?> results = clientServerChannel.onQuery(null, q).firstList();

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {

                ClientMtTable1 o2 = (ClientMtTable1) results.get(1);
                assertNull(o2.getObjectContext());

                List<?> children2 = o2.getTable2Array();
                assertNotNull(children2);
                assertFalse(((ValueHolder) children2).isFault());
                assertEquals(0, children2.size());
            }
        });
    }
}
