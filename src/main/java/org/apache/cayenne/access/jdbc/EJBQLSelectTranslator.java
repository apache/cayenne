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

    EJBQLSelectTranslator(EJBQLTranslationContext context) {
        this.context = context;
    }

    EJBQLTranslationContext getContext() {
        return context;
    }

    public boolean visitDistinct(EJBQLExpression expression) {
        context.append(" DISTINCT");
        return true;
    }

    public boolean visitFrom(EJBQLExpression expression, int finishedChildIndex) {
        context.append(" FROM");
        expression.visit(new EJBQLFromTranslator(context));
        return false;
    }

    public boolean visitGroupBy(EJBQLExpression expression) {
        context.append(" GROUP BY");
        expression.visit(new EJBQLGroupByTranslator(context));
        return false;
    }
    
    public boolean visitHaving(EJBQLExpression expression) {
        context.append(" HAVING");
        expression.visit(new EJBQLConditionTranslator(context));
        return false;
    }

    public boolean visitOrderBy(EJBQLExpression expression) {
        context.append(" ORDER BY");
        expression.visit(new EJBQLOrderByTranslator(context));
        return false;
    }

    public boolean visitSelect(EJBQLExpression expression) {
        context.append("SELECT");
        return true;
    }

    public boolean visitSelectExpressions(EJBQLExpression expression) {
        expression.visit(new EJBQLSelectColumnsTranslator(context));
        return false;
    }

    public boolean visitWhere(EJBQLExpression expression) {
        context.append(" WHERE");
        expression.visit(new EJBQLConditionTranslator(context));
        return false;
    }
}
