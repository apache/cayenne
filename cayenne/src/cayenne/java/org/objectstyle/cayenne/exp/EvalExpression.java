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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.util.Util;

/**
 * Class that performs in-memory Cayenne expressions evaluation.
 * 
 * @deprecated since 1.0.6 expressions evaluation has been reimplemented, and this class
 * is no longer supported (with all its current defficiencies). Use API provided by 
 * Expression class to do in-memory evaluation.
 * 
 * @author Andrei Adamchik
 */
public class EvalExpression extends ExpressionTraversal {
    private static Logger logObj = Logger.getLogger(EvalExpression.class);

    protected Expression exp;

    /**
     * Constructor for EvalExpression.
     */
    public EvalExpression(Expression exp) {
        this.exp = exp;
        this.setHandler(new EvalHandler());
    }

    /**
     * Evaluates internally stored expression for an object.
     * 
     * @return <code>true</code> if object matches the expression,
     * <code>false</code> otherwise.
     */
    public boolean evaluate(Object o) {
        reinit(o);
        traverseExpression(exp);

        return ((EvalHandler) getHandler()).getMatch();
    }

    protected void reinit(Object o) {
        ((EvalHandler) getHandler()).reinit(o);
    }

    /** 
     * Stops early if needed.
     */
    protected void traverseExpression(Object expObj, Expression parentExp) {
        super.traverseExpression(expObj, parentExp);
    }

    class EvalHandler extends TraversalHelper {
        protected List stack = new ArrayList(20);
        protected Object obj;

        public boolean getMatch() {
            return popBoolean();
        }

        /** 
         * Resets handler to start processing a new expresson.
         */
        protected void reinit(Object obj) {
            stack.clear();

            // default - evaluate to false
            push(false);

            this.obj = obj;
        }

        /** 
         * Evaluates expression using values from the stack, pushes the result on the stack.
         */
        public void endListNode(Expression node, Expression parentNode) {
            int type = node.getType();
            int size = node.getOperandCount();
            if (size == 0) {
                throw new IllegalArgumentException(
                    "Empty list expression: " + node);
            }

            boolean result = popBoolean();
            if (type == Expression.AND) {
                for (int i = 1; i < size; i++) {
                    result = result && popBoolean();
                }

            } else if (type == Expression.OR) {
                for (int i = 1; i < size; i++) {
                    result = result || popBoolean();
                }
            } else {
                throw new IllegalArgumentException(
                    "Unrecognized list expression: " + node);
            }
            push(result);
        }

        /** 
         * Evaluates expression using values from the stack, pushes
         * the result on the stack.
         */
        public void endBinaryNode(Expression node, Expression parentNode) {
            int type = node.getType();
            if (type == Expression.EQUAL_TO) {
                Object v2 = pop();
                Object v1 = pop();
                push(Util.nullSafeEquals(v1, v2));
            } else {
                push(null);
            }
        }

        /** 
         * Pushes leaf value on the stack. If leaf is an object expression,
         * it is first evaluated, and the result is pushed on the stack.
         */
        public void objectNode(Object leaf, Expression parentNode) {
            // push value on the stack
            if (parentNode.getType() == Expression.OBJ_PATH) {

                try {
                    push(PropertyUtils.getProperty(obj, (String) leaf));
                } catch (Exception ex) {
                    String msg = "Error reading property '" + leaf + "'.";
                    logObj.warn(msg, ex);
                    throw new ExpressionException(msg, ex);
                }
            } else {
                push(leaf);
            }
        }

        /** 
         * Pops a value from the stack.
         */
        public final Object pop() {
            return stack.remove(stack.size() - 1);
        }

        /** 
         * Pops a value from the stack, converting it to boolean.
         */
        public final boolean popBoolean() {
            Object obj = pop();
            return (obj != null) ? ((Boolean) obj).booleanValue() : false;
        }

        /** 
         * Pops a value from the stack, converting it to int.
         */
        public final int popInt() {
            return ((Integer) pop()).intValue();
        }

        /**
         * Pushes a value to the stack.
         */
        public final void push(Object obj) {
            stack.add(obj);
        }

        /**
         * Pushes a boolean value to the stack.
         */
        public final void push(boolean b) {
            stack.add(b ? Boolean.TRUE : Boolean.FALSE);
        }
    }
}
