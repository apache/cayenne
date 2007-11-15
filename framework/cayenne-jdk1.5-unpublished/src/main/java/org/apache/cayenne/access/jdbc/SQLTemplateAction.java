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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.IteratorUtils;
import org.apache.cayenne.CayenneException;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.util.Util;

/**
 * Implements a strategy for execution of SQLTemplates.
 * 
 * @author Andrus Adamchik
 * @since 1.2 replaces SQLTemplateExecutionPlan
 */
public class SQLTemplateAction implements SQLAction {

    protected DbAdapter adapter;
    protected SQLTemplate query;

    public SQLTemplateAction(SQLTemplate query, DbAdapter adapter) {
        this.query = query;
        this.adapter = adapter;
    }

    /**
     * Returns DbAdapter associated with this execution plan object.
     */
    public DbAdapter getAdapter() {
        return adapter;
    }

    /**
     * Runs a SQLTemplate query, collecting all results. If a callback expects an iterated
     * result, result processing is stopped after the first ResultSet is encountered.
     */
    public void performAction(Connection connection, OperationObserver callback)
            throws SQLException, Exception {

        String template = extractTemplateString();

        // sanity check - misconfigured templates
        if (template == null) {
            throw new CayenneException("No template string configured for adapter "
                    + getAdapter().getClass().getName());
        }

        boolean loggable = QueryLogger.isLoggable();
        int size = query.parametersSize();

        SQLTemplateProcessor templateProcessor = new SQLTemplateProcessor();

        // zero size indicates a one-shot query with no parameters
        // so fake a single entry batch...
        int batchSize = (size > 0) ? size : 1;

        List counts = new ArrayList(batchSize);
        Iterator it = (size > 0) ? query.parametersIterator() : IteratorUtils
                .singletonIterator(Collections.EMPTY_MAP);
        for (int i = 0; i < batchSize; i++) {
            Map nextParameters = (Map) it.next();

            SQLStatement compiled = templateProcessor.processTemplate(
                    template,
                    nextParameters);

            if (loggable) {
                QueryLogger.logQuery(compiled.getSql(), Arrays.asList(compiled
                        .getBindings()));
            }

            execute(connection, callback, compiled, counts);
        }

        // notify of combined counts of all queries inside SQLTemplate multipled by the
        // number of parameter sets...
        int[] ints = new int[counts.size()];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = ((Integer) counts.get(i)).intValue();
        }

        callback.nextBatchCount(query, ints);
    }

    protected void execute(
            Connection connection,
            OperationObserver callback,
            SQLStatement compiled,
            Collection updateCounts) throws SQLException, Exception {

        long t1 = System.currentTimeMillis();
        boolean iteratedResult = callback.isIteratedResult();
        PreparedStatement statement = connection.prepareStatement(compiled.getSql());
        try {
            bind(statement, compiled.getBindings());

            // process a mix of results
            boolean isResultSet = statement.execute();
            boolean firstIteration = true;
            while (true) {

                if (firstIteration) {
                    firstIteration = false;
                }
                else {
                    isResultSet = statement.getMoreResults();
                }

                if (isResultSet) {

                    ResultSet resultSet = statement.getResultSet();

                    if (resultSet != null) {

                        try {
                            processSelectResult(
                                    compiled,
                                    connection,
                                    statement,
                                    resultSet,
                                    callback,
                                    t1);
                        }
                        finally {
                            if (!iteratedResult) {
                                resultSet.close();
                            }
                        }
                        
                        // ignore possible following update counts and bail early on
                        // iterated results
                        if (iteratedResult) {
                            break;
                        }
                    }
                }
                else {
                    int updateCount = statement.getUpdateCount();
                    if (updateCount == -1) {
                        break;
                    }

                    updateCounts.add(new Integer(updateCount));
                    QueryLogger.logUpdateCount(updateCount);
                }
            }
        }
        finally {
            if (!iteratedResult) {
                statement.close();
            }
        }
    }

    protected void processSelectResult(
            SQLStatement compiled,
            Connection connection,
            Statement statement,
            ResultSet resultSet,
            OperationObserver callback,
            long startTime) throws Exception {

        boolean iteratedResult = callback.isIteratedResult();

        ExtendedTypeMap types = adapter.getExtendedTypes();
        RowDescriptor descriptor = (compiled.getResultColumns().length > 0)
                ? new RowDescriptor(compiled.getResultColumns(), types)
                : new RowDescriptor(resultSet, types);
                
           if (query.getColumnNamesCapitalization() != null) {
            if (SQLTemplate.LOWERCASE_COLUMN_NAMES.equals(query
                    .getColumnNamesCapitalization())) {
                descriptor.forceLowerCaseColumnNames();
            }
            else if (SQLTemplate.UPPERCASE_COLUMN_NAMES.equals(query
                    .getColumnNamesCapitalization())) {
                descriptor.forceUpperCaseColumnNames();
            }
        }

        JDBCResultIterator result = new JDBCResultIterator(
                connection,
                statement,
                resultSet,
                descriptor,
                query.getFetchLimit());

        if (!iteratedResult) {
            List resultRows = result.dataRows(false);
            QueryLogger.logSelectCount(resultRows.size(), System.currentTimeMillis()
                    - startTime);

            callback.nextDataRows(query, resultRows);
        }
        else {
            try {
                result.setClosingConnection(true);
                callback.nextDataRows(query, result);
            }
            catch (Exception ex) {
                result.close();
                throw ex;
            }
        }
    }

    /**
     * Extracts a template string from a SQLTemplate query. Exists mainly for the benefit
     * of subclasses that can customize returned template.
     * 
     * @since 1.2
     */
    protected String extractTemplateString() {
        String sql = query.getTemplate(getAdapter().getClass().getName());

        // note that we MUST convert line breaks to spaces. On some databases (DB2)
        // queries with breaks simply won't run; the rest are affected by CAY-726.
        return Util.stripLineBreaks(sql, " ");
    }

    /**
     * Binds parameters to the PreparedStatement.
     */
    protected void bind(PreparedStatement preparedStatement, ParameterBinding[] bindings)
            throws SQLException, Exception {
        // bind parameters
        if (bindings.length > 0) {
            int len = bindings.length;
            for (int i = 0; i < len; i++) {
                adapter.bindParameter(
                        preparedStatement,
                        bindings[i].getValue(),
                        i + 1,
                        bindings[i].getJdbcType(),
                        bindings[i].getPrecision());
            }
        }
    }

    /**
     * Always returns true.
     * 
     * @deprecated since 3.0
     */
    public boolean isRemovingLineBreaks() {
        return true;
    }

    /**
     * @deprecated since 3.0 - does nothing
     */
    public void setRemovingLineBreaks(boolean removingLineBreaks) {

    }

    /**
     * Returns a SQLTemplate for this action.
     */
    public SQLTemplate getQuery() {
        return query;
    }
}
