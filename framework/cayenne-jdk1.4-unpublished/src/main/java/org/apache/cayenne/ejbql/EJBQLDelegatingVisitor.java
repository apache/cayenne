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
package org.apache.cayenne.ejbql;

import org.apache.cayenne.ejbql.parser.EJBQLFromItem;
import org.apache.cayenne.ejbql.parser.EJBQLJoin;
import org.apache.cayenne.ejbql.parser.EJBQLPath;

/**
 * A base implementation of the EJBQLExpressionVisitor that implements all methods to
 * delegate processing to another visitor. This is a convenience superclass for visitors
 * that delegate processing of subtrees to child visitors.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class EJBQLDelegatingVisitor implements EJBQLExpressionVisitor {

    protected EJBQLExpressionVisitor delegate;

    protected boolean continueFlag;

    public EJBQLDelegatingVisitor() {
        this(false);
    }

    public EJBQLDelegatingVisitor(boolean continueFlag) {
        this.continueFlag = continueFlag;
    }

    protected void setDelegate(EJBQLExpressionVisitor delegate) {
        this.delegate = delegate;
    }

    protected EJBQLExpressionVisitor getDelegate() {
        return delegate;
    }

    public boolean visitAbs(EJBQLExpression expression) {
        return delegate != null ? delegate.visitAbs(expression) : continueFlag;
    }

    public boolean visitAbstractSchemaName(EJBQLExpression expression) {
        return delegate != null
                ? delegate.visitAbstractSchemaName(expression)
                : continueFlag;
    }

    public boolean visitAdd(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitAdd(expression, finishedChildIndex)
                : continueFlag;
    }

    public boolean visitAggregate(EJBQLExpression expression) {
        return delegate != null ? delegate.visitAggregate(expression) : continueFlag;
    }

    public boolean visitAll(EJBQLExpression expression) {
        return delegate != null ? delegate.visitAll(expression) : continueFlag;
    }

    public boolean visitAnd(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitAnd(expression, finishedChildIndex)
                : continueFlag;
    }

    public boolean visitAny(EJBQLExpression expression) {
        return delegate != null ? delegate.visitAny(expression) : continueFlag;
    }

    public boolean visitAscending(EJBQLExpression expression) {
        return delegate != null ? delegate.visitAscending(expression) : continueFlag;
    }

    public boolean visitAverage(EJBQLExpression expression) {
        return delegate != null ? delegate.visitAverage(expression) : continueFlag;
    }

    public boolean visitBetween(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitBetween(expression, finishedChildIndex)
                : continueFlag;
    }

    public boolean visitBooleanLiteral(EJBQLExpression expression) {
        return delegate != null ? delegate.visitBooleanLiteral(expression) : continueFlag;
    }

    public boolean visitClassName(EJBQLExpression expression) {
        return delegate != null ? delegate.visitClassName(expression) : continueFlag;
    }

    public boolean visitConcat(EJBQLExpression expression) {
        return delegate != null ? delegate.visitConcat(expression) : continueFlag;
    }

    public boolean visitConstructor(EJBQLExpression expression) {
        return delegate != null ? delegate.visitConstructor(expression) : continueFlag;
    }

    public boolean visitConstructorParameter(EJBQLExpression expression) {
        return delegate != null
                ? delegate.visitConstructorParameter(expression)
                : continueFlag;
    }

    public boolean visitConstructorParameters(EJBQLExpression expression) {
        return delegate != null
                ? delegate.visitConstructorParameters(expression)
                : continueFlag;
    }

    public boolean visitCount(EJBQLExpression expression) {
        return delegate != null ? delegate.visitCount(expression) : continueFlag;
    }

    public boolean visitCurrentDate(EJBQLExpression expression) {
        return delegate != null ? delegate.visitCurrentDate(expression) : continueFlag;
    }

    public boolean visitCurrentTime(EJBQLExpression expression) {
        return delegate != null ? delegate.visitCurrentTime(expression) : continueFlag;
    }

    public boolean visitCurrentTimestamp(EJBQLExpression expression) {
        return delegate != null
                ? delegate.visitCurrentTimestamp(expression)
                : continueFlag;
    }

    public boolean visitDecimalLiteral(EJBQLExpression expression) {
        return delegate != null ? delegate.visitDecimalLiteral(expression) : continueFlag;
    }

    public boolean visitDelete(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitDelete(expression, finishedChildIndex)
                : continueFlag;
    }

    public boolean visitDescending(EJBQLExpression expression) {
        return delegate != null ? delegate.visitDescending(expression) : continueFlag;
    }

    public boolean visitDistinct(EJBQLExpression expression) {
        return delegate != null ? delegate.visitDistinct(expression) : continueFlag;
    }

    public boolean visitDistinctPath(EJBQLExpression expression) {
        return delegate != null ? delegate.visitDistinctPath(expression) : continueFlag;
    }

    public boolean visitDivide(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitDivide(expression, finishedChildIndex)
                : continueFlag;
    }

    public boolean visitEquals(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitEquals(expression, finishedChildIndex)
                : continueFlag;
    }

    public boolean visitEscapeCharacter(EJBQLExpression expression) {
        return delegate != null
                ? delegate.visitEscapeCharacter(expression)
                : continueFlag;
    }

    public boolean visitExists(EJBQLExpression expression) {
        return delegate != null ? delegate.visitExists(expression) : continueFlag;
    }

    public boolean visitFrom(EJBQLExpression expression) {
        return delegate != null ? delegate.visitFrom(expression) : continueFlag;
    }

    public boolean visitFromItem(EJBQLFromItem expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitFromItem(expression, finishedChildIndex)
                : continueFlag;
    }

    public boolean visitGreaterOrEqual(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null ? delegate.visitGreaterOrEqual(
                expression,
                finishedChildIndex) : continueFlag;
    }

    public boolean visitGreaterThan(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null ? delegate.visitGreaterThan(
                expression,
                finishedChildIndex) : continueFlag;
    }

    public boolean visitGroupBy(EJBQLExpression expression) {
        return delegate != null ? delegate.visitGroupBy(expression) : continueFlag;
    }

    public boolean visitHaving(EJBQLExpression expression) {
        return delegate != null ? delegate.visitHaving(expression) : continueFlag;
    }

    public boolean visitIdentificationVariable(EJBQLExpression expression) {
        return delegate != null
                ? delegate.visitIdentificationVariable(expression)
                : continueFlag;
    }

    public boolean visitIdentifier(EJBQLExpression expression) {
        return delegate != null ? delegate.visitIdentifier(expression) : continueFlag;
    }

    public boolean visitIn(EJBQLExpression expression) {
        return delegate != null ? delegate.visitIn(expression) : continueFlag;
    }

    public boolean visitInnerFetchJoin(EJBQLJoin join, int finishedChildIndex) {
        return delegate != null ? delegate.visitInnerFetchJoin(
                join,
                finishedChildIndex) : continueFlag;
    }

    public boolean visitInnerJoin(EJBQLJoin join, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitInnerJoin(join, finishedChildIndex)
                : continueFlag;
    }

    public boolean visitIntegerLiteral(EJBQLExpression expression) {
        return delegate != null ? delegate.visitIntegerLiteral(expression) : continueFlag;
    }

    public boolean visitIsEmpty(EJBQLExpression expression) {
        return delegate != null ? delegate.visitIsEmpty(expression) : continueFlag;
    }

    public boolean visitIsNull(EJBQLExpression expression) {
        return delegate != null ? delegate.visitIsNull(expression) : continueFlag;
    }

    public boolean visitLength(EJBQLExpression expression) {
        return delegate != null ? delegate.visitLength(expression) : continueFlag;
    }

    public boolean visitLessOrEqual(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null ? delegate.visitLessOrEqual(
                expression,
                finishedChildIndex) : continueFlag;
    }

    public boolean visitLessThan(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitLessThan(expression, finishedChildIndex)
                : continueFlag;
    }

    public boolean visitLike(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitLike(expression, finishedChildIndex)
                : continueFlag;
    }

    public boolean visitLocate(EJBQLExpression expression) {
        return delegate != null ? delegate.visitLocate(expression) : continueFlag;
    }

    public boolean visitLower(EJBQLExpression expression) {
        return delegate != null ? delegate.visitLower(expression) : continueFlag;
    }

    public boolean visitMax(EJBQLExpression expression) {
        return delegate != null ? delegate.visitMax(expression) : continueFlag;
    }

    public boolean visitMemberOf(EJBQLExpression expression) {
        return delegate != null ? delegate.visitMemberOf(expression) : continueFlag;
    }

    public boolean visitMin(EJBQLExpression expression) {
        return delegate != null ? delegate.visitMin(expression) : continueFlag;
    }

    public boolean visitMod(EJBQLExpression expression) {
        return delegate != null ? delegate.visitMod(expression) : continueFlag;
    }

    public boolean visitMultiply(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitMultiply(expression, finishedChildIndex)
                : continueFlag;
    }

    public boolean visitNamedInputParameter(EJBQLExpression expression) {
        return delegate != null
                ? delegate.visitNamedInputParameter(expression)
                : continueFlag;
    }

    public boolean visitNegative(EJBQLExpression expression) {
        return delegate != null ? delegate.visitNegative(expression) : continueFlag;
    }

    public boolean visitNot(EJBQLExpression expression) {
        return delegate != null ? delegate.visitNot(expression) : continueFlag;
    }

    public boolean visitNotEquals(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitNotEquals(expression, finishedChildIndex)
                : continueFlag;
    }

    public boolean visitOr(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitOr(expression, finishedChildIndex)
                : continueFlag;
    }

    public boolean visitOrderBy(EJBQLExpression expression) {
        return delegate != null ? delegate.visitOrderBy(expression) : continueFlag;
    }

    public boolean visitOrderByItem(EJBQLExpression expression) {
        return delegate != null ? delegate.visitOrderByItem(expression) : continueFlag;
    }

    public boolean visitOuterFetchJoin(EJBQLJoin join, int finishedChildIndex) {
        return delegate != null ? delegate.visitOuterFetchJoin(
                join,
                finishedChildIndex) : continueFlag;
    }

    public boolean visitOuterJoin(EJBQLJoin join, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitOuterJoin(join, finishedChildIndex)
                : continueFlag;
    }

    public boolean visitPath(EJBQLPath expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitPath(expression, finishedChildIndex)
                : continueFlag;
    }

    public boolean visitPatternValue(EJBQLExpression expression) {
        return delegate != null ? delegate.visitPatternValue(expression) : continueFlag;
    }

    public boolean visitPositionalInputParameter(EJBQLExpression expression) {
        return delegate != null
                ? delegate.visitPositionalInputParameter(expression)
                : continueFlag;
    }

    public boolean visitSelect(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitSelect(expression, finishedChildIndex)
                : continueFlag;
    }

    public boolean visitSelectExpression(EJBQLExpression expression) {
        return delegate != null
                ? delegate.visitSelectExpression(expression)
                : continueFlag;
    }

    public boolean visitSize(EJBQLExpression expression) {
        return delegate != null ? delegate.visitSize(expression) : continueFlag;
    }

    public boolean visitSqrt(EJBQLExpression expression) {
        return delegate != null ? delegate.visitSqrt(expression) : continueFlag;
    }

    public boolean visitStringLiteral(EJBQLExpression expression) {
        return delegate != null ? delegate.visitStringLiteral(expression) : continueFlag;
    }

    public boolean visitSubselect(EJBQLExpression expression) {
        return delegate != null ? delegate.visitSubselect(expression) : continueFlag;
    }

    public boolean visitSubstring(EJBQLExpression expression) {
        return delegate != null ? delegate.visitSubstring(expression) : continueFlag;
    }

    public boolean visitSubtract(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitSubtract(expression, finishedChildIndex)
                : continueFlag;
    }

    public boolean visitSum(EJBQLExpression expression) {
        return delegate != null ? delegate.visitSum(expression) : continueFlag;
    }

    public boolean visitTok(EJBQLExpression expression) {
        return delegate != null ? delegate.visitTok(expression) : continueFlag;
    }

    public boolean visitTrim(EJBQLExpression expression) {
        return delegate != null ? delegate.visitTrim(expression) : continueFlag;
    }

    public boolean visitTrimBoth(EJBQLExpression expression) {
        return delegate != null ? delegate.visitTrimBoth(expression) : continueFlag;
    }

    public boolean visitTrimCharacter(EJBQLExpression expression) {
        return delegate != null ? delegate.visitTrimCharacter(expression) : continueFlag;
    }

    public boolean visitTrimLeading(EJBQLExpression expression) {
        return delegate != null ? delegate.visitTrimLeading(expression) : continueFlag;
    }

    public boolean visitTrimTrailing(EJBQLExpression expression) {
        return delegate != null ? delegate.visitTrimTrailing(expression) : continueFlag;
    }

    public boolean visitUpdate(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitUpdate(expression, finishedChildIndex)
                : continueFlag;
    }

    public boolean visitUpdateField(EJBQLExpression expression) {
        return delegate != null ? delegate.visitUpdateField(expression) : continueFlag;
    }

    public boolean visitUpdateItem(EJBQLExpression expression) {
        return delegate != null ? delegate.visitUpdateItem(expression) : continueFlag;
    }

    public boolean visitUpdateValue(EJBQLExpression expression) {
        return delegate != null ? delegate.visitUpdateValue(expression) : continueFlag;
    }

    public boolean visitUpper(EJBQLExpression expression) {
        return delegate != null ? delegate.visitUpper(expression) : continueFlag;
    }

    public boolean visitWhere(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitWhere(expression, finishedChildIndex)
                : continueFlag;
    }
}
