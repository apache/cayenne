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
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.resolved.ResolvedQuery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The context of a single query operation, passed down the channel chain from the ObjectContext that started it. It
 * holds the user's query and its {@link ResolvedQuery resolved form} (the query is resolved once, when the context is
 * created), and collects the statements the operation runs, grouped by DataNode.
 *
 * @since 6.0
 */
public final class QueryContext {

    private ObjectContext context;
    private final Query<?> query;
    private final ResolvedQuery resolvedQuery;
    private final boolean iteratedResult;

    private boolean exclusiveConnection;
    private final Map<DataNode, List<StatementContext>> statementContexts;

    public QueryContext(ObjectContext context, Query<?> query, ResolvedQuery resolvedQuery, boolean iteratedResult) {
        this.context = context;
        this.query = query;
        this.resolvedQuery = Objects.requireNonNull(resolvedQuery, "Null resolved query");
        this.iteratedResult = iteratedResult;
        this.statementContexts = new LinkedHashMap<>();
    }

    /**
     * Returns the ObjectContext the operation currently runs in. It changes as the operation descends from a nested
     * context to its parent.
     */
    public ObjectContext getContext() {
        return context;
    }

    public void setContext(ObjectContext context) {
        this.context = context;
    }

    /**
     * Returns the query the operation was started with. Null for an operation that has no user query behind it.
     */
    public Query<?> getQuery() {
        return query;
    }

    /**
     * Returns the resolved form of the query, that the operation routes, translates and executes.
     */
    public ResolvedQuery getResolvedQuery() {
        return resolvedQuery;
    }

    /**
     * Returns whether the operation returns an open ResultIterator instead of a materialized result.
     */
    public boolean isIteratedResult() {
        return iteratedResult;
    }

    /**
     * Returns whether the operation owns its connection exclusively, which is the case for an iterated result whose
     * transaction is owned by the iterator.
     */
    public boolean isExclusiveConnection() {
        return exclusiveConnection;
    }

    public void setExclusiveConnection(boolean exclusiveConnection) {
        this.exclusiveConnection = exclusiveConnection;
        for (List<StatementContext> nodeStatementContexts : statementContexts.values()) {
            for (StatementContext statementContext : nodeStatementContexts) {
                statementContext.setExclusiveConnection(exclusiveConnection);
            }
        }
    }

    /**
     * Adds a statement to run on a given node. The statement inherits the iterated and exclusive-connection flags of
     * the operation.
     */
    public void addStatementContext(DataNode node, StatementContext statementContext) {
        Objects.requireNonNull(node, "Null node");
        Objects.requireNonNull(statementContext, "Null statementContext");

        statementContext.setIteratedResult(iteratedResult);
        statementContext.setExclusiveConnection(exclusiveConnection);
        statementContexts.computeIfAbsent(node, n -> new ArrayList<>(1)).add(statementContext);
    }

    /**
     * Returns the statements of the operation grouped by node, in the order they were added.
     */
    public Map<DataNode, List<StatementContext>> getStatementContexts() {
        return statementContexts;
    }

    public void clearStatementContexts() {
        statementContexts.clear();
    }
}
