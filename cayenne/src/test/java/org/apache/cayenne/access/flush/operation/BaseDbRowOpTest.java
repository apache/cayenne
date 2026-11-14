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
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.unit.CompiledTables;
import org.apache.cayenne.unit.TestObjects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BaseDbRowOpTest {

    @Test
    public void equals_SameId() {
        ObjectId id = ObjectId.of("test");

        DbRowOp row1 = new InsertDbRowOp(TestObjects.modifiedObject(id), table(), id);
        DbRowOp row2 = new InsertDbRowOp(TestObjects.modifiedObject(id), table(), id);

        assertEquals(row1, row2);
        assertEquals(row2, row1);
    }

    @Test
    public void equals_EqualId() {
        ObjectId id1 = ObjectId.of("test", "id", 1);
        ObjectId id2 = ObjectId.of("test", "id", 1);

        DbRowOp row1 = new InsertDbRowOp(TestObjects.modifiedObject(id1), table(), id1);
        DbRowOp row2 = new InsertDbRowOp(TestObjects.modifiedObject(id2), table(), id2);

        assertEquals(row1, row2);
        assertEquals(row2, row1);
    }

    @Test
    public void notEquals_EqualId() {
        ObjectId id1 = ObjectId.of("test", "id", 1);
        ObjectId id2 = ObjectId.of("test", "id", 1);

        DbRowOp row1 = new InsertDbRowOp(TestObjects.modifiedObject(id1), table(), id1);
        DbRowOp row2 = new DeleteDbRowOp(TestObjects.modifiedObject(id2), table(), id2);

        assertNotEquals(row1, row2);
        assertNotEquals(row2, row1);
    }

    @Test
    public void equalsInsertUpdate_EqualId() {
        ObjectId id1 = ObjectId.of("test", "id", 1);
        ObjectId id2 = ObjectId.of("test", "id", 1);

        DbRowOp row1 = new InsertDbRowOp(TestObjects.modifiedObject(id1), table(), id1);
        DbRowOp row2 = new UpdateDbRowOp(TestObjects.modifiedObject(id2), table(), id2);

        assertEquals(row1, row2);
        assertEquals(row2, row1);
    }

    @Test
    public void equalsUpdateDelete_EqualId() {
        ObjectId id1 = ObjectId.of("test", "id", 1);
        ObjectId id2 = ObjectId.of("test", "id", 1);

        DbRowOp row1 = new DeleteDbRowOp(TestObjects.modifiedObject(id1), table(), id1);
        DbRowOp row2 = new UpdateDbRowOp(TestObjects.modifiedObject(id2), table(), id2);

        assertEquals(row1, row2);
        assertEquals(row2, row1);
    }

    @Test
    public void notEquals_NotEqualId() {
        ObjectId id1 = ObjectId.of("test", "id", 1);
        ObjectId id2 = ObjectId.of("test", "id", 2);

        DbRowOp row1 = new InsertDbRowOp(TestObjects.modifiedObject(id1), table(), id1);
        DbRowOp row2 = new InsertDbRowOp(TestObjects.modifiedObject(id2), table(), id2);

        assertNotEquals(row1, row2);
        assertNotEquals(row2, row1);
    }


    private CompiledTable table() {
        return CompiledTables.table("TEST");
    }
}