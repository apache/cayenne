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

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.util.PersistentObjectList;

/**
 * A list that holds objects for to-many relationships. All operations, except for
 * resolving the list from DB, are not synchronized. The safest way to implement custom
 * synchronization is to synchronize on parent ObjectStore.
 */
public class ToManyList<E> extends PersistentObjectList<E> implements Serializable {

    /**
     * Creates ToManyList.
     * 
     * @since 1.1
     */
    public ToManyList(Persistent source, String relationship) {
        super(source, relationship);

        // if source is new, set object list right away
        if (isTransientParent()) {
            objectList = new LinkedList<>();
        }
    }

    // ====================================================
    // Standard List Methods.
    // ====================================================
    @Override
    public int hashCode() {
        return 15 + resolvedObjectList().hashCode();
    }

    // ====================================================
    // Tracking list modifications, and resolving it
    // on demand
    // ====================================================

    @Override
    protected boolean shouldAddToRemovedFromUnresolvedList(Object object) {
        // No point in adding a new or transient object -- these will never be fetched
        // from the database.
        if (object instanceof Persistent) {
            Persistent persistent = (Persistent) object;
            if ((persistent.getPersistenceState() == PersistenceState.TRANSIENT)
                    || (persistent.getPersistenceState() == PersistenceState.NEW)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return getClass().getName() + "@" + System.identityHashCode(this);
    }

    @Override
    protected void postprocessAdd(Collection<? extends E> collection) {
        // no need for this operation for Persistent objects...
    }

    @Override
    protected void postprocessRemove(Collection<? extends E> collection) {
        // no need for this operation for Persistent objects...
    }

    @Override
    protected void postprocessAdd(E addedObject) {
        // no need for this operation for Persistent objects...
    }

    @Override
    protected void postprocessRemove(E removedObject) {
        // no need for this operation for Persistent objects...
    }

    @Override
    protected void updateReverse(List<E> resolved) {
        // no need for this operation for Persistent objects...
    }
}
