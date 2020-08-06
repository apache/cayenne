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

import org.apache.cayenne.ejbql.EJBQLExpressionVisitor;

/**
 * Defines a factory for translation visitors of EJBQL. DbAdapters can customize EJBQL
 * translation by providing their own factory implementation.
 * 
 * @since 3.0
 */
public interface EJBQLTranslatorFactory {
    
    EJBQLJoinAppender getJoinAppender(EJBQLTranslationContext context);

    EJBQLExpressionVisitor getSelectTranslator(EJBQLTranslationContext context);

    EJBQLExpressionVisitor getDeleteTranslator(EJBQLTranslationContext context);

    EJBQLExpressionVisitor getUpdateTranslator(EJBQLTranslationContext context);

    EJBQLExpressionVisitor getAggregateColumnTranslator(EJBQLTranslationContext context);

    EJBQLExpressionVisitor getConditionTranslator(EJBQLTranslationContext context);

    EJBQLExpressionVisitor getFromTranslator(EJBQLTranslationContext context);

    EJBQLExpressionVisitor getGroupByTranslator(EJBQLTranslationContext context);

    EJBQLExpressionVisitor getIdentifierColumnsTranslator(EJBQLTranslationContext context);

    EJBQLExpressionVisitor getOrderByTranslator(EJBQLTranslationContext context);

    EJBQLExpressionVisitor getSelectColumnsTranslator(EJBQLTranslationContext context);

    EJBQLExpressionVisitor getUpdateItemTranslator(EJBQLTranslationContext context);
}
