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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.translator.TranslatedStatement;
import org.apache.cayenne.query.Query;

import java.util.List;

/**
 * Defines a set of callback methods that allow {@link DataNode} to pass back query
 * results and notify caller about exceptions.
 * <p>
 * All methods have default implementations: the result callbacks do nothing, the exception callbacks rethrow the
 * reported exception wrapped in a {@link CayenneRuntimeException}, and {@link #isIteratedResult()} returns false. An
 * implementation only needs to override the callbacks it cares about.
 */
public interface OperationObserver extends OperationHints {

    /**
     * Callback invoked with the translated form of a query (its SQL and bindings) just before the statement is
     * executed. It lets a logging observer correlate a statement with the results reported through the other
     * {@code next*} callbacks. The default implementation does nothing.
     *
     * @since 5.0
     */
    default void nextStatement(Query query, TranslatedStatement statement) {
    }

    /**
     * Called at the end of a multi-query DataNode operation.
     *
     * @since 5.0
     */
    default void onSuccess() {
    }

    /**
     * Returns whether results should be returned as a {@link ResultIterator}. Defaults to false.
     */
    @Override
    default boolean isIteratedResult() {
        return false;
    }

    /**
     * Callback method invoked after an updating query is executed.
     */
    default void nextCount(Query query, int resultCount) {
    }

    /**
     * Callback method invoked after a batch update is executed.
     */
    default void nextBatchCount(Query query, int[] resultCount) {
    }

    /**
     * Callback method invoked for each processed ResultSet.
     *
     * @since 3.0
     */
    default void nextRows(Query query, List<?> dataRows) {
    }

    /**
     * Callback method invoked for each opened ResultIterator. If this observer requested
     * results to be returned as a ResultIterator, this method is invoked instead of
     * {@link #nextRows(Query, List)}.
     *
     * @since 3.0
     */
    default void nextRows(Query q, ResultIterator<?> it) {
    }

    /**
     * Callback method invoked after each batch of generated values is read during an update.
     *
     * @since 4.2
     */
    default void nextGeneratedRows(Query query, ResultIterator<?> keys, List<ObjectId> idsToUpdate) {
    }

    /**
     * Callback method invoked on exceptions that happen during an execution of a specific
     * query. The default implementation rethrows the exception wrapped in a {@link CayenneRuntimeException}.
     */
    default void nextQueryException(Query query, Exception ex) {
        throw new CayenneRuntimeException(ex);
    }

    /**
     * Callback method invoked on exceptions that are not tied to a specific query
     * execution, such as JDBC connection exceptions, etc. The default implementation rethrows the exception wrapped in
     * a {@link CayenneRuntimeException}.
     */
    default void nextGlobalException(Exception ex) {
        throw new CayenneRuntimeException(ex);
    }
}
