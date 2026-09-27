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

import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FunctionExpressionFactoryTest {

    @Test
    public void substringExp() throws Exception {
        Expression exp1 = FunctionExpressionFactory.substringExp(Artist.ARTIST_NAME.getExpression(), 10, 15);
        Expression exp2 = FunctionExpressionFactory.substringExp(Artist.ARTIST_NAME.getName(), 10, 15);
        Expression exp3 = FunctionExpressionFactory.substringExp(Artist.ARTIST_NAME.getExpression(), ExpressionFactory.wrapScalarValue(10), ExpressionFactory.wrapScalarValue(15));

        assertTrue(exp1 instanceof SubstringExp);

        assertEquals(3, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));
        assertEquals(10, exp1.getOperand(1));
        assertEquals(15, exp1.getOperand(2));

        assertEquals(exp1, exp2);
        assertEquals(exp2, exp3);
    }

    @Test
    public void trimExp() throws Exception {
        Expression exp1 = FunctionExpressionFactory.trimExp(Artist.ARTIST_NAME.getExpression());
        Expression exp2 = FunctionExpressionFactory.trimExp(Artist.ARTIST_NAME.getName());

        assertTrue(exp1 instanceof TrimExp);

        assertEquals(1, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));

        assertEquals(exp1, exp2);
    }

    @Test
    public void lowerExp() throws Exception {
        Expression exp1 = FunctionExpressionFactory.lowerExp(Artist.ARTIST_NAME.getExpression());
        Expression exp2 = FunctionExpressionFactory.lowerExp(Artist.ARTIST_NAME.getName());

        assertTrue(exp1 instanceof LowerExp);

        assertEquals(1, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));

        assertEquals(exp1, exp2);
    }

    @Test
    public void upperExp() throws Exception {
        Expression exp1 = FunctionExpressionFactory.upperExp(Artist.ARTIST_NAME.getExpression());
        Expression exp2 = FunctionExpressionFactory.upperExp(Artist.ARTIST_NAME.getName());

        assertTrue(exp1 instanceof UpperExp);

        assertEquals(1, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));

        assertEquals(exp1, exp2);
    }

    @Test
    public void lengthExp() throws Exception {
        Expression exp1 = FunctionExpressionFactory.lengthExp(Artist.ARTIST_NAME.getExpression());
        Expression exp2 = FunctionExpressionFactory.lengthExp(Artist.ARTIST_NAME.getName());

        assertTrue(exp1 instanceof LengthExp);

        assertEquals(1, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));

        assertEquals(exp1, exp2);
    }

    @Test
    public void locateExp() throws Exception {
        Expression exp1 = FunctionExpressionFactory.locateExp("abc", Artist.ARTIST_NAME.getExpression());
        Expression exp2 = FunctionExpressionFactory.locateExp("abc", Artist.ARTIST_NAME.getName());
        Expression exp3 = FunctionExpressionFactory.locateExp(ExpressionFactory.wrapScalarValue("abc"), Artist.ARTIST_NAME.getExpression());

        assertTrue(exp1 instanceof LocateExp);

        assertEquals(2, exp1.getOperandCount());
        assertEquals("abc", exp1.getOperand(0));
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(1));

        assertEquals(exp1, exp2);
        assertEquals(exp2, exp3);
    }

    @Test
    public void absExp() throws Exception {
        Expression exp1 = FunctionExpressionFactory.absExp(Artist.ARTIST_NAME.getExpression());
        Expression exp2 = FunctionExpressionFactory.absExp(Artist.ARTIST_NAME.getName());

        assertTrue(exp1 instanceof AbsExp);

        assertEquals(1, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));

        assertEquals(exp1, exp2);
    }

    @Test
    public void sqrtExp() throws Exception {
        Expression exp1 = FunctionExpressionFactory.sqrtExp(Artist.ARTIST_NAME.getExpression());
        Expression exp2 = FunctionExpressionFactory.sqrtExp(Artist.ARTIST_NAME.getName());

        assertTrue(exp1 instanceof SqrtExp);

        assertEquals(1, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));

        assertEquals(exp1, exp2);
    }

    @Test
    public void modExp() throws Exception {
        Expression exp1 = FunctionExpressionFactory.modExp(Artist.ARTIST_NAME.getExpression(), 10);
        Expression exp2 = FunctionExpressionFactory.modExp(Artist.ARTIST_NAME.getName(), 10);
        Expression exp3 = FunctionExpressionFactory.modExp(Artist.ARTIST_NAME.getExpression(), ExpressionFactory.wrapScalarValue(10));

        assertTrue(exp1 instanceof ModExp);

        assertEquals(2, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));
        assertEquals(10, exp1.getOperand(1));

        assertEquals(exp1, exp2);
        assertEquals(exp2, exp3);
    }

    @Test
    public void concatExp() throws Exception {
        Expression exp1 = FunctionExpressionFactory.concatExp(Artist.ARTIST_NAME.getExpression(), ExpressionFactory.wrapScalarValue("abc"), Artist.DATE_OF_BIRTH.getExpression());
        assertTrue(exp1 instanceof ConcatExp);
        assertEquals(3, exp1.getOperandCount());

        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));
        assertEquals("abc", exp1.getOperand(1));
        assertEquals(Artist.DATE_OF_BIRTH.getExpression(), exp1.getOperand(2));

        Expression exp2 = FunctionExpressionFactory.concatExp(Artist.ARTIST_NAME.getName(), Artist.DATE_OF_BIRTH.getName(), Artist.PAINTING_ARRAY.getName());
        assertTrue(exp2 instanceof ConcatExp);
        assertEquals(3, exp2.getOperandCount());

        assertEquals(Artist.ARTIST_NAME.getExpression(), exp2.getOperand(0));
        assertEquals(Artist.DATE_OF_BIRTH.getExpression(), exp2.getOperand(1));
        assertEquals(Artist.PAINTING_ARRAY.getExpression(), exp2.getOperand(2));
    }

    @Test
    public void countTest() throws Exception {
        Expression exp1 = FunctionExpressionFactory.countExp();
        assertTrue(exp1 instanceof CountExp);
        assertEquals(1, exp1.getOperandCount());
        assertEquals(new AsteriskExp(), exp1.getOperand(0));

        Expression exp2 = FunctionExpressionFactory.countExp(Artist.ARTIST_NAME.getExpression());
        assertTrue(exp2 instanceof CountExp);
        assertEquals(1, exp2.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp2.getOperand(0));
    }

    @Test
    public void minTest() throws Exception {
        Expression exp1 = FunctionExpressionFactory.minExp(Artist.ARTIST_NAME.getExpression());
        assertTrue(exp1 instanceof MinExp);
        assertEquals(1, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));
    }

    @Test
    public void maxTest() throws Exception {
        Expression exp1 = FunctionExpressionFactory.maxExp(Artist.ARTIST_NAME.getExpression());
        assertTrue(exp1 instanceof MaxExp);
        assertEquals(1, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));
    }

    @Test
    public void avgTest() throws Exception {
        Expression exp1 = FunctionExpressionFactory.avgExp(Artist.ARTIST_NAME.getExpression());
        assertTrue(exp1 instanceof AvgExp);
        assertEquals(1, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));
    }

    @Test
    public void sumTest() throws Exception {
        Expression exp1 = FunctionExpressionFactory.sumExp(Artist.ARTIST_NAME.getExpression());
        assertTrue(exp1 instanceof SumExp);
        assertEquals(1, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));
    }

    @Test
    public void currentDateTest() throws Exception {
        Expression exp = FunctionExpressionFactory.currentDate();
        assertTrue(exp instanceof CurrentDateExp);
    }

    @Test
    public void currentTimeTest() throws Exception {
        Expression exp = FunctionExpressionFactory.currentTime();
        assertTrue(exp instanceof CurrentTimeExp);
    }

    @Test
    public void currentTimestampTest() throws Exception {
        Expression exp = FunctionExpressionFactory.currentTimestamp();
        assertTrue(exp instanceof CurrentTimestampExp);
    }

    @Test
    public void customOpTest() {
        Expression exp = FunctionExpressionFactory.operator("==>", 123, Artist.ARTIST_NAME.getExpression());
        assertTrue(exp instanceof CustomOperatorExp);
        CustomOperatorExp operator = (CustomOperatorExp) exp;
        assertEquals("==>", operator.getOperator());
        assertEquals(2, operator.getOperandCount());

        assertEquals(123, operator.getOperand(0));
        assertEquals(Artist.ARTIST_NAME.getExpression(), operator.getOperand(1));
    }
}