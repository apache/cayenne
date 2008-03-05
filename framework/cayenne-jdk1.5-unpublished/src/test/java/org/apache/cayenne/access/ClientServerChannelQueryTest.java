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

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.NamedQuery;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.remote.ClientChannel;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;
import org.apache.cayenne.util.PersistentObjectHolder;
import org.apache.cayenne.util.PersistentObjectList;

public class ClientServerChannelQueryTest extends CayenneCase {

    private ObjectContext context;
    private ClientServerChannel serverChannel;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        serverChannel = new ClientServerChannel(getDomain());
        LocalConnection connector = new LocalConnection(
                serverChannel,
                LocalConnection.HESSIAN_SERIALIZATION);

        context = new CayenneContext(new ClientChannel(connector));
    }

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources.getResources().getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testPaginatedQueryServerCacheOverflow() throws Exception {
        createTestData("testPaginatedQueryServerCacheOverflow");

        SelectQuery query = new SelectQuery(ClientMtTable1.class);
        query.addOrdering(ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY, Ordering.ASC);
        query.setPageSize(3);

        List<?> results = context.performQuery(query);

        // read page 1
        assertTrue(results.get(0) instanceof ClientMtTable1);

        // now kick out the server-side list from local cache, and see if the query would
        // recover...
        QueryCache qc = serverChannel.getQueryCache();
        assertEquals(1, qc.size());
        qc.clear();
        assertEquals(0, qc.size());

        assertTrue(results.get(3) instanceof ClientMtTable1);
    }

    public void testNamedQuery() throws Exception {
        createTestData("prepare");

        NamedQuery q = new NamedQuery("AllMtTable1");
        List<?> results = context.performQuery(q);

        assertEquals(2, results.size());
        assertTrue(results.get(0) instanceof ClientMtTable1);
    }

    public void testSelectQueryEntityNameRoot() throws Exception {
        createTestData("prepare");

        SelectQuery q = new SelectQuery("MtTable1");
        List<?> results = context.performQuery(q);

        assertEquals(2, results.size());

        assertTrue(results.get(0) instanceof ClientMtTable1);
    }

    public void testSelectQueryClientClassRoot() throws Exception {
        createTestData("prepare");

        SelectQuery q = new SelectQuery(ClientMtTable1.class);
        List<?> results = context.performQuery(q);

        assertEquals(2, results.size());
        assertTrue(results.get(0) instanceof ClientMtTable1);
    }

    public void testSelectQuerySimpleQualifier() throws Exception {
        createTestData("prepare");

        SelectQuery q = new SelectQuery(ClientMtTable1.class, Expression
                .fromString("globalAttribute1 = 'g1'"));
        List<?> results = context.performQuery(q);

        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof ClientMtTable1);
    }

    public void testSelectQueryToManyRelationshipQualifier() throws Exception {
        createTestData("prepare");

        SelectQuery q = new SelectQuery(ClientMtTable1.class, Expression
                .fromString("table2Array.globalAttribute = 'g1'"));
        List<?> results = context.performQuery(q);

        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof ClientMtTable1);
    }

    public void testSelectQueryOrdering() throws Exception {
        createTestData("prepare");

        SelectQuery q = new SelectQuery("MtTable1");
        q.addOrdering(ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY, true);
        List<?> results = context.performQuery(q);

        assertEquals(2, results.size());

        ClientMtTable1 o1 = (ClientMtTable1) results.get(0);
        ClientMtTable1 o2 = (ClientMtTable1) results.get(1);
        assertTrue(o1.getGlobalAttribute1().compareTo(o2.getGlobalAttribute1()) < 0);

        // now run the same query with reverse ordering to check that the first ordering
        // result wasn't coincidental.

        q.clearOrderings();
        q.addOrdering(ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY, false);
        List<?> results1 = context.performQuery(q);

        assertEquals(2, results1.size());

        ClientMtTable1 o3 = (ClientMtTable1) results1.get(0);
        ClientMtTable1 o4 = (ClientMtTable1) results1.get(1);
        assertTrue(o3.getGlobalAttribute1().compareTo(o4.getGlobalAttribute1()) > 0);
    }

    public void testSelectQueryPrefetchToOne() throws Exception {
        createTestData("prepare");

        SelectQuery q = new SelectQuery(ClientMtTable2.class, Expression
                .fromString("globalAttribute = 'g1'"));
        q.addPrefetch(ClientMtTable2.TABLE1_PROPERTY);
        List<?> results = context.performQuery(q);

        assertEquals(1, results.size());

        ClientMtTable2 result = (ClientMtTable2) results.get(0);

        ValueHolder holder = result.getTable1Direct();
        assertNotNull(holder);
        assertTrue(holder instanceof PersistentObjectHolder);
        PersistentObjectHolder objectHolder = (PersistentObjectHolder) holder;
        assertFalse(objectHolder.isFault());

        ClientMtTable1 target = (ClientMtTable1) objectHolder.getValue();
        assertNotNull(target);
    }

    public void testSelectQueryPrefetchToMany() throws Exception {
        createTestData("prepare");

        SelectQuery q = new SelectQuery(ClientMtTable1.class, Expression
                .fromString("globalAttribute1 = 'g1'"));
        q.addPrefetch(ClientMtTable1.TABLE2ARRAY_PROPERTY);
        List<?> results = context.performQuery(q);

        assertEquals(1, results.size());

        ClientMtTable1 result = (ClientMtTable1) results.get(0);

        List<?> holder = result.getTable2ArrayDirect();
        assertNotNull(holder);
        assertTrue(holder instanceof PersistentObjectList);
        PersistentObjectList objectHolder = (PersistentObjectList) holder;
        assertFalse(objectHolder.isFault());
        assertEquals(2, objectHolder.size());
    }
}
