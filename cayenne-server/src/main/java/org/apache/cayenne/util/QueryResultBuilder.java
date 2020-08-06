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
package org.apache.cayenne.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.QueryResult;
import org.apache.cayenne.QueryResultItem;

/**
 * @since 4.0
 */
public class QueryResultBuilder {

    protected List<QueryResultItem> queryResult;

    public static QueryResultBuilder builder() {
        return new QueryResultBuilder(3);
    }

    public static QueryResultBuilder builder(int expectedSize) {
        return new QueryResultBuilder(expectedSize);
    }

    public static QueryResult empty() {
        return new GenericQueryResult(Collections.<QueryResultItem>emptyList());
    }

    public static QueryResult singleSelect(List<?> selectResult) {
        return new GenericQueryResult(Collections.<QueryResultItem>singletonList(new SelectResult<>(selectResult)));
    }

    public static QueryResult singleObjectSelect(Object selectObject) {
        List<Object> result = Collections.singletonList(selectObject);
        return new GenericQueryResult(Collections.<QueryResultItem>singletonList(new SelectResult<>(result)));
    }

    QueryResultBuilder(int expectedSize) {
        this.queryResult = new ArrayList<>(expectedSize);
    }

    public QueryResultBuilder addSelectResult(List<?> result) {
        queryResult.add(new SelectResult<>(result));
        return this;
    }

    public QueryResultBuilder addBatchUpdateResult(int[] result) {
        queryResult.add(new BatchUpdateResult(result));
        return this;
    }

    public QueryResult build() {
        return new GenericQueryResult(queryResult);
    }

    private static class SelectResult<T> implements QueryResultItem<T> {

        private List<T> result;

        SelectResult(List<T> result) {
            this.result = result;
        }

        @Override
        public boolean isSelectResult() {
            return true;
        }

        @Override
        public boolean isBatchUpdate() {
            return false;
        }

        @Override
        public List<T> getSelectResult() {
            return result;
        }

        @Override
        public int getUpdateCount() {
            throw new CayenneRuntimeException("Can't access update result. This result is a select");
        }

        @Override
        public int[] getBatchUpdateCounts() {
            throw new CayenneRuntimeException("Can't access update result. This result is a select");
        }
    }

    private static class BatchUpdateResult implements QueryResultItem {

        private int[] result;

        BatchUpdateResult(int[] result) {
            this.result = result;
        }

        @Override
        public boolean isSelectResult() {
            return false;
        }

        @Override
        public boolean isBatchUpdate() {
            return result.length > 1;
        }

        @Override
        public List<?> getSelectResult() {
            throw new CayenneRuntimeException("Can't access select result. This result is an update");
        }

        @Override
        public int getUpdateCount() {
            if (result.length == 0) {
                throw new CayenneRuntimeException("No update results");
            }

            if (result.length == 1) {
                return result[0];
            }

            throw new CayenneRuntimeException("This result is a batch update");
        }

        @Override
        public int[] getBatchUpdateCounts() {
            return result;
        }
    }
}
