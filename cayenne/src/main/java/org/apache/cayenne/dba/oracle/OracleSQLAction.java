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
package org.apache.cayenne.dba.oracle;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.StatementContext;
import org.apache.cayenne.access.jdbc.DataNodeSQLAction;
import org.apache.cayenne.access.translator.TranslatedSQL;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * @since 3.0
 */
class OracleSQLAction extends DataNodeSQLAction {

	OracleSQLAction(TranslatedSQL translated, DataNode dataNode) {
		super(translated, dataNode);
	}

	@Override
	protected void processSelectResult(Connection connection, Statement statement, ResultSet resultSet,
									   StatementContext context, long startTime) throws Exception {

		// wrap ResultSet to distinguish between Integer and BigDecimal for
		// Oracle NUMBER
		// columns...

		if (translated.resultColumns().length == 0) {
			resultSet = new OracleResultSetWrapper(resultSet);
		}

		super.processSelectResult(connection, statement, resultSet, context, startTime);
	}
}
