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

import org.apache.cayenne.exp.parser.ASTAbs;
import org.apache.cayenne.exp.parser.ASTAsterisk;
import org.apache.cayenne.exp.parser.ASTAvg;
import org.apache.cayenne.exp.parser.ASTConcat;
import org.apache.cayenne.exp.parser.ASTCount;
import org.apache.cayenne.exp.parser.ASTCurrentDate;
import org.apache.cayenne.exp.parser.ASTCurrentTime;
import org.apache.cayenne.exp.parser.ASTCurrentTimestamp;
import org.apache.cayenne.exp.parser.ASTCustomOperator;
import org.apache.cayenne.exp.parser.ASTLength;
import org.apache.cayenne.exp.parser.ASTLocate;
import org.apache.cayenne.exp.parser.ASTLower;
import org.apache.cayenne.exp.parser.ASTMax;
import org.apache.cayenne.exp.parser.ASTMin;
import org.apache.cayenne.exp.parser.ASTMod;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.exp.parser.ASTScalar;
import org.apache.cayenne.exp.parser.ASTSqrt;
import org.apache.cayenne.exp.parser.ASTSubstring;
import org.apache.cayenne.exp.parser.ASTSum;
import org.apache.cayenne.exp.parser.ASTTrim;
import org.apache.cayenne.exp.parser.ASTUpper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @since 4.0
 */
public class FunctionExpressionFactoryTest {

    @Test
    public void substringExp() throws Exception {
        Expression exp1 = FunctionExpressionFactory.substringExp(Artist.ARTIST_NAME.getExpression(), 10, 15);
        Expression exp2 = FunctionExpressionFactory.substringExp(Artist.ARTIST_NAME.getName(), 10, 15);
        Expression exp3 = FunctionExpressionFactory.substringExp(Artist.ARTIST_NAME.getExpression(), new ASTScalar(10), new ASTScalar(15));

        assertTrue(exp1 instanceof ASTSubstring);

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

        assertTrue(exp1 instanceof ASTTrim);

        assertEquals(1, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));

