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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Arrays;

import org.apache.cayenne.DataRow;
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
}
