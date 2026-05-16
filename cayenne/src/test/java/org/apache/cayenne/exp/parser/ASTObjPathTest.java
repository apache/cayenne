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
package org.apache.cayenne.exp.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.util.TestObject;
import org.junit.jupiter.api.Test;

public class ASTObjPathTest {

	@Test
	public void stringRepresentation() {
		assertEquals("x.y", new ASTObjPath("x.y").toString());
	}

	@Test
	public void toEJBQL() {
		assertEquals("r.x.y", new ASTObjPath("x.y").toEJBQL("r"));
	}
	
	@Test
	public void toEJBQL_OuterJoin() {
		assertEquals("r.x+.y", new ASTObjPath("x+.y").toEJBQL("r"));
	}

	@Test
	public void appendAsString() throws IOException {
		StringBuilder buffer = new StringBuilder();
		new ASTObjPath("x.y").appendAsString(buffer);
		assertEquals("x.y", buffer.toString());
	}

	@Test
	public void evaluate_PersistentObject() {
		ASTObjPath node = new ASTObjPath("artistName");

		Artist a1 = new Artist();
		a1.setArtistName("abc");
		assertEquals("abc", node.evaluate(a1));

		Artist a2 = new Artist();
		a2.setArtistName("123");
		assertEquals("123", node.evaluate(a2));
	}

	@Test
	public void evaluate_JavaBean() {
		ASTObjPath node = new ASTObjPath("property2");

		TestObject b1 = new TestObject();
		b1.setProperty2(1);
		assertEquals(1, node.evaluate(b1));

		TestObject b2 = new TestObject();
		b2.setProperty2(-3);
		assertEquals(-3, node.evaluate(b2));
	}

	@Test
	public void injectPersistentObject() {
		ASTObjPath node = new ASTObjPath("artistName");

		Artist artist = new Artist();
		assertNull(artist.getArtistName());

		node.injectValue(artist, "test");
		assertEquals("test", artist.getArtistName());
	}

}
