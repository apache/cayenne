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
package org.apache.cayenne.unit.di;

import org.apache.cayenne.graph.ArcId;
import org.apache.cayenne.graph.GraphChangeHandler;

public class DataChannelSyncStats implements GraphChangeHandler {

    public int arcsCreated;
    public int arcsDeleted;
    public int nodesCreated;
    public int nodeIdsChanged;
    public int nodePropertiesChanged;
    public int nodesRemoved;

    @Override
    public void arcCreated(Object nodeId, Object targetNodeId, ArcId arcId) {
        arcsCreated++;
    }

    @Override
    public void arcDeleted(Object nodeId, Object targetNodeId, ArcId arcId) {
        arcsDeleted++;
    }

    @Override
    public void nodeCreated(Object nodeId) {
        nodesCreated++;
    }

    @Override
    public void nodeIdChanged(Object nodeId, Object newId) {
        nodeIdsChanged++;
    }

    @Override
    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {
        nodePropertiesChanged++;
    }

    @Override
    public void nodeRemoved(Object nodeId) {
        nodesRemoved++;
    }
}