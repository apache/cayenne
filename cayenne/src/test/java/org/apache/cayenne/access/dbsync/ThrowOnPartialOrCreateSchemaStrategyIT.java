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
package org.apache.cayenne.access.dbsync;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

@UseCayenneRuntime(CayenneProjects.SUS_PROJECT)
public class ThrowOnPartialOrCreateSchemaStrategyIT extends SchemaUpdateStrategyBase {

    @Test
	public void testMixedStrategyTableExist() throws Exception {

		createOneTable("SUS1");
		createOneTable("SUS2");

		String template = "SELECT #result('ARTIST_ID' 'int') FROM ARTIST ORDER BY ARTIST_ID";
		SQLTemplate query = new SQLTemplate(Object.class, template);

		setStrategy(ThrowOnPartialOrCreateSchemaStrategy.class);
		node.performQueries(Collections.singletonList(query), mock(OperationObserver.class));
	}

    @Test
	public void testMixedStrategyTableNoExist() throws Exception {

		String template = "SELECT #result('id' 'int') FROM SUS1";
		SQLTemplate query = new SQLTemplate(Object.class, template);

		setStrategy(ThrowOnPartialOrCreateSchemaStrategy.class);

		node.performQueries(Collections.singletonList(query), mock(OperationObserver.class));
		Map<String, Boolean> nameTables = tablesMap();
		assertTrue(nameTables.get("SUS1"));
		assertEquals(2, existingTables().size());

		node.performQueries(Collections.singletonList(query), mock(OperationObserver.class));
		assertEquals(2, existingTables().size());
	}

    @Test
	public void testMixedStrategyWithOneTable() throws Exception {
		createOneTable("SUS1");
		setStrategy(ThrowOnPartialOrCreateSchemaStrategy.class);

		String template = "SELECT #result('ARTIST_ID' 'int') FROM ARTIST ORDER BY ARTIST_ID";
		SQLTemplate query = new SQLTemplate(Object.class, template);

		try {
			node.performQueries(Collections.singletonList(query), mock(OperationObserver.class));
			assertEquals(1, existingTables().size());
			fail("Must have thrown on partial schema");
		} catch (CayenneRuntimeException e) {
			// expected
		}
	}
}
