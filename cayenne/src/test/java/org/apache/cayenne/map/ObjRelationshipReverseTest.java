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

package org.apache.cayenne.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Pins the reverse relationship resolution for a self-referencing many-to-many mapped over a single table
 * inheritance hierarchy - the model shape of CAY-2779, where the base entity maps a relationship to itself and
 * only a sub-entity maps the complementary one.
 */
public class ObjRelationshipReverseTest {

    private ObjEntity sub;
    private ObjRelationship parents;
    private ObjRelationship children;

    @BeforeEach
    public void buildModel() {

        DataMap map = new DataMap("CAY-2779");

        DbEntity node = new DbEntity("X_NODE");
        node.addAttribute(new DbAttribute("NODE_ID"));
        map.addDbEntity(node);

        DbEntity join = new DbEntity("X_NODE_JOIN");
        join.addAttribute(new DbAttribute("PARENT_ID"));
        join.addAttribute(new DbAttribute("CHILD_ID"));
        map.addDbEntity(join);

        addDbRelationship(node, "nodeJoinParent", "X_NODE_JOIN", "NODE_ID", "PARENT_ID");
        addDbRelationship(node, "nodeJoinChild", "X_NODE_JOIN", "NODE_ID", "CHILD_ID");
        addDbRelationship(join, "nodeParent", "X_NODE", "PARENT_ID", "NODE_ID");
        addDbRelationship(join, "nodeChild", "X_NODE", "CHILD_ID", "NODE_ID");

        ObjEntity base = new ObjEntity("Node");
        base.setDbEntityName("X_NODE");
        map.addObjEntity(base);

        sub = new ObjEntity("NodeA");
        sub.setSuperEntityName("Node");
        map.addObjEntity(sub);

        // the base entity points at itself...
        parents = addObjRelationship(base, "parents", "Node", "nodeJoinChild.nodeParent");

        // ... while the complementary relationship is declared by the sub-entity only
        children = addObjRelationship(sub, "children", "Node", "nodeJoinParent.nodeChild");
    }

    private static void addDbRelationship(DbEntity source, String name, String target, String from, String to) {
        DbRelationship relationship = new DbRelationship(name);
        source.addRelationship(relationship);
        relationship.setTargetEntityName(target);
        relationship.addJoin(new DbJoin(relationship, from, to));
    }

    private static ObjRelationship addObjRelationship(ObjEntity source, String name, String target, String dbPath) {
        ObjRelationship relationship = new ObjRelationship(name);
        source.addRelationship(relationship);
        relationship.setTargetEntityName(target);
        relationship.setDbRelationshipPath(dbPath);
        return relationship;
    }

    @Test
    public void reverseDeclaredBySubEntityIsNotFound() {
        // CAY-2779: "children" is invisible here, as ObjEntity.getRelationships() only ever climbs up the
        // hierarchy. DataContextDeleteAction compensates for this when processing the "Nullify" delete rule
        assertNull(parents.getReverseRelationship());
    }

    @Test
    public void reverseDeclaredBySubEntityIsFoundWhenSearchingThatEntity() {
        assertSame(children, parents.getReverseRelationship(sub));
    }

    @Test
    public void reverseOfSubEntityRelationshipIsFound() {
        // the opposite direction resolves, since "parents" is reachable from the declared target entity (CAY-2777)
        assertSame(parents, children.getReverseRelationship());
    }
}
