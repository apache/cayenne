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
package org.apache.cayenne.query;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.Test;

public class SelectByIdTest {

	@Test
	public void testPrefetch() {

		PrefetchTreeNode root = PrefetchTreeNode.withPath("a.b", PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

		SelectById<Artist> q = SelectById.query(Artist.class, 6);
		q.prefetch(root);

		assertSame(root, q.getPrefetches());
	}

	@Test
	public void testPrefetch_Path() {

		SelectById<Artist> q = SelectById.query(Artist.class, 7);
		q.prefetch("a.b", PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
		PrefetchTreeNode root1 = q.getPrefetches();

		assertNotNull(root1);
		assertNotNull(root1.getNode("a.b"));

		q.prefetch("a.c", PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
		PrefetchTreeNode root2 = q.getPrefetches();

		assertNotNull(root2);
		assertNotNull(root2.getNode("a.c"));
		assertNull(root2.getNode("a.b"));
		assertNotSame(root1, root2);
	}

	@Test
	public void testAddPrefetch() {

		PrefetchTreeNode root = PrefetchTreeNode.withPath("a.b", PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

		SelectById<Artist> q = SelectById.query(Artist.class, 8);
		q.prefetch(root);

		assertSame(root, q.getPrefetches());

		PrefetchTreeNode subRoot = PrefetchTreeNode.withPath("a.b.c", PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
		q.addPrefetch(subRoot);

		assertSame(root, q.getPrefetches());

		assertNotNull(root.getNode("a.b.c"));
	}
}
