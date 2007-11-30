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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.query.Query;

/**
 * Defines a set of callback methods that allow QueryEngine to pass back query results and
 * notify caller about exceptions.
 * 
 * @see org.apache.cayenne.access.QueryEngine
 * @author Andrus Adamchik
 */
// TODO: need a name that better reflects the functionality,
// e.g. OperationContext or QueryContext
public interface OperationObserver extends OperationHints {

    /**
     * Callback method invoked after an updating query is executed.
     */
    public void nextCount(Query query, int resultCount);

    /**
     * Callback method invoked after a batch update is executed.
     */
    public void nextBatchCount(Query query, int[] resultCount);

    /**
     * Callback method invoked for each processed ResultSet.
     */
    public void nextDataRows(Query query, List<DataRow> dataRows);

    /**
     * Callback method invoked for each opened ResultIterator. If this observer requested
     * results to be returned as a ResultIterator, this method is invoked instead of
     * "nextDataRows(Query,List)". OperationObserver is responsible for closing the
     * ResultIterators passed via this method.
     */
    public void nextDataRows(Query q, ResultIterator it);

    /**
     * Callback method invoked after each batch of generated values is read durring an
     * update.
     * 
     * @since 1.2
     */
    public void nextGeneratedDataRows(Query query, ResultIterator keysIterator);

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
