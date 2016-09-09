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
package org.apache.cayenne.exp.parser;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.apache.cayenne.exp.Expression;
import org.junit.Test;

// TODO: split it between AST* unit tests (partially done already)
public class ExpressionEvaluateInMemoryTest {

	@Test
	public void testEvaluateADD() {
		Expression add = new ASTAdd(new Object[] { new Integer(1), new Double(5.5) });
		assertEquals(6.5, ((Number) add.evaluate(null)).doubleValue(), 0.0001);
	}

	@Test
	public void testEvaluateSubtract() {
		Expression subtract = new ASTSubtract(new Object[] { new Integer(1), new Double(0.1), new Double(0.2) });
		assertEquals(0.7, ((Number) subtract.evaluate(null)).doubleValue(), 0.0001);
	}

	@Test
	public void testEvaluateMultiply() {
		Expression multiply = new ASTMultiply(new Object[] { new Integer(2), new Double(3.5) });
		assertEquals(7, ((Number) multiply.evaluate(null)).doubleValue(), 0.0001);
	}

	@Test
	public void testEvaluateDivide() {
		Expression divide = new ASTDivide(new Object[] { new BigDecimal("7.0"), new BigDecimal("2.0") });
		assertEquals(3.5, ((Number) divide.evaluate(null)).doubleValue(), 0.0001);
	}

	@Test
	public void testEvaluateNegate() {
		assertEquals(-3, ((Number) new ASTNegate(new Integer(3)).evaluate(null)).intValue());
		assertEquals(5, ((Number) new ASTNegate(new Integer(-5)).evaluate(null)).intValue());
	}

	@Test
	public void testEvaluateTrue() {
		assertEquals(Boolean.TRUE, new ASTTrue().evaluate(null));
	}

	@Test
	public void testEvaluateFalse() {
		assertEquals(Boolean.FALSE, new ASTFalse().evaluate(null));
	}

}
