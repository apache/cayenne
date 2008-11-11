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
import java.util.Map;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.relationship.FlattenedCircular;
import org.apache.cayenne.testdo.relationship.FlattenedTest1;
import org.apache.cayenne.testdo.relationship.FlattenedTest2;
import org.apache.cayenne.testdo.relationship.FlattenedTest3;
import org.apache.cayenne.unit.RelationshipCase;

/**
 * Test case for objects with flattened relationships.
 * 
 */
public class FlattenedRelationshipsTest extends RelationshipCase {

    protected DataContext context;

    @Override
    protected void setUp() throws Exception {
        deleteTestData();
        context = createDataContext();
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

        int pk = DataObjectUtils.intPKForObject(obj01);

        context = createDataContext();
        FlattenedTest1 fresh01 = DataObjectUtils.objectForPK(
                context,
                FlattenedTest1.class,
                pk);

        assertEquals("t01", fresh01.getName());
        ValueHolder related = (ValueHolder) fresh01.getFt3OverComplex();
        assertTrue(related.isFault());

        assertEquals(2, ((List) related).size());
    }

    public void testUnsetJoinWithPK() throws Exception {
        createTestData("testUnsetJoinWithPK");

        SQLTemplate joinSelect = new SQLTemplate(
                FlattenedTest1.class,
                "SELECT * FROM COMPLEX_JOIN");
        joinSelect.setFetchingDataRows(true);
        assertEquals(3, context.performQuery(joinSelect).size());

        FlattenedTest1 ft1 = DataObjectUtils
                .objectForPK(context, FlattenedTest1.class, 2);

        assertEquals("ft12", ft1.getName());
        List related = ft1.getFt3OverComplex();
        assertTrue(((ValueHolder) related).isFault());

        assertEquals(2, related.size());

        FlattenedTest3 ft3 = DataObjectUtils
                .objectForPK(context, FlattenedTest3.class, 3);
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
        List objects1 = context.performQuery(query1);

        assertEquals(1, objects1.size());
        assertSame(obj01, objects1.get(0));

        // test 2: qualify on flattened relationship
        Expression qual2 = ExpressionFactory.matchExp("ft3Array", obj131);
        SelectQuery query2 = new SelectQuery(FlattenedTest1.class, qual2);
        List objects2 = context.performQuery(query2);

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

        context = createDataContext(); // We need a new context
        SelectQuery q = new SelectQuery(FlattenedTest3.class);
        q.setQualifier(ExpressionFactory.matchExp("name", "FT3Name"));
        List results = context.performQuery(q);

        assertEquals(1, results.size());

        FlattenedTest3 fetchedFT3 = (FlattenedTest3) results.get(0);
        FlattenedTest1 fetchedFT1 = fetchedFT3.getToFT1();
        assertEquals("FT1Name", fetchedFT1.getName());
    }

    public void testTakeObjectSnapshotFlattenedFault() throws Exception {
        createTestData("test");

        // fetch
        List ft3s = context.performQuery(new SelectQuery(FlattenedTest3.class));
        assertEquals(1, ft3s.size());
        FlattenedTest3 ft3 = (FlattenedTest3) ft3s.get(0);

        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);

        // test that taking a snapshot does not trigger a fault, and generally works well
        Map snapshot = context.currentSnapshot(ft3);

        assertEquals("ft3", snapshot.get("NAME"));
        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);

    }

    public void testRefetchWithFlattenedFaultToOneTarget1() throws Exception {
        createTestData("test");

        // fetch
        List ft3s = context.performQuery(new SelectQuery(FlattenedTest3.class));
        assertEquals(1, ft3s.size());
        FlattenedTest3 ft3 = (FlattenedTest3) ft3s.get(0);

        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);

        // refetch
        context.performQuery(new SelectQuery(FlattenedTest3.class));
        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);
    }

    public void testFlattenedCircular() throws Exception {
        createTestData("testFlattenedCircular");
        context = createDataContext();
        FlattenedCircular fc1 = DataObjectUtils.objectForPK(
                context,
                FlattenedCircular.class,
                1);

        List<FlattenedCircular> side2s = fc1.getSide2s();
        assertEquals(2, side2s.size());

        List<FlattenedCircular> side1s = fc1.getSide1s();
        assertTrue(side1s.isEmpty());
    }

}
