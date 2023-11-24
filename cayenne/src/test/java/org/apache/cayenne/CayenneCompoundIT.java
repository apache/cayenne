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

package org.apache.cayenne;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.compound.CharPkTestEntity;
import org.apache.cayenne.testdo.compound.CompoundPkTestEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@UseCayenneRuntime(CayenneProjects.COMPOUND_PROJECT)
public class CayenneCompoundIT extends RuntimeCase {

	@Inject
	private ObjectContext context;

	@Inject
	protected DBHelper dbHelper;

	private TableHelper tCompoundPKTest;
	private TableHelper tCharPKTest;
	private TableHelper tCompoundIntPKTest;

	@Before
	public void setUp() throws Exception {
		tCompoundPKTest = new TableHelper(dbHelper, "COMPOUND_PK_TEST");
		tCompoundPKTest.setColumns("KEY1", "KEY2", "NAME");

		tCharPKTest = new TableHelper(dbHelper, "CHAR_PK_TEST");
		tCharPKTest.setColumns("PK_COL", "OTHER_COL");

		tCompoundIntPKTest = new TableHelper(dbHelper, "COMPOUND_INT_PK");
		tCompoundIntPKTest.setColumns("id1", "id2", "name");
	}

	private void createOneCompoundPK() throws Exception {
		tCompoundPKTest.insert("PK1", "PK2", "BBB");
	}

	private void createCompoundPKs(int size) throws Exception {
		for(int i=0; i<size; i++) {
			tCompoundPKTest.insert("PK"+i, "PK"+(2*i), "BBB"+i);
		}
	}

	private void createOneCharPK() throws Exception {
		tCharPKTest.insert("CPK", "AAAA");
	}

	@Test
	public void testObjectForPKEntityMapCompound() throws Exception {
		createOneCompoundPK();

		Map<String, Object> pk = new HashMap<>();
		pk.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "PK1");
		pk.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "PK2");
		CompoundPkTestEntity object = Cayenne.objectForPK(context, CompoundPkTestEntity.class, pk);

		assertNotNull(object);
		assertEquals("BBB", object.getName());
	}

	@Test
	public void testCompoundPKForObject() throws Exception {
		createOneCompoundPK();

		List<CompoundPkTestEntity> objects = ObjectSelect.query(CompoundPkTestEntity.class).select(context);
		assertEquals(1, objects.size());
		CompoundPkTestEntity object = objects.get(0);

		Map<String, Object> pk = Cayenne.compoundPKForObject(object);
		assertNotNull(pk);
		assertEquals(2, pk.size());
		assertEquals("PK1", pk.get(CompoundPkTestEntity.KEY1_PK_COLUMN));
		assertEquals("PK2", pk.get(CompoundPkTestEntity.KEY2_PK_COLUMN));
	}

	@Test
	public void testIntPKForObjectFailureForCompound() throws Exception {
		createOneCompoundPK();

		List<CompoundPkTestEntity> objects = ObjectSelect.query(CompoundPkTestEntity.class).select(context);
		assertEquals(1, objects.size());
		CompoundPkTestEntity object = objects.get(0);

		try {
			Cayenne.intPKForObject(object);
			fail("intPKForObject must fail for compound key");
		} catch (CayenneRuntimeException ex) {
			// expected
		}
	}

	@Test
	public void testIntPKForObjectFailureForNonNumeric() throws Exception {
		createOneCharPK();

		List<CharPkTestEntity> objects = ObjectSelect.query(CharPkTestEntity.class).select(context);
		assertEquals(1, objects.size());
		CharPkTestEntity object = objects.get(0);

		try {
			Cayenne.intPKForObject(object);
			fail("intPKForObject must fail for non-numeric key");
		} catch (CayenneRuntimeException ex) {

		}
	}

	@Test
	public void testPKForObjectFailureForCompound() throws Exception {
		createOneCompoundPK();

		List<CompoundPkTestEntity> objects = ObjectSelect.query(CompoundPkTestEntity.class).select(context);
		assertEquals(1, objects.size());
		CompoundPkTestEntity object = objects.get(0);

		try {
			Cayenne.pkForObject(object);
			fail("pkForObject must fail for compound key");
		} catch (CayenneRuntimeException ex) {

		}
	}

	@Test
	public void testIntPKForObjectNonNumeric() throws Exception {
		createOneCharPK();

		List<CharPkTestEntity> objects = ObjectSelect.query(CharPkTestEntity.class).select(context);
		assertEquals(1, objects.size());
		CharPkTestEntity object = objects.get(0);

		assertEquals("CPK", Cayenne.pkForObject(object));
	}

	@Test
	public void testPaginatedColumnSelect() throws Exception {
		createCompoundPKs(20);

		List<Object[]> result = ObjectSelect.query(CompoundPkTestEntity.class)
				.columns(CompoundPkTestEntity.NAME, PropertyFactory.createSelf(CompoundPkTestEntity.class))
				.pageSize(7)
				.select(context);
		assertEquals(20, result.size());
		for(Object[] next : result) {
			assertEquals(2, next.length);
			assertEquals(String.class, next[0].getClass());
			assertEquals(CompoundPkTestEntity.class, next[1].getClass());
		}
	}

	@Test
	public void testEjbqlCountSelect() throws Exception {
		tCompoundIntPKTest.insert(1, 2, "test");
		tCompoundIntPKTest.insert(2, 3, "test");
		tCompoundIntPKTest.insert(1, 4, "test");
		tCompoundIntPKTest.insert(2, 5, "test");

		EJBQLQuery query = new EJBQLQuery("SELECT COUNT(a) FROM CompoundIntPk a");
		assertEquals(Collections.singletonList(4L), context.performQuery(query));
	}
}
