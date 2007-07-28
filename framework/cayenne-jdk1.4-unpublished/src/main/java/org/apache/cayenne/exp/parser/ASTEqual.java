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

import java.math.BigDecimal;

import org.apache.cayenne.exp.Expression;

/**
 * "Equal To" expression.
 * 
 * @since 1.1
 * @author Andrus Adamchik
 */
public class ASTEqual extends ConditionNode {
    /**
     * Constructor used by expression parser. Do not invoke directly.
     */
    ASTEqual(int id) {
        super(id);
    }

    public ASTEqual() {
        super(ExpressionParserTreeConstants.JJTEQUAL);
    }

    /**
     * Creates "Equal To" expression.
     */
    public ASTEqual(ASTPath path, Object value) {
        super(ExpressionParserTreeConstants.JJTEQUAL);
        jjtAddChild(path, 0);
        jjtAddChild(new ASTScalar(value), 1);
        connectChildren();
    }

    protected Object evaluateNode(Object o) throws Exception {
        int len = jjtGetNumChildren();
        if (len != 2) {
            return Boolean.FALSE;
        }

        Object o1 = evaluateChild(0, o);
        Object o2 = evaluateChild(1, o);

        // TODO: maybe we need a comparison "strategy" here, instead of
        // a switch of all possible cases? ... there were other requests for
        // more relaxed type-unsafe comparison (e.g. numbers to strings)

        if (o1 == null && o2 == null) {
            return Boolean.TRUE;
        }
        else if (o1 != null) {
            // BigDecimals must be compared using compareTo (
            // see CAY-280 and BigDecimal.equals JavaDoc)
            if (o1 instanceof BigDecimal) {
                if (o2 instanceof BigDecimal) {
                    return ((BigDecimal) o1).compareTo((BigDecimal) o2) == 0
                            ? Boolean.TRUE
                            : Boolean.FALSE;
                }

                return Boolean.FALSE;
            }

            return o1.equals(o2) ? Boolean.TRUE : Boolean.FALSE;
        }
        else {
            return Boolean.FALSE;
        }
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    public Expression shallowCopy() {
        return new ASTEqual(id);
    }

    protected String getExpressionOperator(int index) {
        return "=";
    }

    public int getType() {
        return Expression.EQUAL_TO;
    }
}
