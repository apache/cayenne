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
 * "Not equal to" expression.
 * 
 */
public class ASTNotEqual extends ConditionNode {
    ASTNotEqual(int id) {
        super(id);
    }

    public ASTNotEqual() {
        super(ExpressionParserTreeConstants.JJTNOTEQUAL);
    }

    /**
     * Creates "Not Equal To" expression.
     */
    public ASTNotEqual(ASTPath path, Object value) {
        super(ExpressionParserTreeConstants.JJTNOTEQUAL);
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

        Object o1 = evaluateChild(0, o);
        Object o2 = evaluateChild(1, o);
        return !ASTEqual.evaluateImpl(o1, o2);
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    @Override
    public Expression shallowCopy() {
        return new ASTNotEqual(id);
    }

    @Override
    protected String getExpressionOperator(int index) {
        return "!=";
    }
    
    @Override
    protected String getEJBQLExpressionOperator(int index) {
        if (jjtGetChild(1) instanceof ASTScalar && ((ASTScalar) jjtGetChild(1)).getValue() == null) {
            //for ejbql, we need "is not null" instead of "!= null"
            return "is not";
        }
        return "<>";
    }

    @Override
    public int getType() {
        return Expression.NOT_EQUAL_TO;
    }
}
