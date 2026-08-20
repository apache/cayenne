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

package org.apache.cayenne;

import org.apache.cayenne.access.translator.TranslatedStatement;
import org.apache.cayenne.query.Query;

/**
 * An exception thrown when a failure occurs while executing a query statement or reading its results. It carries the
 * {@link Query} being executed and the {@link TranslatedStatement} that was being executed at the time of the failure,
 * and includes the statement's SQL in the exception message, so that a failure can be correlated with a specific query.
 *
 * @since 5.0
 */
public class CayenneSqlException extends CayenneRuntimeException {

    private final transient Query query;
    private final transient TranslatedStatement statement;

    /**
     * Creates an exception for a failure that happened while executing the provided query and statement. The statement
     * may be null if the failure occurred before a statement was translated.
     */
    public CayenneSqlException(String message, Query query, TranslatedStatement statement, Throwable cause) {
        // pass the pre-built message as a "%s" argument so that any "%" characters in the SQL (e.g. LIKE patterns)
        // are not interpreted as format specifiers by the superclass
        super("%s", cause, buildMessage(message, statement));
        this.query = query;
        this.statement = statement;
    }

    /**
     * Returns the query that was being executed when the failure occurred, or null if it is not known.
     */
    public Query getQuery() {
        return query;
    }

    /**
     * Returns the statement that was being executed when the failure occurred, or null if it is not known.
     */
    public TranslatedStatement getStatement() {
        return statement;
    }

    private static String buildMessage(String message, TranslatedStatement statement) {
        String base = message != null ? message : "SQL failure";
        return statement != null
                ? base + " SQL: [" + statement.sql() + "]"
                : base;
    }
}
