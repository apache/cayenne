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

import org.apache.cayenne.QueryResultItem;
import org.apache.cayenne.ResultIterator;

import java.util.List;

/**
 * Lookups of the first item of a given kind in a multipart query result.
 *
 * @since 5.0
 */
final class QueryResultItems {

    private QueryResultItems() {
    }

    /**
     * Returns the list of the first {@link QueryResultItem.Select} item, or null if there is none.
     */
    static List<?> firstList(List<QueryResultItem> items) {
        for (QueryResultItem item : items) {
            if (item instanceof QueryResultItem.Select<?> select) {
                return select.objects();
            }
        }
        return null;
    }

    /**
     * Returns the iterator of the first {@link QueryResultItem.Iterator} item, or null if there is none.
     */
    static ResultIterator<?> firstIterator(List<QueryResultItem> items) {
        for (QueryResultItem item : items) {
            if (item instanceof QueryResultItem.Iterator<?> iterator) {
                return iterator.iterator();
            }
        }
        return null;
    }

    /**
     * Returns the counts of the first {@link QueryResultItem.Update} item, or null if there is none.
     */
    static int[] firstUpdateCount(List<QueryResultItem> items) {
        for (QueryResultItem item : items) {
            if (item instanceof QueryResultItem.Update update) {
                return update.counts();
            }
        }
        return null;
    }
}
