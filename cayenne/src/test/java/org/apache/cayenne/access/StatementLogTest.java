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
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.SelectResult;
import org.apache.cayenne.UpdateResult;
import org.apache.cayenne.access.jdbc.CSParameter;
import org.apache.cayenne.access.jdbc.PSBatchParameter;
import org.apache.cayenne.access.jdbc.PSParameter;
import org.apache.cayenne.access.jdbc.RSColumn;
import org.apache.cayenne.access.translator.TranslatedBatch;
import org.apache.cayenne.access.translator.TranslatedProcedure;
import org.apache.cayenne.access.translator.TranslatedSelect;
import org.apache.cayenne.access.translator.TranslatedStatement;
import org.apache.cayenne.log.SQLLogger;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SQLTemplate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StatementLogTest {

    private static TranslatedSelect select() {
        return new TranslatedSelect("SELECT 1", new PSParameter<?>[0], new RSColumn[0], null, false, false);
    }

    private static TranslatedBatch batch() {
        PSBatchParameter p = new PSBatchParameter(new Object[]{1, 2, 3}, 1, 0, 0, null);
        return new TranslatedBatch("INSERT", new PSBatchParameter[]{p});
    }

    private static TranslatedProcedure procedure() {
        return new TranslatedProcedure("call p()", new CSParameter<?>[0]);
    }

    private static UpdateResult count(int count) {
        return new UpdateResult(new int[]{count});
    }

    private static class CapturingLogger implements SQLLogger {

        final List<String> calls = new ArrayList<>();

        // captured durations are non-deterministic, so they are asserted separately from the result strings
        final List<Long> durations = new ArrayList<>();

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void logSelect(TranslatedStatement statement, int rowCount, long durationMillis) {
            durations.add(durationMillis);
            calls.add("selected:" + rowCount);
        }

        @Override
        public void logUpdate(TranslatedStatement statement, int rowCount, List<? extends Map<String, ?>> generatedKeys,
                              long durationMillis) {
            durations.add(durationMillis);
            StringBuilder buffer = new StringBuilder("updated:").append(rowCount);
            for (Map<String, ?> keys : generatedKeys) {
                keys.forEach((name, value) -> buffer.append(" generated:").append(name).append('=').append(value));
            }
            calls.add(buffer.toString());
        }

        @Override
        public void logQueryError(TranslatedStatement statement, Throwable error, long durationMillis) {
            durations.add(durationMillis);
            calls.add("error:" + error.getMessage());
        }

        @Override
        public void logAlsoSelect(int rowCount) {
            calls.add("also selected:" + rowCount);
        }

        @Override
        public void logAlsoUpdate(int rowCount) {
            calls.add("also updated:" + rowCount);
        }

        @Override
        public void logTransactionStart() {
        }

        @Override
        public void logTransactionCommit() {
        }

        @Override
        public void logTransactionRollback() {
        }

        @Override
        public void logMessage(String message) {
        }
    }

    private static StatementContext statementContext(SQLTemplate<?> query, SQLLogger logger) {
        StatementContext statementContext = new StatementContext(query, mock(QueryMetadata.class));
        statementContext.setLogger(logger);
        return statementContext;
    }

    private static StatementContext statementContext(CapturingLogger logger) {
        return statementContext(new SQLTemplate<>("SELECT 1"), logger);
    }

    @Test
    public void selectLogsSingleLine() {
        CapturingLogger logger = new CapturingLogger();
        StatementContext statementContext = statementContext(logger);

        statementContext.setTranslated(select());
        statementContext.addResult(new SelectResult<>(asList(new Object(), new Object())));
        statementContext.logComplete();

        assertEquals(List.of("selected:2"), logger.calls);
    }

    @Test
    public void batchSumsUpdateCountsIntoSingleLine() {
        CapturingLogger logger = new CapturingLogger();
        StatementContext statementContext = statementContext(logger);

        statementContext.setTranslated(batch());
        statementContext.addResult(count(1));
        statementContext.addResult(count(1));
        statementContext.addResult(count(1));
        statementContext.logComplete();

        assertEquals(List.of("updated:3"), logger.calls);
    }

    @Test
    public void batchCountsArrayIsSummed() {
        CapturingLogger logger = new CapturingLogger();
        StatementContext statementContext = statementContext(logger);

        statementContext.setTranslated(batch());
        statementContext.addResult(new UpdateResult(new int[]{1, 2, 3}));
        statementContext.logComplete();

        assertEquals(List.of("updated:6"), logger.calls);
    }

    @Test
    public void procedureReportsExtraResultsAsAlsoLines() {
        CapturingLogger logger = new CapturingLogger();
        StatementContext statementContext = statementContext(logger);

        statementContext.setTranslated(procedure());
        statementContext.addResult(new SelectResult<>(
                List.of(new Object(), new Object(), new Object(), new Object(), new Object())));
        statementContext.addResult(count(10));
        statementContext.addResult(count(20));
        statementContext.logComplete();

        assertEquals(List.of("selected:5", "also updated:10", "also updated:20"), logger.calls);
    }

    @Test
    public void outParametersCountAsOneRow() {
        CapturingLogger logger = new CapturingLogger();
        StatementContext statementContext = statementContext(logger);

        statementContext.setTranslated(procedure());
        statementContext.addResult(new OutParametersResult(Map.of("p", 1)));
        statementContext.addResult(new SelectResult<>(List.of(new Object())));
        statementContext.logComplete();

        assertEquals(List.of("selected:1", "also selected:1"), logger.calls);
    }

    @Test
    public void trailingZeroUpdateCountNotReportedAsAlso() {
        // a stored procedure reports its completion status as a 0 update count after the result set - this must not
        // surface as a stray "also updated:0" line
        CapturingLogger logger = new CapturingLogger();
        StatementContext statementContext = statementContext(logger);

        statementContext.setTranslated(procedure());
        statementContext.addResult(new SelectResult<>(List.of(new Object())));
        statementContext.addResult(count(0));
        statementContext.logComplete();

        assertEquals(List.of("selected:1"), logger.calls);
    }

    @Test
    public void emptyBatchCountDoesNotEmitStrayUpdate() {
        // a SQLTemplate SELECT reports its (absent) update counts via an empty counts array - must not log
        CapturingLogger logger = new CapturingLogger();
        StatementContext statementContext = statementContext(logger);

        statementContext.setTranslated(select());
        statementContext.addResult(new SelectResult<>(asList(new Object(), new Object())));
        statementContext.addResult(new UpdateResult(new int[0]));
        statementContext.logComplete();

        assertEquals(List.of("selected:2"), logger.calls);
    }

    @Test
    public void generatedKeysAppendedToUpdateLine() {
        CapturingLogger logger = new CapturingLogger();
        StatementContext statementContext = statementContext(logger);

        DataRow row1 = DataRow.of("Artist", DataRowLayout.of("ARTIST_ID"),
                new Object[]{1L}, SnapshotVersion.next());
        DataRow row2 = DataRow.of("Artist", DataRowLayout.of("ARTIST_ID"),
                new Object[]{2L}, SnapshotVersion.next());

        statementContext.setTranslated(batch());
        statementContext.addGeneratedKeys(asList(row1, row2));
        statementContext.addResult(count(1));
        statementContext.addResult(count(1));
        statementContext.logComplete();

        assertEquals(List.of("updated:2 generated:ARTIST_ID=1 generated:ARTIST_ID=2"), logger.calls);

        // the keys are a result of the statement, but not a logged result set
        assertEquals(3, statementContext.getResults().size());
        assertEquals(new SelectResult<>(asList(row1, row2)), statementContext.getResults().getFirst());
    }

    @Test
    public void completedBatchLoggedBeforeNextRun() {
        CapturingLogger logger = new CapturingLogger();
        StatementContext statementContext1 = statementContext(new SQLTemplate<>("INSERT 1"), logger);
        StatementContext statementContext2 = statementContext(new SQLTemplate<>("INSERT 2"), logger);

        statementContext1.setTranslated(batch());
        statementContext1.addResult(count(2));
        statementContext1.logComplete();

        statementContext2.setTranslated(batch());
        statementContext2.addResult(count(3));
        statementContext2.logComplete();

        assertEquals(List.of("updated:2", "updated:3"), logger.calls);
    }

    @Test
    public void errorLoggedForTranslatedStatement() {
        CapturingLogger logger = new CapturingLogger();
        StatementContext statementContext = statementContext(logger);

        statementContext.setTranslated(select());
        statementContext.logError(new RuntimeException("boom"));

        assertEquals(List.of("error:boom"), logger.calls);
    }

    @Test
    public void errorWithoutTranslatedStatementLogsNothing() {
        CapturingLogger logger = new CapturingLogger();
        StatementContext statementContext = statementContext(logger);

        statementContext.logError(new RuntimeException("boom"));

        assertEquals(List.of(), logger.calls);
    }

    @Test
    public void failedBatchNotReloggedOnTrailingComplete() {
        // the pending batch update must not be flushed on top of the error line
        CapturingLogger logger = new CapturingLogger();
        StatementContext statementContext = statementContext(logger);

        statementContext.setTranslated(batch());
        statementContext.addResult(count(1));
        statementContext.logError(new RuntimeException("boom"));
        statementContext.logComplete();

        assertEquals(List.of("error:boom"), logger.calls);
    }

    @Test
    public void resultsWithoutTranslatedStatementNotLogged() {
        CapturingLogger logger = new CapturingLogger();
        StatementContext statementContext = statementContext(logger);

        statementContext.addResult(new SelectResult<>(List.of(new Object())));
        statementContext.logComplete();

        assertEquals(List.of(), logger.calls);
        assertEquals(1, statementContext.getResults().size());
    }

    @Test
    public void iteratorWrappedToLogRowCountOnClose() {
        CapturingLogger logger = new CapturingLogger();
        StatementContext statementContext = statementContext(logger);

        @SuppressWarnings("unchecked")
        ResultIterator<Object> iterator = mock(ResultIterator.class);
        when(iterator.hasNextRow()).thenReturn(true, true, false);
        when(iterator.nextRow()).thenReturn(new Object(), new Object());

        statementContext.setTranslated(select());
        statementContext.addResult(iterator);

        ResultIterator<?> stored = assertInstanceOf(ResultIterator.class, statementContext.getResults().getFirst());
        assertNotSame(iterator, stored);
        assertEquals(List.of(), logger.calls);

        while (stored.hasNextRow()) {
            stored.nextRow();
        }
        stored.close();

        verify(iterator).close();
        assertEquals(List.of("selected:2"), logger.calls);
    }

    @Test
    public void iteratorNotWrappedWhenLoggingDisabled() {
        StatementContext statementContext = new StatementContext(new SQLTemplate<>("SELECT 1"),
                mock(QueryMetadata.class));

        ResultIterator<?> iterator = mock(ResultIterator.class);
        statementContext.setTranslated(select());
        statementContext.addResult(iterator);

        assertSame(iterator, statementContext.getResults().getFirst());
    }

    @Test
    public void everyLoggedLineCarriesNonNegativeDuration() {
        CapturingLogger logger = new CapturingLogger();
        StatementContext statementContext = statementContext(logger);

        statementContext.setTranslated(select());
        statementContext.addResult(new SelectResult<>(asList(new Object(), new Object())));
        statementContext.logComplete();

        assertEquals(1, logger.durations.size());
        assertTrue(logger.durations.get(0) >= 0);
    }
}
