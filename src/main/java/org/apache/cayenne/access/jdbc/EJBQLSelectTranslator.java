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
package org.apache.cayenne.access.jdbc;

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLExpression;

/**
 * A translator of EJBQL SELECT statements into SQL.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class EJBQLSelectTranslator extends EJBQLBaseVisitor {

    private EJBQLTranslationContext context;

    static String makeDistinctMarker() {
        return "DISTINCT_MARKER";
    }

    static String makeWhereMarker() {
        return "WHERE_MARKER";
    }

    EJBQLSelectTranslator(EJBQLTranslationContext context) {
        this.context = context;
    }

    EJBQLTranslationContext getContext() {
        return context;
    }

    public boolean visitDistinct(EJBQLExpression expression) {
        // "distinct" is appended via a marker as sometimes a later match on to-many would
        // require a DISTINCT insertion.
        context.pushMarker(makeDistinctMarker(), true);
        context.append(" DISTINCT");
        context.popMarker();
        return true;
    }

    public boolean visitFrom(EJBQLExpression expression, int finishedChildIndex) {
        context.append(" FROM");
        context.setAppendingResultColumns(false);
        expression.visit(context.getTranslatorFactory().getFromTranslator(context));
        context.markCurrentPosition(makeWhereMarker());
        return false;
    }

    public boolean visitGroupBy(EJBQLExpression expression) {
        context.append(" GROUP BY");
        expression.visit(context.getTranslatorFactory().getGroupByTranslator(context));
        return false;
    }

    public boolean visitHaving(EJBQLExpression expression) {
        context.append(" HAVING");
        expression.visit(context.getTranslatorFactory().getConditionTranslator(context));
        return false;
    }

    public boolean visitOrderBy(EJBQLExpression expression) {
        context.append(" ORDER BY");
        expression.visit(context.getTranslatorFactory().getOrderByTranslator(context));
        return false;
    }

    public boolean visitSelect(EJBQLExpression expression) {
        // this ensures that result columns are appeneded only in top-level select, but
        // not subselect (as 'visitSelect' is not called on subselect)
        context.setAppendingResultColumns(true);
        return true;
    }

    public boolean visitSelectClause(EJBQLExpression expression) {
        context.append("SELECT");
        context.markCurrentPosition(makeDistinctMarker());
        return true;
    }

    public boolean visitSelectExpressions(EJBQLExpression expression) {
        expression.visit(context.getTranslatorFactory().getSelectColumnsTranslator(
                context));
        return false;
    }

    public boolean visitWhere(EJBQLExpression expression) {
        // "WHERE" is appended via a marker as it may have been already appended when an
        // entity inheritance qualifier was applied.
        context.pushMarker(makeWhereMarker(), true);
        context.append(" WHERE");
        context.popMarker();

        expression.visit(context.getTranslatorFactory().getConditionTranslator(context));
        return false;
    }
}
