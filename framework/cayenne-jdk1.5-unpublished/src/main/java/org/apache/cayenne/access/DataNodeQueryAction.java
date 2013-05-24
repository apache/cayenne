/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;

/**
 * A helper that executes a sequence of queries, providing correct mapping of the results
 * to the original query. Note that this class is not thread-safe as it stores current
 * query execution state.
 * 
 * @since 1.2
 */
class DataNodeQueryAction {

    OperationObserver observer;
    DataNode node;

    public DataNodeQueryAction(DataNode node, OperationObserver observer) {
        this.observer = observer;
        this.node = node;
    }

    public void runQuery(Connection connection, final Query originalQuery)
            throws SQLException, Exception {

        // wrap to ensure that the result is mapped back to the original query, even if
        // the underlying SQLAction uses query substitute...
        OperationObserver wrapper = new OperationObserver() {

            public void nextBatchCount(Query query, int[] resultCount) {
                observer.nextBatchCount(originalQuery, resultCount);
            }

            public void nextCount(Query query, int resultCount) {
                observer.nextCount(originalQuery, resultCount);
            }

            public void nextRows(Query query, List<?> dataRows) {
                observer.nextRows(originalQuery, dataRows);
            }

            public void nextRows(Query q, ResultIterator it) {
                observer.nextRows(originalQuery, it);
            }

            public void nextGeneratedRows(Query query, ResultIterator keysIterator) {
                observer.nextGeneratedRows(originalQuery, keysIterator);
            }

            public void nextGlobalException(Exception ex) {
                observer.nextGlobalException(ex);
            }

            public void nextQueryException(Query query, Exception ex) {
                observer.nextQueryException(originalQuery, ex);
            }

            public boolean isIteratedResult() {
                return observer.isIteratedResult();
            }
        };

        SQLAction action = node.getAdapter().getAction(originalQuery, node);
        action.performAction(connection, wrapper);
    }
}
