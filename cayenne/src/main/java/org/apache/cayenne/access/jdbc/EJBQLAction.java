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
package org.apache.cayenne.access.jdbc;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslationContext;
import org.apache.cayenne.access.translator.EJBQLTranslator;
import org.apache.cayenne.access.translator.TranslatedStatement;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.EJBQLExpressionVisitor;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SQLActionVisitor;
import org.apache.cayenne.query.SQLTemplate;

import java.sql.Connection;
import java.util.List;

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
    public void performAction(Connection connection, OperationObserver observer) throws Exception {
        EJBQLCompiledExpression compiledExpression = query.getExpression(dataNode.getEntityResolver());
        EJBQLTranslator translator = dataNode.getEjbqlTranslator();

        DbEntity rootDbEntity = compiledExpression.getRootDescriptor().getEntity().getDbEntity();
        QuotingStrategy quotingStrategy = dataNode.getAdapter().getQuotingStrategy(rootDbEntity);

        EJBQLTranslationContext context = new EJBQLTranslationContext(
                dataNode.getEntityResolver(),
                query,
                compiledExpression,
                translator,
                dataNode.getAdapter(),
                quotingStrategy);

        compiledExpression.getExpression().visit(new EJBQLBaseVisitor(false) {

            @Override
            public boolean visitSelect(EJBQLExpression expression) {
                EJBQLExpressionVisitor visitor = translator.getSelectTranslator(context);
                expression.visit(visitor);
                return false;
            }

            @Override
            public boolean visitDelete(EJBQLExpression expression) {
                EJBQLExpressionVisitor visitor = translator.getDeleteTranslator(context);
                expression.visit(visitor);
                return false;
            }

            @Override
            public boolean visitUpdate(EJBQLExpression expression) {
                EJBQLExpressionVisitor visitor = translator.getUpdateTranslator(context);
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

        int queryTimeout = md.getQueryTimeout();
        if (queryTimeout != QueryMetadata.QUERY_TIMEOUT_DEFAULT) {
            sqlQuery.setQueryTimeout(queryTimeout);
        }

        // the SQLTemplate is a substitute for the original EJBQLQuery; wrap the observer so that results are reported
        // against the EJBQLQuery the caller submitted rather than the internally compiled SQLTemplate
        actionFactory.sqlAction(sqlQuery).performAction(connection, new OriginalQueryObserver(observer, query));
    }
    
    static class OriginalQueryObserver implements OperationObserver {

        private final OperationObserver delegate;
        private final Query originalQuery;

        OriginalQueryObserver(OperationObserver delegate, Query originalQuery) {
            this.delegate = delegate;
            this.originalQuery = originalQuery;
        }

        @Override
        public void nextStatement(Query query, TranslatedStatement statement) {
            delegate.nextStatement(originalQuery, statement);
        }

        @Override
        public void onSuccess() {
            delegate.onSuccess();
        }

        @Override
        public void nextCount(Query query, int resultCount) {
            delegate.nextCount(originalQuery, resultCount);
        }

        @Override
        public void nextBatchCount(Query query, int[] resultCount) {
            delegate.nextBatchCount(originalQuery, resultCount);
        }

        @Override
        public void nextRows(Query query, List<?> dataRows) {
            delegate.nextRows(originalQuery, dataRows);
        }

        @Override
        public void nextRows(Query query, ResultIterator<?> it) {
            delegate.nextRows(originalQuery, it);
        }

        @Override
        public void nextGeneratedRows(Query query, List<DataRow> keys, List<ObjectId> idsToUpdate) {
            delegate.nextGeneratedRows(originalQuery, keys, idsToUpdate);
        }

        @Override
        public void nextQueryException(Query query, Exception ex) {
            delegate.nextQueryException(originalQuery, ex);
        }

        @Override
        public void nextGlobalException(Exception ex) {
            delegate.nextGlobalException(ex);
        }

        @Override
        public boolean isIteratedResult() {
            return delegate.isIteratedResult();
        }
    }
}
