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

package org.apache.cayenne.access.flush.operation;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.compiled.CompiledTable;

/**
 * Object that represents some change on DB level.
 * Common cases are insert/update/delete of single DB row.
 *
 * @since 4.2
 */
public interface DbRowOp {

    <T> T accept(DbRowOpVisitor<T> visitor);

    /**
     * Returns the type of the operation, as used for sorting.
     *
     * @since 6.0
     */
    DbRowOpType getType();

    CompiledTable getEntity();

    ObjectId getChangeId();

    Persistent getObject();

    /**
     * Returns the key of the batch the operation can run in, or null if it can't run in a batch with other operations.
     * The key is of a complete operation, so the operation must not change after it is taken.
     *
     * @since 6.0
     */
    BatchKey getBatchKey();

    /**
     * Returns whether this and the other operation can run in one SQL batch, i.e. whether they have equal batch keys.
     */
    default boolean isSameBatch(DbRowOp rowOp) {
        BatchKey key = getBatchKey();
        return key != null && key.equals(rowOp.getBatchKey());
    }
}
