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

package org.apache.cayenne.access;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.conn.PoolManager;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.AshwoodEntitySorter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.query.Query;

/**
 * An abstraction of a single physical data storage. This is usually a database server,
 * but can potentially be some other storage type like an LDAP server, etc.
 */
public class DataNode implements QueryEngine {

    protected String name;
    protected DataSource dataSource;
    protected DbAdapter adapter;
    protected String dataSourceLocation;
    protected String dataSourceFactory;
    protected String schemaUpdateStrategyName;
    protected EntityResolver entityResolver;
    protected SchemaUpdateStrategy schemaUpdateStrategy;

    protected EntitySorter entitySorter;
    protected Map<String, DataMap> dataMaps;

    TransactionDataSource readThroughDataSource;

    /**
     * @since 3.0
     */
    public String getSchemaUpdateStrategyName() {
        if (schemaUpdateStrategyName == null) {
            schemaUpdateStrategyName = SkipSchemaUpdateStrategy.class.getName();
        }
        return schemaUpdateStrategyName;
    }

    /**
     * @since 3.0
     */
    public void setSchemaUpdateStrategyName(String schemaUpdateStrategyName) {
        this.schemaUpdateStrategyName = schemaUpdateStrategyName;
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
        this.dataMaps = new HashMap<String, DataMap>();
        this.readThroughDataSource = new TransactionDataSource();

        // since 1.2 we always implement entity sorting, regardless of the underlying DB
        // as the right order is needed for deferred PK propagation (and maybe other
        // things too?)
        this.entitySorter = new AshwoodEntitySorter(Collections.EMPTY_LIST);
    }

    /**
     * Returns node name. Name is used to uniquely identify DataNode within a DataDomain.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns a location of DataSource of this node. Depending on how this node was
     * created, location is either a JNDI name, or a location of node XML file, etc.
     */
    public String getDataSourceLocation() {
        return dataSourceLocation;
    }

    public void setDataSourceLocation(String dataSourceLocation) {
        this.dataSourceLocation = dataSourceLocation;
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

        entitySorter.setDataMaps(dataMaps);
    }

    /**
     * Adds a DataMap to be handled by this node.
     */
    public void addDataMap(DataMap map) {
        this.dataMaps.put(map.getName(), map);

        entitySorter.setDataMaps(getDataMaps());
    }

    public void removeDataMap(String mapName) {
        DataMap map = dataMaps.remove(mapName);
        if (map != null) {
            entitySorter.setDataMaps(getDataMaps());
        }
    }

