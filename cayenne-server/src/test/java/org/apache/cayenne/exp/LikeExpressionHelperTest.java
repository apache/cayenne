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

import static org.junit.Assert.assertEquals;

import org.apache.cayenne.exp.parser.ASTLike;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.exp.parser.PatternMatchNode;
import org.junit.Test;

public class LikeExpressionHelperTest {

	@Test
	public void testEscape_NoEscapeChars() {

		PatternMatchNode node = new ASTLike(new ASTObjPath("x"), "abc");
		LikeExpressionHelper.escape(node);
		assertEquals("abc", node.getOperand(1));
		assertEquals(0, node.getEscapeChar());
	}
	
	@Test
	public void testEscape_OneChar() {

		PatternMatchNode node = new ASTLike(new ASTObjPath("x"), "ab_c");
		LikeExpressionHelper.escape(node);
		assertEquals("ab!_c", node.getOperand(1));
		assertEquals('!', node.getEscapeChar());
	}
	
	@Test
	public void testEscape_TwoChars() {

		PatternMatchNode node = new ASTLike(new ASTObjPath("x"), "ab_c_");
		LikeExpressionHelper.escape(node);
		assertEquals("ab!_c!_", node.getOperand(1));
		assertEquals('!', node.getEscapeChar());
	}
	
	@Test
	public void testEscape_TwoChars_Mix() {

		PatternMatchNode node = new ASTLike(new ASTObjPath("x"), "ab%c_");
		LikeExpressionHelper.escape(node);
		assertEquals("ab!%c!_", node.getOperand(1));
		assertEquals('!', node.getEscapeChar());
	}
	
	@Test
	public void testEscape_AltEscapeChar1() {

		PatternMatchNode node = new ASTLike(new ASTObjPath("x"), "a!%c");
		LikeExpressionHelper.escape(node);
		assertEquals("a!#%c", node.getOperand(1));
		assertEquals('#', node.getEscapeChar());
	}
	
	@Test
	public void testEscape_AltEscapeChar2() {

		PatternMatchNode node = new ASTLike(new ASTObjPath("x"), "a!%c#_");
		LikeExpressionHelper.escape(node);
		assertEquals("a!$%c#$_", node.getOperand(1));
		assertEquals('$', node.getEscapeChar());
	}
}
