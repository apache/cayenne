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
package org.apache.cayenne.dba.postgres;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.jdbc.SelectAction;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.access.translator.select.TranslatedSelect;
import org.apache.cayenne.query.Select;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

/**
 * @since 3.0
 */
class PostgresSelectAction extends SelectAction {

	<T> PostgresSelectAction(Select<T> query, DataNode dataNode) {
		super(query, dataNode);
	}

	@Override
	protected int getInMemoryOffset(int queryOffset) {
		return 0;
	}

	@Override
	protected void performAction(Connection connection, OperationObserver observer, TranslatedSelect translated) throws Exception {

		if (!connection.getAutoCommit() || !readsLargeObjects(translated)) {
			super.performAction(connection, observer, translated);
			return;
		}

		// manual tx management for reading LOBs
		connection.setAutoCommit(false);
		try {
			super.performAction(connection, observer, translated);
			connection.commit();
		} catch (Exception e) {
			try {
				connection.rollback();
			} catch (SQLException ignored) {
				// connection is being returned/closed anyway
			}
			throw e;
		} finally {
			try {
				connection.setAutoCommit(true);
			} catch (SQLException ignored) {
				// connection is being returned/closed anyway
			}
		}
	}

	private static boolean readsLargeObjects(TranslatedSelect translated) {
		for (ColumnDescriptor column : translated.resultColumns()) {
			if (isLargeObject(column.getJdbcType())) {
				return true;
			}
		}
		// a large object bound as a parameter (e.g. in a qualifier) also needs a transaction
		for (ParameterBinding binding : translated.bindings()) {
			if (isLargeObject(binding.getJdbcType())) {
				return true;
			}
		}
		return false;
	}

	private static boolean isLargeObject(int jdbcType) {
		return jdbcType == Types.BLOB || jdbcType == Types.CLOB || jdbcType == Types.NCLOB;
	}
}
