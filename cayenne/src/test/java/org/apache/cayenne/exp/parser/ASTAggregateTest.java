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

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ASTAggregateTest {

    @Test
    public void avgConstruct() {
        ASTAvg avg = new ASTAvg(null);
        assertEquals("AVG", avg.getFunctionName());
    }

    @Test

    public void avgEvaluate() {
        assertThrows(ExpressionException.class, () -> {

            ASTAvg avg = new ASTAvg(null);
            avg.evaluate(new Object());
    
        });
    }

    @Test
    public void avgParse() {
        String expressionString = "avg(artistName)";
        Expression exp = ExpressionFactory.exp(expressionString);
        assertTrue(exp instanceof ASTAvg);
        assertEquals(1, exp.getOperandCount());
        assertTrue(exp.getOperand(0) instanceof ASTObjPath);

        assertEquals(expressionString, exp.toString());
    }

    @Test
    public void countConstruct() {
        ASTCount count = new ASTCount();
        assertEquals("COUNT", count.getFunctionName());
    }

    @Test
    public void countExpParse() {
        String expressionString = "count(artistName)";
        Expression exp = ExpressionFactory.exp(expressionString);
        assertTrue(exp instanceof ASTCount);
        assertEquals(1, exp.getOperandCount());
        assertTrue(exp.getOperand(0) instanceof ASTObjPath);

        assertEquals(expressionString, exp.toString());
    }

    @Test
    public void countAsteriskParse() {
        String expressionString = "count(*)";
        Expression exp = ExpressionFactory.exp(expressionString);
        assertTrue(exp instanceof ASTCount);
        assertEquals(1, exp.getOperandCount());
        assertTrue(exp.getOperand(0) instanceof ASTAsterisk);

        assertEquals(expressionString, exp.toString());
    }

    @Test
    public void countDistinctParse() {
        String expressionString = "count(distinct(artistName))";
        Expression exp = ExpressionFactory.exp(expressionString);
        assertTrue(exp instanceof ASTCount);
        assertEquals(1, exp.getOperandCount());
        assertTrue(exp.getOperand(0) instanceof ASTDistinct);

        ASTDistinct distinct = (ASTDistinct)exp.getOperand(0);
        assertTrue(distinct.getOperand(0) instanceof ASTObjPath);

        assertEquals(expressionString, exp.toString());
    }

    @Test
    public void minConstruct() {
        ASTMin min = new ASTMin(null);
        assertEquals("MIN", min.getFunctionName());
    }

    @Test
    public void minParse() {
        String expressionString = "min(artistName)";
        Expression exp = ExpressionFactory.exp(expressionString);
        assertTrue(exp instanceof ASTMin);
        assertEquals(1, exp.getOperandCount());
        assertTrue(exp.getOperand(0) instanceof ASTObjPath);

        assertEquals(expressionString, exp.toString());
    }

    @Test
    public void maxConstruct() {
        ASTMax max = new ASTMax(null);
        assertEquals("MAX", max.getFunctionName());
    }

    @Test
    public void maxParse() {
        String expressionString = "max(artistName)";
        Expression exp = ExpressionFactory.exp(expressionString);
        assertTrue(exp instanceof ASTMax);
        assertEquals(1, exp.getOperandCount());
        assertTrue(exp.getOperand(0) instanceof ASTObjPath);

        assertEquals(expressionString, exp.toString());
    }

    @Test
    public void sumConstruct() {
        ASTSum sum = new ASTSum(null);
        assertEquals("SUM", sum.getFunctionName());
    }

    @Test
    public void sumParse() {
        String expressionString = "sum(artistName)";
        Expression exp = ExpressionFactory.exp(expressionString);
        assertTrue(exp instanceof ASTSum);
        assertEquals(1, exp.getOperandCount());
        assertTrue(exp.getOperand(0) instanceof ASTObjPath);

        assertEquals(expressionString, exp.toString());
    }

    @Test
    public void customConstruct() {
        AggregateCustom sum = new AggregateCustom();
        assertEquals(AggregateCustom.FUNCTION_NAME, sum.getFunctionName());
    }

    private static class AggregateCustom extends ASTCustomAggregate {

        private static final String FUNCTION_NAME = "aggregate_custom";

        private AggregateCustom() {
            super(FUNCTION_NAME);
        }
    }
}
