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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.locking.RelLockingTestEntity;
import org.apache.cayenne.testdo.locking.SimpleLockingTestEntity;
import org.apache.cayenne.unit.LockingCase;

/**
 * @author Andrus Adamchik
 */
public class OptimisticLockingTest extends LockingCase {

    protected DataContext context;

    @Override
    protected void setUp() throws Exception {
        context = createDataContext();
    }

    public void testSuccessSimpleLockingOnDelete() throws Exception {
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.deleteObject(object);
        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnDeleteFollowedByInvalidate() throws Exception {
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.deleteObject(object);
        context.invalidateObjects(Collections.singletonList(object));
        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnDeleteFollowedByForgetSnapshot()
            throws Exception {
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.deleteObject(object);
        context.getObjectStore().getDataRowCache().forgetSnapshot(object.getObjectId());
        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnDeletePrecededByInvalidate() throws Exception {
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.invalidateObjects(Collections.singletonList(object));
        context.deleteObject(object);
        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnDeletePrecededByForgetSnapshot()
            throws Exception {
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.getObjectStore().getDataRowCache().forgetSnapshot(object.getObjectId());
        context.deleteObject(object);
        context.commitChanges();
    }

    public void testFailSimpleLockingOnDelete() throws Exception {
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("second update");
        context.commitChanges();

        // change row underneath, delete and save... optimistic lock failure expected
        createTestData("SimpleLockUpdate");

        context.deleteObject(object);

        try {
            context.commitChanges();
            fail("Optimistic lock failure expected.");
        }
        catch (OptimisticLockException ex) {
            // optimistic lock failure expected...
        }
    }

    public void testSuccessSimpleLockingOnUpdate() throws Exception {
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(
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
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        object.setDescription("second update");
        context.invalidateObjects(Collections.singletonList(object));

        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnUpdatePrecededByInvalidate() throws Exception {
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.invalidateObjects(Collections.singletonList(object));
        object.setDescription("second update");

        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnUpdateFollowedByForgetSnapshot()
            throws Exception {
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(
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
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(
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
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        // change row underneath, change description and save... optimistic lock failure
        // expected
        createTestData("SimpleLockUpdate");
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
        createTestData("testLockingOnNull");

        List allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        // change description and save... no optimistic lock failure expected...
        object.setDescription("first update");
        context.commitChanges();

        // change row underneath, change description and save... optimistic lock failure
        // expected
        createTestData("SimpleLockUpdate");
        object.setDescription("second update");

        try {
            context.commitChanges();
            fail("Optimistic lock failure expected.");
        }
        catch (OptimisticLockException ex) {
            // optimistic lock failure expected...
        }
    }

    public void testSuccessLockingOnMixed() throws Exception {
        createTestData("testLockingOnMixed");
        SelectQuery query = new SelectQuery(SimpleLockingTestEntity.class);
        query.addOrdering(new Ordering("db:LOCKING_TEST_ID", Ordering.ASC));

        List allObjects = context.performQuery(query);
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
        createTestData("testLockingOnToOne");

        List allObjects = context
                .performQuery(new SelectQuery(RelLockingTestEntity.class));
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
        createTestData("RelLockUpdate");
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
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);
        object.setDescription("first update");

        // change row underneath, change description and save... optimistic lock failure
        // expected
        createTestData("SimpleLockUpdate");

        try {
            context.commitChanges();
            fail("Optimistic lock failure expected.");
        }
        catch (OptimisticLockException ex) {
            Map freshFailedRow = ex.getFreshSnapshot(context);
            assertNotNull(freshFailedRow);
            assertEquals("LockTest1Updated", freshFailedRow.get("NAME"));
        }
    }

    public void testFailRetrieveDeletedRow() throws Exception {
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(
                SimpleLockingTestEntity.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTestEntity object = (SimpleLockingTestEntity) allObjects.get(0);

        object.setDescription("first update");

        // delete row underneath, change description and save... optimistic lock failure
        // expected
        createTestData("SimpleLockDelete");

        try {
            context.commitChanges();
            fail("Optimistic lock failure expected.");
        }
        catch (OptimisticLockException ex) {
            Map freshFailedRow = ex.getFreshSnapshot(context);
            assertNull(freshFailedRow);
        }
    }
}
