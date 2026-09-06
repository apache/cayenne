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
package org.apache.cayenne.dba.mysql;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationHints;
import org.apache.cayenne.access.jdbc.SelectAction;
import org.apache.cayenne.query.Select;

/**
 * @since 3.0
 */
class MySQLSelectAction extends SelectAction {

    <T> MySQLSelectAction(Select<T> query, DataNode dataNode) {
        super(query, dataNode);
    }

    @Override
    protected int getInMemoryOffset(int queryOffset) {
        return 0;
    }

    /**
     * Streams an iterated result when the iterator has the connection to itself.
     */
    @Override
    protected int statementFetchSize(OperationHints hints) {
        int fetchSize = super.statementFetchSize(hints);
        if (fetchSize != 0) {
            return fetchSize;
        }

        // Integer.MIN_VALUE is an indicator specific to MySQL that switches the ResultSet to the streaming mode.
        // It saves memory, as the driver does not batch the results. But it also speeds up large queries (> ~ 2-3K rows;
        // up to 40% faster in our benchmarks). It makes small queries slower, so can't use it as a default.
        return hints.isIteratedResult() && hints.isIteratorExclusiveConnection() ? Integer.MIN_VALUE : 0;
    }
}
