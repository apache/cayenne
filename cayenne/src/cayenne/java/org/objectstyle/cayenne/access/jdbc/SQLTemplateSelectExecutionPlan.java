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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.IteratorUtils;
import org.objectstyle.cayenne.access.DefaultResultIterator;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.util.ResultDescriptor;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.query.SQLTemplate;

/**
 * Implements a stateless strategy for execution of selecting {@link SQLTemplate} queries.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class SQLTemplateSelectExecutionPlan extends SQLTemplateExecutionPlan {

    public SQLTemplateSelectExecutionPlan(DbAdapter adapter) {
        super(adapter);
    }

    /**
     * Runs a SQLTemplate query as a select.
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
        Iterator it =
            (size > 0)
                ? query.parametersIterator()
                : IteratorUtils.singletonIterator(Collections.EMPTY_MAP);
        while (it.hasNext()) {
            Map nextParameters = (Map) it.next();

            SQLSelectStatement compiled =
                templateProcessor.processSelectTemplate(template, nextParameters);

            if (loggable) {
                QueryLogger.logQuery(
                    query.getLoggingLevel(),
                    compiled.getSql(),
                    Arrays.asList(compiled.getBindings()));
            }

            long t1 = System.currentTimeMillis();

            // TODO: we may cache prep statements for this loop, using merged string as a key
            // since it is very likely that it will be the same for multiple parameter sets...
            PreparedStatement statement = connection.prepareStatement(compiled.getSql());

            bind(statement, compiled.getBindings());
            ResultSet rs = statement.executeQuery();

            ResultDescriptor descriptor =
                (compiled.getResultColumns().length > 0)
                    ? ResultDescriptor.createDescriptor(
                        compiled.getResultColumns(),
                        adapter.getExtendedTypes())
                    : ResultDescriptor.createDescriptor(rs, adapter.getExtendedTypes());

            DefaultResultIterator result =
                new DefaultResultIterator(
                    connection,
                    statement,
                    rs,
                    descriptor,
                    query.getFetchLimit());

            if (!observer.isIteratedResult()) {
                // note that we don't need to close ResultIterator
                // since "dataRows" will do it internally
                List resultRows = result.dataRows(true);
                QueryLogger.logSelectCount(
                    query.getLoggingLevel(),
                    resultRows.size(),
                    System.currentTimeMillis() - t1);

                observer.nextDataRows(query, resultRows);
            }
            else {
                try {
                    result.setClosingConnection(true);
                    observer.nextDataRows(query, result);
                }
                catch (Exception ex) {
                    result.close();
                    throw ex;
                }
            }
        }
    }
}
