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
package org.apache.cayenne.ejbql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class EJBQLParser_CompileIT extends ServerCase {

	@Inject
	protected ServerRuntime runtime;

	private EJBQLParser parser;
	private EntityResolver resolver;

	@Before
	public void before() {
		resolver = runtime.getDataDomain().getEntityResolver();
		parser = EJBQLParserFactory.getParser();
	}

	@Test
	public void testGetSource() {
		String source = "select a from Artist a";

		EJBQLCompiledExpression select = parser.compile(source, resolver);
		assertEquals(source, select.getSource());
	}

	@Test
	public void testGetExpression() {
		String source = "select a from Artist a";
		EJBQLCompiledExpression select = parser.compile(source, resolver);
		assertNotNull(select.getExpression());
	}

	@Test
	public void testGetEntityDescriptor() {

		EJBQLCompiledExpression select = parser.compile("select a from Artist a", resolver);

		assertNotNull(select.getEntityDescriptor("a"));
		assertSame(resolver.getClassDescriptor("Artist"), select.getEntityDescriptor("a"));

		EJBQLCompiledExpression select1 = parser.compile("select p from Painting p WHERE p.toArtist.artistName = 'a'",
				resolver);
		assertNotNull(select1.getEntityDescriptor("p"));
		assertSame(resolver.getClassDescriptor("Painting"), select1.getEntityDescriptor("p"));

		assertNotNull(select1.getEntityDescriptor("p.toArtist"));
		assertSame(resolver.getClassDescriptor("Artist"), select1.getEntityDescriptor("p.toArtist"));
	}

	@Test
	public void testGetRootDescriptor() {

		EJBQLCompiledExpression select = parser.compile("select a from Artist a", resolver);

		assertSame("Root is not detected: " + select.getExpression(), resolver.getClassDescriptor("Artist"),
				select.getRootDescriptor());
	}

	@Test
	public void testGetEntityDescriptorCaseSensitivity() {

		EJBQLCompiledExpression select1 = parser.compile("select a from Artist a", resolver);

		assertNotNull(select1.getEntityDescriptor("a"));
		assertNotNull(select1.getEntityDescriptor("A"));

		EJBQLCompiledExpression select2 = parser.compile("select A from Artist A", resolver);

		assertNotNull(select2.getEntityDescriptor("a"));
		assertNotNull(select2.getEntityDescriptor("A"));

		EJBQLCompiledExpression select3 = parser.compile("select a from Artist A", resolver);

		assertNotNull(select3.getEntityDescriptor("a"));
		assertNotNull(select3.getEntityDescriptor("A"));
	}
}
