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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.Transformer;

/**
 * An expression with a varying number of operands. Usually this is
 * used for the list expressions when the list size may vary.
 * 
 * @author Andrei Adamchik
 */
public class ListExpression extends Expression {
    protected List operands = new ArrayList();

    public ListExpression() {
    }

    public ListExpression(int type) {
        this.type = type;
    }
    
    protected void flattenTree() {
        
    }

    protected boolean pruneNodeForPrunedChild(Object prunedChild) {
        return false;
    }

    public Expression transform(Transformer transformer) {
        Expression copy = super.transform(transformer);

        if (!(copy instanceof ListExpression)) {
            return copy;
        }

        // prune itself if the transformation resulted in 
        // no children or a single child
        switch (copy.getOperandCount()) {
            case 1 :
                return (Expression) copy.getOperand(0);
            case 0 :
                return null;
            default :
                return copy;
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
     * @see org.objectstyle.cayenne.exp.Expression#getOperand(int)
     */
    public Object getOperand(int index) {
        if (operands.size() <= index) {
            throw new IllegalArgumentException(
                "Attempt to retrieve operand "
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
            throw new IllegalArgumentException(
                "Attempt to set operand "
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
     * In case requested expression type is the same as internal type,
     * creates and returns a copy of this expression with the internal list of
     * operands expanded with the new expression. If the type of expression is
     * different from this, calls superclass's implementation.
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
        // this class will be deprecated soon, so using
        // a deprecated "toStringBuffer" is OK
        StringBuffer buffer = new StringBuffer();
        toStringBuffer(buffer);
        pw.print(buffer.toString());
    }
}
