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
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.unit.CompiledTables;
import org.apache.cayenne.unit.TestObjects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BatchKeyTest {

    private static final CompiledTable TEST = CompiledTables.table("TEST");
    private static final CompiledTable OTHER = CompiledTables.table("OTHER");

    private int nextId;

    @Test
    public void insert() {
        assertTrue(insert(TEST).isSameBatch(insert(TEST)));
        assertFalse(insert(TEST).isSameBatch(insert(OTHER)));
        assertFalse(insert(TEST).isSameBatch(update(TEST, "a")));
    }

    @Test
    public void update() {
        assertTrue(update(TEST, "a", "b").isSameBatch(update(TEST, "a", "b")));
        assertFalse(update(TEST, "a").isSameBatch(update(TEST, "b")));
        assertFalse(update(TEST, "a").isSameBatch(update(TEST, "a", "b")));
        assertFalse(update(TEST, "a").isSameBatch(update(OTHER, "a")));
    }

    @Test
    public void updateColumnsInAnyOrder() {
        // the values of the rows of a batch are bound by the column names
        assertTrue(update(TEST, "a", "b").isSameBatch(update(TEST, "b", "a")));
    }

    @Test
    public void updateQualifier() {
        UpdateDbRowOp locked = update(TEST, "a");
        locked.getQualifier().addAdditionalQualifier(CompiledTables.column("v"), 1, true);
        UpdateDbRowOp lockedToo = update(TEST, "a");
        lockedToo.getQualifier().addAdditionalQualifier(CompiledTables.column("v"), 2, true);
        UpdateDbRowOp lockedNull = update(TEST, "a");
        lockedNull.getQualifier().addAdditionalQualifier(CompiledTables.column("v"), null, true);

        assertTrue(locked.isSameBatch(lockedToo));
        assertFalse(locked.isSameBatch(update(TEST, "a")));

        // "v IS NULL" is another statement than "v = ?"
        assertFalse(locked.isSameBatch(lockedNull));
    }

    @Test
    public void delete() {
        assertTrue(delete(TEST).isSameBatch(delete(TEST)));
        assertFalse(delete(TEST).isSameBatch(delete(OTHER)));
    }

    @Test
    public void softDelete() {
        assertTrue(softDelete(TEST).isSameBatch(softDelete(TEST)));

        // a soft delete is an update, so it doesn't batch with a delete either way
        assertFalse(delete(TEST).isSameBatch(softDelete(TEST)));
        assertFalse(softDelete(TEST).isSameBatch(delete(TEST)));
    }

    @Test
    public void deleteInsert() {
        DeleteInsertDbRowOp deleteInsert = new DeleteInsertDbRowOp(delete(TEST), insert(TEST));

        assertNull(deleteInsert.getBatchKey());
        assertFalse(deleteInsert.isSameBatch(new DeleteInsertDbRowOp(delete(TEST), insert(TEST))));
        assertFalse(insert(TEST).isSameBatch(deleteInsert));
    }

    @Test
    public void keyTakenOnce() {
        UpdateDbRowOp update = update(TEST, "a");
        assertSame(update.getBatchKey(), update.getBatchKey());
    }

    private Persistent object() {
        return TestObjects.modifiedObject(ObjectId.of("test", "id", nextId++));
    }

    private InsertDbRowOp insert(CompiledTable table) {
        Persistent object = object();
        return new InsertDbRowOp(object, table, object.getObjectId());
    }

    private UpdateDbRowOp update(CompiledTable table, String... columns) {
        Persistent object = object();
        UpdateDbRowOp op = new UpdateDbRowOp(object, table, object.getObjectId());
        for (String column : columns) {
            op.getValues().addValue(CompiledTables.column(column), 1, false);
        }
        return op;
    }

    private DeleteDbRowOp delete(CompiledTable table) {
        Persistent object = object();
        return new DeleteDbRowOp(object, table, object.getObjectId());
    }

    private SoftDeleteDbRowOp softDelete(CompiledTable table) {
        Persistent object = object();
        return new SoftDeleteDbRowOp(object, table, object.getObjectId(), CompiledTables.column("deleted"));
    }
}
