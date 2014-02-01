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
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class ClientExpressionTest extends ClientCase {
    
    @Inject
    private CayenneContext context;
    
    @Inject
    private DBHelper dbHelper;
    
    private TableHelper tMtTable1;
    private TableHelper tMtTable2;
    
    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("MT_TABLE2");
        dbHelper.deleteAll("MT_TABLE1");
        
        tMtTable1 = new TableHelper(dbHelper, "MT_TABLE1");
        tMtTable1.setColumns("TABLE1_ID", "GLOBAL_ATTRIBUTE1", "SERVER_ATTRIBUTE1");
        
        tMtTable2 = new TableHelper(dbHelper, "MT_TABLE2");
        tMtTable2.setColumns("TABLE2_ID", "TABLE1_ID", "GLOBAL_ATTRIBUTE");
    }
    
    protected void createDataSet() throws Exception {
        for(int i = 1; i <= 10; i++) {
            tMtTable1.insert(i ,"1_global" + i, "server" + i);
            tMtTable2.insert(i , i, "2_global" + i);
            tMtTable2.insert(i + 10, i, "2_global" + (i + 10));
        }
    }
    
    public void testPersistentValueInExpression() throws Exception {
        ClientMtTable1 t1 = context.newObject(ClientMtTable1.class);
        ClientMtTable1 t2 = context.newObject(ClientMtTable1.class);
        
        context.commitChanges();
        
        Expression scalar = ExpressionFactory.matchExp(null, t1);
        Expression list = ExpressionFactory.matchAllExp("|", Arrays.asList(t1, t2));
        
        assertEquals(t1.getObjectId(), scalar.getOperand(1));
        assertEquals(t1.getObjectId(), ((ASTEqual)list.getOperand(0)).getOperand(1));
        assertEquals(t2.getObjectId(), ((ASTEqual)list.getOperand(1)).getOperand(1));
    }
    
    public void testListInASTList() throws Exception {
        ClientMtTable1 t1 = context.newObject(ClientMtTable1.class);
        ClientMtTable1 t2 = context.newObject(ClientMtTable1.class);
        
        context.commitChanges();
        
        List<ClientMtTable1> table1List = new ArrayList<ClientMtTable1>();
        table1List.add(t1);
        table1List.add(t2);
        
        // send list in expression factory
        Expression list = ExpressionFactory.inExp(ClientMtTable2.TABLE1_PROPERTY, table1List);
        
        Object[] values = (Object[])((ASTList)list.getOperand(1)).getOperand(0);
        assertEquals(t1.getObjectId(), values[0]);
        assertEquals(t2.getObjectId(), values[1]);
        
        ObjectId t1Id = new ObjectId("MtTable1", "TABLE1_ID", 1);
        ObjectId t2Id = new ObjectId("MtTable1", "TABLE1_ID", 2);
        t1.setObjectId(t1Id);
        t2.setObjectId(t2Id);

        //Expression and client have different copies of object
        assertNotSame(t1.getObjectId(), values[0]);
        assertNotSame(t2.getObjectId(), values[1]);
    }
    
    public void testArrayInASTList() throws Exception {
        ClientMtTable1 t1 = context.newObject(ClientMtTable1.class);
        ClientMtTable1 t2 = context.newObject(ClientMtTable1.class);
        ClientMtTable1 t3 = context.newObject(ClientMtTable1.class);
        
        context.commitChanges();
        
        Object[] tArray = new Object[3];
        tArray[0] = t1;
        tArray[1] = t2;
        
        // send array in expression factory
        Expression list = ExpressionFactory.inExp(ClientMtTable2.TABLE1_PROPERTY, tArray);
        tArray[2] = t3;
        
        Object[] values = (Object[])((ASTList)list.getOperand(1)).getOperand(0);
        assertEquals(tArray.length, values.length);
        assertNotSame(tArray[2], values[2]);
        assertEquals(t1.getObjectId(), values[0]);
        assertEquals(t2.getObjectId(), values[1]);
        
        ObjectId t1Id = new ObjectId("MtTable1", "TABLE1_ID", 1);
        ObjectId t2Id = new ObjectId("MtTable1", "TABLE1_ID", 2);
        t1.setObjectId(t1Id);
        t2.setObjectId(t2Id);
        
        // Expression and client have different arrays
        assertNotSame(t1.getObjectId(), values[0]);
        assertNotSame(t2.getObjectId(), values[1]);
    }
    
    public void testExpressionFactoryMatch() throws Exception {
        createDataSet();
        
        SelectQuery<ClientMtTable1> table1Query = new SelectQuery<ClientMtTable1>(ClientMtTable1.class);
        table1Query.addOrdering(new Ordering("db:TABLE1_ID", SortOrder.ASCENDING));
        List<ClientMtTable1> table1List = context.select(table1Query);
        
        assertNotNull(table1List);
        
        ClientMtTable1 element_1 = table1List.get(0);
        ClientMtTable1 element_2 = table1List.get(1);
        
        Expression exp = ExpressionFactory.matchExp(ClientMtTable2.TABLE1_PROPERTY, element_1);
        SelectQuery<ClientMtTable2> table2Query = new SelectQuery<ClientMtTable2>(ClientMtTable2.class, exp);
        List<ClientMtTable2> table2List = context.select(table2Query);
        
        assertNotNull(table2List);
        assertEquals(2, table2List.size());
        
        exp = ExpressionFactory.matchExp(element_2);
        table2Query = new SelectQuery<ClientMtTable2>(ClientMtTable2.class, exp);
        table2List = context.select(table2Query);

        assertNotNull(table2List);
        assertEquals(2, table2List.size());
    }
    
    public void testExpressionFactoryMatchAll() throws Exception {
        createDataSet();
        
        SelectQuery<ClientMtTable2> table2Query = new SelectQuery<ClientMtTable2>(ClientMtTable2.class);
        table2Query.addOrdering(new Ordering("db:TABLE2_ID", SortOrder.ASCENDING));
        List<ClientMtTable2> table2List = context.select(table2Query);
        
        ClientMtTable2 element_1 = table2List.get(0);
        ClientMtTable2 element_2 = table2List.get(10);
        
        assertEquals(element_1.getTable1(), element_2.getTable1());
        
        Expression exp = ExpressionFactory.matchAllExp("|"+ClientMtTable1.TABLE2ARRAY_PROPERTY, Arrays.asList(element_1, element_2));
        SelectQuery<ClientMtTable1> table1Query = new SelectQuery<ClientMtTable1>(ClientMtTable1.class, exp);
        List<ClientMtTable1> table1List = context.select(table1Query);
        
        assertEquals(1, table1List.size());
    }
    
    public void testExpressionFactoryMatchAny() throws Exception {
        createDataSet();
        
        SelectQuery<ClientMtTable2> table2Query = new SelectQuery<ClientMtTable2>(ClientMtTable2.class);
        table2Query.addOrdering(new Ordering("db:TABLE2_ID", SortOrder.ASCENDING));
        List<ClientMtTable2> table2List = context.select(table2Query);
        
        ClientMtTable2 element_1 = table2List.get(0);
        ClientMtTable2 element_2 = table2List.get(10);
        
        Expression exp = ExpressionFactory.matchAnyExp(element_1, element_2);
        table2Query = new SelectQuery<ClientMtTable2>(ClientMtTable2.class, exp);
        table2List = context.select(table2Query);
        
        assertEquals(2, table2List.size());
    }
    
    public void testExpressionFactoryIn() throws Exception {
        createDataSet();
        
        SelectQuery<ClientMtTable1> table1Query = new SelectQuery<ClientMtTable1>(ClientMtTable1.class);
        table1Query.addOrdering(new Ordering("db:TABLE1_ID", SortOrder.ASCENDING));
        List<ClientMtTable1> table1List = context.select(table1Query);
        
        ClientMtTable1 element_3 = table1List.get(2);
        ClientMtTable1 element_8 = table1List.get(7);
        
        // IN expression via Collection
        Expression exp = ExpressionFactory.inExp(ClientMtTable2.TABLE1_PROPERTY, table1List.subList(3, 6));
        SelectQuery<ClientMtTable2> table2Query = new SelectQuery<ClientMtTable2>(ClientMtTable2.class, exp);
        List<ClientMtTable2> table2List = context.select(table2Query);
        
        assertEquals(6, table2List.size());
        
        // IN expression via Array
        exp = ExpressionFactory.inExp(ClientMtTable2.TABLE1_PROPERTY, element_3, element_8);
        table2Query = new SelectQuery<ClientMtTable2>(ClientMtTable2.class, exp);
        table2List = context.select(table2Query);
        
        assertEquals(4, table2List.size());
    }
    
    public void testExpressionFactoryBetween() throws Exception {
        createDataSet();
        
        SelectQuery<ClientMtTable1> table1Query = new SelectQuery<ClientMtTable1>(ClientMtTable1.class);
        table1Query.addOrdering(new Ordering("db:TABLE1_ID", SortOrder.ASCENDING));
        List<ClientMtTable1> table1List = context.select(table1Query);
        
        ClientMtTable1 element_1 = table1List.get(0);
        ClientMtTable1 element_7 = table1List.get(6);
        
        // between
        Expression exp = ExpressionFactory.betweenExp(ClientMtTable2.TABLE1_PROPERTY, element_1, element_7);
        SelectQuery<ClientMtTable2> table2Query = new SelectQuery<ClientMtTable2>(ClientMtTable2.class, exp);
        List<ClientMtTable2> table2List = context.select(table2Query);
        
        assertEquals(14, table2List.size());
        
        // not between
        exp = ExpressionFactory.notBetweenExp(ClientMtTable2.TABLE1_PROPERTY, element_1, element_7);
        table2Query = new SelectQuery<ClientMtTable2>(ClientMtTable2.class, exp);
        table2List = context.select(table2Query);
        
        assertEquals(6, table2List.size());
    }
    
    public void testExpressionFactoryOperators() throws Exception {
        createDataSet();
        
        SelectQuery<ClientMtTable1> table1Query = new SelectQuery<ClientMtTable1>(ClientMtTable1.class);
        table1Query.addOrdering(new Ordering("db:TABLE1_ID", SortOrder.ASCENDING));
        List<ClientMtTable1> table1List = context.select(table1Query);
        
        ClientMtTable1 element_7 = table1List.get(6);
        
        // greater than, ">"
        Expression exp = ExpressionFactory.greaterExp(ClientMtTable2.TABLE1_PROPERTY, element_7);
        SelectQuery<ClientMtTable2> table2Query = new SelectQuery<ClientMtTable2>(ClientMtTable2.class, exp);
        List<ClientMtTable2> table2List = context.select(table2Query);
        
        assertEquals(6, table2List.size());
        
        // greater than or equal, ">="
        exp = ExpressionFactory.greaterOrEqualExp(ClientMtTable2.TABLE1_PROPERTY, element_7);
        table2Query = new SelectQuery<ClientMtTable2>(ClientMtTable2.class, exp);
        table2List = context.select(table2Query);
        
        assertEquals(8, table2List.size());
        
        // less than, "<"
        exp = ExpressionFactory.lessExp(ClientMtTable2.TABLE1_PROPERTY, element_7);
        table2Query = new SelectQuery<ClientMtTable2>(ClientMtTable2.class, exp);
        table2List = context.select(table2Query);
        
        assertEquals(12, table2List.size());
        
        // less than or equal, "<="
        exp = ExpressionFactory.lessOrEqualExp(ClientMtTable2.TABLE1_PROPERTY, element_7);
        table2Query = new SelectQuery<ClientMtTable2>(ClientMtTable2.class, exp);
        table2List = context.select(table2Query);
        
        assertEquals(14, table2List.size());
    }
    
    public void testExpressionWithParameters() throws Exception {
        createDataSet();
        
        SelectQuery<ClientMtTable1> table1Query = new SelectQuery<ClientMtTable1>(ClientMtTable1.class);
        table1Query.addOrdering(new Ordering("db:TABLE1_ID", SortOrder.ASCENDING));
        List<ClientMtTable1> table1List = context.select(table1Query);
        
        ClientMtTable1 element_1 = table1List.get(0);
        ClientMtTable1 element_5 = table1List.get(4);
        
        Expression exp = Expression.fromString("table1 = $attr");
        exp = exp.expWithParameters(Collections.singletonMap("attr", element_1));
        SelectQuery<ClientMtTable2> table2Query = new SelectQuery<ClientMtTable2>(ClientMtTable2.class, exp);
        List<ClientMtTable2> table2List = context.select(table2Query);
        
        assertEquals(2, table2List.size());
        
        exp = exp.andExp(Expression.fromString("table1 = $attr"))
                .expWithParameters(Collections.singletonMap("attr", element_5));
        table2Query = new SelectQuery<ClientMtTable2>(ClientMtTable2.class, exp);
        table2List = context.select(table2Query);
        
        assertEquals(0, table2List.size());
    }
}
