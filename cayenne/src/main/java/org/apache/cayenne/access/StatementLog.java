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
import org.apache.cayenne.OutParametersResult;
import org.apache.cayenne.QueryResult;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.SelectResult;
import org.apache.cayenne.UpdateResult;
import org.apache.cayenne.access.translator.TranslatedBatch;
import org.apache.cayenne.access.translator.TranslatedStatement;
import org.apache.cayenne.log.SQLLogger;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Drives the {@link SQLLogger} for a single {@link StatementContext}. Emits a header line for the statement with the
 * first result, continuation lines for the results that follow, and an error line if the statement fails. The update
 * counts of a batch are summed up into a single line that is emitted when the statement completes.
 */
final class StatementLog {

    private final SQLLogger logger;

    private TranslatedStatement statement;
    private boolean headerEmitted;
    private boolean batchHasUpdate;
    private int batchUpdateSum;
    private long startNanos;

    // lazily allocated on the first generated keys report, since most statements produce no generated keys
    private List<Map<String, ?>> generatedKeys;

    StatementLog(SQLLogger logger) {
        this.logger = logger;
    }

    void statement(TranslatedStatement statement) {
        this.statement = statement;
        this.headerEmitted = false;
        this.batchHasUpdate = false;
        this.batchUpdateSum = 0;
        this.generatedKeys = null;
        this.startNanos = System.nanoTime();
    }

    /**
     * Logs a result, returning the result to store in the statement context, which is the original result, except for a
     * ResultIterator that becomes the header of the statement: it is wrapped to log the row count on close.
     */
    QueryResult result(QueryResult result) {
        // results reported without a translated statement can't be logged
        if (statement == null) {
            return result;
        }

        switch (result) {
            case SelectResult<?> select -> reportSelect(select.objects().size());
            case OutParametersResult ignored -> reportSelect(1);
            case UpdateResult(int[] counts) -> reportUpdate(counts);
            case ResultIterator<?> iterator -> {
                // iterated result: the row count is only known when the iterator is closed
                if (!headerEmitted) {
                    headerEmitted = true;
                    return new CountingResultIterator<>(iterator, statement);
                }
            }
        }

        return result;
    }

    void generatedKeys(List<DataRow> keys) {
        // buffer the keys and emit them as a trailing "generated:[...]" block on the statement's own update line,
        // rather than as separate lines that would print before the INSERT that produced them
        if (generatedKeys == null) {
            generatedKeys = new ArrayList<>();
        }
        generatedKeys.addAll(keys);
    }

    void complete() {
        flushPendingBatchUpdate();
        statement = null;
    }

    void error(Throwable error) {
        // emit an immediate ERROR line identifying the SQL that failed, then forget the statement so that a pending
        // batch update is not logged on top of the error line
        if (statement != null) {
            logger.logQueryError(statement, error, elapsedMillis());
            statement = null;
        }
    }

    // a batch reports its counts across several results; the accumulated total can only be logged as a single
    // "updated:N" line once the batch is done
    private void flushPendingBatchUpdate() {
        if (!headerEmitted && batchHasUpdate && statement != null) {
            logger.logUpdate(statement, batchUpdateSum, generatedKeys != null ? generatedKeys : List.of(), elapsedMillis());
            headerEmitted = true;
        }
    }

    private void reportSelect(int rowCount) {
        if (headerEmitted) {
            logger.logAlsoSelect(rowCount);
        } else {
            logger.logSelect(statement, rowCount, elapsedMillis());
            headerEmitted = true;
        }
    }

    private void reportUpdate(int[] counts) {
        // an empty count array carries no result to report (e.g. a SQLTemplate SELECT reports its combined update
        // counts this way, and there are none) - avoid a stray "updated:0" line
        if (counts.length == 0) {
            return;
        }

        if (statement instanceof TranslatedBatch) {
            batchHasUpdate = true;
            batchUpdateSum += sum(counts);
        } else {
            reportUpdate(sum(counts));
        }
    }

    private void reportUpdate(int rowCount) {
        if (headerEmitted) {
            // a trailing zero update count is driver noise (e.g. a stored procedure's completion status reported
            // after its result set), so skip the stray "also updated:0" line
            if (rowCount != 0) {
                logger.logAlsoUpdate(rowCount);
            }
        } else {
            logger.logUpdate(statement, rowCount, generatedKeys != null ? generatedKeys : List.of(), elapsedMillis());
            headerEmitted = true;
        }
    }

    private long elapsedMillis() {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static int sum(int[] counts) {
        int total = 0;
        for (int c : counts) {
            // Statement.SUCCESS_NO_INFO / EXECUTE_FAILED carry no usable count
            if (c < 0) {
                return Statement.SUCCESS_NO_INFO;
            }
            total += c;
        }
        return total;
    }

    /**
     * Wraps a user-facing {@link ResultIterator}, counting rows as they are read and logging the compact select line
     * once the iterator is closed.
     */
    private final class CountingResultIterator<T> implements ResultIterator<T> {

        private final ResultIterator<T> delegate;
        private final TranslatedStatement statement;
        private int count;

        CountingResultIterator(ResultIterator<T> delegate, TranslatedStatement statement) {
            this.delegate = delegate;
            this.statement = statement;
        }

        @Override
        public List<T> allRows() {
            List<T> rows = delegate.allRows();
            count += rows.size();
            return rows;
        }

        @Override
        public boolean hasNextRow() {
            return delegate.hasNextRow();
        }

        @Override
        public T nextRow() {
            count++;
            return delegate.nextRow();
        }

        @Override
        public void skipRow() {
            count++;
            delegate.skipRow();
        }

        @Override
        public void close() {
            delegate.close();
            logger.logSelect(statement, count, elapsedMillis());
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return hasNextRow();
                }

                @Override
                public T next() {
                    return nextRow();
                }
            };
        }
    }
}
