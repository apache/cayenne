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
import org.apache.cayenne.access.StatementContext;
import org.apache.cayenne.access.jdbc.RSColumn;
import org.apache.cayenne.access.jdbc.DataNodeSelectAction;
import org.apache.cayenne.access.jdbc.PSParameter;
import org.apache.cayenne.access.translator.TranslatedSelect;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * @since 3.0
 */
class PostgresSelectAction extends DataNodeSelectAction {

	PostgresSelectAction(TranslatedSelect translated, DataNode dataNode) {
		super(translated, dataNode);
	}


	@Override
	public void execute(Connection connection, StatementContext context) throws Exception {

		if (!connection.getAutoCommit() || !needsTransaction()) {
			super.execute(connection, context);
			return;
		}

		// manual tx management for the cases listed in "needsTransaction"
		connection.setAutoCommit(false);
		try {
			super.execute(connection, context);
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

	@Override
	protected RSColumn[] resultColumns(ResultSet rs) throws SQLException {
		return PostgresTimestampTzType.optimizeTimestampColumns(translated.resultColumns(), rs);
	}

	private boolean needsTransaction() {
		// Two things in pgjdbc only work inside a transaction, and are silently degraded in autocommit mode:
		//
		// 1. A fetch size. pgjdbc implements it with a server-side cursor (a portal), and the server closes portals
		//    when the transaction ends. The cursor is used only for a positive fetch size, hence the "> 0" check.
		// 2. Large objects. They are read through the LO API, which is bound to the current transaction.
		return query.statementFetchSize() > 0 || readsLargeObjects(translated);
	}

	private static boolean readsLargeObjects(TranslatedSelect translated) {
		for (RSColumn column : translated.resultColumns()) {
			if (isLargeObject(column.rsType())) {
				return true;
			}
		}
		// a large object bound as a parameter (e.g. in a qualifier) also needs a transaction
		for (PSParameter<?> binding : translated.bindings()) {
			if (isLargeObject(binding.psType())) {
				return true;
			}
		}
		return false;
	}

	private static boolean isLargeObject(int jdbcType) {
		return jdbcType == Types.BLOB || jdbcType == Types.CLOB || jdbcType == Types.NCLOB;
	}
}
