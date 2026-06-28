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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.translator.sqltemplate.TranslatedSQL;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.dba.DbAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VelocitySQLTemplateTranslator_SelectTest {

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
	public void processTemplateUnchanged() throws Exception {
		String sqlTemplate = "SELECT * FROM ME";

		TranslatedSQL compiled = processor.translate(sqlTemplate, Collections.<String, Object> emptyMap(), adapter);

		assertEquals(sqlTemplate, compiled.sql());
		assertEquals(0, compiled.bindings().length);
		assertEquals(0, compiled.resultColumns().length);
	}

	@Test
	public void processSelectTemplate1() throws Exception {
		String sqlTemplate = "SELECT #result('A') FROM ME";

		TranslatedSQL compiled = processor.translate(sqlTemplate, Collections.<String, Object> emptyMap(), adapter);

		assertEquals("SELECT A FROM ME", compiled.sql());
		assertEquals(0, compiled.bindings().length);
		assertEquals(1, compiled.resultColumns().length);
		assertEquals("A", compiled.resultColumns()[0].name());
		assertNull(compiled.resultColumns()[0].javaClass());
	}

	@Test
	public void processSelectTemplate2() throws Exception {
		String sqlTemplate = "SELECT #result('A' 'String') FROM ME";

		TranslatedSQL compiled = processor.translate(sqlTemplate, Collections.<String, Object> emptyMap(), adapter);

		assertEquals("SELECT A FROM ME", compiled.sql());
		assertEquals(0, compiled.bindings().length);

		assertEquals(1, compiled.resultColumns().length);
		assertEquals("A", compiled.resultColumns()[0].name());
		assertEquals("java.lang.String", compiled.resultColumns()[0].javaClass());
	}

	@Test
	public void processSelectTemplate3() throws Exception {
		String sqlTemplate = "SELECT #result('A' 'String' 'B') FROM ME";

		TranslatedSQL compiled = processor.translate(sqlTemplate, Collections.<String, Object> emptyMap(), adapter);

		assertEquals("SELECT A AS B FROM ME", compiled.sql());
		assertEquals(0, compiled.bindings().length);

		assertEquals(1, compiled.resultColumns().length);
		ColumnDescriptor column = compiled.resultColumns()[0];
		assertEquals("A", column.name());
		assertEquals("B", column.dataRowKey());
		assertEquals("java.lang.String", column.javaClass());
	}

	@Test
	public void processSelectTemplate4() throws Exception {
		String sqlTemplate = "SELECT #result('A'), #result('B'), #result('C') FROM ME";

		TranslatedSQL compiled = processor.translate(sqlTemplate, Collections.<String, Object> emptyMap(), adapter);

		assertEquals("SELECT A, B, C FROM ME", compiled.sql());
		assertEquals(0, compiled.bindings().length);

		assertEquals(3, compiled.resultColumns().length);
		assertEquals("A", compiled.resultColumns()[0].name());
		assertEquals("B", compiled.resultColumns()[1].name());
		assertEquals("C", compiled.resultColumns()[2].name());
	}
}
