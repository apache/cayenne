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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.OptimisticLockException;
import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.access.ResultIterator;
import org.apache.cayenne.access.trans.BatchQueryBuilder;
import org.apache.cayenne.access.trans.DeleteBatchQueryBuilder;
import org.apache.cayenne.access.trans.InsertBatchQueryBuilder;
import org.apache.cayenne.access.trans.UpdateBatchQueryBuilder;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.UpdateBatchQuery;

/**
 * @since 1.2
 * @author Andrus Adamchik
 */
public class BatchAction extends BaseSQLAction {

    protected boolean batch;
    protected BatchQuery query;
    protected RowDescriptor keyRowDescriptor;

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
                    QueryLogger.logQueryParameters
                        ("batch bind",
                         query.getDbAttributes(), 
                         queryBuilder.getParameterValues(query),
                         query instanceof InsertBatchQuery);
                }

                queryBuilder.bindParameters(statement, query);
                statement.addBatch();
            }

            // execute the whole batch
            int[] results = statement.executeBatch();
            delegate.nextBatchCount(query, results);

            if (isLoggable) {
                int totalUpdateCount = 0;
                for (int result : results) {

                    // this means Statement.SUCCESS_NO_INFO or Statement.EXECUTE_FAILED
                    if (result < 0) {
                        totalUpdateCount = Statement.SUCCESS_NO_INFO;
                        break;
                    }

                    totalUpdateCount += result;
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
                    QueryLogger.logQueryParameters
                        ("bind", query.getDbAttributes(), queryBuilder.getParameterValues(query), query instanceof InsertBatchQuery);
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
            for (final DbAttribute attr : query.getDbEntity().getGeneratedAttributes()) {
                if (attr.isPrimaryKey()) {
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

        // TODO: andrus, 7/4/2007 - (1) get the type of meaningful PK's from their
        // ObjAttributes; (2) use a different form of Statement.execute -
        // "execute(String,String[])" to be able to map generated column names (this way
        // we can support multiple columns.. although need to check how well this works
        // with most common drivers)

        if (this.keyRowDescriptor == null) {
            // attempt to figure out the right descriptor from the mapping...
            Collection generated = query.getDbEntity().getGeneratedAttributes();
            if (generated.size() == 1) {
                DbAttribute key = (DbAttribute) generated.iterator().next();

                ColumnDescriptor[] columns = new ColumnDescriptor[1];

                // use column name from result set, but type and Java class from DB
                // attribute
                columns[0] = new ColumnDescriptor(keysRS.getMetaData(), 1);
                columns[0].setJdbcType(key.getType());
                columns[0].setJavaClass(TypesMapping.getJavaBySqlType(key.getType()));
                keyRowDescriptor = new RowDescriptor(columns, getAdapter()
                        .getExtendedTypes());
            }
            else {
                keyRowDescriptor = new RowDescriptor(keysRS, getAdapter()
                        .getExtendedTypes());
            }
        }

        ResultIterator iterator = new JDBCResultIterator(
                null,
                null,
                keysRS,
                keyRowDescriptor,
                0);

        observer.nextGeneratedDataRows(query, iterator);
    }
}
