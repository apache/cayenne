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
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.jdbc.RowDescriptor;
import org.apache.cayenne.access.jdbc.SQLTemplateProcessor;
import org.apache.cayenne.access.jdbc.reader.RowReader;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.batch.BatchTranslator;
import org.apache.cayenne.access.translator.batch.BatchTranslatorFactory;
import org.apache.cayenne.access.translator.select.SelectTranslator;
import org.apache.cayenne.access.translator.select.SelectTranslatorFactory;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.log.NoopJdbcEventLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.Select;
import org.apache.cayenne.tx.BaseTransaction;
import org.apache.cayenne.tx.Transaction;
import org.apache.cayenne.util.ToStringBuilder;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
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
public class DataNode implements QueryEngine {

	protected String name;
	protected DataSource dataSource;
	protected DbAdapter adapter;
	protected String dataSourceFactory;
	protected EntityResolver entityResolver;
	protected SchemaUpdateStrategy schemaUpdateStrategy;
	protected Map<String, DataMap> dataMaps;

	private JdbcEventLogger jdbcEventLogger;
	private RowReaderFactory rowReaderFactory;
	private BatchTranslatorFactory batchTranslatorFactory;
	private SelectTranslatorFactory selectTranslatorFactory;
	private SQLTemplateProcessor sqlTemplateProcessor;

	TransactionDataSource readThroughDataSource;

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
		this.readThroughDataSource = new TransactionDataSource();

		// make sure logger is not null
		this.jdbcEventLogger = NoopJdbcEventLogger.getInstance();
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
	public JdbcEventLogger getJdbcEventLogger() {
		if (jdbcEventLogger == null && adapter instanceof JdbcAdapter) {
			jdbcEventLogger = ((JdbcAdapter) adapter).getJdbcEventLogger();
		}

		return jdbcEventLogger;
	}

	/**
	 * @since 3.1
	 */
	public void setJdbcEventLogger(JdbcEventLogger logger) {
		this.jdbcEventLogger = logger;
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
		return dataSource != null ? readThroughDataSource : null;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
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
	@Override
	public void performQueries(Collection<? extends Query> queries, OperationObserver callback) {

		int listSize = queries.size();
		if (listSize == 0) {
			return;
		}

		if (callback.isIteratedResult() && listSize > 1) {
			throw new CayenneRuntimeException("Iterated queries are not allowed in a batch. Batch size: %d", listSize);
		}

		// do this meaningless inexpensive operation to trigger AutoAdapter lazy
		// initialization before opening a connection. Otherwise we may end up
		// with two
		// connections open simultaneously, possibly hitting connection pool
		// upper limit.
		getAdapter().getExtendedTypes();

		Connection connection = null;

		try {
			connection = this.getDataSource().getConnection();
		} catch (Exception globalEx) {
			getJdbcEventLogger().logQueryError(globalEx);

			Transaction transaction = BaseTransaction.getThreadTransaction();
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
				} catch (Exception queryEx) {
					getJdbcEventLogger().logQueryError(queryEx);

					// notify consumer of the exception,
					// stop running further queries
					callback.nextQueryException(nextQuery, queryEx);

					Transaction transaction = BaseTransaction.getThreadTransaction();
					if (transaction != null) {
						transaction.setRollbackOnly();
					}
					break;
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
	@Override
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
	 * Creates a {@link RowReader} using internal {@link RowReaderFactory}.
	 *
	 * @since 4.0
	 */
	public RowReader<?> rowReader(RowDescriptor descriptor, QueryMetadata queryMetadata) {
		return rowReader(descriptor, queryMetadata, Collections.<ObjAttribute, ColumnDescriptor> emptyMap());
	}

	/**
	 * Creates a {@link RowReader} using internal {@link RowReaderFactory}.
	 *
	 * @since 4.0
	 */
	public RowReader<?> rowReader(RowDescriptor descriptor, QueryMetadata queryMetadata,
			Map<ObjAttribute, ColumnDescriptor> attributeOverrides) {
		return rowReaderFactory.rowReader(descriptor, queryMetadata, getAdapter(), attributeOverrides);
	}

	/**
	 * @since 4.0
	 */
	public BatchTranslator batchTranslator(BatchQuery query, String trimFunction) {
		return batchTranslatorFactory.translator(query, getAdapter(), trimFunction);
	}

	/**
	 * @since 4.0
	 */
	public SelectTranslator selectTranslator(Select<?> query) {
		return selectTranslatorFactory.translator(query, getAdapter(), getEntityResolver());
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
	 * @since 4.0
	 */
	public BatchTranslatorFactory getBatchTranslatorFactory() {
		return batchTranslatorFactory;
	}

	/**
	 * @since 4.0
	 */
	public void setBatchTranslatorFactory(BatchTranslatorFactory batchTranslatorFactory) {
		this.batchTranslatorFactory = batchTranslatorFactory;
	}

	/**
	 * @since 4.0
	 */
	public SQLTemplateProcessor getSqlTemplateProcessor() {
		return sqlTemplateProcessor;
	}

	/**
	 * @since 4.0
	 */
	public void setSqlTemplateProcessor(SQLTemplateProcessor sqlTemplateProcessor) {
		this.sqlTemplateProcessor = sqlTemplateProcessor;
	}

	/**
	 * @since 4.0
	 */
	public SelectTranslatorFactory getSelectTranslatorFactory() {
		return selectTranslatorFactory;
	}

	/**
	 * @since 4.0
	 */
	public void setSelectTranslatorFactory(SelectTranslatorFactory selectTranslatorFactory) {
		this.selectTranslatorFactory = selectTranslatorFactory;
	}

    // a read-through DataSource that ensures returning the same connection
    // within
    // transaction.
    final class TransactionDataSource implements DataSource {

        final String CONNECTION_RESOURCE_PREFIX = "DataNode.Connection.";


        @Override
        public Connection getConnection() throws SQLException {
            if (schemaUpdateStrategy != null) {
                schemaUpdateStrategy.updateSchema(DataNode.this);
            }

            Transaction t = BaseTransaction.getThreadTransaction();
            return (t != null) ? t.getOrCreateConnection(CONNECTION_RESOURCE_PREFIX + name, dataSource)
                    : dataSource.getConnection();
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            if (schemaUpdateStrategy != null) {
                schemaUpdateStrategy.updateSchema(DataNode.this);
            }

            Transaction t = BaseTransaction.getThreadTransaction();
            return (t != null) ? t.getOrCreateConnection(CONNECTION_RESOURCE_PREFIX + name, dataSource)
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
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
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
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            // don't throw SQLFeatureNotSupported - this will break JDK 1.5
            // runtime
            throw new UnsupportedOperationException();
        }
    }
}
