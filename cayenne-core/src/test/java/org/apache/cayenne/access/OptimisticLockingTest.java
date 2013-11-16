/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.locking.RelLockingTestEntity;
import org.apache.cayenne.testdo.locking.SimpleLockingTestEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.LOCKING_PROJECT)
public class OptimisticLockingTest extends ServerCase {

    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tSimpleLockingTest;
    protected TableHelper tRelLockingTest;
    protected TableHelper tLockingHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("LOCKING_HELPER");
        dbHelper.deleteAll("REL_LOCKING_TEST");
        dbHelper.deleteAll("SIMPLE_LOCKING_TEST");

        tSimpleLockingTest = new TableHelper(dbHelper, "SIMPLE_LOCKING_TEST");
        tSimpleLockingTest.setColumns("LOCKING_TEST_ID", "NAME", "DESCRIPTION")
                .setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.VARCHAR);

        tRelLockingTest = new TableHelper(dbHelper, "REL_LOCKING_TEST");
        tRelLockingTest.setColumns(
                "REL_LOCKING_TEST_ID",
                "SIMPLE_LOCKING_TEST_ID",
                "NAME");

        tLockingHelper = new TableHelper(dbHelper, "LOCKING_HELPER");
        tLockingHelper.setColumns("LOCKING_HELPER_ID", "REL_LOCKING_TEST_ID", "NAME");
    }

    protected void createSimpleLockingDataSet() throws Exception {
        tLockingHelper.delete().execute();
        tRelLockingTest.delete().execute();
        tSimpleLockingTest.delete().execute();
        tSimpleLockingTest.insert(1, "LockTest1", null);
    }

    protected void createLockingOnNullDataSet() throws Exception {
        tLockingHelper.delete().execute();
        tRelLockingTest.delete().execute();
        tSimpleLockingTest.delete().execute();
        tSimpleLockingTest.insert(1, null, null);
    }

    protected void createLockingOnMixedDataSet() throws Exception {
        tLockingHelper.delete().execute();
        tRelLockingTest.delete().execute();
        tSimpleLockingTest.delete().execute();
        tSimpleLockingTest.insert(1, null, null);
        tSimpleLockingTest.insert(2, "LockTest2", null);
        tSimpleLockingTest.insert(3, "LockTest3", "Another Lock Test");
    }

    protected void createLockingOnToOneDataSet() throws Exception {
        tLockingHelper.delete().execute();
        tRelLockingTest.delete().execute();
        tSimpleLockingTest.delete().execute();
        tSimpleLockingTest.insert(1, "LockTest1", null);
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

    public void testSuccessSimpleLockingOnDelete() throws Exception {
        createSimpleLockingDataSet();

        List<?> allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.deleteObjects(object);
        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnDeleteFollowedByInvalidate() throws Exception {
        createSimpleLockingDataSet();

        List<?> allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.deleteObjects(object);
        context.invalidateObjects(object);
        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnDeleteFollowedByForgetSnapshot()
            throws Exception {
        createSimpleLockingDataSet();

        List<?> allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.deleteObjects(object);
        context.getObjectStore().getDataRowCache().forgetSnapshot(object.getObjectId());
        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnDeletePrecededByInvalidate() throws Exception {
        createSimpleLockingDataSet();

        List<?> allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.invalidateObjects(object);
        context.deleteObjects(object);
        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnDeletePrecededByForgetSnapshot()
            throws Exception {
        createSimpleLockingDataSet();

        List<?> allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.getObjectStore().getDataRowCache().forgetSnapshot(object.getObjectId());
        context.deleteObjects(object);
        context.commitChanges();
    }

    public void testFailSimpleLockingOnDelete() throws Exception {
        createSimpleLockingDataSet();

        List<?> allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("second update");
        context.commitChanges();

        // change row underneath, delete and save... optimistic lock failure expected
        createSimpleLockUpdate();

        context.deleteObjects(object);

        try {
            context.commitChanges();
            fail("Optimistic lock failure expected.");
        }
        catch (OptimisticLockException ex) {
            // optimistic lock failure expected...
        }
    }

    public void testSuccessSimpleLockingOnUpdate() throws Exception {
        createSimpleLockingDataSet();

        List<?> allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        object.setDescription("second update");

        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnUpdateFollowedByInvalidate() throws Exception {
        createSimpleLockingDataSet();

        List<?> allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        object.setDescription("second update");
        context.invalidateObjects(object);

        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnUpdatePrecededByInvalidate() throws Exception {
        createSimpleLockingDataSet();

        List<?> allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.invalidateObjects(object);
        object.setDescription("second update");

        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnUpdateFollowedByForgetSnapshot()
            throws Exception {
        createSimpleLockingDataSet();

        List<?> allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        object.setDescription("second update");
        context.getObjectStore().getDataRowCache().forgetSnapshot(object.getObjectId());

        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnUpdatePrecededByForgetSnapshot()
            throws Exception {
        createSimpleLockingDataSet();

        List<?> allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.getObjectStore().getDataRowCache().forgetSnapshot(object.getObjectId());
        object.setDescription("second update");

        context.commitChanges();
    }

    public void testFailSimpleLocking() throws Exception {
        createSimpleLockingDataSet();

        List<?> allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        // change row underneath, change description and save... optimistic lock failure
        // expected
        createSimpleLockUpdate();

        object.setDescription("second update");

        try {
            context.commitChanges();
            fail("Optimistic lock failure expected.");
        }
        catch (OptimisticLockException ex) {
            // optimistic lock failure expected...
        }
    }

    public void testFailLockingOnNull() throws Exception {
        createLockingOnNullDataSet();

        List<?> allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected...
        object.setDescription("first update");
        context.commitChanges();

        // change row underneath, change description and save... optimistic lock failure
        // expected
        createSimpleLockUpdate();

        object.setDescription("second update");

        try {
            context.commitChanges();
            fail("Optimistic lock failure expected.");
        }
        catch (OptimisticLockException ex) {
            // optimistic lock failure expected...
            assertEquals(object.getObjectId(), ex.getFailedObjectId());
        }
    }

    public void testSuccessLockingOnMixed() throws Exception {
        createLockingOnMixedDataSet();

        SelectQuery query = new SelectQuery(SimpleLockingTestEntity.class);
        query.addOrdering(new Ordering("db:LOCKING_TEST_ID", SortOrder.ASCENDING));

        List<?> allObjects = context.performQuery(query);
        assertEquals(3, allObjects.size());

        SimpleLockingTestEntity object1 = (SimpleLockingTestEntity) allObjects.get(0);
        SimpleLockingTestEntity object2 = (SimpleLockingTestEntity) allObjects.get(1);
        SimpleLockingTestEntity object3 = (SimpleLockingTestEntity) allObjects.get(2);

        // change description and save... no optimistic lock failure expected...
        object1.setDescription("first update for object1");
        object2.setDescription("first update for object2");
        object3.setName("object3 - new name");
        context.commitChanges();

        // TODO: it would be nice to pick inside DataContext to see that 3 batches where
        // generated...
        // this requires refactoring of ContextCommit.
    }

    public void testFailLockingOnToOne() throws Exception {
        createLockingOnToOneDataSet();

        List<?> allObjects = context.performQuery(new SelectQuery(
                RelLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        RelLockingTestEntity object = (RelLockingTestEntity) allObjects.get(0);

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

        try {
            context.commitChanges();
            fail("Optimistic lock failure expected.");
        }
        catch (OptimisticLockException ex) {
            // optimistic lock failure expected...
        }
    }

    public void testFailRetrieveRow() throws Exception {
        createSimpleLockingDataSet();

        List<?> allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);
        object.setDescription("first update");

        // change row underneath, change description and save... optimistic lock failure
        // expected
        createSimpleLockUpdate();

        try {
            context.commitChanges();
            fail("Optimistic lock failure expected.");
        }
        catch (OptimisticLockException ex) {
            Map<?, ?> freshFailedRow = ex.getFreshSnapshot(context);
            assertNotNull(freshFailedRow);
            assertEquals("LockTest1Updated", freshFailedRow.get("NAME"));
        }
    }

    public void testFailRetrieveDeletedRow() throws Exception {
        createSimpleLockingDataSet();

        List<?> allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        object.setDescription("first update");

        // delete row underneath, change description and save... optimistic lock failure
        // expected
        createSimpleLockDelete();

        try {
            context.commitChanges();
            fail("Optimistic lock failure expected.");
        }
        catch (OptimisticLockException ex) {
            Map<?, ?> freshFailedRow = ex.getFreshSnapshot(context);
            assertNull("" + freshFailedRow, freshFailedRow);
        }
    }
}
