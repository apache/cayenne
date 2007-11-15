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

package org.apache.cayenne.dba.sqlserver;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.access.ResultIterator;
import org.apache.cayenne.access.jdbc.ProcedureAction;
import org.apache.cayenne.access.jdbc.RowDescriptor;
import org.apache.cayenne.access.trans.ProcedureTranslator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.Query;

/**
 * ProcedureAction for SQLServer MS JDBC driver. Customizes OUT parameter processing - it
 * has to be done AFTER the ResultSets are read (note that jTDS driver works fine with
 * normal ProcedureAction).
 * <p>
 * <i>See JIRA CAY-251 for details. </i>
 * </p>
 * 
 * @since 1.2
 * @author Andrus Adamchik
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

        public boolean isIteratedResult() {
            return observer.isIteratedResult();
        }
    }
}
