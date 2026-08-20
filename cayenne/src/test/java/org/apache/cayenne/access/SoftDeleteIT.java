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
package org.apache.cayenne.access;

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.configuration.runtime.CoreModule;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.soft_delete.HardDelete;
import org.apache.cayenne.testdo.soft_delete.SoftDelete;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SoftDeleteIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv
            .forProject(CayenneProjects.SOFT_DELETE_PROJECT)
            .withExtraModules(b -> CoreModule.extend(b).softDeleteIfColumnPresent("DELETED"));

    private TableHelper tSoftDelete;

    @BeforeEach
    public void setUp() {
        tSoftDelete = env.table("SOFT_DELETE", "ID", "NAME", "DELETED");
    }

    @Test
    public void softDeleteLeavesRowFlagged() throws Exception {
        DataContext context = env.context();

        SoftDelete object = context.newObject(SoftDelete.class);
        object.setName("a");
        context.commitChanges();

        assertEquals(1, tSoftDelete.getRowCount());
        assertNull(tSoftDelete.selectAll().getFirst()[2], "freshly inserted row should have NULL DELETED");

        assertEquals(1, ObjectSelect.query(SoftDelete.class).selectCount(context));

        context.deleteObjects(object);
        context.commitChanges();

        // object graph is unaware of the underlying UPDATE and treats the delete as final
        assertEquals(PersistenceState.TRANSIENT, object.getPersistenceState());

        // the row physically survives, now flagged as deleted
        assertEquals(1, tSoftDelete.getRowCount());
        List<Object[]> rows = tSoftDelete.selectAll();
        assertTrue(toBoolean(rows.getFirst()[2]), "DELETED flag should be true");

        // the entity qualifier hides the soft-deleted row
        assertEquals(0, ObjectSelect.query(SoftDelete.class).selectCount(context));
    }

    @Test
    public void batchSoftDeleteLeavesRowsFlagged() throws Exception {
        DataContext context = env.context();

        SoftDelete o1 = context.newObject(SoftDelete.class);
        o1.setName("a");
        SoftDelete o2 = context.newObject(SoftDelete.class);
        o2.setName("b");
        SoftDelete o3 = context.newObject(SoftDelete.class);
        o3.setName("c");
        context.commitChanges();

        context.deleteObjects(o1, o2, o3);
        context.commitChanges();

        assertEquals(3, tSoftDelete.getRowCount());
        for (Object[] row : tSoftDelete.selectAll()) {
            assertTrue(toBoolean(row[2]), "DELETED flag should be true");
        }

        assertEquals(0, ObjectSelect.query(SoftDelete.class).selectCount(context));
    }

    @Test
    public void softDeleteHidesOnlyDeletedRow() throws Exception {
        DataContext context = env.context();

        SoftDelete survivor = context.newObject(SoftDelete.class);
        survivor.setName("survivor");
        SoftDelete victim = context.newObject(SoftDelete.class);
        victim.setName("victim");
        context.commitChanges();

        context.deleteObjects(victim);
        context.commitChanges();

        // both rows are still physically present
        assertEquals(2, tSoftDelete.getRowCount());

        // but the qualifier hides the soft-deleted one, leaving only the survivor visible
        List<SoftDelete> visible = ObjectSelect.query(SoftDelete.class).select(context);
        assertEquals(1, visible.size());
        assertEquals("survivor", visible.getFirst().getName());
    }

    @Test
    public void hardDeleteFallbackRemovesRow() throws Exception {
        TableHelper tHardDelete = env.table("HARD_DELETE", "ID", "NAME");
        DataContext context = env.context();

        HardDelete object = context.newObject(HardDelete.class);
        object.setName("a");
        context.commitChanges();

        assertEquals(1, tHardDelete.getRowCount());

        context.deleteObjects(object);
        context.commitChanges();

        // no DELETED column -> the translator falls back to a real DELETE and the row is gone
        assertEquals(0, tHardDelete.getRowCount());
        assertEquals(0, ObjectSelect.query(HardDelete.class).selectCount(context));
    }

    @Test
    public void mixedSoftAndHardDeleteInOneCommit() throws Exception {
        TableHelper tHardDelete = env.table("HARD_DELETE", "ID", "NAME");
        DataContext context = env.context();

        SoftDelete soft = context.newObject(SoftDelete.class);
        soft.setName("soft");
        HardDelete hard = context.newObject(HardDelete.class);
        hard.setName("hard");
        context.commitChanges();

        context.deleteObjects(soft, hard);
        context.commitChanges();

        // the SoftDelete row survives, flagged as deleted, and is hidden by the qualifier
        assertEquals(1, tSoftDelete.getRowCount());
        assertTrue(toBoolean(tSoftDelete.selectAll().getFirst()[2]), "DELETED flag should be true");
        assertEquals(0, ObjectSelect.query(SoftDelete.class).selectCount(context));

        // the HardDelete row is physically gone
        assertEquals(0, tHardDelete.getRowCount());
    }

    private static boolean toBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        // NULL means "not deleted"; some databases store BOOLEAN as a numeric flag
        return value instanceof Number n && n.intValue() != 0;
    }
}
