/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.testdo.locking.RelLockingTest;
import org.objectstyle.cayenne.testdo.locking.SimpleLockingTest;
import org.objectstyle.cayenne.unit.LockingTestCase;

/**
 * @author Andrei Adamchik
 */
public class OptimisticLockingTst extends LockingTestCase {
    protected DataContext context;

    protected void setUp() throws Exception {
        context = createDataContext();
    }

    public void testSuccessSimpleLockingOnDelete() throws Exception {
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(SimpleLockingTest.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTest object = (SimpleLockingTest) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.deleteObject(object);
        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnDeleteFollowedByInvalidate() throws Exception {
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(SimpleLockingTest.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTest object = (SimpleLockingTest) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.deleteObject(object);
        context.invalidateObjects(Collections.singletonList(object));
        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnDeleteFollowedByForgetSnapshot() throws Exception {
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(SimpleLockingTest.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTest object = (SimpleLockingTest) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.deleteObject(object);
        context.getObjectStore().getDataRowCache().forgetSnapshot(object.getObjectId());
        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnDeletePrecededByInvalidate() throws Exception {
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(SimpleLockingTest.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTest object = (SimpleLockingTest) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.invalidateObjects(Collections.singletonList(object));
        context.deleteObject(object);
        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnDeletePrecededByForgetSnapshot() throws Exception {
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(SimpleLockingTest.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTest object = (SimpleLockingTest) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.getObjectStore().getDataRowCache().forgetSnapshot(object.getObjectId());
        context.deleteObject(object);
        context.commitChanges();
    }

    public void testFailSimpleLockingOnDelete() throws Exception {
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(SimpleLockingTest.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTest object = (SimpleLockingTest) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("second update");
        context.commitChanges();

        // change row underneath, delete and save...  optimistic lock failure expected
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

        List allObjects = context.performQuery(new SelectQuery(SimpleLockingTest.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTest object = (SimpleLockingTest) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        object.setDescription("second update");

        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnUpdateFollowedByInvalidate() throws Exception {
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(SimpleLockingTest.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTest object = (SimpleLockingTest) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        object.setDescription("second update");
        context.invalidateObjects(Collections.singletonList(object));

        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnUpdatePrecededByInvalidate() throws Exception {
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(SimpleLockingTest.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTest object = (SimpleLockingTest) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.invalidateObjects(Collections.singletonList(object));
        object.setDescription("second update");

        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnUpdateFollowedByForgetSnapshot() throws Exception {
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(SimpleLockingTest.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTest object = (SimpleLockingTest) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        object.setDescription("second update");
        context.getObjectStore().getDataRowCache().forgetSnapshot(object.getObjectId());

        context.commitChanges();
    }

    public void testSuccessSimpleLockingOnUpdatePrecededByForgetSnapshot() throws Exception {
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(SimpleLockingTest.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTest object = (SimpleLockingTest) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        context.getObjectStore().getDataRowCache().forgetSnapshot(object.getObjectId());
        object.setDescription("second update");

        context.commitChanges();
    }

    public void testFailSimpleLocking() throws Exception {
        createTestData("testSimpleLocking");

        List allObjects = context.performQuery(new SelectQuery(SimpleLockingTest.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTest object = (SimpleLockingTest) allObjects.get(0);

        // change description and save... no optimistic lock failure expected
        object.setDescription("first update");
        context.commitChanges();

        // change row underneath, change description and save...  optimistic lock failure expected
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

        List allObjects = context.performQuery(new SelectQuery(SimpleLockingTest.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTest object = (SimpleLockingTest) allObjects.get(0);

        // change description and save... no optimistic lock failure expected... 
        object.setDescription("first update");
        context.commitChanges();

        // change row underneath, change description and save...  optimistic lock failure expected
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
        SelectQuery query = new SelectQuery(SimpleLockingTest.class);
        query.addOrdering(new Ordering("db:LOCKING_TEST_ID", Ordering.ASC));

        List allObjects = context.performQuery(query);
        assertEquals(3, allObjects.size());

        SimpleLockingTest object1 = (SimpleLockingTest) allObjects.get(0);
        SimpleLockingTest object2 = (SimpleLockingTest) allObjects.get(1);
        SimpleLockingTest object3 = (SimpleLockingTest) allObjects.get(2);

        // change description and save... no optimistic lock failure expected... 
        object1.setDescription("first update for object1");
        object2.setDescription("first update for object2");
        object3.setName("object3 - new name");
        context.commitChanges();

        // TODO: it would be nice to pick inside DataContext to see that 3 batches where generated...
        // this requires refactoring of ContextCommit.
    }

    public void testFailLockingOnToOne() throws Exception {
        createTestData("testLockingOnToOne");

        List allObjects = context.performQuery(new SelectQuery(RelLockingTest.class));
        assertEquals(1, allObjects.size());

        RelLockingTest object = (RelLockingTest) allObjects.get(0);

        // change name and save... no optimistic lock failure expected
        object.setName("first update");
        context.commitChanges();

        // change relationship and save... no optimistic lock failure expected
        SimpleLockingTest object1 =
            (SimpleLockingTest) context.createAndRegisterNewObject(
                SimpleLockingTest.class);
        object.setToSimpleLockingTest(object1);
        context.commitChanges();

        // change row underneath, change description and save...  optimistic lock failure expected
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

        List allObjects = context.performQuery(new SelectQuery(SimpleLockingTest.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTest object = (SimpleLockingTest) allObjects.get(0);
        object.setDescription("first update");

        // change row underneath, change description and save...  optimistic lock failure expected
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

        List allObjects = context.performQuery(new SelectQuery(SimpleLockingTest.class));
        assertEquals(1, allObjects.size());

        SimpleLockingTest object = (SimpleLockingTest) allObjects.get(0);

        object.setDescription("first update");

        // delete row underneath, change description and save...  optimistic lock failure expected
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
