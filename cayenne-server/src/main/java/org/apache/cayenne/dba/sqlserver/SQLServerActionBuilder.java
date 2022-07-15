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

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.JdbcActionBuilder;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.FluentSelect;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.SQLAction;

/**
 * @since 1.2
 */
public class SQLServerActionBuilder extends JdbcActionBuilder {

	/**
	 * Stores the major version of the database.
	 *
	 * @since 4.2
	 */
	private final Integer version;

	/**
	 * @param dataNode current data node
	 * @since 4.0
	 */
	public SQLServerActionBuilder(DataNode dataNode) {
		this(dataNode, null);
	}

	/**
	 * @param dataNode current data node
	 * @param version database server version
	 * @since 4.2
	 */
	public SQLServerActionBuilder(DataNode dataNode, Integer version) {
		super(dataNode);
		this.version = version;
	}

	@Override
	public SQLAction batchAction(BatchQuery query) {
		// check run strategy...

		// optimistic locking is not supported in batches due to JDBC driver limitations
		boolean useOptimisticLock = query.isUsingOptimisticLocking();
		boolean runningAsBatch = !useOptimisticLock && dataNode.getAdapter().supportsBatchUpdates();
		return new SQLServerBatchAction(query, dataNode, runningAsBatch);
	}

	@Override
	public SQLAction procedureAction(ProcedureQuery query) {
		return new SQLServerProcedureAction(query, dataNode);
	}

	/**
	 * @since 4.2
	 */
	@Override
	public <T> SQLAction objectSelectAction(FluentSelect<T, ?> query) {
		return new SQLServerSelectAction(query, dataNode, needInMemoryOffset(query));
	}

	private boolean needInMemoryOffset(FluentSelect<?, ?> query) {
		return query.getOrderings() == null || query.getOrderings().size() == 0
				|| version == null || version < 12;
	}
}
