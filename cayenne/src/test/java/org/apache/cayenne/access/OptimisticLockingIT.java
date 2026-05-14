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

import java.sql.Types;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.locking.RelLockingTestEntity;
import org.apache.cayenne.testdo.locking.SimpleLockingTestEntity;
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OptimisticLockingIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.LOCKING_PROJECT);

    protected DataContext context;

    protected TableHelper tSimpleLockingTest;
    protected TableHelper tRelLockingTest;
    protected TableHelper tLockingHelper;

    @BeforeEach
    public void setUp() throws Exception {
        context = env.context();
        tSimpleLockingTest = env.table("SIMPLE_LOCKING_TEST").setColumns("LOCKING_TEST_ID", "NAME", "DESCRIPTION", "INT_COLUMN_NOTNULL")
                .setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.INTEGER);

        tRelLockingTest = env.table("REL_LOCKING_TEST", "REL_LOCKING_TEST_ID",
                "SIMPLE_LOCKING_TEST_ID",
                "NAME");

        tLockingHelper = env.table("LOCKING_HELPER", "LOCKING_HELPER_ID", "REL_LOCKING_TEST_ID", "NAME");
    }

    protected void createSimpleLockingDataSet() throws Exception {
        tSimpleLockingTest.insert(1, "LockTest1", null, 1);
    }

    protected void createLockingOnNullDataSet() throws Exception {
        tSimpleLockingTest.insert(1, null, null, 0);
    }

    protected void createLockingOnMixedDataSet() throws Exception {
        tSimpleLockingTest.insert(1, null, null, 1);
        tSimpleLockingTest.insert(2, "LockTest2", null, 2);
        tSimpleLockingTest.insert(3, "LockTest3", "Another Lock Test", 3);
    }

    protected void createLockingOnToOneDataSet() throws Exception {
        tSimpleLockingTest.insert(1, "LockTest1", null, 2);
        tRelLockingTest.insert(5, 1, "Rel Test 1");
        tLockingHelper.insert(1, 5, "Locking Helper 1");
    }

    protected void createSimpleLockUpdate() throws Exception {
        assertEquals(1, tSimpleLockingTest
                .update()
                .set("NAME", "LockTest1Updated")
                .where("LOCKING_TEST_ID", 1)
                .execute());
    }

    protected void createRelLockUpdate() throws Exception {
        tRelLockingTest.update().set("SIMPLE_LOCKING_TEST_ID", 1).where(
                "REL_LOCKING_TEST_ID",
                5).execute();
    }

    protected void createSimpleLockDelete() throws Exception {
        tSimpleLockingTest.delete().execute();
    }

    @Test
    public void successSimpleLockingOnDelete() throws Exception {
        createSimpleLockingDataSet();

        List<SimpleLockingTestEntity> allObjects = ObjectSelect
                .query(SimpleLockingTestEntity.class)
                .select(context);
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.deleteObjects(object);
        context.commitChanges();
    }

    @Test
    public void successSimpleLockingOnDeleteFollowedByInvalidate() throws Exception {
        createSimpleLockingDataSet();

        List<SimpleLockingTestEntity> allObjects = ObjectSelect
                .query(SimpleLockingTestEntity.class)
                .select(context);
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.deleteObjects(object);
        context.invalidateObjects(object);
        context.commitChanges();
    }

    @Test
    public void successSimpleLockingOnDeleteFollowedByForgetSnapshot()
            throws Exception {
        createSimpleLockingDataSet();

        List<SimpleLockingTestEntity> allObjects = ObjectSelect
                .query(SimpleLockingTestEntity.class)
                .select(context);
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.deleteObjects(object);
        context.getObjectStore().getDataRowCache().forgetSnapshot(object.getObjectId());
        context.commitChanges();
    }

    @Test
    public void successSimpleLockingOnDeletePrecededByInvalidate() throws Exception {
        createSimpleLockingDataSet();

        List<SimpleLockingTestEntity> allObjects = ObjectSelect
                .query(SimpleLockingTestEntity.class)
                .select(context);
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.invalidateObjects(object);
        context.deleteObjects(object);
        context.commitChanges();
    }

    @Test
    public void successSimpleLockingOnDeletePrecededByForgetSnapshot()
            throws Exception {
        createSimpleLockingDataSet();

        List<SimpleLockingTestEntity> allObjects = ObjectSelect
                .query(SimpleLockingTestEntity.class)
                .select(context);
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.getObjectStore().getDataRowCache().forgetSnapshot(object.getObjectId());
        context.deleteObjects(object);
        context.commitChanges();
    }

    @Test
    public void failSimpleLockingOnDelete() throws Exception {
        createSimpleLockingDataSet();

        List<SimpleLockingTestEntity> allObjects = ObjectSelect
                .query(SimpleLockingTestEntity.class)
                .select(context);
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("second update");
        context.commitChanges();

        // change row underneath, delete and save... optimistic lock failure expected
        createSimpleLockUpdate();

        context.deleteObjects(object);

        assertThrows(OptimisticLockException.class, context::commitChanges);
    }

    @Test
    public void successSimpleLockingOnUpdate() throws Exception {
        createSimpleLockingDataSet();

        List<SimpleLockingTestEntity> allObjects = ObjectSelect
                .query(SimpleLockingTestEntity.class)
                .select(context);
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        object.setDescription("second update");

        context.commitChanges();
    }

    @Test
    public void successSimpleLockingNullablePrimitiveColumn() throws Exception {
        createSimpleLockingDataSet();

        SimpleLockingTestEntity object = ObjectSelect.query(SimpleLockingTestEntity.class).selectOne(context);

        // we should have NULL value in primitive column
        assertEquals(0, object.getIntColumnNull());
        assertNull(object.readPropertyDirectly("intColumnNull"));

        // change object and save... no optimistic lock failure expected
        object.setDescription("first update");
        object.setIntColumnNotnull(2);
        context.commitChanges();

        // update values once more
        object.setDescription("second update");
        object.setIntColumnNull(3);
        context.commitChanges();
    }

    @Test
    public void successSimpleLockingOnUpdateFollowedByInvalidate() throws Exception {
        createSimpleLockingDataSet();

        List<SimpleLockingTestEntity> allObjects = ObjectSelect
                .query(SimpleLockingTestEntity.class)
                .select(context);
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        object.setDescription("second update");
        context.invalidateObjects(object);

        context.commitChanges();
    }

    @Test
    public void successSimpleLockingOnUpdatePrecededByInvalidate() throws Exception {
        createSimpleLockingDataSet();

        List<SimpleLockingTestEntity> allObjects = ObjectSelect
                .query(SimpleLockingTestEntity.class)
                .select(context);
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.invalidateObjects(object);
        object.setDescription("second update");

        context.commitChanges();
    }

    @Test
    public void successSimpleLockingOnUpdateFollowedByForgetSnapshot()
            throws Exception {
        createSimpleLockingDataSet();

        List<SimpleLockingTestEntity> allObjects = ObjectSelect
                .query(SimpleLockingTestEntity.class)
                .select(context);
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        object.setDescription("second update");
        context.getObjectStore().getDataRowCache().forgetSnapshot(object.getObjectId());

        context.commitChanges();
    }

    @Test
    public void successSimpleLockingOnUpdatePrecededByForgetSnapshot()
            throws Exception {
        createSimpleLockingDataSet();

        List<SimpleLockingTestEntity> allObjects = ObjectSelect
                .query(SimpleLockingTestEntity.class)
                .select(context);
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.getObjectStore().getDataRowCache().forgetSnapshot(object.getObjectId());
        object.setDescription("second update");

        context.commitChanges();
    }

    @Test
    public void failSimpleLocking() throws Exception {
        createSimpleLockingDataSet();

        List<SimpleLockingTestEntity> allObjects = ObjectSelect
                .query(SimpleLockingTestEntity.class)
                .select(context);
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        // change row underneath, change description and save... optimistic lock failure
        // expected
        createSimpleLockUpdate();

        object.setDescription("second update");

        assertThrows(OptimisticLockException.class, context::commitChanges);
    }

    @Test
    public void failLockingOnNull() throws Exception {
        createLockingOnNullDataSet();

        List<SimpleLockingTestEntity> allObjects = ObjectSelect
                .query(SimpleLockingTestEntity.class)
                .select(context);
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = allObjects.get(0);

        // change description and save... no optimistic lock failure expected...
        object.setDescription("first update");
        context.commitChanges();

        // change row underneath, change description and save... optimistic lock failure
        // expected
        createSimpleLockUpdate();

        object.setDescription("second update");

        OptimisticLockException ex = assertThrows(OptimisticLockException.class, context::commitChanges);
        assertEquals(object.getObjectId(), ex.getFailedObjectId());
    }

    @Test
    public void successLockingOnMixed() throws Exception {
        createLockingOnMixedDataSet();

        List<SimpleLockingTestEntity> allObjects = ObjectSelect.query(SimpleLockingTestEntity.class)
                .orderBy(new Ordering("db:LOCKING_TEST_ID", SortOrder.ASCENDING))
                .select(context);
        assertEquals(3, allObjects.size());

        SimpleLockingTestEntity object1 = allObjects.get(0);
        SimpleLockingTestEntity object2 = allObjects.get(1);
        SimpleLockingTestEntity object3 = allObjects.get(2);

        // change description and save... no optimistic lock failure expected...
        object1.setDescription("first update for object1");
        object2.setDescription("first update for object2");
        object3.setName("object3 - new name");
        context.commitChanges();

        // TODO: it would be nice to pick inside DataContext to see that 3 batches where
        // generated...
        // this requires refactoring of ContextCommit.
    }

    @Test
    public void successLockingOnToOneNull() throws Exception {
        createLockingOnToOneDataSet();

        List<RelLockingTestEntity> allObjects = ObjectSelect
                .query(RelLockingTestEntity.class)
                .select(context);
        assertEquals(1, allObjects.size());

        RelLockingTestEntity object = allObjects.get(0);

        // set to-one relationship to null and save
        SimpleLockingTestEntity object1 = object.getToSimpleLockingTest();
        object.setToSimpleLockingTest(null);
        context.commitChanges();

        // change to-one relationship to non-null and save... should lock on null value
        object.setToSimpleLockingTest(object1);
        context.commitChanges();
    }

    @Test
    public void failLockingOnToOne() throws Exception {
        createLockingOnToOneDataSet();

        List<RelLockingTestEntity> allObjects = ObjectSelect
                .query(RelLockingTestEntity.class)
                .select(context);
        assertEquals(1, allObjects.size());

        RelLockingTestEntity object = allObjects.get(0);

        // change name and save... no optimistic lock failure expected
        object.setName("first update");
        context.commitChanges();

        // change relationship and save... no optimistic lock failure expected
        SimpleLockingTestEntity object1 = context
                .newObject(SimpleLockingTestEntity.class);
        object.setToSimpleLockingTest(object1);
        context.commitChanges();

        // change row underneath, change description and save... optimistic lock failure
        // expected
        createRelLockUpdate();

        object.setName("third update");

        assertThrows(OptimisticLockException.class, context::commitChanges);
    }

    @Test
    public void failRetrieveRow() throws Exception {
        createSimpleLockingDataSet();

        List<SimpleLockingTestEntity> allObjects = ObjectSelect
                .query(SimpleLockingTestEntity.class)
                .select(context);
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = allObjects.get(0);
        object.setDescription("first update");

        // change row underneath, change description and save... optimistic lock failure
        // expected
        createSimpleLockUpdate();

        OptimisticLockException ex = assertThrows(OptimisticLockException.class, context::commitChanges);
        Map<?, ?> freshFailedRow = ex.getFreshSnapshot(context);
        assertNotNull(freshFailedRow);
        assertEquals("LockTest1Updated", freshFailedRow.get("NAME"));
    }

    @Test
    public void failRetrieveDeletedRow() throws Exception {
        createSimpleLockingDataSet();

        List<SimpleLockingTestEntity> allObjects = ObjectSelect
                .query(SimpleLockingTestEntity.class)
                .select(context);
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = allObjects.get(0);

        object.setDescription("first update");

        // delete row underneath, change description and save... optimistic lock failure
        // expected
        createSimpleLockDelete();

        OptimisticLockException ex = assertThrows(OptimisticLockException.class, context::commitChanges);
        Map<?, ?> freshFailedRow = ex.getFreshSnapshot(context);
        assertNull(freshFailedRow, "" + freshFailedRow);
    }
}
