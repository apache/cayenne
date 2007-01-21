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


package org.apache.cayenne.dba.oracle;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.access.trans.SelectTranslator;
import org.apache.cayenne.query.QueryMetadata;

/**
 * Select translator that implements Oracle-specific optimizations.
 * 
 * @author Andrus Adamchik
 */
public class OracleSelectTranslator extends SelectTranslator {

    private static boolean testedDriver;
    private static boolean useOptimizations;
    private static Method statementSetRowPrefetch;

    private static final Object[] rowPrefetchArgs = new Object[] {
        new Integer(100)
    };

    public String createSqlString() throws Exception {

        String sqlString = super.createSqlString();

        QueryMetadata info = getQuery().getMetaData(getEntityResolver());
        if (info.getFetchLimit() > 0) {
            sqlString = "SELECT * FROM ("
                    + sqlString
                    + ") WHERE rownum <= "
                    + info.getFetchLimit();
        }

        return sqlString;
    }

    /**
     * Determines if we can use Oracle optimizations. If yes, configure this object to use
     * them via reflection.
     */
    private static final synchronized void testDriver(Statement st) {
        if (testedDriver) {
            return;
        }

        // invalid call.. give it another chance later
        if (st == null) {
            return;
        }

        testedDriver = true;

        try {
            // search for matching methods in class and its superclasses

            Class[] args2 = new Class[] {
                Integer.TYPE
            };
            statementSetRowPrefetch = st.getClass().getMethod("setRowPrefetch", args2);

            useOptimizations = true;
        }
        catch (Exception ex) {
            useOptimizations = false;
            statementSetRowPrefetch = null;
        }
    }

    /**
     * Translates internal query into PreparedStatement, applying Oracle optimizations if
     * possible.
     */
    public PreparedStatement createStatement() throws Exception {
        String sqlStr = createSqlString();
        QueryLogger.logQuery(sqlStr, values);
        PreparedStatement stmt = connection.prepareStatement(sqlStr);

        initStatement(stmt);

        if (!testedDriver) {
            testDriver(stmt);
        }

        if (useOptimizations) {
            // apply Oracle optimization of the statement

            // Performance tests conducted by Arndt (bug #699966) show
            // that using explicit "defineColumnType" slows things down,
            // so this is disabled now

            // 1. name result columns
            /*
             * List columns = getColumns(); int len = columns.size(); Object[] args = new
             * Object[2]; for (int i = 0; i < len; i++) { DbAttribute attr = (DbAttribute)
             * columns.get(i); args[0] = new Integer(i + 1); args[1] = new
             * Integer(attr.getType()); statementDefineColumnType.invoke(stmt, args); }
             */

            // 2. prefetch bigger batches of rows
            // [This optimization didn't give any measurable performance
            // increase. Keeping it for the future research]
            // Note that this is done by statement,
            // instead of Connection, since we do not want to mess
            // with Connection that is potentially used by
            // other people.
            statementSetRowPrefetch.invoke(stmt, rowPrefetchArgs);
        }

        return stmt;
    }
}
