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

package org.apache.cayenne.gen;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.cayenne.gen.mock.TestClassGenerationAction;
import org.apache.cayenne.map.CallbackDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.QueryDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

public class ClassGenerationActionTest extends CgenCase {

	@TempDir
	public File tempFolder;

	protected ClassGenerationAction action;
	protected Collection<StringWriter> writers;

	protected CgenConfiguration cgenConfiguration;

	@BeforeEach
	public void setUp() throws Exception {
		writers = new ArrayList<>(3);
		cgenConfiguration = new CgenConfiguration();
		action = new TestClassGenerationAction(getUnitTestInjector().getInstance(ClassGenerationActionFactory.class)
				.createAction(cgenConfiguration), writers);
	}

	@AfterEach
	public void tearDown() throws Exception {
		action = null;
		writers = null;
	}

	@Test
	public void executeArtifactPairsImports() throws Exception {

		ObjEntity testEntity1 = new ObjEntity("TE1");
		testEntity1.setClassName("org.example.TestClass1");

		cgenConfiguration.setMakePairs(true);
		cgenConfiguration.setSuperPkg("org.example.auto");

		List<String> generated = execute(new EntityArtifact(testEntity1));
		assertNotNull(generated);
		assertEquals(2, generated.size());

		String superclass = generated.get(0);
		assertTrue(superclass.contains("package org.example.auto;"), superclass);
		assertTrue(superclass.contains("import org.apache.cayenne.PersistentObject;"), superclass);

		String subclass = generated.get(1);
		assertTrue(subclass.contains("package org.example;"), subclass);
		assertTrue(subclass.contains("import org.example.auto._TestClass1;"), subclass);
	}

	@Test
	public void executeArtifactPairsMapRelationships() throws Exception {

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

		cgenConfiguration.setMakePairs(true);

		List<String> generated = execute(new EntityArtifact(testEntity1));
		assertNotNull(generated);
		assertEquals(2, generated.size());

		String superclass = generated.get(0);
		assertTrue(superclass.contains("import java.util.Map;"), superclass);
	}

	@Test
	public void executeArtifactPairsAttribute() throws Exception {

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

		cgenConfiguration.setMakePairs(true);

		List<String> generated = execute(new EntityArtifact(testEntity1));
		assertNotNull(generated);
		assertEquals(2, generated.size());
		String superclass = generated.get(0);

		assertTrue(superclass.contains("public void setID(int ID)"), superclass);
		assertTrue(superclass.contains("this.ID = ID;"), superclass);

		assertTrue(superclass.contains("public int getID()"), superclass);
		assertTrue(superclass.contains("return this.ID;"), superclass);

		assertTrue(superclass.contains("public void setName(char name)"), superclass);
		assertTrue(superclass.contains("this.name = name;"), superclass);

		assertTrue(superclass.contains("public char getName()"), superclass);
		assertTrue(superclass.contains("return this.name;"), superclass);

	}

	@Test
	public void executeDataMapQueryNames() throws Exception {
		runDataMapTest();
	}

	private void runDataMapTest() throws Exception {
		QueryDescriptor descriptor = QueryDescriptor.selectQueryDescriptor();
		descriptor.setName("TestQuery");

		DataMap map = new DataMap();
		map.addQueryDescriptor(descriptor);
		map.setName("testmap");
		List<String> generated;
		map.setDefaultPackage("testpackage");
		generated = execute(new DataMapArtifact(map, map.getQueryDescriptors()));
		assertEquals(2, generated.size());
		assertTrue(generated.get(0).contains("public static final String TEST_QUERY_QUERYNAME = \"TestQuery\""));
	}

	@Test
	public void callbackMethodGeneration() throws Exception {
		assertCallbacks();
	}

	private void assertCallbacks() throws Exception {
		ObjEntity testEntity1 = new ObjEntity("TE1");
		testEntity1.setClassName("org.example.TestClass1");
		int i = 0;
		for (CallbackDescriptor cb : testEntity1.getCallbackMap().getCallbacks()) {
			cb.addCallbackMethod("cb" + i++);
		}

		cgenConfiguration.setMakePairs(true);

		List<String> generated = execute(new EntityArtifact(testEntity1));
		assertNotNull(generated);
		assertEquals(2, generated.size());

		String superclass = generated.get(0);

		assertTrue(superclass.contains("public abstract class _TestClass1"), superclass);

		for (int j = 0; j < i; j++) {
			assertTrue(superclass.contains("protected abstract void cb" + j + "();"), superclass);
		}

		String subclass = generated.get(1);
		for (int j = 0; j < i; j++) {
			assertTrue(subclass.contains("protected void cb" + j + "() {"), subclass);
		}
	}

