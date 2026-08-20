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

import org.apache.cayenne.access.translator.TranslatedStatement;

import java.util.List;
import java.util.Map;

/**
 * A no-op {@link SQLLogger}. Used as a null-object default in contexts that run without a configured logger, such as
 * schema-generation tools and tests. It always reports {@link #isEnabled()} as false; to actually disable logging in a
 * running application, set the {@code cayenne-sql} log level instead.
 *
 * @since 5.0
 */
public class NoopSQLLogger implements SQLLogger {

    private static final NoopSQLLogger instance = new NoopSQLLogger();

    public static NoopSQLLogger getInstance() {
        return instance;
    }

    private NoopSQLLogger() {
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void logSelect(TranslatedStatement statement, int rowCount, long durationMillis) {
    }

    @Override
    public void logUpdate(TranslatedStatement statement, int rowCount, List<? extends Map<String, ?>> generatedKeys,
                          long durationMillis) {
    }

    @Override
    public void logQueryError(TranslatedStatement statement, Throwable error, long durationMillis) {
    }

    @Override
    public void logAlsoSelect(int rowCount) {
    }

    @Override
    public void logAlsoUpdate(int rowCount) {
    }

    @Override
    public void logTransactionStart() {
    }

    @Override
    public void logTransactionCommit() {
    }

    @Override
    public void logTransactionRollback() {
    }

    @Override
    public void logMessage(String message) {
    }
}
