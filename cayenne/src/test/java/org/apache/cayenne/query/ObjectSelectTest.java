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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.Test;

import static org.junit.Assert.*;

public class ObjectSelectTest {

	@Test
	public void testDataRowQuery() {
		ObjectSelect<DataRow> q = ObjectSelect.dataRowQuery(Artist.class);
		assertNotNull(q);
		assertTrue(q.isFetchingDataRows());

		assertEquals(Artist.class, q.getEntityType());
		assertNull(q.getEntityName());
		assertNull(q.getDbEntityName());
	}

	@Test
	public void testQuery_RootType() {
		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);
		assertNotNull(q);
		assertNull(q.getWhere());
		assertFalse(q.isFetchingDataRows());

		assertEquals(Artist.class, q.getEntityType());
		assertNull(q.getEntityName());
		assertNull(q.getDbEntityName());
	}

	@Test
	public void testQuery_RootType_WithQualifier() {
		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class, ExpressionFactory.matchExp("a", "A"));
		assertNotNull(q);
		assertEquals("a = \"A\"", q.getWhere().toString());
		assertFalse(q.isFetchingDataRows());

		assertEquals(Artist.class, q.getEntityType());
		assertNull(q.getEntityName());
		assertNull(q.getDbEntityName());
	}

	@Test
	public void testQuery_TypeAndEntity() {
		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class, "Painting");
		assertNotNull(q);
		assertFalse(q.isFetchingDataRows());

		assertNull(q.getEntityType());
		assertEquals("Painting", q.getEntityName());
		assertNull(q.getDbEntityName());
	}

	@Test
	public void testQuery_TypeAndDbEntity() {
		ObjectSelect<DataRow> q = ObjectSelect.dbQuery("PAINTING");
		assertNotNull(q);
		assertTrue(q.isFetchingDataRows());

		assertNull(q.getEntityType());
		assertNull(q.getEntityName());
		assertEquals("PAINTING", q.getDbEntityName());
	}

	@Test
	public void testWhere() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		q.where(ExpressionFactory.matchExp("a", 3));
		assertEquals("a = 3", q.getWhere().toString());

		q.where(ExpressionFactory.matchExp("b", 4));
		assertEquals("(a = 3) and (b = 4)", q.getWhere().toString());
	}

	@Test
	public void testAnd_Array() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		q.where(ExpressionFactory.matchExp("a", 3));
		assertEquals("a = 3", q.getWhere().toString());

		q.and(ExpressionFactory.matchExp("b", 4), ExpressionFactory.greaterExp("c", 5));
		assertEquals("(a = 3) and (b = 4) and (c > 5)", q.getWhere().toString());
	}

	@Test
	public void testAnd_Collection() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		q.where(ExpressionFactory.matchExp("a", 3));
		assertEquals("a = 3", q.getWhere().toString());

		Collection<Expression> exps = Arrays.asList(ExpressionFactory.matchExp("b", 4),
				ExpressionFactory.greaterExp("c", 5));

		q.and(exps);
		assertEquals("(a = 3) and (b = 4) and (c > 5)", q.getWhere().toString());
	}

	@Test
	public void testAnd_ArrayNull() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		q.where(ExpressionFactory.matchExp("a", 3));
		assertEquals("a = 3", q.getWhere().toString());

		q.and();
		assertEquals("a = 3", q.getWhere().toString());
	}

	@Test
	public void testAnd_ArrayEmpty() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		q.where(ExpressionFactory.matchExp("a", 3));
		assertEquals("a = 3", q.getWhere().toString());

		q.and(new Expression[0]);
		assertEquals("a = 3", q.getWhere().toString());
	}

	@Test
	public void testAnd_CollectionEmpty() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		q.where(ExpressionFactory.matchExp("a", 3));
		assertEquals("a = 3", q.getWhere().toString());

		q.and(Collections.<Expression> emptyList());
		assertEquals("a = 3", q.getWhere().toString());
	}

	@Test
	public void testOr_Array() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		q.where(ExpressionFactory.matchExp("a", 3));
		assertEquals("a = 3", q.getWhere().toString());

		q.or(ExpressionFactory.matchExp("b", 4), ExpressionFactory.greaterExp("c", 5));
		assertEquals("(a = 3) or (b = 4) or (c > 5)", q.getWhere().toString());
	}

	@Test
	public void testOr_Collection() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		q.where(ExpressionFactory.matchExp("a", 3));
		assertEquals("a = 3", q.getWhere().toString());

		Collection<Expression> exps = Arrays.asList(ExpressionFactory.matchExp("b", 4),
				ExpressionFactory.greaterExp("c", 5));

		q.or(exps);
		assertEquals("(a = 3) or (b = 4) or (c > 5)", q.getWhere().toString());
	}

	@Test
	public void testOrderBy_Array() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		Ordering o1 = new Ordering("x");
		q.orderBy(o1);

		Object[] result1 = q.getOrderings().toArray();
		assertEquals(1, result1.length);
		assertSame(o1, result1[0]);

		Ordering o2 = new Ordering("y");
		q.orderBy(o2);

		Object[] result2 = q.getOrderings().toArray();
		assertEquals(2, result2.length);
		assertSame(o1, result2[0]);
		assertSame(o2, result2[1]);
	}

	@Test
	public void testOrderBy_Collection() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		Ordering o1 = new Ordering("x");
		q.orderBy(Collections.singletonList(o1));

		Object[] result1 = q.getOrderings().toArray();
		assertEquals(1, result1.length);
		assertSame(o1, result1[0]);

		Ordering o2 = new Ordering("y");
		q.orderBy(Collections.singletonList(o2));

		Object[] result2 = q.getOrderings().toArray();
		assertEquals(2, result2.length);
		assertSame(o1, result2[0]);
		assertSame(o2, result2[1]);
	}

	@Test
	public void testOrderBy_PropertyStrategy() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		q.orderBy("x", SortOrder.ASCENDING_INSENSITIVE);

		Object[] result1 = q.getOrderings().toArray();
		assertEquals(1, result1.length);
		assertEquals(new Ordering("x", SortOrder.ASCENDING_INSENSITIVE), result1[0]);

		q.orderBy("y", SortOrder.DESCENDING);

		Object[] result2 = q.getOrderings().toArray();
		assertEquals(2, result2.length);
		assertEquals(new Ordering("y", SortOrder.DESCENDING), result2[1]);
	}

	@Test
	public void testOrderBy_Property() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		q.orderBy("x");

		Object[] result1 = q.getOrderings().toArray();
		assertEquals(1, result1.length);
		assertEquals(new Ordering("x", SortOrder.ASCENDING), result1[0]);

		q.orderBy("y");

		Object[] result2 = q.getOrderings().toArray();
		assertEquals(2, result2.length);
		assertEquals(new Ordering("x", SortOrder.ASCENDING), result2[0]);
		assertEquals(new Ordering("y", SortOrder.ASCENDING), result2[1]);
	}

	@Test
	public void testPrefetch() {

		PrefetchTreeNode root = PrefetchTreeNode.withPath("a.b", PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);
		q.prefetch(root);

		PrefetchTreeNode root1 = q.getPrefetches();
		assertNotNull(root1);
		assertNotNull(root1.getNode("a.b"));
		assertEquals(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS, root1.getNode("a.b").getSemantics());
	}

	@Test
	public void testPrefetch_Path() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);
		q.prefetch("a.b", PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
		PrefetchTreeNode root1 = q.getPrefetches();

		assertNotNull(root1);
		assertNotNull(root1.getNode("a.b"));

		q.prefetch("a.c", PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
		PrefetchTreeNode root2 = q.getPrefetches();

		assertNotNull(root2);
		assertNotNull(root2.getNode("a.c"));
		assertNotNull(root2.getNode("a.b"));
	}

	@Test
	public void testPrefetch_Merge() {

		PrefetchTreeNode root = PrefetchTreeNode.withPath("a.b", PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);
		q.prefetch(root);

		PrefetchTreeNode root1 = q.getPrefetches();
		assertNotNull(root1);
		assertNotNull(root1.getNode("a.b"));
		assertEquals(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS, root1.getNode("a.b").getSemantics());

		PrefetchTreeNode subRoot = PrefetchTreeNode.withPath("a.b.c", PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
		q.prefetch(subRoot);

		root1 = q.getPrefetches();
		assertNotNull(root1);
		assertNotNull(root1.getNode("a.b"));
		assertEquals(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS, root1.getNode("a.b").getSemantics());

		assertNotNull(root1.getNode("a.b.c"));
		assertEquals(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS, root1.getNode("a.b.c").getSemantics());
	}

	@Test
	public void testLimit() {
		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		assertEquals(0, q.getLimit());
		q.limit(2);
		assertEquals(2, q.getLimit());

		q.limit(3).limit(5);
		assertEquals(5, q.getLimit());
	}

	@Test
	public void testOffset() {
		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		assertEquals(0, q.getOffset());
		q.offset(2);
		assertEquals(2, q.getOffset());

		q.offset(3).offset(5);
		assertEquals(5, q.getOffset());
	}

	@Test
	public void testStatementFetchSize() {
		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		assertEquals(0, q.getStatementFetchSize());
		q.statementFetchSize(2);
		assertEquals(2, q.getStatementFetchSize());

		q.statementFetchSize(3).statementFetchSize(5);
		assertEquals(5, q.getStatementFetchSize());
	}

	@Test
	public void testCacheGroups_Collection() {
		ObjectSelect<DataRow> q = ObjectSelect.dataRowQuery(Artist.class);

		assertEquals(QueryCacheStrategy.NO_CACHE, q.getCacheStrategy());
		assertNull(q.getCacheGroup());

		q.cacheGroup("a");
		assertEquals(QueryCacheStrategy.NO_CACHE, q.getCacheStrategy());
		assertEquals("a", q.getCacheGroup());
	}

	@Test
	public void testCacheStrategy() {
		ObjectSelect<DataRow> q = ObjectSelect.dataRowQuery(Artist.class);

		assertEquals(QueryCacheStrategy.NO_CACHE, q.getCacheStrategy());
		assertNull(q.getCacheGroup());

		q.cacheStrategy(QueryCacheStrategy.LOCAL_CACHE, "b");
		assertSame(QueryCacheStrategy.LOCAL_CACHE, q.getCacheStrategy());
		assertEquals("b", q.getCacheGroup());

		q.cacheStrategy(QueryCacheStrategy.SHARED_CACHE);
		assertSame(QueryCacheStrategy.SHARED_CACHE, q.getCacheStrategy());
		assertNull(q.getCacheGroup());
	}

	@Test
	public void testLocalCache() {
		ObjectSelect<DataRow> q = ObjectSelect.dataRowQuery(Artist.class);

		assertEquals(QueryCacheStrategy.NO_CACHE, q.getCacheStrategy());
		assertNull(q.getCacheGroup());

		q.localCache("a");
		assertSame(QueryCacheStrategy.LOCAL_CACHE, q.getCacheStrategy());
		assertEquals("a", q.getCacheGroup());

		q.localCache();
		assertSame(QueryCacheStrategy.LOCAL_CACHE, q.getCacheStrategy());
		assertNull(q.getCacheGroup());
	}

	@Test
	public void testSharedCache() {
		ObjectSelect<DataRow> q = ObjectSelect.dataRowQuery(Artist.class);

		assertEquals(QueryCacheStrategy.NO_CACHE, q.getCacheStrategy());
		assertNull(q.getCacheGroup());

		q.sharedCache("b");
		assertSame(QueryCacheStrategy.SHARED_CACHE, q.getCacheStrategy());
		assertEquals("b", q.getCacheGroup());

		q.sharedCache();
		assertSame(QueryCacheStrategy.SHARED_CACHE, q.getCacheStrategy());
		assertNull(q.getCacheGroup());
	}

	@Test
	public void testQueryTimeout() {
		ObjectSelect<Artist> query = ObjectSelect.query(Artist.class);
		assertEquals(-1, query.getQueryTimeout());

		query.queryTimeout(10);
		assertEquals(10, query.getQueryTimeout());

		query.queryTimeout(1).queryTimeout(2);
		assertEquals(2, query.getQueryTimeout());
	}

}
