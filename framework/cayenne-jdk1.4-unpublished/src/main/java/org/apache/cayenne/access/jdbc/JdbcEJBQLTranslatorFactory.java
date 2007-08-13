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

import org.apache.cayenne.ejbql.EJBQLExpressionVisitor;

/**
 * A default EJBQLTranslatorFactory.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class JdbcEJBQLTranslatorFactory implements EJBQLTranslatorFactory {

    public EJBQLExpressionVisitor getDeleteTranslator(EJBQLTranslationContext context) {
        return new EJBQLDeleteTranslator(context);
    }

    public EJBQLExpressionVisitor getSelectTranslator(EJBQLTranslationContext context) {
        return new EJBQLSelectTranslator(context);
    }

    public EJBQLExpressionVisitor getUpdateTranslator(EJBQLTranslationContext context) {
        return new EJBQLUpdateTranslator(context);
    }

    public EJBQLExpressionVisitor getAggregateColumnTranslator(
            EJBQLTranslationContext context) {
        return new EJBQLAggregateColumnTranslator(context);
    }

    public EJBQLExpressionVisitor getConditionTranslator(EJBQLTranslationContext context) {
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
        return new EJBQLIdentifierColumnsTranslator(context);
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
}
