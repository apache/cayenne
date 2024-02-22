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

package org.apache.cayenne.query;

import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.util.Util;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PrefetchTreeNodeTest {

    @Test
    public void testAddPath() {
        PrefetchTreeNode tree = new PrefetchTreeNode();
        tree.addPath("abc");
        tree.addPath("abc.def.mnk");
        tree.addPath("xyz");

        assertTrue(tree.isPhantom());

        PrefetchTreeNode n1 = tree.getNode("abc");
        assertNotNull(n1);
        assertTrue(n1.isPhantom());
        assertEquals("abc", n1.getName());

        PrefetchTreeNode n2 = tree.getNode("abc.def");
        assertNotNull(n2);
        assertTrue(n2.isPhantom());
        assertEquals("def", n2.getName());

        PrefetchTreeNode n3 = tree.getNode("abc.def.mnk");
        assertNotNull(n3);
        assertTrue(n3.isPhantom());
        assertEquals("mnk", n3.getName());

        PrefetchTreeNode n4 = tree.getNode("xyz");
        assertNotNull(n4);
        assertTrue(n4.isPhantom());
        assertEquals("xyz", n4.getName());
    }

    @Test
    public void testGetPath() {
        PrefetchTreeNode tree = new PrefetchTreeNode();
        tree.addPath("abc");
        tree.addPath("abc.def.mnk");
        tree.addPath("xyz");

        assertEquals(CayennePath.of(""), tree.getPath());

        PrefetchTreeNode n1 = tree.getNode("abc");
        assertEquals(CayennePath.of("abc"), n1.getPath());

        PrefetchTreeNode n2 = tree.getNode("abc.def");
        assertEquals(CayennePath.of("abc.def"), n2.getPath());

        PrefetchTreeNode n3 = tree.getNode("abc.def.mnk");
        assertEquals(CayennePath.of("abc.def.mnk"), n3.getPath());

        PrefetchTreeNode n4 = tree.getNode("xyz");
        assertEquals(CayennePath.of("xyz"), n4.getPath());
    }

    @Test
    public void testTreeSerialization() throws Exception {
        PrefetchTreeNode n1 = new PrefetchTreeNode();
        PrefetchTreeNode n2 = n1.addPath("abc");

        PrefetchTreeNode nc1 = (PrefetchTreeNode) Util.cloneViaSerialization(n1);
        assertNotNull(nc1);

        PrefetchTreeNode nc2 = nc1.getNode("abc");
        assertNotNull(nc2);
        assertNotSame(nc2, n2);
        assertSame(nc1, nc2.getParent());
        assertEquals("abc", nc2.getName());
    }

    @Test
    public void testSubtreeSerialization() throws Exception {
        PrefetchTreeNode n1 = new PrefetchTreeNode();
        PrefetchTreeNode n2 = n1.addPath("abc");
        PrefetchTreeNode n3 = n2.addPath("xyz");

        // test that substree was serialized as independent tree, instead of sucking
        PrefetchTreeNode nc2 = (PrefetchTreeNode) Util.cloneViaSerialization(n2);
        assertNotNull(nc2);
        assertNull(nc2.getParent());

        PrefetchTreeNode nc3 = nc2.getNode("xyz");
        assertNotNull(nc3);
        assertNotSame(nc3, n3);
        assertSame(nc2, nc3.getParent());
        assertEquals("xyz", nc3.getName());
    }

    @Test
    public void testCloneJointSubtree() throws Exception {
        PrefetchTreeNode root = new PrefetchTreeNode(null, "root");
        root.setPhantom(false);
        PrefetchTreeNode joint1 = root.addPath("joint1");
        joint1.setPhantom(false);
        joint1.setSemantics(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
        PrefetchTreeNode joint2 = root.addPath("joint2");
        joint2.setPhantom(false);
        joint2.setSemantics(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
        PrefetchTreeNode disjoint = joint1.addPath("disjoint1");
        disjoint.setPhantom(false);
        disjoint.setSemantics(PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);

        PrefetchTreeNode cloned = root.cloneJointSubtree();
        assertEquals("root", cloned.getName());

        assertEquals(2, cloned.getChildren().size());

        PrefetchTreeNode joint1Clone = cloned.getChild("joint1");
        assertNotNull(joint1Clone);
        assertEquals(CayennePath.of("joint1"), joint1.getPath());
        assertEquals(0, joint1Clone.getChildren().size());

        PrefetchTreeNode joint2Clone = cloned.getChild("joint2");
        assertNotNull(joint2Clone);
        assertEquals(CayennePath.of("joint2"), joint2.getPath());
        assertEquals(0, joint2Clone.getChildren().size());

    }
}
