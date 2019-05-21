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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.QueryResult;
import org.apache.cayenne.QueryResultItem;

import java.util.Iterator;
import java.util.List;

/**
 * Generic implementation of QueryResult using List as QueryResultItem storage.
 *
 * @since 4.0
 */
public class GenericQueryResult<T> implements QueryResult<T> {

    protected Class<T> resultClass;
    protected List<QueryResultItem> resultItems;

    public GenericQueryResult(List<QueryResultItem> resultItems) {
        this.resultItems = resultItems;
    }

    public GenericQueryResult(List<QueryResultItem> resultItems, Class<T> resultClass) {
        this(resultItems);
        this.resultClass = resultClass;
    }

    @Override
    public int size() {
        return resultItems.size();
    }

    @Override
    public boolean isList() {
        for (QueryResultItem item : resultItems) {
            if (item.isSelectResult()) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<T> firstList() {
        for (QueryResultItem item : resultItems) {
            if (item.isSelectResult()) {
                return item.getSelectResult();
            }
        }

        throw new CayenneRuntimeException("Result is not a select result.");
    }

    @Override
    public int[] firstBatchUpdateCount() {
        for (QueryResultItem item : resultItems) {
            if (!item.isSelectResult()) {
                return item.getBatchUpdateCounts();
            }
        }

        throw new CayenneRuntimeException("Result is not a batch update count.");
    }

    @Override
    public int firstUpdateCount() {
        for (QueryResultItem item : resultItems) {
            if (!item.isSelectResult()) {
                return item.getUpdateCount();
            }
        }

        throw new CayenneRuntimeException("Result is not an update count.");
    }

    @Override
    public Iterator<QueryResultItem> iterator() {
        return resultItems.iterator();
    }
}
