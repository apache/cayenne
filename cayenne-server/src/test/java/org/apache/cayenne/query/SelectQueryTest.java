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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.ExpressionParameter;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.Before;
import org.junit.Test;

public class SelectQueryTest {

	private SelectQuery<?> q;

	@Before
	public void before() {
		this.q = new SelectQuery<Object>();
	}

	@Test
	public void testAddPrefetch() {

		assertNull(q.getPrefetchTree());
		q.addPrefetch("a.b.c");
		assertNotNull(q.getPrefetchTree());
		assertEquals(1, q.getPrefetchTree().nonPhantomNodes().size());
		assertNotNull(q.getPrefetchTree().getNode("a.b.c"));
	}

	@Test
	public void testAddPrefetchDuplicates() {

		q.addPrefetch("a.b.c");
		q.addPrefetch("a.b.c");

		assertEquals(1, q.getPrefetchTree().nonPhantomNodes().size());
	}

	@Test
	public void testClearPrefetches() {

		q.addPrefetch("abc");
		q.addPrefetch("xyz");
		assertNotNull(q.getPrefetchTree());

		q.clearPrefetches();
		assertNull(q.getPrefetchTree());
	}

	@Test
	public void testPageSize() throws Exception {
		q.setPageSize(10);
		assertEquals(10, q.getPageSize());
	}

	@Test
	public void testAddOrdering1() throws Exception {
		Ordering ord = new Ordering();
		q.addOrdering(ord);
		assertEquals(1, q.getOrderings().size());
		assertSame(ord, q.getOrderings().get(0));
	}

	@Test
	public void testAddOrdering2() throws Exception {
		String path = "a.b.c";
		q.addOrdering(path, SortOrder.DESCENDING);
		assertEquals(1, q.getOrderings().size());

		Ordering ord = q.getOrderings().get(0);
		assertEquals(path, ord.getSortSpec().getOperand(0));
		assertEquals(false, ord.isAscending());
	}

	@Test
	public void testDistinct() throws Exception {
		assertFalse(q.isDistinct());
		q.setDistinct(true);
		assertTrue(q.isDistinct());
	}

	@Test
	public void testQueryWithParams1() {
		q.setRoot(Artist.class);
		q.setDistinct(true);

		SelectQuery<?> q1 = q.queryWithParameters(new HashMap<String, Object>(), true);
		assertSame(q.getRoot(), q1.getRoot());
		assertEquals(q.isDistinct(), q1.isDistinct());
		assertNull(q1.getQualifier());
	}

	@Test
	public void testQueryWithParams2() throws Exception {
		q.setRoot(Artist.class);

		List<Expression> list = new ArrayList<Expression>();
		list.add(ExpressionFactory.matchExp("k1", new ExpressionParameter("test1")));
		list.add(ExpressionFactory.matchExp("k2", new ExpressionParameter("test2")));
		list.add(ExpressionFactory.matchExp("k3", new ExpressionParameter("test3")));
		list.add(ExpressionFactory.matchExp("k4", new ExpressionParameter("test4")));
		q.setQualifier(ExpressionFactory.joinExp(Expression.OR, list));

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("test2", "abc");
		params.put("test3", "xyz");
		SelectQuery<?> q1 = q.queryWithParameters(params, true);
		assertSame(q.getRoot(), q1.getRoot());
		assertNotNull(q1.getQualifier());
		assertTrue(q1.getQualifier() != q.getQualifier());
	}

	@Test
	public void testQueryWithParamsSkipName() {
		q.setRoot(Artist.class);
		q.setDistinct(true);
		q.setName("name");

		SelectQuery<?> q1 = q.queryWithParameters(Collections.<String, Object> emptyMap());
		assertEquals("name", q.getName());
		assertNull(q1.getName());
	}
}
