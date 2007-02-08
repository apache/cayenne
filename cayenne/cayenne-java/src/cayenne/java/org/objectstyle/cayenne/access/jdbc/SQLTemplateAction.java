/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.access.jdbc;

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
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.query.SQLAction;
import org.objectstyle.cayenne.query.SQLTemplate;
import org.objectstyle.cayenne.util.Util;

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
        if (counts != null) {
            int[] ints = new int[counts.size()];
            for (int i = 0; i < ints.length; i++) {
                ints[i] = ((Integer) counts.get(i)).intValue();
            }

            callback.nextBatchCount(query, ints);
        }
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
                            if (iteratedResult) {
                                break;
                            }
                            else {
                                resultSet.close();
                            }
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