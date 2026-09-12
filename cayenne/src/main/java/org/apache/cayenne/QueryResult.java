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
package org.apache.cayenne;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A single item of a multipart query result, as returned by {@code SQLExec.execute(..)},
 * {@code MappedExec.execute(..)} and {@code ProcedureCall.call(..)}: a list of selected objects, update counts, an
 * open result iterator, or the OUT parameters of a stored procedure. Items are listed in the order they were
 * produced, so callers that know the shape of their query can access them by index. The hierarchy is sealed, so
 * callers can exhaustively switch over the item type:
 * <pre>{@code
 * for (QueryResult item : SQLExec.query(sql).execute(context)) {
 *     switch (item) {
 *         case QueryResult.Select<?> select -> process(select.objects());
 *         case QueryResult.Update update -> process(update.counts());
 *         case QueryResult.Iterator<?> iterator -> process(iterator.iterator());
 *         case QueryResult.OutParameters out -> process(out.values());
 *     }
 * }
 * }</pre>
 *
 * @since 4.0
 */
public sealed interface QueryResult {

    /**
     * A list of objects or data rows produced by a selecting query.
     *
     * @param <T> the type of the list elements.
     * @since 5.0
     */
    record Select<T>(List<T> objects) implements QueryResult {

        public Select {
            Objects.requireNonNull(objects, "Null objects");
        }
    }

    /**
     * Update counts produced by a non-selecting query. A regular update produces a single count, while a batch
     * produces one count per batch element.
     *
     * @since 5.0
     */
    record Update(int[] counts) implements QueryResult {

        public Update {
            Objects.requireNonNull(counts, "Null counts");
        }

        /**
         * Returns the update count of a non-batch update.
         *
         * @throws CayenneRuntimeException if this item does not contain exactly one count.
         */
        public int count() {
            if (counts.length != 1) {
                throw new CayenneRuntimeException("Expected a single update count, got %d", counts.length);
            }
            return counts[0];
        }
    }

    /**
     * An open iterator over the rows of a selecting query executed in the iterated mode. The caller owns the
     * iterator and must close it.
     *
     * @param <T> the type of the iterated elements.
     * @since 5.0
     */
    record Iterator<T>(ResultIterator<T> iterator) implements QueryResult {

        public Iterator {
            Objects.requireNonNull(iterator, "Null iterator");
        }
    }

    /**
     * The values of the OUT and INOUT parameters of a stored procedure call, keyed by parameter name. A call
     * produces at most one such item, and only if the procedure declares such parameters.
     *
     * @since 5.0
     */
    record OutParameters(Map<String, ?> values) implements QueryResult {

        public OutParameters {
            Objects.requireNonNull(values, "Null values");
        }
    }
}
