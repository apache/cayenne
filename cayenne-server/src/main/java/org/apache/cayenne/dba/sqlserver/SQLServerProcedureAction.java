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

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.jdbc.ProcedureAction;
import org.apache.cayenne.access.jdbc.RowDescriptor;
import org.apache.cayenne.access.translator.procedure.ProcedureTranslator;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.Query;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * ProcedureAction for SQLServer MS JDBC driver. Customizes OUT parameter
 * processing - it has to be done AFTER the ResultSets are read (note that jTDS
 * driver works fine with normal ProcedureAction).
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
	public SQLServerProcedureAction(ProcedureQuery query, DataNode dataNode) {
		super(query, dataNode);
	}

	@Override
	public void performAction(Connection connection, OperationObserver observer) throws SQLException, Exception {

		ProcedureTranslator transl = createTranslator(connection);

		try (CallableStatement statement = (CallableStatement) transl.createStatement();) {
			// stored procedure may contain a mixture of update counts and
			// result sets,
			// and out parameters. Read out parameters first, then
			// iterate until we exhaust all results
			boolean hasResultSet = statement.execute();

			// local observer to cache results and provide them to the external
			// observer
			// in the order consistent with other adapters.

			Observer localObserver = new Observer(observer);

			// read query, using local observer

			while (true) {
				if (hasResultSet) {

					try (ResultSet rs = statement.getResultSet();) {
						RowDescriptor descriptor = describeResultSet(rs, processedResultSets++);
						readResultSet(rs, descriptor, query, localObserver);
					}
				} else {
					int updateCount = statement.getUpdateCount();
					if (updateCount == -1) {
						break;
					}
					dataNode.getJdbcEventLogger().logUpdateCount(updateCount);
					localObserver.nextCount(query, updateCount);
				}

				hasResultSet = statement.getMoreResults();
			}

			// read out parameters to the main observer ... AFTER the main
			// result set
			// TODO: I hope SQLServer does not support ResultSets as OUT
			// parameters,
			// otherwise
			// the order of custom result descriptors will be messed up
			readProcedureOutParameters(statement, observer);

			// add results back to main observer
			localObserver.flushResults(query);
		}
	}

	class Observer implements OperationObserver {

		List<List<?>> results;
		List<Integer> counts;
		OperationObserver observer;

		Observer(OperationObserver observer) {
			this.observer = observer;
		}

		void flushResults(Query query) {
			if (results != null) {
				for (List<?> result : results) {
					observer.nextRows(query, result);
				}
				results = null;
			}

			if (counts != null) {
				for (Integer count : counts) {
					observer.nextCount(query, count);
				}
				counts = null;
			}
		}

		@Override
		public void nextBatchCount(Query query, int[] resultCount) {
			observer.nextBatchCount(query, resultCount);
		}

		@Override
		public void nextCount(Query query, int resultCount) {
			// does not delegate to wrapped observer
			// but instead caches results locally.
			if (counts == null) {
				counts = new ArrayList<>();
			}

			counts.add(Integer.valueOf(resultCount));
		}

		@Override
		public void nextRows(Query query, List<?> dataRows) {
			// does not delegate to wrapped observer
			// but instead caches results locally.
			if (results == null) {
				results = new ArrayList<>();
			}

			results.add(dataRows);
		}

		@Override
		public void nextRows(Query q, ResultIterator it) {
			observer.nextRows(q, it);
		}

		@Override
		public void nextGlobalException(Exception ex) {
			observer.nextGlobalException(ex);
		}

		@Override
		public void nextGeneratedRows(Query query, ResultIterator<?> keys, List<ObjectId> idsToUpdate) {
			observer.nextGeneratedRows(query, keys, idsToUpdate);
		}

		@Override
		public void nextQueryException(Query query, Exception ex) {
			observer.nextQueryException(query, ex);
		}

		@Override
		public boolean isIteratedResult() {
			return observer.isIteratedResult();
		}
	}
}
