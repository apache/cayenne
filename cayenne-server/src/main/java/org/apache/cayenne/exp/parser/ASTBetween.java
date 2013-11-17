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

import org.apache.cayenne.exp.Expression;

/**
 * "Between" expression.
 * 
 * @since 1.1
 */
public class ASTBetween extends ConditionNode {

    ASTBetween(int id) {
        super(id);
    }

    public ASTBetween() {
        super(ExpressionParserTreeConstants.JJTBETWEEN);
    }

    public ASTBetween(ASTPath path, Object value1, Object value2) {
        super(ExpressionParserTreeConstants.JJTBETWEEN);
        jjtAddChild(path, 0);
        jjtAddChild(new ASTScalar(value1), 1);
        jjtAddChild(new ASTScalar(value2), 2);

        connectChildren();
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        int len = jjtGetNumChildren();
        if (len != 3) {
            return Boolean.FALSE;
        }

        Object o1 = evaluateChild(0, o);
        Object o2 = evaluateChild(1, o);
        Object o3 = evaluateChild(2, o);
        Evaluator e = Evaluator.evaluator(o1);

        return e.compare(o1, o2) >= 0 && e.compare(o1, o3) <= 0 ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    @Override
    public Expression shallowCopy() {
        return new ASTBetween(id);
    }

    @Override
    protected String getExpressionOperator(int index) {
        return (index == 2) ? "and" : "between";
    }

    @Override
    public int getType() {
        return Expression.BETWEEN;
    }
}
