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
 * A visitor interface to inspect the EJBQL expression tree.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public interface EJBQLExpressionVisitor {

    boolean visitAbs(EJBQLExpression expression);

    boolean visitAbstractSchemaName(EJBQLExpression expression);

    /**
     * Called on visiting "add" expression and also after visiting every expression child.
     * 
     * @param expression an "add" node being visited.
     * @param finishedChildIndex "-1" when the expression node is visited for the first
     *            time, before its children; otherwise this is an index of a child just
     *            visited.
     */
    boolean visitAdd(EJBQLExpression expression, int finishedChildIndex);

    boolean visitAggregate(EJBQLExpression expression);

    boolean visitAll(EJBQLExpression expression);

    /**
     * Called on visiting "and" expression and also after visiting every expression child.
     * 
     * @param expression an "and" node being visited.
     * @param finishedChildIndex "-1" when the expression node is visited for the first
     *            time, before its children; otherwise this is an index of a child just
     *            visited.
     */
    boolean visitAnd(EJBQLExpression expression, int finishedChildIndex);

    boolean visitAny(EJBQLExpression expression);

    boolean visitAscending(EJBQLExpression expression);

    boolean visitAverage(EJBQLExpression expression);

    /**
     * Called on visiting "between" expression and also after visiting every expression
     * child.
     * 
     * @param expression an "between" node being visited.
     * @param finishedChildIndex "-1" when the expression node is visited for the first
     *            time, before its children; otherwise this is an index of a child just
     *            visited.
     */
    boolean visitBetween(EJBQLExpression expression, int finishedChildIndex);

    boolean visitBooleanLiteral(EJBQLExpression expression);

    boolean visitClassName(EJBQLExpression expression);

    boolean visitConcat(EJBQLExpression expression);

    boolean visitConstructor(EJBQLExpression expression);

    boolean visitConstructorParameter(EJBQLExpression expression);

    boolean visitConstructorParameters(EJBQLExpression expression);

    boolean visitCount(EJBQLExpression expression);

    boolean visitCurrentDate(EJBQLExpression expression);

    boolean visitCurrentTime(EJBQLExpression expression);

    boolean visitCurrentTimestamp(EJBQLExpression expression);

    boolean visitDecimalLiteral(EJBQLExpression expression);

    /**
     * Called on visiting "delete" expression and also after visiting every expression
     * child.
     * 
     * @param expression a "delete" node being visited.
     * @param finishedChildIndex "-1" when the expression node is visited for the first
     *            time, before its children; otherwise this is an index of a child just
     *            visited.
     */
    boolean visitDelete(EJBQLExpression expression, int finishedChildIndex);

    boolean visitDescending(EJBQLExpression expression);

    boolean visitDistinct(EJBQLExpression expression);

    boolean visitDistinctPath(EJBQLExpression expression);

    /**
     * Called on visiting "divide" expression and also after visiting every expression
     * child.
     * 
     * @param expression an "divide" node being visited.
     * @param finishedChildIndex "-1" when the expression node is visited for the first
     *            time, before its children; otherwise this is an index of a child just
     *            visited.
     */
    boolean visitDivide(EJBQLExpression expression, int finishedChildIndex);

    /**
     * Called on visiting "equals" expression and also after visiting every expression
     * child.
     * 
     * @param expression an "equals" node being visited.
     * @param finishedChildIndex "-1" when the expression node is visited for the first
     *            time, before its children; otherwise this is an index of a child just
     *            visited.
     */
    boolean visitEquals(EJBQLExpression expression, int finishedChildIndex);

    boolean visitEscapeCharacter(EJBQLExpression expression);

    boolean visitExists(EJBQLExpression expression);

    boolean visitFrom(EJBQLExpression expression, int finishedChildIndex);

    boolean visitFromItem(EJBQLFromItem expression, int finishedChildIndex);

    /**
     * Called on visiting ">=" expression and also after visiting every expression child.
     * 
     * @param expression an ">=" node being visited.
     * @param finishedChildIndex "-1" when the expression node is visited for the first
     *            time, before its children; otherwise this is an index of a child just
     *            visited.
     */
    boolean visitGreaterOrEqual(EJBQLExpression expression, int finishedChildIndex);

    /**
     * Called on visiting ">=" expression and also after visiting every expression child.
     * 
     * @param expression an ">=" node being visited.
     * @param finishedChildIndex "-1" when the expression node is visited for the first
     *            time, before its children; otherwise this is an index of a child just
     *            visited.
     */
    boolean visitGreaterThan(EJBQLExpression expression, int finishedChildIndex);

    boolean visitGroupBy(EJBQLExpression expression);

    boolean visitHaving(EJBQLExpression expression);

    boolean visitIdentificationVariable(EJBQLExpression expression);

    boolean visitIdentifier(EJBQLExpression expression);

    boolean visitIn(EJBQLExpression expression);

    boolean visitInnerFetchJoin(EJBQLJoin join, int finishedChildIndex);

    boolean visitInnerJoin(EJBQLJoin join, int finishedChildIndex);

    boolean visitIntegerLiteral(EJBQLExpression expression);

    boolean visitIsEmpty(EJBQLExpression expression);

    boolean visitIsNull(EJBQLExpression expression);

    boolean visitLength(EJBQLExpression expression);

    /**
     * Called on visiting "<=" expression and also after visiting every expression child.
     * 
     * @param expression an "<=" node being visited.
     * @param finishedChildIndex "-1" when the expression node is visited for the first
     *            time, before its children; otherwise this is an index of a child just
     *            visited.
     */
    boolean visitLessOrEqual(EJBQLExpression expression, int finishedChildIndex);

    /**
     * Called on visiting "<" expression and also after visiting every expression child.
     * 
     * @param expression an "<" node being visited.
     * @param finishedChildIndex "-1" when the expression node is visited for the first
     *            time, before its children; otherwise this is an index of a child just
     *            visited.
     */
    boolean visitLessThan(EJBQLExpression expression, int finishedChildIndex);

    /**
     * Called on visiting "LIKE" expression and also after visiting every expression
     * child.
     * 
     * @param expression an "LIKE" node being visited.
     * @param finishedChildIndex "-1" when the expression node is visited for the first
     *            time, before its children; otherwise this is an index of a child just
     *            visited.
     */
    boolean visitLike(EJBQLExpression expression, int finishedChildIndex);

    boolean visitLocate(EJBQLExpression expression);

    boolean visitLower(EJBQLExpression expression);

    boolean visitMax(EJBQLExpression expression);

    boolean visitMemberOf(EJBQLExpression expression);

    boolean visitMin(EJBQLExpression expression);

    boolean visitMod(EJBQLExpression expression);

    /**
     * Called on visiting "*" expression and also after visiting every expression child.
     * 
     * @param expression an "*" node being visited.
     * @param finishedChildIndex "-1" when the expression node is visited for the first
     *            time, before its children; otherwise this is an index of a child just
     *            visited.
     */
    boolean visitMultiply(EJBQLExpression expression, int finishedChildIndex);

    boolean visitNamedInputParameter(EJBQLExpression expression);

    boolean visitNegative(EJBQLExpression expression);

    boolean visitNot(EJBQLExpression expression);

    /**
     * Called on visiting "!=" expression and also after visiting every expression child.
     * 
     * @param expression an "!=" node being visited.
     * @param finishedChildIndex "-1" when the expression node is visited for the first
     *            time, before its children; otherwise this is an index of a child just
     *            visited.
     */
    boolean visitNotEquals(EJBQLExpression expression, int finishedChildIndex);

    /**
     * Called on visiting "or" expression and also after visiting every expression child.
     * 
     * @param expression an "or" node being visited.
     * @param finishedChildIndex "-1" when the expression node is visited for the first
     *            time, before its children; otherwise this is an index of a child just
     *            visited.
     */
    boolean visitOr(EJBQLExpression expression, int finishedChildIndex);

    boolean visitOrderBy(EJBQLExpression expression);

    boolean visitOrderByItem(EJBQLExpression expression);

    boolean visitOuterFetchJoin(EJBQLJoin join, int finishedChildIndex);

    boolean visitOuterJoin(EJBQLJoin join, int finishedChildIndex);

    /**
     * Called on visiting "path" expression and also after visiting every expression
     * child.
     * 
     * @param expression a "path" node being visited.
     * @param finishedChildIndex "-1" when the expression node is visited for the first
     *            time, before its children; otherwise this is an index of a child just
     *            visited.
     */
    boolean visitPath(EJBQLPath expression, int finishedChildIndex);

    boolean visitPatternValue(EJBQLExpression expression);

    boolean visitPositionalInputParameter(EJBQLExpression expression);

    /**
     * Called on visiting "select" and also after visiting every expression child.
     * 
     * @param expression a "select" node being visited.
     * @param finishedChildIndex "-1" when the expression node is visited for the first
     *            time, before its children; otherwise this is an index of a child just
     *            visited.
     */
    boolean visitSelect(EJBQLExpression expression, int finishedChildIndex);

    boolean visitSelectExpression(EJBQLExpression expression);

    boolean visitSize(EJBQLExpression expression);

    boolean visitSqrt(EJBQLExpression expression);

    boolean visitStringLiteral(EJBQLExpression expression);

    boolean visitSubselect(EJBQLExpression expression);

    boolean visitSubstring(EJBQLExpression expression);

    /**
     * Called on visiting "subtract" expression and also after visiting every expression
     * child.
     * 
     * @param expression an "subtract" node being visited.
     * @param finishedChildIndex "-1" when the expression node is visited for the first
     *            time, before its children; otherwise this is an index of a child just
     *            visited.
     */
    boolean visitSubtract(EJBQLExpression expression, int finishedChildIndex);

    boolean visitSum(EJBQLExpression expression);

    boolean visitTok(EJBQLExpression expression);

    boolean visitTrim(EJBQLExpression expression);

    boolean visitTrimBoth(EJBQLExpression expression);

    boolean visitTrimCharacter(EJBQLExpression expression);

    boolean visitTrimLeading(EJBQLExpression expression);

    boolean visitTrimTrailing(EJBQLExpression expression);

    /**
     * Called on visiting "update" expression and also after visiting every expression
     * child.
     * 
     * @param expression a "update" node being visited.
     * @param finishedChildIndex "-1" when the expression node is visited for the first
     *            time, before its children; otherwise this is an index of a child just
     *            visited.
     */
    boolean visitUpdate(EJBQLExpression expression, int finishedChildIndex);

    boolean visitUpdateField(EJBQLExpression expression);

    boolean visitUpdateItem(EJBQLExpression expression);

    boolean visitUpdateValue(EJBQLExpression expression);

    boolean visitUpper(EJBQLExpression expression);

    boolean visitWhere(EJBQLExpression expression, int finishedChildIndex);
}
