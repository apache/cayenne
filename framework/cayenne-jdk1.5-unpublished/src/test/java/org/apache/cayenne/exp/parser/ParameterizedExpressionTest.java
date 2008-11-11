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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.TstTraversalHandler;

/**
 * Tests parameterized expressions of the new form introduced in 1.1
 * 
 * @since 1.1
 */
public class ParameterizedExpressionTest extends TestCase {

    public void testNulls() {
        Expression e1 = Expression.fromString("x = null");
        Expression e2 = e1.expWithParameters(Collections.EMPTY_MAP, true);
        assertNotNull(e2);
        TstTraversalHandler.compareExps(e1, e2);
    }

    /**
     * Tests how parameter substitution algorithm works on an expression with no
     * parameters.
     * 
     * @throws Exception
     */
    public void testCopy1() {
        Expression e1 = Expression.fromString("k1 = 'v1' or k2 = 'v2' or k3 = 'v3'");
        Expression e2 = e1.expWithParameters(Collections.EMPTY_MAP, true);
        TstTraversalHandler.compareExps(e1, e2);
    }

    /**
     * Tests how parameter substitution algorithm works on an expression with no
     * parameters.
     */
    public void testCopy2() {
        Expression e1 = Expression
                .fromString("(k1 = 'v1' and k2 = 'v2' and k3 = 'v3') or (k1 = 'v1')");
        Expression e2 = e1.expWithParameters(Collections.EMPTY_MAP, true);
        TstTraversalHandler.compareExps(e1, e2);
    }

    public void testCopy3() {
        Expression e1 = Expression.fromString("(k1 / 2) = (k2 * 2)");
        Expression e2 = e1.expWithParameters(Collections.EMPTY_MAP, true);
        TstTraversalHandler.compareExps(e1, e2);
    }

    /**
     * Tests how parameter substitution algorithm works on an expression with no
     * parameters.
     */
    public void testFailOnMissingParams() {
        Expression e1 = Expression.fromString("k1 = $test or k2 = 'v2' or k3 = 'v3'");

        try {
            e1.expWithParameters(Collections.EMPTY_MAP, false);
            fail("Parameter was missing, but no exception was thrown.");
        }
        catch (ExpressionException ex) {
            // exception expected
        }
    }

    public void testParams1() {
        Expression e1 = Expression.fromString("k1 = $test");

        Map map = new HashMap();
        map.put("test", "xyz");
        Expression e2 = e1.expWithParameters(map, false);
        assertNotNull(e2);
        assertEquals(2, e2.getOperandCount());
        assertEquals(Expression.EQUAL_TO, e2.getType());
        assertEquals("xyz", e2.getOperand(1));
    }

    public void testParams2() {
        Expression e1 = Expression.fromString("k1 like $test");

        Map map = new HashMap();
        map.put("test", "xyz");
        Expression e2 = e1.expWithParameters(map, false);
        assertNotNull(e2);
        assertEquals(2, e2.getOperandCount());
        assertEquals(Expression.LIKE, e2.getType());
        assertEquals("xyz", e2.getOperand(1));
    }

    public void testNoParams1() {
        Expression e1 = Expression.fromString("k1 = $test");
        Expression e2 = e1.expWithParameters(Collections.EMPTY_MAP, true);

        // all expression nodes must be pruned
        assertNull(e2);
    }

    public void testNoParams2() {
        Expression e1 = Expression
                .fromString("k1 = $test1 or k2 = $test2 or k3 = $test3 or k4 = $test4");

        Map params = new HashMap();
        params.put("test2", "abc");
        params.put("test3", "xyz");
        Expression e2 = e1.expWithParameters(params, true);

        // some expression nodes must be pruned
        assertNotNull(e2);
        assertEquals(2, e2.getOperandCount());

        Expression k2 = (Expression) e2.getOperand(0);
        assertEquals("abc", k2.getOperand(1));

        Expression k3 = (Expression) e2.getOperand(1);
        assertEquals("xyz", k3.getOperand(1));
    }

    public void testNoParams3() {
        Expression e1 = Expression
                .fromString("k1 = $test1 or k2 = $test2 or k3 = $test3 or k4 = $test4");

        Map params = new HashMap();
        params.put("test4", "123");
        Expression e2 = e1.expWithParameters(params, true);

        // some expression nodes must be pruned
        assertNotNull(e2);
        assertEquals(2, e2.getOperandCount());
        assertEquals("123", e2.getOperand(1));
        assertEquals("k4", ((Expression) e2.getOperand(0)).getOperand(0));
    }
}
