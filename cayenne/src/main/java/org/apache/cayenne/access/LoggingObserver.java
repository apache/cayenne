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

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.translator.TranslatedBatch;
import org.apache.cayenne.access.translator.TranslatedStatement;
import org.apache.cayenne.log.SqlLogger;
import org.apache.cayenne.query.Query;

import java.sql.Statement;
import java.util.Iterator;
import java.util.List;

/**
 * An {@link OperationObserver} decorator that correlates each executed statement (reported via
 * {@link #nextStatement}) with its results (reported via the {@code next*} callbacks) and drives a {@link SqlLogger}
 * to emit compact, single-line log messages. All callbacks are delegated to the wrapped observer unchanged.
 * <p>
 * The first result of a statement produces the main line (SQL + bindings + {@code selected:N}/{@code updated:N}); a
 * statement that yields more results reports them as {@code also ...} lines — except batches, whose per-row update
 * counts are summed into a single {@code updated:N} line. Exceptions are not logged (the SQL travels with the thrown
 * exception instead).
 *
 * @since 5.0
 */
class LoggingObserver implements OperationObserver {

    private final OperationObserver delegate;
    private final SqlLogger logger;

    private TranslatedStatement current;
    private boolean headerEmitted;
    private boolean batchHasUpdate;
    private int batchUpdateSum;

    LoggingObserver(OperationObserver delegate, SqlLogger logger) {
        this.delegate = delegate;
        this.logger = logger;
    }

    /**
     * Emits any pending statement line (currently only summed batch updates) that has not been logged yet. Called by
     * {@link DataNode} after the query loop completes.
     */
    void flush() {
        flushPending();
        current = null;
    }

    private void flushPending() {
        if (!headerEmitted && batchHasUpdate && current != null) {
            logger.logUpdate(current, batchUpdateSum);
            headerEmitted = true;
        }
    }

    private void reportSelect(int rowCount) {
        if (headerEmitted) {
            logger.logAlsoSelect(rowCount);
        } else {
            logger.logSelect(current, rowCount);
            headerEmitted = true;
        }
    }

    private void reportUpdate(int rowCount) {
        if (headerEmitted) {
            logger.logAlsoUpdate(rowCount);
        } else {
            logger.logUpdate(current, rowCount);
            headerEmitted = true;
        }
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

    @Override
    public void nextStatement(Query query, TranslatedStatement statement) {
        flushPending();
        this.current = statement;
        this.headerEmitted = false;
        this.batchHasUpdate = false;
        this.batchUpdateSum = 0;
        delegate.nextStatement(query, statement);
    }

    @Override
    public void nextCount(Query query, int resultCount) {
        if (current instanceof TranslatedBatch) {
            batchHasUpdate = true;
            batchUpdateSum += resultCount;
        } else if (current != null) {
            reportUpdate(resultCount);
        }
        delegate.nextCount(query, resultCount);
    }

    @Override
    public void nextBatchCount(Query query, int[] resultCount) {
        // an empty count array carries no result to report (e.g. a SQLTemplate SELECT reports its combined update
        // counts this way, and there are none) - avoid a stray "updated:0" line
        if (resultCount.length > 0) {
            if (current instanceof TranslatedBatch) {
                batchHasUpdate = true;
                batchUpdateSum += sum(resultCount);
            } else if (current != null) {
                reportUpdate(sum(resultCount));
            }
        }
        delegate.nextBatchCount(query, resultCount);
    }

    @Override
    public void nextRows(Query query, List<?> dataRows) {
        if (current != null) {
            reportSelect(dataRows.size());
        }
        delegate.nextRows(query, dataRows);
    }

    @Override
    public void nextRows(Query q, ResultIterator<?> it) {
        // iterated result: the row count is only known when the iterator is closed
        if (current != null && !headerEmitted) {
            it = new CountingResultIterator<>(it, current);
            headerEmitted = true;
        }
        delegate.nextRows(q, it);
    }

    @Override
    public void nextGeneratedRows(Query query, ResultIterator<?> keys, List<ObjectId> idsToUpdate) {
        delegate.nextGeneratedRows(query, keys, idsToUpdate);
    }

    @Override
    public void nextQueryException(Query query, Exception ex) {
        delegate.nextQueryException(query, ex);
    }

    @Override
    public void nextGlobalException(Exception ex) {
        delegate.nextGlobalException(ex);
    }

    @Override
    public boolean isIteratedResult() {
        return delegate.isIteratedResult();
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
            logger.logSelect(statement, count);
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
