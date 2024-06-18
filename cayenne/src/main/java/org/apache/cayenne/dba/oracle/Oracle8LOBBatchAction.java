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

package org.apache.cayenne.dba.oracle;

import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.BatchQueryRow;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.UpdateBatchQuery;
import org.apache.cayenne.util.Util;

/**
 * @since 3.0
 */
class Oracle8LOBBatchAction implements SQLAction {

	private BatchQuery query;
	private DbAdapter adapter;
	private JdbcEventLogger logger;

	private static void bind(DbAdapter adapter, PreparedStatement statement, DbAttributeBinding[] bindings)
			throws SQLException, Exception {

		for (DbAttributeBinding b : bindings) {
			DbAttributeBinding binding = new DbAttributeBinding(b.getAttribute());
			adapter.bindParameter(statement, binding);
		}
	}

	Oracle8LOBBatchAction(BatchQuery query, DbAdapter adapter, JdbcEventLogger logger) {
		this.adapter = adapter;
		this.query = query;
		this.logger = logger;
	}

	@Override
	public void performAction(Connection connection, OperationObserver observer) throws SQLException, Exception {

		Oracle8LOBBatchTranslator translator;
		if (query instanceof InsertBatchQuery) {
			translator = new Oracle8LOBInsertBatchTranslator((InsertBatchQuery) query, adapter,
					OracleAdapter.TRIM_FUNCTION);
		} else if (query instanceof UpdateBatchQuery) {
			translator = new Oracle8LOBUpdateBatchTranslator((UpdateBatchQuery) query, adapter,
					OracleAdapter.TRIM_FUNCTION);
		} else {
			throw new CayenneRuntimeException("Unsupported batch type for special LOB processing: " + query);
		}

		translator.setNewBlobFunction(OracleAdapter.NEW_BLOB_FUNCTION);
		translator.setNewClobFunction(OracleAdapter.NEW_CLOB_FUNCTION);

		// no batching is done, queries are translated
		// for each batch set, since prepared statements
		// may be different depending on whether LOBs are NULL or not..

		Oracle8LOBBatchQueryWrapper selectQuery = new Oracle8LOBBatchQueryWrapper(query);
		List<DbAttribute> qualifierAttributes = selectQuery.getDbAttributesForLOBSelectQualifier();

		for (BatchQueryRow row : query.getRows()) {

			selectQuery.indexLOBAttributes(row);

			int updated;
			String updateStr = translator.createSql(row);

			// 1. run row update
			logger.log(updateStr);

			try (PreparedStatement statement = connection.prepareStatement(updateStr)) {

				DbAttributeBinding[] bindings = translator.updateBindings(row);
				logger.logQueryParameters("bind", bindings);

				bind(adapter, statement, bindings);

				updated = statement.executeUpdate();
				logger.logUpdateCount(updated);
			}

			// 2. run row LOB update (SELECT...FOR UPDATE and writing out LOBs)
			processLOBRow(connection, translator, selectQuery, qualifierAttributes, row);

			// finally, notify delegate that the row was updated
			observer.nextCount(query, updated);
		}
	}

