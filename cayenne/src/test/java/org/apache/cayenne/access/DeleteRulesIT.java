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
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class DeleteRulesIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.RELATIONSHIPS_DELETE_RULES_PROJECT);

    @Test
    public void denyToOne() {

        DeleteRuleTest1 test1 = env.dataContext().newObject(DeleteRuleTest1.class);
        DeleteRuleTest2 test2 = env.dataContext().newObject(DeleteRuleTest2.class);
        test1.setTest2(test2);
        env.dataContext().commitChanges();

        assertThrows(Exception.class, () -> env.dataContext().deleteObjects(test1));
        env.dataContext().commitChanges();

    }

    @Test
    public void noActionToOne() {
        DeleteRuleTest2 test2 = env.dataContext().newObject(DeleteRuleTest2.class);
        DeleteRuleTest3 test3 = env.dataContext().newObject(DeleteRuleTest3.class);
        test3.setToDeleteRuleTest2(test2);
        env.dataContext().commitChanges();

        // must go on without exceptions...
        env.dataContext().deleteObjects(test3);
        env.dataContext().commitChanges();
    }

    @Test
    public void noActionToMany() {
        DeleteRuleTest2 test2 = env.dataContext().newObject(DeleteRuleTest2.class);
        DeleteRuleTest3 test3 = env.dataContext().newObject(DeleteRuleTest3.class);
        test3.setToDeleteRuleTest2(test2);
        env.dataContext().commitChanges();

        // must go on without exceptions...
        env.dataContext().deleteObjects(test2);

        // don't commit, since this will cause a constraint exception
    }

    @Test
    public void noActionFlattened() {
        // temporarily set delete rule to NOACTION...
        int oldRule = changeDeleteRule(DeleteRule.NO_ACTION);

        try {
            DeleteRuleFlatA a = env.dataContext().newObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = env.dataContext().newObject(DeleteRuleFlatB.class);

            a.addToFlatB(b);
            env.dataContext().commitChanges();

            // must go on without exceptions...
            env.dataContext().deleteObjects(a);

            // assert that join is deleted
            assertJoinDeleted(a, b);
            assertEquals(PersistenceState.DELETED, a.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, b.getPersistenceState());
            assertTrue(b.getUntitledRel().contains(a));
            env.dataContext().commitChanges();

        } finally {
            changeDeleteRule(oldRule);
        }
    }

    @Test
    public void noActionFlattenedNoReverse() {
        // temporarily set delete rule to NOACTION...
        int oldRule = changeDeleteRule(DeleteRule.NO_ACTION);
        ObjRelationship reverse = unsetReverse();

        try {
            DeleteRuleFlatA a = env.dataContext().newObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = env.dataContext().newObject(DeleteRuleFlatB.class);

            a.addToFlatB(b);
            env.dataContext().commitChanges();

            // must go on without exceptions...
            env.dataContext().deleteObjects(a);

            // assert that join is deleted
            assertJoinDeleted(a, b);
            assertEquals(PersistenceState.DELETED, a.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, b.getPersistenceState());
            env.dataContext().commitChanges();
        } finally {
            changeDeleteRule(oldRule);
            restoreReverse(reverse);
        }
    }

    @Test
    public void cascadeFlattened() {
        // temporarily set delete rule to CASCADE...
        int oldRule = changeDeleteRule(DeleteRule.CASCADE);

        try {
            DeleteRuleFlatA a = env.dataContext().newObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = env.dataContext().newObject(DeleteRuleFlatB.class);
            a.addToFlatB(b);
            env.dataContext().commitChanges();

            // must go on without exceptions...
            env.dataContext().deleteObjects(a);

            // assert that join is deleted
            assertJoinDeleted(a, b);
            env.dataContext().commitChanges();

            assertEquals(PersistenceState.TRANSIENT, a.getPersistenceState());
            assertEquals(PersistenceState.TRANSIENT, b.getPersistenceState());
        } finally {
            changeDeleteRule(oldRule);
        }
    }

    @Test
    public void cascadeFlattenedNoReverse() {
        // temporarily set delete rule to CASCADE...
        int oldRule = changeDeleteRule(DeleteRule.CASCADE);
        ObjRelationship reverse = unsetReverse();

        try {
            DeleteRuleFlatA a = env.dataContext().newObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = env.dataContext().newObject(DeleteRuleFlatB.class);
            a.addToFlatB(b);
            env.dataContext().commitChanges();

            // must go on without exceptions...
            env.dataContext().deleteObjects(a);

            // assert that join is deleted
            assertJoinDeleted(a, b);
            env.dataContext().commitChanges();
            assertEquals(PersistenceState.TRANSIENT, a.getPersistenceState());
            assertEquals(PersistenceState.TRANSIENT, b.getPersistenceState());
        } finally {
            changeDeleteRule(oldRule);
            restoreReverse(reverse);
        }
    }

    @Test
    public void nullifyFlattened() {
        // temporarily set delete rule to NULLIFY...
        int oldRule = changeDeleteRule(DeleteRule.NULLIFY);

        try {
            DeleteRuleFlatA a = env.dataContext().newObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = env.dataContext().newObject(DeleteRuleFlatB.class);
            a.addToFlatB(b);
            env.dataContext().commitChanges();

            // must go on without exceptions...
            env.dataContext().deleteObjects(a);

            // assert that join is deleted
            assertJoinDeleted(a, b);
            assertEquals(PersistenceState.DELETED, a.getPersistenceState());
            assertEquals(PersistenceState.MODIFIED, b.getPersistenceState());
            assertFalse(b.getUntitledRel().contains(a));
            env.dataContext().commitChanges();
        } finally {
            changeDeleteRule(oldRule);
        }
    }

    @Test
    public void nullifyFlattenedNoReverse() {
        // temporarily set delete rule to NULLIFY...
        int oldRule = changeDeleteRule(DeleteRule.NULLIFY);
        ObjRelationship reverse = unsetReverse();

        try {
            DeleteRuleFlatA a = env.dataContext().newObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = env.dataContext().newObject(DeleteRuleFlatB.class);
            a.addToFlatB(b);
            env.dataContext().commitChanges();

            // must go on without exceptions...
            env.dataContext().deleteObjects(a);

            // assert that join is deleted
            assertJoinDeleted(a, b);
            assertEquals(PersistenceState.DELETED, a.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, b.getPersistenceState());
            env.dataContext().commitChanges();
        } finally {
            changeDeleteRule(oldRule);
            restoreReverse(reverse);
        }
    }

    @Test
    public void denyFlattened() {
        // temporarily set delete rule to DENY...
        int oldRule = changeDeleteRule(DeleteRule.DENY);

        try {
            DeleteRuleFlatA a = env.dataContext().newObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = env.dataContext().newObject(DeleteRuleFlatB.class);
            a.addToFlatB(b);
            env.dataContext().commitChanges();

            assertThrows(DeleteDenyException.class, () -> env.dataContext().deleteObjects(a));
            assertJoinNotDeleted(a, b);
        } finally {
            changeDeleteRule(oldRule);
        }
    }

    @Test
    public void denyFlattenedNoReverse() {
        // temporarily set delete rule to DENY...
        int oldRule = changeDeleteRule(DeleteRule.DENY);
        ObjRelationship reverse = unsetReverse();

        try {
            DeleteRuleFlatA a = env.dataContext().newObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = env.dataContext().newObject(DeleteRuleFlatB.class);
            a.addToFlatB(b);
            env.dataContext().commitChanges();

            assertThrows(DeleteDenyException.class, () -> env.dataContext().deleteObjects(a));
            assertJoinNotDeleted(a, b);
        } finally {
            changeDeleteRule(oldRule);
            restoreReverse(reverse);
        }
    }

    private int changeDeleteRule(int deleteRule) {
        ObjEntity entity = env.dataContext().getEntityResolver().getObjEntity(DeleteRuleFlatA.class);

        ObjRelationship relationship = entity.getRelationship(DeleteRuleFlatA.FLAT_B.getName());
        int oldRule = relationship.getDeleteRule();
        relationship.setDeleteRule(deleteRule);
        return oldRule;
    }

    private ObjRelationship unsetReverse() {
        ObjEntity entity = env.dataContext().getEntityResolver().getObjEntity(DeleteRuleFlatA.class);

        ObjRelationship relationship = entity.getRelationship(DeleteRuleFlatA.FLAT_B.getName());
        ObjRelationship reverse = relationship.getReverseRelationship();

        if (reverse != null) {
            reverse.getSourceEntity().removeRelationship(reverse.getName());
            env.dataContext().getEntityResolver().getClassDescriptorMap().removeDescriptor("DeleteRuleFlatA");
            env.dataContext().getEntityResolver().getClassDescriptorMap().removeDescriptor("DeleteRuleFlatB");
        }

        return reverse;
    }

    private void restoreReverse(ObjRelationship reverse) {
        ObjEntity entity = env.dataContext().getEntityResolver().getObjEntity(DeleteRuleFlatA.class);

        ObjRelationship relationship = entity.getRelationship(DeleteRuleFlatA.FLAT_B.getName());
        relationship.getTargetEntity().addRelationship(reverse);
        env.dataContext().getEntityResolver().getClassDescriptorMap().removeDescriptor("DeleteRuleFlatA");
        env.dataContext().getEntityResolver().getClassDescriptorMap().removeDescriptor("DeleteRuleFlatB");
    }

    private void assertJoinDeleted(DeleteRuleFlatA a, DeleteRuleFlatB b) {

        ObjectDiff changes = env.dataContext().getObjectStore().changes.get(a.getObjectId());

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
        ObjectDiff changes = env.dataContext().getObjectStore().changes.get(a.getObjectId());

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
