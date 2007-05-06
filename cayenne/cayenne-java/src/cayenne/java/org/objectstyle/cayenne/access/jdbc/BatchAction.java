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
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.OptimisticLockException;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.ResultIterator;
import org.objectstyle.cayenne.access.trans.BatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.DeleteBatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.InsertBatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.UpdateBatchQueryBuilder;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.DeleteBatchQuery;
import org.objectstyle.cayenne.query.InsertBatchQuery;
import org.objectstyle.cayenne.query.UpdateBatchQuery;

/**
 * @since 1.2
 * @author Andrei Adamchik
 */
public class BatchAction extends BaseSQLAction {

    protected boolean batch;
    protected BatchQuery query;

    public BatchAction(BatchQuery batchQuery, DbAdapter adapter,
            EntityResolver entityResolver) {
        super(adapter, entityResolver);
        this.query = batchQuery;
    }

    public boolean isBatch() {
        return batch;
    }

    public void setBatch(boolean runningAsBatch) {
        this.batch = runningAsBatch;
    }

    public void performAction(Connection connection, OperationObserver observer)
            throws SQLException, Exception {

        BatchQueryBuilder queryBuilder = createBuilder();
        boolean generatesKeys = hasGeneratedKeys();

        if (batch && !generatesKeys) {
            runAsBatch(connection, queryBuilder, observer);
        }
        else {
            runAsIndividualQueries(connection, queryBuilder, observer, generatesKeys);
        }
    }

    protected BatchQueryBuilder createBuilder() throws CayenneException {
        if (query instanceof InsertBatchQuery) {
            return new InsertBatchQueryBuilder(getAdapter());
        }
        else if (query instanceof UpdateBatchQuery) {
            return new UpdateBatchQueryBuilder(getAdapter());
        }
        else if (query instanceof DeleteBatchQuery) {
            return new DeleteBatchQueryBuilder(getAdapter());
        }
        else {
            throw new CayenneException("Unsupported batch query: " + query);
        }
    }

    protected void runAsBatch(
            Connection con,
            BatchQueryBuilder queryBuilder,
            OperationObserver delegate) throws SQLException, Exception {

        String queryStr = queryBuilder.createSqlString(query);
        boolean isLoggable = QueryLogger.isLoggable();

        // log batch SQL execution
        QueryLogger.logQuery(queryStr, Collections.EMPTY_LIST);

        // run batch
        query.reset();

        PreparedStatement statement = con.prepareStatement(queryStr);
        try {
            while (query.next()) {

                if (isLoggable) {
                    QueryLogger.logQueryParameters("batch bind", queryBuilder
                            .getParameterValues(query));
                }

                queryBuilder.bindParameters(statement, query);
                statement.addBatch();
            }

            // execute the whole batch
            int[] results = statement.executeBatch();
            delegate.nextBatchCount(query, results);

            if (isLoggable) {
                int totalUpdateCount = 0;
                for (int i = 0; i < results.length; i++) {

                    // this means Statement.SUCCESS_NO_INFO or Statement.EXECUTE_FAILED
                    if (results[i] < 0) {
                        totalUpdateCount = Statement.SUCCESS_NO_INFO;
                        break;
                    }

                    totalUpdateCount += results[i];
                }

                QueryLogger.logUpdateCount(totalUpdateCount);
            }
        }
        finally {
            try {
                statement.close();
            }
            catch (Exception e) {
            }
        }
    }

    /**
     * Executes batch as individual queries over the same prepared statement.
     */
    protected void runAsIndividualQueries(
            Connection connection,
            BatchQueryBuilder queryBuilder,
            OperationObserver delegate,
            boolean generatesKeys) throws SQLException, Exception {

        boolean isLoggable = QueryLogger.isLoggable();
        boolean useOptimisticLock = query.isUsingOptimisticLocking();

        String queryStr = queryBuilder.createSqlString(query);

        // log batch SQL execution
        QueryLogger.logQuery(queryStr, Collections.EMPTY_LIST);

        // run batch queries one by one
        query.reset();

        PreparedStatement statement = (generatesKeys) ? connection.prepareStatement(
                queryStr,
                Statement.RETURN_GENERATED_KEYS) : connection.prepareStatement(queryStr);
        try {
            while (query.next()) {
                if (isLoggable) {
                    QueryLogger.logQueryParameters("bind", queryBuilder
                            .getParameterValues(query));
                }

                queryBuilder.bindParameters(statement, query);

                int updated = statement.executeUpdate();
                if (useOptimisticLock && updated != 1) {

                    Map snapshot = Collections.EMPTY_MAP;
                    if (query instanceof UpdateBatchQuery) {
                        snapshot = ((UpdateBatchQuery) query).getCurrentQualifier();
                    }
                    else if (query instanceof DeleteBatchQuery) {
                        snapshot = ((DeleteBatchQuery) query).getCurrentQualifier();
                    }

                    throw new OptimisticLockException(
                            query.getDbEntity(),
                            queryStr,
                            snapshot);
                }

                delegate.nextCount(query, updated);

                if (generatesKeys) {
                    processGeneratedKeys(statement, delegate);
                }

                if (isLoggable) {
                    QueryLogger.logUpdateCount(updated);
                }
            }
        }
        finally {
            try {
                statement.close();
            }
            catch (Exception e) {
            }
        }
    }

    /**
     * Returns whether BatchQuery generates any keys.
     */
    protected boolean hasGeneratedKeys() {
        // see if we are configured to support generated keys
        if (!adapter.supportsGeneratedKeys()) {
            return false;
        }

        // see if the query needs them
        if (query instanceof InsertBatchQuery) {

            // see if any of the generated attributes is PK
            Iterator attributes = query.getDbEntity().getGeneratedAttributes().iterator();
            while (attributes.hasNext()) {
                if (((DbAttribute) attributes.next()).isPrimaryKey()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Implements generated keys extraction supported in JDBC 3.0 specification.
     */
    protected void processGeneratedKeys(Statement statement, OperationObserver observer)
            throws SQLException, CayenneException {

        ResultSet keysRS = statement.getGeneratedKeys();
        RowDescriptor descriptor = new RowDescriptor(keysRS, getAdapter()
                .getExtendedTypes());
        ResultIterator iterator = new JDBCResultIterator(
                null,
                null,
                keysRS,
                descriptor,
                0);

        observer.nextGeneratedDataRows(query, iterator);
    }
}