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
 * A logging service used by Cayenne to output database interactions as compact, single-line messages.
 *
 * @since 5.0
 */
public interface SqlLogger {

    /**
     * Returns true if statement logging is enabled. Callers should skip the work of assembling results when this
     * returns false.
     */
    boolean isEnabled();

    /**
     * Logs the main line for a statement that returned a result set: SQL + {@code bind:[...]} + {@code selected:N},
     * followed by a trailing {@code time_ms:N} block with the statement's execution time.
     *
     * @param statement      the translated statement carrying SQL and bindings
     * @param rowCount       the number of selected rows
     * @param durationMillis the statement's execution time in milliseconds
     */
    void logSelect(TranslatedStatement statement, int rowCount, long durationMillis);

    /**
     * Logs the main line for a statement that performed an update: SQL + {@code bind:[...]} + {@code updated:N},
     * optionally followed by a {@code generated:[...]} block listing any database-generated keys, and a trailing
     * {@code time_ms:N} block with the statement's execution time.
     *
     * @param statement      the translated statement carrying SQL and bindings
     * @param rowCount       the number of updated rows
     * @param generatedKeys  the database-generated keys of the inserted rows, or an empty list if none
     * @param durationMillis the statement's execution time in milliseconds
     */
    void logUpdate(TranslatedStatement statement, int rowCount, List<? extends Map<String, ?>> generatedKeys,
                   long durationMillis);

    /**
     * Logs, at the ERROR level, the statement that was in progress when a query exception occurred:
     * SQL + {@code bind:[...]} + the error message, followed by a trailing {@code time_ms:N} block with the time
     * elapsed until the failure.
     *
     * @param statement      the translated statement carrying SQL and bindings
     * @param error          the exception thrown while executing the statement
     * @param durationMillis the time in milliseconds elapsed until the failure
     */
    void logQueryError(TranslatedStatement statement, Throwable error, long durationMillis);

    /**
     * Logs a select count continuation line for the statement whose header was already logged.
     */
    void logAlsoSelect(int rowCount);

    /**
     * Logs an update count continuation line for the statement whose header was already logged.
     */
    void logAlsoUpdate(int rowCount);

    /**
     * Logs a transaction start boundary (emitted at DEBUG level).
     */
    void logTransactionStart();

    /**
     * Logs a transaction commit boundary (emitted at DEBUG level).
     */
    void logTransactionCommit();

    /**
     * Logs a transaction rollback boundary (emitted at DEBUG level).
     */
    void logTransactionRollback();

    /**
     * Logs an arbitrary message, such as DDL, PK-generation SQL, or adapter-detection notes.
     */
    void logMessage(String message);
}
