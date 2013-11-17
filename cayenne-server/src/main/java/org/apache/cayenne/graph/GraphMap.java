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

package org.apache.cayenne.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A base implementation of GraphManager that stores graph nodes keyed by their ids.
 * <h3>Tracking Object Changes</h3>
 * <p>
 * Registered objects may choose to notify GraphMap of their changes by using callback
 * methods defined in GraphChangeHandler interface. GraphMap itself implements as noops,
 * leaving it up to subclasses to handle object updates.
 * </p>
 * 
 * @since 1.2
 */
public class GraphMap implements GraphManager {

    protected Map<Object, Object> nodes;

    /**
     * Creates a new GraphMap.
     */
    public GraphMap() {
        this.nodes = new HashMap<Object, Object>();
    }

    // *** GraphMap methods

    /**
     * Returns an immutable collection of registered nodes.
     */
    public Collection<Object> registeredNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public synchronized Object getNode(Object nodeId) {
        return nodes.get(nodeId);
    }

    public synchronized void registerNode(Object nodeId, Object nodeObject) {
        nodes.put(nodeId, nodeObject);
    }

    public synchronized Object unregisterNode(Object nodeId) {
        return nodes.remove(nodeId);
    }

    // *** methods for tracking local changes declared in GraphChangeHandler interface
    
    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        // noop
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        // noop
    }

    public void nodeCreated(Object nodeId) {
        // noop
    }

    public void nodeRemoved(Object nodeId) {
        // noop
    }

    public void nodeIdChanged(Object nodeId, Object newId) {
        // noop
    }

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {
        // noop
    }
}
