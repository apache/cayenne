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
import org.apache.cayenne.map.DbAttribute;

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
     * Logs the main line for a statement that returned a result set: SQL + {@code bind:[...]} + {@code selected:N}.
     *
     * @param statement the translated statement carrying SQL and bindings
     * @param rowCount  the number of selected rows
     */
    void logSelect(TranslatedStatement statement, int rowCount);

    /**
     * Logs the main line for a statement that performed an update.
     */
    void logUpdate(TranslatedStatement statement, int rowCount);

    /**
     * Logs a select count continuation line for the statement whose header was already logged.
     */
    void logAlsoSelect(int rowCount);

    /**
     * Logs an update count continuation line for the statement whose header was already logged.
     */
    void logAlsoUpdate(int rowCount);

    /**
     * Logs a database-generated primary key value.
     */
    void logGeneratedKey(DbAttribute attribute, Object value);

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
