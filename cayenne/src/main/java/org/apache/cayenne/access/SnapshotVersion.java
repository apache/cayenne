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

import org.apache.cayenne.Persistent;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Incrementing global snapshot version generator. A version is taken once per operation that produces snapshots, i.e.
 * once per SQL statement result, per commit and per snapshot event, and every {@code DataRow} the operation produces
 * carries it, as does every object created from or committed as such a row.
 *
 * @since 5.0
 */
public final class SnapshotVersion {

    /**
     * The version of a snapshot that belongs to no operation: one built for a caller and neither cached nor compared,
     * such as the one returned by {@code DataContext.currentSnapshot(..)}. It is also the version of an object that was
     * never loaded from or committed as a snapshot.
     */
    public static final long NONE = Persistent.DEFAULT_VERSION;

    // make sure the starting value is different from NONE
    private static final AtomicLong counter = new AtomicLong(NONE + 1);

    private SnapshotVersion() {
    }

    /**
     * Returns the version of a new operation, greater than any version returned before, and never {@link #NONE}.
     */
    public static long next() {
        return counter.getAndIncrement();
    }
}
