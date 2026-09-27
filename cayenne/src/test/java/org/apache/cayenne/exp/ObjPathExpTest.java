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
package org.apache.cayenne.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.util.TestObject;
import org.junit.jupiter.api.Test;

public class ObjPathExpTest {

	@Test
	public void constructor_NoOperands() {
		ObjPathExp node = new ObjPathExp();
		assertNull(node.getPath());
		assertEquals(0, node.getChildCount());
	}

	@Test
	public void constructor_NullOperand() {
		ObjPathExp node = new ObjPathExp((Object) null);
		assertEquals(CayennePath.EMPTY_PATH, node.getPath());
		assertEquals(0, node.getChildCount());
	}

	@Test
	public void constructor_TooManyOperands() {
		assertThrows(IllegalArgumentException.class, () -> new ObjPathExp("a", "b"));
	}

	@Test
	public void stringRepresentation() {
		assertEquals("x.y", ExpressionFactory.pathExp("x.y").toString());
	}

	@Test
	public void toEJBQL() {
		assertEquals("r.x.y", ExpressionFactory.pathExp("x.y").toEJBQL("r"));
	}
	
	@Test
	public void toEJBQL_OuterJoin() {
		assertEquals("r.x+.y", ExpressionFactory.pathExp("x+.y").toEJBQL("r"));
	}

	@Test
	public void appendAsString() throws IOException {
		StringBuilder buffer = new StringBuilder();
		ExpressionFactory.pathExp("x.y").appendAsString(buffer);
		assertEquals("x.y", buffer.toString());
	}

	@Test
	public void evaluate_PersistentObject() {
		ObjPathExp node = new ObjPathExp("artistName");

		Artist a1 = new Artist();
		a1.setArtistName("abc");
		assertEquals("abc", node.evaluate(a1));

		Artist a2 = new Artist();
		a2.setArtistName("123");
		assertEquals("123", node.evaluate(a2));
	}

	@Test
	public void evaluate_JavaBean() {
		ObjPathExp node = new ObjPathExp("property2");

		TestObject b1 = new TestObject();
		b1.setProperty2(1);
		assertEquals(1, node.evaluate(b1));

		TestObject b2 = new TestObject();
		b2.setProperty2(-3);
		assertEquals(-3, node.evaluate(b2));
	}

}
