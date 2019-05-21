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

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.Test;

// TODO: split it between AST* unit tests (partially done already)
public class ExpressionEvaluateInMemoryTest {

	@Test
	public void testEvaluateADD() {
		Expression add = new ASTAdd(1, 5.5);
		assertEquals(6.5, ((Number) add.evaluate(null)).doubleValue(), 0.0001);
	}

	@Test
	public void testEvaluateSubtract() {
		Expression subtract = new ASTSubtract(1, 0.1, 0.2);
		assertEquals(0.7, ((Number) subtract.evaluate(null)).doubleValue(), 0.0001);
	}

	@Test
	public void testEvaluateMultiply() {
		Expression multiply = new ASTMultiply(2, 3.5);
		assertEquals(7, ((Number) multiply.evaluate(null)).doubleValue(), 0.0001);
	}

	@Test
	public void testEvaluateDivide() {
		Expression divide = new ASTDivide(new BigDecimal("7.0"), new BigDecimal("2.0"));
		assertEquals(3.5, ((Number) divide.evaluate(null)).doubleValue(), 0.0001);
	}

	@Test
	public void testEvaluateNegate() {
		assertEquals(-3, ((Number) new ASTNegate(Integer.valueOf(3)).evaluate(null)).intValue());
		assertEquals(5, ((Number) new ASTNegate(Integer.valueOf(-5)).evaluate(null)).intValue());
	}

	@Test
	public void testEvaluateTrue() {
		assertEquals(Boolean.TRUE, new ASTTrue().evaluate(null));
	}

	@Test
	public void testEvaluateFalse() {
		assertEquals(Boolean.FALSE, new ASTFalse().evaluate(null));
	}

	@Test
    public void testEvaluateNullCompare() throws Exception {
        Expression expression = new ASTGreater(new ASTObjPath("artistName"), "A");
        assertFalse(expression.match(new Artist()));
        assertFalse(expression.notExp().match(new Artist()));
    }

    @Test
    public void testEvaluateCompareNull() throws Exception {
        Artist a1 = new Artist();
        a1.setArtistName("Name");
        Expression expression = new ASTGreater(new ASTObjPath("artistName"), null);
        assertFalse(expression.match(a1));
        assertFalse(expression.notExp().match(a1));
        a1.setSomeOtherObjectProperty(new BigDecimal(1));
        expression = ExpressionFactory.exp("someOtherObjectProperty > null");
        assertFalse(expression.match(a1));
    }

    @Test
    public void testEvaluateEqualsNull() throws Exception {
        Artist a1 = new Artist();
        Expression isNull = Artist.ARTIST_NAME.isNull();
        assertTrue(isNull.match(a1));
        assertFalse(isNull.notExp().match(a1));
    }

    @Test
    public void testEvaluateNotEqualsNullColumn() throws Exception {
        Expression notEquals = ExpressionFactory.exp("artistName <> someOtherProperty");
        assertFalse(notEquals.match(new Artist()));
        assertTrue(notEquals.notExp().match(new Artist()));
    }

    @Test
    public void testNullAnd() {
        Expression nullExp = ExpressionFactory.exp("null > 0");

        ASTAnd nullAndTrue = new ASTAnd(new Object[] {nullExp, new ASTTrue()});
        assertFalse(nullAndTrue.match(null));
        assertFalse(nullAndTrue.notExp().match(null));

        ASTAnd nullAndFalse = new ASTAnd(new Object[] {nullExp, new ASTFalse()});
        assertFalse(nullAndFalse.match(null));
        assertTrue(nullAndFalse.notExp().match(null));
    }

    @Test
    public void testNullOr() {
        Expression nullExp = ExpressionFactory.exp("null > 0");

        ASTOr nullOrTrue = new ASTOr(new Object[] {nullExp, new ASTTrue()});
        assertTrue(nullOrTrue.match(null));
        assertFalse(nullOrTrue.notExp().match(null));

        ASTOr nullOrFalse = new ASTOr(new Object[] {nullExp, new ASTFalse()});
        assertFalse(nullOrFalse.match(null));
        assertFalse(nullOrFalse.notExp().match(null));
    }
}
