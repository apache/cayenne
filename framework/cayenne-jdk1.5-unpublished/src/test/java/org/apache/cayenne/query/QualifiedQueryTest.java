package org.apache.cayenne.query;

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

import junit.framework.TestCase;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;

public class QualifiedQueryTest extends TestCase {

    public void testSetQualifier() {
        QualifiedQuery query = new MockQualifiedQuery();
        assertNull(query.getQualifier());

        Expression qual = ExpressionFactory.expressionOfType(Expression.AND);
        query.setQualifier(qual);
        assertNotNull(query.getQualifier());
        assertSame(qual, query.getQualifier());
    }

    public void testAndQualifier() {
        QualifiedQuery query = new MockQualifiedQuery();
        assertNull(query.getQualifier());

        Expression e1 = ExpressionFactory.expressionOfType(Expression.EQUAL_TO);
        query.andQualifier(e1);
        assertSame(e1, query.getQualifier());

        Expression e2 = ExpressionFactory.expressionOfType(Expression.NOT_EQUAL_TO);
        query.andQualifier(e2);
        assertEquals(Expression.AND, query.getQualifier().getType());
    }

    public void testOrQualifier() {
        QualifiedQuery query = new MockQualifiedQuery();
        assertNull(query.getQualifier());

        Expression e1 = ExpressionFactory.expressionOfType(Expression.EQUAL_TO);
        query.orQualifier(e1);
        assertSame(e1, query.getQualifier());

        Expression e2 = ExpressionFactory.expressionOfType(Expression.NOT_EQUAL_TO);
        query.orQualifier(e2);
        assertEquals(Expression.OR, query.getQualifier().getType());
    }
}
