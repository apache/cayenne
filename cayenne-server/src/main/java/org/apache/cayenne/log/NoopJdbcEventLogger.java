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

import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.map.DbAttribute;

/**
 * Noop implementation of JdbcEventLogger
 *
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

	@Override
	public void logGeneratedKey(DbAttribute attribute, Object value) {
	}
	
	@Override
	public void logQuery(String sql, ParameterBinding[] bindings) {
	}

	@Override
	public void logQueryParameters(String label, ParameterBinding[] bindings) {
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
