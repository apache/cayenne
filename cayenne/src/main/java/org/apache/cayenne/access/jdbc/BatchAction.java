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

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.OptimisticLockException;
import org.apache.cayenne.access.jdbc.reader.RowReader;
import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.access.translator.batch.BatchTranslator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.BatchQueryRow;
import org.apache.cayenne.query.InsertBatchQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @since 1.2
 */
public class BatchAction extends BaseSQLAction {

	protected boolean runningAsBatch;
	protected BatchQuery query;
	protected RowDescriptor keyRowDescriptor;

	private static void bind(DbAdapter adapter, PreparedStatement statement, DbAttributeBinding[] bindings)
			throws Exception {

		for (DbAttributeBinding b : bindings) {
			if (!b.isExcluded()) {
				adapter.bindParameter(statement, b);
			}
		}
	}

	/**
	 * @since 4.0
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
	public void performAction(Connection connection, OperationObserver observer) throws Exception {
		BatchTranslator translator = createTranslator();

		boolean isBatch = canRunAsBatch();
		boolean generatesKeys = hasGeneratedKeys() && supportsGeneratedKeys(isBatch);

		if (isBatch) {
			runAsBatch(connection, translator, observer, generatesKeys);
		} else {
			runAsIndividualQueries(connection, translator, observer, generatesKeys);
		}
	}

	protected boolean canRunAsBatch() {
		if(!runningAsBatch || query.getRows().size() <= 1) {
			return false;
		}

		if (hasGeneratedKeys()) {
			// turn off batch mode if we generate keys but can't do so in a batch
			return supportsGeneratedKeys(true) &&
					!dataNode.getEntityResolver().getEntitySorter().isReflexive(query.getDbEntity());
		}

		return true;
	}

	protected BatchTranslator createTranslator() {
		return dataNode.batchTranslator(query, null);
	}

	protected void runAsBatch(Connection con, BatchTranslator translator, OperationObserver delegate, boolean generatesKeys)
			throws Exception {

		String sql = translator.getSql();
		JdbcEventLogger logger = dataNode.getJdbcEventLogger();
		boolean isLoggable = logger.isLoggable();

		// log batch SQL execution
		logger.log(sql);

		// run batch

		DbAdapter adapter = dataNode.getAdapter();

		try (PreparedStatement statement = prepareStatement(con, sql, adapter, generatesKeys)) {
			for (BatchQueryRow row : query.getRows()) {

				DbAttributeBinding[] bindings = translator.updateBindings(row);
				logger.logQueryParameters("batch bind", bindings);
				bind(adapter, statement, bindings);

				statement.addBatch();
			}

			// execute the whole batch
			int[] results = statement.executeBatch();
			delegate.nextBatchCount(query, results);

			if (generatesKeys) {
				processGeneratedKeys(statement, delegate, query.getRows());
			}
			
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
		}
	}

	/**
	 * Executes batch as individual queries over the same prepared statement.
	 */
	protected void runAsIndividualQueries(Connection connection, BatchTranslator translator,
			OperationObserver delegate, boolean generatesKeys) throws SQLException, Exception {

		if(query.getRows().isEmpty()) {
			return;
		}

		JdbcEventLogger logger = dataNode.getJdbcEventLogger();
		boolean useOptimisticLock = query.isUsingOptimisticLocking();

		String queryStr = translator.getSql();

		// log batch SQL execution
		logger.log(queryStr);

		// run batch queries one by one

		DbAdapter adapter = dataNode.getAdapter();

		try (PreparedStatement statement = prepareStatement(connection, queryStr, adapter, generatesKeys)) {
			for (BatchQueryRow row : query.getRows()) {

				DbAttributeBinding[] bindings = translator.updateBindings(row);
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
		}
	}

	protected PreparedStatement prepareStatement(Connection connection,	String queryStr,
												 DbAdapter adapter,	boolean generatedKeys) throws SQLException {
		return (generatedKeys)
				? connection.prepareStatement(queryStr, Statement.RETURN_GENERATED_KEYS)
				: connection.prepareStatement(queryStr);
	}

	protected boolean supportsGeneratedKeys(boolean isBatch) {
		// see if we are configured to support generated keys
		return isBatch
				? dataNode.getAdapter().supportsGeneratedKeysForBatchInserts()
				: dataNode.getAdapter().supportsGeneratedKeys();
	}
				
	/**
	 * Returns whether BatchQuery generates any keys.
	 */
	protected boolean hasGeneratedKeys() {
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
	 * @since 4.0
	 */
	protected void processGeneratedKeys(Statement statement, OperationObserver observer, BatchQueryRow row)
			throws SQLException {
		processGeneratedKeys(statement, observer, Collections.singletonList(row));
	}

	/**
	 * @since 4.2
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void processGeneratedKeys(Statement statement, OperationObserver observer, List<BatchQueryRow> rows)
			throws SQLException {

		ResultSet keysRS = statement.getGeneratedKeys();

		// TODO: andrus, 7/4/2007 - use a different form of Statement.execute -
		//  "execute(String,String[])" to be able to map generated column names
		// (this way we can support multiple columns..
		// although need to check how well this works with most common drivers)

		RowDescriptorBuilder builder = new RowDescriptorBuilder();

		if (this.keyRowDescriptor == null) {
			// attempt to figure out the right descriptor from the mapping...
			Collection<DbAttribute> generated = query.getDbEntity().getGeneratedAttributes();
			if (generated.size() == 1 && keysRS.getMetaData().getColumnCount() == 1) {
				DbAttribute key = generated.iterator().next();

				ColumnDescriptor[] columns = new ColumnDescriptor[1];

				// use column name from result set, but type and Java class from DB attribute
				columns[0] = new ColumnDescriptor(keysRS.getMetaData(), 1);
				columns[0].setJdbcType(key.getType());
				columns[0].setJavaClass(typeForGeneratedPK(key));
				builder.setColumns(columns);
			} else {
				builder.setResultSet(keysRS);
			}

			this.keyRowDescriptor = builder.getDescriptor(dataNode.getAdapter().getExtendedTypes());
		}

		RowReader<?> rowReader = dataNode.rowReader(keyRowDescriptor, query.getMetaData(dataNode.getEntityResolver()),
				Collections.emptyMap());
		ResultIterator iterator = new JDBCResultIterator(null, keysRS, rowReader);

		List<ObjectId> objectIds = new ArrayList<>(rows.size());
		for(BatchQueryRow row : rows) {
			objectIds.add(row.getObjectId());
		}
		observer.nextGeneratedRows(query, iterator, objectIds);
	}

	private String typeForGeneratedPK(DbAttribute key) {
		String entityName = getQuery().getRows().get(0).getObjectId().getEntityName();
		ObjEntity objEntity = dataNode.getEntityResolver().getObjEntity(entityName);
		if(objEntity != null) {
			ObjAttribute attributeForDbAttribute = objEntity.getAttributeForDbAttribute(key);
			if(attributeForDbAttribute != null) {
				return attributeForDbAttribute.getType();
			}
		}
		return key.getJavaClass();
	}
}
