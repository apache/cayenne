/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.log;

import java.util.List;

import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.map.DbAttribute;

/**
 * A logging service used by Cayenne to output database interactions.
 * 
 * @since 3.1
 */
public interface JdbcEventLogger {

	/**
	 * Logs an arbitrary message.
	 */
	void log(String message);

	/**
	 * Logs database connection event using container data source.
	 * 
	 * @deprecated since 4.0 connection events are logged by the DataSources
	 *             using their own logger.
	 */
	@Deprecated
	void logConnect(String dataSource);

	/**
	 * @deprecated since 4.0 connection events are logged by the DataSources
	 *             using their own logger.
	 */
	@Deprecated
	void logConnect(String url, String userName, String password);

	/**
	 * @deprecated since 4.0 connection events are logged by the DataSources
	 *             using their own logger.
	 */
	@Deprecated
	void logPoolCreated(DataSourceInfo dsi);

	/**
	 * @deprecated since 4.0 connection events are logged by the DataSources
	 *             using their own logger.
	 */
	@Deprecated
	void logConnectSuccess();

	/**
	 * @deprecated since 4.0 connection events are logged by the DataSources
	 *             using their own logger.
	 */
	@Deprecated
	void logConnectFailure(Throwable th);

	void logGeneratedKey(DbAttribute attribute, Object value);

	/**
	 * @deprecated since 4.0 use
	 *             {@link #logQuery(String, ParameterBinding[], long)}.
	 */
	@Deprecated
	void logQuery(String sql, List<?> params);

	/**
	 * @deprecated since 4.0 use
	 *             {@link #logQuery(String, ParameterBinding[], long)}.
	 */
	@Deprecated
	void logQuery(String sql, List<DbAttribute> attrs, List<?> params, long time);

	/**
	 * @since 4.0
	 */
	void logQuery(String sql, ParameterBinding[] bindings, long translatedIn);

	/**
	 * @since 4.0
	 */
	void logQueryParameters(String label, ParameterBinding[] bindings);

	/**
	 * @deprecated since 4.0 in favor of
	 *             {@link #logQueryParameters(String, ParameterBinding[])}
	 */
	@Deprecated
	void logQueryParameters(String label, List<DbAttribute> attrs, List<Object> parameters, boolean isInserting);

	void logSelectCount(int count, long time);

	/**
	 * 
	 * @param count
	 * @param time
	 *            (milliseconds) time query took to run
	 * @param sql
	 *            SQL that was executed, printed when time exceeds timeThreshold
	 * 
	 * @since 4.0
	 */
	void logSelectCount(int count, long time, String sql);

	void logUpdateCount(int count);

	void logBeginTransaction(String transactionLabel);

	void logCommitTransaction(String transactionLabel);

	void logRollbackTransaction(String transactionLabel);

	void logQueryError(Throwable th);

	/**
	 * Returns true if current thread default log level is high enough to
	 * generate output.
	 */
	boolean isLoggable();
}
