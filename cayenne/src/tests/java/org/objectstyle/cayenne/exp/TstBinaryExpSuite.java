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

/**
 * A suite of binary expression tests.
 * 
 * @author Andrei Adamchik
 */
public class TstBinaryExpSuite extends TstExpressionSuite {

    private static final TstExpressionCase like1 = buildLike1();
    private static final TstExpressionCase likeic1 = buildLikeIgnoreCase1();
    private static final TstExpressionCase in1 = buildIn1();
    private static final TstExpressionCase in2 = buildIn2();
    private static final TstExpressionCase isNull = buildIsNull();
    private static final TstExpressionCase isNotNull = buildIsNotNull();

    /** Cayenne syntax: "toGallery.galleryName in ('g1', 'g2', g3')" */
    private static TstExpressionCase buildIn1() {
        List in = new ArrayList();
        in.add("g1");
        in.add("g2");
        in.add("g3");

        Expression e1 = ExpressionFactory.expressionOfType(Expression.IN);

        Expression e10 =
            ExpressionFactory.expressionOfType(Expression.OBJ_PATH);
        e10.setOperand(0, "toGallery.galleryName");
        e1.setOperand(0, e10);

        Expression e11 = ExpressionFactory.expressionOfType(Expression.LIST);
        e11.setOperand(0, in);
        e1.setOperand(1, e11);

        return new TstExpressionCase(
            "Exhibit",
            e1,
            "ta.GALLERY_NAME IN (?, ?, ?)",
            3,
            2);
    }

    private static TstExpressionCase buildIsNull() {
        Expression e1 = ExpressionFactory.expressionOfType(Expression.EQUAL_TO);

        Expression e10 =
            ExpressionFactory.expressionOfType(Expression.OBJ_PATH);
        e10.setOperand(0, "toGallery.galleryName");
        e1.setOperand(0, e10);
        e1.setOperand(1, null);

        return new TstExpressionCase(
            "Exhibit",
            e1,
            "ta.GALLERY_NAME IS NULL",
            2,
            2);
    }

    private static TstExpressionCase buildIsNotNull() {
        Expression e1 =
            ExpressionFactory.expressionOfType(Expression.NOT_EQUAL_TO);

        Expression e10 =
            ExpressionFactory.expressionOfType(Expression.OBJ_PATH);
        e10.setOperand(0, "toGallery.galleryName");
        e1.setOperand(0, e10);
        e1.setOperand(1, null);

        return new TstExpressionCase(
            "Exhibit",
            e1,
            "ta.GALLERY_NAME IS NOT NULL",
            2,
            2);
    }

    /** Cayenne syntax: "toGallery.galleryName in ('g1', 'g2', g3')" */
    private static TstExpressionCase buildIn2() {
        // test Object[]
        Object[] in = new Object[] { "g1", "g2", "g3" };

        Expression e1 = ExpressionFactory.expressionOfType(Expression.IN);

        Expression e10 =
            ExpressionFactory.expressionOfType(Expression.OBJ_PATH);
        e10.setOperand(0, "toGallery.galleryName");
        e1.setOperand(0, e10);

        Expression e11 = ExpressionFactory.expressionOfType(Expression.LIST);
        e11.setOperand(0, in);
        e1.setOperand(1, e11);

        return new TstExpressionCase(
            "Exhibit",
            e1,
            "ta.GALLERY_NAME IN (?, ?, ?)",
            3,
            2);
    }

    /** Cayenne syntax: "toGallery.galleryName like 'a%'" */
    private static TstExpressionCase buildLike1() {
        Expression e1 = ExpressionFactory.expressionOfType(Expression.LIKE);
        Expression e10 =
            ExpressionFactory.expressionOfType(Expression.OBJ_PATH);
        e10.setOperand(0, "toGallery.galleryName");
        e1.setOperand(0, e10);
        e1.setOperand(1, "a%");
        return new TstExpressionCase(
            "Exhibit",
            e1,
            "ta.GALLERY_NAME LIKE ?",
            2,
            2);
    }

    /** Cayenne syntax: "toGallery.galleryName likeIgnoreCase 'a%'" */
    private static TstExpressionCase buildLikeIgnoreCase1() {
        Expression e1 =
            ExpressionFactory.expressionOfType(Expression.LIKE_IGNORE_CASE);
        Expression e10 =
            ExpressionFactory.expressionOfType(Expression.OBJ_PATH);
        e10.setOperand(0, "toGallery.galleryName");
        e1.setOperand(0, e10);
        e1.setOperand(1, "a%");
        return new TstExpressionCase(
            "Exhibit",
            e1,
            "UPPER(ta.GALLERY_NAME) LIKE UPPER(?)",
            2,
            2);
    }

    public TstBinaryExpSuite() {
        addCase(like1);
        addCase(likeic1);
        addCase(in1);
        addCase(in2);
        addCase(isNull);
        addCase(isNotNull);
    }
}
