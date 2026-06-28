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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.access.translator.procedure.TranslatedProcedure;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.query.ProcedureColumn;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.QueryMetadata;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * A SQLAction that runs a stored procedure. Note that ProcedureAction has
 * internal state and is not thread-safe.
 * 
 * @since 1.2
 */
public class ProcedureAction extends BaseSQLAction {

	protected ProcedureQuery query;

	/**
	 * Holds a number of ResultSets processed by the action. This value is reset
	 * to zero on every "performAction" call.
	 */
	protected int processedResultSets;

	/**
	 * @since 4.0
	 */
	public ProcedureAction(ProcedureQuery query, DataNode dataNode) {
		super(dataNode);
		this.query = query;
	}

	@Override
	public void performAction(Connection connection, OperationObserver observer) throws Exception {

		processedResultSets = 0;

		TranslatedProcedure translated = dataNode.getProcedureTranslator()
				.translate(query, dataNode.getAdapter(), dataNode.getEntityResolver());

		dataNode.getJdbcEventLogger().logQuery(translated.sql(), translated.bindings());

		try (CallableStatement statement = connection.prepareCall(translated.sql())) {
			initStatement(statement);
			bindParameters(statement, translated);

			// stored procedure may contain a mixture of update counts and
			// result sets,
			// and out parameters. Read out parameters first, then
			// iterate until we exhaust all results

			// TODO: andrus, 4/2/2007 - according to the docs we should store
			// the boolean
			// return value of this method and avoid calling 'getMoreResults' if
			// it is
			// true.
			// some db's handle this well, some don't (MySQL).

			// 09/23/2013: almost all adapters except Oracle (and maybe a few
			// more?) are actually using the correct strategy, so making it a
			// default in the superclass, and isolating hack to subclasses is
			// probably a good idea

			statement.execute();

			// read out parameters
			readProcedureOutParameters(statement, observer);

			// read the rest of the query
			while (true) {
				if (statement.getMoreResults()) {

					try (ResultSet rs = statement.getResultSet()) {
						ColumnDescriptor[] columns = describeResultSet(rs, processedResultSets++);
						readResultSet(rs, columns, query, observer);
					}
				} else {
					int updateCount = statement.getUpdateCount();
					if (updateCount == -1) {
						break;
					}
					dataNode.getJdbcEventLogger().logUpdateCount(updateCount);
					observer.nextCount(query, updateCount);
				}
			}
		}
	}

	/**
	 * Applies the translated bindings to the CallableStatement: registers OUT parameters and binds IN parameters.
	 * A stored procedure parameter can be both IN and OUT at the same time.
	 *
	 * @since 5.0
	 */
	protected void bindParameters(CallableStatement statement, TranslatedProcedure translated) throws Exception {
		DbAdapter adapter = dataNode.getAdapter();
		ProcedureParameter[] callParams = translated.callParams();
		ParameterBinding[] bindings = translated.bindings();

		for (int i = 0; i < callParams.length; i++) {
			ProcedureParameter param = callParams[i];

			if (param.isOutParam()) {
				int precision = param.getPrecision();
				if (precision >= 0) {
					statement.registerOutParameter(i + 1, param.getType(), precision);
				} else {
					statement.registerOutParameter(i + 1, param.getType());
				}
			}

			if (param.isInParameter()) {
				adapter.bindParameter(statement, bindings[i]);
			}
		}
	}

	/**
	 * Describes the result set columns, resolving an {@link ExtendedType} for each.
	 *
	 * @param resultSet
	 *            JDBC ResultSet
	 * @param setIndex
	 *            a zero-based index of the ResultSet in the query results.
	 */
	protected ColumnDescriptor[] describeResultSet(ResultSet resultSet, int setIndex) throws SQLException {

		if (setIndex < 0) {
			throw new IllegalArgumentException("Expected a non-negative result set index. Got: " + setIndex);
		}

		ColumnDescriptor.RowBuilder builder = ColumnDescriptor.rowBuilder();

		List<ProcedureColumn[]> descriptors = query.getResultDescriptors();

		if (descriptors.isEmpty()) {
			builder.resultSet(resultSet);
		} else {

			// if one result is described, all of them must be present...
			if (setIndex >= descriptors.size() || descriptors.get(setIndex) == null) {
				throw new CayenneRuntimeException("No descriptor for result set at index '%d' configured.", setIndex);
			}

			builder.columns(toColumnDescriptors(
					descriptors.get(setIndex),
					dataNode.getAdapter().getExtendedTypes()));
		}

		switch (query.getColumnNamesCapitalization()) {
		case LOWER:
			builder.useLowercaseColumnNames();
			break;
		case UPPER:
			builder.useUppercaseColumnNames();
			break;
		}

		return builder.build(dataNode.getAdapter().getExtendedTypes());
	}

	private static ColumnDescriptor[] toColumnDescriptors(ProcedureColumn[] columns, ExtendedTypeMap typeMap) {
		ColumnDescriptor[] result = new ColumnDescriptor[columns.length];
		for (int i = 0; i < columns.length; i++) {
			ProcedureColumn c = columns[i];
			ExtendedType type = typeMap.getRegisteredType(c.javaClass());
			result[i] = new ColumnDescriptor(c.name(), c.dataRowKey(), c.jdbcType(), type, null);
		}
		return result;
	}

	/**
	 * Returns stored procedure for an internal query.
	 */
	protected Procedure getProcedure() {
		return query.getMetaData(dataNode.getEntityResolver()).getProcedure();
	}

	/**
	 * Helper method that reads OUT parameters of a CallableStatement.
	 */
	protected void readProcedureOutParameters(CallableStatement statement, OperationObserver delegate) throws Exception {

		long t1 = System.currentTimeMillis();

		// build result row...
		DataRow result = null;
		List<ProcedureParameter> parameters = getProcedure().getCallParameters();
		for (int i = 0; i < parameters.size(); i++) {
			ProcedureParameter parameter = parameters.get(i);

			if (!parameter.isOutParam()) {
				continue;
			}

			if (result == null) {
				result = new DataRow(2);
			}

			ExtendedType type = dataNode.getAdapter().getExtendedTypes()
					.getRegisteredType(TypesMapping.getJavaBySqlType(parameter.getType()));
			Object val = type.materializeObject(statement, i + 1, parameter.getType());

			result.put(parameter.getName(), val);
		}

		if (result != null && !result.isEmpty()) {
			// treat out parameters as a separate data row set
			dataNode.getJdbcEventLogger().logSelectCount(1, System.currentTimeMillis() - t1);
			delegate.nextRows(query, Collections.singletonList(result));
		}
	}

	protected void initStatement(CallableStatement statement) throws Exception {
		QueryMetadata queryMetadata = query.getMetaData(dataNode.getEntityResolver());
		int statementFetchSize = queryMetadata.getStatementFetchSize();
		if (statementFetchSize != 0) {
			statement.setFetchSize(statementFetchSize);
		}

		int queryTimeout = queryMetadata.getQueryTimeout();
		if(queryTimeout != QueryMetadata.QUERY_TIMEOUT_DEFAULT) {
			statement.setQueryTimeout(queryTimeout);
		}
	}
}
