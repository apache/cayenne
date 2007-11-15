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
import java.math.BigDecimal;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.util.ConversionUtil;

/**
 * "Negate" expression.
 * 
 * @since 1.1
 * @author Andrus Adamchik
 */
public class ASTNegate extends SimpleNode {
    ASTNegate(int id) {
        super(id);
    }

    public ASTNegate() {
        super(ExpressionParserTreeConstants.JJTNEGATE);
    }

    public ASTNegate(Object node) {
        super(ExpressionParserTreeConstants.JJTNEGATE);
        jjtAddChild(wrapChild(node), 0);
        connectChildren();
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    public Expression shallowCopy() {
        return new ASTNegate(id);
    }

    protected Object evaluateNode(Object o) throws Exception {
        int len = jjtGetNumChildren();
        if (len == 0) {
            return null;
        }

        BigDecimal result = ConversionUtil.toBigDecimal(evaluateChild(0, o));
        return result != null ? result.negate() : null;
    }

    public void encodeAsString(PrintWriter pw) {
        if ((children != null) && (children.length > 0)) {
            pw.print("-");

            SimpleNode child = (SimpleNode) children[0];

            // don't call super - we have our own parenthesis policy 
            boolean useParen =
                parent != null
                    && !((child instanceof ASTScalar) || (child instanceof ASTPath));
            if (useParen) {
                pw.print("(");
            }

            child.encodeAsString(pw);

            if (useParen) {
                pw.print(')');
            }
        }
    }

    protected String getExpressionOperator(int index) {
        throw new UnsupportedOperationException(
            "No operator for '" + ExpressionParserTreeConstants.jjtNodeName[id] + "'");
    }

    public int getType() {
        return Expression.NEGATIVE;
    }

    public int getOperandCount() {
        return 1;
    }
}
