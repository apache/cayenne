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
package org.apache.cayenne.dba.mysql;

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

public class MySQLSelectActionTest {

    private TestConnection connection;
    private DataNode dataNode;

    @BeforeEach
    public void setUp() {
        dataNode = DataNodes.withAdapter(TestAdapters.jdbc(), (columns, segments, query, adapter) -> rs -> null);
        connection = new TestConnection();
    }

    private void execute(int statementFetchSize, boolean iterated, boolean exclusiveConnection) throws Exception {
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
        context.setIteratedResult(iterated);
        context.setExclusiveConnection(exclusiveConnection);

        new MySQLSelectAction(select, dataNode).execute(connection, context);
    }

    @Test
    public void noFetchSizeOnListResultLeavesDriverDefault() throws Exception {
        execute(0, false, false);
        assertEquals(List.of("executeQuery", "statement.close"), connection.getCalls());
    }

    @Test
    public void noFetchSizeOnIteratorWithExclusiveConnectionStreams() throws Exception {
        execute(0, true, true);
        assertEquals(List.of("setFetchSize(" + Integer.MIN_VALUE + ")", "executeQuery"), connection.getCalls());
    }

    @Test
    public void noFetchSizeOnIteratorInCallerTransactionLeavesDriverDefault() throws Exception {
        execute(0, true, false);
        assertEquals(List.of("executeQuery"), connection.getCalls());
    }

    @Test
    public void streamingFetchSizeOnListResultIsPassedThrough() throws Exception {
        execute(Integer.MIN_VALUE, false, false);
        assertEquals(List.of("setFetchSize(" + Integer.MIN_VALUE + ")", "executeQuery", "statement.close"),
                connection.getCalls());
    }

    @Test
    public void streamingFetchSizeOnIteratorInCallerTransactionIsPassedThrough() throws Exception {
        execute(Integer.MIN_VALUE, true, false);
        assertEquals(List.of("setFetchSize(" + Integer.MIN_VALUE + ")", "executeQuery"), connection.getCalls());
    }

    @Test
    public void positiveFetchSizeOnIteratorWithExclusiveConnectionIsPassedThrough() throws Exception {
        execute(100, true, true);
        assertEquals(List.of("setFetchSize(100)", "executeQuery"), connection.getCalls());
    }
}
