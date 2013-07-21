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

import org.apache.cayenne.ejbql.parser.EJBQLAggregateColumn;
import org.apache.cayenne.ejbql.parser.EJBQLDecimalLiteral;
import org.apache.cayenne.ejbql.parser.EJBQLFromItem;
import org.apache.cayenne.ejbql.parser.EJBQLIntegerLiteral;
import org.apache.cayenne.ejbql.parser.EJBQLJoin;
import org.apache.cayenne.ejbql.parser.EJBQLPositionalInputParameter;

/**
 * A noop implementation of the EJBQL visitor that returns same preset boolean value from
 * all methods. Intended for subclassing.
 * 
 * @since 3.0
 */
public class EJBQLBaseVisitor implements EJBQLExpressionVisitor {

    protected boolean continueFlag;

    public EJBQLBaseVisitor() {
        this(true);
    }

    public EJBQLBaseVisitor(boolean continueFlag) {
        this.continueFlag = continueFlag;
    }

    public boolean visitAbs(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitAbstractSchemaName(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitAdd(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitAggregate(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitAll(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitAnd(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitAny(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitAscending(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitAverage(EJBQLAggregateColumn expression) {
        return continueFlag;
    }

    public boolean visitBetween(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitBooleanLiteral(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitClassName(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitConcat(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitConstructor(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitConstructorParameter(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitConstructorParameters(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitCount(EJBQLAggregateColumn expression) {
        return continueFlag;
    }

    public boolean visitCurrentDate(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitCurrentTime(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitCurrentTimestamp(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitDecimalLiteral(EJBQLDecimalLiteral expression) {
        return continueFlag;
    }

    public boolean visitDelete(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitDescending(EJBQLExpression expression) {
        return continueFlag;
    }
    
    public boolean visitDbPath(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitDistinct(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitDivide(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitEquals(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitEscapeCharacter(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitExists(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitFrom(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitFromItem(EJBQLFromItem expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitGreaterOrEqual(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitGreaterThan(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitGroupBy(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitHaving(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitIdentificationVariable(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitIdentifier(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitIn(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitInnerFetchJoin(EJBQLJoin join) {
        return continueFlag;
    }

    public boolean visitInnerJoin(EJBQLJoin join) {
        return continueFlag;
    }

    public boolean visitIntegerLiteral(EJBQLIntegerLiteral expression) {
        return continueFlag;
    }

    public boolean visitIsEmpty(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitIsNull(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitLength(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitLessOrEqual(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitLessThan(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitLike(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitLocate(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitLower(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitMax(EJBQLAggregateColumn expression) {
        return continueFlag;
    }

    public boolean visitMemberOf(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitMin(EJBQLAggregateColumn expression) {
        return continueFlag;
    }

    public boolean visitMod(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitMultiply(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitNamedInputParameter(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitNegative(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitNot(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitNotEquals(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitOr(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitOrderBy(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitOrderByItem(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitOuterFetchJoin(EJBQLJoin join) {
        return continueFlag;
    }

    public boolean visitOuterJoin(EJBQLJoin join) {
        return continueFlag;
    }

    public boolean visitPath(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitPatternValue(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitPositionalInputParameter(EJBQLPositionalInputParameter expression) {
        return continueFlag;
    }

    public boolean visitSelect(EJBQLExpression expression) {
        return continueFlag;
    }
    
    public boolean visitSelectClause(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitSelectExpression(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitSelectExpressions(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitSize(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitSqrt(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitStringLiteral(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitSubselect(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitSubstring(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitSubtract(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitSum(EJBQLAggregateColumn expression) {
        return continueFlag;
    }

    public boolean visitTok(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitTrim(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitTrimBoth(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitTrimCharacter(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitTrimLeading(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitTrimTrailing(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitUpdate(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitUpdateField(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitUpdateItem(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitUpdateValue(EJBQLExpression expression) {
        return continueFlag;
    }

    public boolean visitUpper(EJBQLExpression expression, int finishedChildIndex) {
        return continueFlag;
    }

    public boolean visitWhere(EJBQLExpression expression) {
        return continueFlag;
    }
}
