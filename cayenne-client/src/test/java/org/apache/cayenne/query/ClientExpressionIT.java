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
package org.apache.cayenne.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.ASTEqual;
import org.apache.cayenne.exp.parser.ASTList;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

@UseServerRuntime(CayenneProjects.MULTI_TIER_PROJECT)
public class ClientExpressionIT extends ClientCase {

    @Inject
    private CayenneContext context;

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

    private void createDataSet() throws Exception {
        for(int i = 1; i <= 10; i++) {
            tMtTable1.insert(i ,"1_global" + i, "server" + i);
            tMtTable2.insert(i , i, "2_global" + i);
            tMtTable2.insert(i + 10, i, "2_global" + (i + 10));
        }
    }

    @Test
    public void testPersistentValueInExpression() {
        ClientMtTable1 t1 = context.newObject(ClientMtTable1.class);
        ClientMtTable1 t2 = context.newObject(ClientMtTable1.class);

        context.commitChanges();

        Expression scalar = ExpressionFactory.matchExp((String)null, t1);
        Expression list = ExpressionFactory.matchAllExp("|", Arrays.asList(t1, t2));

        assertEquals(t1.getObjectId(), scalar.getOperand(1));
        assertEquals(t1.getObjectId(), ((ASTEqual)list.getOperand(0)).getOperand(1));
        assertEquals(t2.getObjectId(), ((ASTEqual)list.getOperand(1)).getOperand(1));
    }

    @Test
    public void testListInASTList() {
        ClientMtTable1 t1 = context.newObject(ClientMtTable1.class);
        ClientMtTable1 t2 = context.newObject(ClientMtTable1.class);

        context.commitChanges();

        List<ClientMtTable1> table1List = new ArrayList<>();
        table1List.add(t1);
        table1List.add(t2);

        // send list in expression factory
        Expression list = ClientMtTable2.TABLE1.in(table1List);

        Object[] values = (Object[])((ASTList)list.getOperand(1)).getOperand(0);
        assertEquals(t1.getObjectId(), values[0]);
        assertEquals(t2.getObjectId(), values[1]);

        ObjectId t1Id = ObjectId.of("MtTable1", "TABLE1_ID", 1);
        ObjectId t2Id = ObjectId.of("MtTable1", "TABLE1_ID", 2);
        t1.setObjectId(t1Id);
        t2.setObjectId(t2Id);

        //Expression and client have different copies of object
        assertNotSame(t1.getObjectId(), values[0]);
        assertNotSame(t2.getObjectId(), values[1]);
    }

    @Test
    public void testArrayInASTList() {
        ClientMtTable1 t1 = context.newObject(ClientMtTable1.class);
        ClientMtTable1 t2 = context.newObject(ClientMtTable1.class);
        ClientMtTable1 t3 = context.newObject(ClientMtTable1.class);

        context.commitChanges();

        Object[] tArray = new Object[3];
        tArray[0] = t1;
        tArray[1] = t2;

        // send array in expression factory
        Expression list = ExpressionFactory.inExp(ClientMtTable2.TABLE1.getName(), tArray);
        tArray[2] = t3;

        Object[] values = (Object[])((ASTList)list.getOperand(1)).getOperand(0);
        assertEquals(tArray.length, values.length);
        assertNotSame(tArray[2], values[2]);
        assertEquals(t1.getObjectId(), values[0]);
        assertEquals(t2.getObjectId(), values[1]);

        ObjectId t1Id = ObjectId.of("MtTable1", "TABLE1_ID", 1);
        ObjectId t2Id = ObjectId.of("MtTable1", "TABLE1_ID", 2);
        t1.setObjectId(t1Id);
        t2.setObjectId(t2Id);

        // Expression and client have different arrays
        assertNotSame(t1.getObjectId(), values[0]);
        assertNotSame(t2.getObjectId(), values[1]);
    }

