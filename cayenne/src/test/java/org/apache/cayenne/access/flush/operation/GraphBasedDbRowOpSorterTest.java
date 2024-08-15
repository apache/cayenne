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
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntitySorter;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

public class GraphBasedDbRowOpSorterTest {
    private EntitySorter entitySorter;
    private DbRowOpSorter sorter;

    @Before
    public void createSorter() {
        entitySorter = mock(EntitySorter.class);
        EntityResolver entityResolver = mock(EntityResolver.class);

        when(entitySorter.getDbEntityComparator())
                .thenReturn(Comparator.comparing(DbEntity::getName));
        when(entitySorter.isReflexive(argThat(ent -> ent.getName().equals("reflexive"))))
                .thenReturn(true);

        DataDomain dataDomain = mock(DataDomain.class);
        when(dataDomain.getEntitySorter()).thenReturn(entitySorter);
        when(dataDomain.getEntityResolver()).thenReturn(entityResolver);

        sorter = new GraphBasedDbRowOpSorter(() -> dataDomain);
    }

    @Test
    public void sortEmptyList() {
        List<DbRowOp> rows = new ArrayList<>();
        List<DbRowOp> sorted = sorter.sort(rows);
        assertTrue(sorted.isEmpty());
    }

    @Test
    public void sortByOpEntity() {
        ObjectId id1 = ObjectId.of("test4", "id", 1);
        ObjectId id2 = ObjectId.of("test2", "id", 2);
        ObjectId id3 = ObjectId.of("test3", "id", 3);
        ObjectId id4 = ObjectId.of("test1", "id", 4);

        DbRowOp op1 = new InsertDbRowOp(mockObject(id1), mockEntity("test4"), id1);
        DbRowOp op2 = new InsertDbRowOp(mockObject(id2), mockEntity("test2"), id2);
        DbRowOp op3 = new InsertDbRowOp(mockObject(id3), mockEntity("test3"), id3);
        DbRowOp op4 = new InsertDbRowOp(mockObject(id4), mockEntity("test1"), id4);

        List<DbRowOp> rows = Arrays.asList(op1, op2, op3, op4);
        List<DbRowOp> expected = Arrays.asList(op1, op2, op3, op4);

        List<DbRowOp> sorted = sorter.sort(rows);
        assertEquals(expected, sorted);
    }

    @Test
    public void sortById() {
        ObjectId id1 = ObjectId.of("test", "id", 1);
        ObjectId id2 = ObjectId.of("test", "id", 2);
        ObjectId id3 = ObjectId.of("test", "id", 2);
        ObjectId id4 = ObjectId.of("test", "id", 3);

        DbEntity test = mockEntity("test");
        InsertDbRowOp op1 = new InsertDbRowOp(mockObject(id1), test, id1);
        InsertDbRowOp op2 = new InsertDbRowOp(mockObject(id2), test, id2);
        DeleteDbRowOp op3 = new DeleteDbRowOp(mockObject(id3), test, id3);
        DeleteDbRowOp op4 = new DeleteDbRowOp(mockObject(id4), test, id4);

        List<DbRowOp> rows = Arrays.asList(op1, op2, op3, op4);
        List<DbRowOp> expected = Arrays.asList(op3, op1, op2, op4);

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

        DbEntity test = mockEntity("test");
        DbEntity test2 = mockEntity("test2");
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

        List<DbRowOp> expected = Arrays.asList(op[2], op[8], op[9], op[0], op[1], op[3], op[4], op[5], op[6], op[7]);
        List<DbRowOp> sorted = sorter.sort(Arrays.asList(op));

        assertEquals(expected, sorted);
    }

    @Test
    public void sortReflexive() {
        ObjectId id1 = ObjectId.of("reflexive", "id", 1);
        ObjectId id2 = ObjectId.of("reflexive", "id", 2);
        ObjectId id3 = ObjectId.of("reflexive", "id", 3);
        ObjectId id4 = ObjectId.of("reflexive", "id", 4);

        DbEntity reflexive = mockEntity("reflexive");
        DbRowOp op1 = new InsertDbRowOp(mockObject(id1), reflexive, id1);
        DbRowOp op2 = new InsertDbRowOp(mockObject(id2), reflexive, id2);
        DbRowOp op3 = new InsertDbRowOp(mockObject(id3), reflexive, id3);
        DbRowOp op4 = new InsertDbRowOp(mockObject(id4), reflexive, id4);

        List<DbRowOp> rows = Arrays.asList(op1, op2, op3, op4);
        List<DbRowOp> expected = Arrays.asList(op1, op2, op3, op4);

        List<DbRowOp> sorted = sorter.sort(rows);
        assertEquals(expected, sorted); // no actual sorting is done
        verifyNoInteractions(entitySorter);
    }

    private Persistent mockObject(ObjectId id) {
        Persistent persistent = mock(Persistent.class);
        when(persistent.getObjectId()).thenReturn(id);
        when(persistent.getPersistenceState()).thenReturn(PersistenceState.MODIFIED);
        return persistent;
    }

    private DbEntity mockEntity(String name) {
        DbAttribute attribute1 = new DbAttribute("id");
        attribute1.setPrimaryKey(true);
        DbAttribute attribute2 = new DbAttribute("attr");
        DbEntity testEntity = new DbEntity(name);
        testEntity.addAttribute(attribute1);
        testEntity.addAttribute(attribute2);
        return testEntity;
    }
}