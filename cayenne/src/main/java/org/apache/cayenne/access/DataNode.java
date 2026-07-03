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
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.BatchTranslator;
import org.apache.cayenne.access.translator.EJBQLTranslator;
import org.apache.cayenne.access.translator.ProcedureTranslator;
import org.apache.cayenne.access.translator.SQLTemplateTranslator;
import org.apache.cayenne.access.translator.SelectTranslator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.log.NoopSqlLogger;
import org.apache.cayenne.log.SqlLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.UpdateBatchQuery;
import org.apache.cayenne.tx.BaseTransaction;
import org.apache.cayenne.tx.Transaction;
import org.apache.cayenne.util.ToStringBuilder;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * An abstraction of a single physical data storage. This is usually a database
 * server, but can potentially be some other storage type like an LDAP server,
 * etc.
 */
public class DataNode {

    protected String name;
    protected DbAdapter adapter;
    protected String dataSourceFactory;
    protected EntityResolver entityResolver;
    protected SchemaUpdateStrategy schemaUpdateStrategy;
    protected Map<String, DataMap> dataMaps;

    private DataSource dataSource;
    private SqlLogger sqlLogger;
    private RowReaderFactory rowReaderFactory;
    private BatchTranslator<InsertBatchQuery> insertBatchTranslator;
    private BatchTranslator<UpdateBatchQuery> updateBatchTranslator;
    private BatchTranslator<DeleteBatchQuery> deleteBatchTranslator;
    private SelectTranslator selectTranslator;
    private ProcedureTranslator procedureTranslator;
    private EJBQLTranslator ejbqlTranslator;
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
        this.sqlLogger = NoopSqlLogger.getInstance();
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
    public SqlLogger getSqlLogger() {
        return sqlLogger;
    }

    /**
     * @since 3.1
     */
    public void setSqlLogger(SqlLogger logger) {
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
     * Returns a name of DataSourceFactory class for this node.
     */
    public String getDataSourceFactory() {
        return dataSourceFactory;
    }

    public void setDataSourceFactory(String dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
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
     * Returns DbAdapter object. This is a plugin that handles RDBMS
     * vendor-specific features.
     */
    public DbAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(DbAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Returns a DataNode that should handle queries for all DataMap components.
     *
     * @since 1.1
     * @deprecated unused and unneeded
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public DataNode lookupDataNode(DataMap dataMap) {
        // we don't know any better than to return ourselves...
        return this;
    }

    /**
     * Runs queries using Connection obtained from internal DataSource.
     *
     * @since 1.1
     */
    public void performQueries(Collection<? extends Query> queries, OperationObserver operationObserver) {

        int listSize = queries.size();
        if (listSize == 0) {
            return;
        }

        if (operationObserver.isIteratedResult() && listSize > 1) {
            throw new CayenneRuntimeException("Iterated queries are not allowed in a batch. Batch size: %d", listSize);
        }


        // do this meaningless inexpensive operation to trigger AutoAdapter lazy initialization before opening a
        // connection. Otherwise, we may end up with two connections open simultaneously, possibly hitting connection
        // pool upper limit.
        getAdapter().getExtendedTypes();

        OperationObserver instrumentedObserver = sqlLogger.isEnabled()
                ? new LoggingObserver(operationObserver, sqlLogger)
                : operationObserver;

        Transaction tx = BaseTransaction.getThreadTransaction();
        Connection connection;

        try {
            connection = dataSource.getConnection();
        } catch (Exception globalEx) {
            if (tx != null) {
                tx.setRollbackOnly();
            }

            instrumentedObserver.nextGlobalException(globalEx);
            return;
        }

        try {
            DataNodeQueryAction queryRunner = new DataNodeQueryAction(this, instrumentedObserver);

            for (Query nextQuery : queries) {

                // catch exceptions for each individual query
                try {
                    queryRunner.runQuery(connection, nextQuery);
                } catch (Exception queryEx) {
                    instrumentedObserver.nextQueryException(nextQuery, queryEx);

                    if (tx != null) {
                        tx.setRollbackOnly();
                    }
                    break;
                }
            }

            instrumentedObserver.afterLastStatement();
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

    /**
     * @since 5.0
     */
    public EJBQLTranslator getEjbqlTranslator() {
        return ejbqlTranslator;
    }

    /**
     * @since 5.0
     */
    public void setEjbqlTranslator(EJBQLTranslator ejbqlTranslator) {
        this.ejbqlTranslator = ejbqlTranslator;
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
