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
package org.apache.cayenne.access.translator.ejbql;

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLExpression;

/**
 * A translator of EJBQL SELECT statements into SQL.
 * 
 * @since 3.0
 */
public class EJBQLSelectTranslator extends EJBQLBaseVisitor {

    protected EJBQLTranslationContext context;

    protected EJBQLSelectTranslator(EJBQLTranslationContext context) {
        this.context = context;
    }

    EJBQLTranslationContext getContext() {
        return context;
    }

    @Override
    public boolean visitDistinct(EJBQLExpression expression) {
        // "distinct" is appended via a marker as sometimes a later match on to-many would
        // require a DISTINCT insertion.
        context.pushMarker(context.makeDistinctMarker(), true);
        context.append(" DISTINCT");
        context.popMarker();
        return true;
    }

    @Override
    public boolean visitFrom(EJBQLExpression expression, int finishedChildIndex) {
        context.append(" FROM");
        context.setAppendingResultColumns(false);
        expression.visit(context.getTranslatorFactory().getFromTranslator(context));
        context.markCurrentPosition(context.makeWhereMarker());
        context.markCurrentPosition(context.makeEntityQualifierMarker());
        return false;
    }

    @Override
    public boolean visitGroupBy(EJBQLExpression expression) {
        context.append(" GROUP BY");
        expression.visit(context.getTranslatorFactory().getGroupByTranslator(context));
        return false;
    }

    @Override
    public boolean visitHaving(EJBQLExpression expression) {
        context.append(" HAVING");
        expression.visit(context.getTranslatorFactory().getConditionTranslator(context));
        return false;
    }

    @Override
    public boolean visitOrderBy(EJBQLExpression expression) {
        context.append(" ORDER BY");
        expression.visit(context.getTranslatorFactory().getOrderByTranslator(context));
        return false;
    }

    @Override
    public boolean visitSelect(EJBQLExpression expression) {
        // this ensures that result columns are appeneded only in top-level select, but
        // not subselect (as 'visitSelect' is not called on subselect)
        context.setAppendingResultColumns(true);
        return true;
    }

    @Override
    public boolean visitSelectClause(EJBQLExpression expression) {
        context.append("SELECT");
        context.markCurrentPosition(context.makeDistinctMarker());
        return true;
    }

    @Override
    public boolean visitSelectExpressions(EJBQLExpression expression) {
        expression.visit(context.getTranslatorFactory().getSelectColumnsTranslator(
                context));
        return false;
    }

    @Override
    public boolean visitWhere(EJBQLExpression expression) {
        // "WHERE" is appended via a marker as it may have been already appended when an
        // entity inheritance qualifier was applied.
        context.pushMarker(context.makeWhereMarker(), true);
        context.append(" WHERE");
        context.popMarker();

        if (context.findOrCreateMarkedBuffer(context.makeEntityQualifierMarker()).length() > 0) {
            context.append(" AND");
        }

        expression.visit(context.getTranslatorFactory().getConditionTranslator(context));
        return false;
    }

}
