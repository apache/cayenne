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

/**
 * Defines API that allows a DataNode to obtain information about query execution strategy.
 * 
 */
public interface OperationHints {

    /**
     * Returns <code>true</code> to indicate that any results of a select operation
     * should be returned as a ResultIterator. <code>false</code> is returned when the
     * results are expected as a list.
     */
    default boolean isIteratedResult() {
        return false;
    }

    /**
     * Returns <code>true</code> if an iterated result keeps its connection to itself until the ResultIterator is
     * closed, so that no other statement can run on that connection while the ResultSet is open. Returns
     * <code>false</code> if the connection belongs to a caller-managed transaction, which may run other statements
     * (queries, fault resolution, commits) while the iterator is open. Only meaningful when
     * {@link #isIteratedResult()} is true. Adapters use it to decide whether a result can be streamed, as some
     * drivers lock the connection for the duration of a streamed result. The default is <code>false</code>.
     *
     * @since 5.0
     */
    default boolean isIteratorExclusiveConnection() {
        return false;
    }
}
