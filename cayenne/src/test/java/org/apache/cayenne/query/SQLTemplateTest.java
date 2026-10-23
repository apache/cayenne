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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.jupiter.api.Test;

public class SQLTemplateTest {

	@Test
	public void noRoot() {
		SQLTemplate<Object> query = new SQLTemplate<>("SELECT");
		assertNull(query.getPersistentType());
		assertNull(query.getEntityName());
		assertNull(query.getDataMapName());
		assertEquals("SELECT", query.getDefaultTemplate());
		assertFalse(query.isFetchingDataRows());
	}

	@Test
	public void query() {
		SQLTemplate<Artist> query = SQLTemplate.query(Artist.class, "SELECT");
		assertSame(Artist.class, query.getPersistentType());
		assertNull(query.getEntityName());
		assertNull(query.getDataMapName());
		assertEquals("SELECT", query.getDefaultTemplate());
		assertFalse(query.isFetchingDataRows());
	}

	@Test
	public void entityQuery() {
		SQLTemplate<Persistent> query = SQLTemplate.entityQuery("SomeEntity", "SELECT");
		assertNull(query.getPersistentType());
		assertEquals("SomeEntity", query.getEntityName());
		assertNull(query.getDataMapName());
		assertFalse(query.isFetchingDataRows());
	}

	@Test
	public void dataRowQuery() {
		SQLTemplate<DataRow> query = SQLTemplate.dataRowQuery("SELECT");
		assertNull(query.getPersistentType());
		assertNull(query.getEntityName());
		assertNull(query.getDataMapName());
		assertTrue(query.isFetchingDataRows());
	}

	@Test
	public void dataRowQueryDataMap() {
		SQLTemplate<DataRow> query = SQLTemplate.dataRowQuery("map1", "SELECT");
		assertNull(query.getPersistentType());
		assertNull(query.getEntityName());
		assertEquals("map1", query.getDataMapName());
		assertTrue(query.isFetchingDataRows());
	}

	@Test
	public void queryWithParametersKeepsRoot() {
		SQLTemplate<Artist> q1 = SQLTemplate.query(Artist.class, "SELECT");
		SQLTemplate<Artist> q2 = q1.queryWithParameters(Map.of("a", "b"));
		assertSame(Artist.class, q2.getPersistentType());
		assertEquals("SELECT", q2.getDefaultTemplate());

		SQLTemplate<DataRow> q3 = SQLTemplate.dataRowQuery("map1", "SELECT");
		SQLTemplate<DataRow> q4 = q3.queryWithParameters(Map.of("a", "b"));
		assertEquals("map1", q4.getDataMapName());
		assertTrue(q4.isFetchingDataRows());
	}

	@Test
	public void setParams() throws Exception {
		SQLTemplate query = new SQLTemplate("SELECT");

		assertTrue(query.getParams().isEmpty());

		Map<String, Object> params = new HashMap<>();
		params.put("a", "b");

		query.setParams(params);
		assertEquals(params, query.getParams());

		query.setParams(null);
		assertTrue(query.getParams().isEmpty());
	}

	@Test
	public void setParamsArray() throws Exception {
		SQLTemplate query = new SQLTemplate("SELECT");

		assertTrue(query.getPositionalParams().isEmpty());

		query.setParamsArray("N", "m");
		assertEquals(Arrays.asList("N", "m"), query.getPositionalParams());

		query.setParamsArray();
		assertTrue(query.getPositionalParams().isEmpty());
	}

	@Test
	public void setParams_MixingStyles() throws Exception {

		SQLTemplate query = new SQLTemplate("SELECT");

		assertTrue(query.getParams().isEmpty());
		assertTrue(query.getPositionalParams().isEmpty());

		Map<String, Object> params = Collections.<String, Object> singletonMap("a", "b");
		query.setParams(params);
		assertEquals(params, query.getParams());
		assertTrue(query.getPositionalParams().isEmpty());

		query.setParamsArray("D", "G");
		assertEquals(Arrays.asList("D", "G"), query.getPositionalParams());
		assertTrue(query.getParams().isEmpty());

		// even resetting named to null should result in resetting positional
		query.setParams(null);
		assertTrue(query.getParams().isEmpty());
		assertTrue(query.getPositionalParams().isEmpty());
	}

	@Test
	public void getDefaultTemplate() {
		SQLTemplate query = new SQLTemplate("AAA # BBB");
		assertEquals("AAA # BBB", query.getDefaultTemplate());
	}

	@Test
	public void getTemplate() {
		SQLTemplate query = new SQLTemplate((String) null);

		// no template for key, no default template... must be null
		assertNull(query.getTemplate("key1"));

		// no template for key, must return default
		query.setDefaultTemplate("AAA # BBB");
		assertEquals("AAA # BBB", query.getTemplate("key1"));

		// must find template
		query.setTemplate("key1", "XYZ");
		assertEquals("XYZ", query.getTemplate("key1"));

		// add another template.. still must find
		query.setTemplate("key2", "123");
		assertEquals("XYZ", query.getTemplate("key1"));
		assertEquals("123", query.getTemplate("key2"));
	}

	@Test
	public void columnNameCapitalization() {
		SQLTemplate q1 = SQLTemplate.entityQuery("E1", "SELECT");
		assertSame(CapsStrategy.DEFAULT, q1.getColumnNamesCapitalization());
		q1.setColumnNamesCapitalization(CapsStrategy.UPPER);
		assertEquals(CapsStrategy.UPPER, q1.getColumnNamesCapitalization());
	}

	@Test
	public void notReadOnlyMetadata() {
		assertFalse(SQLTemplate.entityQuery("E1", "SELECT").getMetaData(mock(EntityResolver.class)).isReadOnly());
	}
}
