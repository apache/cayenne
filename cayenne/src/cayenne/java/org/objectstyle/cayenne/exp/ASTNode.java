/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.exp;

import java.util.Collection;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;
import org.apache.oro.text.perl.Perl5Util;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.util.Util;

/**
 * A node of the Abstract Syntax Tree (AST) for a compiled Cayenne expression. 
 * ASTNode is implemented as a linked list holding a reference to the next node 
 * in the chain. This way, a chain of AST nodes can be evaluated by calling 
 * a method on the starting node. ASTNodes also has abstract API for various 
 * optimizations based on conditional execution. 
 * 
 * <p>Also serves as a factory for specialized nodes handling various operations.</p>
 * 
 * @since 1.0.6
 * @author Andrei Adamchik
 */
abstract class ASTNode {
    private static Logger logObj = Logger.getLogger(ASTNode.class);

    // used by all regex processing nodes
    // note that according to the docs it is synchronized
    private static final Perl5Util regexUtil = new Perl5Util();

    protected ASTNode nextNode;

    static ASTNode buildObjectNode(Object object, ASTNode parent) {
        ASTNode node = new PushNode(object);
        return parent != null ? parent.wrapChildNode(node) : node;
    }

    static ASTNode buildExpressionNode(Expression expression, ASTNode parent) {
        ASTNode node;

        switch (expression.getType()) {
            case Expression.OBJ_PATH :
            case Expression.DB_PATH :
                node = new PropertyNode(expression);
                break;
            case Expression.EQUAL_TO :
                node = new EqualsNode();
                break;
            case Expression.OR :
                node = new OrNode();
                break;
            case Expression.AND :
                node = new AndNode();
                break;
            case Expression.NOT :
                node = new NotNode();
                break;
            case Expression.LIST :
                node = new ListNode(expression.getOperand(0));
                break;
            case Expression.NOT_EQUAL_TO :
                node = new NotEqualsNode();
                break;
            case Expression.LESS_THAN :
                node = new LessThanNode();
                break;
            case Expression.LESS_THAN_EQUAL_TO :
                node = new LessThanEqualsToNode();
                break;
            case Expression.GREATER_THAN :
                node = new GreaterThanNode();
                break;
            case Expression.GREATER_THAN_EQUAL_TO :
                node = new GreaterThanEqualsToNode();
                break;
            case Expression.BETWEEN :
                node = new BetweenNode(false);
                break;
            case Expression.NOT_BETWEEN :
                node = new BetweenNode(true);
                break;
            case Expression.IN :
                node = new InNode(false);
                break;
            case Expression.NOT_IN :
                node = new InNode(true);
                break;
            case Expression.LIKE :
                node = new LikeNode((String) expression.getOperand(1), false, false);
                break;
            case Expression.NOT_LIKE :
                node = new LikeNode((String) expression.getOperand(1), true, false);
                break;
            case Expression.LIKE_IGNORE_CASE :
                node = new LikeNode((String) expression.getOperand(1), false, true);
                break;
            case Expression.NOT_LIKE_IGNORE_CASE :
                node = new LikeNode((String) expression.getOperand(1), true, true);
                break;
            default :
                throw new ExpressionException(
                    "Unsupported expression type: "
                        + expression.getType()
                        + " ("
                        + expression.expName()
                        + ")");
        }

        return parent != null ? parent.wrapChildNode(node) : node;
    }

