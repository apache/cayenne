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

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.OutParametersResult;
import org.apache.cayenne.access.StatementContext;
import org.apache.cayenne.access.jdbc.RSColumn;
import org.apache.cayenne.access.jdbc.DataNodeProcedureAction;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.map.compiled.CompiledProcedureParameter;
import org.apache.cayenne.access.translator.TranslatedProcedure;

/**
 * Oracle-specific DataNodeProcedureAction that supports ResultSet OUT parameters.
 * 
 * @since 1.2
 */
class OracleProcedureAction extends DataNodeProcedureAction {

	OracleProcedureAction(TranslatedProcedure translated, DataNode dataNode) {
		super(translated, dataNode);
	}

	/**
	 * Helper method that reads OUT parameters of a CallableStatement.
	 */
	@Override
	protected void readProcedureOutParameters(CallableStatement statement, StatementContext context)
			throws SQLException, Exception {

		Map<String, Object> result = null;
		List<CompiledProcedureParameter> parameters = query.procedure().parameters();
		for (int i = 0; i < parameters.size(); i++) {
			CompiledProcedureParameter parameter = parameters.get(i);

			if (!parameter.isOutParameter()) {
				continue;
			}

			// ==== start Oracle-specific part
			if (parameter.jdbcType() == OracleAdapter.getOracleCursorType()) {

				try (ResultSet rs = (ResultSet) statement.getObject(i + 1);) {
					RSColumn[] rsColumns = describeResultSet(rs, processedResultSets++);
					readResultSet(rs, rsColumns, context);
				}
			}
			// ==== end Oracle-specific part
			else {
				if (result == null) {
					result = new LinkedHashMap<>();
				}

				ExtendedType type = dataNode.getAdapter().getExtendedTypes()
						.getRegisteredType(TypesMapping.getJavaBySqlType(parameter.jdbcType()));
				Object val = type.materializeObject(statement, i + 1, parameter.jdbcType());

				result.put(parameter.name(), val);
			}
		}

		if (result != null) {
			context.addResult(new OutParametersResult(result));
		}
	}
}
