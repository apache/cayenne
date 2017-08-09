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

package org.apache.cayenne.velocity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.jdbc.SQLStatement;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.junit.Before;
import org.junit.Test;

public class VelocitySQLTemplateProcessorTest {

	private VelocitySQLTemplateProcessor processor;

	@Before
	public void before() {
		processor = new VelocitySQLTemplateProcessor();
	}

	@Test
	public void testProcessTemplateUnchanged1() throws Exception {
		String sqlTemplate = "SELECT * FROM ME";

		SQLStatement compiled = processor.processTemplate(sqlTemplate, Collections.<String, Object> emptyMap());

		assertEquals(sqlTemplate, compiled.getSql());
		assertEquals(0, compiled.getBindings().length);
	}

	@Test
	public void testProcessTemplateUnchanged2() throws Exception {
		String sqlTemplate = "SELECT a.b as XYZ FROM $SYSTEM_TABLE";

		SQLStatement compiled = processor.processTemplate(sqlTemplate, Collections.<String, Object> emptyMap());

		assertEquals(sqlTemplate, compiled.getSql());
		assertEquals(0, compiled.getBindings().length);
	}

	@Test
	public void testProcessTemplateSimpleDynamicContent() throws Exception {
		String sqlTemplate = "SELECT * FROM ME WHERE $a";

		Map<String, Object> map = Collections.<String, Object> singletonMap("a", "VALUE_OF_A");
		SQLStatement compiled = processor.processTemplate(sqlTemplate, map);

		assertEquals("SELECT * FROM ME WHERE VALUE_OF_A", compiled.getSql());

		// bindings are not populated, since no "bind" macro is used.
		assertEquals(0, compiled.getBindings().length);
	}

	@Test
	public void testProcessTemplateBind() throws Exception {
		String sqlTemplate = "SELECT * FROM ME WHERE "
				+ "COLUMN1 = #bind($a 'VARCHAR') AND COLUMN2 = #bind($b 'INTEGER')";
		Map<String, Object> map = Collections.<String, Object> singletonMap("a", "VALUE_OF_A");
		SQLStatement compiled = processor.processTemplate(sqlTemplate, map);

		assertEquals("SELECT * FROM ME WHERE COLUMN1 = ? AND COLUMN2 = ?", compiled.getSql());
		assertEquals(2, compiled.getBindings().length);
		assertBindingValue("VALUE_OF_A", compiled.getBindings()[0]);
		assertBindingValue(null, compiled.getBindings()[1]);
	}

	@Test
	public void testProcessTemplateBindGuessVarchar() throws Exception {
		String sqlTemplate = "SELECT * FROM ME WHERE COLUMN1 = #bind($a)";
		Map<String, Object> map = Collections.<String, Object> singletonMap("a", "VALUE_OF_A");

		SQLStatement compiled = processor.processTemplate(sqlTemplate, map);

		assertEquals(1, compiled.getBindings().length);
		assertBindingType(Types.VARCHAR, compiled.getBindings()[0]);
	}

	@Test
	public void testProcessTemplateBindGuessInteger() throws Exception {
		String sqlTemplate = "SELECT * FROM ME WHERE COLUMN1 = #bind($a)";
		Map<String, Object> map = Collections.<String, Object> singletonMap("a", 4);

		SQLStatement compiled = processor.processTemplate(sqlTemplate, map);

		assertEquals(1, compiled.getBindings().length);
		assertBindingType(Types.INTEGER, compiled.getBindings()[0]);
	}

	@Test
	public void testProcessTemplateBindEqual() throws Exception {
		String sqlTemplate = "SELECT * FROM ME WHERE COLUMN #bindEqual($a 'VARCHAR')";

		SQLStatement compiled = processor.processTemplate(sqlTemplate, Collections.<String, Object> emptyMap());

		assertEquals("SELECT * FROM ME WHERE COLUMN IS NULL", compiled.getSql());
		assertEquals(0, compiled.getBindings().length);

		Map<String, Object> map = Collections.<String, Object> singletonMap("a", "VALUE_OF_A");

		compiled = processor.processTemplate(sqlTemplate, map);

		assertEquals("SELECT * FROM ME WHERE COLUMN = ?", compiled.getSql());
		assertEquals(1, compiled.getBindings().length);
		assertBindingValue("VALUE_OF_A", compiled.getBindings()[0]);
	}

