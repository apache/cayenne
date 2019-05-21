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
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 4.2
 */
public class BaseDbRowOpTest {

    @Test
    public void testEquals_SameId() {
        ObjectId id = ObjectId.of("test");

        DbRowOp row1 = new InsertDbRowOp(mockObject(id), mockEntity(), id);
        DbRowOp row2 = new InsertDbRowOp(mockObject(id), mockEntity(), id);

        assertEquals(row1, row2);
        assertEquals(row2, row1);
    }

    @Test
    public void testEquals_EqualId() {
        ObjectId id1 = ObjectId.of("test", "id", 1);
        ObjectId id2 = ObjectId.of("test", "id", 1);

        DbRowOp row1 = new InsertDbRowOp(mockObject(id1), mockEntity(), id1);
        DbRowOp row2 = new InsertDbRowOp(mockObject(id2), mockEntity(), id2);

        assertEquals(row1, row2);
        assertEquals(row2, row1);
    }

    @Test
    public void testNotEquals_EqualId() {
        ObjectId id1 = ObjectId.of("test", "id", 1);
        ObjectId id2 = ObjectId.of("test", "id", 1);

        DbRowOp row1 = new InsertDbRowOp(mockObject(id1), mockEntity(), id1);
        DbRowOp row2 = new DeleteDbRowOp(mockObject(id2), mockEntity(), id2);

        assertNotEquals(row1, row2);
        assertNotEquals(row2, row1);
    }

    @Test
    public void testEqualsInsertUpdate_EqualId() {
        ObjectId id1 = ObjectId.of("test", "id", 1);
        ObjectId id2 = ObjectId.of("test", "id", 1);

        DbRowOp row1 = new InsertDbRowOp(mockObject(id1), mockEntity(), id1);
        DbRowOp row2 = new UpdateDbRowOp(mockObject(id2), mockEntity(), id2);

        assertEquals(row1, row2);
        assertEquals(row2, row1);
    }

    @Test
    public void testEqualsUpdateDelete_EqualId() {
        ObjectId id1 = ObjectId.of("test", "id", 1);
        ObjectId id2 = ObjectId.of("test", "id", 1);

        DbRowOp row1 = new DeleteDbRowOp(mockObject(id1), mockEntity(), id1);
        DbRowOp row2 = new UpdateDbRowOp(mockObject(id2), mockEntity(), id2);

        assertEquals(row1, row2);
        assertEquals(row2, row1);
    }

    @Test
    public void testNotEquals_NotEqualId() {
        ObjectId id1 = ObjectId.of("test", "id", 1);
        ObjectId id2 = ObjectId.of("test", "id", 2);

        DbRowOp row1 = new InsertDbRowOp(mockObject(id1), mockEntity(), id1);
        DbRowOp row2 = new InsertDbRowOp(mockObject(id2), mockEntity(), id2);

        assertNotEquals(row1, row2);
        assertNotEquals(row2, row1);
    }

    private Persistent mockObject(ObjectId id) {
        Persistent persistent = mock(Persistent.class);
        when(persistent.getObjectId()).thenReturn(id);
        when(persistent.getPersistenceState()).thenReturn(PersistenceState.MODIFIED);
        return persistent;
    }

    private DbEntity mockEntity() {
        DbAttribute attribute1 = new DbAttribute("id");
        attribute1.setPrimaryKey(true);
        DbAttribute attribute2 = new DbAttribute("attr");
        DbEntity testEntity = new DbEntity("TEST");
        testEntity.addAttribute(attribute1);
        testEntity.addAttribute(attribute2);
        return testEntity;
    }
}