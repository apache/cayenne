/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphManager;

/**
 * Tracks dirty Persistent objects.
 * 
 * @since 1.2
 */
class ObjectContextStateLog implements GraphChangeHandler {

    Set<Object> dirtyIds;
    GraphManager graphManager;

    ObjectContextStateLog(GraphManager graphManager) {
        this.dirtyIds = new HashSet<Object>();
        this.graphManager = graphManager;
    }

    void clear() {
        dirtyIds = new HashSet<Object>();
    }

    /**
     * Updates dirty objects state and clears dirty ids map.
     */
    void graphCommitted() {
        /**
         * Array for deleted ids, to avoid concurrent modification
         */
        List deletedIds = new Vector();
        
        for (Object id : dirtyIds) {
            Object node = graphManager.getNode(id);
            if (node instanceof Persistent) {
                Persistent persistentNode = (Persistent) node;
                switch (persistentNode.getPersistenceState()) {
                    case PersistenceState.MODIFIED:
                    case PersistenceState.NEW:
                        persistentNode.setPersistenceState(PersistenceState.COMMITTED);
                        break;
                    case PersistenceState.DELETED:
                        deletedIds.add(id);
                        persistentNode.setPersistenceState(PersistenceState.TRANSIENT);
                        break;
                }
            }
        }
        
        /**
         * Now unregister all deleted objects
         */
        for (Object id : deletedIds) {
            graphManager.unregisterNode(id);
        }

        clear();
    }

    void graphReverted() {
        for (Object id : dirtyIds) {
            Object node = graphManager.getNode(id);
            if (node instanceof Persistent) {
                Persistent persistentNode = (Persistent) node;
                switch (persistentNode.getPersistenceState()) {
                    case PersistenceState.MODIFIED:
                    case PersistenceState.DELETED:
                        persistentNode.setPersistenceState(PersistenceState.COMMITTED);
                        break;
                    case PersistenceState.NEW:
                        persistentNode.setPersistenceState(PersistenceState.TRANSIENT);
                        break;
                }
            }
        }

        clear();
    }

    boolean hasChanges() {
        return !dirtyIds.isEmpty();
    }

    Collection<Object> dirtyIds() {
        return dirtyIds;
    }

    Collection<Object> dirtyNodes() {
        if (dirtyIds.isEmpty()) {
            return Collections.EMPTY_SET;
        }

        Collection<Object> objects = new ArrayList<Object>(dirtyIds.size());
        for (Object id : dirtyIds) {
            objects.add(graphManager.getNode(id));
        }

        return objects;
    }

    Collection<Object> dirtyNodes(int state) {
        if (dirtyIds.isEmpty()) {
            return Collections.EMPTY_SET;
        }

        int size = dirtyIds.size();
        Collection<Object> objects = new ArrayList<Object>(size > 50 ? size / 2 : size);
        for (Object id : dirtyIds) {
            Persistent o = (Persistent) graphManager.getNode(id);

            if (o.getPersistenceState() == state) {
                objects.add(o);
            }
        }

        return objects;
    }

    void unregisterNode(Object nodeId) {
        dirtyIds.remove(nodeId);
    }

    // *** GraphChangeHandler methods

    public void nodeIdChanged(Object nodeId, Object newId) {
        if (dirtyIds.remove(nodeId)) {
            dirtyIds.add(newId);
        }
    }

    public void nodeCreated(Object nodeId) {
        dirtyIds.add(nodeId);
    }

    public void nodeRemoved(Object nodeId) {
        dirtyIds.add(nodeId);
    }

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {
        dirtyIds.add(nodeId);
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        dirtyIds.add(nodeId);
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        dirtyIds.add(nodeId);
    }

}
