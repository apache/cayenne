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

/**
 * @since 1.2
 */
public class NodeDeleteOperation extends NodeDiff {

    public NodeDeleteOperation(Object nodeId) {
        super(nodeId);
    }
    
    public NodeDeleteOperation(Object nodeId, int diffId) {
        super(nodeId, diffId);
    }

    @Override
    public void apply(GraphChangeHandler tracker) {
        tracker.nodeRemoved(nodeId);
    }

    @Override
    public void undo(GraphChangeHandler tracker) {
        tracker.nodeCreated(nodeId);
    }
}