	void processLOBRow(Connection con, Oracle8LOBBatchTranslator queryBuilder, Oracle8LOBBatchQueryWrapper selectQuery,
			List<DbAttribute> qualifierAttributes, BatchQueryRow row) throws SQLException, Exception {

		List<DbAttribute> lobAttributes = selectQuery.getDbAttributesForUpdatedLOBColumns();
		if (lobAttributes.size() == 0) {
			return;
		}

		final boolean isLoggable = logger.isLoggable();

		List<Object> qualifierValues = selectQuery.getValuesForLOBSelectQualifier(row);
		List<Object> lobValues = selectQuery.getValuesForUpdatedLOBColumns();
		int parametersSize = qualifierValues.size();
		int lobSize = lobAttributes.size();

		String selectStr = queryBuilder.createLOBSelectString(lobAttributes, qualifierAttributes);

		try (PreparedStatement selectStatement = con.prepareStatement(selectStr)) {
			DbAttributeBinding[] attributeBindings = null;
			if(isLoggable) {
				attributeBindings = new DbAttributeBinding[parametersSize];
			}
			for (int i = 0; i < parametersSize; i++) {
				DbAttribute attribute = qualifierAttributes.get(i);
				Object value = qualifierValues.get(i);
				ExtendedType extendedType = value != null
						? adapter.getExtendedTypes().getRegisteredType(value.getClass())
						: adapter.getExtendedTypes().getDefaultType();

				DbAttributeBinding binding = new DbAttributeBinding(attribute);
				binding.setStatementPosition(i + 1);
				binding.setValue(value);
				binding.setExtendedType(extendedType);
				adapter.bindParameter(selectStatement, binding);
				if(isLoggable) {
					attributeBindings[i] = binding;
				}
			}

			if (isLoggable) {
				logger.logQuery(selectStr, attributeBindings);
			}

			try (ResultSet result = selectStatement.executeQuery()) {
				if (!result.next()) {
					throw new CayenneRuntimeException("Missing LOB row.");
				}

				// read the only expected row
				for (int i = 0; i < lobSize; i++) {
					DbAttribute attribute = lobAttributes.get(i);
					int type = attribute.getType();

					if (type == Types.CLOB) {
						Clob clob = result.getClob(i + 1);
						Object clobVal = lobValues.get(i);

						if (clobVal instanceof char[]) {
							writeClob(clob, (char[]) clobVal);
						} else {
							writeClob(clob, clobVal.toString());
						}
					} else if (type == Types.BLOB) {
						Blob blob = result.getBlob(i + 1);

						Object blobVal = lobValues.get(i);
						if (blobVal instanceof byte[]) {
							writeBlob(blob, (byte[]) blobVal);
						} else {
							String className = (blobVal != null) ? blobVal.getClass().getName() : null;
							throw new CayenneRuntimeException("Unsupported class of BLOB value: %s", className);
						}
					} else {
						throw new CayenneRuntimeException("Only BLOB or CLOB is expected here, got: %s", type);
					}
				}

				if (result.next()) {
					throw new CayenneRuntimeException("More than one LOB row found.");
				}
			}
		}
	}

	/**
	 * Override the Oracle writeBlob() method to be compatible with Oracle8
	 * drivers.
	 */
	protected void writeBlob(Blob blob, byte[] value) {
		// Fix for CAY-1307. For Oracle8, get the method found by reflection in
		// OracleAdapter. (Code taken from Cayenne 2.)
		Method getBinaryStreamMethod = Oracle8Adapter.getOutputStreamFromBlobMethod();
		try {

			try (OutputStream out = (OutputStream) getBinaryStreamMethod.invoke(blob, (Object[]) null)) {
				out.write(value);
				out.flush();
			}
		} catch (Exception e) {
			throw new CayenneRuntimeException("Error processing BLOB.", Util.unwindException(e));
		}
	}

	/**
	 * Override the Oracle writeClob() method to be compatible with Oracle8
	 * drivers.
	 */
	protected void writeClob(Clob clob, char[] value) {
		Method getWriterMethod = Oracle8Adapter.getWriterFromClobMethod();
		try {

			try (Writer out = (Writer) getWriterMethod.invoke(clob, (Object[]) null)) {
				out.write(value);
				out.flush();
			}

		} catch (Exception e) {
			throw new CayenneRuntimeException("Error processing CLOB.", Util.unwindException(e));
		}
	}

	/**
	 * Override the Oracle writeClob() method to be compatible with Oracle8
	 * drivers.
	 */
	protected void writeClob(Clob clob, String value) {
		Method getWriterMethod = Oracle8Adapter.getWriterFromClobMethod();
		try {

			try (Writer out = (Writer) getWriterMethod.invoke(clob, (Object[]) null)) {
				out.write(value);
				out.flush();
			}
		} catch (Exception e) {
			throw new CayenneRuntimeException("Error processing CLOB.", Util.unwindException(e));
		}
	}
}
