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

package org.apache.cayenne.dba.postgres;

import java.sql.Connection;

import org.apache.cayenne.access.trans.ProcedureTranslator;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.sqlserver.SQLServerProcedureAction;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.ProcedureQuery;

/**
 * Current implementation simply relies on SQLServerProcedureAction superclass behavior.
 * Namely that CallableStatement.execute() rewinds result set pointer so
 * CallableStatement.getMoreResults() shouldn't be invoked until the first result set is
 * processed.
 * 
 * @since 1.2
 */
class PostgresProcedureAction extends SQLServerProcedureAction {

    PostgresProcedureAction(ProcedureQuery query, JdbcAdapter adapter,
            EntityResolver entityResolver) {
        super(query, adapter, entityResolver);
    }

    /**
     * Creates a translator that adds parenthesis to no-param queries.
     */
    // see CAY-750 for the problem description
    @Override
    protected ProcedureTranslator createTranslator(Connection connection) {
        ProcedureTranslator translator = new PostgresProcedureTranslator();
        translator.setAdapter(getAdapter());
        translator.setQuery(query);
        translator.setEntityResolver(getEntityResolver());
        translator.setConnection(connection);
        translator.setJdbcEventLogger(adapter.getJdbcEventLogger());
        return translator;
    }

    static class PostgresProcedureTranslator extends ProcedureTranslator {

        @Override
        protected String createSqlString() {

            String sql = super.createSqlString();

            // add empty parameter parenthesis
            if (sql.endsWith("}") && !sql.endsWith(")}")) {
                sql = sql.substring(0, sql.length() - 1) + "()}";
            }

            return sql;
        }
    }
}
