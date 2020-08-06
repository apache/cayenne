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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.remote.hessian.service.HessianUtil;
import org.junit.Test;

public class PrefetchTreeNodeTest {

	@Test
	public void testTreeSerializationWithHessian() throws Exception {
		PrefetchTreeNode n1 = new PrefetchTreeNode();
		PrefetchTreeNode n2 = n1.addPath("abc");

		PrefetchTreeNode nc1 = (PrefetchTreeNode) HessianUtil.cloneViaClientServerSerialization(n1,
				new EntityResolver());
		assertNotNull(nc1);

		PrefetchTreeNode nc2 = nc1.getNode("abc");
		assertNotNull(nc2);
		assertNotSame(nc2, n2);
		assertSame(nc1, nc2.getParent());
		assertEquals("abc", nc2.getName());
	}

	@Test
	public void testSubtreeSerializationWithHessian() throws Exception {
		PrefetchTreeNode n1 = new PrefetchTreeNode();
		PrefetchTreeNode n2 = n1.addPath("abc");
		PrefetchTreeNode n3 = n2.addPath("xyz");

		// test that substree was serialized as independent tree, instead of
		// sucking
		PrefetchTreeNode nc2 = (PrefetchTreeNode) HessianUtil.cloneViaClientServerSerialization(n2,
				new EntityResolver());
		assertNotNull(nc2);
		assertNull(nc2.getParent());

		PrefetchTreeNode nc3 = nc2.getNode("xyz");
		assertNotNull(nc3);
		assertNotSame(nc3, n3);
		assertSame(nc2, nc3.getParent());
		assertEquals("xyz", nc3.getName());
	}

	@Test
	public void testMerge() {
		PrefetchTreeNode original = new PrefetchTreeNode();
		original.addPath("a").setPhantom(true);
		original.addPath("a.b").setSemantics(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
		original.addPath("a.b").setPhantom(false);
		original.addPath("c").setSemantics(PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
		original.addPath("c").setPhantom(false);
		original.addPath("f").setSemantics(PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
		original.addPath("f").setPhantom(false);

		PrefetchTreeNode toMerge = new PrefetchTreeNode();
		toMerge.addPath("a").setPhantom(false);
		toMerge.addPath("a.b").setSemantics(PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);
		toMerge.addPath("d.e").setSemantics(PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
		toMerge.addPath("d.e").setPhantom(false);
		toMerge.addPath("c").setSemantics(PrefetchTreeNode.UNDEFINED_SEMANTICS);

		original.merge(toMerge);

		assertSame(original, original.getRoot());
		assertEquals(4, original.getChildren().size());

		PrefetchTreeNode mergedA = original.getChild("a");
		assertEquals(1, mergedA.getChildren().size());
		assertFalse("Phantom flag wasn't turned off", mergedA.isPhantom());
		assertEquals(PrefetchTreeNode.UNDEFINED_SEMANTICS, mergedA.getSemantics());

		PrefetchTreeNode mergedB = mergedA.getChild("b");
		assertEquals(0, mergedB.getChildren().size());
		assertFalse(mergedB.isPhantom());
		assertEquals("Semantics was't merged", PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS,
				mergedB.getSemantics());

		PrefetchTreeNode mergedC = original.getChild("c");
		assertEquals(0, mergedC.getChildren().size());
		assertFalse(mergedC.isPhantom());
		assertEquals("Semantics was overridden to undefined", PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS,
				mergedC.getSemantics());

		PrefetchTreeNode mergedD = original.getChild("d");
		assertEquals(1, mergedD.getChildren().size());
		assertTrue(mergedD.isPhantom());
		assertEquals(PrefetchTreeNode.UNDEFINED_SEMANTICS, mergedD.getSemantics());
		assertNotSame("Merged node wasn't cloned", toMerge.getChild("d"), mergedD);

		PrefetchTreeNode mergedE = mergedD.getChild("e");
		assertEquals(0, mergedE.getChildren().size());
		assertFalse(mergedE.isPhantom());
		assertEquals(PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS, mergedE.getSemantics());

		PrefetchTreeNode mergedF = original.getChild("f");
		assertEquals(0, mergedF.getChildren().size());
		assertFalse(mergedF.isPhantom());
		assertEquals(PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS, mergedF.getSemantics());
	}

	@Test
	public void testMerge_NonRoot() {
		PrefetchTreeNode original = new PrefetchTreeNode();
		original.addPath("a").setPhantom(true);
		original.addPath("a.b").setSemantics(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
		original.addPath("a.b").setPhantom(false);

		PrefetchTreeNode toMerge = new PrefetchTreeNode(null, "a.b.c");
		toMerge.setPhantom(false);
		toMerge.setSemantics(PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);

		original.merge(toMerge);

		assertSame(original, original.getRoot());
		assertEquals(1, original.getChildren().size());

		PrefetchTreeNode mergedA = original.getChild("a");
		assertEquals(1, mergedA.getChildren().size());
		assertTrue(mergedA.isPhantom());
		assertEquals(PrefetchTreeNode.UNDEFINED_SEMANTICS, mergedA.getSemantics());

		PrefetchTreeNode mergedB = mergedA.getChild("b");
		assertEquals(1, mergedB.getChildren().size());
		assertFalse(mergedB.isPhantom());
		assertEquals(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS, mergedB.getSemantics());

		PrefetchTreeNode mergedC = mergedB.getChild("c");
		assertEquals(0, mergedC.getChildren().size());
		assertFalse(mergedC.isPhantom());
		assertEquals(PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS, mergedC.getSemantics());
	}
}
