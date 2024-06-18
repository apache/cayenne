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

package org.apache.cayenne;

import java.util.List;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.relationships_flattened.Entity1;
import org.apache.cayenne.testdo.relationships_flattened.Entity3;
import org.apache.cayenne.testdo.relationships_flattened.FlattenedCircular;
import org.apache.cayenne.testdo.relationships_flattened.FlattenedTest1;
import org.apache.cayenne.testdo.relationships_flattened.FlattenedTest2;
import org.apache.cayenne.testdo.relationships_flattened.FlattenedTest3;
import org.apache.cayenne.testdo.relationships_flattened.FlattenedTest4;
import org.apache.cayenne.testdo.relationships_flattened.FlattenedTest5;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.apache.cayenne.validation.ValidationResult;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test case for objects with flattened relationships.
 */
@UseCayenneRuntime(CayenneProjects.RELATIONSHIPS_FLATTENED_PROJECT)
public class FlattenedRelationshipsIT extends RuntimeCase {

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

    @Before
    public void setUp() throws Exception {
        tFlattenedTest1 = new TableHelper(dbHelper, "FLATTENED_TEST_1");
        tFlattenedTest1.setColumns("FT1_ID", "NAME");

        tFlattenedTest2 = new TableHelper(dbHelper, "FLATTENED_TEST_2");
        tFlattenedTest2.setColumns("FT2_ID", "FT1_ID", "NAME");

        tFlattenedTest3 = new TableHelper(dbHelper, "FLATTENED_TEST_3");
        tFlattenedTest3.setColumns("FT3_ID", "FT2_ID", "NAME");

        tComplexJoin = new TableHelper(dbHelper, "COMPLEX_JOIN");
        tComplexJoin.setColumns("PK", "FT1_FK", "FT3_FK", "EXTRA_COLUMN");

        tFlattenedCircular = new TableHelper(dbHelper, "FLATTENED_CIRCULAR");
        tFlattenedCircular.setColumns("ID");

        tFlattenedCircularJoin = new TableHelper(dbHelper, "FLATTENED_CIRCULAR_JOIN");
        tFlattenedCircularJoin.setColumns("SIDE1_ID", "SIDE2_ID");
    }

    private void createFlattenedTestDataSet() throws Exception {
        tFlattenedTest1.insert(1, "ft1");
        tFlattenedTest1.insert(2, "ft12");
        tFlattenedTest2.insert(1, 1, "ft2");
        tFlattenedTest3.insert(1, 1, "ft3");
    }

    private void createFlattenedCircularDataSet() throws Exception {
        tFlattenedCircular.insert(1);
        tFlattenedCircular.insert(2);
        tFlattenedCircular.insert(3);
        tFlattenedCircularJoin.insert(1, 2);
        tFlattenedCircularJoin.insert(1, 3);
    }

    private void createCircularJoinDataSet() throws Exception {
        tFlattenedTest1.insert(2, "ft12");
        tFlattenedTest3.insert(2, null, "ft3-a");
        tFlattenedTest3.insert(3, null, "ft3-b");
        tComplexJoin.insert(2000, 2, 2, "A");
        tComplexJoin.insert(2001, 2, 3, "B");
        tComplexJoin.insert(2002, 2, 3, "C");
    }

    @Test
    public void testInsertJoinWithPK() {
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

    @Test
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

    @Test
    public void testQualifyOnToManyFlattened() {
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
        List<FlattenedTest1> objects1 = ObjectSelect.query(FlattenedTest1.class)
                .where(FlattenedTest1.FT3ARRAY.dot(FlattenedTest3.NAME).eq("t031"))
                .select(context);

        assertEquals(1, objects1.size());
        assertSame(obj01, objects1.get(0));

        // test 2: qualify on flattened relationship
        List<FlattenedTest1> objects2 = ObjectSelect.query(FlattenedTest1.class)
                .where(FlattenedTest1.FT3ARRAY.contains(obj131))
                .select(context);

        assertEquals(1, objects2.size());
        assertSame(obj11, objects2.get(0));
    }

    @Test
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

        List<FlattenedTest3> results = ObjectSelect.query(FlattenedTest3.class, FlattenedTest3.NAME.eq("FT3Name")).select(context);

        assertEquals(1, results.size());

        FlattenedTest3 fetchedFT3 = results.get(0);
        FlattenedTest1 fetchedFT1 = fetchedFT3.getToFT1();
        assertEquals("FT1Name", fetchedFT1.getName());
    }

    @Test
    public void testTakeObjectSnapshotFlattenedFault() throws Exception {
        createFlattenedTestDataSet();

        // fetch
        List<FlattenedTest3> ft3s = ObjectSelect.query(FlattenedTest3.class).select(context);
        assertEquals(1, ft3s.size());
        FlattenedTest3 ft3 = ft3s.get(0);

        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);

        // test that taking a snapshot does not trigger a fault, and generally works well
        DataRow snapshot = context.currentSnapshot(ft3);