    /**
     * Returns DataSource used by this DataNode to obtain connections.
     */
    public DataSource getDataSource() {
        return dataSource != null ? readThroughDataSource : null;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Returns DbAdapter object. This is a plugin that handles RDBMS vendor-specific
     * features.
     */
    public DbAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(DbAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Returns a DataNode that should hanlde queries for all DataMap components.
     * 
     * @since 1.1
     */
    public DataNode lookupDataNode(DataMap dataMap) {
        // we don't know any better than to return ourselves...
        return this;
    }

    /**
     * Runs queries using Connection obtained from internal DataSource.
     * 
     * @since 1.1
     */
    public void performQueries(Collection<Query> queries, OperationObserver callback) {

        int listSize = queries.size();
        if (listSize == 0) {
            return;
        }

        if (callback.isIteratedResult() && listSize > 1) {
            throw new CayenneRuntimeException(
                    "Iterated queries are not allowed in a batch. Batch size: "
                            + listSize);
        }

        QueryLogger.logQueryStart(listSize);

        // do this meaningless inexpensive operation to trigger AutoAdapter lazy
        // initialization before opening a connection. Otherwise we may end up with two
        // connections open simultaneously, possibly hitting connection pool upper limit.
        getAdapter().getExtendedTypes();

        Connection connection = null;

        try {
            connection = this.getDataSource().getConnection();
        }
        catch (Exception globalEx) {
            QueryLogger.logQueryError(globalEx);

            Transaction transaction = Transaction.getThreadTransaction();
            if (transaction != null) {
                transaction.setRollbackOnly();
            }

            callback.nextGlobalException(globalEx);
            return;
        }

        try {
            DataNodeQueryAction queryRunner = new DataNodeQueryAction(this, callback);

            for (Query nextQuery : queries) {

                // catch exceptions for each individual query
                try {
                    queryRunner.runQuery(connection, nextQuery);
                }
                catch (Exception queryEx) {
                    QueryLogger.logQueryError(queryEx);

                    // notify consumer of the exception,
                    // stop running further queries
                    callback.nextQueryException(nextQuery, queryEx);

                    Transaction transaction = Transaction.getThreadTransaction();
                    if (transaction != null) {
                        transaction.setRollbackOnly();
                    }
                    break;
                }
            }
        }
        finally {
            try {
                connection.close();
            }
            catch (SQLException e) {
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
     * Sets EntityResolver. DataNode relies on externally set EntityResolver, so if the
     * node is created outside of DataDomain stack, a valid EntityResolver must be
     * provided explicitly.
     * 
     * @since 1.1
     */
    public void setEntityResolver(org.apache.cayenne.map.EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    /**
     * Returns EntitySorter used by the DataNode.
     */
    public EntitySorter getEntitySorter() {
        return entitySorter;
    }

    /**
     * Sets an EntitySorter that is used to order objects on commit.
     * 
     * @since 1.2
     */
    public void setEntitySorter(EntitySorter entitySorter) {
        this.entitySorter = entitySorter;
    }

    /**
     * Tries to close JDBC connections opened by this node's data source.
     */
    public synchronized void shutdown() {
        try {
            // TODO: theoretically someone maybe using our PoolManager as a container
            // mapped DataSource, so we should use some other logic to determine whether
            // this is a DataNode-managed DS.
            if (dataSource instanceof PoolManager) {
                ((PoolManager) dataSource).dispose();
                dataSource = null;
            }
        }
        catch (SQLException ex) {
        }
    }

    // a read-through DataSource that ensures returning the same connection within
    // transaction.
    final class TransactionDataSource implements DataSource {

        final String CONNECTION_RESOURCE_PREFIX = "DataNode.Connection.";

        public Connection getConnection() throws SQLException {
            if (schemaUpdateStrategy != null) {
                schemaUpdateStrategy.updateSchema(DataNode.this);
            }
            Transaction t = Transaction.getThreadTransaction();
            
            if (t != null) {
                String key = CONNECTION_RESOURCE_PREFIX + name;
                Connection c = t.getConnection(key);

                if (c == null || c.isClosed()) {
                    c = dataSource.getConnection();
                    t.addConnection(key, c);
                }

                // wrap transaction-attached connections in a decorator that prevents them
                // from being closed by callers, as transaction should take care of them
                // on commit or rollback.
                return new TransactionConnectionDecorator(c);
            }

           
            return dataSource.getConnection();
        }

        public Connection getConnection(String username, String password)
                throws SQLException {
            if (schemaUpdateStrategy != null) {
                schemaUpdateStrategy.updateSchema(DataNode.this);
            }
            Transaction t = Transaction.getThreadTransaction();
            if (t != null) {
                String key = CONNECTION_RESOURCE_PREFIX + name;
                Connection c = t.getConnection(key);

                if (c == null || c.isClosed()) {
                    c = dataSource.getConnection();
                    t.addConnection(key, c);
                }

                // wrap transaction-attached connections in a decorator that prevents them
                // from being closed by callers, as transaction should take care of them
                // on commit or rollback.
                return new TransactionConnectionDecorator(c);
            }

            return dataSource.getConnection(username, password);
        }

        public int getLoginTimeout() throws SQLException {
            return dataSource.getLoginTimeout();
        }

        public PrintWriter getLogWriter() throws SQLException {
            return dataSource.getLogWriter();
        }

        public void setLoginTimeout(int seconds) throws SQLException {
            dataSource.setLoginTimeout(seconds);
        }

        public void setLogWriter(PrintWriter out) throws SQLException {
            dataSource.setLogWriter(out);
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new UnsupportedOperationException();
        }
    }
}
