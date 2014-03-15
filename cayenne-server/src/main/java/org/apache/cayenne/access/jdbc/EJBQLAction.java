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

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslationContext;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslatorFactory;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.EJBQLExpressionVisitor;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SQLActionVisitor;
import org.apache.cayenne.query.SQLTemplate;

/**
 * Parses an EJBQL statement, converting it to SQL. Executes the resulting SQL.
 * 
 * @since 3.0
 */
public class EJBQLAction extends BaseSQLAction {

    protected SQLActionVisitor actionFactory;
    protected EJBQLQuery query;

    public EJBQLAction(EJBQLQuery query, SQLActionVisitor actionFactory, DataNode dataNode) {
        super(dataNode);

        this.query = query;
        this.actionFactory = actionFactory;
    }

    @Override
    public void performAction(Connection connection, OperationObserver observer)
 throws SQLException, Exception {
        EJBQLCompiledExpression compiledExpression = query.getExpression(dataNode.getEntityResolver());
        final EJBQLTranslatorFactory translatorFactory = ((JdbcAdapter) dataNode.getAdapter())
                .getEjbqlTranslatorFactory();
        final EJBQLTranslationContext context = new EJBQLTranslationContext(dataNode.getEntityResolver(), query,
                compiledExpression, translatorFactory, dataNode.getAdapter().getQuotingStrategy());

        compiledExpression.getExpression().visit(new EJBQLBaseVisitor(false) {

            @Override
            public boolean visitSelect(EJBQLExpression expression) {
                EJBQLExpressionVisitor visitor = translatorFactory.getSelectTranslator(context);
                expression.visit(visitor);
                return false;
            }

            @Override
            public boolean visitDelete(EJBQLExpression expression) {
                EJBQLExpressionVisitor visitor = translatorFactory.getDeleteTranslator(context);
                expression.visit(visitor);
                return false;
            }

            @Override
            public boolean visitUpdate(EJBQLExpression expression) {
                EJBQLExpressionVisitor visitor = translatorFactory.getUpdateTranslator(context);
                expression.visit(visitor);
                return false;
            }
        });

        SQLTemplate sqlQuery = context.getQuery();

        // update with metadata
        QueryMetadata md = query.getMetaData(dataNode.getEntityResolver());
        sqlQuery.setFetchLimit(md.getFetchLimit());
        sqlQuery.setFetchOffset(md.getFetchOffset());
        sqlQuery.setResult(compiledExpression.getResult());
        sqlQuery.setPageSize(md.getPageSize());

        if (md.getStatementFetchSize() != 0) {
            sqlQuery.setStatementFetchSize(md.getStatementFetchSize());
        }

        actionFactory.sqlAction(sqlQuery).performAction(connection, observer);
    }
}
