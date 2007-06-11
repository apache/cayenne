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

import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.SQLActionVisitor;
import org.apache.cayenne.query.SQLTemplate;

/**
 * Parses an EJBQL statement, converting it to SQL. Executes the resulting SQL.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class EJBQLAction extends BaseSQLAction {

    protected SQLActionVisitor actionFactory;
    protected EJBQLQuery query;

    public EJBQLAction(EJBQLQuery query, SQLActionVisitor actionFactory,
            DbAdapter adapter, EntityResolver entityResolver) {
        super(adapter, entityResolver);

        this.query = query;
        this.actionFactory = actionFactory;
    }

    public void performAction(Connection connection, OperationObserver observer)
            throws SQLException, Exception {
        EJBQLCompiledExpression compiledExpression = query.getExpression(getEntityResolver());
        final EJBQLTranslationContext context = new EJBQLTranslationContext(compiledExpression);
        
        compiledExpression.getExpression().visit(new EJBQLBaseVisitor(false) {

            public boolean visitSelect(EJBQLExpression expression, int finishedChildIndex) {
                EJBQLSelectTranslator visitor = new EJBQLSelectTranslator(context);
                expression.visit(visitor);
                return false;
            }

            public boolean visitDelete(EJBQLExpression expression, int finishedChildIndex) {
                throw new UnsupportedOperationException("Not yet implemented");
            }

            public boolean visitUpdate(EJBQLExpression expression, int finishedChildIndex) {
                throw new UnsupportedOperationException("Not yet implemented");
            }
        });

        SQLTemplate sqlQuery = context.getQuery();
        actionFactory.sqlAction(sqlQuery).performAction(connection, observer);
    }
}
