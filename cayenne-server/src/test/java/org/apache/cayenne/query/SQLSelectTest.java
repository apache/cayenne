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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Arrays;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.map.EntityResolver;
import org.junit.Test;

public class SQLSelectTest {

	@Test
	public void testCacheGroups_Collection() {
		SQLSelect<DataRow> q = SQLSelect.dataRowQuery("bla");

		assertNull(q.getCacheStrategy());
		assertNull(q.getCacheGroups());

		q.cacheGroups(Arrays.asList("a", "b"));
		assertNull(q.getCacheStrategy());
		assertArrayEquals(new String[] { "a", "b" }, q.getCacheGroups());
	}

	@Test
	public void testCacheStrategy() {
		SQLSelect<DataRow> q = SQLSelect.dataRowQuery("bla");

		assertNull(q.getCacheStrategy());
		assertNull(q.getCacheGroups());

		q.cacheStrategy(QueryCacheStrategy.LOCAL_CACHE, "a", "b");
		assertSame(QueryCacheStrategy.LOCAL_CACHE, q.getCacheStrategy());
		assertArrayEquals(new String[] { "a", "b" }, q.getCacheGroups());

		q.cacheStrategy(QueryCacheStrategy.SHARED_CACHE);
		assertSame(QueryCacheStrategy.SHARED_CACHE, q.getCacheStrategy());
		assertNull(q.getCacheGroups());
	}

	@Test
	public void testLocalCache() {
		SQLSelect<DataRow> q = SQLSelect.dataRowQuery("bla");

		assertNull(q.getCacheStrategy());
		assertNull(q.getCacheGroups());

		q.localCache("a", "b");
		assertSame(QueryCacheStrategy.LOCAL_CACHE, q.getCacheStrategy());
		assertArrayEquals(new String[] { "a", "b" }, q.getCacheGroups());

		q.localCache();
		assertSame(QueryCacheStrategy.LOCAL_CACHE, q.getCacheStrategy());
		assertNull(q.getCacheGroups());
	}

	@Test
	public void testSharedCache() {
		SQLSelect<DataRow> q = SQLSelect.dataRowQuery("bla");

		assertNull(q.getCacheStrategy());
		assertNull(q.getCacheGroups());

		q.sharedCache("a", "b");
		assertSame(QueryCacheStrategy.SHARED_CACHE, q.getCacheStrategy());
		assertArrayEquals(new String[] { "a", "b" }, q.getCacheGroups());

		q.sharedCache();
		assertSame(QueryCacheStrategy.SHARED_CACHE, q.getCacheStrategy());
		assertNull(q.getCacheGroups());
	}

	@Test
	public void testCreateReplacementQuery() {

		SQLSelect<DataRow> q = SQLSelect.dataRowQuery("bla");
		Query replacement = q.createReplacementQuery(mock(EntityResolver.class));
		assertThat(replacement, instanceOf(SQLTemplate.class));
	}

	@Test
	public void testCreateReplacementQuery_ParamsArray_Single() {

		SQLSelect<DataRow> q = SQLSelect.dataRowQuery("bla").paramsArray("a");
		SQLTemplate replacement = (SQLTemplate) q.createReplacementQuery(mock(EntityResolver.class));
		assertArrayEquals(new Object[] { "a" }, replacement.getPositionalParams().toArray());
	}

	@Test
	public void testCreateReplacementQuery_ParamsArray_Multiple() {

		SQLSelect<DataRow> q = SQLSelect.dataRowQuery("bla").paramsArray("a", "b");
		SQLTemplate replacement = (SQLTemplate) q.createReplacementQuery(mock(EntityResolver.class));
		assertArrayEquals(new Object[] { "a", "b" }, replacement.getPositionalParams().toArray());
	}

	@Test
	public void testGetMetadata_ParamsArray_Multiple_Cache() {

		EntityResolver resolver = mock(EntityResolver.class);
		QueryMetadata md0 = SQLSelect.dataRowQuery("bla").localCache().getMetaData(resolver);
		QueryMetadata md1 = SQLSelect.dataRowQuery("bla").localCache().paramsArray("a").getMetaData(resolver);
		QueryMetadata md2 = SQLSelect.dataRowQuery("bla").localCache().paramsArray("a", "b").getMetaData(resolver);
		QueryMetadata md3 = SQLSelect.dataRowQuery("bla").localCache().paramsArray(null, "b").getMetaData(resolver);

		assertNotNull(md0.getCacheKey());
		assertNotNull(md1.getCacheKey());
		assertNotNull(md2.getCacheKey());
		assertNotNull(md3.getCacheKey());
		
		assertNotEquals(md0.getCacheKey(), md1.getCacheKey());
		assertNotEquals(md0.getCacheKey(), md2.getCacheKey());
		assertNotEquals(md1.getCacheKey(), md2.getCacheKey());
		assertNotEquals(md3.getCacheKey(), md2.getCacheKey());
	}
}
