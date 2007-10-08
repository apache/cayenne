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


package org.apache.cayenne.exp;

import java.io.PrintWriter;

/**
 * Generic unary expression. Describes expression in a form: "<tt>operation operand</tt>".
 * SQL example of unary expression is NOT expression.
 * 
 * @deprecated since 1.2
 * @author Andrus Adamchik
 */
public class UnaryExpression extends Expression {

    protected Object operand;

    public UnaryExpression() {
    }

    public UnaryExpression(int type) {
        this.type = type;
    }

    public Expression notExp() {
        Expression exp = ExpressionFactory.expressionOfType(Expression.NOT);
        exp.setOperand(0, this);
        return exp;
    }

    public Object evaluate(Object o) {
        return ASTCompiler.compile(this).evaluateASTChain(o);
    }

    protected void flattenTree() {

    }

    protected boolean pruneNodeForPrunedChild(Object prunedChild) {
        return true;
    }

    /**
     * Creates a copy of this expression node, without copying children.
     * 
     * @since 1.1
     */
    public Expression shallowCopy() {
        return new UnaryExpression(type);
    }

    public final int getOperandCount() {
        return 1;
    }

    public Object getOperand(int index) {
        if (index == 0)
            return operand;

        throw new IllegalArgumentException("Invalid operand index for UnaryExpression: "
                + index);
    }

    public void setOperand(int index, Object value) {
        if (index == 0) {
            operand = value;
            return;
        }

        throw new IllegalArgumentException("Invalid operand index for UnaryExpression: "
                + index);
    }

    /**
     * @since 1.1
     */
    public void encodeAsString(PrintWriter pw) {
        for (int i = 0; i < getOperandCount(); i++) {
            if (i > 0 || getOperandCount() == 1) {
                pw.print(" ");
                pw.print(expName());
                pw.print(" ");
            }

            Object op = getOperand(i);
            if (op == null) {
                pw.print("<null>");
            }
            else if (op instanceof String) {
                pw.print("'" + op + "'");
            }
            else if (op instanceof Expression) {
                pw.print('(');
                ((Expression) op).encodeAsString(pw);
                pw.print(')');
            }
            else {
                pw.print(String.valueOf(op));
            }
        }
    }
}
