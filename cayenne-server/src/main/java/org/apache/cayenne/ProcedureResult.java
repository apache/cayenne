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
package org.apache.cayenne;

import java.util.List;

/**
 * Result of procedure call.
 *
 * @since 4.0
 */
public class ProcedureResult<T> {

    protected List<QueryResult> results;
    protected Class<T> resultClass;

    public ProcedureResult(List<QueryResult> results) {
        this.results = results;
    }

    public ProcedureResult(List<QueryResult> results, Class<T> resultClass) {
        this(results);
        this.resultClass = resultClass;
    }

    public List<QueryResult> getResults() {
        return results;
    }

    /**
     * Returns first list found in the procedure execution result.
     */
    public List<T> getSelectResult() {
        for (QueryResult result : results) {
            if (result.isSelectResult()) {
                return (List<T>) result.getSelectResult();
            }
        }

        throw new CayenneRuntimeException("This result is not a select result.");
    }

    /**
     * Returns first batch update count found in the procedure execution result.
     */
    public int[] getUpdateResult() {
        for (QueryResult result : results) {
            if (result.isBatchUpdate()) {
                return result.getBatchUpdateResult();
            }
        }

        throw new CayenneRuntimeException("This result is not an update result.");
    }

    /**
     * Returns procedure OUT parameter by its name defined in the mapping file.
     */
    public Object getParam(String paramName) {
        return ((DataRow) getSelectResult().get(0)).get(paramName);
    }
}
