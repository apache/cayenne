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

package org.apache.cayenne.access.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.jdbc.reader.RowReader;
import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.access.translator.select.SelectTranslator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.query.PrefetchProcessor;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.Select;

/**
 * A SQLAction that handles SelectQuery execution.
 * 
 * @since 1.2
 */
public class SelectAction extends BaseSQLAction {

	private static void bind(DbAdapter adapter, PreparedStatement statement, DbAttributeBinding[] bindings) throws Exception {

		for (DbAttributeBinding b : bindings) {

			if (b.isExcluded()) {
				continue;
			}

			// null DbAttributes are a result of inferior qualifier
			// processing (qualifier can't map parameters to DbAttributes
			// and therefore only supports standard java types now) hence, a
			// special moronic case here:
			if (b.getAttribute() == null) {
				statement.setObject(b.getStatementPosition(), b.getValue());
			} else {
				adapter.bindParameter(statement, b);
			}
		}

	}

	protected Select<?> query;
	protected QueryMetadata queryMetadata;

	/**
	 * @since 4.0
	 */
	public SelectAction(Select<?> query, DataNode dataNode) {
		super(dataNode);
		this.query = query;
		this.queryMetadata = query.getMetaData(dataNode.getEntityResolver());
	}

	@Override
	public void performAction(Connection connection, OperationObserver observer) throws Exception {

		final long t1 = System.currentTimeMillis();

		JdbcEventLogger logger = dataNode.getJdbcEventLogger();
		SelectTranslator translator = dataNode.selectTranslator(query);
		final String sql = translator.getSql();
		final DbAttributeBinding[] bindings = translator.getBindings();

		logger.logQuery(sql, bindings);

		PreparedStatement statement = connection.prepareStatement(sql);
		bind(dataNode.getAdapter(), statement, bindings);

		int fetchSize = queryMetadata.getStatementFetchSize();
		if (fetchSize != 0) {
			statement.setFetchSize(fetchSize);
		}

		int queryTimeout = queryMetadata.getQueryTimeout();
		if(queryTimeout != QueryMetadata.QUERY_TIMEOUT_DEFAULT) {
			statement.setQueryTimeout(queryTimeout);
		}

		ResultSet rs;

		// need to run in try-catch block to close statement properly if
		// exception happens
		try {
			rs = statement.executeQuery();
		} catch (Exception ex) {
			statement.close();
			throw ex;
		}
		RowDescriptor descriptor = new RowDescriptorBuilder().setColumns(translator.getResultColumns()).getDescriptor(
				dataNode.getAdapter().getExtendedTypes());

		RowReader<?> rowReader = dataNode.rowReader(descriptor, queryMetadata, translator.getAttributeOverrides());

		ResultIterator<?> it = new JDBCResultIterator<>(statement, rs, rowReader);
		it = forIteratedResult(it, observer, connection, t1, sql);
		it = forSuppressedDistinct(it, translator);
		it = forFetchLimit(it, translator);

		// TODO: Should do something about closing ResultSet and
		// PreparedStatement in this method, instead of relying on
		// DefaultResultIterator to do that later

		if (observer.isIteratedResult()) {
			try {
				observer.nextRows(query, it);
			} catch (Exception ex) {
				it.close();
				throw ex;
			}
		} else {
			List<?> resultRows;
			try {
				resultRows = it.allRows();
			} finally {
				it.close();
			}

			dataNode.getJdbcEventLogger().logSelectCount(resultRows.size(), System.currentTimeMillis() - t1, sql);

			observer.nextRows(query, resultRows);
		}
	}

	private <T> ResultIterator<T> forIteratedResult(ResultIterator<T> iterator, OperationObserver observer,
			Connection connection, final long queryStartedAt, final String sql) {
		if (!observer.isIteratedResult()) {
			return iterator;
		}

		return new ConnectionAwareResultIterator<T>(iterator, connection) {
			@Override
			protected void doClose() {
				dataNode.getJdbcEventLogger().logSelectCount(rowCounter, System.currentTimeMillis() - queryStartedAt, sql);
				super.doClose();
			}
		};
	}

	private <T> ResultIterator<T> forFetchLimit(ResultIterator<T> iterator, SelectTranslator translator) {
		// wrap iterator in a fetch limit checker ... there are a few cases when
		// in-memory fetch limit is a noop, however in a general case this is
		// needed, as the SQL result count does not directly correspond to the
		// number of objects returned from Cayenne.

		int fetchLimit = queryMetadata.getFetchLimit();
		int offset = translator.isSuppressingDistinct()
				? queryMetadata.getFetchOffset()
				: getInMemoryOffset(queryMetadata.getFetchOffset());

		if (fetchLimit > 0 || offset > 0) {
			return new LimitResultIterator<>(iterator, offset, fetchLimit);
		} else {
			return iterator;
		}
	}

	private <T> ResultIterator<T> forSuppressedDistinct(ResultIterator<T> iterator, SelectTranslator translator) {
		if (!translator.isSuppressingDistinct() ||
				queryMetadata.isSuppressingDistinct()) {
			return iterator;
		}

		// wrap result iterator if distinct has to be suppressed

		// a joint prefetch warrants full row compare
		final boolean[] compareFullRows = new boolean[1];
		compareFullRows[0] = translator.hasJoins();

		final PrefetchTreeNode rootPrefetch = queryMetadata.getPrefetchTree();
		if (!compareFullRows[0] && rootPrefetch != null) {
			rootPrefetch.traverse(new PrefetchProcessor() {

				@Override
				public void finishPrefetch(PrefetchTreeNode node) {
				}

				@Override
				public boolean startDisjointPrefetch(PrefetchTreeNode node) {
					// continue to children only if we are at root
					return rootPrefetch == node;
				}

				@Override
				public boolean startDisjointByIdPrefetch(PrefetchTreeNode node) {
					// continue to children only if we are at root
					return rootPrefetch == node;
				}

				@Override
				public boolean startUnknownPrefetch(PrefetchTreeNode node) {
					// continue to children only if we are at root
					return rootPrefetch == node;
				}

				@Override
				public boolean startJointPrefetch(PrefetchTreeNode node) {
					if (rootPrefetch != node) {
						compareFullRows[0] = true;
						return false;
					}

					return true;
				}

				@Override
				public boolean startPhantomPrefetch(PrefetchTreeNode node) {
					return true;
				}
			});
		}

		return new DistinctResultIterator<>(iterator, queryMetadata.getDbEntity(), compareFullRows[0]);
	}

}
