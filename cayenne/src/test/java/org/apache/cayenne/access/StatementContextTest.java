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
import org.apache.cayenne.DataRowLayout;
import org.apache.cayenne.OutParametersResult;
import org.apache.cayenne.QueryResult;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.SelectResult;
import org.apache.cayenne.UpdateResult;
import org.apache.cayenne.access.translator.TranslatedSQL;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SQLTemplate;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

public class StatementContextTest {

    private static StatementContext context() {
        return new StatementContext(new SQLTemplate<>("SELECT 1"), mock(QueryMetadata.class));
    }

    @Test
    public void standaloneStatementIsNotIterated() {
        StatementContext statementContext = context();

        assertFalse(statementContext.isIteratedResult());
        assertFalse(statementContext.isExclusiveConnection());
        assertNull(statementContext.getTranslated());
        assertTrue(statementContext.getResults().isEmpty());
    }

    @Test
    public void translated() {
        StatementContext context = context();
        TranslatedSQL statement = new TranslatedSQL("SELECT 1", null, null);

        context.setTranslated(statement);

        assertSame(statement, context.getTranslated());
    }

    @Test
    public void resultsKeptInOrder() {
        StatementContext context = context();

        SelectResult<Object> select = new SelectResult<>(List.of(new Object()));
        UpdateResult update = new UpdateResult(new int[]{1});
        OutParametersResult out = new OutParametersResult(Map.of("p", 1));
        ResultIterator<?> iterator = mock(ResultIterator.class);

        context.addResult(select);
        context.addResult(update);
        context.addResult(out);
        context.addResult(iterator);

        List<QueryResult> results = context.getResults();
        assertEquals(4, results.size());
        assertSame(select, results.get(0));
        assertSame(update, results.get(1));
        assertSame(out, results.get(2));
        assertSame(iterator, results.get(3));
    }

    @Test
    public void generatedKeysAppendedAsSelectResult() {
        StatementContext context = context();
        DataRow key = DataRow.of("E", DataRowLayout.of("ID"), new Object[]{7L}, SnapshotVersion.next());

        context.addResult(new UpdateResult(new int[]{1}));
        context.addGeneratedKeys(List.of(key));

        assertEquals(2, context.getResults().size());
        assertEquals(new SelectResult<>(List.of(key)), context.getResults().get(1));
    }
}
