/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.CayenneSqlException;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.BatchTranslator;
import org.apache.cayenne.access.translator.ProcedureTranslator;
import org.apache.cayenne.access.translator.SQLTemplateTranslator;
import org.apache.cayenne.access.translator.SelectTranslator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.log.NoopSQLLogger;
import org.apache.cayenne.log.SQLLogger;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.UpdateBatchQuery;
import org.apache.cayenne.tx.BaseTransaction;
import org.apache.cayenne.tx.Transaction;
import org.apache.cayenne.util.ToStringBuilder;
import org.apache.cayenne.util.Util;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

/**
 * An abstraction of a single physical data storage. This is usually a database
 * server, but can potentially be some other storage type like an LDAP server,
 * etc.
 */
public class DataNode {

    private final String name;
    private final TransactionDataSource dataSource;

    // resolved lazily by the initializer on first use, as this may require DB access, and the stack must be able to
    // start when the DB is not available
    private volatile DbAdapter adapter;
    private DataNodeInitializer initializer;

    // the custom generator from the constructor, or else the default generator of the adapter, created when the
    // adapter is resolved
    private volatile PkGenerator pkGenerator;

    private final SQLLogger sqlLogger;
    private final RowReaderFactory rowReaderFactory;
    private final SelectTranslator selectTranslator;
    private final ProcedureTranslator procedureTranslator;
    private final SQLTemplateTranslator sqlTemplateTranslator;
    private final BatchTranslator<InsertBatchQuery> insertBatchTranslator;
    private final BatchTranslator<UpdateBatchQuery> updateBatchTranslator;
    private final BatchTranslator<DeleteBatchQuery> deleteBatchTranslator;

    /**
     * Creates a new DataNode. A node is immutable once created: everything it needs is passed here, and the only
     * state resolved later is the adapter, with its default PkGenerator unless a custom one is given, on the first
     * use of the node.
     *
     * @since 6.0
     */
    public DataNode(
            String name,
            DataSource dataSource,
            DataNodeInitializer initializer,
            PkGenerator pkGenerator,
            SQLLogger sqlLogger,
            RowReaderFactory rowReaderFactory,
            SelectTranslator selectTranslator,
            ProcedureTranslator procedureTranslator,
            SQLTemplateTranslator sqlTemplateTranslator,
            BatchTranslator<InsertBatchQuery> insertBatchTranslator,
            BatchTranslator<UpdateBatchQuery> updateBatchTranslator,
            BatchTranslator<DeleteBatchQuery> deleteBatchTranslator) {

        this.name = name;
        this.dataSource = dataSource != null ? new TransactionDataSource(this, dataSource) : null;
        this.initializer = initializer;
        this.pkGenerator = pkGenerator;

        this.sqlLogger = sqlLogger != null ? sqlLogger : NoopSQLLogger.getInstance();
        this.rowReaderFactory = rowReaderFactory;
        this.selectTranslator = selectTranslator;
        this.procedureTranslator = procedureTranslator;
        this.sqlTemplateTranslator = sqlTemplateTranslator;
        this.insertBatchTranslator = insertBatchTranslator;
        this.updateBatchTranslator = updateBatchTranslator;
        this.deleteBatchTranslator = deleteBatchTranslator;
    }

    /**
     * Returns node name. Name is used to uniquely identify DataNode within a DataDomain.
     */
    public String getName() {
        return name;
    }

    /**
     * @since 3.1
     */
    public SQLLogger getSqlLogger() {
        return sqlLogger;
    }

    /**
     * Returns DataSource used by this DataNode to obtain connections.
     */
    public DataSource getDataSource() {
        // return the transaction-aware wrapper, not the raw pool, so callers share the connection of the current
        // transaction
        return dataSource;
    }

    /**
     * Returns DbAdapter object. This is a plugin that handles RDBMS vendor-specific features. The adapter is resolved
     * by the node's {@link DataNodeInitializer} on first access, and cached. So this call may access the DB and may
     * fail if the DB is not available.
     */
    public DbAdapter getAdapter() {
        DbAdapter resolved = adapter;
        if (resolved == null) {
            synchronized (this) {
                // the initializer is read under the lock, as it is cleared once it has run
                resolved = adapter;
                if (resolved == null && initializer != null) {
                    resolved = initializer.init();
                    if (resolved == null) {
                        throw new CayenneRuntimeException("Initializer of DataNode '%s' returned no DbAdapter", name);
                    }

                    // "adapter" is the publication flag, so the generator must be in place before it is set
                    if (pkGenerator == null) {
                        pkGenerator = resolved.createPkGenerator();
                    }
                    adapter = resolved;

                    // the initializer has done its job, release whatever it holds on to
                    initializer = null;
                }
            }
        }

        return resolved;
    }

    /**
     * Returns the PkGenerator used by this node to generate primary keys: the custom generator the node was created
     * with, or else the default generator of the node's {@link DbAdapter}.
     *
     * @since 5.0
     */
    public PkGenerator getPkGenerator() {

        // the default generator is created together with the adapter, which may still need to be resolved
        if (pkGenerator == null) {
            getAdapter();
        }

        return pkGenerator;
    }

