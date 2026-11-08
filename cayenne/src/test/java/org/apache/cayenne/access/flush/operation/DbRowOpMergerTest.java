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

import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.unit.CompiledTables;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DbRowOpMergerTest {

    @Test
    public void mergeUpdateDelete() {
        ObjectId id = ObjectId.of("test");

        UpdateDbRowOp row1 = new UpdateDbRowOp(mockObject(id), mockEntity(), id);
        DeleteDbRowOp row2 = new DeleteDbRowOp(mockObject(id), mockEntity(), id);

        {
            DbRowOpMerger merger = new DbRowOpMerger();
            DbRowOp row = merger.apply(row1, row2);
            assertSame(row2, row);
        }

        {
            DbRowOpMerger merger = new DbRowOpMerger();
            DbRowOp row = merger.apply(row2, row1);
            assertSame(row2, row);
        }
    }

    @Test
    public void mergeInsertDelete() {
        ObjectId id = ObjectId.of("test");

        InsertDbRowOp row1 = new InsertDbRowOp(mockObject(id), mockEntity(), id);
        DeleteDbRowOp row2 = new DeleteDbRowOp(mockObject(id), mockEntity(), id);

        {
            DbRowOpMerger merger = new DbRowOpMerger();
            DbRowOp row = merger.apply(row1, row2);
            assertSame(row2, row);
        }
    }

    @Test
    public void mergeUpdateInsert() {
        ObjectId id = ObjectId.of("test");

        UpdateDbRowOp row1 = new UpdateDbRowOp(mockObject(id), mockEntity(), id);
        InsertDbRowOp row2 = new InsertDbRowOp(mockObject(id), mockEntity(), id);

        {
            DbRowOpMerger merger = new DbRowOpMerger();
            DbRowOp row = merger.apply(row1, row2);
            assertSame(row2, row);
        }

        {
            DbRowOpMerger merger = new DbRowOpMerger();
            DbRowOp row = merger.apply(row2, row1);
            assertSame(row1, row);
        }
    }

    @Test
    public void mergeInsertInsert() {
        ObjectId id = ObjectId.of("test");

        CompiledColumn column1 = CompiledTables.column("attr1");
        CompiledColumn column2 = CompiledTables.column("attr2");

        InsertDbRowOp row1 = new InsertDbRowOp(mockObject(id), mockEntity(), id);
        row1.getValues().addValue(column1, 1, false);
        InsertDbRowOp row2 = new InsertDbRowOp(mockObject(id), mockEntity(), id);
        row2.getValues().addValue(column2, 2, false);

        {
            DbRowOpMerger merger = new DbRowOpMerger();
            DbRowOp row = merger.apply(row1, row2);
            assertSame(row2, row);
            Map<String, Object> snapshot = ((InsertDbRowOp) row).getValues().getSnapshot();
            assertEquals(2, snapshot.size());
            assertEquals(1, snapshot.get("attr1"));
            assertEquals(2, snapshot.get("attr2"));
        }

        {
            DbRowOpMerger merger = new DbRowOpMerger();
            DbRowOp row = merger.apply(row2, row1);
            assertSame(row1, row);
            Map<String, Object> snapshot = ((InsertDbRowOp) row).getValues().getSnapshot();
            assertEquals(2, snapshot.size());
            assertEquals(1, snapshot.get("attr1"));
            assertEquals(2, snapshot.get("attr2"));
        }
    }

    @Test
    public void mergeUpdateUpdate() {
        ObjectId id = ObjectId.of("test");

        CompiledColumn column1 = CompiledTables.column("attr1");
        CompiledColumn column2 = CompiledTables.column("attr2");

        UpdateDbRowOp row1 = new UpdateDbRowOp(mockObject(id), mockEntity(), id);
        row1.getValues().addValue(column1, 1, false);
        UpdateDbRowOp row2 = new UpdateDbRowOp(mockObject(id), mockEntity(), id);
        row2.getValues().addValue(column2, 2, false);

        {
            DbRowOpMerger merger = new DbRowOpMerger();
            DbRowOp row = merger.apply(row1, row2);
            assertSame(row2, row);
            Map<String, Object> snapshot = ((UpdateDbRowOp) row).getValues().getSnapshot();
            assertEquals(2, snapshot.size());
            assertEquals(1, snapshot.get("attr1"));
            assertEquals(2, snapshot.get("attr2"));
        }

        {
            DbRowOpMerger merger = new DbRowOpMerger();
            DbRowOp row = merger.apply(row2, row1);
            assertSame(row1, row);
            Map<String, Object> snapshot = ((UpdateDbRowOp) row).getValues().getSnapshot();
            assertEquals(2, snapshot.size());
            assertEquals(1, snapshot.get("attr1"));
            assertEquals(2, snapshot.get("attr2"));
        }
    }

    private Persistent mockObject(ObjectId id) {
        Persistent persistent = mock(Persistent.class);
        when(persistent.getObjectId()).thenReturn(id);
        when(persistent.getPersistenceState()).thenReturn(PersistenceState.MODIFIED);
        return persistent;
    }

    private CompiledTable mockEntity() {
        return CompiledTables.table("TEST");
    }

}