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

import org.apache.cayenne.DeleteDenyException;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.ObjectDiff.ArcOperation;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.graph.NodeDiff;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.testdo.relationships_delete_rules.DeleteRuleFlatA;
import org.apache.cayenne.testdo.relationships_delete_rules.DeleteRuleFlatB;
import org.apache.cayenne.testdo.relationships_delete_rules.DeleteRuleTest1;
import org.apache.cayenne.testdo.relationships_delete_rules.DeleteRuleTest2;
import org.apache.cayenne.testdo.relationships_delete_rules.DeleteRuleTest3;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@UseCayenneRuntime(CayenneProjects.RELATIONSHIPS_DELETE_RULES_PROJECT)
public class DeleteRulesIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Test
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

    @Test
    public void testNoActionToOne() {
        DeleteRuleTest2 test2 = context.newObject(DeleteRuleTest2.class);
        DeleteRuleTest3 test3 = context.newObject(DeleteRuleTest3.class);
        test3.setToDeleteRuleTest2(test2);
        context.commitChanges();

        // must go on without exceptions...
        context.deleteObjects(test3);
        context.commitChanges();
    }

    @Test
    public void testNoActionToMany() {
        DeleteRuleTest2 test2 = context.newObject(DeleteRuleTest2.class);
        DeleteRuleTest3 test3 = context.newObject(DeleteRuleTest3.class);
        test3.setToDeleteRuleTest2(test2);
        context.commitChanges();

        // must go on without exceptions...
        context.deleteObjects(test2);

        // don't commit, since this will cause a constraint exception
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

        ObjRelationship relationship = entity.getRelationship(DeleteRuleFlatA.FLAT_B.getName());
        int oldRule = relationship.getDeleteRule();
        relationship.setDeleteRule(deleteRule);
        return oldRule;
    }

    private ObjRelationship unsetReverse() {
        ObjEntity entity = context.getEntityResolver().getObjEntity(DeleteRuleFlatA.class);

        ObjRelationship relationship = entity.getRelationship(DeleteRuleFlatA.FLAT_B.getName());
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

        ObjRelationship relationship = entity.getRelationship(DeleteRuleFlatA.FLAT_B.getName());
        relationship.getTargetEntity().addRelationship(reverse);
        context.getEntityResolver().getClassDescriptorMap().removeDescriptor("DeleteRuleFlatA");
        context.getEntityResolver().getClassDescriptorMap().removeDescriptor("DeleteRuleFlatB");
    }

    private void assertJoinDeleted(DeleteRuleFlatA a, DeleteRuleFlatB b) {

        ObjectDiff changes = context.getObjectStore().changes.get(a.getObjectId());

        assertNotNull(changes);
        Collection<NodeDiff> diffs = new ArrayList<>();
        changes.appendDiffs(diffs);
        for (Object diff : diffs) {
            if (diff instanceof ArcOperation) {
                ArcOperation arcDelete = (ArcOperation) diff;
                if (arcDelete.getNodeId().equals(a.getObjectId())
                        && arcDelete.getTargetNodeId().equals(b.getObjectId())
                        && arcDelete.getArcId().getForwardArc().equals(DeleteRuleFlatA.FLAT_B.getName())
                        && arcDelete.isDelete()) {
                    return;
                }
            }
        }

        fail("Join was not deleted for flattened relationship");
    }

    private void assertJoinNotDeleted(DeleteRuleFlatA a, DeleteRuleFlatB b) {
        ObjectDiff changes = context.getObjectStore().changes.get(a.getObjectId());

        if (changes != null) {
            Collection<NodeDiff> diffs = new ArrayList<>();
            changes.appendDiffs(diffs);
            for (Object diff : diffs) {
                if (diff instanceof ArcOperation) {
                    ArcOperation arcDelete = (ArcOperation) diff;
                    if (arcDelete.getNodeId().equals(a.getObjectId())
                            && arcDelete.getTargetNodeId().equals(b.getObjectId())
                            && arcDelete.getArcId().getForwardArc().equals(DeleteRuleFlatA.FLAT_B.getName())
                            && !arcDelete.isDelete()) {
                        fail("Join was  deleted for flattened relationship");
                    }
                }
            }
        }
    }
}
