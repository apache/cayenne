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
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.BatchTranslator;
import org.apache.cayenne.access.translator.ProcedureTranslator;
import org.apache.cayenne.access.translator.SQLTemplateTranslator;
import org.apache.cayenne.access.translator.SelectTranslator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.DbAdapterDetector;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.log.NoopSQLLogger;
import org.apache.cayenne.log.SQLLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntitySorter;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * An abstraction of a single physical data storage. This is usually a database
 * server, but can potentially be some other storage type like an LDAP server,
 * etc.
 */
public class DataNode {

    protected String name;

    // may be detected lazily, as detecting the DB type requires DB access, and the stack must be able to start when
    // the DB is not available
    protected volatile DbAdapter adapter;
    private List<DbAdapterDetector> adapterDetectors;
    // when not installed by the user, created lazily from the adapter, as this may require resolving the adapter first
    private volatile PkGenerator pkGenerator;
    // tells whether "pkGenerator" was installed by the user, and hence must not be replaced when the adapter changes
    private boolean customPkGenerator;

    protected EntityResolver entityResolver;
    protected EntitySorter entitySorter;
    protected SchemaUpdateStrategy schemaUpdateStrategy;
    protected Map<String, DataMap> dataMaps;

    private TransactionDataSource dataSource;
    private SQLLogger sqlLogger;
    private RowReaderFactory rowReaderFactory;
    private BatchTranslator<InsertBatchQuery> insertBatchTranslator;
    private BatchTranslator<UpdateBatchQuery> updateBatchTranslator;
    private BatchTranslator<DeleteBatchQuery> deleteBatchTranslator;
    private SelectTranslator selectTranslator;
    private ProcedureTranslator procedureTranslator;
    private SQLTemplateTranslator sqlTemplateTranslator;

    /**
     * Creates a new unnamed DataNode.
     */
    public DataNode() {
        this(null);
    }

    /**
     * Creates a new DataNode, assigning it a name.
     */
    public DataNode(String name) {

        this.name = name;
        this.dataMaps = new HashMap<>();

        // make sure logger is not null
        this.sqlLogger = NoopSQLLogger.getInstance();
    }

    /**
     * @since 3.0
     */
    public SchemaUpdateStrategy getSchemaUpdateStrategy() {
        return schemaUpdateStrategy;
    }

    /**
     * @since 3.0
     */
    public void setSchemaUpdateStrategy(SchemaUpdateStrategy schemaUpdateStrategy) {
        this.schemaUpdateStrategy = schemaUpdateStrategy;
    }

    /**
     * @since 3.1
     */
    public SQLLogger getSqlLogger() {
        return sqlLogger;
    }

    /**
     * @since 3.1
     */
    public void setSQLLogger(SQLLogger logger) {
        this.sqlLogger = logger;
    }

    /**
     * Returns node name. Name is used to uniquely identify DataNode within a
     * DataDomain.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns an unmodifiable collection of DataMaps handled by this DataNode.
     */
    public Collection<DataMap> getDataMaps() {
        return Collections.unmodifiableCollection(dataMaps.values());
    }

    /**
     * Returns datamap with specified name, null if none present
     */
    public DataMap getDataMap(String name) {
        return dataMaps.get(name);
    }

    public void setDataMaps(Collection<DataMap> dataMaps) {
        for (DataMap map : dataMaps) {
            this.dataMaps.put(map.getName(), map);
        }
    }

    /**
     * Adds a DataMap to be handled by this node.
     */
    public void addDataMap(DataMap map) {
        this.dataMaps.put(map.getName(), map);
    }

    public void removeDataMap(DataMap map) {
        removeDataMap(map.getName());
    }

    public void removeDataMap(String mapName) {
        dataMaps.remove(mapName);
    }

    /**
     * Returns DataSource used by this DataNode to obtain connections.
     */
    public DataSource getDataSource() {
        // return the read-through wrapper, not the raw pool, so callers participate in schema
        // updates and transaction connection sharing
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = new TransactionDataSource(this, dataSource);
    }

    /**
     * Returns DbAdapter object. This is a plugin that handles RDBMS vendor-specific features. When no adapter was set
     * explicitly, it is detected off the node's DataSource with the {@link #setAdapterDetectors(List) detectors} on
     * first access, and cached. So this call may access the DB and may fail if the DB is not available.
     */
    public DbAdapter getAdapter() {
        DbAdapter resolved = adapter;
        if (resolved == null && adapterDetectors != null) {
            synchronized (this) {
                resolved = adapter;
                if (resolved == null) {
                    resolved = detectAdapter();
                    adapter = resolved;
                }
            }
        }

        return resolved;
    }