	@Test
	public void testProcessTemplateBindNotEqual() throws Exception {
		String sqlTemplate = "SELECT * FROM ME WHERE COLUMN #bindNotEqual($a 'VARCHAR')";

		SQLStatement compiled = processor.processTemplate(sqlTemplate, Collections.<String, Object> emptyMap());

		assertEquals("SELECT * FROM ME WHERE COLUMN IS NOT NULL", compiled.getSql());
		assertEquals(0, compiled.getBindings().length);

		Map<String, Object> map = Collections.<String, Object> singletonMap("a", "VALUE_OF_A");

		compiled = processor.processTemplate(sqlTemplate, map);

		assertEquals("SELECT * FROM ME WHERE COLUMN <> ?", compiled.getSql());
		assertEquals(1, compiled.getBindings().length);
		assertBindingValue("VALUE_OF_A", compiled.getBindings()[0]);
	}

	@Test
	public void testProcessTemplateID() throws Exception {
		String sqlTemplate = "SELECT * FROM ME WHERE COLUMN1 = #bind($helper.cayenneExp($a, 'db:ID_COLUMN'))";

		DataObject dataObject = new CayenneDataObject();
		dataObject.setObjectId(new ObjectId("T", "ID_COLUMN", 5));

		Map<String, Object> map = Collections.<String, Object> singletonMap("a", dataObject);

		SQLStatement compiled = processor.processTemplate(sqlTemplate, map);

		assertEquals("SELECT * FROM ME WHERE COLUMN1 = ?", compiled.getSql());
		assertEquals(1, compiled.getBindings().length);
		assertBindingValue(new Integer(5), compiled.getBindings()[0]);
	}

	@Test
	public void testProcessTemplateNotEqualID() throws Exception {
		String sqlTemplate = "SELECT * FROM ME WHERE "
				+ "COLUMN1 #bindNotEqual($helper.cayenneExp($a, 'db:ID_COLUMN1')) "
				+ "AND COLUMN2 #bindNotEqual($helper.cayenneExp($a, 'db:ID_COLUMN2'))";

		Map<String, Object> idMap = new HashMap<>();
		idMap.put("ID_COLUMN1", new Integer(3));
		idMap.put("ID_COLUMN2", "aaa");
		ObjectId id = new ObjectId("T", idMap);
		DataObject dataObject = new CayenneDataObject();
		dataObject.setObjectId(id);

		Map<String, Object> map = Collections.<String, Object> singletonMap("a", dataObject);

		SQLStatement compiled = processor.processTemplate(sqlTemplate, map);

		assertEquals("SELECT * FROM ME WHERE COLUMN1 <> ? AND COLUMN2 <> ?", compiled.getSql());
		assertEquals(2, compiled.getBindings().length);
		assertBindingValue(new Integer(3), compiled.getBindings()[0]);
		assertBindingValue("aaa", compiled.getBindings()[1]);
	}

	@Test
	public void testProcessTemplateConditions() throws Exception {
		String sqlTemplate = "SELECT * FROM ME #if($a) WHERE COLUMN1 > #bind($a)#end";

		Map<String, Object> map = Collections.<String, Object> singletonMap("a", "VALUE_OF_A");

		SQLStatement compiled = processor.processTemplate(sqlTemplate, map);

		assertEquals("SELECT * FROM ME  WHERE COLUMN1 > ?", compiled.getSql());
		assertEquals(1, compiled.getBindings().length);
		assertBindingValue("VALUE_OF_A", compiled.getBindings()[0]);

		compiled = processor.processTemplate(sqlTemplate, Collections.<String, Object> emptyMap());

		assertEquals("SELECT * FROM ME ", compiled.getSql());
		assertEquals(0, compiled.getBindings().length);
	}

	@Test
	public void testProcessTemplateBindCollection() throws Exception {
		String sqlTemplate = "SELECT * FROM ME WHERE COLUMN IN (#bind($list 'VARCHAR'))";

		Map<String, Object> map = Collections.<String, Object> singletonMap("list", Arrays.asList("a", "b", "c"));
		SQLStatement compiled = new VelocitySQLTemplateProcessor().processTemplate(sqlTemplate, map);

		assertEquals("SELECT * FROM ME WHERE COLUMN IN (?,?,?)", compiled.getSql());
		assertEquals(3, compiled.getBindings().length);

		compiled = processor.processTemplate(sqlTemplate, map);
		assertBindingValue("a", compiled.getBindings()[0]);
		assertBindingValue("b", compiled.getBindings()[1]);
		assertBindingValue("c", compiled.getBindings()[2]);
	}

	private void assertBindingValue(Object expectedValue, Object binding) {
		assertTrue("Not a binding!", binding instanceof ParameterBinding);
		assertEquals(expectedValue, ((ParameterBinding) binding).getValue());
	}

	private void assertBindingType(Integer expectedType, Object binding) {
		assertTrue("Not a binding!", binding instanceof ParameterBinding);
		assertEquals(expectedType, ((ParameterBinding) binding).getJdbcType());
	}
}