    /**
     * Executes the statements, in order, on a single Connection obtained from the internal DataSource. The SQL action
     * of each statement fills its context with the translated statement and the results.
     *
     * @since 6.0
     */
    public void execute(List<StatementContext> statementContexts) {
        if (statementContexts.isEmpty()) {
            return;
        }

        if (statementContexts.size() > 1 && statementContexts.getFirst().isIteratedResult()) {
            throw new CayenneRuntimeException("Iterated queries are not allowed in a batch. Batch size: %d",
                    statementContexts.size());
        }

        // initialize the node before opening a connection, as the lazy initialization (adapter detection, schema
        // update) opens connections of its own. Otherwise, we may end up with two connections open simultaneously,
        // possibly hitting connection pool upper limit.
        DbAdapter adapter = getAdapter();

        Transaction tx = BaseTransaction.getThreadTransaction();

        Connection connection;
        try {
            connection = getDataSource().getConnection();
        } catch (Exception e) {
            if (tx != null) {
                tx.setRollbackOnly();
            }
            throw new CayenneRuntimeException("Global exception.", Util.unwindException(e));
        }

        SQLLogger logger = getSqlLogger();
        try {
            for (StatementContext statementContext : statementContexts) {
                statementContext.setLogger(logger);
                try {
                    adapter.getAction(statementContext.getQuery(), this).performAction(connection, statementContext);
                    statementContext.logComplete();
                } catch (Exception e) {
                    statementContext.logError(e);

                    // preserve meaningful Cayenne exceptions, even when they wrap a lower-level cause; only wrap raw
                    // lower-level failures in a CayenneSqlException so they can be correlated with the failing
                    // statement
                    Throwable unwound = Util.unwindException(e, CayenneRuntimeException.class);
                    if (unwound instanceof CayenneRuntimeException cayenneException) {
                        throw cayenneException;
                    }
                    throw new CayenneSqlException("Query exception.", statementContext, unwound);
                }
            }
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                // ignore closing exceptions...
            }
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("name", getName()).toString();
    }

    /**
     * @since 4.0
     */
    public RowReaderFactory getRowReaderFactory() {
        return rowReaderFactory;
    }

    /**
     * @since 5.0
     */
    public BatchTranslator<InsertBatchQuery> getInsertBatchTranslator() {
        return insertBatchTranslator;
    }

    /**
     * @since 5.0
     */
    public BatchTranslator<UpdateBatchQuery> getUpdateBatchTranslator() {
        return updateBatchTranslator;
    }

    /**
     * @since 5.0
     */
    public BatchTranslator<DeleteBatchQuery> getDeleteBatchTranslator() {
        return deleteBatchTranslator;
    }

    /**
     * @since 5.0
     */
    public SQLTemplateTranslator getSqlTemplateTranslator() {
        return sqlTemplateTranslator;
    }

    /**
     * @since 5.0
     */
    public SelectTranslator getSelectTranslator() {
        return selectTranslator;
    }

    /**
     * @since 5.0
     */
    public ProcedureTranslator getProcedureTranslator() {
        return procedureTranslator;
    }

    // a read-through DataSource that ensures returning the same connection
    // within transaction.
    static class TransactionDataSource implements DataSource {

        final String CONNECTION_RESOURCE_PREFIX = "DataNode.Connection.";

        private final DataNode dataNode;
        private final DataSource dataSource;

        public TransactionDataSource(DataNode dataNode, DataSource dataSource) {
            this.dataNode = dataNode;
            this.dataSource = dataSource;
        }

        @Override
        public Connection getConnection() throws SQLException {
            Transaction t = BaseTransaction.getThreadTransaction();
            return (t != null)
                    ? t.getOrCreateConnection(CONNECTION_RESOURCE_PREFIX + dataNode.getName(), dataSource)
                    : dataSource.getConnection();
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            Transaction t = BaseTransaction.getThreadTransaction();
            return (t != null)
                    ? t.getOrCreateConnection(CONNECTION_RESOURCE_PREFIX + dataNode.getName(), dataSource)
                    : dataSource.getConnection(username, password);
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return dataSource.getLoginTimeout();
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return dataSource.getLogWriter();
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            dataSource.setLoginTimeout(seconds);
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
            dataSource.setLogWriter(out);
        }

        /**
         * @since 3.0
         */
        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return iface.isAssignableFrom(dataSource.getClass());
        }

        /**
         * @since 3.0
         */
        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            try {
                return iface.cast(dataSource);
            } catch (ClassCastException e) {
                throw new SQLException("Not a DataSource: " + e.getMessage());
            }
        }

        /**
         * @since 3.1
         */
        @Override
        public Logger getParentLogger() {
            // don't throw SQLFeatureNotSupported - this will break JDK 1.5 runtime
            throw new UnsupportedOperationException();
        }
    }
}
