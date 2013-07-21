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
 * A translator of EJBQL UPDATE statements into SQL.
 * 
 * @since 3.0
 */
class EJBQLUpdateTranslator extends EJBQLBaseVisitor {

    private EJBQLTranslationContext context;
    private int itemCount;

    EJBQLUpdateTranslator(EJBQLTranslationContext context) {
        this.context = context;
    }

    EJBQLTranslationContext getContext() {
        return context;
    }

    @Override
    public boolean visitUpdate(EJBQLExpression expression) {
        context.append("UPDATE");
        return true;
    }

    @Override
    public boolean visitWhere(EJBQLExpression expression) {
        context.append(" WHERE");
        expression.visit(context.getTranslatorFactory().getConditionTranslator(context));
        return false;
    }

    @Override
    public boolean visitFrom(EJBQLExpression expression, int finishedChildIndex) {
        expression.visit(context.getTranslatorFactory().getFromTranslator(context));

        return false;
    }

    @Override
    public boolean visitUpdateItem(EJBQLExpression expression, int finishedChildIndex) {
        if (itemCount++ > 0) {
            context.append(',');
        }
        else {
            context.append(" SET");
        }

        expression.visit(context.getTranslatorFactory().getUpdateItemTranslator(context));
        return false;
    }
}