    @Test
    public void testExpressionFactoryMatch() throws Exception {
        createDataSet();

        ObjectSelect<ClientMtTable1> table1Query = ObjectSelect.query(ClientMtTable1.class)
                .orderBy(new Ordering("db:TABLE1_ID", SortOrder.ASCENDING));
        List<ClientMtTable1> table1List = table1Query.select(context);

        assertNotNull(table1List);

        ClientMtTable1 element_1 = table1List.get(0);
        ClientMtTable1 element_2 = table1List.get(1);

        Expression exp = ClientMtTable2.TABLE1.eq(element_1);
        ObjectSelect<ClientMtTable2> table2Query = ObjectSelect.query(ClientMtTable2.class)
                .where(exp);
        List<ClientMtTable2> table2List = table2Query.select(context);

        assertNotNull(table2List);
        assertEquals(2, table2List.size());

        exp = ExpressionFactory.matchExp(element_2);
        table2Query = ObjectSelect.query(ClientMtTable2.class)
                .where(exp);
        table2List = context.select(table2Query);

        assertNotNull(table2List);
        assertEquals(2, table2List.size());
    }

    @Test
    public void testExpressionFactoryMatchAll() throws Exception {
        createDataSet();

        ObjectSelect<ClientMtTable2> table2Query = ObjectSelect.query(ClientMtTable2.class)
                .orderBy(new Ordering("db:TABLE2_ID", SortOrder.ASCENDING));
        List<ClientMtTable2> table2List = context.select(table2Query);

        ClientMtTable2 element_1 = table2List.get(0);
        ClientMtTable2 element_2 = table2List.get(10);

        assertEquals(element_1.getTable1(), element_2.getTable1());

        Expression exp = ExpressionFactory.matchAllExp("|" + ClientMtTable1.TABLE2ARRAY.getName(), Arrays.asList(element_1, element_2));
        ObjectSelect<ClientMtTable1> table1Query = ObjectSelect.query(ClientMtTable1.class)
                .where(exp);
        List<ClientMtTable1> table1List = table1Query.select(context);

        assertEquals(1, table1List.size());
    }

    @Test
    public void testExpressionFactoryMatchAny() throws Exception {
        createDataSet();

        ObjectSelect<ClientMtTable2> table2Query = ObjectSelect.query(ClientMtTable2.class)
                .orderBy(new Ordering("db:TABLE2_ID", SortOrder.ASCENDING));
        List<ClientMtTable2> table2List = context.select(table2Query);

        ClientMtTable2 element_1 = table2List.get(0);
        ClientMtTable2 element_2 = table2List.get(10);

        Expression exp = ExpressionFactory.matchAnyExp(element_1, element_2);
        table2Query = ObjectSelect.query(ClientMtTable2.class)
                .where(exp);
        table2List = context.select(table2Query);

        assertEquals(2, table2List.size());
    }

    @Test
    public void testExpressionFactoryIn() throws Exception {
        createDataSet();

        ObjectSelect<ClientMtTable1> table1Query = ObjectSelect.query(ClientMtTable1.class)
                .orderBy(new Ordering("db:TABLE1_ID", SortOrder.ASCENDING));
        List<ClientMtTable1> table1List = context.select(table1Query);

        ClientMtTable1 element_3 = table1List.get(2);
        ClientMtTable1 element_8 = table1List.get(7);

        // IN expression via Collection
        Expression exp = ExpressionFactory.inExp(ClientMtTable2.TABLE1.getName(), table1List.subList(3, 6));
        ObjectSelect<ClientMtTable2> table2Query = ObjectSelect.query(ClientMtTable2.class)
                .where(exp);
        List<ClientMtTable2> table2List = context.select(table2Query);

        assertEquals(6, table2List.size());

        // IN expression via Array
        exp = ExpressionFactory.inExp(ClientMtTable2.TABLE1.getName(), element_3, element_8);
        table2Query = ObjectSelect.query(ClientMtTable2.class)
                .where(exp);
        table2List = context.select(table2Query);

        assertEquals(4, table2List.size());
    }