	protected List<String> execute(Artifact artifact) throws Exception {

		action.execute(artifact);

		List<String> strings = new ArrayList<>(writers.size());
		for (StringWriter writer : writers) {
			strings.add(writer.toString());
		}
		return strings;
	}

	@Test
	public void fileForSuperclass() throws Exception {

		TemplateType templateType = TemplateType.DATAMAP_SUPERCLASS;

		cgenConfiguration.setRootPath(tempFolder.toPath());
		cgenConfiguration.updateOutputPath(Paths.get("."));
		action = new ClassGenerationAction(cgenConfiguration);
		ObjEntity testEntity1 = new ObjEntity("TEST");
		testEntity1.setClassName("TestClass1");
		action.context.put(Artifact.SUPER_PACKAGE_KEY, "");
		action.context.put(Artifact.SUPER_CLASS_KEY, "TestClass1");

		File outFile = new File(tempFolder + "/TestClass1.java");
		assertFalse(outFile.exists());

		try(Writer out = action.openWriter(templateType)) {
			out.write("// generated");
			// the file is written on close, not on open
			assertFalse(outFile.exists());
		}
		assertEquals("// generated", Files.readString(outFile.toPath()));

		// superclasses are regenerated unconditionally
		try(Writer out = action.openWriter(templateType)) {
			assertNotNull(out);
			out.write("// regenerated");
		}
		assertEquals("// regenerated", Files.readString(outFile.toPath()));
	}

	@Test
	public void unchangedFileLeftAlone() throws Exception {

		TemplateType templateType = TemplateType.DATAMAP_SUPERCLASS;

		cgenConfiguration.setRootPath(tempFolder.toPath());
		cgenConfiguration.updateOutputPath(Paths.get("."));
		action = new ClassGenerationAction(cgenConfiguration);
		action.context.put(Artifact.SUPER_PACKAGE_KEY, "");
		action.context.put(Artifact.SUPER_CLASS_KEY, "TestClass1");

		File outFile = new File(tempFolder + "/TestClass1.java");
		try(Writer out = action.openWriter(templateType)) {
			out.write("// generated");
		}

		// backdate the file, so that an actual rewrite would be detectable
		assertTrue(outFile.setLastModified(outFile.lastModified() - 5_000L));
		long backdated = outFile.lastModified();

		try(Writer out = action.openWriter(templateType)) {
			out.write("// generated");
		}

		assertEquals(backdated, outFile.lastModified(), "Identical file must not be rewritten");
		assertEquals("// generated", Files.readString(outFile.toPath()));
	}

	@Test
	public void fileForClass() throws Exception {

		TemplateType templateType = TemplateType.DATAMAP_SINGLE_CLASS;

		cgenConfiguration.setRootPath(tempFolder.toPath());
		cgenConfiguration.updateOutputPath(Paths.get("."));
		action = new ClassGenerationAction(cgenConfiguration);
		ObjEntity testEntity1 = new ObjEntity("TEST");
		testEntity1.setClassName("TestClass1");
		action.context.put(Artifact.SUB_PACKAGE_KEY, "");
		action.context.put(Artifact.SUB_CLASS_KEY, "TestClass1");

		File outFile = new File(tempFolder + "/TestClass1.java");
		assertFalse(outFile.exists());

		try(Writer out = action.openWriter(templateType)) {
			out.write("// generated");
		}
		assertTrue(outFile.exists());

		// existing subclasses are preserved when generating class pairs...
		assertNull(action.openWriter(templateType));

		// ... and when generating single classes without "overwrite"
		cgenConfiguration.setMakePairs(false);
		assertNull(action.openWriter(templateType));

		// "overwrite" regenerates the subclass unconditionally
		cgenConfiguration.setOverwrite(true);
		try(Writer out = action.openWriter(templateType)) {
			assertNotNull(out);
			out.write("// regenerated");
		}
		assertEquals("// regenerated", Files.readString(outFile.toPath()));
	}
}
