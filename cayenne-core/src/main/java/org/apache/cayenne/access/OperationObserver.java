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

import java.util.List;

import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.query.Query;

/**
 * Defines a set of callback methods that allow {@link QueryEngine} to pass back query
 * results and notify caller about exceptions.
 */
public interface OperationObserver extends OperationHints {

    /**
     * Callback method invoked after an updating query is executed.
     */
    void nextCount(Query query, int resultCount);

    /**
     * Callback method invoked after a batch update is executed.
     */
    void nextBatchCount(Query query, int[] resultCount);

    /**
     * Callback method invoked for each processed ResultSet.
     * 
     * @since 3.0
     */
    void nextRows(Query query, List<?> dataRows);

    /**
     * Callback method invoked for each opened ResultIterator. If this observer requested
     * results to be returned as a ResultIterator, this method is invoked instead of
     * {@link #nextRows(Query, List)}.
     * 
     * @since 3.0
     */
    void nextRows(Query q, ResultIterator it);

    /**
     * Callback method invoked after each batch of generated values is read during an
     * update.
     * 
     * @since 3.0
     */
    void nextGeneratedRows(Query query, ResultIterator keysIterator);

    /**
     * Callback method invoked on exceptions that happen during an execution of a specific
     * query.
     */
    public void nextQueryException(Query query, Exception ex);

    /**
     * Callback method invoked on exceptions that are not tied to a specific query
     * execution, such as JDBC connection exceptions, etc.
     */
    public void nextGlobalException(Exception ex);
}
