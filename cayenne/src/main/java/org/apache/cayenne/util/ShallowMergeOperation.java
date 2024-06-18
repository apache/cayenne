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
package org.apache.cayenne.util;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * An operation that merges changes from a single object registered in some ObjectContext,
 * to a peer object in an ObjectConext that is a child of that context.
 * 
 * @since 3.1
 */
public class ShallowMergeOperation {

    private final ObjectContext context;

    public ShallowMergeOperation(ObjectContext context) {
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

        GraphManager graphManager = context.getGraphManager();

        // have to synchronize almost the entire method to prevent multiple threads from
        // messing up Persistent objects per CAY-845.
        synchronized (graphManager) {
            T object = (T) graphManager.getNode(id);

            // merge into an existing object
            if (object == null) {
                object = (T) descriptor.createObject();
                object.setObjectContext(context);
                object.setObjectId(id);

                if (peerState == PersistenceState.HOLLOW) {
                    object.setPersistenceState(PersistenceState.HOLLOW);
                }
                else {
                    object.setPersistenceState(PersistenceState.COMMITTED);
                }

                graphManager.registerNode(id, object);
            }

            // TODO: Andrus, 1/24/2006 implement smart merge for modified objects...
            if (peerState != PersistenceState.HOLLOW
                    && object.getPersistenceState() != PersistenceState.MODIFIED
                    && object.getPersistenceState() != PersistenceState.DELETED) {

                descriptor.shallowMerge(peerInParentContext, object);

                if (object.getPersistenceState() == PersistenceState.HOLLOW) {
                    object.setPersistenceState(PersistenceState.COMMITTED);
                }
            }

            return object;
        }
    }
}
