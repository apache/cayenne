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

package org.apache.cayenne.unit.jira;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 */
public class CAY_10062004_ExpTest {

    @Test
    public void testDeepCopy() throws Exception {

        Expression parsed = ExpressionFactory.exp("(a = 1 and a = 2) or (a != 1 and a != 2)");
        Expression finalExpression = parsed.deepCopy();

        assertEquals(parsed, finalExpression);
        assertEquals(parsed.toString(), finalExpression.toString());
    }

    @Test
    public void testAndExpOrExp() throws Exception {

        Expression parsed = ExpressionFactory.exp("(a = 1 and a = 2) or (a != 1 and a != 2)");

        Expression first = ExpressionFactory.exp("a = 1");
        Expression second = ExpressionFactory.exp("a = 2");
        Expression third = ExpressionFactory.exp("a != 1");
        Expression fourth = ExpressionFactory.exp("a != 2");

        // this internally calls "joinExp"
        Expression firstAndSecond = first.andExp(second);
        Expression thirdAndFourth = third.andExp(fourth);

        // this internally calls "joinExp"
        Expression finalExpression = firstAndSecond.orExp(thirdAndFourth);

        assertEquals(parsed, finalExpression);
        assertEquals(parsed.toString(), finalExpression.toString());
    }
}