    @Test
    public void testExpressionFactoryBetween() throws Exception {
        createDataSet();

        ObjectSelect<ClientMtTable1> table1Query = ObjectSelect.query(ClientMtTable1.class)
                .orderBy(new Ordering("db:TABLE1_ID", SortOrder.ASCENDING));
        List<ClientMtTable1> table1List = context.select(table1Query);

        ClientMtTable1 element_1 = table1List.get(0);
        ClientMtTable1 element_7 = table1List.get(6);

        // between
        Expression exp = ExpressionFactory.betweenExp(ClientMtTable2.TABLE1.getName(), element_1, element_7);
        ObjectSelect<ClientMtTable2> table2Query = ObjectSelect.query(ClientMtTable2.class)
                .where(exp);
        List<ClientMtTable2> table2List = context.select(table2Query);

        assertEquals(14, table2List.size());

        // not between
        exp = ExpressionFactory.notBetweenExp(ClientMtTable2.TABLE1.getName(), element_1, element_7);
        table2Query = ObjectSelect.query(ClientMtTable2.class)
                .where(exp);
        table2List = context.select(table2Query);

        assertEquals(6, table2List.size());
    }

    @Test
    public void testExpressionFactoryOperators() throws Exception {
        createDataSet();

        ObjectSelect<ClientMtTable1> table1Query = ObjectSelect.query(ClientMtTable1.class)
                .orderBy(new Ordering("db:TABLE1_ID", SortOrder.ASCENDING));
        List<ClientMtTable1> table1List = context.select(table1Query);

        ClientMtTable1 element_7 = table1List.get(6);

        // greater than, ">"
        Expression exp = ExpressionFactory.greaterExp(ClientMtTable2.TABLE1.getName(), element_7);
        ObjectSelect<ClientMtTable2> table2Query = ObjectSelect.query(ClientMtTable2.class)
                .where(exp);
        List<ClientMtTable2> table2List = context.select(table2Query);

        assertEquals(6, table2List.size());

        // greater than or equal, ">="
        exp = ExpressionFactory.greaterOrEqualExp(ClientMtTable2.TABLE1.getName(), element_7);
        table2Query = ObjectSelect.query(ClientMtTable2.class)
                .where(exp);
        table2List = context.select(table2Query);

        assertEquals(8, table2List.size());

        // less than, "<"
        exp = ExpressionFactory.lessExp(ClientMtTable2.TABLE1.getName(), element_7);
        table2Query = ObjectSelect.query(ClientMtTable2.class)
                .where(exp);
        table2List = context.select(table2Query);

        assertEquals(12, table2List.size());

        // less than or equal, "<="
        exp = ExpressionFactory.lessOrEqualExp(ClientMtTable2.TABLE1.getName(), element_7);
        table2Query = ObjectSelect.query(ClientMtTable2.class)
                .where(exp);
        table2List = context.select(table2Query);

        assertEquals(14, table2List.size());
    }

    @Test
    public void testParams() throws Exception {
        createDataSet();

        ObjectSelect<ClientMtTable1> table1Query = ObjectSelect.query(ClientMtTable1.class)
                .orderBy(new Ordering("db:TABLE1_ID", SortOrder.ASCENDING));
        List<ClientMtTable1> table1List = context.select(table1Query);

        ClientMtTable1 element_1 = table1List.get(0);
        ClientMtTable1 element_5 = table1List.get(4);

        Expression exp = ExpressionFactory.exp("table1 = $attr");
        exp = exp.params(Collections.singletonMap("attr", element_1));
        ObjectSelect<ClientMtTable2> table2Query = ObjectSelect.query(ClientMtTable2.class)
                .where(exp);
        List<ClientMtTable2> table2List = context.select(table2Query);

        assertEquals(2, table2List.size());

        exp = exp.andExp(ExpressionFactory.exp("table1 = $attr")).params(Collections.singletonMap("attr", element_5));
        table2Query = ObjectSelect.query(ClientMtTable2.class)
                .where(exp);
        table2List = context.select(table2Query);

        assertEquals(0, table2List.size());
    }
}
