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

/**
 * Represents a collection of items which are results of a multipart query execution.
 *
 * @since 4.0
 */
public interface QueryResult<T> extends Iterable<QueryResultItem> {

    /**
     * Returns a number of results in the response.
     */
    int size();

    /**
     * Returns whether current iteration result is a list or an update count.
     */
    boolean isList();

    /**
     * A utility method for quickly retrieving the first list in the response. Returns
     * null if the query has no lists.
     */
    List<T> firstList();

    /**
     * A utility method for quickly retrieving the first batch update count array from the response.
     */
    int[] firstBatchUpdateCount();

    /**
     * A utility method for quick retrieval of the first update count from the response.
     */
    int firstUpdateCount();
}
