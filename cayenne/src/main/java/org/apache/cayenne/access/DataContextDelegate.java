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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.Query;

/**
 * Defines API for a DataContext "delegate" - an object that is temporarily passed control
 * by DataContext at some critical points in the normal flow of execution. A delegate thus
 * can modify the flow, abort an operation, modify the objects participating in an
 * operation, or perform any other tasks it deems necessary. DataContextDelegate is shared
 * by DataContext and its ObjectStore.
 * 
 * @see org.apache.cayenne.access.DataContext
 * @since 1.1
 */
public interface DataContextDelegate {

    /**
     * Invoked before a Query is executed via <em>DataContext.performQuery</em>. The
     * delegate may substitute the Query with a different one or may return null to discard
     * the query.
     * 
     * @since 1.2
     */
    Query willPerformQuery(DataContext context, Query query);

    /**
     * Invoked before a Query is executed via <em>DataContext.performGenericQuery</em>.
     * The delegate may substitute the Query with a different one or may return null to
     * discard the query.
     * 
     * @since 1.2
     */
    Query willPerformGenericQuery(DataContext context, Query query);

    /**
     * Invoked by parent DataContext whenever an object change is detected. This can be a
     * change to the object snapshot, or a modification of an "independent" relationship
     * not resulting in a snapshot change. In the later case snapshot argument may be
     * null. If a delegate returns <code>true</code>, ObjectStore will attempt to merge
     * the changes into an object.
     */
    boolean shouldMergeChanges(Persistent object, DataRow snapshotInStore);

    /**
     * Called after a successful merging of external changes to an object. If previosly a
     * delegate returned <code>false</code> from
     * {@link #shouldMergeChanges(Persistent, DataRow)}, this method is not invoked,
     * since changes were not merged.
     */
    void finishedMergeChanges(Persistent object);

    /**
     * Invoked by ObjectStore whenever it is detected that a database row was deleted for
     * object. If a delegate returns <code>true</code>, ObjectStore will change
     * MODIFIED objects to NEW (resulting in recreating the deleted record on next commit)
     * and all other objects - to TRANSIENT. To block this behavior, delegate should
     * return <code>false</code>, and possibly do its own processing.
     * 
     * @param object Persistent that was deleted externally and is still present in the
     *            ObjectStore associated with the delegate.
     */
    boolean shouldProcessDelete(Persistent object);

    /**
     * Called after a successful processing of externally deleted object. If previosly a
     * delegate returned <code>false</code> from
     * {@link #shouldProcessDelete(Persistent)}, this method is not invoked, since no
     * processing was done.
     */
    void finishedProcessDelete(Persistent object);
}
