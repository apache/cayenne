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

import org.apache.cayenne.unit.CayenneTestCase;

/**
 * @deprecated since 1.2
 */
public class ListExpressionTst extends CayenneTestCase {
    // non-existent type
    private static final int defaultType = -33;
    protected ListExpression expr;

    protected void setUp() throws Exception {
        expr = new ListExpression(defaultType);
    }

    public void testGetType() throws Exception {
        assertEquals(defaultType, expr.getType());
    }

    public void testGetOperandCount() throws Exception {
        assertEquals(0, expr.getOperandCount());

        expr.appendOperand(new Object());
        assertEquals(1, expr.getOperandCount());

        expr.appendOperand(new Object());
        assertEquals(2, expr.getOperandCount());
    }

    public void testGetOperandAtIndex() throws Exception {
        try {
            expr.getOperand(0);
            fail();
        } catch (Exception ex) {
            // exception expected..
        }
        
        Object obj = new Object();
        expr.appendOperand(obj);
        assertSame(obj, expr.getOperand(0));
    }

    public void testSetOperandAtIndex() throws Exception {
        Object o1 = new Object();
        Object o2 = new Object();

        expr.setOperand(0, o1);
        expr.setOperand(1, o2);
        assertSame(o1, expr.getOperand(0));
        assertSame(o2, expr.getOperand(1));

        try {
            expr.setOperand(3, o1);
            fail();
        } catch (Exception ex) {
            // exception expected..
        }
    }

}
