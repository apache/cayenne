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

import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.compound.CharPkTestEntity;
import org.apache.cayenne.testdo.compound.CompoundPkTestEntity;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CayenneCompoundIT {

	@RegisterExtension
	static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.COMPOUND_PROJECT);

	private TableHelper tCompoundPKTest;
	private TableHelper tCharPKTest;
	private TableHelper tCompoundIntPKTest;

	@BeforeEach
	public void setUp() throws Exception {
		tCompoundPKTest = env.table("COMPOUND_PK_TEST", "KEY1", "KEY2", "NAME");

		tCharPKTest = env.table("CHAR_PK_TEST", "PK_COL", "OTHER_COL");

		tCompoundIntPKTest = env.table("COMPOUND_INT_PK", "id1", "id2", "name");
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
	public void objectForPKEntityMapCompound() throws Exception {
		createOneCompoundPK();

		Map<String, Object> pk = new HashMap<>();
		pk.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "PK1");
		pk.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "PK2");
		CompoundPkTestEntity object = Cayenne.objectForPK(env.context(), CompoundPkTestEntity.class, pk);

		assertNotNull(object);
		assertEquals("BBB", object.getName());
	}

	@Test
	public void compoundPKForObject() throws Exception {
		createOneCompoundPK();

		List<CompoundPkTestEntity> objects = ObjectSelect.query(CompoundPkTestEntity.class).select(env.context());
		assertEquals(1, objects.size());
		CompoundPkTestEntity object = objects.get(0);

		Map<String, Object> pk = Cayenne.compoundPKForObject(object);
		assertNotNull(pk);
		assertEquals(2, pk.size());
		assertEquals("PK1", pk.get(CompoundPkTestEntity.KEY1_PK_COLUMN));
		assertEquals("PK2", pk.get(CompoundPkTestEntity.KEY2_PK_COLUMN));
	}

	@Test
	public void intPKForObjectFailureForCompound() throws Exception {
		createOneCompoundPK();

		List<CompoundPkTestEntity> objects = ObjectSelect.query(CompoundPkTestEntity.class).select(env.context());
		assertEquals(1, objects.size());
		CompoundPkTestEntity object = objects.get(0);

		assertThrows(CayenneRuntimeException.class, () -> Cayenne.intPKForObject(object));
	}

	@Test
	public void intPKForObjectFailureForNonNumeric() throws Exception {
		createOneCharPK();

		List<CharPkTestEntity> objects = ObjectSelect.query(CharPkTestEntity.class).select(env.context());
		assertEquals(1, objects.size());
		CharPkTestEntity object = objects.get(0);

		assertThrows(CayenneRuntimeException.class, () -> Cayenne.intPKForObject(object));
	}

	@Test
	public void pkForObjectFailureForCompound() throws Exception {
		createOneCompoundPK();

		List<CompoundPkTestEntity> objects = ObjectSelect.query(CompoundPkTestEntity.class).select(env.context());
		assertEquals(1, objects.size());
		CompoundPkTestEntity object = objects.get(0);

		assertThrows(CayenneRuntimeException.class, () -> Cayenne.pkForObject(object));
	}

	@Test
	public void intPKForObjectNonNumeric() throws Exception {
		createOneCharPK();

		List<CharPkTestEntity> objects = ObjectSelect.query(CharPkTestEntity.class).select(env.context());
		assertEquals(1, objects.size());
		CharPkTestEntity object = objects.get(0);

		assertEquals("CPK", Cayenne.pkForObject(object));
	}

	@Test
	public void paginatedColumnSelect() throws Exception {
		createCompoundPKs(20);

		List<Object[]> result = ObjectSelect.query(CompoundPkTestEntity.class)
				.columns(CompoundPkTestEntity.NAME, PropertyFactory.createSelf(CompoundPkTestEntity.class))
				.pageSize(7)
				.select(env.context());
		assertEquals(20, result.size());
		for(Object[] next : result) {
			assertEquals(2, next.length);
			assertEquals(String.class, next[0].getClass());
			assertEquals(CompoundPkTestEntity.class, next[1].getClass());
		}
	}

	@Test
	public void ejbqlCountSelect() throws Exception {
		tCompoundIntPKTest.insert(1, 2, "test");
		tCompoundIntPKTest.insert(2, 3, "test");
		tCompoundIntPKTest.insert(1, 4, "test");
		tCompoundIntPKTest.insert(2, 5, "test");

		EJBQLQuery query = new EJBQLQuery("SELECT COUNT(a) FROM CompoundIntPk a");
		assertEquals(Collections.singletonList(4L), env.context().performQuery(query));
	}
}
