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
 * @since 3.1
 */
public class ASTBitwiseNot extends SimpleNode {

    ASTBitwiseNot(int id) {
        super(id);
    }

    public ASTBitwiseNot() {

        // TODO: parser support
        super(-1);
    }

    public ASTBitwiseNot(SimpleNode expression) {
        // TODO: parser support
        super(-1);

        jjtAddChild(expression, 0);
        connectChildren();
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        int len = jjtGetNumChildren();
        if (len != 1) {
            return Boolean.FALSE;
        }

        long value = ConversionUtil.toLong(evaluateChild(0, o), Long.MIN_VALUE);

        if (value == Long.MIN_VALUE) {
            return null;
        }

        return ~value;
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    @Override
    public Expression shallowCopy() {
        return new ASTBitwiseNot(id);
    }

    @Override
    protected String getExpressionOperator(int index) {
        return "~";
    }

    @Override
    protected String getEJBQLExpressionOperator(int index) {
        throw new UnsupportedOperationException("EJBQL 'bitwise not' is not supported");
    }

    @Override
    public int getType() {
        return Expression.BITWISE_NOT;
    }
}
