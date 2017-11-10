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

	private SelectQuery<?> query;

	@Before
	public void before() {
		this.query = new SelectQuery<Object>();
	}

	@Test
	public void testAddPrefetch() {

		assertNull(query.getPrefetchTree());
		query.addPrefetch("a.b.c");
		assertNotNull(query.getPrefetchTree());
		assertEquals(1, query.getPrefetchTree().nonPhantomNodes().size());
		assertNotNull(query.getPrefetchTree().getNode("a.b.c"));
	}

	@Test
	public void testAddPrefetchDuplicates() {

		query.addPrefetch("a.b.c");
		query.addPrefetch("a.b.c");

		assertEquals(1, query.getPrefetchTree().nonPhantomNodes().size());
	}

	@Test
	public void testClearPrefetches() {

		query.addPrefetch("abc");
		query.addPrefetch("xyz");
		assertNotNull(query.getPrefetchTree());

		query.clearPrefetches();
		assertNull(query.getPrefetchTree());
	}

	@Test
	public void testPageSize() throws Exception {
		query.setPageSize(10);
		assertEquals(10, query.getPageSize());
	}

	@Test
	public void testAddOrdering1() throws Exception {
		Ordering ord = new Ordering();
		query.addOrdering(ord);
		assertEquals(1, query.getOrderings().size());
		assertSame(ord, query.getOrderings().get(0));
	}

	@Test
	public void testAddOrdering2() throws Exception {
		String path = "a.b.c";
		query.addOrdering(path, SortOrder.DESCENDING);
		assertEquals(1, query.getOrderings().size());

		Ordering ord = query.getOrderings().get(0);
		assertEquals(path, ord.getSortSpec().getOperand(0));
		assertEquals(false, ord.isAscending());
	}

	@Test
	public void testDistinct() throws Exception {
		assertFalse(query.isDistinct());
		query.setDistinct(true);
		assertTrue(query.isDistinct());
	}

	@Test
	public void testQueryWithParams1() {
		query.setRoot(Artist.class);
		query.setDistinct(true);

		SelectQuery<?> q1 = query.queryWithParameters(new HashMap<String, Object>(), true);
		assertSame(query.getRoot(), q1.getRoot());
		assertEquals(query.isDistinct(), q1.isDistinct());
		assertNull(q1.getQualifier());
	}

	@Test
	public void testQueryWithParams2() throws Exception {
		query.setRoot(Artist.class);

		List<Expression> list = new ArrayList<Expression>();
		list.add(ExpressionFactory.matchExp("k1", new ExpressionParameter("test1")));
		list.add(ExpressionFactory.matchExp("k2", new ExpressionParameter("test2")));
		list.add(ExpressionFactory.matchExp("k3", new ExpressionParameter("test3")));
		list.add(ExpressionFactory.matchExp("k4", new ExpressionParameter("test4")));
		query.setQualifier(ExpressionFactory.joinExp(Expression.OR, list));

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("test2", "abc");
		params.put("test3", "xyz");
		SelectQuery<?> q1 = query.queryWithParameters(params, true);
		assertSame(query.getRoot(), q1.getRoot());
		assertNotNull(q1.getQualifier());
		assertTrue(q1.getQualifier() != query.getQualifier());
	}
	
	@Test
	public void testAndQualifier() {
		assertNull(query.getQualifier());

		Expression e1 = ExpressionFactory.expressionOfType(Expression.EQUAL_TO);
		query.andQualifier(e1);
		assertSame(e1, query.getQualifier());

		Expression e2 = ExpressionFactory.expressionOfType(Expression.NOT_EQUAL_TO);
		query.andQualifier(e2);
		assertEquals(Expression.AND, query.getQualifier().getType());
	}

	@Test
	public void testOrQualifier() {
		assertNull(query.getQualifier());

		Expression e1 = ExpressionFactory.expressionOfType(Expression.EQUAL_TO);
		query.orQualifier(e1);
		assertSame(e1, query.getQualifier());

		Expression e2 = ExpressionFactory.expressionOfType(Expression.NOT_EQUAL_TO);
		query.orQualifier(e2);
		assertEquals(Expression.OR, query.getQualifier().getType());
	}
	
	@Test
	public void testSetQualifier() {
		assertNull(query.getQualifier());

		Expression qual = ExpressionFactory.expressionOfType(Expression.AND);
		query.setQualifier(qual);
		assertNotNull(query.getQualifier());
		assertSame(qual, query.getQualifier());
	}
}
