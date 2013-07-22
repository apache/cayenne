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

    public void log(String message) {
    }

    public void logConnect(String dataSource) {
    }

    public void logConnect(String url, String userName, String password) {
    }

    public void logPoolCreated(DataSourceInfo dsi) {
    }

    public void logConnectSuccess() {
    }

    public void logConnectFailure(Throwable th) {
    }

    public void logGeneratedKey(DbAttribute attribute, Object value) {
    }

    public void logQuery(String sql, List<?> params) {
    }

    public void logQuery(String sql, List<DbAttribute> attrs, List<?> params, long time) {
    }

    public void logQueryParameters(
            String label,
            List<DbAttribute> attrs,
            List<Object> parameters,
            boolean isInserting) {
    }

    public void logSelectCount(int count, long time) {
    }

    public void logSelectCount(int count, long time, String sql) {
    }
	
    public void logUpdateCount(int count) {
    }

    public void logBeginTransaction(String transactionLabel) {
    }

    public void logCommitTransaction(String transactionLabel) {
    }

    public void logRollbackTransaction(String transactionLabel) {
    }

    public void logQueryError(Throwable th) {
    }

    public boolean isLoggable() {
        return false;
    }
}
