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
import java.util.HashMap;
import java.util.Map;

public class MockGraphManager implements GraphManager {

    protected Map<Object, Object> map;

    public MockGraphManager() {
        this.map = new HashMap<Object, Object>();
    }

    public Collection<Object> registeredNodes() {
        return map.values();
    }

    public Object getNode(Object nodeId) {
        return map.get(nodeId);
    }

    public void registerNode(Object nodeId, Object nodeObject) {
        map.put(nodeId, nodeObject);
    }

    public Object unregisterNode(Object nodeId) {
        return map.remove(nodeId);
    }
    
    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
    }

    public void graphCommitAborted() {
    }

    public void graphCommitStarted() {
    }

    public void graphCommitted() {
    }

    public void graphRolledback() {
    }

    public void nodeCreated(Object nodeId) {
    }

    public void nodeIdChanged(Object nodeId, Object newId) {
    }

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {
    }

    public void nodeRemoved(Object nodeId) {
    }

}
