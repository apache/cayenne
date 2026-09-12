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

import org.apache.cayenne.ObjectId;
/**
 * Defines callback API that can be used by object graph nodes to notify of their state
 * changes. Graph nodes can be any objects as long as each node supports a notion of a
 * unique id within the graph and each directional arc has a unique identifier within its
 * source node.
 * 
 * @since 1.2
 */
public interface GraphChangeHandler {

    /**
     * Notifies implementing object that a node was assigned a new id.
     */
    default void nodeIdChanged(ObjectId id, ObjectId newId) {
    }

    /**
     * Notifies implementing object that a new node was created in the graph.
     */
    default void nodeCreated(ObjectId id) {
    }

    /**
     * Notifies implementing object that a node was removed from the graph.
     */
    default void nodeRemoved(ObjectId id) {
    }

    /**
     * Notifies implementing object that a node's property was modified.
     */
    default void nodePropertyChanged(
            ObjectId id,
            String property,
            Object oldValue,
            Object newValue) {
    }

    /**
     * Notifies implementing object that a new arc was created between two nodes.
     */
    default void arcCreated(ObjectId id, ObjectId targetId, ArcId arcId) {
    }

    /**
     * Notifies implementing object that an arc between two nodes was deleted.
     */
    default void arcDeleted(ObjectId id, ObjectId targetId, ArcId arcId) {
    }
}
