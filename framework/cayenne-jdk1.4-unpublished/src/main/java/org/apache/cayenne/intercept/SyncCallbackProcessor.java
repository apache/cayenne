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
package org.apache.cayenne.intercept;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.map.CallbackMap;

class SyncCallbackProcessor implements GraphChangeHandler {

    final DataChannelCallbackInterceptor interceptor;
    private GraphManager graphManager;
    Collection updated;
    Collection persisted;
    Collection removed;
    private Set seenIds;

    SyncCallbackProcessor(DataChannelCallbackInterceptor interceptor,
            GraphManager graphManager, GraphDiff changes) {
        this.interceptor = interceptor;
        this.seenIds = new HashSet();
        this.graphManager = graphManager;
        changes.apply(this);
    }

    void applyPreCommit(int syncType) {
        switch (syncType) {
            case DataChannel.FLUSH_CASCADE_SYNC:
            case DataChannel.FLUSH_NOCASCADE_SYNC:
                apply(CallbackMap.PRE_UPDATE, updated);

                if (interceptor.isContextCallbacksEnabled()) {
                    apply(CallbackMap.PRE_PERSIST, persisted);
                    apply(CallbackMap.PRE_REMOVE, removed);
                }
        }
    }

    void applyPostCommit(int syncType) {
        switch (syncType) {
            case DataChannel.FLUSH_CASCADE_SYNC:
            case DataChannel.FLUSH_NOCASCADE_SYNC:
                apply(CallbackMap.POST_UPDATE, updated);
                apply(CallbackMap.POST_REMOVE, removed);
                apply(CallbackMap.POST_PERSIST, persisted);
                break;
            case DataChannel.ROLLBACK_CASCADE_SYNC:
                apply(CallbackMap.POST_LOAD, updated);
                apply(CallbackMap.POST_LOAD, removed);
        }
    }

    void apply(int callbackType, Collection objects) {
        if (objects != null) {
            interceptor.getCallbackRegistry().performCallbacks(callbackType, objects);
        }
    }

    public void nodeCreated(Object nodeId) {
        if (seenIds.add(nodeId)) {

            Object node = graphManager.getNode(nodeId);
            if (node != null) {

                if (persisted == null) {
                    persisted = new ArrayList();
                }

                persisted.add(node);
            }
        }
    }

    public void nodeRemoved(Object nodeId) {
        if (seenIds.add(nodeId)) {

            Object node = graphManager.getNode(nodeId);
            if (node != null) {

                if (removed == null) {
                    removed = new ArrayList();
                }

                removed.add(node);
            }
        }
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        // TODO: andrus, 9/21/2006 - should we register to-many relationship updates?
        nodeUpdated(nodeId);
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        // TODO: andrus, 9/21/2006 - should we register to-many relationship updates?
        nodeUpdated(nodeId);
    }

    public void nodeIdChanged(Object nodeId, Object newId) {
    }

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {
        nodeUpdated(nodeId);
    }

    private void nodeUpdated(Object nodeId) {
        if (seenIds.add(nodeId)) {

            Object node = graphManager.getNode(nodeId);
            if (node != null) {

                if (updated == null) {
                    updated = new ArrayList();
                }

                updated.add(node);
            }
        }
    }
}