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
import org.junit.Test;

import static org.junit.Assert.*;

public class ASTNegateTest {

    @Test
    public void testParse() {
        // we don't have negative numbers, it's a combination of ASTNegate and Scalar
        Expression exp = ExpressionFactory.exp("-1");
        Object operand = exp.getOperand(0);

        assertTrue(exp instanceof ASTNegate);
        assertEquals(1, operand);
        assertEquals("-1", exp.toString());
    }

    @Test
    public void testEvaluate_null() {
        ASTNegate negate = new ASTNegate();
        negate.setOperand(0, new ASTScalar(null));

        Object result = negate.evaluate(null);
        assertNull(result);
    }
}
