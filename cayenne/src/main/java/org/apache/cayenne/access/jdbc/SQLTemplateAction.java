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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.jdbc.reader.RowReader;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DefaultScalarResultSegment;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.util.Util;

/**
 * Implements a strategy for execution of SQLTemplates.
 *
 * @since 1.2 replaces SQLTemplateExecutionPlan
 */
public class SQLTemplateAction implements SQLAction {

	protected SQLTemplate query;
	protected QueryMetadata queryMetadata;

	protected DbEntity dbEntity;
	protected DataNode dataNode;
	protected DbAdapter dbAdapter;

	/**
	 * @since 4.0
	 */
	public SQLTemplateAction(SQLTemplate query, DataNode dataNode) {
		this.query = query;
		this.dataNode = dataNode;
		this.queryMetadata = query.getMetaData(dataNode.getEntityResolver());
		this.dbEntity = queryMetadata.getDbEntity();

		// using unwrapped adapter to check for the right SQL flavor...
		this.dbAdapter = dataNode.getAdapter().unwrap();
	}

	/**
	 * Returns unwrapped DbAdapter used to find correct SQL for a given DB.
	 */
	public DbAdapter getAdapter() {
		return dbAdapter;
	}

	/**
	 * Runs a SQLTemplate query, collecting all results. If a callback expects
	 * an iterated result, result processing is stopped after the first
	 * ResultSet is encountered.
	 */
	@Override
	public void performAction(Connection connection, OperationObserver callback) throws SQLException, Exception {

		String template = extractTemplateString();

		// sanity check - misconfigured templates
		if (template == null) {
			throw new CayenneRuntimeException("No template string configured for adapter " + dbAdapter.getClass().getName());
		}

		boolean loggable = dataNode.getJdbcEventLogger().isLoggable();
		List<Number> counts = new ArrayList<>();

		// bind either positional or named parameters;
		// for legacy reasons named parameters are processed as a batch.. this
		// should go away after 4.0; newer positional parameter only support a
		// single set of values.
		if (query.getPositionalParams().isEmpty()) {
			runWithNamedParametersBatch(connection, callback, template, counts, loggable);
		} else {
			runWithPositionalParameters(connection, callback, template, counts, loggable);
		}

		// notify of combined counts of all queries inside SQLTemplate
		// multiplied by the number of parameter sets...
		int[] ints = new int[counts.size()];
		for (int i = 0; i < ints.length; i++) {
			ints[i] = counts.get(i).intValue();
		}

		callback.nextBatchCount(query, ints);
	}

	private void bindExtendedTypes(ParameterBinding[] bindings) {
		int i = 1;
		for (ParameterBinding binding : bindings) {
			Object value = binding.getValue();
			ExtendedType extendedType = value != null
					? getAdapter().getExtendedTypes().getRegisteredType(value.getClass())
					: getAdapter().getExtendedTypes().getDefaultType();
			binding.setExtendedType(extendedType);
			binding.setStatementPosition(i++);
		}
	}

	private void runWithPositionalParameters(Connection connection, OperationObserver callback, String template,
											 Collection<Number> counts, boolean loggable) throws Exception {

		SQLStatement compiled = dataNode.getSqlTemplateProcessor().processTemplate(template,
				query.getPositionalParams());

		bindExtendedTypes(compiled.getBindings());
		if (loggable) {
			dataNode.getJdbcEventLogger().logQuery(compiled.getSql(), compiled.getBindings());
		}

		execute(connection, callback, compiled, counts);
	}

	@SuppressWarnings("unchecked")
	private void runWithNamedParametersBatch(Connection connection, OperationObserver callback, String template,
											 Collection<Number> counts, boolean loggable) throws Exception {

		int size = query.parametersSize();

		// zero size indicates a one-shot query with no parameters
		// so fake a single entry batch...
		int batchSize = (size > 0) ? size : 1;

		// for now supporting deprecated batch parameters...
		Iterator<Map<String, ?>> it;
		if(size == 0) {
			Iterator empty = Collections.singleton(Collections.emptyMap()).iterator();
			it = empty;
		} else {
			it = query.parametersIterator();
		}

		for (int i = 0; i < batchSize; i++) {
			Map<String, ?> nextParameters = it.next();
			SQLStatement compiled = dataNode.getSqlTemplateProcessor().processTemplate(template, nextParameters);
			bindExtendedTypes(compiled.getBindings());
			if (loggable) {
				dataNode.getJdbcEventLogger().logQuery(compiled.getSql(), compiled.getBindings());
			}

			execute(connection, callback, compiled, counts);
		}

	}

