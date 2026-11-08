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

package org.apache.cayenne.access.flush;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.flush.operation.DefaultDbRowOpSorter;
import org.apache.cayenne.access.flush.operation.BaseDbRowOp;
import org.apache.cayenne.access.flush.operation.DbRowOp;
import org.apache.cayenne.access.flush.operation.DbRowOpSorter;
import org.apache.cayenne.access.flush.operation.DeleteDbRowOp;
import org.apache.cayenne.access.flush.operation.InsertDbRowOp;
import org.apache.cayenne.access.flush.operation.UpdateDbRowOp;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.map.compiled.TestMappings;
import org.apache.cayenne.unit.CompiledTables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DefaultDbRowOpSorterTest {

    private CompiledMapping mapping;
    private DbRowOpSorter sorter;

    @BeforeEach
    public void createSorter() {
        // a chain of FKs test4 -> test3 -> test2 -> test1 -> test fixes the table order, and the "reflexive" table
        // references itself
        DataMap map = new DataMap("M");
        DbEntity test = CompiledTables.authoredTable("test");
        DbEntity test1 = CompiledTables.authoredTable("test1");
        DbEntity test2 = CompiledTables.authoredTable("test2");
        DbEntity test3 = CompiledTables.authoredTable("test3");
        DbEntity test4 = CompiledTables.authoredTable("test4");
        DbEntity reflexive = CompiledTables.authoredTable("reflexive");
        for (DbEntity table : List.of(test, test1, test2, test3, test4, reflexive)) {
            map.addDbEntity(table);
        }
        addForeignKey(test1, test, "test");
        addForeignKey(test2, test1, "test1");
        addForeignKey(test3, test2, "test2");
        addForeignKey(test4, test3, "test3");
        addForeignKey(reflexive, reflexive, "parent");

        ObjEntity reflexiveEntity = new ObjEntity("reflexive");
        reflexiveEntity.setDbEntityName("reflexive");
        map.addObjEntity(reflexiveEntity);
        ObjRelationship parent = new ObjRelationship("parent");
        reflexiveEntity.addRelationship(parent);
        parent.setTargetEntityName("reflexive");
        parent.setDbRelationshipPath("parent");


        mapping = TestMappings.compile(map);

        DataDomain dataDomain = mock(DataDomain.class);
        when(dataDomain.getMapping()).thenReturn(mapping);

        sorter = new DefaultDbRowOpSorter(() -> dataDomain);
    }

    @Test
    public void sortEmptyList() {
        List<DbRowOp> rows = new ArrayList<>();
        List<DbRowOp> sorted = sorter.sort(rows);
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
        DbRowOp op1 = new InsertDbRowOp(mockObject(id1), test, id1);
        DbRowOp op2 = new UpdateDbRowOp(mockObject(id2), test, id2);
        DbRowOp op3 = new DeleteDbRowOp(mockObject(id3), test, id3);
        DbRowOp op4 = new InsertDbRowOp(mockObject(id4), test, id4);
        DbRowOp op5 = new UpdateDbRowOp(mockObject(id5), test, id5);
        DbRowOp op6 = new DeleteDbRowOp(mockObject(id6), test, id6);

        List<DbRowOp> rows = Arrays.asList(op1, op2, op3, op4, op5, op6);
        List<DbRowOp> expected = Arrays.asList(op1, op4, op2, op5, op3, op6);

        List<DbRowOp> sorted = sorter.sort(rows);
        assertEquals(expected, sorted);
    }

    @Test
    public void sortByOpEntity() {
        ObjectId id1 = ObjectId.of("test4", "id", 1);
        ObjectId id2 = ObjectId.of("test2", "id", 2);
        ObjectId id3 = ObjectId.of("test3", "id", 3);
        ObjectId id4 = ObjectId.of("test1", "id", 4);

        DbRowOp op1 = new InsertDbRowOp(mockObject(id1), table("test4"), id1);
        DbRowOp op2 = new InsertDbRowOp(mockObject(id2), table("test2"), id2);
        DbRowOp op3 = new InsertDbRowOp(mockObject(id3), table("test3"), id3);
        DbRowOp op4 = new InsertDbRowOp(mockObject(id4), table("test1"), id4);

        List<DbRowOp> rows = Arrays.asList(op1, op2, op3, op4);
        List<DbRowOp> expected = Arrays.asList(op4, op2, op3, op1);

        List<DbRowOp> sorted = sorter.sort(rows);
        assertEquals(expected, sorted);
    }

    @Test
    public void sortById() {
        ObjectId id1 = ObjectId.of("test", "id", 1);
        ObjectId id2 = ObjectId.of("test", "id", 2);
        ObjectId id3 = ObjectId.of("test", "id", 2);
        ObjectId id4 = ObjectId.of("test", "id", 3);

        CompiledTable test = table("test");
        InsertDbRowOp op1 = new InsertDbRowOp(mockObject(id1), test, id1);
        InsertDbRowOp op2 = new InsertDbRowOp(mockObject(id2), test, id2);
        DeleteDbRowOp op3 = new DeleteDbRowOp(mockObject(id3), test, id3);
        DeleteDbRowOp op4 = new DeleteDbRowOp(mockObject(id4), test, id4);

        List<DbRowOp> rows = Arrays.asList(op1, op2, op3, op4);
        List<DbRowOp> expected = Arrays.asList(op1, op2, op3, op4);

        List<DbRowOp> sorted = sorter.sort(rows);
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
        op[0] = new InsertDbRowOp(mockObject(id1),  test2, id1);
        op[1] = new InsertDbRowOp(mockObject(id2),  test,  id2);
        op[2] = new DeleteDbRowOp(mockObject(id3),  test,  id3);
        op[3] = new UpdateDbRowOp(mockObject(id4),  test,  id4);
        op[4] = new InsertDbRowOp(mockObject(id5),  test2, id5);
        op[5] = new DeleteDbRowOp(mockObject(id6),  test,  id6);
        op[6] = new InsertDbRowOp(mockObject(id7),  test,  id7);
        op[7] = new UpdateDbRowOp(mockObject(id8),  test2, id8);
        op[8] = new DeleteDbRowOp(mockObject(id9),  test2, id9);
        op[9] = new DeleteDbRowOp(mockObject(id10), test,  id10);

        List<DbRowOp> expected = Arrays.asList(op[1], op[6], op[0], op[4], op[3], op[7], op[8], op[2], op[5], op[9]);
        List<DbRowOp> sorted = sorter.sort(Arrays.asList(op));

        assertEquals(expected, sorted);
    }

    @Test
    public void sortReflexive() {
        // temporary ids: the parents are read from the objects, not looked up in a context
        ObjectId id1 = ObjectId.of("reflexive");
        ObjectId id2 = ObjectId.of("reflexive");
        ObjectId id3 = ObjectId.of("reflexive");
        ObjectId id4 = ObjectId.of("reflexive");

        CompiledTable reflexive = table("reflexive");
        Persistent o1 = mockObject(id1);
        Persistent o2 = mockObject(id2);
        Persistent o3 = mockObject(id3);
        Persistent o4 = mockObject(id4);

        // o1 is a child of o3, o4 is a child of o2
        when(o1.readProperty("parent")).thenReturn(o3);
        when(o4.readProperty("parent")).thenReturn(o2);

        DbRowOp op1 = new InsertDbRowOp(o1, reflexive, id1);
        DbRowOp op2 = new InsertDbRowOp(o2, reflexive, id2);
        DbRowOp op3 = new InsertDbRowOp(o3, reflexive, id3);
        DbRowOp op4 = new InsertDbRowOp(o4, reflexive, id4);

        List<DbRowOp> sorted = sorter.sort(new ArrayList<>(Arrays.asList(op1, op2, op3, op4)));
        assertEquals(4, sorted.size());

        // parents are inserted before their children
        assertTrue(sorted.indexOf(op3) < sorted.indexOf(op1), sorted.toString());
        assertTrue(sorted.indexOf(op2) < sorted.indexOf(op4), sorted.toString());
    }

    @Test
    public void sortReflexiveDeletes() {
        // temporary ids: the parents are read from the objects, not looked up in a context
        ObjectId id1 = ObjectId.of("reflexive");
        ObjectId id2 = ObjectId.of("reflexive");
        ObjectId id3 = ObjectId.of("reflexive");
        ObjectId id4 = ObjectId.of("reflexive");

        CompiledTable reflexive = table("reflexive");
        Persistent o1 = mockObject(id1);
        Persistent o2 = mockObject(id2);
        Persistent o3 = mockObject(id3);
        Persistent o4 = mockObject(id4);

        // o3 is a child of o1, o2 is a child of o4
        when(o3.readProperty("parent")).thenReturn(o1);
        when(o2.readProperty("parent")).thenReturn(o4);

        DbRowOp op1 = new DeleteDbRowOp(o1, reflexive, id1);
        DbRowOp op2 = new DeleteDbRowOp(o2, reflexive, id2);
        DbRowOp op3 = new DeleteDbRowOp(o3, reflexive, id3);
        DbRowOp op4 = new DeleteDbRowOp(o4, reflexive, id4);

        List<DbRowOp> sorted = sorter.sort(new ArrayList<>(Arrays.asList(op1, op2, op3, op4)));
        assertEquals(4, sorted.size());

        // children are deleted before their parents
        assertTrue(sorted.indexOf(op3) < sorted.indexOf(op1), sorted.toString());
        assertTrue(sorted.indexOf(op2) < sorted.indexOf(op4), sorted.toString());
    }

    @Test
    public void sortReflexiveUpdates() {
        ObjectId id1 = ObjectId.of("reflexive", "id", 1);
        ObjectId id2 = ObjectId.of("reflexive", "id", 2);
        ObjectId id3 = ObjectId.of("reflexive", "id", 3);
        ObjectId id4 = ObjectId.of("reflexive", "id", 4);

        CompiledTable reflexive = table("reflexive");
        Persistent o1 = mockObject(id1);
        Persistent o2 = mockObject(id2);
        Persistent o3 = mockObject(id3);
        Persistent o4 = mockObject(id4);

        // the parent references would reorder inserts, but updates are not sorted reflexively
        when(o1.readProperty("parent")).thenReturn(o3);
        when(o2.readProperty("parent")).thenReturn(o4);

        DbRowOp op1 = new UpdateDbRowOp(o1, reflexive, id1);
        DbRowOp op2 = new UpdateDbRowOp(o2, reflexive, id2);
        DbRowOp op3 = new UpdateDbRowOp(o3, reflexive, id3);
        DbRowOp op4 = new UpdateDbRowOp(o4, reflexive, id4);

        List<DbRowOp> rows = Arrays.asList(op1, op2, op3, op4);
        List<DbRowOp> expected = Arrays.asList(op1, op2, op3, op4);

        List<DbRowOp> sorted = sorter.sort(rows);
        assertEquals(expected, sorted);
        verify(o1, never()).readProperty("parent");
        verify(o2, never()).readProperty("parent");
    }

    private CompiledTable table(String name) {
        return mapping.schema().table(name);
    }

    private Persistent mockObject(ObjectId id) {
        Persistent persistent = mock(Persistent.class);
        when(persistent.getObjectId()).thenReturn(id);
        when(persistent.getPersistenceState()).thenReturn(PersistenceState.MODIFIED);
        return persistent;
    }

    private static void addForeignKey(DbEntity source, DbEntity target, String name) {
        String fkColumn = name + "_id";
        source.addAttribute(new DbAttribute(fkColumn));

        DbRelationship relationship = new DbRelationship(name);
        source.addRelationship(relationship);
        relationship.setTargetEntityName(target.getName());
        relationship.setToMany(false);
        relationship.setForeignKey(true);
        relationship.addJoin(new DbJoin(relationship, fkColumn, "id"));
    }
}
