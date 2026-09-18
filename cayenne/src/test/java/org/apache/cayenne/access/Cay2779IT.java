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

import org.apache.cayenne.access.ObjectDiff.ArcOperation;
import org.apache.cayenne.graph.NodeDiff;
import org.apache.cayenne.testdo.relationships_many_to_many_join.NodeA;
import org.apache.cayenne.testdo.relationships_many_to_many_join.NodeB;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the "Nullify" delete rule on a self-referencing many-to-many mapped over a single table inheritance
 * hierarchy. The base entity "Node" maps "parents" to itself, while the complementary "children" is declared by
 * the "NodeA" sub-entity, so the reverse of "parents" is not reachable from its declared target entity.
 */
public class Cay2779IT {

    @RegisterExtension
    static final CayenneTestsEnv env =
            CayenneTestsEnv.forProject(CayenneProjects.RELATIONSHIPS_MANY_TO_MANY_JOIN_PROJECT);

    private NodeA newNodeA(String name) {
        NodeA node = env.context().newObject(NodeA.class);
        node.setName(name);
        return node;
    }

    private int joinRowCount() throws SQLException {
        return env.table("X_NODE_JOIN").getRowCount();
    }

    @Test
    public void nullifyOnDeleteOfChildUpdatesParent() throws SQLException {

        NodeA parent = newNodeA("parent");
        NodeA child = newNodeA("child");
        child.addToParents(parent);
        env.context().commitChanges();

        // "addToParents" does not populate the parent's "children" in memory, as the reverse of "parents" is not
        // reachable from its declared target entity, so re-read both objects to start from the committed state
        env.context().invalidateObjects(parent, child);

        assertEquals(1, joinRowCount());
        assertTrue(parent.getChildren().contains(child));

        env.context().deleteObjects(child);

        // CAY-2779: the parent used to keep the deleted child in its collection, as the reverse arc of "parents"
        // is declared by a sub-entity of the declared target and so resolved to null
        assertFalse(parent.getChildren().contains(child));

        env.context().commitChanges();
        assertEquals(0, joinRowCount());
    }

    @Test
    public void nullifyRecordsArcDeletedGraphOp() {

        NodeA parent = newNodeA("parent");
        NodeA child = newNodeA("child");
        child.addToParents(parent);
        env.context().commitChanges();
        env.context().invalidateObjects(parent, child);

        assertTrue(parent.getChildren().contains(child));
        env.context().deleteObjects(child);

        // the other half of CAY-2779 - without a recorded arc operation any listener flow depending on it
        // silently does nothing
        ObjectDiff changes = env.context().getObjectStore().changes.get(parent.getObjectId());
        assertNotNull(changes);

        Collection<NodeDiff> diffs = new ArrayList<>();
        changes.appendDiffs(diffs);

        boolean arcDeleted = diffs.stream().anyMatch(diff -> diff instanceof ArcOperation arcOp
                && arcOp.getNodeId().equals(parent.getObjectId())
                && arcOp.getTargetNodeId().equals(child.getObjectId())
                && arcOp.getArcId().getForwardArc().equals("children")
                && arcOp.isDelete());
        assertTrue(arcDeleted, "No 'arcDeleted' operation recorded for the sub-entity reverse relationship");
    }

    @Test
    public void deleteChildWithSubEntityParentNotMappingReverse() throws SQLException {

        NodeA mappingParent = newNodeA("mappingParent");

        // "NodeB" maps no "children" relationship at all, so it has no reverse arc to unset
        NodeB plainParent = env.context().newObject(NodeB.class);
        plainParent.setName("plainParent");

        NodeA child = newNodeA("child");
        child.addToParents(mappingParent);
        child.addToParents(plainParent);
        env.context().commitChanges();
        env.context().invalidateObjects(mappingParent, plainParent, child);

        assertEquals(2, joinRowCount());
        assertTrue(mappingParent.getChildren().contains(child));

        env.context().deleteObjects(child);

        assertFalse(mappingParent.getChildren().contains(child));

        env.context().commitChanges();
        assertEquals(0, joinRowCount());
    }

    @Test
    public void deleteParentNullifiesChildSide() throws SQLException {

        NodeA parent = newNodeA("parent");
        NodeA child = newNodeA("child");
        parent.addToChildren(child);
        env.context().commitChanges();

        // the reverse of "children" is reachable from its declared target entity, so this direction always worked
        assertEquals(1, joinRowCount());
        assertTrue(child.getParents().contains(parent));

        env.context().deleteObjects(parent);

        assertFalse(child.getParents().contains(parent));

        env.context().commitChanges();
        assertEquals(0, joinRowCount());
    }

    @Test
    public void relationshipsAreSetOnBothSides() throws SQLException {

        NodeA parent = newNodeA("parent");
        NodeA child = newNodeA("child");

        // sets the reverse "parents" too, since it is reachable from the declared target entity
        parent.addToChildren(child);
        env.context().commitChanges();

        assertEquals(1, joinRowCount());
        assertTrue(child.getParents().contains(parent));
    }
}
