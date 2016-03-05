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

import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.map.DbAttribute;

/**
 * @since 3.1
 */
public class NoopJdbcEventLogger implements JdbcEventLogger {

	private static final NoopJdbcEventLogger instance = new NoopJdbcEventLogger();

	public static NoopJdbcEventLogger getInstance() {
		return instance;
	}

	private NoopJdbcEventLogger() {

	}

	@Override
	public void log(String message) {
	}

	@Deprecated
	@Override
	public void logConnect(String dataSource) {
	}

	@Deprecated
	@Override
	public void logConnect(String url, String userName, String password) {
	}

	@Deprecated
	@Override
	public void logPoolCreated(DataSourceInfo dsi) {
	}

	@Deprecated
	@Override
	public void logConnectSuccess() {
	}

	@Deprecated
	@Override
	public void logConnectFailure(Throwable th) {
	}

	@Override
	public void logGeneratedKey(DbAttribute attribute, Object value) {
	}

	@Override
	public void logQuery(String sql, List<?> params) {
	}

	@Deprecated
	@Override
	public void logQuery(String sql, List<DbAttribute> attrs, List<?> params, long time) {
	}
	
	@Override
	public void logQuery(String sql, DbAttributeBinding[] bindings, long translatedIn) {
	}

	@Override
	@Deprecated
	public void logQueryParameters(String label, List<DbAttribute> attrs, List<Object> parameters, boolean isInserting) {
	}

	@Override
	public void logQueryParameters(String label, DbAttributeBinding[] bindings) {
	}

	@Override
	public void logSelectCount(int count, long time) {
	}

	@Override
	public void logSelectCount(int count, long time, String sql) {
	}

	@Override
	public void logUpdateCount(int count) {
	}

	@Override
	public void logBeginTransaction(String transactionLabel) {
	}

	@Override
	public void logCommitTransaction(String transactionLabel) {
	}

	@Override
	public void logRollbackTransaction(String transactionLabel) {
	}

	@Override
	public void logQueryError(Throwable th) {
	}

	@Override
	public boolean isLoggable() {
		return false;
	}
}
