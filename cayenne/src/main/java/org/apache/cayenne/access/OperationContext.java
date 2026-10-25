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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A mutable context of a single query operation that travels down the stack from the {@link ObjectContext} that
 * started it, through the {@link org.apache.cayenne.DataChannel}s and their query filters, to the {@link DataNode}s
 * that execute the SQL. It is created by the originating context with the query to run, and is filled in along the
 * way: query routing adds a {@link StatementContext} per query to execute, and the nodes and their SQL actions fill
 * those with translated statements and results. The response of the operation is what
 * {@link org.apache.cayenne.DataChannel#onQuery(OperationContext)} returns.
 *
 * @since 6.0
 */
public final class OperationContext {

    private ObjectContext context;
    private final Query query;
    private final QueryMetadata metadata;
    private final boolean iteratedResult;

    private boolean exclusiveConnection;
    private final Map<DataNode, List<StatementContext>> statementContexts;

    /**
     * Creates a context for a query started by an ObjectContext. The query metadata is resolved once here and
     * is available down the stack via {@link #getMetadata()}.
     *
     * @param context        the ObjectContext that runs the query.
     * @param query          the query to run.
     * @param resolver       the EntityResolver to resolve the query metadata with.
     * @param iteratedResult whether a select result is to be returned as a {@link org.apache.cayenne.ResultIterator}
     *                       instead of a list.
     */
    public OperationContext(ObjectContext context, Query query, EntityResolver resolver, boolean iteratedResult) {
        this.context = context;
        this.query = Objects.requireNonNull(query, "Null query");
        this.metadata = query.getMetaData(resolver);
        this.iteratedResult = iteratedResult;
        this.statementContexts = new LinkedHashMap<>();
    }

    /**
     * Returns the ObjectContext on whose behalf the operation is being run, i.e. the context that result objects are
     * registered with. For a query started in a nested context, this is the nested context until the operation
     * reaches the channel of its parent DataContext, and the parent from there on down the stack, as the parent
     * resolves the objects before handing them to the child.
     */
    public ObjectContext getContext() {
        return context;
    }

    /**
     * Sets the ObjectContext on whose behalf the operation is run from here on down the stack. Called by the channel
     * of a parent DataContext when a nested context's query passes through it.
     */
    public void setContext(ObjectContext context) {
        this.context = context;
    }

    /**
     * Returns the query the operation was started with. This is the query as seen by the ObjectContext; the queries
     * actually executed are the ones of the {@link #getStatementContexts() statements}.
     */
    public Query getQuery() {
        return query;
    }

    /**
     * Returns the metadata of the {@link #getQuery() query}.
     */
    public QueryMetadata getMetadata() {
        return metadata;
    }

    /**
     * Returns whether a select result is to be returned as a {@link org.apache.cayenne.ResultIterator} that the
     * caller reads and closes, instead of a list read in full.
     */
    public boolean isIteratedResult() {
        return iteratedResult;
    }

    /**
     * Returns whether an iterated result keeps its connection to itself until the ResultIterator is closed, so that
     * no other statement runs on that connection while the ResultSet is open. False if the connection belongs to a
     * caller-managed transaction, which may run other statements (queries, fault resolution, commits) while the
     * iterator is open. Only meaningful when {@link #isIteratedResult()} is true. Adapters use it to decide whether a
     * result can be streamed, as some drivers lock the connection for the duration of a streamed result.
     */
    public boolean isExclusiveConnection() {
        return exclusiveConnection;
    }

    /**
     * Records whether the iterated result has the connection to itself, on the operation and on the statements added
     * so far. Set by the DataDomain when it opens a transaction just for the iterator.
     */
    public void setExclusiveConnection(boolean exclusiveConnection) {
        this.exclusiveConnection = exclusiveConnection;
        for (List<StatementContext> nodeStatementContexts : statementContexts.values()) {
            for (StatementContext statementContext : nodeStatementContexts) {
                statementContext.setExclusiveConnection(exclusiveConnection);
            }
        }
    }

    /**
     * Adds a statement to be executed on a node, setting its iterator flags to those of the operation. Called during
     * query routing for each query to be executed; each {@link DataNode} later executes its statements in this
     * order.
     */
    public void addStatementContext(DataNode node, StatementContext statementContext) {
        Objects.requireNonNull(node, "Null node");
        Objects.requireNonNull(statementContext, "Null statementContext");

        statementContext.setIteratedResult(iteratedResult);
        statementContext.setExclusiveConnection(exclusiveConnection);
        statementContexts.computeIfAbsent(node, n -> new ArrayList<>(1)).add(statementContext);
    }

    /**
     * Returns the statements by the node that executes them. The nodes are in the order of their first statement,
     * the statements of a node in the order they were added.
     */
    public Map<DataNode, List<StatementContext>> getStatementContexts() {
        return statementContexts;
    }

    /**
     * Removes all statements. A query filter that runs the filter chain more than once for the same operation (e.g. to
     * retry after a transient failure) must call this before each repeated run, as the response is assembled from
     * the statements, and the ones of the previous attempt would otherwise be included.
     */
    public void clearStatementContexts() {
        statementContexts.clear();
    }
}
