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

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * An operation that merges changes from a single object registered in some ObjectContext,
 * to a peer object in an ObjectConext that is a child of that context.
 * 
 * @since 3.1
 */
class ShallowMergeOperation {

    private final DataContext context;

    public ShallowMergeOperation(DataContext context) {
        this.context = context;
    }

    public <T extends Persistent> T merge(T peerInParentContext) {

        if (peerInParentContext == null) {
            throw new IllegalArgumentException("Null peerInParentContext");
        }

        // handling of HOLLOW peer state is here for completeness... Wonder if we ever
        // have a case where it is applicable.
        int peerState = peerInParentContext.getPersistenceState();

        ObjectId id = peerInParentContext.getObjectId();

        ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(
                id.getEntityName());

        DataContextObjectStore objectStore = context.getObjectStore();

        // a new object starts HOLLOW, and is merged below if the peer has state to merge
        T object = (T) objectStore.getOrCreateObject(id);

        // TODO: Andrus, 1/24/2006 implement smart merge for modified objects...
        if (peerState != PersistenceState.HOLLOW) {
            objectStore.withObjectLock(id, () -> {
                int state = object.getPersistenceState();

                // the peer's snapshot version is carried over, so that a newer merge is not undone by an older one
                if (state != PersistenceState.MODIFIED
                        && state != PersistenceState.DELETED
                        && peerInParentContext.getSnapshotVersion() >= object.getSnapshotVersion()) {

                    descriptor.shallowMerge(peerInParentContext, object);
                    object.setSnapshotVersion(peerInParentContext.getSnapshotVersion());

                    if (object.getPersistenceState() == PersistenceState.HOLLOW) {
                        object.setPersistenceState(PersistenceState.COMMITTED);
                    }
                }
                return null;
            });
        }

        return object;
    }
}
