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
package org.apache.cayenne.log;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * A {@link JdbcEventLogger} built on top of slf4j-api logger.
 * 
 * @since 3.1
 * @since 4.0 renamed from CommonsJdbcEventLogger to Slf4jJdbcEventLogger as part of migration to SLF4J
 */
public class Slf4jJdbcEventLogger implements JdbcEventLogger {

	private static final Logger logger = LoggerFactory.getLogger(JdbcEventLogger.class);

	protected long queryExecutionTimeLoggingThreshold;

	public Slf4jJdbcEventLogger(@Inject RuntimeProperties runtimeProperties) {
		this.queryExecutionTimeLoggingThreshold = runtimeProperties.getLong(
				Constants.QUERY_EXECUTION_TIME_LOGGING_THRESHOLD_PROPERTY, 0);
	}

	@Override
	public void log(String message) {
		if (message != null) {
			logger.info(message);
		}
	}

	@Override
	public void logGeneratedKey(DbAttribute attribute, Object value) {
		if (isLoggable()) {
			String entity = attribute.getEntity().getName();
			logger.info("Generated PK: " + entity + "." + attribute.getName() + " = " + value);
		}
	}

	@Override
	public void logQuery(String sql, ParameterBinding[] bindings) {
		if (isLoggable()) {

			StringBuilder buffer = new StringBuilder(sql).append(" ");
			appendParameters(buffer, "bind", bindings);

			if (buffer.length() > 0) {
				logger.info(buffer.toString());
			}
		}
	}

	@Override
	public void logQueryParameters(String label, ParameterBinding[] bindings) {

		if (isLoggable() && bindings.length > 0) {

			StringBuilder buffer = new StringBuilder();
			appendParameters(buffer, label, bindings);

			if (buffer.length() > 0) {
				logger.info(buffer.toString());
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected void appendParameters(StringBuilder buffer, String label, ParameterBinding[] bindings) {

		int len = bindings.length;
		if (len > 0) {

			boolean hasIncluded = false;

			for (int i = 0, j = 1; i < len; i++) {
				ParameterBinding b = bindings[i];

				if (b.isExcluded()) {
					continue;
				}

				if (hasIncluded) {
					buffer.append(", ");
				} else {
					hasIncluded = true;
					buffer.append("[").append(label).append(": ");
				}


				buffer.append(j++);

				if(b instanceof DbAttributeBinding) {
					DbAttribute attribute = ((DbAttributeBinding) b).getAttribute();
					if (attribute != null) {
						buffer.append("->");
						buffer.append(attribute.getName());
					}
				}

				buffer.append(":");

				if (b.getExtendedType() != null) {
					buffer.append(b.getExtendedType().toString(b.getValue()));
				} else if(b.getValue() == null) {
				    buffer.append("NULL");
                } else {
					buffer.append(b.getValue().getClass().getName())
                            .append("@")
                            .append(System.identityHashCode(b.getValue()));
				}
			}

			if (hasIncluded) {
				buffer.append("]");
			}
		}
	}

	@Override
	public void logSelectCount(int count, long time) {
		logSelectCount(count, time, null);
	}

	@Override
	public void logSelectCount(int count, long time, String sql) {

		if (isLoggable()) {
			StringBuilder buf = new StringBuilder();

			if (count == 1) {
				buf.append("=== returned 1 row.");
			} else {
				buf.append("=== returned ").append(count).append(" rows.");
			}

			if (time >= 0) {
				buf.append(" - took ").append(time).append(" ms.");
			}

			logger.info(buf.toString());
		}

		if (queryExecutionTimeLoggingThreshold > 0 && time > queryExecutionTimeLoggingThreshold) {
			String message = "Query time exceeded threshold (" + time + " ms): ";
			logger.warn(message + sql, new CayenneRuntimeException(message + "%s", sql));
		}
	}

	@Override
	public void logUpdateCount(int count) {
		if (isLoggable()) {
			if (count < 0) {
				logger.info("=== updated ? rows");
			} else {
				String countStr = (count == 1) ? "=== updated 1 row." : "=== updated " + count + " rows.";
				logger.info(countStr);
			}
		}
	}

	@Override
	public void logBeginTransaction(String transactionLabel) {
		logger.info("--- " + transactionLabel);
	}

	@Override
	public void logCommitTransaction(String transactionLabel) {
		logger.info("+++ " + transactionLabel);
	}

	@Override
	public void logRollbackTransaction(String transactionLabel) {
		logger.info("*** " + transactionLabel);
	}

	@Override
	public void logQueryError(Throwable th) {
		if (isLoggable()) {
			if (th != null) {
				th = Util.unwindException(th);
			}

			logger.info("*** error.", th);

			if (th instanceof SQLException) {
				SQLException sqlException = ((SQLException) th).getNextException();
				while (sqlException != null) {
					logger.info("*** nested SQL error.", sqlException);
					sqlException = sqlException.getNextException();
				}
			}
		}
	}

	@Override
	public boolean isLoggable() {
		return logger.isInfoEnabled();
	}
}