        assertEquals("ft3", snapshot.get("NAME"));
        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);

    }

    @Test
    public void testRefetchWithFlattenedFaultToOneTarget1() throws Exception {
        createFlattenedTestDataSet();

        // fetch
        List<FlattenedTest3> ft3s = ObjectSelect.query(FlattenedTest3.class).select(context);
        assertEquals(1, ft3s.size());
        FlattenedTest3 ft3 = ft3s.get(0);

        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);

        // refetch
        ObjectSelect.query(FlattenedTest3.class).select(context);
        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);
    }

    @Test
    public void testFlattenedCircular() throws Exception {
        createFlattenedCircularDataSet();

        FlattenedCircular fc1 = Cayenne.objectForPK(context, FlattenedCircular.class, 1);

        List<FlattenedCircular> side2s = fc1.getSide2s();
        assertEquals(2, side2s.size());

        List<FlattenedCircular> side1s = fc1.getSide1s();
        assertTrue(side1s.isEmpty());
    }

    /**
     * Should be able to save/insert an object with flattened (complex) toOne relationship
     */
    @Test
    public void testFlattenedComplexToOneRelationship() {
        FlattenedTest1 ft1 = context.newObject(FlattenedTest1.class);
        ft1.setName("FT1");

        FlattenedTest5 ft5 = context.newObject(FlattenedTest5.class);
        ft5.setName("FT5");
        ft5.setToFT1(ft1);

        context.commitChanges();

        FlattenedTest5 ft5Persisted = ObjectSelect.query(FlattenedTest5.class).selectFirst(context);
        assertEquals(ft1, ft5Persisted.getToFT1());
    }

    /**
     * Should be able to save/insert an object with null flattened (complex) toOne relationship
     */
    @Test
    public void testNullFlattenedComplexToOneRelationship() {
        FlattenedTest5 ft5 = context.newObject(FlattenedTest5.class);
        ft5.setName("FT5");

        // should be valid for save
        ValidationResult validationResult = new ValidationResult();
        ft5.validateForSave(validationResult);

        assertTrue(validationResult.toString(), validationResult.getFailures().isEmpty());

        context.commitChanges();

        assertEquals(1, ObjectSelect.query(FlattenedTest5.class).selectCount(context));
    }

    @Test
    public void testSetFlattenedRelationship() {
        FlattenedTest1 flattenedTest1 = context.newObject(FlattenedTest1.class);
        flattenedTest1.setName("f1");
        FlattenedTest3 flattenedTest3 = context.newObject(FlattenedTest3.class);
        flattenedTest3.setName("f3");
        flattenedTest3.setToFT1(flattenedTest1);

        context.commitChanges();

        List<FlattenedTest3> flattenedTest3s = ObjectSelect.query(FlattenedTest3.class)
                .prefetch(FlattenedTest3.TO_FT1.disjoint())
                .select(context);
        assertEquals(1, flattenedTest3s.size());
        assertEquals("f3", flattenedTest3s.get(0).getName());
        assertEquals("f1", flattenedTest3s.get(0).getToFT1().getName());
    }

    @Test
    public void testSecondToOneReverseToFk() {
        FlattenedTest1 flattenedTest1 = context.newObject(FlattenedTest1.class);
        flattenedTest1.setName("f1");
        FlattenedTest4 flattenedTest4 = context.newObject(FlattenedTest4.class);
        flattenedTest4.setName("f4");
        flattenedTest1.addToFt4ArrayFor1(flattenedTest4);
        context.commitChanges();
    }

    @Test
    public void testSecondToOneToFk() {
        Entity1 entity1 = context.newObject(Entity1.class);
        Entity3 entity3 = context.newObject(Entity3.class);
        entity1.setToEntity3(entity3);
        context.commitChanges();

        List<Entity1> entity1s = ObjectSelect.query(Entity1.class)
                .prefetch(Entity1.TO_ENTITY3.disjoint())
                .select(context);
        assertEquals(1, entity1s.size());
        assertNotNull(entity1s.get(0).getToEntity3());
    }

    @Test
    public void testSetFlattenedCircular() {
        FlattenedCircular flattenedCircular1 = context.newObject(FlattenedCircular.class);
        FlattenedCircular flattenedCircular2 = context.newObject(FlattenedCircular.class);
        flattenedCircular1.addToSide1s(flattenedCircular2);
        context.commitChanges();

        List<FlattenedCircular> flattenedCirculars = ObjectSelect.query(FlattenedCircular.class)
                .prefetch(FlattenedCircular.SIDE1S.disjoint())
                .select(context);

        assertEquals(2, flattenedCirculars.size());
    }

    @Test
    public void testFt1ToFt5Flattened() {
        FlattenedTest1 flattenedTest1 = context.newObject(FlattenedTest1.class);
        flattenedTest1.setName("f1");
        FlattenedTest5 flattenedTest5 = context.newObject(FlattenedTest5.class);
        flattenedTest5.setName("f5");
        flattenedTest1.addToFt5Array(flattenedTest5);
        context.commitChanges();

        List<FlattenedTest1> flattenedTest1s = ObjectSelect.query(FlattenedTest1.class)
                .prefetch(FlattenedTest1.FT5ARRAY.disjoint())
                .select(context);
        assertEquals(1, flattenedTest1s.size());
        assertEquals(1, flattenedTest1s.get(0).getFt5Array().size());
    }

    @Test
    public void testFt5ToFt1Flattened() {
        FlattenedTest5 flattenedTest5 = context.newObject(FlattenedTest5.class);
        flattenedTest5.setName("f5");
        FlattenedTest1 flattenedTest1 = context.newObject(FlattenedTest1.class);
        flattenedTest1.setName("f1");
        flattenedTest5.setToFT1(flattenedTest1);
        context.commitChanges();

        List<FlattenedTest5> flattenedTest5s = ObjectSelect.query(FlattenedTest5.class)
                .prefetch(FlattenedTest5.TO_FT1.disjoint())
                .select(context);
        assertEquals(1, flattenedTest5s.size());
        assertEquals("f5", flattenedTest5s.get(0).getName());
        assertEquals("f1", flattenedTest5s.get(0).getToFT1().getName());
    }
}
