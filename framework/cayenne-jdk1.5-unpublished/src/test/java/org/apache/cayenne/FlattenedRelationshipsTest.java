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

import java.sql.Types;
import java.util.List;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.relationship.FlattenedCircular;
import org.apache.cayenne.testdo.relationship.FlattenedTest1;
import org.apache.cayenne.testdo.relationship.FlattenedTest2;
import org.apache.cayenne.testdo.relationship.FlattenedTest3;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

/**
 * Test case for objects with flattened relationships.
 */
@UseServerRuntime(ServerCase.RELATIONSHIPS_PROJECT)
public class FlattenedRelationshipsTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DataContext context1;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tFlattenedTest1;
    private TableHelper tFlattenedTest2;
    private TableHelper tFlattenedTest3;
    private TableHelper tComplexJoin;
    private TableHelper tFlattenedCircular;
    private TableHelper tFlattenedCircularJoin;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("COMPLEX_JOIN");
        dbHelper.deleteAll("FLATTENED_TEST_4");
        dbHelper.deleteAll("FLATTENED_TEST_3");
        dbHelper.deleteAll("FLATTENED_TEST_2");
        dbHelper.deleteAll("FLATTENED_TEST_1");
        dbHelper.deleteAll("FLATTENED_CIRCULAR_JOIN");
        dbHelper.deleteAll("FLATTENED_CIRCULAR");

        tFlattenedTest1 = new TableHelper(dbHelper, "FLATTENED_TEST_1");
        tFlattenedTest1.setColumns("FT1_ID", "NAME");

        tFlattenedTest2 = new TableHelper(dbHelper, "FLATTENED_TEST_2");
        tFlattenedTest2.setColumns("FT2_ID", "FT1_ID", "NAME");

        tFlattenedTest3 = new TableHelper(dbHelper, "FLATTENED_TEST_3");
        tFlattenedTest3.setColumns("FT3_ID", "FT2_ID", "NAME").setColumnTypes(
                Types.INTEGER, Types.INTEGER, Types.VARCHAR);

        tComplexJoin = new TableHelper(dbHelper, "COMPLEX_JOIN");
        tComplexJoin.setColumns("PK", "FT1_FK", "FT3_FK", "EXTRA_COLUMN");

        tFlattenedCircular = new TableHelper(dbHelper, "FLATTENED_CIRCULAR");
        tFlattenedCircular.setColumns("ID");

        tFlattenedCircularJoin = new TableHelper(dbHelper, "FLATTENED_CIRCULAR_JOIN");
        tFlattenedCircularJoin.setColumns("SIDE1_ID", "SIDE2_ID");
    }

    protected void createFlattenedTestDataSet() throws Exception {
        tFlattenedTest1.insert(1, "ft1");
        tFlattenedTest1.insert(2, "ft12");
        tFlattenedTest2.insert(1, 1, "ft2");
        tFlattenedTest3.insert(1, 1, "ft3");
    }

    protected void createFlattenedCircularDataSet() throws Exception {
        tFlattenedCircular.insert(1);
        tFlattenedCircular.insert(2);
        tFlattenedCircular.insert(3);
        tFlattenedCircularJoin.insert(1, 2);
        tFlattenedCircularJoin.insert(1, 3);
    }

    protected void createCircularJoinDataSet() throws Exception {
        tFlattenedTest1.insert(2, "ft12");
        tFlattenedTest3.insert(2, null, "ft3-a");
        tFlattenedTest3.insert(3, null, "ft3-b");
        tComplexJoin.insert(2000, 2, 2, "A");
        tComplexJoin.insert(2001, 2, 3, "B");
        tComplexJoin.insert(2002, 2, 3, "C");
    }

    public void testInsertJoinWithPK() throws Exception {
        FlattenedTest1 obj01 = context.newObject(FlattenedTest1.class);
        FlattenedTest3 obj11 = context.newObject(FlattenedTest3.class);
        FlattenedTest3 obj12 = context.newObject(FlattenedTest3.class);

        obj01.setName("t01");
        obj11.setName("t11");
        obj12.setName("t12");

        obj01.addToFt3OverComplex(obj11);
        obj01.addToFt3OverComplex(obj12);

        context.commitChanges();

        int pk = Cayenne.intPKForObject(obj01);

        context.invalidateObjects(obj01, obj11, obj12);

        FlattenedTest1 fresh01 = Cayenne.objectForPK(context1, FlattenedTest1.class, pk);

        assertEquals("t01", fresh01.getName());
        ValueHolder related = (ValueHolder) fresh01.getFt3OverComplex();
        assertTrue(related.isFault());

        assertEquals(2, ((List<?>) related).size());
    }

    public void testUnsetJoinWithPK() throws Exception {
        createCircularJoinDataSet();

        SQLTemplate joinSelect = new SQLTemplate(
                FlattenedTest1.class,
                "SELECT * FROM COMPLEX_JOIN");
        joinSelect.setFetchingDataRows(true);
        assertEquals(3, context.performQuery(joinSelect).size());

        FlattenedTest1 ft1 = Cayenne.objectForPK(context, FlattenedTest1.class, 2);

        assertEquals("ft12", ft1.getName());
        List<FlattenedTest3> related = ft1.getFt3OverComplex();
        assertTrue(((ValueHolder) related).isFault());

        assertEquals(2, related.size());

        FlattenedTest3 ft3 = Cayenne.objectForPK(context, FlattenedTest3.class, 3);
        assertTrue(related.contains(ft3));

        ft1.removeFromFt3OverComplex(ft3);
        assertFalse(related.contains(ft3));
        context.commitChanges();

        // the thing here is that there are two join records between
        // FT1 and FT3 (emulating invalid data or extras in the join table that
        // are ignored in the object model).. all (2) joins must be deleted
        assertEquals(1, context.performQuery(joinSelect).size());
    }

    public void testQualifyOnToManyFlattened() throws Exception {
        FlattenedTest1 obj01 = context.newObject(FlattenedTest1.class);
        FlattenedTest2 obj02 = context.newObject(FlattenedTest2.class);
        FlattenedTest3 obj031 = context.newObject(FlattenedTest3.class);
        FlattenedTest3 obj032 = context.newObject(FlattenedTest3.class);

        FlattenedTest1 obj11 = context.newObject(FlattenedTest1.class);
        FlattenedTest2 obj12 = context.newObject(FlattenedTest2.class);
        FlattenedTest3 obj131 = context.newObject(FlattenedTest3.class);

        obj01.setName("t01");
        obj02.setName("t02");
        obj031.setName("t031");
        obj032.setName("t032");
        obj02.setToFT1(obj01);
        obj02.addToFt3Array(obj031);
        obj02.addToFt3Array(obj032);

        obj11.setName("t11");
        obj131.setName("t131");
        obj12.setName("t12");
        obj12.addToFt3Array(obj131);
        obj12.setToFT1(obj11);

        context.commitChanges();

        // test 1: qualify on flattened attribute
        Expression qual1 = ExpressionFactory.matchExp("ft3Array.name", "t031");
        SelectQuery query1 = new SelectQuery(FlattenedTest1.class, qual1);
        List<?> objects1 = context.performQuery(query1);

        assertEquals(1, objects1.size());
        assertSame(obj01, objects1.get(0));

        // test 2: qualify on flattened relationship
        Expression qual2 = ExpressionFactory.matchExp("ft3Array", obj131);
        SelectQuery query2 = new SelectQuery(FlattenedTest1.class, qual2);
        List<?> objects2 = context.performQuery(query2);

        assertEquals(1, objects2.size());
        assertSame(obj11, objects2.get(0));
    }

    public void testToOneSeriesFlattenedRel() {

        FlattenedTest1 ft1 = (FlattenedTest1) context.newObject("FlattenedTest1");
        ft1.setName("FT1Name");
        FlattenedTest2 ft2 = (FlattenedTest2) context.newObject("FlattenedTest2");
        ft2.setName("FT2Name");
        FlattenedTest3 ft3 = (FlattenedTest3) context.newObject("FlattenedTest3");
        ft3.setName("FT3Name");

        ft2.setToFT1(ft1);
        ft2.addToFt3Array(ft3);
        context.commitChanges();

        context.invalidateObjects(ft1, ft2, ft3);

        SelectQuery q = new SelectQuery(FlattenedTest3.class);
        q.setQualifier(ExpressionFactory.matchExp("name", "FT3Name"));
        List<?> results = context1.performQuery(q);

        assertEquals(1, results.size());

        FlattenedTest3 fetchedFT3 = (FlattenedTest3) results.get(0);
        FlattenedTest1 fetchedFT1 = fetchedFT3.getToFT1();
        assertEquals("FT1Name", fetchedFT1.getName());
    }

    public void testTakeObjectSnapshotFlattenedFault() throws Exception {
        createFlattenedTestDataSet();

        // fetch
        List<?> ft3s = context.performQuery(new SelectQuery(FlattenedTest3.class));
        assertEquals(1, ft3s.size());
        FlattenedTest3 ft3 = (FlattenedTest3) ft3s.get(0);

        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);

        // test that taking a snapshot does not trigger a fault, and generally works well
        DataRow snapshot = context.currentSnapshot(ft3);

        assertEquals("ft3", snapshot.get("NAME"));
        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);

    }

    public void testRefetchWithFlattenedFaultToOneTarget1() throws Exception {
        createFlattenedTestDataSet();

        // fetch
        List<?> ft3s = context.performQuery(new SelectQuery(FlattenedTest3.class));
        assertEquals(1, ft3s.size());
        FlattenedTest3 ft3 = (FlattenedTest3) ft3s.get(0);

        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);

        // refetch
        context.performQuery(new SelectQuery(FlattenedTest3.class));
        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);
    }

    public void testFlattenedCircular() throws Exception {
        createFlattenedCircularDataSet();

        FlattenedCircular fc1 = Cayenne.objectForPK(context, FlattenedCircular.class, 1);

        List<FlattenedCircular> side2s = fc1.getSide2s();
        assertEquals(2, side2s.size());

        List<FlattenedCircular> side1s = fc1.getSide1s();
        assertTrue(side1s.isEmpty());
    }

}
