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

import org.apache.cayenne.access.jdbc.CSParameter;
import org.apache.cayenne.access.jdbc.PSBatchParameter;
import org.apache.cayenne.access.jdbc.PSParameter;
import org.apache.cayenne.access.jdbc.RSColumn;
import org.apache.cayenne.access.translator.TranslatedBatch;
import org.apache.cayenne.access.translator.TranslatedProcedure;
import org.apache.cayenne.access.translator.TranslatedSelect;
import org.apache.cayenne.access.translator.TranslatedStatement;
import org.apache.cayenne.log.SqlLogger;
import org.apache.cayenne.map.DbAttribute;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class LoggingObserverTest {

    private static TranslatedSelect select() {
        return new TranslatedSelect("SELECT 1", new PSParameter<?>[0], new RSColumn[0], false, false);
    }

    private static TranslatedBatch batch() {
        PSBatchParameter p = new PSBatchParameter(new Object[]{1, 2, 3}, 1, 0, 0, null);
        return new TranslatedBatch("INSERT", new PSBatchParameter[]{p});
    }

    private static TranslatedProcedure procedure() {
        return new TranslatedProcedure("call p()", new CSParameter<?>[0]);
    }

    /**
     * A capturing {@link SqlLogger} that records the sequence of statement calls for assertions.
     */
    private static class CapturingLogger implements SqlLogger {
        final List<String> calls = new ArrayList<>();

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void logSelect(TranslatedStatement statement, int rowCount) {
            calls.add("selected:" + rowCount);
        }

        @Override
        public void logUpdate(TranslatedStatement statement, int rowCount) {
            calls.add("updated:" + rowCount);
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
        public void logGeneratedKey(DbAttribute attribute, Object value) {
            calls.add("generated PK " + attribute.getName() + ":" + value);
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

    private LoggingObserver observer(CapturingLogger logger) {
        return new LoggingObserver(mock(OperationObserver.class), logger);
    }

    @Test
    public void selectLogsSingleLine() {
        CapturingLogger logger = new CapturingLogger();
        LoggingObserver observer = observer(logger);

        observer.nextStatement(null, select());
        observer.nextRows(null, asList(new Object(), new Object()));
        observer.onSuccess();

        assertEquals(List.of("selected:2"), logger.calls);
    }

    @Test
    public void batchSumsUpdateCountsIntoSingleLine() {
        CapturingLogger logger = new CapturingLogger();
        LoggingObserver observer = observer(logger);

        observer.nextStatement(null, batch());
        observer.nextCount(null, 1);
        observer.nextCount(null, 1);
        observer.nextCount(null, 1);
        observer.onSuccess();

        assertEquals(List.of("updated:3"), logger.calls);
    }

    @Test
    public void procedureReportsExtraResultsAsAlsoLines() {
        CapturingLogger logger = new CapturingLogger();
        LoggingObserver observer = observer(logger);

        observer.nextStatement(null, procedure());
        observer.nextRows(null, List.of(new Object(), new Object(), new Object(), new Object(), new Object()));
        observer.nextCount(null, 10);
        observer.nextCount(null, 20);
        observer.onSuccess();

        assertEquals(List.of("selected:5", "also updated:10", "also updated:20"), logger.calls);
    }

    @Test
    public void emptyBatchCountDoesNotEmitStrayUpdate() {
        // a SQLTemplate SELECT reports its (absent) update counts via an empty nextBatchCount - must not log
        CapturingLogger logger = new CapturingLogger();
        LoggingObserver observer = observer(logger);

        observer.nextStatement(null, select());
        observer.nextRows(null, asList(new Object(), new Object()));
        observer.nextBatchCount(null, new int[0]);
        observer.onSuccess();

        assertEquals(List.of("selected:2"), logger.calls);
    }

    @Test
    public void newStatementFlushesPreviousBatch() {
        CapturingLogger logger = new CapturingLogger();
        LoggingObserver observer = observer(logger);

        observer.nextStatement(null, batch());
        observer.nextCount(null, 2);
        // a second batch statement should flush the first
        observer.nextStatement(null, batch());
        observer.nextCount(null, 3);
        observer.onSuccess();

        assertEquals(List.of("updated:2", "updated:3"), logger.calls);
    }
}
