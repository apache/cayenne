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

    protected void setDelegate(EJBQLExpressionVisitor delegate) {
        this.delegate = delegate;
    }

    protected EJBQLExpressionVisitor getDelegate() {
        return delegate;
    }

    public boolean visitAbs(EJBQLExpression expression) {
        return delegate != null ? delegate.visitAbs(expression) : false;
    }

    public boolean visitAbstractSchemaName(EJBQLExpression expression) {
        return delegate != null ? delegate.visitAbstractSchemaName(expression) : false;
    }

    public boolean visitAdd(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitAdd(expression, finishedChildIndex)
                : false;
    }

    public boolean visitAggregate(EJBQLExpression expression) {
        return delegate != null ? delegate.visitAggregate(expression) : false;
    }

    public boolean visitAll(EJBQLExpression expression) {
        return delegate != null ? delegate.visitAll(expression) : false;
    }

    public boolean visitAnd(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitAnd(expression, finishedChildIndex)
                : false;
    }

    public boolean visitAny(EJBQLExpression expression) {
        return delegate != null ? delegate.visitAny(expression) : false;
    }

    public boolean visitAscending(EJBQLExpression expression) {
        return delegate != null ? delegate.visitAscending(expression) : false;
    }

    public boolean visitAverage(EJBQLExpression expression) {
        return delegate != null ? delegate.visitAverage(expression) : false;
    }

    public boolean visitBetween(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitBetween(expression, finishedChildIndex)
                : false;
    }

    public boolean visitBooleanLiteral(EJBQLExpression expression) {
        return delegate != null ? delegate.visitBooleanLiteral(expression) : false;
    }

    public boolean visitClassName(EJBQLExpression expression) {
        return delegate != null ? delegate.visitClassName(expression) : false;
    }

    public boolean visitConcat(EJBQLExpression expression) {
        return delegate != null ? delegate.visitConcat(expression) : false;
    }

    public boolean visitConstructor(EJBQLExpression expression) {
        return delegate != null ? delegate.visitConstructor(expression) : false;
    }

    public boolean visitConstructorParameter(EJBQLExpression expression) {
        return delegate != null ? delegate.visitConstructorParameter(expression) : false;
    }

    public boolean visitConstructorParameters(EJBQLExpression expression) {
        return delegate != null ? delegate.visitConstructorParameters(expression) : false;
    }

    public boolean visitCount(EJBQLExpression expression) {
        return delegate != null ? delegate.visitCount(expression) : false;
    }

    public boolean visitCurrentDate(EJBQLExpression expression) {
        return delegate != null ? delegate.visitCurrentDate(expression) : false;
    }

    public boolean visitCurrentTime(EJBQLExpression expression) {
        return delegate != null ? delegate.visitCurrentTime(expression) : false;
    }

    public boolean visitCurrentTimestamp(EJBQLExpression expression) {
        return delegate != null ? delegate.visitCurrentTimestamp(expression) : false;
    }

    public boolean visitDecimalLiteral(EJBQLExpression expression) {
        return delegate != null ? delegate.visitDecimalLiteral(expression) : false;
    }

    public boolean visitDelete(EJBQLExpression expression) {
        return delegate != null ? delegate.visitDelete(expression) : false;
    }

    public boolean visitDescending(EJBQLExpression expression) {
        return delegate != null ? delegate.visitDescending(expression) : false;
    }

    public boolean visitDistinct(EJBQLExpression expression) {
        return delegate != null ? delegate.visitDistinct(expression) : false;
    }

    public boolean visitDistinctPath(EJBQLExpression expression) {
        return delegate != null ? delegate.visitDistinctPath(expression) : false;
    }

    public boolean visitDivide(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitDivide(expression, finishedChildIndex)
                : false;
    }

    public boolean visitEquals(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitEquals(expression, finishedChildIndex)
                : false;
    }

    public boolean visitEscapeCharacter(EJBQLExpression expression) {
        return delegate != null ? delegate.visitEscapeCharacter(expression) : false;
    }

    public boolean visitExists(EJBQLExpression expression) {
        return delegate != null ? delegate.visitExists(expression) : false;
    }

    public boolean visitFrom(EJBQLExpression expression) {
        return delegate != null ? delegate.visitFrom(expression) : false;
    }

    public boolean visitFromItem(EJBQLExpression expression) {
        return delegate != null ? delegate.visitFromItem(expression) : false;
    }

    public boolean visitGreaterOrEqual(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null ? delegate.visitGreaterOrEqual(
                expression,
                finishedChildIndex) : false;
    }

    public boolean visitGreaterThan(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null ? delegate.visitGreaterThan(
                expression,
                finishedChildIndex) : false;
    }

    public boolean visitGroupBy(EJBQLExpression expression) {
        return delegate != null ? delegate.visitGroupBy(expression) : false;
    }

    public boolean visitHaving(EJBQLExpression expression) {
        return delegate != null ? delegate.visitHaving(expression) : false;
    }

    public boolean visitIdentificationVariable(EJBQLExpression expression) {
        return delegate != null
                ? delegate.visitIdentificationVariable(expression)
                : false;
    }

    public boolean visitIdentifier(EJBQLExpression expression) {
        return delegate != null ? delegate.visitIdentifier(expression) : false;
    }

    public boolean visitIn(EJBQLExpression expression) {
        return delegate != null ? delegate.visitIn(expression) : false;
    }

    public boolean visitInnerFetchJoin(EJBQLExpression expression) {
        return delegate != null ? delegate.visitInnerFetchJoin(expression) : false;
    }

    public boolean visitInnerJoin(EJBQLExpression expression) {
        return delegate != null ? delegate.visitInnerJoin(expression) : false;
    }

    public boolean visitIntegerLiteral(EJBQLExpression expression) {
        return delegate != null ? delegate.visitIntegerLiteral(expression) : false;
    }

    public boolean visitIsEmpty(EJBQLExpression expression) {
        return delegate != null ? delegate.visitIsEmpty(expression) : false;
    }

    public boolean visitIsNull(EJBQLExpression expression) {
        return delegate != null ? delegate.visitIsNull(expression) : false;
    }

    public boolean visitLength(EJBQLExpression expression) {
        return delegate != null ? delegate.visitLength(expression) : false;
    }

    public boolean visitLessOrEqual(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null ? delegate.visitLessOrEqual(
                expression,
                finishedChildIndex) : false;
    }

    public boolean visitLessThan(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitLessThan(expression, finishedChildIndex)
                : false;
    }

    public boolean visitLike(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitLike(expression, finishedChildIndex)
                : false;
    }

    public boolean visitLocate(EJBQLExpression expression) {
        return delegate != null ? delegate.visitLocate(expression) : false;
    }

    public boolean visitLower(EJBQLExpression expression) {
        return delegate != null ? delegate.visitLower(expression) : false;
    }

    public boolean visitMax(EJBQLExpression expression) {
        return delegate != null ? delegate.visitMax(expression) : false;
    }

    public boolean visitMemberOf(EJBQLExpression expression) {
        return delegate != null ? delegate.visitMemberOf(expression) : false;
    }

    public boolean visitMin(EJBQLExpression expression) {
        return delegate != null ? delegate.visitMin(expression) : false;
    }

    public boolean visitMod(EJBQLExpression expression) {
        return delegate != null ? delegate.visitMod(expression) : false;
    }

    public boolean visitMultiply(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitMultiply(expression, finishedChildIndex)
                : false;
    }

    public boolean visitNamedInputParameter(EJBQLExpression expression) {
        return delegate != null ? delegate.visitNamedInputParameter(expression) : false;
    }

    public boolean visitNegative(EJBQLExpression expression) {
        return delegate != null ? delegate.visitNegative(expression) : false;
    }

    public boolean visitNot(EJBQLExpression expression) {
        return delegate != null ? delegate.visitNot(expression) : false;
    }

    public boolean visitNotEquals(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitNotEquals(expression, finishedChildIndex)
                : false;
    }

    public boolean visitOr(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitOr(expression, finishedChildIndex)
                : false;
    }

    public boolean visitOrderBy(EJBQLExpression expression) {
        return delegate != null ? delegate.visitOrderBy(expression) : false;
    }

    public boolean visitOrderByItem(EJBQLExpression expression) {
        return delegate != null ? delegate.visitOrderByItem(expression) : false;
    }

    public boolean visitOuterFetchJoin(EJBQLExpression expression) {
        return delegate != null ? delegate.visitOuterFetchJoin(expression) : false;
    }

    public boolean visitOuterJoin(EJBQLExpression expression) {
        return delegate != null ? delegate.visitOuterJoin(expression) : false;
    }

    public boolean visitPath(EJBQLExpression expression) {
        return delegate != null ? delegate.visitPath(expression) : false;
    }

    public boolean visitPatternValue(EJBQLExpression expression) {
        return delegate != null ? delegate.visitPatternValue(expression) : false;
    }

    public boolean visitPositionalInputParameter(EJBQLExpression expression) {
        return delegate != null
                ? delegate.visitPositionalInputParameter(expression)
                : false;
    }

    public boolean visitSelect(EJBQLExpression expression) {
        return delegate != null ? delegate.visitSelect(expression) : false;
    }

    public boolean visitSelectExpression(EJBQLExpression expression) {
        return delegate != null ? delegate.visitSelectExpression(expression) : false;
    }

    public boolean visitSize(EJBQLExpression expression) {
        return delegate != null ? delegate.visitSize(expression) : false;
    }

    public boolean visitSqrt(EJBQLExpression expression) {
        return delegate != null ? delegate.visitSqrt(expression) : false;
    }

    public boolean visitStringLiteral(EJBQLExpression expression) {
        return delegate != null ? delegate.visitStringLiteral(expression) : false;
    }

    public boolean visitSubselect(EJBQLExpression expression) {
        return delegate != null ? delegate.visitSubselect(expression) : false;
    }

    public boolean visitSubstring(EJBQLExpression expression) {
        return delegate != null ? delegate.visitSubstring(expression) : false;
    }

    public boolean visitSubtract(EJBQLExpression expression, int finishedChildIndex) {
        return delegate != null
                ? delegate.visitSubtract(expression, finishedChildIndex)
                : false;
    }

    public boolean visitSum(EJBQLExpression expression) {
        return delegate != null ? delegate.visitSum(expression) : false;
    }

    public boolean visitTok(EJBQLExpression expression) {
        return delegate != null ? delegate.visitTok(expression) : false;
    }

    public boolean visitTrim(EJBQLExpression expression) {
        return delegate != null ? delegate.visitTrim(expression) : false;
    }

    public boolean visitTrimBoth(EJBQLExpression expression) {
        return delegate != null ? delegate.visitTrimBoth(expression) : false;
    }

    public boolean visitTrimCharacter(EJBQLExpression expression) {
        return delegate != null ? delegate.visitTrimCharacter(expression) : false;
    }

    public boolean visitTrimLeading(EJBQLExpression expression) {
        return delegate != null ? delegate.visitTrimLeading(expression) : false;
    }

    public boolean visitTrimTrailing(EJBQLExpression expression) {
        return delegate != null ? delegate.visitTrimTrailing(expression) : false;
    }

    public boolean visitUpdate(EJBQLExpression expression) {
        return delegate != null ? delegate.visitUpdate(expression) : false;
    }

    public boolean visitUpdateField(EJBQLExpression expression) {
        return delegate != null ? delegate.visitUpdateField(expression) : false;
    }

    public boolean visitUpdateItem(EJBQLExpression expression) {
        return delegate != null ? delegate.visitUpdateItem(expression) : false;
    }

    public boolean visitUpdateValue(EJBQLExpression expression) {
        return delegate != null ? delegate.visitUpdateValue(expression) : false;
    }

    public boolean visitUpper(EJBQLExpression expression) {
        return delegate != null ? delegate.visitUpper(expression) : false;
    }

    public boolean visitWhere(EJBQLExpression expression) {
        return delegate != null ? delegate.visitWhere(expression) : false;
    }
}
