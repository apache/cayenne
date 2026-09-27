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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.jupiter.api.Test;

public class AndExpTest {

	@Test
	public void constructor_Children() {
		Expression e1 = new EqualExp(ExpressionFactory.pathExp("artistName"), "abc");
		Expression e2 = new EqualExp(ExpressionFactory.pathExp("artistName"), "xyz");
		Expression e3 = new EqualExp(ExpressionFactory.pathExp("artistName"), "123");

		AndExp e = new AndExp(e1, e2, e3);
		assertEquals(3, e.getChildCount());
		assertSame(e1, e.getChild(0));
		assertSame(e2, e.getChild(1));
		assertSame(e3, e.getChild(2));
	}

	@Test
	public void constructor_DoesNotChangeOperands() {
		// a node holds no reference to its parent, so it can be shared between trees and prints the same in each
		Expression e1 = ExpressionFactory.exp("a = 1");
		Expression and = new AndExp(e1, ExpressionFactory.exp("b = 2"));
		Expression or = new OrExp(e1, ExpressionFactory.exp("c = 3"));

		assertEquals("a = 1", e1.toString());
		assertEquals("(a = 1) and (b = 2)", and.toString());
		assertEquals("(a = 1) or (c = 3)", or.toString());
	}

	@Test
	public void constructor_InvalidChild() {
		// child validation in addChild() must still run for constructor operands
		assertThrows(ExpressionException.class, () -> new AndExp(ExpressionFactory.pathExp("a"), "b"));
	}

	@Test
	public void evaluateAND() {
		Expression e1 = new EqualExp(ExpressionFactory.pathExp("artistName"), "abc");
		Expression e2 = new EqualExp(ExpressionFactory.pathExp("artistName"), "abc");

		AndExp e = new AndExp(new Object[] { e1, e2 });

		Artist match = new Artist();
		match.setArtistName("abc");
		assertTrue(e.match(match));

		Artist noMatch = new Artist();
		noMatch.setArtistName("123");
		assertFalse(e.match(noMatch));
	}

}
