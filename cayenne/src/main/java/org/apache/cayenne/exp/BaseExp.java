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

import org.apache.cayenne.util.Util;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Superclass of the expression node classes: a tree node holding its children. A node does not know its parent, so
 * it can be shared between trees.
 *
 * @since 5.0
 */
public abstract class BaseExp extends Expression {
    protected BaseExp[] children;

    protected BaseExp(Object... operands) {
        if (operands != null && operands.length > 0) {

            // presize the array and bypass the overridable setOperand(): subclasses are not initialized yet
            children = new BaseExp[operands.length];
            for (int i = 0; i < operands.length; i++) {
                // a null operand is a null scalar value, not a missing child
                BaseExp child = operands[i] != null ? wrapChild(operands[i]) : new ScalarExp();
                addChild(child, i);
            }
            childrenAdded();
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
     * Returns the node name: the class name without the "AST" prefix.
     */
    @Override
    public String expName() {
        String name = getClass().getSimpleName();
        return name.startsWith("AST") ? name.substring(3) : name;
    }

    /**
     * Flattens the tree under this node by eliminating any children that are of
     * the same class as this node and copying their children to this node.
     */
    @Override
    protected void flattenTree() {
        boolean shouldFlatten = false;
        int newSize = 0;

        for (BaseExp child : children) {
            if (child.getClass() == getClass()) {
                shouldFlatten = true;
                newSize += child.getChildCount();
            } else {
                newSize++;
            }
        }

        if (shouldFlatten) {
            BaseExp[] newChildren = new BaseExp[newSize];
            int j = 0;

            for (BaseExp c : children) {
                if (c.getClass() == getClass()) {
                    for (int k = 0; k < c.getChildCount(); ++k) {
                        newChildren[j++] = c.getChild(k);
                    }
                } else {
                    newChildren[j++] = c;
                }
            }

            if (j != newSize) {
                throw new ExpressionException("Assertion error: " + j + " != " + newSize);
            }

            this.children = newChildren;
        }
    }

    @Override
    public void appendAsString(Appendable out) throws IOException {
        if ((children != null) && (children.length > 0)) {
            for (int i = 0; i < children.length; ++i) {
                if (i > 0) {
                    out.append(' ');
                    out.append(getExpressionOperator(i));
                    out.append(' ');
                }

                appendChildAsString(i, out);
            }
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
     * Appends the child at the given index to the output, wrapped in parentheses if the child asks for it via
     * {@link #parenthesizeAsOperand()}.
     */
    protected void appendChildAsString(int index, Appendable out) throws IOException {
        BaseExp child = children[index];
        if (child == null) {
            out.append("null");
            return;
        }

        boolean parenthesize = child.parenthesizeAsOperand();
        if (parenthesize) {
            out.append('(');
        }
        child.appendAsString(out);
        if (parenthesize) {
            out.append(')');
        }
    }

    @Override
    public Object getOperand(int index) {
        BaseExp child = getChild(index);

        // unwrap ScalarExp nodes - this is likely a temporary thing to keep it compatible
        // with QualifierTranslator. In the future we might want to keep scalar nodes
        // for the purpose of expression evaluation.
        return unwrapChild(child);
    }

    protected BaseExp wrapChild(Object child) {
        // when child is null, there's no way of telling whether this is a scalar or not... fuzzy...
        // maybe we should stop using this method - it is too generic
        return (child instanceof BaseExp || child == null) ? (BaseExp) child : new ScalarExp(child);
    }

    protected Object unwrapChild(BaseExp child) {
        return (child instanceof ScalarExp) ? ((ScalarExp) child).getValue() : child;
    }

    @Override
    public int getOperandCount() {
        return getChildCount();
    }

    @Override
    public void setOperand(int index, Object value) {
        addChild(wrapChild(value), index);
    }

    /**
     * A hook called by the parser once all the children of this node are added.
     */
    public void childrenAdded() {
    }

    /**
     * Sets a child at the given position, growing the children array if needed. The child is asked first whether this
     * node is a valid parent for it, see {@link #isValidParent(BaseExp)}.
     */
    public void addChild(BaseExp child, int i) {
        if (child != null && !child.isValidParent(this)) {
            throw new ExpressionException(child.expName() + ": invalid parent - " + expName());
        }

        if (children == null) {
            children = new BaseExp[i + 1];
        } else if (i >= children.length) {
            BaseExp[] c = new BaseExp[i + 1];
            System.arraycopy(children, 0, c, 0, children.length);
            children = c;
        }
        children[i] = child;
    }

    /**
     * Whether this node can be an operand of the given node. Called by the parent when the child is added, to check
     * what the grammar can't: e.g. a condition can only be an operand of a condition aggregate. By default any parent
     * is valid.
     */
    protected boolean isValidParent(BaseExp parent) {
        return true;
    }

    /**
     * Returns a child node as is, without unwrapping scalars the way {@link #getOperand(int)} does.
     */
    public BaseExp getChild(int i) {
        return children[i];
    }

    public final int getChildCount() {
        return (children == null) ? 0 : children.length;
    }

    /**
     * Evaluates itself with object, pushing result on the stack.
     */
    protected abstract Object evaluateNode(Object o) throws Exception;

    protected Object evaluateChild(int index, Object o) throws Exception {
        BaseExp node = getChild(index);
        return node != null ? node.evaluate(o) : null;
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
        if ((children != null) && (children.length > 0)) {
            appendChildrenAsEJBQL(parameterAccumulator, out, rootId);
        }
    }

    /**
     * Encodes child of this node with specified index to EJBQL
     */
    protected void appendChildrenAsEJBQL(List<Object> parameterAccumulator, Appendable out, String rootId)
            throws IOException {
        for (int i = 0; i < children.length; ++i) {
            if (i > 0) {
                out.append(' ');
                out.append(getEJBQLExpressionOperator(i));
                out.append(' ');
            }

            appendChildAsEJBQL(i, parameterAccumulator, out, rootId);
        }
    }

    /**
     * Encodes the child at the given index to EJBQL, wrapped in parentheses if the child asks for it via
     * {@link #parenthesizeAsEJBQLOperand()}.
     */
    protected void appendChildAsEJBQL(int index, List<Object> parameterAccumulator, Appendable out, String rootId)
            throws IOException {
        BaseExp child = children[index];
        if (child == null) {
            out.append("null");
            return;
        }

        boolean parenthesize = child.parenthesizeAsEJBQLOperand();
        if (parenthesize) {
            out.append('(');
        }
        child.appendAsEJBQL(parameterAccumulator, out, rootId);
        if (parenthesize) {
            out.append(')');
        }
    }
}
