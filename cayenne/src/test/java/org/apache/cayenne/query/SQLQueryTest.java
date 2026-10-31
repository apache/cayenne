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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.apache.cayenne.dba.postgres.PostgresAdapter;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.resolved.ResolvedQuery;
import org.apache.cayenne.query.resolved.ResolvedSQL;
import org.junit.jupiter.api.Test;

public class SQLQueryTest {

	private static ResolvedSQL resolved(SQLQuery<?> query) {
		return query.resolve(mock(EntityResolver.class));
	}

	@Test
	public void cacheStrategy() {
		SQLQuery<DataRow> q = SQLQuery.query("bla");

		assertSame(QueryCacheStrategy.NO_CACHE, resolved(q).cacheStrategy());
		assertNull(resolved(q).cacheGroup());

		q.cacheStrategy(QueryCacheStrategy.LOCAL_CACHE, "a");
		assertSame(QueryCacheStrategy.LOCAL_CACHE, resolved(q).cacheStrategy());
		assertEquals("a", resolved(q).cacheGroup());

		q.cacheStrategy(QueryCacheStrategy.SHARED_CACHE);
		assertSame(QueryCacheStrategy.SHARED_CACHE, resolved(q).cacheStrategy());
		assertNull(resolved(q).cacheGroup());
	}

	@Test
	public void localCache() {
		SQLQuery<DataRow> q = SQLQuery.query("bla");

		assertSame(QueryCacheStrategy.NO_CACHE, resolved(q).cacheStrategy());
		assertNull(resolved(q).cacheGroup());

		q.localCache("a");
		assertSame(QueryCacheStrategy.LOCAL_CACHE, resolved(q).cacheStrategy());
		assertEquals("a", resolved(q).cacheGroup());

		q.localCache();
		assertSame(QueryCacheStrategy.LOCAL_CACHE, resolved(q).cacheStrategy());
		assertNull(resolved(q).cacheGroup());
	}

	@Test
	public void sharedCache() {
		SQLQuery<DataRow> q = SQLQuery.query("bla");

		assertSame(QueryCacheStrategy.NO_CACHE, resolved(q).cacheStrategy());
		assertNull(resolved(q).cacheGroup());

		q.sharedCache("a");
		assertSame(QueryCacheStrategy.SHARED_CACHE, resolved(q).cacheStrategy());
		assertEquals("a", resolved(q).cacheGroup());

		q.sharedCache();
		assertSame(QueryCacheStrategy.SHARED_CACHE, resolved(q).cacheStrategy());
		assertNull(resolved(q).cacheGroup());
	}

	@Test
	public void resolve() {
		SQLQuery<DataRow> q = SQLQuery.query("bla");
		ResolvedSQL resolved = q.resolve(mock(EntityResolver.class));
		assertSame(q, resolved.source());
		assertEquals("bla", resolved.defaultTemplate());
		assertTrue(resolved.fetchingDataRows());
		assertNull(resolved.dataMap());
	}

	@Test
	public void resolve_ParamsArray_Single() {
		SQLQuery<DataRow> q = SQLQuery.query("bla").paramsArray("a");
		assertArrayEquals(new Object[] { "a" }, q.resolve(mock(EntityResolver.class)).positionalParams().toArray());
	}

	@Test
	public void resolve_ParamsArray_Multiple() {
		SQLQuery<DataRow> q = SQLQuery.query("bla").paramsArray("a", "b");
		assertArrayEquals(new Object[] { "a", "b" }, q.resolve(mock(EntityResolver.class)).positionalParams().toArray());
	}

	@Test
	public void resolve_DataNode() {
		SQLQuery<DataRow> q = SQLQuery.query("bla").dataNode("n1");
		assertEquals("n1", q.resolve(mock(EntityResolver.class)).dataNodeName());
	}

	@Test
	public void resolve_Dialect() {
		SQLQuery<DataRow> q = SQLQuery.query("default")
				.append(" plus")
				.dialect(MySQLAdapter.class, "mysql")
				.dialectAppend(MySQLAdapter.class, " plus")
				.dialectAppend(PostgresAdapter.class, "postgres")
				.dialectAppend(PostgresAdapter.class, " plus");

		ResolvedSQL resolved = q.resolve(mock(EntityResolver.class));
		assertEquals("default plus", resolved.defaultTemplate());
		assertEquals("mysql plus", resolved.templates().get(MySQLAdapter.class.getName()));
		assertEquals("postgres plus", resolved.templates().get(PostgresAdapter.class.getName()));
		assertEquals("default plus", resolved.template(mock(DbAdapter.class)));
	}

	@Test
	public void getMetadata_ParamsArray_Multiple_Cache() {

		EntityResolver resolver = mock(EntityResolver.class);
		ResolvedQuery md0 = SQLQuery.query("bla").localCache().resolve(resolver);
		ResolvedQuery md1 = SQLQuery.query("bla").localCache().paramsArray("a").resolve(resolver);
		ResolvedQuery md2 = SQLQuery.query("bla").localCache().paramsArray("a", "b").resolve(resolver);
		ResolvedQuery md3 = SQLQuery.query("bla").localCache().paramsArray(null, "b").resolve(resolver);

		assertNotNull(md0.cacheKey());
		assertNotNull(md1.cacheKey());
		assertNotNull(md2.cacheKey());
		assertNotNull(md3.cacheKey());
		
		assertNotEquals(md0.cacheKey(), md1.cacheKey());
		assertNotEquals(md0.cacheKey(), md2.cacheKey());
		assertNotEquals(md1.cacheKey(), md2.cacheKey());
		assertNotEquals(md3.cacheKey(), md2.cacheKey());
	}

	@Test
	public void notReadOnlyMetadata() {
		// raw SQL is opaque, so SQLQuery keeps the regular transaction
		assertFalse(SQLQuery.query("SELECT * FROM X").resolve(mock(EntityResolver.class)).readOnly());
	}
}
