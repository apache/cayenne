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
 * A translator of EJBQL DELETE statements into SQL.
 * 
 * @since 3.0
 */
public class EJBQLDeleteTranslator extends EJBQLBaseVisitor {

    protected EJBQLTranslationContext context;

    public EJBQLDeleteTranslator(EJBQLTranslationContext context) {
        this.context = context;
    }

    @Override
    public boolean visitDelete(EJBQLExpression expression) {
        context.append("DELETE");
        return true;
    }

    @Override
    public boolean visitFrom(EJBQLExpression expression, int finishedChildIndex) {
        context.append(" FROM");
        expression.visit(context.getTranslatorFactory().getFromTranslator(context));
        return false;
    }

    @Override
    public boolean visitWhere(EJBQLExpression expression) {
        context.append(" WHERE");
        expression.visit(context.getTranslatorFactory().getConditionTranslator(context));
        return false;
    }
}
