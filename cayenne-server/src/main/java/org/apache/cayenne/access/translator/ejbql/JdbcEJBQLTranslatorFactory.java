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

import org.apache.cayenne.ejbql.EJBQLExpressionVisitor;

/**
 * A default EJBQLTranslatorFactory.
 * 
 * @since 3.0
 */
public class JdbcEJBQLTranslatorFactory implements EJBQLTranslatorFactory {

    protected static final String JOIN_APPENDER_KEY = "$JoinAppender";
    
    protected boolean caseInsensitive = false;

    public EJBQLJoinAppender getJoinAppender(EJBQLTranslationContext context) {
        EJBQLJoinAppender appender = (EJBQLJoinAppender) context
                .getAttribute(JOIN_APPENDER_KEY);

        if (appender == null) {
            appender = new EJBQLJoinAppender(context);
            context.setAttribute(JOIN_APPENDER_KEY, appender);
        }

        return appender;
    }

    public EJBQLExpressionVisitor getDeleteTranslator(EJBQLTranslationContext context) {
        context.setUsingAliases(false);
        return new EJBQLDeleteTranslator(context);
    }

    public EJBQLExpressionVisitor getSelectTranslator(EJBQLTranslationContext context) {
        return new EJBQLSelectTranslator(context);
    }

    public EJBQLExpressionVisitor getUpdateTranslator(EJBQLTranslationContext context) {
        context.setUsingAliases(false);
        return new EJBQLUpdateTranslator(context);
    }

    public EJBQLExpressionVisitor getAggregateColumnTranslator(
            EJBQLTranslationContext context) {
        return new EJBQLAggregateColumnTranslator(context);
    }

    public EJBQLExpressionVisitor getConditionTranslator(EJBQLTranslationContext context) {
        context.setCaseInsensitive(caseInsensitive);
        return new EJBQLConditionTranslator(context);
    }

    public EJBQLExpressionVisitor getFromTranslator(EJBQLTranslationContext context) {
        return new EJBQLFromTranslator(context);
    }

    public EJBQLExpressionVisitor getGroupByTranslator(EJBQLTranslationContext context) {
        return new EJBQLGroupByTranslator(context);
    }

    public EJBQLExpressionVisitor getIdentifierColumnsTranslator(
            EJBQLTranslationContext context) {
        if(context.getMetadata().getPageSize() > 0){
            return new EJBQLIdColumnsTranslator(context);
        }
        else{
            return new EJBQLIdentifierColumnsTranslator(context);
        }
    }

    public EJBQLExpressionVisitor getOrderByTranslator(EJBQLTranslationContext context) {
        return new EJBQLOrderByTranslator(context);
    }

    public EJBQLExpressionVisitor getSelectColumnsTranslator(
            EJBQLTranslationContext context) {
        return new EJBQLSelectColumnsTranslator(context);
    }

    public EJBQLExpressionVisitor getUpdateItemTranslator(EJBQLTranslationContext context) {
        return new EJBQLUpdateItemTranslator(context);
    }
    
    public void setCaseInsensitive(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }
}
