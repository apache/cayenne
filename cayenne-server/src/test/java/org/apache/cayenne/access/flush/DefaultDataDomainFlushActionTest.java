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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.flush.operation.BaseDbRowOp;
import org.apache.cayenne.access.flush.operation.DbRowOp;
import org.apache.cayenne.access.flush.operation.DeleteDbRowOp;
import org.apache.cayenne.access.flush.operation.InsertDbRowOp;
import org.apache.cayenne.access.flush.operation.UpdateDbRowOp;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.Query;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @since 4.2
 */
public class DefaultDataDomainFlushActionTest {

    @Test
    public void mergeSameObjectIds() {
        ObjectId id1  = ObjectId.of("test2", "id", 1);
        ObjectId id2  = ObjectId.of("test",  "id", 2);
        ObjectId id3  = ObjectId.of("test",  "id", 2);
        ObjectId id4  = ObjectId.of("test",  "id", 3);
        ObjectId id5  = ObjectId.of("test2", "id", 4);
        ObjectId id6  = ObjectId.of("test",  "id", 5);
        ObjectId id7  = ObjectId.of("test",  "id", 6);
        ObjectId id8  = ObjectId.of("test2", "id", 3);
        ObjectId id9  = ObjectId.of("test2", "id", 4);
        ObjectId id10 = ObjectId.of("test",  "id", 6);

        DbEntity test = mockEntity("test");
        DbEntity test2 = mockEntity("test2");
        BaseDbRowOp[] op = new BaseDbRowOp[10];
        op[0] = new InsertDbRowOp(mockObject(id1),  test2, id1); // +
        op[1] = new InsertDbRowOp(mockObject(id2),  test,  id2); // -
        op[2] = new DeleteDbRowOp(mockObject(id3),  test,  id3); // -
        op[3] = new UpdateDbRowOp(mockObject(id4),  test,  id4); // +
        op[4] = new InsertDbRowOp(mockObject(id5),  test2, id5); // -
        op[5] = new DeleteDbRowOp(mockObject(id6),  test,  id6); // +
        op[6] = new InsertDbRowOp(mockObject(id7),  test,  id7); // -
        op[7] = new UpdateDbRowOp(mockObject(id8),  test2, id8); // +
        op[8] = new DeleteDbRowOp(mockObject(id9),  test2, id9); // -
        op[9] = new DeleteDbRowOp(mockObject(id10), test,  id10);// -

        DefaultDataDomainFlushAction action = mock(DefaultDataDomainFlushAction.class);
        when(action.mergeSameObjectIds((List<DbRowOp>) any(List.class))).thenCallRealMethod();

        Collection<DbRowOp> merged = action.mergeSameObjectIds(new ArrayList<>(Arrays.asList(op)));
        assertEquals(7, merged.size());
        assertThat(merged, hasItems(op[0], op[3], op[5], op[7]));
        assertThat(merged, not(hasItem(sameInstance(op[1]))));
        assertThat(merged, not(hasItem(sameInstance(op[2]))));
        assertThat(merged, not(hasItem(sameInstance(op[4]))));
        assertThat(merged, not(hasItem(sameInstance(op[6]))));
        assertThat(merged, not(hasItem(sameInstance(op[8]))));
        assertThat(merged, not(hasItem(sameInstance(op[9]))));
    }

    @Test
    public void mergeSameObjectsId_ReplacementId() {
        ObjectId id1  = ObjectId.of("db:test2");
        id1.getReplacementIdMap().put("id", 1);
        ObjectId id2  = ObjectId.of("db:test");
        id2.getReplacementIdMap().put("id", 1);
        ObjectId id3  = ObjectId.of("db:test");
        id3.getReplacementIdMap().put("id", 1);
        ObjectId id4  = ObjectId.of("db:test");
        id4.getReplacementIdMap().put("id", 2);

        DbEntity test = mockEntity("test");
        DbEntity test2 = mockEntity("test2");
        BaseDbRowOp[] op = new BaseDbRowOp[4];
        op[0] = new InsertDbRowOp(mockObject(id1),  test2, id1); // +
        op[1] = new InsertDbRowOp(mockObject(id2),  test,  id2); // -
        op[2] = new DeleteDbRowOp(mockObject(id3),  test,  id3); // -
        op[3] = new UpdateDbRowOp(mockObject(id4),  test,  id4); // +

        DefaultDataDomainFlushAction action = mock(DefaultDataDomainFlushAction.class);
        when(action.mergeSameObjectIds((List<DbRowOp>) any(List.class))).thenCallRealMethod();

        Collection<DbRowOp> merged = action.mergeSameObjectIds(new ArrayList<>(Arrays.asList(op)));
        assertEquals(3, merged.size());

        assertThat(merged, hasItems(op[0], op[2], op[3]));
        assertThat(merged, not(hasItem(sameInstance(op[1]))));
    }

