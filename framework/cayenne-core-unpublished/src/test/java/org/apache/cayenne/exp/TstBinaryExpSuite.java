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

import java.util.ArrayList;
import java.util.List;

/**
 * A suite of binary expression tests.
 * 
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
