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
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.compiled.TestMappings;
import org.apache.cayenne.query.resolved.ResolvedSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ObjectSelectTest {

	// the execution settings of a query are not readable from the query itself, only from the resolved query
	private static ResolvedSelect resolve(ObjectSelect<?> query) {
		DataMap map = new DataMap("map");
		ObjEntity artist = new ObjEntity("Artist");
		artist.setClassName(Artist.class.getName());
		map.addObjEntity(artist);
		map.addObjEntity(new ObjEntity("Painting"));
		map.addDbEntity(new DbEntity("PAINTING"));
		return query.resolve(TestMappings.compile(map));
	}

	@Test
	public void dataRowQuery() {
		ObjectSelect<DataRow> q = ObjectSelect.dataRowQuery(Artist.class);
		assertNotNull(q);
		assertTrue(resolve(q).fetchingDataRows());
		assertEquals("from Artist", q.toQL());
	}

	@Test
	public void query_RootType() {
		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);
		assertNotNull(q);
		assertNull(q.where);
		assertFalse(resolve(q).fetchingDataRows());
		assertEquals("from Artist", q.toQL());
	}

	@Test
	public void query_RootType_WithQualifier() {
		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class).where(ExpressionFactory.matchExp("a", "A"));
		assertNotNull(q);
		assertEquals("a = \"A\"", q.where.toString());
		assertFalse(resolve(q).fetchingDataRows());
		assertEquals("from Artist where a = \"A\"", q.toQL());
	}

	@Test
	public void query_TypeAndEntity() {
		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class, "Painting");
		assertNotNull(q);
		assertFalse(resolve(q).fetchingDataRows());
		assertEquals("from Painting", q.toQL());
	}

	@Test
	public void query_TypeAndDbEntity() {
		ObjectSelect<DataRow> q = ObjectSelect.dbQuery("PAINTING");
		assertNotNull(q);
		assertTrue(resolve(q).fetchingDataRows());
		assertEquals("from db:PAINTING", q.toQL());
	}

	@Test
	public void where() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		q.where(ExpressionFactory.matchExp("a", 3));
		assertEquals("a = 3", q.where.toString());

		q.where(ExpressionFactory.matchExp("b", 4));
		assertEquals("(a = 3) and (b = 4)", q.where.toString());
	}

	@Test
	public void and_Array() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		q.where(ExpressionFactory.matchExp("a", 3));
		assertEquals("a = 3", q.where.toString());

		q.and(ExpressionFactory.matchExp("b", 4), ExpressionFactory.greaterExp("c", 5));
		assertEquals("(a = 3) and (b = 4) and (c > 5)", q.where.toString());
	}

	@Test
	public void and_Collection() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		q.where(ExpressionFactory.matchExp("a", 3));
		assertEquals("a = 3", q.where.toString());

		Collection<Expression> exps = Arrays.asList(ExpressionFactory.matchExp("b", 4),
				ExpressionFactory.greaterExp("c", 5));

		q.and(exps);
		assertEquals("(a = 3) and (b = 4) and (c > 5)", q.where.toString());
	}

	@Test
	public void and_ArrayNull() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		q.where(ExpressionFactory.matchExp("a", 3));
		assertEquals("a = 3", q.where.toString());

		q.and();
		assertEquals("a = 3", q.where.toString());
	}

	@Test
	public void and_ArrayEmpty() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		q.where(ExpressionFactory.matchExp("a", 3));
		assertEquals("a = 3", q.where.toString());

		q.and(new Expression[0]);
		assertEquals("a = 3", q.where.toString());
	}

	@Test
	public void and_CollectionEmpty() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		q.where(ExpressionFactory.matchExp("a", 3));
		assertEquals("a = 3", q.where.toString());

		q.and(Collections.<Expression> emptyList());
		assertEquals("a = 3", q.where.toString());
	}

	@Test
	public void or_Array() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		q.where(ExpressionFactory.matchExp("a", 3));
		assertEquals("a = 3", q.where.toString());

		q.or(ExpressionFactory.matchExp("b", 4), ExpressionFactory.greaterExp("c", 5));
		assertEquals("(a = 3) or (b = 4) or (c > 5)", q.where.toString());
	}

	@Test
	public void or_Collection() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		q.where(ExpressionFactory.matchExp("a", 3));
		assertEquals("a = 3", q.where.toString());

		Collection<Expression> exps = Arrays.asList(ExpressionFactory.matchExp("b", 4),
				ExpressionFactory.greaterExp("c", 5));

		q.or(exps);
		assertEquals("(a = 3) or (b = 4) or (c > 5)", q.where.toString());
	}

	@Test
	public void orderBy_Array() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		Ordering o1 = new Ordering("x");
		q.orderBy(o1);

		Object[] result1 = q.orderings.toArray();
		assertEquals(1, result1.length);
		assertSame(o1, result1[0]);

		Ordering o2 = new Ordering("y");
		q.orderBy(o2);

		Object[] result2 = q.orderings.toArray();
		assertEquals(2, result2.length);
		assertSame(o1, result2[0]);
		assertSame(o2, result2[1]);
	}

	@Test
	public void orderBy_Collection() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		Ordering o1 = new Ordering("x");
		q.orderBy(Collections.singletonList(o1));

		Object[] result1 = q.orderings.toArray();
		assertEquals(1, result1.length);
		assertSame(o1, result1[0]);

		Ordering o2 = new Ordering("y");
		q.orderBy(Collections.singletonList(o2));

		Object[] result2 = q.orderings.toArray();
		assertEquals(2, result2.length);
		assertSame(o1, result2[0]);
		assertSame(o2, result2[1]);
	}

	@Test
	public void orderBy_PropertyStrategy() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		q.orderBy("x", SortOrder.ASCENDING_INSENSITIVE);

		Object[] result1 = q.orderings.toArray();
		assertEquals(1, result1.length);
		assertEquals(new Ordering("x", SortOrder.ASCENDING_INSENSITIVE), result1[0]);

		q.orderBy("y", SortOrder.DESCENDING);

		Object[] result2 = q.orderings.toArray();
		assertEquals(2, result2.length);
		assertEquals(new Ordering("y", SortOrder.DESCENDING), result2[1]);
	}

	@Test
	public void orderBy_Property() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		q.orderBy("x");

		Object[] result1 = q.orderings.toArray();
		assertEquals(1, result1.length);
		assertEquals(new Ordering("x", SortOrder.ASCENDING), result1[0]);

		q.orderBy("y");

		Object[] result2 = q.orderings.toArray();
		assertEquals(2, result2.length);
		assertEquals(new Ordering("x", SortOrder.ASCENDING), result2[0]);
		assertEquals(new Ordering("y", SortOrder.ASCENDING), result2[1]);
	}

	@Test
	public void prefetch() {

		PrefetchTreeNode root = PrefetchTreeNode.withPath("a.b", PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);
		q.prefetch(root);

		PrefetchTreeNode root1 = q.prefetches;
		assertNotNull(root1);
		assertNotNull(root1.getNode("a.b"));
		assertEquals(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS, root1.getNode("a.b").getSemantics());
	}

	@Test
	public void prefetch_Path() {

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);
		q.prefetch("a.b", PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
		PrefetchTreeNode root1 = q.prefetches;

		assertNotNull(root1);
		assertNotNull(root1.getNode("a.b"));

		q.prefetch("a.c", PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
		PrefetchTreeNode root2 = q.prefetches;

		assertNotNull(root2);
		assertNotNull(root2.getNode("a.c"));
		assertNotNull(root2.getNode("a.b"));
	}

	@Test
	public void prefetch_Merge() {

		PrefetchTreeNode root = PrefetchTreeNode.withPath("a.b", PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);
		q.prefetch(root);

		PrefetchTreeNode root1 = q.prefetches;
		assertNotNull(root1);
		assertNotNull(root1.getNode("a.b"));
		assertEquals(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS, root1.getNode("a.b").getSemantics());

		PrefetchTreeNode subRoot = PrefetchTreeNode.withPath("a.b.c", PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
		q.prefetch(subRoot);

		root1 = q.prefetches;
		assertNotNull(root1);
		assertNotNull(root1.getNode("a.b"));
		assertEquals(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS, root1.getNode("a.b").getSemantics());

		assertNotNull(root1.getNode("a.b.c"));
		assertEquals(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS, root1.getNode("a.b.c").getSemantics());
	}

	@Test
	public void limit() {
		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		assertEquals(0, resolve(q).fetchLimit());
		q.limit(2);
		assertEquals(2, resolve(q).fetchLimit());

		q.limit(3).limit(5);
		assertEquals(5, resolve(q).fetchLimit());
	}

	@Test
	public void offset() {
		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		assertEquals(0, resolve(q).fetchOffset());
		q.offset(2);
		assertEquals(2, resolve(q).fetchOffset());

		q.offset(3).offset(5);
		assertEquals(5, resolve(q).fetchOffset());
	}

	@Test
	public void statementFetchSize() {
		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class);

		assertEquals(0, resolve(q).statementFetchSize());
		q.statementFetchSize(2);
		assertEquals(2, resolve(q).statementFetchSize());

		q.statementFetchSize(3).statementFetchSize(5);
		assertEquals(5, resolve(q).statementFetchSize());
	}

	@Test
	public void cacheGroups_Collection() {
		ObjectSelect<DataRow> q = ObjectSelect.dataRowQuery(Artist.class);

		assertEquals(QueryCacheStrategy.NO_CACHE, resolve(q).cacheStrategy());
		assertNull(resolve(q).cacheGroup());

		q.cacheGroup("a");
		assertEquals(QueryCacheStrategy.NO_CACHE, resolve(q).cacheStrategy());
		assertEquals("a", resolve(q).cacheGroup());
	}

	@Test
	public void cacheStrategy() {
		ObjectSelect<DataRow> q = ObjectSelect.dataRowQuery(Artist.class);

		assertEquals(QueryCacheStrategy.NO_CACHE, resolve(q).cacheStrategy());
		assertNull(resolve(q).cacheGroup());

		q.cacheStrategy(QueryCacheStrategy.LOCAL_CACHE, "b");
		assertSame(QueryCacheStrategy.LOCAL_CACHE, resolve(q).cacheStrategy());
		assertEquals("b", resolve(q).cacheGroup());

		q.cacheStrategy(QueryCacheStrategy.SHARED_CACHE);
		assertSame(QueryCacheStrategy.SHARED_CACHE, resolve(q).cacheStrategy());
		assertNull(resolve(q).cacheGroup());
	}

	@Test
	public void localCache() {
		ObjectSelect<DataRow> q = ObjectSelect.dataRowQuery(Artist.class);

		assertEquals(QueryCacheStrategy.NO_CACHE, resolve(q).cacheStrategy());
		assertNull(resolve(q).cacheGroup());

		q.localCache("a");
		assertSame(QueryCacheStrategy.LOCAL_CACHE, resolve(q).cacheStrategy());
		assertEquals("a", resolve(q).cacheGroup());

		q.localCache();
		assertSame(QueryCacheStrategy.LOCAL_CACHE, resolve(q).cacheStrategy());
		assertNull(resolve(q).cacheGroup());
	}

	@Test
	public void sharedCache() {
		ObjectSelect<DataRow> q = ObjectSelect.dataRowQuery(Artist.class);

		assertEquals(QueryCacheStrategy.NO_CACHE, resolve(q).cacheStrategy());
		assertNull(resolve(q).cacheGroup());

		q.sharedCache("b");
		assertSame(QueryCacheStrategy.SHARED_CACHE, resolve(q).cacheStrategy());
		assertEquals("b", resolve(q).cacheGroup());

		q.sharedCache();
		assertSame(QueryCacheStrategy.SHARED_CACHE, resolve(q).cacheStrategy());
		assertNull(resolve(q).cacheGroup());
	}

	@Test
	public void queryTimeout() {
		ObjectSelect<Artist> query = ObjectSelect.query(Artist.class);
		assertEquals(-1, resolve(query).queryTimeout());

		query.queryTimeout(10);
		assertEquals(10, resolve(query).queryTimeout());

		query.queryTimeout(1).queryTimeout(2);
		assertEquals(2, resolve(query).queryTimeout());
	}

	@Test
	public void readOnlyMetadata() {
		assertTrue(resolve(ObjectSelect.query(Artist.class)).readOnly());
	}

}
