/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.exp.parser;

import java.util.Collection;
import java.util.Iterator;

import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.util.ConversionUtil;

/**
 * "Or" expression.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class ASTOr extends AggregateConditionNode {
    ASTOr(int id) {
        super(id);
    }

    public ASTOr() {
        super(ExpressionParserTreeConstants.JJTOR);
    }

    public ASTOr(Object[] nodes) {
        super(ExpressionParserTreeConstants.JJTOR);
        int len = nodes.length;
        for (int i = 0; i < len; i++) {
            jjtAddChild((Node) nodes[i], i);
        }
    }

    public ASTOr(Collection nodes) {
        super(ExpressionParserTreeConstants.JJTOR);
        int len = nodes.size();
        Iterator it = nodes.iterator();
        for (int i = 0; i < len; i++) {
            jjtAddChild((Node) it.next(), i);
        }
    }

    protected Object evaluateNode(Object o) throws Exception {
        int len = jjtGetNumChildren();
        if (len == 0) {
            return Boolean.FALSE;
        }

        for (int i = 0; i < len; i++) {
            if (ConversionUtil.toBoolean(evaluateChild(i, o))) {
                return Boolean.TRUE;
            }
        }

        return Boolean.FALSE;
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    public Expression shallowCopy() {
        return new ASTOr(id);
    }

    protected String getExpressionOperator(int index) {
        return "or";
    }

    public int getType() {
        return Expression.OR;
    }

    public void jjtClose() {
        super.jjtClose();
        flattenTree();
    }
}
