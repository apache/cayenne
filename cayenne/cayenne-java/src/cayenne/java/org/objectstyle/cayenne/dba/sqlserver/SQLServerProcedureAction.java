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
package org.objectstyle.cayenne.dba.sqlserver;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.ResultIterator;
import org.objectstyle.cayenne.access.jdbc.ProcedureAction;
import org.objectstyle.cayenne.access.jdbc.RowDescriptor;
import org.objectstyle.cayenne.access.trans.ProcedureTranslator;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.query.ProcedureQuery;
import org.objectstyle.cayenne.query.Query;

/**
 * ProcedureAction for SQLServer MS JDBC driver. Customizes OUT parameter processing - it
 * has to be done AFTER the ResultSets are read (note that jTDS driver works fine with
 * normal ProcedureAction).
 * <p>
 * <i>See JIRA CAY-251 for details. </i>
 * </p>
 * 
 * @since 1.2
 * @author Andrei Adamchik
 */
public class SQLServerProcedureAction extends ProcedureAction {

    public SQLServerProcedureAction(ProcedureQuery query, DbAdapter adapter,
            EntityResolver entityResolver) {
        super(query, adapter, entityResolver);
    }

    public void performAction(Connection connection, OperationObserver observer)
            throws SQLException, Exception {

        ProcedureTranslator transl = createTranslator(connection);
        CallableStatement statement = (CallableStatement) transl.createStatement();

        try {
            // stored procedure may contain a mixture of update counts and result sets,
            // and out parameters. Read out parameters first, then
            // iterate until we exhaust all results
            boolean hasResultSet = statement.execute();

            // local observer to cache results and provide them to the external observer
            // in the order consistent with other adapters.

            Observer localObserver = new Observer(observer);

            // read query, using local observer

            while (true) {
                if (hasResultSet) {
                    ResultSet rs = statement.getResultSet();
                    try {
                        RowDescriptor descriptor = describeResultSet(
                                rs,
                                processedResultSets++);
                        readResultSet(rs, descriptor, query, localObserver);
                    }
                    finally {
                        try {
                            rs.close();
                        }
                        catch (SQLException ex) {
                        }
                    }
                }
                else {
                    int updateCount = statement.getUpdateCount();
                    if (updateCount == -1) {
                        break;
                    }
                    QueryLogger.logUpdateCount(updateCount);
                    localObserver.nextCount(query, updateCount);
                }

                hasResultSet = statement.getMoreResults();
            }

            // read out parameters to the main observer ... AFTER the main result set
            // TODO: I hope SQLServer does not support ResultSets as OUT parameters,
            // otherwise
            // the order of custom result descriptors will be messed up
            readProcedureOutParameters(statement, observer);

            // add results back to main observer
            localObserver.flushResults(query);
        }
        finally {
            try {
                statement.close();
            }
            catch (SQLException ex) {

            }
        }
    }

    class Observer implements OperationObserver {

        List results;
        List counts;
        OperationObserver observer;

        Observer(OperationObserver observer) {
            this.observer = observer;
        }

        void flushResults(Query query) {
            if (results != null) {
                Iterator it = results.iterator();
                while (it.hasNext()) {
                    observer.nextDataRows(query, (List) it.next());
                }

                results = null;
            }

            if (counts != null) {
                Iterator it = counts.iterator();
                while (it.hasNext()) {
                    observer.nextCount(query, ((Number) it.next()).intValue());
                }

                counts = null;
            }
        }

        public void nextBatchCount(Query query, int[] resultCount) {
            observer.nextBatchCount(query, resultCount);
        }

        public void nextCount(Query query, int resultCount) {
            // does not delegate to wrapped observer
            // but instead caches results locally.
            if (counts == null) {
                counts = new ArrayList();
            }

            counts.add(new Integer(resultCount));
        }

        public void nextDataRows(Query query, List dataRows) {
            // does not delegate to wrapped observer
            // but instead caches results locally.
            if (results == null) {
                results = new ArrayList();
            }

            results.add(dataRows);
        }

        public void nextDataRows(Query q, ResultIterator it) {
            observer.nextDataRows(q, it);
        }

        public void nextGlobalException(Exception ex) {
            observer.nextGlobalException(ex);
        }

        public void nextGeneratedDataRows(Query query, ResultIterator keysIterator) {
            observer.nextGeneratedDataRows(query, keysIterator);
        }

        public void nextQueryException(Query query, Exception ex) {
            observer.nextQueryException(query, ex);
        }

        /**
         * @deprecated Unused since 1.2
         */
        public Level getLoggingLevel() {
            return observer.getLoggingLevel();
        }

        public boolean isIteratedResult() {
            return observer.isIteratedResult();
        }
    }
}