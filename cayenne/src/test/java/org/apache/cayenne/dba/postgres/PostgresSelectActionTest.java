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

package org.apache.cayenne.dba.postgres;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.StatementContext;
import org.apache.cayenne.access.jdbc.PSParameter;
import org.apache.cayenne.access.jdbc.RSColumn;
import org.apache.cayenne.access.translator.TranslatedSelect;
import org.apache.cayenne.map.compiled.TestMappings;
import org.apache.cayenne.query.resolved.ResolvedQueries;
import org.apache.cayenne.query.resolved.ResolvedSelect;
import org.apache.cayenne.unit.DataNodes;
import org.apache.cayenne.unit.TestAdapters;
import org.apache.cayenne.unit.jdbc.TestConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks the local transaction that PostgresSelectAction opens around a select. pgjdbc uses a server-side cursor
 * for "statementFetchSize" only inside a transaction, so an autocommit connection must be switched for the duration
 * of the query (CAY-3011), the same way it already is for LOB reads.
 */
public class PostgresSelectActionTest {

    private TestConnection connection;
    private DataNode dataNode;

    @BeforeEach
    public void setUp() {
        dataNode = DataNodes.withAdapter(TestAdapters.jdbc(), (columns, segments, query, adapter) -> rs -> null);
        connection = new TestConnection();
    }

    private void execute(int statementFetchSize) throws Exception {
        ResolvedSelect query = ResolvedQueries.select(statementFetchSize);
        TranslatedSelect select = new TranslatedSelect(
                query,
                "SELECT 1",
                new PSParameter[0],
                new RSColumn[0],
                null,
                false,
                false);
        StatementContext context = new StatementContext(query, TestMappings.empty());
        new PostgresSelectAction(select, dataNode).execute(connection, context);
    }

    @Test
    public void fetchSizeOnAutoCommitConnectionRunsInTransaction() throws Exception {
        connection.autoCommit(true);

        execute(10);

        assertEquals(List.of(
                "setAutoCommit(false)",
                "setFetchSize(10)",
                "executeQuery",
                "statement.close",
                "commit",
                "setAutoCommit(true)"), connection.getCalls());
    }

    @Test
    public void noFetchSizeOnAutoCommitConnectionLeavesAutoCommitAlone() throws Exception {
        connection.autoCommit(true);

        execute(0);

        assertEquals(List.of("executeQuery", "statement.close"), connection.getCalls());
    }

    @Test
    public void fetchSizeInsideExistingTransactionIsNotCommittedLocally() throws Exception {
        connection.autoCommit(false);

        execute(10);

        // the transaction belongs to the caller: the fetch size is passed through, but nothing is committed here
        assertEquals(List.of("setFetchSize(10)", "executeQuery", "statement.close"), connection.getCalls());
    }

    @Test
    public void failureInTransactionRollsBackAndRestoresAutoCommit() {
        connection.autoCommit(true).failQuery(new RuntimeException("boom"));

        assertThrows(RuntimeException.class, () -> execute(10));

        assertEquals(List.of(
                "setAutoCommit(false)",
                "setFetchSize(10)",
                "executeQuery",
                "statement.close",
                "rollback",
                "setAutoCommit(true)"), connection.getCalls());
    }
}
