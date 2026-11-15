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

package org.apache.cayenne.access.flush.operation;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.map.compiled.TestMappings;
import org.apache.cayenne.unit.CompiledTables;
import org.apache.cayenne.unit.TestObjects;
import org.apache.cayenne.unit.TestDomains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultDbRowOpPlannerTest {
    private CompiledMapping mapping;
    private DbRowOpPlanner planner;

    @BeforeEach
    public void createPlanner() {
        // unrelated tables, and a "reflexive" table referencing itself
        DataMap map = new DataMap("M");
        for (String name : List.of("test", "test1", "test2", "test3", "test4", "reflexive")) {
            map.addDbEntity(CompiledTables.authoredTable(name));
        }
        DbEntity reflexive = map.getDbEntity("reflexive");
        reflexive.addAttribute(new DbAttribute("parent_id"));
        DbRelationship parent = new DbRelationship("parent");
        reflexive.addRelationship(parent);
        parent.setTargetEntityName("reflexive");
        parent.setToMany(false);
        parent.setForeignKey(true);
        parent.addJoin(new DbJoin(parent, "parent_id", "id"));

        mapping = TestMappings.compile(map);
        DataDomain dataDomain = TestDomains.of(mapping);

        planner = new DefaultDbRowOpPlanner(() -> dataDomain);
    }

    @Test
    public void sortEmptyList() {
        List<DbRowOp> rows = new ArrayList<>();
        List<DbRowOp> sorted = planner.plan(rows);
        assertTrue(sorted.isEmpty());
    }

    @Test
    public void sortByOpType() {
        ObjectId id1 = ObjectId.of("test", "id", 1);
        ObjectId id2 = ObjectId.of("test", "id", 2);
        ObjectId id3 = ObjectId.of("test", "id", 3);
        ObjectId id4 = ObjectId.of("test", "id", 4);
        ObjectId id5 = ObjectId.of("test", "id", 5);
        ObjectId id6 = ObjectId.of("test", "id", 6);

        CompiledTable test = table("test");
        DbRowOp op1 = new InsertDbRowOp(TestObjects.modifiedObject(id1), test, id1);
        DbRowOp op2 = new UpdateDbRowOp(TestObjects.modifiedObject(id2), test, id2);
        DbRowOp op3 = new DeleteDbRowOp(TestObjects.modifiedObject(id3), test, id3);
        DbRowOp op4 = new InsertDbRowOp(TestObjects.modifiedObject(id4), test, id4);
        DbRowOp op5 = new UpdateDbRowOp(TestObjects.modifiedObject(id5), test, id5);
        DbRowOp op6 = new DeleteDbRowOp(TestObjects.modifiedObject(id6), test, id6);

        List<DbRowOp> rows = Arrays.asList(op1, op2, op3, op4, op5, op6);

        // no dependencies between the rows, so inserts, updates, deletes, each kept together
        List<DbRowOp> expected = Arrays.asList(op1, op4, op2, op5, op3, op6);

        List<DbRowOp> sorted = planner.plan(rows);
        assertEquals(expected, sorted);
    }

    @Test
    public void sortByOpEntity() {
        ObjectId id1 = ObjectId.of("test4", "id", 1);
        ObjectId id2 = ObjectId.of("test2", "id", 2);
        ObjectId id3 = ObjectId.of("test3", "id", 3);
        ObjectId id4 = ObjectId.of("test1", "id", 4);

        DbRowOp op1 = new InsertDbRowOp(TestObjects.modifiedObject(id1), table("test4"), id1);
        DbRowOp op2 = new InsertDbRowOp(TestObjects.modifiedObject(id2), table("test2"), id2);
        DbRowOp op3 = new InsertDbRowOp(TestObjects.modifiedObject(id3), table("test3"), id3);
        DbRowOp op4 = new InsertDbRowOp(TestObjects.modifiedObject(id4), table("test1"), id4);

        List<DbRowOp> rows = Arrays.asList(op1, op2, op3, op4);

        // no dependencies between the rows, so the table order
        List<DbRowOp> expected = Arrays.asList(op4, op2, op3, op1);

        List<DbRowOp> sorted = planner.plan(rows);
        assertEquals(expected, sorted);
    }

    @Test
    public void sortById() {
        ObjectId id1 = ObjectId.of("test", "id", 1);
        ObjectId id2 = ObjectId.of("test", "id", 2);
        ObjectId id3 = ObjectId.of("test", "id", 2);
        ObjectId id4 = ObjectId.of("test", "id", 3);

        CompiledTable test = table("test");
        InsertDbRowOp op1 = new InsertDbRowOp(TestObjects.modifiedObject(id1), test, id1);
        InsertDbRowOp op2 = new InsertDbRowOp(TestObjects.modifiedObject(id2), test, id2);
        DeleteDbRowOp op3 = new DeleteDbRowOp(TestObjects.modifiedObject(id3), test, id3);
        DeleteDbRowOp op4 = new DeleteDbRowOp(TestObjects.modifiedObject(id4), test, id4);

        List<DbRowOp> rows = Arrays.asList(op1, op2, op3, op4);

        // the inserts go first, except for the one of the PK deleted, that waits for the delete
        List<DbRowOp> expected = Arrays.asList(op1, op3, op4, op2);

        List<DbRowOp> sorted = planner.plan(rows);
        assertEquals(expected, sorted);
    }

    @Test
    public void sortByIdDifferentEntities() {
        ObjectId id1  = ObjectId.of("test2", "id", 1);
        ObjectId id2  = ObjectId.of("test",  "id", 2);
        ObjectId id3  = ObjectId.of("test",  "id", 2);
        ObjectId id4  = ObjectId.of("test",  "id", 3);
        ObjectId id5  = ObjectId.of("test2", "id", 4);
        ObjectId id6  = ObjectId.of("test",  "id", 5);
        ObjectId id7  = ObjectId.of("test",  "id", 8);
        ObjectId id8  = ObjectId.of("test2", "id", 7);
        ObjectId id9  = ObjectId.of("test2", "id", 4);
        ObjectId id10 = ObjectId.of("test",  "id", 8);

        CompiledTable test = table("test");
        CompiledTable test2 = table("test2");
        BaseDbRowOp[] op = new BaseDbRowOp[10];
        op[0] = new InsertDbRowOp(TestObjects.modifiedObject(id1),  test2, id1);
        op[1] = new InsertDbRowOp(TestObjects.modifiedObject(id2),  test,  id2);
        op[2] = new DeleteDbRowOp(TestObjects.modifiedObject(id3),  test,  id3);
        op[3] = new UpdateDbRowOp(TestObjects.modifiedObject(id4),  test,  id4);
        op[4] = new InsertDbRowOp(TestObjects.modifiedObject(id5),  test2, id5);
        op[5] = new DeleteDbRowOp(TestObjects.modifiedObject(id6),  test,  id6);
        op[6] = new InsertDbRowOp(TestObjects.modifiedObject(id7),  test,  id7);
        op[7] = new UpdateDbRowOp(TestObjects.modifiedObject(id8),  test2, id8);
        op[8] = new DeleteDbRowOp(TestObjects.modifiedObject(id9),  test2, id9);
        op[9] = new DeleteDbRowOp(TestObjects.modifiedObject(id10), test,  id10);

        // inserts, updates, deletes (in the reverse table order), each group by table, except for the inserts of the
        // deleted PKs, that wait for the deletes
        List<DbRowOp> expected = Arrays.asList(op[0], op[3], op[7], op[8], op[4], op[2], op[5], op[9], op[1], op[6]);
        List<DbRowOp> sorted = planner.plan(Arrays.asList(op));

        assertEquals(expected, sorted);
    }

    @Test
    public void sortReflexive() {
        ObjectId id1 = ObjectId.of("reflexive", "id", 1);
        ObjectId id2 = ObjectId.of("reflexive", "id", 2);
        ObjectId id3 = ObjectId.of("reflexive", "id", 3);
        ObjectId id4 = ObjectId.of("reflexive", "id", 4);

        CompiledTable reflexive = table("reflexive");
        DbRowOp op1 = new InsertDbRowOp(TestObjects.modifiedObject(id1), reflexive, id1);
        DbRowOp op2 = new InsertDbRowOp(TestObjects.modifiedObject(id2), reflexive, id2);
        DbRowOp op3 = new InsertDbRowOp(TestObjects.modifiedObject(id3), reflexive, id3);
        DbRowOp op4 = new InsertDbRowOp(TestObjects.modifiedObject(id4), reflexive, id4);

        List<DbRowOp> rows = Arrays.asList(op1, op2, op3, op4);
        List<DbRowOp> expected = Arrays.asList(op1, op2, op3, op4);

        List<DbRowOp> sorted = planner.plan(rows);
        assertEquals(expected, sorted); // no actual sorting is done
    }


    private CompiledTable table(String name) {
        return mapping.schema().table(name);
    }
}