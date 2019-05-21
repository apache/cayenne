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
package org.apache.cayenne.dba.mysql;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.jdbc.ProcedureAction;
import org.apache.cayenne.access.jdbc.RowDescriptor;
import org.apache.cayenne.access.translator.procedure.ProcedureTranslator;
import org.apache.cayenne.query.ProcedureQuery;

/**
 * @since 3.0
 */
class MySQLProcedureAction extends ProcedureAction {

	public MySQLProcedureAction(ProcedureQuery query, DataNode dataNode) {
		super(query, dataNode);
	}

	@Override
	public void performAction(Connection connection, OperationObserver observer) throws SQLException, Exception {

		processedResultSets = 0;

		ProcedureTranslator transl = createTranslator(connection);

		try (CallableStatement statement = (CallableStatement) transl.createStatement();) {

			// this is one difference with super - we need to read the first
			// result set
			// without calling 'getMoreResults' - which may actually be a good
			// default
			// strategy?
			boolean firstResult = statement.execute();

			// read out parameters
			readProcedureOutParameters(statement, observer);

			// read first result
			if (firstResult) {
				processResultSet(statement, observer);
			} else if (!processUpdate(statement, observer)) {
				return;
			}

			// read the rest of the query
			while (true) {
				if (statement.getMoreResults()) {
					processResultSet(statement, observer);
				} else if (!processUpdate(statement, observer)) {
					break;
				}
			}
		}

	}

	private void processResultSet(CallableStatement statement, OperationObserver observer) throws Exception {
		ResultSet rs = Objects.requireNonNull(statement.getResultSet());

		try {
			RowDescriptor descriptor = describeResultSet(rs, processedResultSets++);
			readResultSet(rs, descriptor, query, observer);
		} finally {
			try {
				rs.close();
			} catch (SQLException ex) {
			}
		}
	}

	private boolean processUpdate(CallableStatement statement, OperationObserver observer) throws Exception {
		int updateCount = statement.getUpdateCount();
		if (updateCount == -1) {
			return false;
		}
		dataNode.getJdbcEventLogger().logUpdateCount(updateCount);
		observer.nextCount(query, updateCount);

		return true;
	}

	/**
	 * Creates a translator that adds parenthesis to no-param queries.
	 */
	// see CAY-750 for the problem description
	@Override
	protected ProcedureTranslator createTranslator(Connection connection) {
		ProcedureTranslator translator = new MySQLProcedureTranslator();
		translator.setAdapter(dataNode.getAdapter());
		translator.setQuery(query);
		translator.setEntityResolver(dataNode.getEntityResolver());
		translator.setConnection(connection);
		translator.setJdbcEventLogger(dataNode.getJdbcEventLogger());
		return translator;
	}

	// same as postgres translator - should we make this the default?
	static class MySQLProcedureTranslator extends ProcedureTranslator {

		@Override
		protected String createSqlString() {

			String sql = super.createSqlString();

			// add empty parameter parenthesis
			if (sql.endsWith("}") && !sql.endsWith(")}")) {
				sql = sql.substring(0, sql.length() - 1) + "()}";
			}

			return sql;
		}
	}
}
