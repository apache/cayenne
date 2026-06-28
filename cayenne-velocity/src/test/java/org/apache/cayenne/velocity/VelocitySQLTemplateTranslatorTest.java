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

package org.apache.cayenne.velocity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.GenericPersistentObject;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.translator.sqltemplate.TranslatedSQL;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.dba.DbAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VelocitySQLTemplateTranslatorTest {

	private VelocitySQLTemplateTranslator processor;
	private DbAdapter adapter;

	@BeforeEach
	public void before() {
		processor = new VelocitySQLTemplateTranslator();
		adapter = mock(DbAdapter.class);
		when(adapter.preferredBindingType(anyInt())).thenAnswer(i -> i.getArgument(0));
		when(adapter.getExtendedTypes()).thenReturn(new ExtendedTypeMap());
	}

	@Test
	public void processTemplateUnchanged1() throws Exception {
		String sqlTemplate = "SELECT * FROM ME";

		TranslatedSQL compiled = processor.translate(sqlTemplate, Collections.<String, Object> emptyMap(), adapter);

		assertEquals(sqlTemplate, compiled.sql());
		assertEquals(0, compiled.bindings().length);
	}

	@Test
	public void processTemplateUnchanged2() throws Exception {
		String sqlTemplate = "SELECT a.b as XYZ FROM $SYSTEM_TABLE";

		TranslatedSQL compiled = processor.translate(sqlTemplate, Collections.<String, Object> emptyMap(), adapter);

		assertEquals(sqlTemplate, compiled.sql());
		assertEquals(0, compiled.bindings().length);
	}

	@Test
	public void processTemplateSimpleDynamicContent() throws Exception {
		String sqlTemplate = "SELECT * FROM ME WHERE $a";

		Map<String, Object> map = Collections.<String, Object> singletonMap("a", "VALUE_OF_A");
		TranslatedSQL compiled = processor.translate(sqlTemplate, map, adapter);

		assertEquals("SELECT * FROM ME WHERE VALUE_OF_A", compiled.sql());

		// bindings are not populated, since no "bind" macro is used.
		assertEquals(0, compiled.bindings().length);
	}

	@Test
	public void processTemplateBind() throws Exception {
		String sqlTemplate = "SELECT * FROM ME WHERE "
				+ "COLUMN1 = #bind($a 'VARCHAR') AND COLUMN2 = #bind($b 'INTEGER')";
		Map<String, Object> map = Collections.<String, Object> singletonMap("a", "VALUE_OF_A");
		TranslatedSQL compiled = processor.translate(sqlTemplate, map, adapter);

		assertEquals("SELECT * FROM ME WHERE COLUMN1 = ? AND COLUMN2 = ?", compiled.sql());
		assertEquals(2, compiled.bindings().length);
		assertBindingValue("VALUE_OF_A", compiled.bindings()[0]);
		assertBindingValue(null, compiled.bindings()[1]);
	}

	@Test
	public void processTemplateBindGuessVarchar() throws Exception {
		String sqlTemplate = "SELECT * FROM ME WHERE COLUMN1 = #bind($a)";
		Map<String, Object> map = Collections.<String, Object> singletonMap("a", "VALUE_OF_A");

		TranslatedSQL compiled = processor.translate(sqlTemplate, map, adapter);

		assertEquals(1, compiled.bindings().length);
		assertBindingType(Types.VARCHAR, compiled.bindings()[0]);
	}

	@Test
	public void processTemplateBindGuessInteger() throws Exception {
		String sqlTemplate = "SELECT * FROM ME WHERE COLUMN1 = #bind($a)";
		Map<String, Object> map = Collections.<String, Object> singletonMap("a", 4);

		TranslatedSQL compiled = processor.translate(sqlTemplate, map, adapter);

		assertEquals(1, compiled.bindings().length);
		assertBindingType(Types.INTEGER, compiled.bindings()[0]);
	}

	@Test
	public void processTemplateBindEqual() throws Exception {
		String sqlTemplate = "SELECT * FROM ME WHERE COLUMN #bindEqual($a 'VARCHAR')";

		TranslatedSQL compiled = processor.translate(sqlTemplate, Collections.<String, Object> emptyMap(), adapter);

		assertEquals("SELECT * FROM ME WHERE COLUMN IS NULL", compiled.sql());
		assertEquals(0, compiled.bindings().length);

		Map<String, Object> map = Collections.<String, Object> singletonMap("a", "VALUE_OF_A");

		compiled = processor.translate(sqlTemplate, map, adapter);

		assertEquals("SELECT * FROM ME WHERE COLUMN = ?", compiled.sql());
		assertEquals(1, compiled.bindings().length);
		assertBindingValue("VALUE_OF_A", compiled.bindings()[0]);
	}

	@Test
	public void processTemplateBindNotEqual() throws Exception {
		String sqlTemplate = "SELECT * FROM ME WHERE COLUMN #bindNotEqual($a 'VARCHAR')";

		TranslatedSQL compiled = processor.translate(sqlTemplate, Collections.<String, Object> emptyMap(), adapter);

		assertEquals("SELECT * FROM ME WHERE COLUMN IS NOT NULL", compiled.sql());
		assertEquals(0, compiled.bindings().length);

		Map<String, Object> map = Collections.<String, Object> singletonMap("a", "VALUE_OF_A");

		compiled = processor.translate(sqlTemplate, map, adapter);

		assertEquals("SELECT * FROM ME WHERE COLUMN <> ?", compiled.sql());
		assertEquals(1, compiled.bindings().length);
		assertBindingValue("VALUE_OF_A", compiled.bindings()[0]);
	}

	@Test
	public void processTemplateID() throws Exception {
		String sqlTemplate = "SELECT * FROM ME WHERE COLUMN1 = #bind($helper.cayenneExp($a, 'db:ID_COLUMN'))";

		Persistent persistent = new GenericPersistentObject();
		persistent.setObjectId(ObjectId.of("T", "ID_COLUMN", 5));

		Map<String, Object> map = Collections.<String, Object> singletonMap("a", persistent);

		TranslatedSQL compiled = processor.translate(sqlTemplate, map, adapter);

		assertEquals("SELECT * FROM ME WHERE COLUMN1 = ?", compiled.sql());
		assertEquals(1, compiled.bindings().length);
		assertBindingValue(5, compiled.bindings()[0]);
	}

	@Test
	public void processTemplateNotEqualID() throws Exception {
		String sqlTemplate = "SELECT * FROM ME WHERE "
				+ "COLUMN1 #bindNotEqual($helper.cayenneExp($a, 'db:ID_COLUMN1')) "
				+ "AND COLUMN2 #bindNotEqual($helper.cayenneExp($a, 'db:ID_COLUMN2'))";

		Map<String, Object> idMap = new HashMap<>();
		idMap.put("ID_COLUMN1", 3);
		idMap.put("ID_COLUMN2", "aaa");
        ObjectId id = ObjectId.of("T", idMap);
		Persistent persistent = new GenericPersistentObject();
		persistent.setObjectId(id);

		Map<String, Object> map = Collections.<String, Object> singletonMap("a", persistent);

		TranslatedSQL compiled = processor.translate(sqlTemplate, map, adapter);

		assertEquals("SELECT * FROM ME WHERE COLUMN1 <> ? AND COLUMN2 <> ?", compiled.sql());
		assertEquals(2, compiled.bindings().length);
		assertBindingValue(3, compiled.bindings()[0]);
		assertBindingValue("aaa", compiled.bindings()[1]);
	}

	@Test
	public void processTemplateConditions() throws Exception {
		String sqlTemplate = "SELECT * FROM ME #if($a) WHERE COLUMN1 > #bind($a)#end";

		Map<String, Object> map = Collections.<String, Object> singletonMap("a", "VALUE_OF_A");

		TranslatedSQL compiled = processor.translate(sqlTemplate, map, adapter);

		assertEquals("SELECT * FROM ME  WHERE COLUMN1 > ?", compiled.sql());
		assertEquals(1, compiled.bindings().length);
		assertBindingValue("VALUE_OF_A", compiled.bindings()[0]);

		compiled = processor.translate(sqlTemplate, Collections.<String, Object> emptyMap(), adapter);

		assertEquals("SELECT * FROM ME ", compiled.sql());
		assertEquals(0, compiled.bindings().length);
	}

	@Test
	public void processTemplateBindCollection() throws Exception {
		String sqlTemplate = "SELECT * FROM ME WHERE COLUMN IN (#bind($list 'VARCHAR'))";

		Map<String, Object> map = Collections.<String, Object> singletonMap("list", Arrays.asList("a", "b", "c"));
		TranslatedSQL compiled = new VelocitySQLTemplateTranslator().translate(sqlTemplate, map, adapter);

		assertEquals("SELECT * FROM ME WHERE COLUMN IN (?,?,?)", compiled.sql());
		assertEquals(3, compiled.bindings().length);

		compiled = processor.translate(sqlTemplate, map, adapter);
		assertBindingValue("a", compiled.bindings()[0]);
		assertBindingValue("b", compiled.bindings()[1]);
		assertBindingValue("c", compiled.bindings()[2]);
	}

	@Test
	public void unknownDirective() throws Exception {
		String sqlTemplate = "SELECT #from(1) FROM a";
		TranslatedSQL compiled = processor.translate(sqlTemplate, Collections.emptyMap(), adapter);
        assertEquals("SELECT #from(1) FROM a", compiled.sql());
	}

	private void assertBindingValue(Object expectedValue, Object binding) {
		assertTrue(binding instanceof ParameterBinding, "Not a binding!");
		assertEquals(expectedValue, ((ParameterBinding) binding).value());
	}

	private void assertBindingType(Integer expectedType, Object binding) {
		assertTrue(binding instanceof ParameterBinding, "Not a binding!");
		assertEquals(expectedType, ((ParameterBinding) binding).jdbcType());
	}
}
