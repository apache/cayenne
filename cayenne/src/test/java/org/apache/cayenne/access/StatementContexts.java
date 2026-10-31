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

import org.apache.cayenne.QueryResult;
import org.apache.cayenne.SelectResult;
import org.apache.cayenne.UpdateResult;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;

import java.util.List;

/**
 * Test helpers to execute a single query on a {@link DataNode} and read the results of its {@link StatementContext}.
 */
public final class StatementContexts {

    private StatementContexts() {
    }

    /**
     * Creates a statement context for the query on the node without executing it, for tests that invoke a SQLAction
     * directly.
     */
    public static StatementContext newRun(DataNode node, EntityResolver resolver, Query<?> query) {
        return new StatementContext(query, query.getMetaData(resolver), resolver);
    }

    /**
     * Executes the query on the node, returning its statement context with the results.
     */
    public static StatementContext run(DataNode node, EntityResolver resolver, Query<?> query) {
        StatementContext statementContext = newRun(node, resolver, query);
        node.execute(List.of(statementContext));
        return statementContext;
    }

    /**
     * Returns the rows of the first select result of the statement, or null if there is none.
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> rows(StatementContext statementContext) {
        for (QueryResult result : statementContext.getResults()) {
            if (result instanceof SelectResult<?> select) {
                return (List<T>) select.objects();
            }
        }
        return null;
    }

    /**
     * Returns the counts of the first update result of the statement, or null if there is none.
     */
    public static int[] counts(StatementContext statementContext) {
        for (QueryResult result : statementContext.getResults()) {
            if (result instanceof UpdateResult(int[] counts)) {
                return counts;
            }
        }
        return null;
    }
}
