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
import org.apache.cayenne.OutParametersResult;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.SelectResult;
import org.apache.cayenne.UpdateResult;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.StatementContext;
import org.apache.cayenne.access.jdbc.reader.RowReader;
import org.apache.cayenne.access.translator.TranslatedProcedure;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.compiled.CompiledProcedureParameter;
import org.apache.cayenne.query.ProcedureColumn;
import org.apache.cayenne.query.resolved.ResolvedProcedure;
import org.apache.cayenne.query.resolved.ResolvedQuery;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs a translated stored procedure call, reporting its OUT parameters, result sets and update
 * counts.
 *
 * @since 6.0
 */
public class DataNodeProcedureAction {

	protected final DataNode dataNode;
	protected final TranslatedProcedure translated;
	protected final ResolvedProcedure query;

	/**
	 * Holds a number of ResultSets processed by the action. This value is reset to zero on every "execute"
	 * call.
	 */
	protected int processedResultSets;

	public DataNodeProcedureAction(TranslatedProcedure translated, DataNode dataNode) {
		this.dataNode = dataNode;
		this.translated = translated;
		this.query = translated.query();
	}

	public void execute(Connection connection, StatementContext context) throws Exception {

		processedResultSets = 0;

		try (CallableStatement statement = connection.prepareCall(translated.sql())) {
			initStatement(statement, context);
			bindParameters(statement);

			// stored procedure may contain a mixture of update counts and
			// result sets, and out parameters. Read out parameters first, then
			// iterate until we exhaust all results

			// TODO: andrus, 4/2/2007 - according to the docs we should store
			// the boolean return value of this method and avoid calling 'getMoreResults' if
			// it is true. some db's handle this well, some don't (MySQL).

			// 09/23/2013: almost all adapters except Oracle (and maybe a few
			// more?) are actually using the correct strategy, so making it a
			// default in the superclass, and isolating hack to subclasses is
			// probably a good idea

			statement.execute();

			// read out parameters
			readProcedureOutParameters(statement, context);

			// read the rest of the query
			while (true) {
				if (statement.getMoreResults()) {
					try (ResultSet rs = statement.getResultSet()) {
						RSColumn[] columns = describeResultSet(rs, processedResultSets++);
						readResultSet(rs, columns, context);
					}
				} else {
					int updateCount = statement.getUpdateCount();
					if (updateCount == -1) {
						break;
					}
					context.addResult(new UpdateResult(new int[]{updateCount}));
				}
			}
		}
	}

	/**
	 * Binds the IN parameters and registers the OUT parameters of the call.
	 */
	protected void bindParameters(CallableStatement statement) throws Exception {
		DbAdapter adapter = dataNode.getAdapter();
		CSParameter<?>[] params = translated.params();
		for (int i = 0; i < params.length; i++) {
			CSParameter<?> binding = params[i];
			CompiledProcedureParameter param = binding.param();

			if (param.isOutParameter()) {
				int precision = param.precision();
				if (precision >= 0) {
					statement.registerOutParameter(i + 1, param.jdbcType(), precision);
				} else {
					statement.registerOutParameter(i + 1, param.jdbcType());
				}
			}

			if (param.isInParameter()) {
				adapter.bindParameter(statement, binding);
			}
		}
	}

	/**
	 * Returns the descriptors of the columns of a result set, from the query's result descriptors if any were set,
	 * or from the ResultSet metadata.
	 *
	 * @param setIndex the index of the result set among those returned by the procedure
	 */
	protected RSColumn[] describeResultSet(ResultSet resultSet, int setIndex) throws SQLException {
		if (setIndex < 0) {
			throw new IllegalArgumentException("Expected a non-negative result set index. Got: " + setIndex);
		}

		RSColumn.RowBuilder builder = RSColumn.rowBuilder();

		List<ProcedureColumn[]> descriptors = query.resultDescriptors();
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

		switch (query.columnNameCaps()) {
		case LOWER:
			builder.useLowercaseColumnNames();
			break;
		case UPPER:
			builder.useUppercaseColumnNames();
			break;
		}

		return builder.build(dataNode.getAdapter().getExtendedTypes());
	}

	private static RSColumn[] toColumnDescriptors(ProcedureColumn[] columns, ExtendedTypeMap typeMap) {
		RSColumn[] result = new RSColumn[columns.length];
		for (int i = 0; i < columns.length; i++) {
			ProcedureColumn c = columns[i];
			ExtendedType<?> type = typeMap.getRegisteredType(c.javaClass());
			result[i] = new RSColumn(c.name(), c.jdbcType(), c.dataRowKey(), type, null);
		}
		return result;
	}

	/**
	 * Reads the OUT parameters of the call, reporting them as an {@link OutParametersResult} if there are any.
	 */
	protected void readProcedureOutParameters(CallableStatement statement, StatementContext context) throws Exception {

		Map<String, Object> result = null;
		List<CompiledProcedureParameter> parameters = query.procedure().parameters();
		for (int i = 0; i < parameters.size(); i++) {
			CompiledProcedureParameter parameter = parameters.get(i);

			if (!parameter.isOutParameter()) {
				continue;
			}

			if (result == null) {
				result = new LinkedHashMap<>();
			}

			ExtendedType<?> type = dataNode.getAdapter()
					.getExtendedTypes()
					.getRegisteredType(TypesMapping.getJavaBySqlType(parameter.jdbcType()));
			Object val = type.materializeObject(statement, i + 1, parameter.jdbcType());
			result.put(parameter.name(), val);
		}

		if (result != null) {
			context.addResult(new OutParametersResult(result));
		}
	}

	protected void initStatement(CallableStatement statement, StatementContext context) throws Exception {
		int statementFetchSize = query.statementFetchSize();
		if (statementFetchSize != 0) {
			statement.setFetchSize(statementFetchSize);
		}

		int queryTimeout = query.queryTimeout();
		if(queryTimeout != ResolvedQuery.QUERY_TIMEOUT_DEFAULT) {
			statement.setQueryTimeout(queryTimeout);
		}
	}

	/**
	 * Reads a ResultSet, reporting the rows (or an open iterator over them) to the statement context.
	 */
	protected void readResultSet(ResultSet resultSet, RSColumn[] columns, StatementContext context) throws Exception {
		ResultIterator<?> it = resultIterator(resultSet, columns, context);
		if (!context.isIteratedResult()) {
			context.addResult(new SelectResult<>(it.allRows()));
		} else {
			try {
				context.addResult(it);
			} catch (Exception ex) {
				it.close();
				throw ex;
			}
		}
	}

	/**
	 * Creates an iterator over the ResultSet rows, applying the fetch offset and limit of the query in memory.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected ResultIterator<?> resultIterator(ResultSet resultSet, RSColumn[] columns, StatementContext context) {
		RowReader<?> rowReader = dataNode.getRowReaderFactory()
				.rowReader(columns, query.resultSegments(), query, dataNode.getAdapter());
		RSIterator resultReader = new RSIterator(null, resultSet, rowReader);
		return new LimitResultIterator(resultReader, query.fetchOffset(), query.fetchLimit());
	}
}
