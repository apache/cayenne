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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.cayenne.DeleteDenyException;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.ObjectDiff.ArcOperation;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.graph.NodeDiff;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.relationship.DeleteRuleFlatA;
import org.apache.cayenne.testdo.relationship.DeleteRuleFlatB;
import org.apache.cayenne.testdo.relationship.DeleteRuleTest1;
import org.apache.cayenne.testdo.relationship.DeleteRuleTest2;
import org.apache.cayenne.testdo.relationship.DeleteRuleTest3;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.RELATIONSHIPS_PROJECT)
public class DeleteRulesTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("DELETE_RULE_TEST3");
        dbHelper.deleteAll("DELETE_RULE_TEST1");
        dbHelper.deleteAll("DELETE_RULE_TEST2");
        dbHelper.deleteAll("DELETE_RULE_JOIN");
        dbHelper.deleteAll("DELETE_RULE_FLATB");
        dbHelper.deleteAll("DELETE_RULE_FLATA");
    }

    public void testDenyToOne() {

        DeleteRuleTest1 test1 = context.newObject(DeleteRuleTest1.class);
        DeleteRuleTest2 test2 = context.newObject(DeleteRuleTest2.class);
        test1.setTest2(test2);
        context.commitChanges();

        try {
            context.deleteObjects(test1);
            fail("Should have thrown an exception");
        } catch (Exception e) {
            // GOOD!
        }
        context.commitChanges();

    }

    public void testNoActionToOne() {
        DeleteRuleTest2 test2 = context.newObject(DeleteRuleTest2.class);
        DeleteRuleTest3 test3 = context.newObject(DeleteRuleTest3.class);
        test3.setToDeleteRuleTest2(test2);
        context.commitChanges();

        // must go on without exceptions...
        context.deleteObjects(test3);
        context.commitChanges();
    }

    public void testNoActionToMany() {
        DeleteRuleTest2 test2 = context.newObject(DeleteRuleTest2.class);
        DeleteRuleTest3 test3 = context.newObject(DeleteRuleTest3.class);
        test3.setToDeleteRuleTest2(test2);
        context.commitChanges();

        // must go on without exceptions...
        context.deleteObjects(test2);

        // don't commit, since this will cause a constraint exception
    }

    public void testNoActionFlattened() {
        // temporarily set delete rule to NOACTION...
        int oldRule = changeDeleteRule(DeleteRule.NO_ACTION);

        try {
            DeleteRuleFlatA a = context.newObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = context.newObject(DeleteRuleFlatB.class);

            a.addToFlatB(b);
            context.commitChanges();

            // must go on without exceptions...
            context.deleteObjects(a);

            // assert that join is deleted
            assertJoinDeleted(a, b);
            assertEquals(PersistenceState.DELETED, a.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, b.getPersistenceState());
            assertTrue(b.getUntitledRel().contains(a));
            context.commitChanges();

        } finally {
            changeDeleteRule(oldRule);
        }
    }

    public void testNoActionFlattenedNoReverse() {
        // temporarily set delete rule to NOACTION...
        int oldRule = changeDeleteRule(DeleteRule.NO_ACTION);
        ObjRelationship reverse = unsetReverse();

        try {
            DeleteRuleFlatA a = context.newObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = context.newObject(DeleteRuleFlatB.class);

            a.addToFlatB(b);
            context.commitChanges();

            // must go on without exceptions...
            context.deleteObjects(a);

            // assert that join is deleted
            assertJoinDeleted(a, b);
            assertEquals(PersistenceState.DELETED, a.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, b.getPersistenceState());
            context.commitChanges();
        } finally {
            changeDeleteRule(oldRule);
            restoreReverse(reverse);
        }
    }

    public void testCascadeFlattened() {
        // temporarily set delete rule to CASCADE...
        int oldRule = changeDeleteRule(DeleteRule.CASCADE);

        try {
            DeleteRuleFlatA a = context.newObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = context.newObject(DeleteRuleFlatB.class);
            a.addToFlatB(b);
            context.commitChanges();

            // must go on without exceptions...
            context.deleteObjects(a);

            // assert that join is deleted
            assertJoinDeleted(a, b);
            context.commitChanges();

            assertEquals(PersistenceState.TRANSIENT, a.getPersistenceState());
            assertEquals(PersistenceState.TRANSIENT, b.getPersistenceState());
        } finally {
            changeDeleteRule(oldRule);
        }
    }

    public void testCascadeFlattenedNoReverse() {
        // temporarily set delete rule to CASCADE...
        int oldRule = changeDeleteRule(DeleteRule.CASCADE);
        ObjRelationship reverse = unsetReverse();

        try {
            DeleteRuleFlatA a = context.newObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = context.newObject(DeleteRuleFlatB.class);
            a.addToFlatB(b);
            context.commitChanges();

            // must go on without exceptions...
            context.deleteObjects(a);

            // assert that join is deleted
            assertJoinDeleted(a, b);
            context.commitChanges();
            assertEquals(PersistenceState.TRANSIENT, a.getPersistenceState());
            assertEquals(PersistenceState.TRANSIENT, b.getPersistenceState());
        } finally {
            changeDeleteRule(oldRule);
            restoreReverse(reverse);
        }
    }

    public void testNullifyFlattened() {
        // temporarily set delete rule to NULLIFY...
        int oldRule = changeDeleteRule(DeleteRule.NULLIFY);

        try {
            DeleteRuleFlatA a = context.newObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = context.newObject(DeleteRuleFlatB.class);
            a.addToFlatB(b);
            context.commitChanges();

            // must go on without exceptions...
            context.deleteObjects(a);

            // assert that join is deleted
            assertJoinDeleted(a, b);
            assertEquals(PersistenceState.DELETED, a.getPersistenceState());
            assertEquals(PersistenceState.MODIFIED, b.getPersistenceState());
            assertFalse(b.getUntitledRel().contains(a));
            context.commitChanges();
        } finally {
            changeDeleteRule(oldRule);
        }
    }

    public void testNullifyFlattenedNoReverse() {
        // temporarily set delete rule to NULLIFY...
        int oldRule = changeDeleteRule(DeleteRule.NULLIFY);
        ObjRelationship reverse = unsetReverse();

        try {
            DeleteRuleFlatA a = context.newObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = context.newObject(DeleteRuleFlatB.class);
            a.addToFlatB(b);
            context.commitChanges();

            // must go on without exceptions...
            context.deleteObjects(a);

            // assert that join is deleted
            assertJoinDeleted(a, b);
            assertEquals(PersistenceState.DELETED, a.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, b.getPersistenceState());
            context.commitChanges();
        } finally {
            changeDeleteRule(oldRule);
            restoreReverse(reverse);
        }
    }

    public void testDenyFlattened() {
        // temporarily set delete rule to DENY...
        int oldRule = changeDeleteRule(DeleteRule.DENY);

        try {
            DeleteRuleFlatA a = context.newObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = context.newObject(DeleteRuleFlatB.class);
            a.addToFlatB(b);
            context.commitChanges();

            try {
                context.deleteObjects(a);
                fail("Must have thrown a deny exception..");
            } catch (DeleteDenyException ex) {
                // expected... but check further
                assertJoinNotDeleted(a, b);
            }
        } finally {
            changeDeleteRule(oldRule);
        }
    }

    public void testDenyFlattenedNoReverse() {
        // temporarily set delete rule to DENY...
        int oldRule = changeDeleteRule(DeleteRule.DENY);
        ObjRelationship reverse = unsetReverse();

        try {
            DeleteRuleFlatA a = context.newObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = context.newObject(DeleteRuleFlatB.class);
            a.addToFlatB(b);
            context.commitChanges();

            try {
                context.deleteObjects(a);
                fail("Must have thrown a deny exception..");
            } catch (DeleteDenyException ex) {
                // expected... but check further
                assertJoinNotDeleted(a, b);
            }
        } finally {
            changeDeleteRule(oldRule);
            restoreReverse(reverse);
        }
    }

    private int changeDeleteRule(int deleteRule) {
        ObjEntity entity = context.getEntityResolver().getObjEntity(DeleteRuleFlatA.class);

        ObjRelationship relationship = entity.getRelationship(DeleteRuleFlatA.FLAT_B_PROPERTY);
        int oldRule = relationship.getDeleteRule();
        relationship.setDeleteRule(deleteRule);
        return oldRule;
    }

    private ObjRelationship unsetReverse() {
        ObjEntity entity = context.getEntityResolver().getObjEntity(DeleteRuleFlatA.class);

        ObjRelationship relationship = entity.getRelationship(DeleteRuleFlatA.FLAT_B_PROPERTY);
        ObjRelationship reverse = relationship.getReverseRelationship();

        if (reverse != null) {
            reverse.getSourceEntity().removeRelationship(reverse.getName());
            context.getEntityResolver().getClassDescriptorMap().removeDescriptor("DeleteRuleFlatA");
            context.getEntityResolver().getClassDescriptorMap().removeDescriptor("DeleteRuleFlatB");
        }

        return reverse;
    }

    private void restoreReverse(ObjRelationship reverse) {
        ObjEntity entity = context.getEntityResolver().getObjEntity(DeleteRuleFlatA.class);

        ObjRelationship relationship = entity.getRelationship(DeleteRuleFlatA.FLAT_B_PROPERTY);
        relationship.getTargetEntity().addRelationship(reverse);
        context.getEntityResolver().getClassDescriptorMap().removeDescriptor("DeleteRuleFlatA");
        context.getEntityResolver().getClassDescriptorMap().removeDescriptor("DeleteRuleFlatB");
    }

    private void assertJoinDeleted(DeleteRuleFlatA a, DeleteRuleFlatB b) {

        ObjectDiff changes = context.getObjectStore().changes.get(a.getObjectId());

        assertNotNull(changes);
        Collection<NodeDiff> diffs = new ArrayList<NodeDiff>();
        changes.appendDiffs(diffs);
        Iterator<?> it = diffs.iterator();
        while (it.hasNext()) {
            Object diff = it.next();
            if (diff instanceof ArcOperation) {
                ArcOperation arcDelete = (ArcOperation) diff;
                if (arcDelete.getNodeId().equals(a.getObjectId())
                        && arcDelete.getTargetNodeId().equals(b.getObjectId())
                        && arcDelete.getArcId().equals(DeleteRuleFlatA.FLAT_B_PROPERTY) && arcDelete.isDelete()) {
                    return;
                }
            }
        }

        fail("Join was not deleted for flattened relationship");
    }

    private void assertJoinNotDeleted(DeleteRuleFlatA a, DeleteRuleFlatB b) {
        ObjectDiff changes = context.getObjectStore().changes.get(a.getObjectId());

        if (changes != null) {
            Collection<NodeDiff> diffs = new ArrayList<NodeDiff>();
            changes.appendDiffs(diffs);
            Iterator<?> it = diffs.iterator();
            while (it.hasNext()) {
                Object diff = it.next();
                if (diff instanceof ArcOperation) {
                    ArcOperation arcDelete = (ArcOperation) diff;
                    if (arcDelete.getNodeId().equals(a.getObjectId())
                            && arcDelete.getTargetNodeId().equals(b.getObjectId())
                            && arcDelete.getArcId().equals(DeleteRuleFlatA.FLAT_B_PROPERTY) && !arcDelete.isDelete()) {
                        fail("Join was  deleted for flattened relationship");
                    }
                }
            }
        }
    }
}
