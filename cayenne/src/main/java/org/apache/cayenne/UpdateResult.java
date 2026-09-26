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

import java.util.Objects;

/**
 * A {@link QueryResult} item holding the update counts produced by a non-selecting query. A regular update produces
 * a single count, while a batch produces one count per batch element.
 *
 * @since 5.0
 */
public record UpdateResult(int[] counts) implements QueryResult {

    public UpdateResult {
        Objects.requireNonNull(counts, "Null counts");
    }

    /**
     * Returns the update count of a non-batch update.
     *
     * @throws CayenneRuntimeException if this item does not contain exactly one count.
     */
    public int count() {
        if (counts.length != 1) {
            throw new CayenneRuntimeException("Expected a single update count, got %d", counts.length);
        }
        return counts[0];
    }
}
