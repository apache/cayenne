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
 * A visitor interface to inspect the EJBQL expression tree. Visit methods return
 * booleans, indicating whether the children of a given node should be visited.
 * 
 * @since 3.0
 */
public interface EJBQLExpressionVisitor {

    boolean visitAbs(EJBQLExpression expression, int finishedChildIndex);

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

    boolean visitAverage(EJBQLAggregateColumn expression);

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

    boolean visitConcat(EJBQLExpression expression, int finishedChildIndex);

    boolean visitConstructor(EJBQLExpression expression);

    boolean visitConstructorParameter(EJBQLExpression expression);

    boolean visitConstructorParameters(EJBQLExpression expression);

    boolean visitCount(EJBQLAggregateColumn expression);

    boolean visitCurrentDate(EJBQLExpression expression);

    boolean visitCurrentTime(EJBQLExpression expression);

    boolean visitCurrentTimestamp(EJBQLExpression expression);

    boolean visitDecimalLiteral(EJBQLDecimalLiteral expression);

    boolean visitDelete(EJBQLExpression expression);

    boolean visitDescending(EJBQLExpression expression);

    boolean visitDistinct(EJBQLExpression expression);

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

    boolean visitIn(EJBQLExpression expression, int finishedChildIndex);

    boolean visitInnerFetchJoin(EJBQLJoin join);

    boolean visitInnerJoin(EJBQLJoin join);

    boolean visitIntegerLiteral(EJBQLIntegerLiteral expression);

    boolean visitIsEmpty(EJBQLExpression expression);

    boolean visitIsNull(EJBQLExpression expression, int finishedChildIndex);

    boolean visitLength(EJBQLExpression expression, int finishedChildIndex);

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

    boolean visitLocate(EJBQLExpression expression, int finishedChildIndex);

    boolean visitLower(EJBQLExpression expression, int finishedChildIndex);

    boolean visitMax(EJBQLAggregateColumn expression);

    boolean visitMemberOf(EJBQLExpression expression);

    boolean visitMin(EJBQLAggregateColumn expression);

    boolean visitMod(EJBQLExpression expression, int finishedChildIndex);

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

    boolean visitOuterFetchJoin(EJBQLJoin join);

    boolean visitOuterJoin(EJBQLJoin join);

    /**
     * Called on visiting "path" expression and also after visiting every expression
     * child.
     * 
     * @param expression a "path" node being visited.
     * @param finishedChildIndex "-1" when the expression node is visited for the first
     *            time, before its children; otherwise this is an index of a child just
     *            visited.
     */
    boolean visitPath(EJBQLExpression expression, int finishedChildIndex);
    
    boolean visitDbPath(EJBQLExpression expression, int finishedChildIndex);

    boolean visitPatternValue(EJBQLExpression expression);

    boolean visitPositionalInputParameter(EJBQLPositionalInputParameter expression);

    boolean visitSelect(EJBQLExpression expression);
    
    boolean visitSelectClause(EJBQLExpression expression);

    boolean visitSelectExpression(EJBQLExpression expression);

    boolean visitSelectExpressions(EJBQLExpression expression);

    boolean visitSize(EJBQLExpression expression);

    boolean visitSqrt(EJBQLExpression expression, int finishedChildIndex);

    boolean visitStringLiteral(EJBQLExpression expression);

    boolean visitSubselect(EJBQLExpression expression);

    boolean visitSubstring(EJBQLExpression expression, int finishedChildIndex);

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

    boolean visitSum(EJBQLAggregateColumn expression);

    boolean visitTok(EJBQLExpression expression);

    boolean visitTrim(EJBQLExpression expression, int finishedChildIndex);

    boolean visitTrimBoth(EJBQLExpression expression);

    boolean visitTrimCharacter(EJBQLExpression expression);

    boolean visitTrimLeading(EJBQLExpression expression);

    boolean visitTrimTrailing(EJBQLExpression expression);

    boolean visitUpdate(EJBQLExpression expression);

    boolean visitUpdateField(EJBQLExpression expression, int finishedChildIndex);

    boolean visitUpdateItem(EJBQLExpression expression, int finishedChildIndex);

    boolean visitUpdateValue(EJBQLExpression expression);

    boolean visitUpper(EJBQLExpression expression, int finishedChildIndex);

    boolean visitWhere(EJBQLExpression expression);
}
