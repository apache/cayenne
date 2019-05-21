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

package org.apache.cayenne.graph;

import java.util.Collection;

/**
 * Represents a generic "managed" graph with nodes mapped by their ids. Inherited
 * GraphChangeHandler methods are intended as callbacks for graph node objects to notify
 * graph of their changes.
 * 
 * @since 1.2
 */
public interface GraphManager extends GraphChangeHandler {

    /**
     * Returns a graph node given an id.
     */
    Object getNode(Object nodeId);

    /**
     * "Registers" a graph node, usually storing the node in some internal map using its
     * id as a key.
     */
    void registerNode(Object nodeId, Object nodeObject);

    /**
     * "Unregisters" a graph node, forgetting any information associated with nodeId.
     */
    Object unregisterNode(Object nodeId);

    /**
     * Returns all graph nodes registered with GraphManager.
     */
    Collection<Object> registeredNodes();
}
