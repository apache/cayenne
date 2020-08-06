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
package org.apache.cayenne.dba.db2;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.jdbc.ProcedureAction;
import org.apache.cayenne.access.jdbc.RowDescriptor;
import org.apache.cayenne.access.translator.procedure.ProcedureTranslator;
import org.apache.cayenne.query.ProcedureQuery;

/**
 * @since 3.1
 */
class DB2ProcedureAction extends ProcedureAction {

	DB2ProcedureAction(ProcedureQuery query, DataNode dataNode) {
		super(query, dataNode);
	}

	@Override
	public void performAction(Connection connection, OperationObserver observer) throws SQLException, Exception {

		// cloned from super except for result processing consistent with
		// CAY-1874

		processedResultSets = 0;

		ProcedureTranslator transl = createTranslator(connection);

		try (CallableStatement statement = (CallableStatement) transl.createStatement();) {
			initStatement(statement);
			boolean hasResultSet = statement.execute();

			// read out parameters
			readProcedureOutParameters(statement, observer);

			// read the rest of the query
			while (true) {
				if (hasResultSet) {

					try (ResultSet rs = statement.getResultSet();) {
						RowDescriptor descriptor = describeResultSet(rs, processedResultSets++);
						readResultSet(rs, descriptor, query, observer);
					}
				} else {
					int updateCount = statement.getUpdateCount();
					if (updateCount == -1) {
						break;
					}
					dataNode.getJdbcEventLogger().logUpdateCount(updateCount);
					observer.nextCount(query, updateCount);
				}
				hasResultSet = statement.getMoreResults();
			}
		}
	}
}