    @Test
    public void mergeSameObjectIdCompoundId() {
        Map<String, Object> keyMap1_1 = new LinkedHashMap<>();
        keyMap1_1.put("student_id", 1);
        keyMap1_1.put("course_id", 1);

        Map<String, Object> keyMap1_2 = new LinkedHashMap<>();
        keyMap1_2.put("student_id", 1);
        keyMap1_2.put("course_id", 2);

        Map<String, Object> keyMap1_3 = new LinkedHashMap<>();
        keyMap1_3.put("student_id", 1);
        keyMap1_3.put("course_id", 3);

        ObjectId studentObjectId1  = ObjectId.of("student", "id", 1);

        ObjectId enrollmentObjectId1_1  = ObjectId.of("enrollment",  keyMap1_1);
        enrollmentObjectId1_1.getReplacementIdMap().put("student_id",1);

        ObjectId enrollmentObjectId1_2  = ObjectId.of("enrollment",  keyMap1_2);
        enrollmentObjectId1_2.getReplacementIdMap().put("student_id",1);

        ObjectId enrollmentObjectId1_3  = ObjectId.of("enrollment",  keyMap1_3);
        enrollmentObjectId1_3.getReplacementIdMap().put("student_id",1);

        DbEntity student = mockEntity("student");
        DbEntity enrollment = mockEntity("enrollment");

        List<DbRowOp> ops = new ArrayList<>();

        ops.add(new DeleteDbRowOp(mockObject(studentObjectId1),  student, studentObjectId1));
        ops.add(new UpdateDbRowOp(mockObject(enrollmentObjectId1_1),  enrollment, enrollmentObjectId1_1));
        ops.add(new UpdateDbRowOp(mockObject(enrollmentObjectId1_3),  enrollment, enrollmentObjectId1_3));
        ops.add(new UpdateDbRowOp(mockObject(enrollmentObjectId1_2),  enrollment, enrollmentObjectId1_2));

        ops.add(new DeleteDbRowOp(mockObject(enrollmentObjectId1_3),  enrollment, enrollmentObjectId1_3));
        ops.add(new DeleteDbRowOp(mockObject(enrollmentObjectId1_2),  enrollment, enrollmentObjectId1_2));
        ops.add(new DeleteDbRowOp(mockObject(enrollmentObjectId1_1),  enrollment, enrollmentObjectId1_1));


        DefaultDataDomainFlushAction action = mock(DefaultDataDomainFlushAction.class);
        when(action.mergeSameObjectIds((List<DbRowOp>) any(List.class))).thenCallRealMethod();

        Collection<DbRowOp> merged = action.mergeSameObjectIds(ops);
        assertEquals(4, merged.size());

        merged.forEach(dbRowOp -> assertThat(dbRowOp, instanceOf(DeleteDbRowOp.class)));
    }


    @Test
    public void createQueries() {
        ObjectId id1  = ObjectId.of("test",  "id", 1);
        ObjectId id2  = ObjectId.of("test",  "id", 2);
        ObjectId id3  = ObjectId.of("test2", "id", 3);
        ObjectId id4  = ObjectId.of("test2", "id", 4);
        ObjectId id5  = ObjectId.of("test",  "id", 5);
        ObjectId id6  = ObjectId.of("test2", "id", 6);
        ObjectId id7  = ObjectId.of("test",  "id", 7);

        DbEntity test = mockEntity("test");
        DbEntity test2 = mockEntity("test2");

        List<DbRowOp> ops = new ArrayList<>();
        ops.add(new InsertDbRowOp(mockObject(id1),  test,  id1));
        ops.add(new InsertDbRowOp(mockObject(id2),  test,  id2));
        ops.add(new InsertDbRowOp(mockObject(id3),  test2, id5));
        ops.add(new InsertDbRowOp(mockObject(id4),  test2, id7));
        ops.add(new UpdateDbRowOp(mockObject(id5),  test,  id3));
        ops.add(new DeleteDbRowOp(mockObject(id6),  test2, id6));
        ops.add(new DeleteDbRowOp(mockObject(id7),  test,  id4));

        DefaultDataDomainFlushAction action = mock(DefaultDataDomainFlushAction.class);
        when(action.createQueries((List<DbRowOp>) any(List.class))).thenCallRealMethod();

        List<? extends Query> queries = action.createQueries(ops);
        assertEquals(4, queries.size());
        assertThat(queries.get(0), instanceOf(InsertBatchQuery.class));
        InsertBatchQuery insert1 = (InsertBatchQuery)queries.get(0);
        assertSame(test, insert1.getDbEntity());
        assertEquals(2, insert1.getRows().size());

        assertThat(queries.get(1), instanceOf(InsertBatchQuery.class));
        InsertBatchQuery insert2 = (InsertBatchQuery)queries.get(1);
        assertSame(test2, insert2.getDbEntity());
        assertEquals(2, insert2.getRows().size());

        assertThat(queries.get(2), instanceOf(DeleteBatchQuery.class));
        DeleteBatchQuery delete1 = (DeleteBatchQuery)queries.get(2);
        assertSame(test2, delete1.getDbEntity());
        assertEquals(1, delete1.getRows().size());

        assertThat(queries.get(3), instanceOf(DeleteBatchQuery.class));
        DeleteBatchQuery delete2 = (DeleteBatchQuery)queries.get(3);
        assertSame(test, delete2.getDbEntity());
        assertEquals(1, delete2.getRows().size());
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