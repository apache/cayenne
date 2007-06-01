/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.IteratorUtils;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.query.SQLTemplate;

/**
 * Implements a stateless strategy for execution of updating {@link SQLTemplate} 
 * queries.
 * 
 * @author Andrei Adamchik
 * @since 1.1
 */
// TODO: it is very likely that there will be an ExecutionPlan interface 
// soon, and all query types will be run by an execution plan instead of
// a DataNode. Then ExecutionPlan will become a true Strategy pattern...
public class SQLTemplateExecutionPlan {

    protected DbAdapter adapter;

    public SQLTemplateExecutionPlan(DbAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Returns DbAdapter associated with this execution plan object.
     */
    public DbAdapter getAdapter() {
        return adapter;
    }

    /**
     * Runs a SQLTemplate query as an update.
     */
    public void execute(
        Connection connection,
        SQLTemplate query,
        OperationObserver observer)
        throws SQLException, Exception {

        SQLTemplateProcessor templateProcessor = new SQLTemplateProcessor();
        String template = query.getTemplate(adapter.getClass().getName());

        boolean loggable = QueryLogger.isLoggable(query.getLoggingLevel());
        int size = query.parametersSize();

        // zero size indicates a one-shot query with no parameters
        // so fake a single entry batch...
        int[] counts = new int[size > 0 ? size : 1];
        Iterator it =
            (size > 0)
                ? query.parametersIterator()
                : IteratorUtils.singletonIterator(Collections.EMPTY_MAP);
        for (int i = 0; i < counts.length; i++) {
            Map nextParameters = (Map) it.next();

            SQLStatement compiled =
                templateProcessor.processTemplate(template, nextParameters);

            if (loggable) {
                QueryLogger.logQuery(
                    query.getLoggingLevel(),
                    compiled.getSql(),
                    Arrays.asList(compiled.getBindings()));
            }

            // TODO: we may cache prep statements for this loop, using merged string as a key
            // since it is very likely that it will be the same for multiple parameter sets...
            PreparedStatement statement = connection.prepareStatement(compiled.getSql());
            try {
                bind(statement, compiled.getBindings());
                counts[i] = statement.executeUpdate();
                QueryLogger.logUpdateCount(query.getLoggingLevel(), counts[i]);
            }
            finally {
                statement.close();
            }
        }

        observer.nextBatchCount(query, counts);
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
}
