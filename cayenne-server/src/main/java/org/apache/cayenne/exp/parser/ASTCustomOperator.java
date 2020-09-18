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

import java.io.IOException;

import org.apache.cayenne.exp.Expression;

/**
 * @since 4.2
 */
public class ASTCustomOperator extends SimpleNode {

    private String operator;

    public ASTCustomOperator(int id) {
        super(id);
    }

    public ASTCustomOperator(String operator) {
        super(ExpressionParser.JJTCUSTOMOPERATOR);
        this.operator = operator;
    }

    public ASTCustomOperator(String operator, Object[] nodes) {
        super(ExpressionParser.JJTCUSTOMOPERATOR);
        this.operator = operator;
        int len = nodes.length;
        for (int i = 0; i < len; i++) {
            jjtAddChild(wrapChild(nodes[i]), i);
        }

        connectChildren();
    }

    @Override
    public void jjtAddChild(Node n, int i) {
        // First argument should be used as an operator when created by the expression parser
        if(operator == null && i == 0) {
            if(!(n instanceof ASTScalar)) {
                throw new IllegalArgumentException("ASTScalar expected, got " + n);
            }
            this.operator = ((ASTScalar) n).getValue().toString();
            return;
        }
        super.jjtAddChild(n, operator != null ? i : --i);
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        throw new UnsupportedOperationException("Can't evaluate custom operator in memory");
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    @Override
    public void appendAsString(Appendable out) throws IOException {
        out.append("op(\"").append(operator).append("\"");
        if ((children != null) && (children.length > 0)) {
            for (Node child : children) {
                out.append(", ");
                if (child == null) {
                    out.append("null");
                } else {
                    ((SimpleNode) child).appendAsString(out);
                }
            }
        }
        out.append(")");
    }

    @Override
    public int getType() {
        return Expression.CUSTOM_OP;
    }

    public String getOperator() {
        return operator;
    }

    @Override
    protected String getExpressionOperator(int index) {
        return operator;
    }

    @Override
    public Expression shallowCopy() {
        return new ASTCustomOperator(operator);
    }
}
