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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.jupiter.api.Test;

// TODO: split it between AST* unit tests (partially done already)
public class ExpressionEvaluateInMemoryTest {

	@Test
	public void evaluateADD() {
		Expression add = new AddExp(1, 5.5);
		assertEquals(6.5, ((Number) add.evaluate(null)).doubleValue(), 0.0001);
	}

	@Test
	public void evaluateSubtract() {
		Expression subtract = new SubtractExp(1, 0.1, 0.2);
		assertEquals(0.7, ((Number) subtract.evaluate(null)).doubleValue(), 0.0001);
	}

	@Test
	public void evaluateMultiply() {
		Expression multiply = new MultiplyExp(2, 3.5);
		assertEquals(7, ((Number) multiply.evaluate(null)).doubleValue(), 0.0001);
	}

	@Test
	public void evaluateDivide() {
		Expression divide = new DivideExp(new BigDecimal("7.0"), new BigDecimal("2.0"));
		assertEquals(3.5, ((Number) divide.evaluate(null)).doubleValue(), 0.0001);
	}

	@Test
	public void evaluateNegate() {
		assertEquals(-3, ((Number) new NegateExp(Integer.valueOf(3)).evaluate(null)).intValue());
		assertEquals(5, ((Number) new NegateExp(Integer.valueOf(-5)).evaluate(null)).intValue());
	}

	@Test
	public void evaluateTrue() {
		assertEquals(Boolean.TRUE, new TrueExp().evaluate(null));
	}

	@Test
	public void evaluateFalse() {
		assertEquals(Boolean.FALSE, new FalseExp().evaluate(null));
	}

	@Test
    public void evaluateNullCompare() throws Exception {
        Expression expression = new GreaterExp(ExpressionFactory.pathExp("artistName"), "A");
        assertFalse(expression.match(new Artist()));
        assertFalse(expression.notExp().match(new Artist()));
    }

    @Test
    public void evaluateCompareNull() throws Exception {
        Artist a1 = new Artist();
        a1.setArtistName("Name");
        Expression expression = new GreaterExp(ExpressionFactory.pathExp("artistName"), null);
        assertFalse(expression.match(a1));
        assertFalse(expression.notExp().match(a1));
        a1.setSomeOtherObjectProperty(new BigDecimal(1));
        expression = ExpressionFactory.exp("someOtherObjectProperty > null");
        assertFalse(expression.match(a1));
    }

    @Test
    public void evaluateEqualsNull() throws Exception {
        Artist a1 = new Artist();
        Expression isNull = Artist.ARTIST_NAME.isNull();
        assertTrue(isNull.match(a1));
        assertFalse(isNull.notExp().match(a1));
    }

    @Test
    public void evaluateNotEqualsNullColumn() throws Exception {
        Expression notEquals = ExpressionFactory.exp("artistName <> someOtherProperty");
        assertFalse(notEquals.match(new Artist()));
        assertTrue(notEquals.notExp().match(new Artist()));
    }

    @Test
    public void nullAnd() {
        Expression nullExp = ExpressionFactory.exp("null > 0");

        AndExp nullAndTrue = new AndExp(new Object[] {nullExp, new TrueExp()});
        assertFalse(nullAndTrue.match(null));
        assertFalse(nullAndTrue.notExp().match(null));

        AndExp nullAndFalse = new AndExp(new Object[] {nullExp, new FalseExp()});
        assertFalse(nullAndFalse.match(null));
        assertTrue(nullAndFalse.notExp().match(null));
    }

    @Test
    public void nullOr() {
        Expression nullExp = ExpressionFactory.exp("null > 0");

        OrExp nullOrTrue = new OrExp(new Object[] {nullExp, new TrueExp()});
        assertTrue(nullOrTrue.match(null));
        assertFalse(nullOrTrue.notExp().match(null));

        OrExp nullOrFalse = new OrExp(new Object[] {nullExp, new FalseExp()});
        assertFalse(nullOrFalse.match(null));
        assertFalse(nullOrFalse.notExp().match(null));
    }
}
