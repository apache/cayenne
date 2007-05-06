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
package org.objectstyle.cayenne.exp;

public class TstUnaryExpSuite extends TstExpressionSuite {
    
    private static final TstExpressionCase negative1 = buildNegative1();
    private static final TstExpressionCase negative2 = buildNegative2();
    private static final TstExpressionCase negative3 = buildNegative3();
    
    /** Cayenne syntax: "-5" */
    private static TstExpressionCase buildNegative1() {           
        Expression e1 = ExpressionFactory.expressionOfType(Expression.NEGATIVE);
        e1.setOperand(0, new Integer(5));
        return new TstExpressionCase("Painting",
        e1, 
        "-?",
        1, 1
        );
    }
    
    /** Cayenne syntax: "-estimatedPrice" */
    private static TstExpressionCase buildNegative2() {           
        Expression e1 = ExpressionFactory.expressionOfType(Expression.NEGATIVE);
        Expression e10 = ExpressionFactory.expressionOfType(Expression.OBJ_PATH);
        e10.setOperand(0, "estimatedPrice");
        e1.setOperand(0, e10);
        return new TstExpressionCase("Painting",
        e1, 
        "-ta.ESTIMATED_PRICE",
        2, 1
        );
    }
    
    /** Cayenne syntax: "-toGallery.paintingArray.estimatedPrice" */
    private static TstExpressionCase buildNegative3() {           
        Expression e1 = ExpressionFactory.expressionOfType(Expression.NEGATIVE);
        Expression e10 = ExpressionFactory.expressionOfType(Expression.OBJ_PATH);
        e10.setOperand(0, "toGallery.paintingArray.estimatedPrice");
        e1.setOperand(0, e10);
        return new TstExpressionCase("Exhibit",
        e1, 
        "-ta.ESTIMATED_PRICE",
        2, 1
        );
    }
    
    
    public TstUnaryExpSuite() {
        addCase(negative1);
        addCase(negative2);
        addCase(negative3);
    }
}
