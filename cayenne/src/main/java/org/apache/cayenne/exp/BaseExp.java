/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.exp;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.util.Util;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Superclass of the expression node classes: a tree node holding its operands. An operand is either a nested node
 * or a plain value; a {@link ScalarExp} passed as an operand is unwrapped to its value, so a scalar node only ever
 * appears as a root expression of its own. A node does not know its parent, so it can be shared between trees.
 *
 * @since 5.0
 */
public abstract sealed class BaseExp extends Expression permits AggregateConditionExp, AsteriskExp, CaseWhenExp,
        ConditionExp, CustomOperatorExp, ElseExp, EnclosingObjectExp, FullObjectExp, ListExp, NegateExp, PathExp,
        ScalarExp, SubqueryExp, ThenExp, ValueExp {
    protected Object[] operands;

    protected BaseExp(Object... operands) {
        if (operands != null && operands.length > 0) {
            setOperands(operands);
        }
        // else - a bare node
    }

    /**
     * Always returns empty map.
     */
    @Override
    public Map<String, String> getPathAliases() {
        return Collections.emptyMap();
    }

    protected abstract String getExpressionOperator(int index);

    /**
     * Returns operator for EJBQL statements, which can differ for Cayenne expression operator
     */
    protected String getEJBQLExpressionOperator(int index) {
        return getExpressionOperator(index);
    }

    @Override
    protected boolean pruneNodeForPrunedChild(Object prunedChild) {
        return true;
    }

    /**
     * Returns the node name: the class name without the "Exp" suffix.
     */
    @Override
    public String expName() {
        String name = getClass().getSimpleName();
        return name.endsWith("Exp") ? name.substring(0, name.length() - 3) : name;
    }

    /**
     * Flattens the tree under this node by eliminating any operands that are nodes of the same class as this node
     * and copying their operands to this node.
     */
    @Override
    protected void flattenTree() {
        if (operands == null) {
            return;
        }

        boolean shouldFlatten = false;
        int newSize = 0;

        for (Object operand : operands) {
            if (operand != null && operand.getClass() == getClass()) {
                shouldFlatten = true;
                newSize += ((BaseExp) operand).getOperandCount();
            } else {
                newSize++;
            }
        }

        if (shouldFlatten) {
            Object[] newOperands = new Object[newSize];
            int j = 0;

            for (Object operand : operands) {
                if (operand != null && operand.getClass() == getClass()) {
                    BaseExp nested = (BaseExp) operand;
                    for (int k = 0; k < nested.getOperandCount(); ++k) {
                        newOperands[j++] = nested.operands[k];
                    }
                } else {
                    newOperands[j++] = operand;
                }
            }

            if (j != newSize) {
                throw new ExpressionException("Assertion error: " + j + " != " + newSize);
            }

            this.operands = newOperands;
        }
    }

    @Override
    public void appendAsString(Appendable out) throws IOException {
        int count = getOperandCount();
        for (int i = 0; i < count; ++i) {
            if (i > 0) {
                out.append(' ');
                out.append(getExpressionOperator(i));
                out.append(' ');
            }

            appendOperandAsString(i, out);
        }
    }

    /**
     * Whether this node is wrapped in parentheses when printed as an operand of another node. Compound nodes are,
     * leaves and nodes with their own delimiters (function calls, lists) are not.
     */
    protected boolean parenthesizeAsOperand() {
        return true;
    }

    /**
     * Same as {@link #parenthesizeAsOperand()}, for the EJBQL form of the node. The two forms differ for a node that
     * is a function call in one and an infix operator in the other.
     */
    protected boolean parenthesizeAsEJBQLOperand() {
        return parenthesizeAsOperand();
    }

    /**
     * Appends the operand at the given index to the output: a nested node as itself, wrapped in parentheses if it
     * asks for it via {@link #parenthesizeAsOperand()}, a plain value as a literal.
     */
    protected void appendOperandAsString(int index, Appendable out) throws IOException {
        // print the operand as stored, not via getOperand(..): an unresolved enum prints without loading its class
        Object operand = operands[index];
        if (!(operand instanceof BaseExp node)) {
            ExpHelper.appendScalarAsString(out, operand, '\"');
            return;
        }

        boolean parenthesize = node.parenthesizeAsOperand();
        if (parenthesize) {
            out.append('(');
        }
        node.appendAsString(out);
        if (parenthesize) {
            out.append(')');
        }
    }

    @Override
    public Object getOperand(int index) {
        if (operands == null) {
            throw new ArrayIndexOutOfBoundsException(index);
        }

        Object operand = operands[index];

        // an enum parsed from a String is resolved on access, so that an expression can be parsed and printed
        // without the enum class being available, e.g. in the Modeler
        return operand instanceof EnumExp.EnumValue enumValue ? enumValue.resolve() : operand;
    }

    @Override
    public int getOperandCount() {
        return operands == null ? 0 : operands.length;
    }

    /**
     * Sets the operand at the given position, growing the operands array if needed. A {@link ScalarExp} is unwrapped
     * to its value and a {@link Persistent} is replaced with its ObjectId. A nested node is asked first whether this
     * node is a valid parent for it, see {@link #isValidParent(BaseExp)}.
     */
    @Override
    public void setOperand(int index, Object value) {
        Object operand = switch (value) {
            case ScalarExp scalar -> scalar.value;
            case Persistent persistent -> persistent.getObjectId();
            case null, default -> value;
        };

        if (operand instanceof BaseExp node && !node.isValidParent(this)) {
            throw new ExpressionException(node.expName() + ": invalid parent - " + expName());
        }

        if (operands == null) {
            operands = new Object[index + 1];
        } else if (index >= operands.length) {
            Object[] grown = new Object[index + 1];
            System.arraycopy(operands, 0, grown, 0, operands.length);
            operands = grown;
        }
        operands[index] = operand;
    }

    /**
     * Replaces all the operands of this node with the given ones. Called by the constructor and by the parser once
     * all the operands of a node are known, so a node that post-processes its operands overrides this method.
     */
    public void setOperands(Object... operands) {
        this.operands = null;
        if (operands != null) {
            for (int i = 0; i < operands.length; i++) {
                setOperand(i, operands[i]);
            }
        }
    }

    /**
     * Whether this node can be an operand of the given node. Called by the parent when the operand is set, to check
     * what the grammar can't: e.g. a condition can only be an operand of a condition aggregate. By default any parent
     * is valid.
     */
    protected boolean isValidParent(BaseExp parent) {
        return true;
    }

    /**
     * Evaluates itself with object, pushing result on the stack.
     */
    protected abstract Object evaluateNode(Object o) throws Exception;

    /**
     * Evaluates the operand at the given index: a nested node is evaluated against the object, a plain value is
     * returned as is.
     */
    protected Object evaluateOperand(int index, Object o) throws Exception {
        Object operand = getOperand(index);
        return switch (operand) {
            case BaseExp node -> node.evaluate(o);
            case ExpressionParameter parameter -> throw new ExpressionException(
                    "Uninitialized parameter: " + parameter + ", call 'params' first.");
            case null, default -> operand;
        };
    }

    @Override
    public Expression notExp() {
        return new NotExp(this);
    }

    @Override
    public Expression exists() {
        throw new UnsupportedOperationException("Can't use exists() operator with this expression");
    }

    @Override
    public Expression notExists() {
        throw new UnsupportedOperationException("Can't use not exists() operator with this expression");
    }

    @Override
    public Object evaluate(Object o) {
        // wrap in try/catch to provide unified exception processing
        try {
            return evaluateNode(o);
        } catch (Throwable th) {
            String string = this.toString();
            throw new ExpressionException("Error evaluating expression '%s'",
                    string, Util.unwindException(th), string);
        }
    }

    public void appendAsEJBQL(Appendable out, String rootId) throws IOException {
        appendAsEJBQL(null, out, rootId);
    }

    @Override
    public void appendAsEJBQL(List<Object> parameterAccumulator, Appendable out, String rootId) throws IOException {
        if (getOperandCount() > 0) {
            appendOperandsAsEJBQL(parameterAccumulator, out, rootId);
        }
    }

    /**
     * Encodes the operands of this node to EJBQL, separated by the operator.
     */
    protected void appendOperandsAsEJBQL(List<Object> parameterAccumulator, Appendable out, String rootId)
            throws IOException {
        int count = getOperandCount();
        for (int i = 0; i < count; ++i) {
            if (i > 0) {
                out.append(' ');
                out.append(getEJBQLExpressionOperator(i));
                out.append(' ');
            }

            appendOperandAsEJBQL(i, parameterAccumulator, out, rootId);
        }
    }

    /**
     * Encodes the operand at the given index to EJBQL: a nested node as itself, wrapped in parentheses if it asks for
     * it via {@link #parenthesizeAsEJBQLOperand()}, a plain value as a literal or a positional parameter.
     */
    protected void appendOperandAsEJBQL(int index, List<Object> parameterAccumulator, Appendable out, String rootId)
            throws IOException {
        Object operand = getOperand(index);
        if (!(operand instanceof BaseExp node)) {
            ExpHelper.encodeOperandAsEJBQL(parameterAccumulator, out, operand);
            return;
        }

        boolean parenthesize = node.parenthesizeAsEJBQLOperand();
        if (parenthesize) {
            out.append('(');
        }
        node.appendAsEJBQL(parameterAccumulator, out, rootId);
        if (parenthesize) {
            out.append(')');
        }
    }
}
