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
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 4.2
 */
public class DbRowOpMergerTest {

    @Test
    public void testMergeUpdateDelete() {
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
    public void testMergeInsertDelete() {
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
    public void testMergeUpdateInsert() {
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
    public void testMergeInsertInsert() {
        ObjectId id = ObjectId.of("test");

        DbAttribute attr1 = new DbAttribute("attr1");
        DbAttribute attr2 = new DbAttribute("attr2");

        InsertDbRowOp row1 = new InsertDbRowOp(mockObject(id), mockEntity(), id);
        row1.getValues().addValue(attr1, 1, false);
        InsertDbRowOp row2 = new InsertDbRowOp(mockObject(id), mockEntity(), id);
        row2.getValues().addValue(attr2, 2, false);

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
    public void testMergeUpdateUpdate() {
        ObjectId id = ObjectId.of("test");

        DbAttribute attr1 = new DbAttribute("attr1");
        DbAttribute attr2 = new DbAttribute("attr2");

        UpdateDbRowOp row1 = new UpdateDbRowOp(mockObject(id), mockEntity(), id);
        row1.getValues().addValue(attr1, 1, false);
        UpdateDbRowOp row2 = new UpdateDbRowOp(mockObject(id), mockEntity(), id);
        row2.getValues().addValue(attr2, 2, false);

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