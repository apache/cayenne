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
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.FunctionExpressionFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @since 4.0
 */
public class ASTExtractTest {

    @Test
    public void testYear() {
        String expStr = "year(dateColumn)";
        Expression expParsed = ExpressionFactory.exp(expStr);
        Expression expFromFactory = FunctionExpressionFactory.yearExp("dateColumn");

        assertTrue(expParsed instanceof ASTExtract);
        assertTrue(expFromFactory instanceof ASTExtract);

        assertEquals(expStr, expParsed.toString());
        assertEquals(expStr, expFromFactory.toString());
    }

    @Test
    public void testMonth() {
        String expStr = "month(dateColumn)";
        Expression expParsed = ExpressionFactory.exp(expStr);
        Expression expFromFactory = FunctionExpressionFactory.monthExp("dateColumn");

        assertTrue(expParsed instanceof ASTExtract);
        assertTrue(expFromFactory instanceof ASTExtract);

        assertEquals(expStr, expParsed.toString());
        assertEquals(expStr, expFromFactory.toString());
    }

    @Test
    public void testWeek() {
        String expStr = "week(dateColumn)";
        Expression expParsed = ExpressionFactory.exp(expStr);
        Expression expFromFactory = FunctionExpressionFactory.weekExp("dateColumn");

        assertTrue(expParsed instanceof ASTExtract);
        assertTrue(expFromFactory instanceof ASTExtract);

        assertEquals(expStr, expParsed.toString());
        assertEquals(expStr, expFromFactory.toString());
    }

    @Test
    public void testDayOfYear() {
        String expStr = "dayOfYear(dateColumn)";
        Expression expParsed = ExpressionFactory.exp(expStr);
        Expression expFromFactory = FunctionExpressionFactory.dayOfYearExp("dateColumn");

        assertTrue(expParsed instanceof ASTExtract);
        assertTrue(expFromFactory instanceof ASTExtract);

        assertEquals(expStr, expParsed.toString());
        assertEquals(expStr, expFromFactory.toString());
    }

    @Test
    public void testDay() {
        String expStr = "day(dateColumn)";
        Expression expParsed = ExpressionFactory.exp(expStr);

        assertTrue(expParsed instanceof ASTExtract);

        assertEquals(expStr, expParsed.toString());
    }

    @Test
    public void testDayOfMonth() {
        String expStr = "dayOfMonth(dateColumn)";
        Expression expParsed = ExpressionFactory.exp(expStr);
        Expression expFromFactory = FunctionExpressionFactory.dayOfMonthExp("dateColumn");

        assertTrue(expParsed instanceof ASTExtract);
        assertTrue(expFromFactory instanceof ASTExtract);

        assertEquals(expStr, expParsed.toString());
        assertEquals(expStr, expFromFactory.toString());
    }

    @Test
    public void testDayOfWeek() {
        String expStr = "dayOfWeek(dateColumn)";
        Expression expParsed = ExpressionFactory.exp(expStr);
        Expression expFromFactory = FunctionExpressionFactory.dayOfWeekExp("dateColumn");

        assertTrue(expParsed instanceof ASTExtract);
        assertTrue(expFromFactory instanceof ASTExtract);

        assertEquals(expStr, expParsed.toString());
        assertEquals(expStr, expFromFactory.toString());
    }

    @Test
    public void testHour() {
        String expStr = "hour(dateColumn)";
        Expression expParsed = ExpressionFactory.exp(expStr);
        Expression expFromFactory = FunctionExpressionFactory.hourExp("dateColumn");

        assertTrue(expParsed instanceof ASTExtract);
        assertTrue(expFromFactory instanceof ASTExtract);

        assertEquals(expStr, expParsed.toString());
        assertEquals(expStr, expFromFactory.toString());
    }

    @Test
    public void testMinute() {
        String expStr = "minute(dateColumn)";
        Expression expParsed = ExpressionFactory.exp(expStr);
        Expression expFromFactory = FunctionExpressionFactory.minuteExp("dateColumn");

        assertTrue(expParsed instanceof ASTExtract);
        assertTrue(expFromFactory instanceof ASTExtract);

        assertEquals(expStr, expParsed.toString());
        assertEquals(expStr, expFromFactory.toString());
    }

    @Test
    public void testSecond() {
        String expStr = "second(dateColumn)";
        Expression expParsed = ExpressionFactory.exp(expStr);
        Expression expFromFactory = FunctionExpressionFactory.secondExp("dateColumn");

        assertTrue(expParsed instanceof ASTExtract);
        assertTrue(expFromFactory instanceof ASTExtract);

        assertEquals(expStr, expParsed.toString());
        assertEquals(expStr, expFromFactory.toString());
    }

}
