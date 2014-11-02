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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.util.Util;
import org.junit.Test;

public class SQLTemplateTest {

	@Test
	public void testSetParams() throws Exception {
		SQLTemplate query = new SQLTemplate();

		assertNotNull(query.getParams());
		assertTrue(query.getParams().isEmpty());

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("a", "b");

		query.setParams(params);
		assertEquals(params, query.getParams());

		query.setParams(null);
		assertNotNull(query.getParams());
		assertTrue(query.getParams().isEmpty());
	}

	@Test
	public void testGetDefaultTemplate() {
		SQLTemplate query = new SQLTemplate();
		query.setDefaultTemplate("AAA # BBB");
		assertEquals("AAA # BBB", query.getDefaultTemplate());
	}

	@Test
	public void testGetTemplate() {
		SQLTemplate query = new SQLTemplate();

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
	public void testColumnNameCapitalization() {
		SQLTemplate q1 = new SQLTemplate("E1", "SELECT");
		assertSame(CapsStrategy.DEFAULT, q1.getColumnNamesCapitalization());
		q1.setColumnNamesCapitalization(CapsStrategy.UPPER);
		assertEquals(CapsStrategy.UPPER, q1.getColumnNamesCapitalization());
	}

	@Test
	public void testSerializability() throws Exception {
		SQLTemplate o = new SQLTemplate("Test", "DO SQL");
		Object clone = Util.cloneViaSerialization(o);

		assertTrue(clone instanceof SQLTemplate);
		SQLTemplate c1 = (SQLTemplate) clone;

		assertNotSame(o, c1);
		assertEquals(o.getRoot(), c1.getRoot());
		assertEquals(o.getDefaultTemplate(), c1.getDefaultTemplate());
	}
}
