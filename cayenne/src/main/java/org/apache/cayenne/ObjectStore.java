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

import org.apache.cayenne.graph.GraphChangeHandler;

import java.util.Collection;
import java.util.List;

/**
 * A registry of persistent objects managed by an {@link ObjectContext}, keyed by {@link ObjectId}. Also tracks the
 * committed state of those objects as {@link DataRow} snapshots. Inherited {@link GraphChangeHandler} methods are
 * callbacks for registered objects to notify the store of their changes.
 * <p>
 * Users rarely need to access the store directly, as ObjectContext serves as a facade for most of its operations.
 *
 * @since 5.0
 */
public interface ObjectStore extends GraphChangeHandler {

    /**
     * Returns a registered object for the id, or null if no such object is registered.
     */
    Persistent getObject(Object nodeId);

    /**
     * Registers an object under the id.
     */
    void registerObject(Object nodeId, Persistent nodeObject);

    /**
     * Unregisters an object with the id, forgetting any information associated with it. Returns the unregistered
     * object, or null if none was registered.
     */
    Persistent unregisterObject(Object nodeId);

    /**
     * Returns a copy of the collection of all registered objects.
     */
    Collection<Persistent> registeredObjects();

    /**
     * Returns registered objects in a given {@link PersistenceState}.
     */
    List<Persistent> objectsInState(int state);

    /**
     * Returns true if any registered objects have uncommitted changes.
     */
    boolean hasChanges();

    /**
     * Returns a committed snapshot of an object with the id from the cache, or null if it is not cached. Unlike
     * {@link #getSnapshot(ObjectId)}, never fetches from the database.
     */
    DataRow getCachedSnapshot(ObjectId oid);

    /**
     * Returns a committed snapshot of an object with the id, fetching it from the database if needed. Returns null
     * if no such row exists.
     */
    DataRow getSnapshot(ObjectId oid);
}