    private static boolean contains(Object[] objects, Object object) {
        int size = objects.length;

        for (int i = 0; i < size; i++) {
            if (Util.nullSafeEquals(objects[i], object)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Optionally can wrap a child node with a reference wrapper.
     * Default implementation simply returns childNode unchanged.
     */
    ASTNode wrapChildNode(ASTNode childNode) {
        return childNode;
    }

    ASTNode getNextNode() {
        return nextNode;
    }

    void setNextNode(ASTNode nextNode) {
        this.nextNode = nextNode;
    }

    abstract void appendString(StringBuffer buffer);

    /**
     * Evaluates a chain of ASTNodes, starting from this node, in the context
     * of a JavaBean object that will provide property values for the path nodes. 
     * The result is converted to boolean.
     */
    boolean evaluateBooleanASTChain(Object bean) throws ExpressionException {
        return ASTStack.booleanFromObject(evaluateASTChain(bean));
    }

    /**
     * Evaluates a chain of ASTNodes, starting from this node, in the context
     * of a JavaBean object that will provide property values for the path nodes.
     */
    Object evaluateASTChain(Object bean) throws ExpressionException {
        ASTNode currentNode = this;
        ASTStack stack = new ASTStack();

        // wrap in try/catch to provide unified exception processing
        try {
            while ((currentNode = currentNode.evaluateWithObject(stack, bean)) != null) {
                // empty loop, all the action happens in condition part
            }

            return stack.pop();
        }
        catch (Throwable th) {
            if (th instanceof ExpressionException) {
                throw (ExpressionException) th;
            }
            else {
                throw new ExpressionException(
                    "Error evaluating expression.",
                    this.toString(),
                    Util.unwindException(th));
            }
        }
    }

    /**
     * Evaluates expression using stack for the current values and
     * an object parameter as a context for evaluation. Returns the next 
     * ASTNode to evaluate or null if evaluation is complete.
     */
    abstract ASTNode evaluateWithObject(ASTStack stack, Object bean);

    public String toString() {
        // appends to a buffer producing a reverse polish notation (RPN)
        StringBuffer buffer = new StringBuffer();

        for (ASTNode node = this; node != null; node = node.getNextNode()) {
            node.appendString(buffer);
        }

        return buffer.toString();
    }

    // ***************** concrete subclasses

    final static class PushNode extends ASTNode {
        Object value;

        PushNode(Object value) {
            this.value = value;
        }

        ASTNode evaluateWithObject(ASTStack stack, Object bean) {
            stack.push(value);
            return nextNode;
        }

        void appendString(StringBuffer buffer) {
            buffer.append("%@");
        }
    }

    final static class ListNode extends ASTNode {
        Object[] value;

        ListNode(Object object) {
            // TODO: maybe it makes sense to sort the list
            // now, to speed up lookups later?
            if (object instanceof Collection) {
                value = ((Collection) object).toArray();
            }
            else if (object instanceof Object[]) {
                value = (Object[]) object;
            }
            else {
                // object is really not a collection... I guess it would make
                // sense to wrap it in a list
                value = new Object[] { object };
            }
        }

        ASTNode evaluateWithObject(ASTStack stack, Object bean) {
            stack.push(value);
            return nextNode;
        }

        void appendString(StringBuffer buffer) {
            buffer.append("(%@)");
        }
    }

    final static class PropertyNode extends ASTNode {
        Expression pathExp;
        String propertyPath;

        PropertyNode(Expression pathExp) {
            this.pathExp = pathExp;
            this.propertyPath = (String) pathExp.getOperand(0);
        }

        ASTNode evaluateWithObject(ASTStack stack, Object bean) {
            // push property value on stack
            try {
                // for DataObjects it should be faster to read property via 
                // dataObject methods instead of reflection
                // for entities the whole meaning is different - we should return 
                // an iterator over attributes/relationships...
                stack.push(
                    (bean instanceof DataObject)
                        ? ((DataObject) bean).readNestedProperty(propertyPath)
                        : (bean instanceof Entity)
                        ? ((Entity) bean).resolvePathComponents(pathExp)
                        : PropertyUtils.getProperty(bean, propertyPath));
            }
            catch (Exception ex) {
                String beanClass = (bean != null) ? bean.getClass().getName() : "<null>";
                String msg =
                    "Error reading property '" + beanClass + "." + propertyPath + "'.";
                logObj.warn(msg, ex);
                throw new ExpressionException(msg, ex);
            }
            return nextNode;
        }

        void appendString(StringBuffer buffer) {
            buffer.append("'").append(propertyPath).append("'");
        }
    }

    final static class EqualsNode extends ASTNode {
        ASTNode evaluateWithObject(ASTStack stack, Object bean) {
            // expects at least two values on the stack
            stack.push(Util.nullSafeEquals(stack.pop(), stack.pop()));
            return nextNode;
        }

        void appendString(StringBuffer buffer) {
            buffer.append(" = ");
        }
    }

    final static class AndNode extends ASTNode {
        ASTNode evaluateWithObject(ASTStack stack, Object bean) {
            // expects two booleans on the stack
            stack.push(stack.popBoolean() && stack.popBoolean());
            return nextNode;
        }

        ASTNode wrapChildNode(ASTNode childNode) {
            return new AndOperandWrapper(childNode, this);
        }

        void appendString(StringBuffer buffer) {
            buffer.append(" and ");
        }
    }

    final static class OrNode extends ASTNode {
        ASTNode evaluateWithObject(ASTStack stack, Object bean) {
            // expects two booleans on the stack
            stack.push(stack.popBoolean() || stack.popBoolean());
            return nextNode;
        }

        ASTNode wrapChildNode(ASTNode childNode) {
            return new OrOperandWrapper(childNode, this);
        }

        void appendString(StringBuffer buffer) {
            buffer.append(" or ");
        }
    }

    final static class NotNode extends ASTNode {
        ASTNode evaluateWithObject(ASTStack stack, Object bean) {
            // expects one boolean on the stack
            stack.push(!stack.popBoolean());
            return nextNode;
        }

        void appendString(StringBuffer buffer) {
            buffer.append(" not ");
        }
    }

    final static class NotEqualsNode extends ASTNode {
        ASTNode evaluateWithObject(ASTStack stack, Object bean) {
            // expects at least two values on the stack
            stack.push(!Util.nullSafeEquals(stack.pop(), stack.pop()));
            return nextNode;
        }

        void appendString(StringBuffer buffer) {
            buffer.append(" != ");
        }
    }

    final static class LessThanNode extends ASTNode {
        ASTNode evaluateWithObject(ASTStack stack, Object bean) {
            // expects at least two values on the stack
            boolean result = false;

            Object c1 = stack.pop();
            Comparable c2 = stack.popComparable();

            // can't compare nulls...be consistent with SQL
            if (c1 != null && c2 != null) {
                // values are popped in reverse order of insertion, 
                // so compare the one popped last...
                result = c2.compareTo(c1) < 0;
            }

            stack.push(result);
            return nextNode;
        }

        void appendString(StringBuffer buffer) {
            buffer.append(" < ");
        }
    }

    final static class LessThanEqualsToNode extends ASTNode {
        ASTNode evaluateWithObject(ASTStack stack, Object bean) {
            // expects at least two values on the stack
            boolean result = false;

            Object c1 = stack.pop();
            Comparable c2 = stack.popComparable();

            // can't compare nulls...be consistent with SQL
            if (c1 != null && c2 != null) {
                // values are popped in reverse order of insertion, 
                // so compare the one popped last...
                result = c2.compareTo(c1) <= 0;
            }

            stack.push(result);
            return nextNode;
        }

        void appendString(StringBuffer buffer) {
            buffer.append(" <= ");
        }
    }

    final static class GreaterThanNode extends ASTNode {
        ASTNode evaluateWithObject(ASTStack stack, Object bean) {
            // expects at least two values on the stack
            boolean result = false;

            Object c1 = stack.pop();
            Comparable c2 = stack.popComparable();

            // can't compare nulls...be consistent with SQL
            if (c1 != null && c2 != null) {
                // values are popped in reverse order of insertion, 
                // so compare the one popped last...
                result = c2.compareTo(c1) > 0;
            }

            stack.push(result);
            return nextNode;
        }

        void appendString(StringBuffer buffer) {
            buffer.append(" > ");
        }
    }

    final static class GreaterThanEqualsToNode extends ASTNode {
        ASTNode evaluateWithObject(ASTStack stack, Object bean) {
            // expects at least two values on the stack
            boolean result = false;

            Object c1 = stack.pop();
            Comparable c2 = stack.popComparable();

            // can't compare nulls...be consistent with SQL
            if (c1 != null && c2 != null) {
                // values are popped in reverse order of insertion, 
                // so compare the one popped last...
                result = c2.compareTo(c1) >= 0;
            }

            stack.push(result);
            return nextNode;
        }

        void appendString(StringBuffer buffer) {
            buffer.append(" >= ");
        }
    }

    final static class BetweenNode extends ASTNode {
        boolean negate;

        BetweenNode(boolean negate) {
            this.negate = negate;
        }

        ASTNode evaluateWithObject(ASTStack stack, Object bean) {
            boolean result = false;

            // pop in reverse order - c3 must be BETWEEN c2 and c1

            Comparable c1 = stack.popComparable();
            Comparable c2 = stack.popComparable();
            Object c3 = stack.pop();

            // can't compare nulls...be consistent with SQL
            if (c1 != null && c2 != null && c3 != null) {
                // values are popped in reverse order of insertion, 
                // so compare the one popped last...
                result = c2.compareTo(c3) <= 0 && c1.compareTo(c3) >= 0;
            }

            stack.push((negate) ? !result : result);
            return nextNode;
        }

        void appendString(StringBuffer buffer) {
            if (negate) {
                buffer.append(" NOT");
            }
            buffer.append(" BETWEEN ");
        }
    }

    final static class InNode extends ASTNode {
        boolean negate;

        InNode(boolean negate) {
            this.negate = negate;
        }

        ASTNode evaluateWithObject(ASTStack stack, Object bean) {

            // pop in reverse order - o2 must be IN o1 list
            Object[] o1 = (Object[]) stack.pop();
            Object o2 = stack.pop();

            boolean result = contains(o1, o2);
            stack.push((negate) ? !result : result);
            return nextNode;
        }

        void appendString(StringBuffer buffer) {
            if (negate) {
                buffer.append(" NOT");
            }
            buffer.append(" IN ");
        }
    }

    final static class LikeNode extends ASTNode {
        String regex;
        boolean negate;

        LikeNode(String pattern, boolean negate, boolean ignoreCase) {
            this.regex = Util.sqlPatternToRegex(pattern, ignoreCase);
            this.negate = negate;
        }

        ASTNode evaluateWithObject(ASTStack stack, Object bean) {
            // LIKE AST uses a single operand, since regex is precompiled
            Object o = stack.pop();
            String string = (o != null) ? o.toString() : null;

            boolean match = regexUtil.match(regex, string);
            stack.push((negate) ? !match : match);
            return nextNode;
        }

        void appendString(StringBuffer buffer) {
            if (negate) {
                buffer.append(" NOT");
            }

            buffer.append(" LIKE ").append(regex);
        }
    }

    //  ***************** wrappers and other helpers

    /**
     * A wrapper for an ASTNode that allows to skip further peer
     * nodes evaluation on a certain outcome. Useful for optimizing 
     * AND, OR operations, etc.
     */
    abstract static class ConditionalJumpNode extends ASTNode {
        ASTNode wrappedNode;
        ASTNode altNode;

        ConditionalJumpNode(ASTNode wrappedNode, ASTNode altNode) {
            this.wrappedNode = wrappedNode;
            this.altNode = altNode;
        }

        ASTNode evaluateWithObject(ASTStack stack, Object bean) {
            ASTNode next = wrappedNode.evaluateWithObject(stack, bean);

            // pick on stack state and decide whether to continue
            // with the normal flow, or jump to alternative node
            return jumpPastAltNode(stack)
                ? ((altNode != null) ? altNode.getNextNode() : null)
                : next;
        }

        ASTNode getNextNode() {
            return (wrappedNode != null) ? wrappedNode.getNextNode() : null;
        }

        void setNextNode(ASTNode nextNode) {
            if (wrappedNode != null) {
                wrappedNode.setNextNode(nextNode);
            }
        }

        void appendString(StringBuffer buffer) {
            buffer.append("(");
            if (wrappedNode != null) {
                wrappedNode.appendString(buffer);
            }
            else {
                buffer.append("?");
            }
            buffer.append(")");
        }

        abstract boolean jumpPastAltNode(ASTStack stack);
    }

    final static class AndOperandWrapper extends ConditionalJumpNode {
        AndOperandWrapper(ASTNode wrappedNode, ASTNode altNode) {
            super(wrappedNode, altNode);
        }

        boolean jumpPastAltNode(ASTStack stack) {
            return !stack.peekBoolean();
        }
    }

    final static class OrOperandWrapper extends ConditionalJumpNode {
        OrOperandWrapper(ASTNode wrappedNode, ASTNode altNode) {
            super(wrappedNode, altNode);
        }

        boolean jumpPastAltNode(ASTStack stack) {
            return stack.peekBoolean();
        }
    }
}
