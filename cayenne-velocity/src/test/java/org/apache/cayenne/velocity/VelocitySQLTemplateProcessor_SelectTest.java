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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.jdbc.SQLStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VelocitySQLTemplateProcessor_SelectTest {

	private VelocitySQLTemplateProcessor processor;

	@BeforeEach
	public void before() {
		processor = new VelocitySQLTemplateProcessor();
	}

	@Test
	public void processTemplateUnchanged() throws Exception {
		String sqlTemplate = "SELECT * FROM ME";

		SQLStatement compiled = processor.processTemplate(sqlTemplate, Collections.<String, Object> emptyMap());

		assertEquals(sqlTemplate, compiled.sql());
		assertEquals(0, compiled.bindings().length);
		assertEquals(0, compiled.resultColumns().length);
	}

	@Test
	public void processSelectTemplate1() throws Exception {
		String sqlTemplate = "SELECT #result('A') FROM ME";

		SQLStatement compiled = processor.processTemplate(sqlTemplate, Collections.<String, Object> emptyMap());

		assertEquals("SELECT A FROM ME", compiled.sql());
		assertEquals(0, compiled.bindings().length);
		assertEquals(1, compiled.resultColumns().length);
		assertEquals("A", compiled.resultColumns()[0].getName());
		assertNull(compiled.resultColumns()[0].getJavaClass());
	}

	@Test
	public void processSelectTemplate2() throws Exception {
		String sqlTemplate = "SELECT #result('A' 'String') FROM ME";

		SQLStatement compiled = processor.processTemplate(sqlTemplate, Collections.<String, Object> emptyMap());

		assertEquals("SELECT A FROM ME", compiled.sql());
		assertEquals(0, compiled.bindings().length);

		assertEquals(1, compiled.resultColumns().length);
		assertEquals("A", compiled.resultColumns()[0].getName());
		assertEquals("java.lang.String", compiled.resultColumns()[0].getJavaClass());
	}

	@Test
	public void processSelectTemplate3() throws Exception {
		String sqlTemplate = "SELECT #result('A' 'String' 'B') FROM ME";

		SQLStatement compiled = processor.processTemplate(sqlTemplate, Collections.<String, Object> emptyMap());

		assertEquals("SELECT A AS B FROM ME", compiled.sql());
		assertEquals(0, compiled.bindings().length);

		assertEquals(1, compiled.resultColumns().length);
		ColumnDescriptor column = compiled.resultColumns()[0];
		assertEquals("A", column.getName());
		assertEquals("B", column.getDataRowKey());
		assertEquals("java.lang.String", column.getJavaClass());
	}

	@Test
	public void processSelectTemplate4() throws Exception {
		String sqlTemplate = "SELECT #result('A'), #result('B'), #result('C') FROM ME";

		SQLStatement compiled = processor.processTemplate(sqlTemplate, Collections.<String, Object> emptyMap());

		assertEquals("SELECT A, B, C FROM ME", compiled.sql());
		assertEquals(0, compiled.bindings().length);

		assertEquals(3, compiled.resultColumns().length);
		assertEquals("A", compiled.resultColumns()[0].getName());
		assertEquals("B", compiled.resultColumns()[1].getName());
		assertEquals("C", compiled.resultColumns()[2].getName());
	}
}
