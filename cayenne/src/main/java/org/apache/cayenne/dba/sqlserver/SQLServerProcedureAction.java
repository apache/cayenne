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

package org.apache.cayenne.dba.sqlserver;

import org.apache.cayenne.SelectResult;
import org.apache.cayenne.UpdateResult;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.StatementContext;
import org.apache.cayenne.access.jdbc.ProcedureAction;
import org.apache.cayenne.access.jdbc.RSColumn;
import org.apache.cayenne.access.translator.TranslatedProcedure;
import org.apache.cayenne.query.ProcedureQuery;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * ProcedureAction for SQLServer MS JDBC driver. Customizes OUT parameter processing - it has to be done AFTER the
 * ResultSets are read (note that jTDS driver works fine with normal ProcedureAction).
 * <p>
 * <i>See JIRA CAY-251 for details. </i>
 * </p>
 *
 * @since 1.2
 */
public class SQLServerProcedureAction extends ProcedureAction {

	/**
	 * @since 4.0
	 */
	public SQLServerProcedureAction(ProcedureQuery<?> query, DataNode dataNode) {
		super(query, dataNode);
	}

	@Override
	public void performAction(Connection connection, StatementContext context) throws Exception {

		TranslatedProcedure translated = dataNode.getProcedureTranslator()
				.translate(query, dataNode.getAdapter(), dataNode.getEntityResolver());

		context.setTranslated(translated);

		try (CallableStatement statement = connection.prepareCall(translated.sql())) {
			bindParameters(statement, translated);

			// stored procedure may contain a mixture of update counts and
			// result sets,
			// and out parameters. Read out parameters first, then
			// iterate until we exhaust all results
			boolean hasResultSet = statement.execute();

			// buffer the results locally to report them in the order consistent with other adapters: all result sets
			// first, then all update counts, both after the OUT parameters
			List<List<?>> results = new ArrayList<>();
			List<Integer> counts = new ArrayList<>();

			while (true) {
				if (hasResultSet) {
					try (ResultSet rs = statement.getResultSet()) {
						RSColumn[] columns = describeResultSet(rs, processedResultSets++);
						if (context.isIteratedResult()) {
							// an iterator is handed over as is, and can't be reordered
							readResultSet(rs, columns, context);
						} else {
							results.add(resultIterator(rs, columns, context).allRows());
						}
					}
				} else {
					int updateCount = statement.getUpdateCount();
					if (updateCount == -1) {
						break;
					}
					counts.add(updateCount);
				}

				hasResultSet = statement.getMoreResults();
			}

			// read out parameters ... AFTER the main result set
			// TODO: I hope SQLServer does not support ResultSets as OUT
			// parameters,
			// otherwise
			// the order of custom result descriptors will be messed up
			readProcedureOutParameters(statement, context);

			// report the buffered results
			for (List<?> result : results) {
				context.addResult(new SelectResult<>(result));
			}
			for (Integer count : counts) {
				context.addResult(new UpdateResult(new int[]{count}));
			}
		}
	}
}
