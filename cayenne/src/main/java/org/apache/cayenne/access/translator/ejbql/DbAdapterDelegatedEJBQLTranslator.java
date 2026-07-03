/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access.translator.ejbql;

import org.apache.cayenne.access.translator.EJBQLTranslator;
import org.apache.cayenne.ejbql.EJBQLExpressionVisitor;

/**
 * An {@link EJBQLTranslator} that resolves the actual translator from {@link org.apache.cayenne.dba.DbAdapter#getEjbqlTranslator()}
 * allowing adapters to customize translation. The adapter is obtained from the {@link EJBQLTranslationContext}.
 *
 * @since 5.0
 */
public class DbAdapterDelegatedEJBQLTranslator implements EJBQLTranslator {

    @Override
    public EJBQLJoinAppender getJoinAppender(EJBQLTranslationContext context) {
        return delegate(context).getJoinAppender(context);
    }

    @Override
    public EJBQLExpressionVisitor getSelectTranslator(EJBQLTranslationContext context) {
        return delegate(context).getSelectTranslator(context);
    }

    @Override
    public EJBQLExpressionVisitor getDeleteTranslator(EJBQLTranslationContext context) {
        return delegate(context).getDeleteTranslator(context);
    }

    @Override
    public EJBQLExpressionVisitor getUpdateTranslator(EJBQLTranslationContext context) {
        return delegate(context).getUpdateTranslator(context);
    }

    @Override
    public EJBQLExpressionVisitor getAggregateColumnTranslator(EJBQLTranslationContext context) {
        return delegate(context).getAggregateColumnTranslator(context);
    }

    @Override
    public EJBQLExpressionVisitor getConditionTranslator(EJBQLTranslationContext context) {
        return delegate(context).getConditionTranslator(context);
    }

    @Override
    public EJBQLExpressionVisitor getFromTranslator(EJBQLTranslationContext context) {
        return delegate(context).getFromTranslator(context);
    }

    @Override
    public EJBQLExpressionVisitor getGroupByTranslator(EJBQLTranslationContext context) {
        return delegate(context).getGroupByTranslator(context);
    }

    @Override
    public EJBQLExpressionVisitor getIdentifierColumnsTranslator(EJBQLTranslationContext context) {
        return delegate(context).getIdentifierColumnsTranslator(context);
    }

    @Override
    public EJBQLExpressionVisitor getOrderByTranslator(EJBQLTranslationContext context) {
        return delegate(context).getOrderByTranslator(context);
    }

    @Override
    public EJBQLExpressionVisitor getSelectColumnsTranslator(EJBQLTranslationContext context) {
        return delegate(context).getSelectColumnsTranslator(context);
    }

    @Override
    public EJBQLExpressionVisitor getUpdateItemTranslator(EJBQLTranslationContext context) {
        return delegate(context).getUpdateItemTranslator(context);
    }

    private EJBQLTranslator delegate(EJBQLTranslationContext context) {
        return context.getAdapter().getEjbqlTranslator();
    }
}