        assertEquals(exp1, exp2);
    }

    @Test
    public void lowerExp() throws Exception {
        Expression exp1 = FunctionExpressionFactory.lowerExp(Artist.ARTIST_NAME.getExpression());
        Expression exp2 = FunctionExpressionFactory.lowerExp(Artist.ARTIST_NAME.getName());

        assertTrue(exp1 instanceof ASTLower);

        assertEquals(1, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));

        assertEquals(exp1, exp2);
    }

    @Test
    public void upperExp() throws Exception {
        Expression exp1 = FunctionExpressionFactory.upperExp(Artist.ARTIST_NAME.getExpression());
        Expression exp2 = FunctionExpressionFactory.upperExp(Artist.ARTIST_NAME.getName());

        assertTrue(exp1 instanceof ASTUpper);

        assertEquals(1, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));

        assertEquals(exp1, exp2);
    }

    @Test
    public void lengthExp() throws Exception {
        Expression exp1 = FunctionExpressionFactory.lengthExp(Artist.ARTIST_NAME.getExpression());
        Expression exp2 = FunctionExpressionFactory.lengthExp(Artist.ARTIST_NAME.getName());

        assertTrue(exp1 instanceof ASTLength);

        assertEquals(1, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));

        assertEquals(exp1, exp2);
    }


    @Test
    public void locateExp() throws Exception {
        Expression exp1 = FunctionExpressionFactory.locateExp("abc", Artist.ARTIST_NAME.getExpression());
        Expression exp2 = FunctionExpressionFactory.locateExp("abc", Artist.ARTIST_NAME.getName());
        Expression exp3 = FunctionExpressionFactory.locateExp(new ASTScalar("abc"), Artist.ARTIST_NAME.getExpression());

        assertTrue(exp1 instanceof ASTLocate);

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

        assertTrue(exp1 instanceof ASTAbs);

        assertEquals(1, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));

        assertEquals(exp1, exp2);
    }

    @Test
    public void sqrtExp() throws Exception {
        Expression exp1 = FunctionExpressionFactory.sqrtExp(Artist.ARTIST_NAME.getExpression());
        Expression exp2 = FunctionExpressionFactory.sqrtExp(Artist.ARTIST_NAME.getName());

        assertTrue(exp1 instanceof ASTSqrt);

        assertEquals(1, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));

        assertEquals(exp1, exp2);
    }


    @Test
    public void modExp() throws Exception {
        Expression exp1 = FunctionExpressionFactory.modExp(Artist.ARTIST_NAME.getExpression(), 10);
        Expression exp2 = FunctionExpressionFactory.modExp(Artist.ARTIST_NAME.getName(), 10);
        Expression exp3 = FunctionExpressionFactory.modExp(Artist.ARTIST_NAME.getExpression(), new ASTScalar(10));

        assertTrue(exp1 instanceof ASTMod);

        assertEquals(2, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));
        assertEquals(10, exp1.getOperand(1));

        assertEquals(exp1, exp2);
        assertEquals(exp2, exp3);
    }


    @Test
    public void concatExp() throws Exception {
        Expression exp1 = FunctionExpressionFactory.concatExp(Artist.ARTIST_NAME.getExpression(), new ASTScalar("abc"), Artist.DATE_OF_BIRTH.getExpression());
        assertTrue(exp1 instanceof ASTConcat);
        assertEquals(3, exp1.getOperandCount());

        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));
        assertEquals("abc", exp1.getOperand(1));
        assertEquals(Artist.DATE_OF_BIRTH.getExpression(), exp1.getOperand(2));


        Expression exp2 = FunctionExpressionFactory.concatExp(Artist.ARTIST_NAME.getName(), Artist.DATE_OF_BIRTH.getName(), Artist.PAINTING_ARRAY.getName());
        assertTrue(exp2 instanceof ASTConcat);
        assertEquals(3, exp2.getOperandCount());

        assertEquals(Artist.ARTIST_NAME.getExpression(), exp2.getOperand(0));
        assertEquals(Artist.DATE_OF_BIRTH.getExpression(), exp2.getOperand(1));
        assertEquals(Artist.PAINTING_ARRAY.getExpression(), exp2.getOperand(2));
    }

    @Test
    public void countTest() throws Exception {
        Expression exp1 = FunctionExpressionFactory.countExp();
        assertTrue(exp1 instanceof ASTCount);
        assertEquals(1, exp1.getOperandCount());
        assertEquals(new ASTAsterisk(), exp1.getOperand(0));

        Expression exp2 = FunctionExpressionFactory.countExp(Artist.ARTIST_NAME.getExpression());
        assertTrue(exp2 instanceof ASTCount);
        assertEquals(1, exp2.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp2.getOperand(0));
    }

    @Test
    public void minTest() throws Exception {
        Expression exp1 = FunctionExpressionFactory.minExp(Artist.ARTIST_NAME.getExpression());
        assertTrue(exp1 instanceof ASTMin);
        assertEquals(1, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));
    }

    @Test
    public void maxTest() throws Exception {
        Expression exp1 = FunctionExpressionFactory.maxExp(Artist.ARTIST_NAME.getExpression());
        assertTrue(exp1 instanceof ASTMax);
        assertEquals(1, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));
    }

    @Test
    public void avgTest() throws Exception {
        Expression exp1 = FunctionExpressionFactory.avgExp(Artist.ARTIST_NAME.getExpression());
        assertTrue(exp1 instanceof ASTAvg);
        assertEquals(1, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));
    }

    @Test
    public void sumTest() throws Exception {
        Expression exp1 = FunctionExpressionFactory.sumExp(Artist.ARTIST_NAME.getExpression());
        assertTrue(exp1 instanceof ASTSum);
        assertEquals(1, exp1.getOperandCount());
        assertEquals(Artist.ARTIST_NAME.getExpression(), exp1.getOperand(0));
    }

    @Test
    public void currentDateTest() throws Exception {
        Expression exp = FunctionExpressionFactory.currentDate();
        assertTrue(exp instanceof ASTCurrentDate);
    }

    @Test
    public void currentTimeTest() throws Exception {
        Expression exp = FunctionExpressionFactory.currentTime();
        assertTrue(exp instanceof ASTCurrentTime);
    }

    @Test
    public void currentTimestampTest() throws Exception {
        Expression exp = FunctionExpressionFactory.currentTimestamp();
        assertTrue(exp instanceof ASTCurrentTimestamp);
    }

    @Test
    public void customOpTest() {
        Expression exp = FunctionExpressionFactory.operator("==>", 123, Artist.ARTIST_NAME.getExpression());
        assertTrue(exp instanceof ASTCustomOperator);
        ASTCustomOperator operator = (ASTCustomOperator) exp;
        assertEquals("==>", operator.getOperator());
        assertEquals(2, operator.jjtGetNumChildren());

        assertEquals(123, operator.getOperand(0));
        assertEquals(Artist.ARTIST_NAME.getExpression(), operator.getOperand(1));
    }
}