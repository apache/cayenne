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

package org.apache.cayenne.access;

import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.MappedSelect;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.util.PersistentObjectHolder;
import org.apache.cayenne.util.PersistentObjectList;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@UseServerRuntime(CayenneProjects.MULTI_TIER_PROJECT)
public class ClientServerChannelQueryIT extends ClientCase {

    @Inject(ClientCase.ROP_CLIENT_KEY)
    protected ObjectContext context;

    @Inject
    private ClientServerChannel serverChannel;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tMtTable1;
    private TableHelper tMtTable2;

    @Before
    public void setUp() throws Exception {
        tMtTable1 = new TableHelper(dbHelper, "MT_TABLE1");
        tMtTable1.setColumns("TABLE1_ID", "GLOBAL_ATTRIBUTE1", "SERVER_ATTRIBUTE1");

        tMtTable2 = new TableHelper(dbHelper, "MT_TABLE2");
        tMtTable2.setColumns("TABLE2_ID", "TABLE1_ID", "GLOBAL_ATTRIBUTE");
    }

    protected void createSevenMtTable1sDataSet() throws Exception {

        for (int i = 1; i <= 7; i++) {
            tMtTable1.insert(i, "g" + i, "s" + i);
        }
    }

    protected void createTwoMtTable1sAnd2sDataSet() throws Exception {

        tMtTable1.insert(1, "g1", "s1");
        tMtTable1.insert(2, "g2", "s2");

        tMtTable2.insert(1, 1, "g1");
        tMtTable2.insert(2, 1, "g2");
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testPaginatedQueryServerCacheOverflow() throws Exception {
        createSevenMtTable1sDataSet();

        ObjectSelect<ClientMtTable1> query = ObjectSelect.query(ClientMtTable1.class)
                .orderBy(ClientMtTable1.GLOBAL_ATTRIBUTE1.asc())
                .pageSize(3);

        List<?> results = context.performQuery(query);

        // read page 1
        assertTrue(results.get(0) instanceof ClientMtTable1);

        assertTrue(results.get(3) instanceof ClientMtTable1);
    }

    @Test
    public void testParameterizedMappedToEJBQLQueries() throws Exception {
        createTwoMtTable1sAnd2sDataSet();

        List<?> r1 = context.performQuery(MappedSelect.query("ParameterizedEJBQLMtQuery").param("g", "g1"));
        assertEquals(1, r1.size());
        assertTrue(r1.get(0) instanceof ClientMtTable1);
    }

    @Test
    public void testNamedQuery() throws Exception {
        createTwoMtTable1sAnd2sDataSet();

        List<?> results = context.performQuery(MappedSelect.query("AllMtTable1"));

        assertEquals(2, results.size());
        assertTrue(results.get(0) instanceof ClientMtTable1);
    }

    @Test
    public void testSelectQueryEntityNameRoot() throws Exception {
        createTwoMtTable1sAnd2sDataSet();

        ObjectSelect q = ObjectSelect.query(MtTable1.class);
        List<?> results = context.performQuery(q);

        assertEquals(2, results.size());

        assertTrue(results.get(0) instanceof ClientMtTable1);
    }

    @Test
    public void testSelectQueryClientClassRoot() throws Exception {
        createTwoMtTable1sAnd2sDataSet();

        ObjectSelect<ClientMtTable1> q = ObjectSelect.query(ClientMtTable1.class);
        List<?> results = context.performQuery(q);

        assertEquals(2, results.size());
        assertTrue(results.get(0) instanceof ClientMtTable1);
    }

    @Test
    public void testSelectQuerySimpleQualifier() throws Exception {
        createTwoMtTable1sAnd2sDataSet();

        ObjectSelect<ClientMtTable1> q = ObjectSelect.query(ClientMtTable1.class)
                .where(ClientMtTable1.GLOBAL_ATTRIBUTE1.eq("g1"));
        List<?> results = context.performQuery(q);

        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof ClientMtTable1);
    }

    @Test
    public void testSelectQueryToManyRelationshipQualifier() throws Exception {
        createTwoMtTable1sAnd2sDataSet();

        ObjectSelect<ClientMtTable1> q = ObjectSelect.query(ClientMtTable1.class)
                .where(ClientMtTable1.TABLE2ARRAY.dot(ClientMtTable2.GLOBAL_ATTRIBUTE).eq("g1"));
        List<?> results = context.performQuery(q);

        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof ClientMtTable1);
    }

    @Test
    public void testSelectQueryOrdering() throws Exception {
        createTwoMtTable1sAnd2sDataSet();

        ObjectSelect q = ObjectSelect.query(MtTable1.class)
                .orderBy(ClientMtTable1.GLOBAL_ATTRIBUTE1.asc());
        List<?> results = context.performQuery(q);

        assertEquals(2, results.size());

        ClientMtTable1 o1 = (ClientMtTable1) results.get(0);
        ClientMtTable1 o2 = (ClientMtTable1) results.get(1);
        assertTrue(o1.getGlobalAttribute1().compareTo(o2.getGlobalAttribute1()) < 0);

        // now run the same query with reverse ordering to check that the first ordering
        // result wasn't coincidental.
        q.getOrderings().clear();
        q.orderBy(ClientMtTable1.GLOBAL_ATTRIBUTE1.desc());
        List<?> results1 = context.performQuery(q);

        assertEquals(2, results1.size());

        ClientMtTable1 o3 = (ClientMtTable1) results1.get(0);
        ClientMtTable1 o4 = (ClientMtTable1) results1.get(1);
        assertTrue(o3.getGlobalAttribute1().compareTo(o4.getGlobalAttribute1()) > 0);
    }

    @Test
    public void testSelectQueryPrefetchToOne() throws Exception {
        createTwoMtTable1sAnd2sDataSet();

        ObjectSelect<ClientMtTable2> q = ObjectSelect.query(ClientMtTable2.class)
                .where(ClientMtTable2.GLOBAL_ATTRIBUTE.eq("g1"))
                .prefetch(ClientMtTable2.TABLE1.disjoint());
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

    @Test
    public void testSelectQueryPrefetchToMany() throws Exception {
        createTwoMtTable1sAnd2sDataSet();

        ObjectSelect<ClientMtTable1> q = ObjectSelect.query(ClientMtTable1.class)
                .where(ClientMtTable1.GLOBAL_ATTRIBUTE1.eq("g1"))
                .prefetch(ClientMtTable1.TABLE2ARRAY.joint());
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
