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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.jdbc.RowDescriptorBuilder;
import org.apache.cayenne.access.jdbc.SQLStatement;
import org.apache.cayenne.access.jdbc.SQLTemplateAction;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.SQLTemplate;

/**
 * @since 3.0
 */
class OracleSQLTemplateAction extends SQLTemplateAction {

	protected DbEntity dbEntity;

	OracleSQLTemplateAction(SQLTemplate query, DataNode dataNode) {
		super(query, dataNode);
		this.dbEntity = query.getMetaData(dataNode.getEntityResolver()).getDbEntity();
	}

	@Override
	protected void processSelectResult(SQLStatement compiled, Connection connection, Statement statement,
			ResultSet resultSet, OperationObserver callback, long startTime) throws Exception {

		// wrap ResultSet to distinguish between Integer and BigDecimal for
		// Oracle NUMBER
		// columns...

		if (compiled.getResultColumns().length == 0) {
			resultSet = new OracleResultSetWrapper(resultSet);
		}

		super.processSelectResult(compiled, connection, statement, resultSet, callback, startTime);
	}

	/**
	 * @since 3.0
	 */
	@Override
	protected RowDescriptorBuilder configureRowDescriptorBuilder(SQLStatement compiled, ResultSet resultSet)
			throws SQLException {

		RowDescriptorBuilder builder = super.configureRowDescriptorBuilder(compiled, resultSet);

		return builder;
	}
}
