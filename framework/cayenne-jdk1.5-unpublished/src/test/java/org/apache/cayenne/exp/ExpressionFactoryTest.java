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

package org.apache.cayenne.exp;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class ExpressionFactoryTest extends TestCase {

    // non-existent type
    private static final int badType = -50;

    public void testExpressionOfBadType() throws Exception {
        try {
            ExpressionFactory.expressionOfType(badType);
            fail();
        }
        catch (ExpressionException ex) {
            // exception expected
        }
    }

    public void testBetweenExp() throws Exception {
        Object v1 = new Object();
        Object v2 = new Object();
        Expression exp = ExpressionFactory.betweenExp("abc", v1, v2);
        assertEquals(Expression.BETWEEN, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.OBJ_PATH, path.getType());
    }

    public void testBetweenDbExp() throws Exception {
        Object v1 = new Object();
        Object v2 = new Object();
        Expression exp = ExpressionFactory.betweenDbExp("abc", v1, v2);
        assertEquals(Expression.BETWEEN, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.DB_PATH, path.getType());
    }

    public void testNotBetweenExp() throws Exception {
        Object v1 = new Object();
        Object v2 = new Object();
        Expression exp = ExpressionFactory.notBetweenExp("abc", v1, v2);
        assertEquals(Expression.NOT_BETWEEN, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.OBJ_PATH, path.getType());
    }
    
    public void testNotBetweenDbExp() throws Exception {
        Object v1 = new Object();
        Object v2 = new Object();
        Expression exp = ExpressionFactory.notBetweenDbExp("abc", v1, v2);
        assertEquals(Expression.NOT_BETWEEN, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.DB_PATH, path.getType());
    }

    public void testGreaterExp() throws Exception {
        Object v = new Object();
        Expression exp = ExpressionFactory.greaterExp("abc", v);
        assertEquals(Expression.GREATER_THAN, exp.getType());
    }

    public void testGreaterDbExp() throws Exception {
        Object v = new Object();
        Expression exp = ExpressionFactory.greaterDbExp("abc", v);
        assertEquals(Expression.GREATER_THAN, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.DB_PATH, path.getType());
    }

    public void testGreaterOrEqualExp() throws Exception {
        Object v = new Object();
        Expression exp = ExpressionFactory.greaterOrEqualExp("abc", v);
        assertEquals(Expression.GREATER_THAN_EQUAL_TO, exp.getType());
    }

    public void testGreaterOrEqualDbExp() throws Exception {
        Object v = new Object();
        Expression exp = ExpressionFactory.greaterOrEqualDbExp("abc", v);
        assertEquals(Expression.GREATER_THAN_EQUAL_TO, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.DB_PATH, path.getType());
    }

    public void testLessExp() throws Exception {
        Object v = new Object();
        Expression exp = ExpressionFactory.lessExp("abc", v);
        assertEquals(Expression.LESS_THAN, exp.getType());
    }

    public void testLessDbExp() throws Exception {
        Object v = new Object();
        Expression exp = ExpressionFactory.lessDbExp("abc", v);
        assertEquals(Expression.LESS_THAN, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.DB_PATH, path.getType());
    }

    public void testLessOrEqualExp() throws Exception {
        Object v = new Object();
        Expression exp = ExpressionFactory.lessOrEqualExp("abc", v);
        assertEquals(Expression.LESS_THAN_EQUAL_TO, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.OBJ_PATH, path.getType());
    }

    public void testLessOrEqualDbExp() throws Exception {
        Object v = new Object();
        Expression exp = ExpressionFactory.lessOrEqualDbExp("abc", v);
        assertEquals(Expression.LESS_THAN_EQUAL_TO, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.DB_PATH, path.getType());
    }

    public void testInExp1() throws Exception {
        Object[] v = new Object[] {
                "a", "b"
        };
        Expression exp = ExpressionFactory.inExp("abc", v);
        assertEquals(Expression.IN, exp.getType());
    }

    public void testInExp2() throws Exception {
        List<Object> v = new ArrayList<Object>();
        v.add("a");
        v.add("b");
        Expression exp = ExpressionFactory.inExp("abc", v);
        assertEquals(Expression.IN, exp.getType());
    }

    public void testInExp3() throws Exception {
        List<Object> v = new ArrayList<Object>();
        Expression exp = ExpressionFactory.inExp("abc", v);
        assertEquals(Expression.FALSE, exp.getType());
    }
    
    public void testLikeExp() throws Exception {
        String v = "abc";
        Expression exp = ExpressionFactory.likeExp("abc", v);
        assertEquals(Expression.LIKE, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.OBJ_PATH, path.getType());
    }

    public void testLikeDbExp() throws Exception {
        String v = "abc";
        Expression exp = ExpressionFactory.likeDbExp("abc", v);
        assertEquals(Expression.LIKE, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.DB_PATH, path.getType());
    }

    public void testLikeIgnoreCaseExp() throws Exception {
        String v = "abc";
        Expression exp = ExpressionFactory.likeIgnoreCaseExp("abc", v);
        assertEquals(Expression.LIKE_IGNORE_CASE, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.OBJ_PATH, path.getType());
    }

    public void testLikeIgnoreCaseDbExp() throws Exception {
        String v = "abc";
        Expression exp = ExpressionFactory.likeIgnoreCaseDbExp("abc", v);
        assertEquals(Expression.LIKE_IGNORE_CASE, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.DB_PATH, path.getType());
    }

    public void testNotLikeIgnoreCaseExp() throws Exception {
        String v = "abc";
        Expression exp = ExpressionFactory.notLikeIgnoreCaseExp("abc", v);
        assertEquals(Expression.NOT_LIKE_IGNORE_CASE, exp.getType());
    }
    
    // testing CAY-941 bug
    public void testLikeExpNull() throws Exception {
        Expression exp = ExpressionFactory.likeExp("abc", null);
        assertEquals(Expression.LIKE, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.OBJ_PATH, path.getType());
        assertNull(exp.getOperand(1));
    }
}
