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
package org.apache.cayenne.gen;

import org.apache.cayenne.map.CallbackDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.QueryDescriptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ClassGenerationActionTest {

	protected ClassGenerationAction action;
	protected Collection<StringWriter> writers;

	@Before
	public void setUp() throws Exception {
		this.writers = new ArrayList<StringWriter>(3);
		this.action = new ClassGenerationAction() {

			@Override
			protected Writer openWriter(TemplateType templateType) throws Exception {
				StringWriter writer = new StringWriter();
				writers.add(writer);
				return writer;
			}
		};
	}

	@After
	public void tearDown() throws Exception {
		action = null;
		writers = null;
	}

	@Test
	public void testExecuteArtifactPairsImports() throws Exception {

		ObjEntity testEntity1 = new ObjEntity("TE1");
		testEntity1.setClassName("org.example.TestClass1");

		action.setMakePairs(true);
		action.setSuperPkg("org.example.auto");

		List<String> generated = execute(new EntityArtifact(testEntity1));
		assertNotNull(generated);
		assertEquals(2, generated.size());

		String superclass = generated.get(0);
		assertTrue(superclass, superclass.contains("package org.example.auto;"));
		assertTrue(superclass, superclass.contains("import org.apache.cayenne.CayenneDataObject;"));

		String subclass = generated.get(1);
		assertTrue(subclass, subclass.contains("package org.example;"));
		assertTrue(subclass, subclass.contains("import org.example.auto._TestClass1;"));
	}

	@Test
	public void testExecuteArtifactPairsMapRelationships() throws Exception {

		ObjEntity testEntity1 = new ObjEntity("TE1");
		testEntity1.setClassName("org.example.TestClass1");

		final ObjEntity testEntity2 = new ObjEntity("TE1");
		testEntity2.setClassName("org.example.TestClass2");

		ObjRelationship relationship = new ObjRelationship("xMap") {

			private static final long serialVersionUID = 8042147877503405974L;

			@Override
			public boolean isToMany() {
				return true;
			}

			@Override
			public ObjEntity getTargetEntity() {
				return testEntity2;
			}
		};
		relationship.setCollectionType("java.util.Map");
		testEntity1.addRelationship(relationship);

		action.setMakePairs(true);

		List<String> generated = execute(new EntityArtifact(testEntity1));
		assertNotNull(generated);
		assertEquals(2, generated.size());

		String superclass = generated.get(0);
		assertTrue(superclass, superclass.contains("import java.util.Map;"));
	}

	@Test
	public void testExecuteArtifactPairsAttribute() throws Exception {

		ObjEntity testEntity1 = new ObjEntity("TE1");
		testEntity1.setClassName("org.example.TestClass1");

		ObjAttribute attr = new ObjAttribute();
		attr.setName("ID");
		attr.setType("int");

		ObjAttribute attr1 = new ObjAttribute();
		attr1.setName("name");
		attr1.setType("char");

		testEntity1.addAttribute(attr);
		testEntity1.addAttribute(attr1);

		action.setMakePairs(true);

		List<String> generated = execute(new EntityArtifact(testEntity1));
		assertNotNull(generated);
		assertEquals(2, generated.size());
		String superclass = generated.get(0);

		assertTrue(superclass, superclass.contains("public void setID(int ID)"));
		assertTrue(superclass, superclass.contains("writeProperty(\"ID\", ID);"));

		assertTrue(superclass, superclass.contains("public int getID()"));
		assertTrue(superclass, superclass.contains("Object value = readProperty(\"ID\");"));
		assertTrue(superclass, superclass.contains("return (value != null) ? (Integer) value : 0;"));

		assertTrue(superclass, superclass.contains("public void setName(char name)"));
		assertTrue(superclass, superclass.contains("writeProperty(\"name\", name);"));

		assertTrue(superclass, superclass.contains("public char getName()"));
		assertTrue(superclass, superclass.contains("Object value = readProperty(\"name\");"));
		assertTrue(superclass, superclass.contains("return (value != null) ? (Character) value : 0;"));

	}

	@Test
	public void testExecuteDataMapQueryNames() throws Exception {
		runDataMapTest(false);
	}

	@Test
	public void testExecuteClientDataMapQueryNames() throws Exception {
		runDataMapTest(true);
	}

	private void runDataMapTest(boolean client) throws Exception {
		QueryDescriptor descriptor = QueryDescriptor.selectQueryDescriptor();
        descriptor.setName("TestQuery");

		DataMap map = new DataMap();
		map.addQueryDescriptor(descriptor);
		map.setName("testmap");
		List<String> generated;
		if (client) {
			map.setDefaultClientPackage("testpackage");
			generated = execute(new ClientDataMapArtifact(map, map.getQueryDescriptors()));
		} else {
			map.setDefaultPackage("testpackage");
			generated = execute(new DataMapArtifact(map, map.getQueryDescriptors()));
		}
		assertEquals(2, generated.size());
		assertTrue(generated.get(0).contains("public static final String TEST_QUERY_QUERYNAME = \"TestQuery\""));
	}

	@Test
	public void testCallbackMethodGeneration() throws Exception {
		assertCallbacks(false);
	}

	@Test
	public void testClientCallbackMethodGeneration() throws Exception {
		assertCallbacks(true);
	}

	private void assertCallbacks(boolean isClient) throws Exception {
		ObjEntity testEntity1 = new ObjEntity("TE1");
		testEntity1.setClassName("org.example.TestClass1");
		int i = 0;
		for (CallbackDescriptor cb : testEntity1.getCallbackMap().getCallbacks()) {
			cb.addCallbackMethod("cb" + i++);
		}

		if (isClient) {

			action = new ClientClassGenerationAction() {
				@Override
				protected Writer openWriter(TemplateType templateType) throws Exception {
					StringWriter writer = new StringWriter();
					writers.add(writer);
					return writer;
				}

			};

		}

		action.setMakePairs(true);

		List<String> generated = execute(new EntityArtifact(testEntity1));
		assertNotNull(generated);
		assertEquals(2, generated.size());

		String superclass = generated.get(0);

		assertTrue(superclass, superclass.contains("public abstract class _TestClass1"));

		for (int j = 0; j < i; j++) {
			assertTrue(superclass, superclass.contains("protected abstract void cb" + j + "();"));
		}

		String subclass = generated.get(1);
		for (int j = 0; j < i; j++) {
			assertTrue(subclass, subclass.contains("protected void cb" + j + "() {"));
		}
	}

	protected List<String> execute(Artifact artifact) throws Exception {

		action.execute(artifact);

		List<String> strings = new ArrayList<String>(writers.size());
		for (StringWriter writer : writers) {
			strings.add(writer.toString());
		}
		return strings;
	}
}