    private DbAdapter detectAdapter() {
        if (dataSource == null) {
            throw new CayenneRuntimeException("Can't detect the adapter of DataNode '%s': no DataSource", name);
        }

        // detect off the raw DataSource, not off the transaction-aware wrapper: the wrapper runs the schema update
        // strategy, which itself needs the adapter, and the detection connection must not join the current transaction
        try (Connection c = dataSource.dataSource.getConnection()) {
            return DbAdapterDetector.detect(c.getMetaData(), adapterDetectors);
        } catch (SQLException e) {
            throw new CayenneRuntimeException("Error detecting database type: " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Installs the detectors to run once against the node's DataSource, on first {@link #getAdapter()} call. This
     * defers DB type detection until the adapter is actually needed, allowing the stack to start when the DB is not
     * available. An adapter set explicitly via {@link #setAdapter(DbAdapter)} takes precedence.
     *
     * @since 6.0
     */
    public void setAdapterDetectors(List<DbAdapterDetector> adapterDetectors) {
        this.adapterDetectors = adapterDetectors;
    }

    public void setAdapter(DbAdapter adapter) {
        this.adapter = adapter;

        // a generator built for the old adapter is meaningless for the new one, so reset it to be lazily recreated
        // on demand. A generator explicitly installed by the user is left alone.
        if (!customPkGenerator) {
            this.pkGenerator = null;
        }
    }

    /**
     * Returns the PkGenerator used by this node to generate primary keys. Unless an explicit generator was installed
     * with {@link #setPkGenerator(PkGenerator)}, this is the default generator of the node's {@link DbAdapter}.
     *
     * @since 5.0
     */
    public PkGenerator getPkGenerator() {

        // not creating the default generator until it is needed, as resolving the adapter may open a DB connection,
        // and we must allow the stack to start when the DB is not available
        if (pkGenerator == null) {
            synchronized (this) {
                if (pkGenerator == null) {
                    DbAdapter adapter = getAdapter();
                    if (adapter != null) {
                        pkGenerator = adapter.createPkGenerator();
                    }
                }
            }
        }

        return pkGenerator;
    }

    /**
     * Installs a custom PkGenerator, overriding the default generator of the node's {@link DbAdapter}. A custom
     * generator is retained across {@link #setAdapter(DbAdapter)} calls. Passing null restores the adapter default.
     *
     * @since 5.0
     */
    public void setPkGenerator(PkGenerator pkGenerator) {
        if (pkGenerator != null) {
            this.pkGenerator = pkGenerator;
            this.customPkGenerator = true;
        } else {
            this.customPkGenerator = false;
            this.pkGenerator = null;
        }
    }

    /**
     * Executes the statements, in order, on a single Connection obtained from the internal DataSource. The SQL action
     * of each statement fills its context with the translated statement and the results. A failure to obtain a
     * connection is thrown as a CayenneRuntimeException; a failure of a statement is thrown as a
     * {@link CayenneSqlException} carrying its context, unless the failure is a CayenneRuntimeException in its own
     * right (e.g. an {@link OptimisticLockException}), which is rethrown as is. The statements after a failed one are
     * not executed.
     *
     * @param statementContexts the statements to execute, normally the ones routed to this node within an operation,
     *                          i.e. this node's entry in {@link OperationContext#getStatementContexts()}.
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

        // resolve the adapter before opening a connection, as a lazy adapter detection opens a connection of its own.
        // Otherwise, we may end up with two connections open simultaneously, possibly hitting connection pool upper
        // limit.
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

    /**
     * Returns EntityResolver that handles DataMaps of this node.
     */
    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    /**
     * Sets EntityResolver. DataNode relies on externally set EntityResolver, so
     * if the node is created outside of DataDomain stack, a valid
     * EntityResolver must be provided explicitly.
     *
     * @since 1.1
     */
    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    /**
     * Returns an EntitySorter that sorts the entities of this node based on their mutual dependencies.
     *
     * @since 5.0
     */
    public EntitySorter getEntitySorter() {
        return entitySorter;
    }

    /**
     * Sets EntitySorter. DataNode relies on an externally set sorter, so if the node is created outside of the
     * DataDomain stack, a valid EntitySorter must be provided explicitly.
     *
     * @since 5.0
     */
    public void setEntitySorter(EntitySorter entitySorter) {
        this.entitySorter = entitySorter;
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
     * @since 4.0
     */
    public void setRowReaderFactory(RowReaderFactory rowReaderFactory) {
        this.rowReaderFactory = rowReaderFactory;
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
    public void setInsertBatchTranslator(BatchTranslator<InsertBatchQuery> insertBatchTranslator) {
        this.insertBatchTranslator = insertBatchTranslator;
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
    public void setUpdateBatchTranslator(BatchTranslator<UpdateBatchQuery> updateBatchTranslator) {
        this.updateBatchTranslator = updateBatchTranslator;
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
    public void setDeleteBatchTranslator(BatchTranslator<DeleteBatchQuery> deleteBatchTranslator) {
        this.deleteBatchTranslator = deleteBatchTranslator;
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
    public void setSqlTemplateTranslator(SQLTemplateTranslator sqlTemplateTranslator) {
        this.sqlTemplateTranslator = sqlTemplateTranslator;
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
    public void setSelectTranslator(SelectTranslator selectTranslator) {
        this.selectTranslator = selectTranslator;
    }

    /**
     * @since 5.0
     */
    public ProcedureTranslator getProcedureTranslator() {
        return procedureTranslator;
    }

    /**
     * @since 5.0
     */
    public void setProcedureTranslator(ProcedureTranslator procedureTranslator) {
        this.procedureTranslator = procedureTranslator;
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
            // read the strategy from the node, as it may be set after this data source is created
            SchemaUpdateStrategy schemaUpdateStrategy = dataNode.getSchemaUpdateStrategy();
            if (schemaUpdateStrategy != null) {
                schemaUpdateStrategy.updateSchema(dataNode);
            }

            Transaction t = BaseTransaction.getThreadTransaction();
            return (t != null)
                    ? t.getOrCreateConnection(CONNECTION_RESOURCE_PREFIX + dataNode.getName(), dataSource)
                    : dataSource.getConnection();
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            // read the strategy from the node, as it may be set after this data source is created
            SchemaUpdateStrategy schemaUpdateStrategy = dataNode.getSchemaUpdateStrategy();
            if (schemaUpdateStrategy != null) {
                schemaUpdateStrategy.updateSchema(dataNode);
            }

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
