/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne.access.translator.procedure;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.access.translator.ProcedureParameterBinding;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.log.NoopJdbcEventLogger;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.query.ProcedureQuery;

/**
 * Stored procedure query translator.
 */
public class ProcedureTranslator {

	/**
	 * Helper class to make OUT and VOID parameters logger-friendly.
	 */
	static class NotInParam {

		protected String type;

		public NotInParam(String type) {
			this.type = type;
		}

		@Override
		public String toString() {
			return type;
		}
	}

	private static NotInParam OUT_PARAM = new NotInParam("[OUT]");

	protected ProcedureQuery query;
	protected Connection connection;
	protected DbAdapter adapter;
	protected EntityResolver entityResolver;
	protected List<ProcedureParameter> callParams;
	protected List<Object> values;
	protected JdbcEventLogger logger;

	public ProcedureTranslator() {
		this.logger = NoopJdbcEventLogger.getInstance();
	}

	public void setQuery(ProcedureQuery query) {
		this.query = query;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	public void setAdapter(DbAdapter adapter) {
		this.adapter = adapter;
	}

	/**
	 * @since 3.1
	 */
	public void setJdbcEventLogger(JdbcEventLogger logger) {
		this.logger = logger;
	}

	/**
	 * @since 3.1
	 */
	public JdbcEventLogger getJdbcEventLogger() {
		return logger;
	}

	/**
	 * @since 1.2
	 */
	public void setEntityResolver(EntityResolver entityResolver) {
		this.entityResolver = entityResolver;
	}

	/**
	 * Creates an SQL String for the stored procedure call.
	 */
	protected String createSqlString() {
		Procedure procedure = getProcedure();

		StringBuilder buf = new StringBuilder();

		int totalParams = callParams.size();

		// check if procedure returns values
		if (procedure.isReturningValue()) {
			totalParams--;
			buf.append("{? = call ");
		} else {
			buf.append("{call ");
		}

		buf.append(procedure.getFullyQualifiedName());

		if (totalParams > 0) {
			// unroll the loop
			buf.append("(?");

			for (int i = 1; i < totalParams; i++) {
				buf.append(", ?");
			}

			buf.append(")");
		}

		buf.append("}");
		return buf.toString();
	}

	/**
	 * Creates and binds a PreparedStatement to execute query SQL via JDBC.
	 */
	public PreparedStatement createStatement() throws Exception {
		this.callParams = getProcedure().getCallParameters();
		this.values = new ArrayList<>(callParams.size());

		initValues();
		String sqlStr = createSqlString();

		if (logger.isLoggable()) {
			// need to convert OUT/VOID parameters to loggable strings
			ParameterBinding[] parameterBindings = new ParameterBinding[values.size()];
			for (int i=0; i<values.size(); i++) {
				ProcedureParameter procedureParameter = callParams.get(i);
				Object value = values.get(i);
				if(value instanceof NotInParam) {
					value = value.toString();
				}
				parameterBindings[i] = new ParameterBinding(value,
						procedureParameter.getType(), procedureParameter.getPrecision());
			}
			logger.logQuery(sqlStr, parameterBindings);
		}
		CallableStatement stmt = connection.prepareCall(sqlStr);
		initStatement(stmt);
		return stmt;
	}

	public Procedure getProcedure() {
		return query.getMetaData(entityResolver).getProcedure();
	}

	public ProcedureQuery getProcedureQuery() {
		return query;
	}

	/**
	 * Set IN and OUT parameters.
	 */
	protected void initStatement(CallableStatement stmt) throws Exception {
		if (values != null && values.size() > 0) {
			List<ProcedureParameter> params = getProcedure().getCallParameters();

			int len = values.size();
			for (int i = 0; i < len; i++) {
				ProcedureParameter param = params.get(i);

				// !Stored procedure parameter can be both in and out
				// at the same time
				if (param.isOutParam()) {
					setOutParam(stmt, param, i + 1);
				}

				if (param.isInParameter()) {
					setInParam(stmt, param, values.get(i), i + 1);
				}
			}
		}
	}

	protected void initValues() {
		Map<String, ?> queryValues = getProcedureQuery().getParameters();

		// match values with parameters in the correct order.
		// make an assumption that a missing value is NULL
		// Any reason why this is bad?

		for (ProcedureParameter param : callParams) {

			if (param.getDirection() == ProcedureParameter.OUT_PARAMETER) {
				values.add(OUT_PARAM);
			} else {
				values.add(queryValues.get(param.getName()));
			}
		}
	}

	/**
	 * Sets a single IN parameter of the CallableStatement.
	 */
	protected void setInParam(
			CallableStatement stmt,
			ProcedureParameter param,
			Object val,
			int pos) throws Exception {
		ExtendedType extendedType = val != null
				? adapter.getExtendedTypes().getRegisteredType(val.getClass())
				: adapter.getExtendedTypes().getDefaultType();

		ProcedureParameterBinding binding = new ProcedureParameterBinding(param);
		binding.setStatementPosition(pos);
		binding.setValue(val);
		binding.setExtendedType(extendedType);
		adapter.bindParameter(stmt, binding);
	}

	/**
	 * Sets a single OUT parameter of the CallableStatement.
	 */
	protected void setOutParam(CallableStatement stmt, ProcedureParameter param, int pos)
			throws Exception {

		int precision = param.getPrecision();
		if (precision >= 0) {
			stmt.registerOutParameter(pos, param.getType(), precision);
		} else {
			stmt.registerOutParameter(pos, param.getType());
		}
	}
}
