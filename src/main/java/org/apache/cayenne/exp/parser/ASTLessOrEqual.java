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
import org.apache.cayenne.util.ConversionUtil;

/**
 * "Less than or equal to" expression.
 * 
 * @since 1.1
 * @author Andrus Adamchik
 */
public class ASTLessOrEqual extends ConditionNode {

    /**
     * Constructor used by expression parser. Do not invoke directly.
     */
    ASTLessOrEqual(int id) {
        super(id);
    }

    public ASTLessOrEqual() {
        super(ExpressionParserTreeConstants.JJTLESSOREQUAL);
    }

    public ASTLessOrEqual(ASTPath path, Object value) {
        super(ExpressionParserTreeConstants.JJTLESSOREQUAL);
        jjtAddChild(path, 0);
        jjtAddChild(new ASTScalar(value), 1);
        connectChildren();
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        int len = jjtGetNumChildren();
        if (len != 2) {
            return Boolean.FALSE;
        }

        Comparable c1 = ConversionUtil.toComparable(evaluateChild(0, o));
        if (c1 == null) {
            return Boolean.FALSE;
        }

        Comparable c2 = ConversionUtil.toComparable(evaluateChild(1, o));
        if (c2 == null) {
            return Boolean.FALSE;
        }

        return c1.compareTo(c2) <= 0 ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    @Override
    public Expression shallowCopy() {
        return new ASTLessOrEqual(id);
    }

    @Override
    protected String getExpressionOperator(int index) {
        return "<=";
    }

    @Override
    public int getType() {
        return Expression.LESS_THAN_EQUAL_TO;
    }
}
