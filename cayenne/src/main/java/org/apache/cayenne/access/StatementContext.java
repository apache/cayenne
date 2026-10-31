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


package org.apache.cayenne.access;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.QueryResult;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.SelectResult;
import org.apache.cayenne.access.translator.TranslatedStatement;
import org.apache.cayenne.log.SQLLogger;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The context of a single statement: the execution of one {@link Query} on a {@link DataNode}. Within a query
 * operation it is created by query routing and added to the {@link OperationContext}, which sets the iterator flags;
 * a flush or a primary key generation create standalone statements. The executing node attaches its SQL logger, and
 * the {@link org.apache.cayenne.query.SQLAction} fills the context in: it reports the translated statement before
 * executing it, and then every result the statement produces, in the order produced.
 *
 * @since 6.0
 */
public final class StatementContext {

    private final Query<?> query;
    private final QueryMetadata metadata;
    private final EntityResolver resolver;
    private StatementLog log;
    private boolean iteratedResult;
    private boolean exclusiveConnection;
    private TranslatedStatement translated;
    private final List<QueryResult> results;

    /**
     * Creates a statement of a query, with a result read in full, i.e. not iterated.
     *
     * @param query    the query to execute.
     * @param metadata the metadata of the query, resolved with the resolver.
     * @param resolver the mapping the query is executed against, used by the SQL action to translate the query.
     */
    public StatementContext(Query<?> query, QueryMetadata metadata, EntityResolver resolver) {
        this.query = Objects.requireNonNull(query, "Null query");
        this.metadata = Objects.requireNonNull(metadata, "Null metadata");
        this.resolver = Objects.requireNonNull(resolver, "Null resolver");
        this.results = new ArrayList<>(1);
    }

    /**
     * Returns the query of this statement. For a routed query this is the query that was routed, which may differ from
     * the query the operation was started with, e.g. a {@link org.apache.cayenne.query.PrefetchSelectQuery}, a batch
     * query of a flush, or the query that an indirect query delegates to.
     */
    public Query<?> getQuery() {
        return query;
    }

    /**
     * Returns the metadata of the {@link #getQuery() query}. Unlike {@link OperationContext#getMetadata()}, this is
     * the metadata of the query actually executed, e.g. of a prefetch query.
     */
    public QueryMetadata getMetadata() {
        return metadata;
    }

    /**
     * Returns the mapping the statement is executed against, i.e. the EntityResolver of the DataDomain that routed
     * the query to the node.
     */
    public EntityResolver getResolver() {
        return resolver;
    }

    /**
     * Returns whether a select result is to be reported as a {@link ResultIterator} instead of a list.
     *
     * @see OperationContext#isIteratedResult()
     */
    public boolean isIteratedResult() {
        return iteratedResult;
    }

    /**
     * Sets whether a select result is to be reported as a {@link ResultIterator} instead of a list. Set by the
     * {@link OperationContext} the statement is added to.
     */
    public void setIteratedResult(boolean iteratedResult) {
        this.iteratedResult = iteratedResult;
    }

    /**
     * Returns whether an iterated result has the connection to itself until it is closed.
     *
     * @see OperationContext#isExclusiveConnection()
     */
    public boolean isExclusiveConnection() {
        return exclusiveConnection;
    }

    /**
     * Sets whether an iterated result has the connection to itself until it is closed. Set by the
     * {@link OperationContext} the statement is added to.
     */
    public void setExclusiveConnection(boolean exclusiveConnection) {
        this.exclusiveConnection = exclusiveConnection;
    }

    /**
     * Returns the translated form of the query (its SQL and bindings), or null if the query was not translated yet,
     * or its translation failed.
     */
    public TranslatedStatement getTranslated() {
        return translated;
    }

    /**
     * Returns the results reported so far, in the order they were produced.
     */
    public List<QueryResult> getResults() {
        return results;
    }

    /**
     * Sets the SQL logger that the statement reports to: a header line with the first result, continuation lines for
     * the results that follow, and an error line if the statement fails. Called by the {@link DataNode} before
     * executing the statement. A disabled logger turns logging off.
     */
    public void setLogger(SQLLogger logger) {
        this.log = logger != null && logger.isEnabled() ? new StatementLog(logger) : null;
    }

    /**
     * Sets the translated form of the query (its SQL and bindings). Called by the SQL action just before the
     * statement is executed, so that a failure and the results can be correlated with the statement. Starts the
     * execution timer of the statement log.
     */
    public void setTranslated(TranslatedStatement statement) {
        this.translated = statement;
        if (log != null) {
            log.statement(statement);
        }
    }

    /**
     * Appends a result produced by the statement: a result set read in full, a {@link ResultIterator} over an open
     * result set, update counts, or the OUT parameters of a procedure call. With SQL logging on, a ResultIterator is
     * stored and later returned wrapped in an iterator that logs the row count when it is closed.
     */
    public void addResult(QueryResult result) {
        results.add(log != null ? log.result(result) : result);
    }

    /**
     * Appends the keys the database generated for the inserted rows, as a {@link SelectResult} of one
     * {@link DataRow} per row. Unlike {@link #addResult(QueryResult)}, this does not log a result set, but reports
     * the keys on the statement's update line instead.
     */
    public void addGeneratedKeys(List<DataRow> keys) {
        if (log != null) {
            log.generatedKeys(keys);
        }
        results.add(new SelectResult<>(keys));
    }

    /**
     * Emits the pending update line of a batch to the SQL log. Called by the {@link DataNode} after the SQL action
     * returns.
     */
    public void logComplete() {
        if (log != null) {
            log.complete();
        }
    }

    /**
     * Emits an error line for the translated statement, if any, to the SQL log. Called by the {@link DataNode} when
     * the SQL action throws.
     */
    public void logError(Throwable error) {
        if (log != null) {
            log.error(error);
        }
    }
}
