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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;

/**
 * A helper that executes a sequence of queries, providing correct mapping of the results
 * to the original query. Note that this class is not thread-safe as it stores current
 * query execution state.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataNodeQueryAction implements OperationObserver {

    OperationObserver observer;
    DataNode node;

    private Query currentQuery;

    public DataNodeQueryAction(DataNode node, OperationObserver observer) {
        this.observer = observer;
        this.node = node;
    }

    public void runQuery(Connection connection, Query query) throws SQLException,
            Exception {

        // remember root query ... it will be used to map the results, even if SQLAction
        // uses query substitute...
        this.currentQuery = query;

        SQLAction action = node.getAdapter().getAction(query, node);
        action.performAction(connection, this);
    }

    public void nextBatchCount(Query query, int[] resultCount) {
        observer.nextBatchCount(currentQuery, resultCount);
    }

    public void nextCount(Query query, int resultCount) {
        observer.nextCount(currentQuery, resultCount);
    }

    public void nextDataRows(Query query, List<DataRow> dataRows) {
        observer.nextDataRows(currentQuery, dataRows);
    }

    public void nextDataRows(Query q, ResultIterator it) {
        observer.nextDataRows(currentQuery, it);
    }

    public void nextGeneratedDataRows(Query query, ResultIterator keysIterator) {
        observer.nextGeneratedDataRows(currentQuery, keysIterator);
    }

    public void nextGlobalException(Exception ex) {
        observer.nextGlobalException(ex);
    }

    public void nextQueryException(Query query, Exception ex) {
        observer.nextQueryException(currentQuery, ex);
    }

    public boolean isIteratedResult() {
        return observer.isIteratedResult();
    }
}