	protected void execute(Connection connection, OperationObserver callback, SQLStatement compiled,
						   Collection<Number> updateCounts) throws SQLException, Exception {

		long t1 = System.currentTimeMillis();
		boolean iteratedResult = callback.isIteratedResult();
		int generatedKeys = query.isReturnGeneratedKeys() ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS;
		PreparedStatement statement = connection.prepareStatement(compiled.getSql(), generatedKeys);

		try {
			bind(statement, compiled.getBindings());

			// process a mix of results
			boolean isResultSet = statement.execute();

			if(query.isReturnGeneratedKeys()) {
				ResultSet generatedKeysResultSet = statement.getGeneratedKeys();
				if (generatedKeysResultSet != null) {
					processSelectResult(compiled, connection, statement, generatedKeysResultSet, callback, t1);
				}
			}

			boolean firstIteration = true;
			while (true) {
				if (firstIteration) {
					firstIteration = false;
				} else {
					isResultSet = statement.getMoreResults();
				}

				if (isResultSet) {

					ResultSet resultSet = statement.getResultSet();
					if (resultSet != null) {

						try {
							processSelectResult(compiled, connection, statement, resultSet, callback, t1);
						} finally {
							if (!iteratedResult) {
								resultSet.close();
							}
						}

						// ignore possible following update counts and bail early on iterated results
						if (iteratedResult) {
							break;
						}
					}
				} else {
					int updateCount = statement.getUpdateCount();
					if (updateCount == -1) {
						break;
					}

					updateCounts.add(updateCount);
					dataNode.getJdbcEventLogger().logUpdateCount(updateCount);
				}
			}
		} finally {
			if (!iteratedResult) {
				statement.close();
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void processSelectResult(SQLStatement compiled, Connection connection, Statement statement,
									   ResultSet resultSet, OperationObserver callback, final long startTime) throws Exception {

		boolean iteratedResult = callback.isIteratedResult();
		ExtendedTypeMap types = dataNode.getAdapter().getExtendedTypes();
		RowDescriptorBuilder builder = configureRowDescriptorBuilder(compiled, resultSet);
		recreateQueryMetadata(resultSet);
		RowReader<?> rowReader = dataNode.rowReader(builder.getDescriptor(types), queryMetadata);
		ResultIterator<?> it = new JDBCResultIterator<>(statement, resultSet, rowReader);

		if (iteratedResult) {

			it = new ConnectionAwareResultIterator(it, connection) {
				@Override
				protected void doClose() {
					dataNode.getJdbcEventLogger().logSelectCount(rowCounter, System.currentTimeMillis() - startTime);
					super.doClose();
				}
			};
		}

		it = new LimitResultIterator<>(it, getFetchOffset(), query.getFetchLimit());

		if (iteratedResult) {
			try {
				callback.nextRows(query, it);
			} catch (Exception ex) {
				it.close();
				throw ex;
			}
		} else {
			// note that we are not closing the iterator here, relying on caller
			// to close the underlying ResultSet on its own... this is a hack,
			// maybe a cleaner flow is due here.
			List<?> resultRows = it.allRows();

			dataNode.getJdbcEventLogger().logSelectCount(resultRows.size(), System.currentTimeMillis() - startTime);

			callback.nextRows(query, resultRows);
		}
	}

	private void recreateQueryMetadata(ResultSet resultSet) throws SQLException {
		if(query.isUseScalar() && queryMetadata.getResultSetMapping() != null && queryMetadata.getResultSetMapping().isEmpty()){
			for(int i = 0; i < resultSet.getMetaData().getColumnCount(); i++) {
				queryMetadata.getResultSetMapping().add(new DefaultScalarResultSegment(String.valueOf(i), i));
			}
		}
	}

	/**
	 * Creates column descriptors based on compiled statement and query metadata
	 */
	private ColumnDescriptor[] createColumnDescriptors(SQLStatement compiled) {
		// SQLTemplate #result columns take precedence over other ways to determine the type
		if (compiled.getResultColumns().length > 0) {
			if(query.getResultColumnsTypes() != null) {
				throw new CayenneRuntimeException("Caused by setting return types by directives and by parameters in query.");
			} else {
				return compiled.getResultColumns();
			}
		}

		// check explicitly set column types
		if(query.getResultColumnsTypes() == null) {
			return null;
		}

		int size = query.getResultColumnsTypes().size();
		ColumnDescriptor[] columnDescriptors = new ColumnDescriptor[size];
		for(int i = 0; i < size; i++) {
			ColumnDescriptor columnDescriptor = new ColumnDescriptor();
			columnDescriptor.setJavaClass(query.getResultColumnsTypes().get(i).getCanonicalName());
			columnDescriptors[i] = columnDescriptor;
		}
		return columnDescriptors;
	}

	/**
	 * @since 3.0
	 */
	protected RowDescriptorBuilder configureRowDescriptorBuilder(SQLStatement compiled, ResultSet resultSet)
			throws SQLException {
		RowDescriptorBuilder builder = new RowDescriptorBuilder()
				.setResultSet(resultSet)
				.setColumns(createColumnDescriptors(compiled))
				.validateDuplicateColumnNames();

		if(query.getResultColumnsTypes() != null) {
			builder.mergeColumnsWithRsMetadata();
		}

		ObjEntity entity = queryMetadata.getObjEntity();
		if (entity != null && isResultColumnTypesEmpty()) {
			// TODO: andrus 2008/03/28 support flattened attributes with aliases...
			for (ObjAttribute attribute : entity.getAttributes()) {
				CayennePath column = attribute.getDbAttributePath();
				if (column == null || column.length() > 1) {
					continue;
				}
				builder.overrideColumnType(column.value(), attribute.getType());
			}
		}

		// override numeric Java types based on JDBC defaults for DbAttributes, as Oracle
		// ResultSetMetadata is not very precise about NUMERIC distinctions...
		// (BigDecimal vs Long vs. Integer)
		if (dbEntity != null && isResultColumnTypesEmpty()) {
			for (DbAttribute attribute : dbEntity.getAttributes()) {
				if (!builder.isOverriden(attribute.getName()) && TypesMapping.isNumeric(attribute.getType())) {
					builder.overrideColumnType(attribute.getName(), TypesMapping.getJavaBySqlType(attribute));
				}
			}
		}

		switch (query.getColumnNamesCapitalization()) {
			case LOWER:
				builder.useLowercaseColumnNames();
				break;
			case UPPER:
				builder.useUppercaseColumnNames();
				break;
		}

		return builder;
	}

	private boolean isResultColumnTypesEmpty(){
		return query.getResultColumnsTypes() == null || query.getResultColumnsTypes().isEmpty();
	}

	/**
	 * Extracts a template string from a SQLTemplate query. Exists mainly for
	 * the benefit of subclasses that can customize returned template.
	 *
	 * @since 1.2
	 */
	protected String extractTemplateString() {
		String sql = query.getTemplate(dbAdapter.getClass().getName());

		// note that we MUST convert line breaks to spaces. On some databases (DB2)
		// queries with breaks simply won't run; the rest are affected by CAY-726.
		return Util.stripLineBreaks(sql, ' ');
	}

	/**
	 * Binds parameters to the PreparedStatement.
	 */
	protected void bind(PreparedStatement preparedStatement, ParameterBinding[] bindings)
			throws SQLException, Exception {
		// bind parameters
		for (ParameterBinding binding : bindings) {
			dataNode.getAdapter().bindParameter(preparedStatement, binding);
		}

		if (queryMetadata.getStatementFetchSize() != 0) {
			preparedStatement.setFetchSize(queryMetadata.getStatementFetchSize());
		}

		int queryTimeout = queryMetadata.getQueryTimeout();
		if(queryTimeout != QueryMetadata.QUERY_TIMEOUT_DEFAULT) {
			preparedStatement.setQueryTimeout(queryTimeout);
		}
	}

	/**
	 * Returns a SQLTemplate for this action.
	 */
	public SQLTemplate getQuery() {
		return query;
	}

	/**
	 * @since 3.0
	 */
	protected int getFetchOffset() {
		return query.getFetchOffset();
	}
}
