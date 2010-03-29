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

import java.io.PrintWriter;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.util.ConversionUtil;

/**
 * "Not" expression.
 * 
 * @since 1.1
 */
public class ASTNot extends AggregateConditionNode {

    ASTNot(int id) {
        super(id);
    }

    public ASTNot() {
        super(ExpressionParserTreeConstants.JJTNOT);
    }

    public ASTNot(Node expression) {
        super(ExpressionParserTreeConstants.JJTNOT);
        jjtAddChild(expression, 0);
        connectChildren();
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        int len = jjtGetNumChildren();
        if (len == 0) {
            return Boolean.FALSE;
        }

        return ConversionUtil.toBoolean(evaluateChild(0, o))
                ? Boolean.FALSE
                : Boolean.TRUE;
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    @Override
    public Expression shallowCopy() {
        return new ASTNot(id);
    }

    @Override
    public int getType() {
        return Expression.NOT;
    }

    @Override
    public void encodeAsString(PrintWriter pw) {
        pw.print("not ");
        super.encodeAsString(pw);
    }

    /**
     * @since 3.0
     */
    @Override
    public void encodeAsEJBQL(PrintWriter pw, String rootId) {
        encodeAsString(pw);
    }

    @Override
    protected String getExpressionOperator(int index) {
        throw new UnsupportedOperationException("No operator for '"
                + ExpressionParserTreeConstants.jjtNodeName[id]
                + "'");
    }
}
