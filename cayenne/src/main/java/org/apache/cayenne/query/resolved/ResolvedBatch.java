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

package org.apache.cayenne.query.resolved;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.Query;

import java.util.List;

/**
 * A resolved description of a batch of inserts, updates or deletes of the rows of a single DbEntity, created by the
 * flush. A batch is translated into a single parametrized statement, executed once per {@link #rows() row}.
 *
 * @since 6.0
 */
public sealed interface ResolvedBatch extends ResolvedQuery permits InsertBatch, UpdateBatch, DeleteBatch {

    /**
     * A batch is created by the framework, so there is no user query behind it.
     */
    @Override
    default Query<?> source() {
        return null;
    }

    @Override
    default DataMap dataMap() {
        return dbEntity().getDataMap();
    }

    /**
     * Returns the attributes bound by the batch statement, in the order of the statement parameters.
     */
    List<DbAttribute> dbAttributes();

    List<BatchRow> rows();

    /**
     * Returns true if this batch should run one row at a time, checking that each row updated exactly one record.
     */
    boolean usingOptimisticLocking();

    /**
     * Returns the value of a row for the attribute at the given index of {@link #dbAttributes()}. The value may be a
     * {@link org.apache.cayenne.access.DeferredValue}, to be resolved when bound.
     */
    Object value(BatchRow row, int attributeIndex);
}
