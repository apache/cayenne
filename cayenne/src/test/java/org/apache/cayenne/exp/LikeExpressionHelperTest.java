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

import org.junit.jupiter.api.Test;

public class LikeExpressionHelperTest {

	@Test
	public void escape_NoEscapeChars() {

		PatternMatchExp node = new LikeExp(ExpressionFactory.pathExp("x"), "abc");
		LikeExpressionHelper.escape(node);
		assertEquals("abc", node.getOperand(1));
		assertEquals(0, node.getEscapeChar());
	}
	
	@Test
	public void escape_OneChar() {

		PatternMatchExp node = new LikeExp(ExpressionFactory.pathExp("x"), "ab_c");
		LikeExpressionHelper.escape(node);
		assertEquals("ab!_c", node.getOperand(1));
		assertEquals('!', node.getEscapeChar());
	}
	
	@Test
	public void escape_TwoChars() {

		PatternMatchExp node = new LikeExp(ExpressionFactory.pathExp("x"), "ab_c_");
		LikeExpressionHelper.escape(node);
		assertEquals("ab!_c!_", node.getOperand(1));
		assertEquals('!', node.getEscapeChar());
	}
	
	@Test
	public void escape_TwoChars_Mix() {

		PatternMatchExp node = new LikeExp(ExpressionFactory.pathExp("x"), "ab%c_");
		LikeExpressionHelper.escape(node);
		assertEquals("ab!%c!_", node.getOperand(1));
		assertEquals('!', node.getEscapeChar());
	}
	
	@Test
	public void escape_AltEscapeChar1() {

		PatternMatchExp node = new LikeExp(ExpressionFactory.pathExp("x"), "a!%c");
		LikeExpressionHelper.escape(node);
		assertEquals("a!#%c", node.getOperand(1));
		assertEquals('#', node.getEscapeChar());
	}
	
	@Test
	public void escape_AltEscapeChar2() {

		PatternMatchExp node = new LikeExp(ExpressionFactory.pathExp("x"), "a!%c#_");
		LikeExpressionHelper.escape(node);
		assertEquals("a!$%c#$_", node.getOperand(1));
		assertEquals('$', node.getEscapeChar());
	}
}
