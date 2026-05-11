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

import java.util.Collections;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SelectByIdTest {

	@Test
	public void prefetch() {

		PrefetchTreeNode root = PrefetchTreeNode.withPath("a.b", PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

		SelectById<Artist> q = SelectById.queryId(Artist.class, 6);
		q.prefetch(root);

		PrefetchTreeNode prefetch = q.getPrefetches();

		assertNotNull(prefetch);
		assertNotNull(prefetch.getNode("a.b"));
	}

	@Test
	public void prefetch_Path() {

		SelectById<Artist> q = SelectById.queryId(Artist.class, 7);
		q.prefetch("a.b", PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
		PrefetchTreeNode prefetch = q.getPrefetches();

		assertNotNull(prefetch);
		assertNotNull(prefetch.getNode("a.b"));

		q.prefetch("a.c", PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
		prefetch = q.getPrefetches();

		assertNotNull(prefetch);
		assertNotNull(prefetch.getNode("a.c"));
		assertNotNull(prefetch.getNode("a.b"));
	}

	@Test
	public void prefetch_Subroot() {

		PrefetchTreeNode root = PrefetchTreeNode.withPath("a.b", PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

		SelectById<Artist> q = SelectById.queryId(Artist.class, 8);
		q.prefetch(root);

		PrefetchTreeNode prefetch = q.getPrefetches();

		assertNotNull(prefetch.getNode("a.b"));

		PrefetchTreeNode subRoot = PrefetchTreeNode.withPath("a.b.c", PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
		q.prefetch(subRoot);

		prefetch = q.getPrefetches();

		assertNotNull(prefetch.getNode("a.b"));
		assertNotNull(prefetch.getNode("a.b.c"));
	}

	@Test
	public void queryId_NullId() {
		assertNotNull(SelectById.queryId(Artist.class, null));
	}

	@Test
	public void dataRowQueryId_NullId() {
		assertNotNull(SelectById.dataRowQueryId(Artist.class, null));
	}

	@Test
	public void dataRowQueryIds_EmptyVarargs() {
		assertNotNull(SelectById.dataRowQueryIds(Artist.class));
	}

	@Test
	public void dataRowQueryIdsCollection_EmptyCollection() {
		assertNotNull(SelectById.dataRowQueryIdsCollection(Artist.class, Collections.emptyList()));
	}

	@Test
	public void dataRowQueryMap_EmptyMap() {
		assertThrows(CayenneRuntimeException.class,
				() -> SelectById.dataRowQueryMap(Artist.class, Collections.emptyMap()));
	}

	@Test
	public void dataRowQueryMaps_EmptyVarargs() {
		assertNotNull(SelectById.dataRowQueryMaps(Artist.class));
	}

	@Test
	public void dataRowQueryMapsCollection_EmptyCollection() {
		assertNotNull(SelectById.dataRowQueryMapsCollection(Artist.class, Collections.emptyList()));
	}
}
