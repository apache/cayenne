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

package org.apache.cayenne.unit.jira;

import junit.framework.TestCase;

import org.apache.cayenne.exp.Expression;

/**
 */
public class CAY_10062004_ExpTest extends TestCase {

    public void testDeepCopy() throws Exception {

        Expression parsed = Expression
                .fromString("(a = 1 and a = 2) or (a != 1 and a != 2)");
        Expression finalExpression = parsed.deepCopy();

        assertEquals(parsed, finalExpression);
        assertEquals(parsed.toString(), finalExpression.toString());
    }

    public void testAndExpOrExp() throws Exception {

        Expression parsed = Expression
                .fromString("(a = 1 and a = 2) or (a != 1 and a != 2)");

        Expression first = Expression.fromString("a = 1");
        Expression second = Expression.fromString("a = 2");
        Expression third = Expression.fromString("a != 1");
        Expression fourth = Expression.fromString("a != 2");

        // this internally calls "joinExp"
        Expression firstAndSecond = first.andExp(second);
        Expression thirdAndFourth = third.andExp(fourth);

        // this internally calls "joinExp"
        Expression finalExpression = firstAndSecond.orExp(thirdAndFourth);

        assertEquals(parsed, finalExpression);
        assertEquals(parsed.toString(), finalExpression.toString());
    }
}
