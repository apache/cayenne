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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.query.Query;

/**
 * Helper class to process test queries results.
 */
public class MockOperationObserver implements OperationObserver {

    protected Map resultRows = new HashMap();
    protected Map resultCounts = new HashMap();
    protected Map resultBatch = new HashMap();

    public List rowsForQuery(Query q) {
        return (List) resultRows.get(q);
    }

    public int countForQuery(Query q) {
        Integer count = (Integer) resultCounts.get(q);
        return (count != null) ? count.intValue() : -1;
    }

    public int[] countsForQuery(Query q) {
        return (int[]) resultBatch.get(q);
    }

    public void nextCount(Query query, int resultCount) {
        resultCounts.put(query, new Integer(resultCount));
    }

    public void nextRows(Query query, List<?> dataRows) {
        resultRows.put(query, dataRows);
    }

    public void nextBatchCount(Query query, int[] resultCount) {
        resultBatch.put(query, resultCount);
    }

    public void nextGlobalException(Exception ex) {
        throw new CayenneRuntimeException(ex);
    }

    public void nextQueryException(Query query, Exception ex) {
        throw new CayenneRuntimeException(ex);
    }

    public void nextRows(Query q, ResultIterator it) {
    }

    @Override
    public void nextGeneratedRows(Query query, ResultIterator keys, ObjectId idToUpdate) {
    }

    public boolean isIteratedResult() {
        return false;
    }
}
