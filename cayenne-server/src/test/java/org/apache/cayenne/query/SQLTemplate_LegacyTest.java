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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

public class SQLTemplate_LegacyTest {

	@Test
	@SuppressWarnings("unchecked")
	@Deprecated
	public void testQueryWithParameters() {
		SQLTemplate q1 = new SQLTemplate("E1", "SELECT");

		Query q2 = q1.queryWithParameters(Collections.EMPTY_MAP);
		assertNotNull(q2);
		assertNotSame(q1, q2);
		assertTrue(q2 instanceof SQLTemplate);

		Query q3 = q1.queryWithParameters(Collections.singletonMap("a", "b"));
		assertNotNull(q3);
		assertNotSame(q1, q3);

		Query q4 = q1.queryWithParameters(Collections.singletonMap("a", "b"));
		assertNotNull(q4);
		assertNotSame(q3, q4);
	}

	@SuppressWarnings("unchecked")
	@Test
	@Deprecated
	public void testSetParameters_SingleParameterSet() throws Exception {
		SQLTemplate query = new SQLTemplate();

		assertNotNull(query.getParameters());
		assertTrue(query.getParameters().isEmpty());

		Map<String, Object> params = new HashMap<>();
		params.put("a", "b");

		query.setParameters(params);
		assertEquals(params, query.getParameters());
		Iterator<?> it = query.parametersIterator();
		assertTrue(it.hasNext());
		assertEquals(params, it.next());
		assertFalse(it.hasNext());

		query.setParameters();
		assertNotNull(query.getParameters());
		assertTrue(query.getParameters().isEmpty());
		it = query.parametersIterator();
		assertFalse(it.hasNext());
	}

	@Test
	@SuppressWarnings("unchecked")
	@Deprecated
	public void testSetParameters_BatchParameterSet() throws Exception {
		SQLTemplate query = new SQLTemplate();

		assertNotNull(query.getParameters());
		assertTrue(query.getParameters().isEmpty());

		Map<String, Object> params1 = new HashMap<>();
		params1.put("a", "b");

		Map<String, Object> params2 = new HashMap<>();
		params2.put("1", "2");

		query.setParameters(new Map[] { params1, params2, null });
		assertEquals(params1, query.getParameters());
		Iterator<?> it = query.parametersIterator();
		assertTrue(it.hasNext());
		assertEquals(params1, it.next());
		assertTrue(it.hasNext());
		assertEquals(params2, it.next());
		assertTrue(it.hasNext());
		assertTrue(((Map<String, Object>) it.next()).isEmpty());
		assertFalse(it.hasNext());

		query.setParameters((Map[]) null);
		assertNotNull(query.getParameters());
		assertTrue(query.getParameters().isEmpty());
		it = query.parametersIterator();
		assertFalse(it.hasNext());
	}
}
