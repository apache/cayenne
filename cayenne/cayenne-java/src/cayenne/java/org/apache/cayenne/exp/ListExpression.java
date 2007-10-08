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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.Transformer;

/**
 * An expression with a varying number of operands. Usually this is used for the list
 * expressions when the list size may vary.
 * 
 * @author Andrus Adamchik
 * @deprecated since 1.2, replaced by {@link org.apache.cayenne.exp.parser.ASTList}.
 */
public class ListExpression extends Expression {

    protected List operands = new ArrayList();

    public ListExpression() {
    }

    public ListExpression(int type) {
        this.type = type;
    }

    public Object evaluate(Object o) {
        return ASTCompiler.compile(this).evaluateASTChain(o);
    }

    public Expression notExp() {
        Expression exp = ExpressionFactory.expressionOfType(Expression.NOT);
        exp.setOperand(0, this);
        return exp;
    }

    protected void flattenTree() {

    }

    protected boolean pruneNodeForPrunedChild(Object prunedChild) {
        return false;
    }

    protected Object transformExpression(Transformer transformer) {
        Object transformed = super.transformExpression(transformer);

        if (!(transformed instanceof ListExpression)) {
            return transformed;
        }

        ListExpression listExpression = (ListExpression) transformed;

        // prune itself if the transformation resulted in
        // no children or a single child
        switch (listExpression.getOperandCount()) {
            case 1:
                return listExpression.getOperand(0);
            case 0:
                return PRUNED_NODE;
            default:
                return listExpression;
        }
    }

    /**
     * Creates a copy of this expression node, without copying children.
     * 
     * @since 1.1
     */
    public Expression shallowCopy() {
        return new ListExpression(type);
    }

    /**
     * Returns the number of operands currently in the list.
     */
    public int getOperandCount() {
        return operands.size();
    }

    /**
     * @see org.apache.cayenne.exp.Expression#getOperand(int)
     */
    public Object getOperand(int index) {
        if (operands.size() <= index) {
            throw new IllegalArgumentException("Attempt to retrieve operand "
                    + index
                    + ", while current number of operands is "
                    + operands.size());
        }

        return operands.get(index);
    }

    /**
     * 
     */
    public void setOperand(int index, Object value) {
        if (operands.size() == index) {
            appendOperand(value);
        }
        else if (operands.size() > index) {
            operands.set(index, value);
        }
        else {
            throw new IllegalArgumentException("Attempt to set operand "
                    + index
                    + ", while current number of operands is "
                    + operands.size());
        }
    }

    public void appendOperand(Object value) {
        operands.add(value);
    }

    public void appendOperands(Collection operands) {
        this.operands.addAll(operands);
    }

    public void removeOperand(Object value) {
        operands.remove(value);
    }

    /**
     * In case requested expression type is the same as internal type, creates and returns
     * a copy of this expression with the internal list of operands expanded with the new
     * expression. If the type of expression is different from this, calls superclass's
     * implementation.
     */
    public Expression joinExp(int type, Expression exp) {
        if (type != this.type) {
            return super.joinExp(type, exp);
        }

        // create a copy of self
        ListExpression copy = new ListExpression();
        copy.setType(type);
        copy.appendOperands(operands);
        copy.appendOperand(exp);

        return copy;
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
