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
import org.apache.cayenne.access.translator.TranslatedStatement;
import org.apache.cayenne.query.Query;

import java.util.List;

/**
 * An {@link OperationObserver} decorator that reports all results against the original query, even when the underlying
 * {@link org.apache.cayenne.query.SQLAction} executes a substitute query. All callbacks are delegated to the wrapped
 * observer with the {@code query} argument replaced by the original query.
 */
class OriginalQueryObserver implements OperationObserver {

    private final OperationObserver delegate;
    private final Query originalQuery;

    OriginalQueryObserver(OperationObserver delegate, Query originalQuery) {
        this.delegate = delegate;
        this.originalQuery = originalQuery;
    }

    @Override
    public void nextStatement(Query query, TranslatedStatement statement) {
        delegate.nextStatement(originalQuery, statement);
    }

    @Override
    public void onSuccess() {
        delegate.onSuccess();
    }

    @Override
    public void nextCount(Query query, int resultCount) {
        delegate.nextCount(originalQuery, resultCount);
    }

    @Override
    public void nextBatchCount(Query query, int[] resultCount) {
        delegate.nextBatchCount(originalQuery, resultCount);
    }

    @Override
    public void nextRows(Query query, List<?> dataRows) {
        delegate.nextRows(originalQuery, dataRows);
    }

    @Override
    public void nextRows(Query query, ResultIterator<?> it) {
        delegate.nextRows(originalQuery, it);
    }

    @Override
    public void nextGeneratedRows(Query query, ResultIterator<?> keys, List<ObjectId> idsToUpdate) {
        delegate.nextGeneratedRows(originalQuery, keys, idsToUpdate);
    }

    @Override
    public void nextQueryException(Query query, Exception ex) {
        delegate.nextQueryException(originalQuery, ex);
    }

    @Override
    public void nextGlobalException(Exception ex) {
        delegate.nextGlobalException(ex);
    }

    @Override
    public boolean isIteratedResult() {
        return delegate.isIteratedResult();
    }
}
