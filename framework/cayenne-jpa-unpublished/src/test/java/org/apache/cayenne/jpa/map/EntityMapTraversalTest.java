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
package org.apache.cayenne.jpa.map;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;

import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.util.HierarchicalTreeVisitor;
import org.apache.cayenne.util.TraversalUtil;

/**
 * Tests traversal annotations.
 * 
 */
public class EntityMapTraversalTest extends TestCase {

    public void testTraversal() {
        JpaEntityMap map = new JpaEntityMap();

        JpaEntity e1 = new JpaEntity();
        map.getEntities().add(e1);

        JpaBasic a1 = new JpaBasic();
        e1.getAttributes().getBasicAttributes().add(a1);

        JpaId a2 = new JpaId();
        e1.getAttributes().getIds().add(a2);

        JpaColumn c1 = new JpaColumn();
        a1.setColumn(c1);

        JpaColumn c2 = new JpaColumn();
        a2.setColumn(c2);

        JpaMappedSuperclass m1 = new JpaMappedSuperclass();
        map.getMappedSuperclasses().add(m1);

        TestVisitor visitor = new TestVisitor();
        TraversalUtil.traverse(map, visitor);

        assertTrue(visitor.isVisited(map));
        assertTrue(visitor.isVisited(e1));
        assertTrue(visitor.isVisited(a1));
        assertTrue(visitor.isVisited(a2));
        assertTrue(visitor.isVisited(c1));
        assertTrue(visitor.isVisited(c2));
        assertTrue(visitor.isVisited(m1));
    }

    class TestVisitor implements HierarchicalTreeVisitor {

        protected Collection<Object> visitedNodes = new ArrayList<Object>();

        public HierarchicalTreeVisitor childVisitor(ProjectPath path, Class<?> childType) {
            return this;
        }

        public boolean isVisited(Object node) {
            return visitedNodes.contains(node);
        }

        public void onFinishNode(ProjectPath path) {
            visitedNodes.add(path.getObject());
        }

        public boolean onStartNode(ProjectPath path) {
            return true;
        }
    }
}
