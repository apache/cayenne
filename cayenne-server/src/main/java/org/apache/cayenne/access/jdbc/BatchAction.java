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
import java.util.List;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.OptimisticLockException;
import org.apache.cayenne.access.jdbc.reader.RowReader;
import org.apache.cayenne.access.translator.batch.BatchParameterBinding;
import org.apache.cayenne.access.translator.batch.BatchTranslator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.BatchQueryRow;
import org.apache.cayenne.query.InsertBatchQuery;

/**
 * @since 1.2
 */
public class BatchAction extends BaseSQLAction {

    protected boolean runningAsBatch;
    protected BatchQuery query;
    protected RowDescriptor keyRowDescriptor;

    private static void bind(DbAdapter adapter, PreparedStatement statement, List<BatchParameterBinding> bindings)
            throws SQLException, Exception {
        int len = bindings.size();
        for (int i = 0; i < len; i++) {
            BatchParameterBinding b = bindings.get(i);
            adapter.bindParameter(statement, b.getValue(), i + 1, b.getAttribute().getType(), b.getAttribute()
                    .getScale());
        }
    }

    /**
     * @since 3.2
     */
    public BatchAction(BatchQuery query, DataNode dataNode, boolean runningAsBatch) {
        super(dataNode);
        this.query = query;
        this.runningAsBatch = runningAsBatch;
    }

    /**
     * @return Query which originated this action
     */
    public BatchQuery getQuery() {
        return query;
    }

    @Override
    public void performAction(Connection connection, OperationObserver observer) throws SQLException, Exception {

        BatchTranslator translator = createTranslator();
        boolean generatesKeys = hasGeneratedKeys();

        if (runningAsBatch && !generatesKeys) {
            runAsBatch(connection, translator, observer);
        } else {
            runAsIndividualQueries(connection, translator, observer, generatesKeys);
        }
    }

    protected BatchTranslator createTranslator() throws CayenneException {
        return dataNode.batchTranslator(query, null);
    }

    protected void runAsBatch(Connection con, BatchTranslator translator, OperationObserver delegate)
            throws SQLException, Exception {

        String queryStr = translator.createSqlString();
        JdbcEventLogger logger = dataNode.getJdbcEventLogger();
        boolean isLoggable = logger.isLoggable();

        // log batch SQL execution
        logger.logQuery(queryStr, Collections.EMPTY_LIST);

        // run batch

        DbAdapter adapter = dataNode.getAdapter();
        PreparedStatement statement = con.prepareStatement(queryStr);
        try {
            for (BatchQueryRow row : query.getRows()) {

                List<BatchParameterBinding> bindings = translator.createBindings(row);
                logger.logQueryParameters("batch bind", bindings);
                bind(adapter, statement, bindings);

                statement.addBatch();
            }

            // execute the whole batch
            int[] results = statement.executeBatch();
            delegate.nextBatchCount(query, results);

            if (isLoggable) {
                int totalUpdateCount = 0;
                for (int result : results) {

                    // this means Statement.SUCCESS_NO_INFO or
                    // Statement.EXECUTE_FAILED
                    if (result < 0) {
                        totalUpdateCount = Statement.SUCCESS_NO_INFO;
                        break;
                    }

                    totalUpdateCount += result;
                }

                logger.logUpdateCount(totalUpdateCount);
            }
        } finally {
            try {
                statement.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Executes batch as individual queries over the same prepared statement.
     */
    protected void runAsIndividualQueries(Connection connection, BatchTranslator translator,
            OperationObserver delegate, boolean generatesKeys) throws SQLException, Exception {

        JdbcEventLogger logger = dataNode.getJdbcEventLogger();
        boolean useOptimisticLock = query.isUsingOptimisticLocking();

        String queryStr = translator.createSqlString();

        // log batch SQL execution
        logger.logQuery(queryStr, Collections.EMPTY_LIST);

        // run batch queries one by one

        DbAdapter adapter = dataNode.getAdapter();
        PreparedStatement statement = (generatesKeys) ? connection.prepareStatement(queryStr,
                Statement.RETURN_GENERATED_KEYS) : connection.prepareStatement(queryStr);
        try {
            for (BatchQueryRow row : query.getRows()) {

                List<BatchParameterBinding> bindings = translator.createBindings(row);
                logger.logQueryParameters("bind", bindings);

                bind(adapter, statement, bindings);

                int updated = statement.executeUpdate();
                if (useOptimisticLock && updated != 1) {
                    throw new OptimisticLockException(row.getObjectId(), query.getDbEntity(), queryStr,
                            row.getQualifier());
                }

                delegate.nextCount(query, updated);

                if (generatesKeys) {
                    processGeneratedKeys(statement, delegate, row);
                }

                logger.logUpdateCount(updated);
            }
        } finally {
            try {
                statement.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Returns whether BatchQuery generates any keys.
     */
    protected boolean hasGeneratedKeys() {
        // see if we are configured to support generated keys
        if (!dataNode.getAdapter().supportsGeneratedKeys()) {
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
     * 
     * @since 3.2
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void processGeneratedKeys(Statement statement, OperationObserver observer, BatchQueryRow row)
            throws SQLException, CayenneException {

        ResultSet keysRS = statement.getGeneratedKeys();

        // TODO: andrus, 7/4/2007 - (1) get the type of meaningful PK's from
        // their
        // ObjAttributes; (2) use a different form of Statement.execute -
        // "execute(String,String[])" to be able to map generated column names
        // (this way
        // we can support multiple columns.. although need to check how well
        // this works
        // with most common drivers)

        RowDescriptorBuilder builder = new RowDescriptorBuilder();

        if (this.keyRowDescriptor == null) {
            // attempt to figure out the right descriptor from the mapping...
            Collection<DbAttribute> generated = query.getDbEntity().getGeneratedAttributes();
            if (generated.size() == 1) {
                DbAttribute key = generated.iterator().next();

                ColumnDescriptor[] columns = new ColumnDescriptor[1];

                // use column name from result set, but type and Java class from
                // DB
                // attribute
                columns[0] = new ColumnDescriptor(keysRS.getMetaData(), 1);
                columns[0].setJdbcType(key.getType());
                columns[0].setJavaClass(TypesMapping.getJavaBySqlType(key.getType()));
                builder.setColumns(columns);
            } else {
                builder.setResultSet(keysRS);
            }

            this.keyRowDescriptor = builder.getDescriptor(dataNode.getAdapter().getExtendedTypes());
        }

        RowReader<?> rowReader = dataNode.rowReader(keyRowDescriptor, query.getMetaData(dataNode.getEntityResolver()),
                Collections.<ObjAttribute, ColumnDescriptor> emptyMap());
        ResultIterator iterator = new JDBCResultIterator(null, keysRS, rowReader);

        observer.nextGeneratedRows(query, iterator, row.getObjectId());
    }
}
