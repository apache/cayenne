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

import java.io.Serializable;

/**
 * Represents a change in an object graph. This can be a simple change (like a node
 * property update) or a composite change that consists of a number of smaller changes.
 * 
 * @since 1.2
 */
public interface GraphDiff extends Serializable {

    /**
     * Returns true if this diff is simply a placeholder and does not perform any actual
     * operation.
     */
    boolean isNoop();

    /**
     * Calls appropriate methods on the handler to "replay" this change.
     */
    void apply(GraphChangeHandler handler);

    /**
     * Calls appropriate methods on the handler to revert this change.
     */
    void undo(GraphChangeHandler handler);
}
