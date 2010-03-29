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
package org.apache.cayenne.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;

/**
 * @since 3.0
 */
abstract class DataChannelSyncCallbackAction implements GraphChangeHandler {

    static DataChannelSyncCallbackAction getCallbackAction(
            LifecycleCallbackRegistry callbackRegistry,
            GraphManager graphManager,
            GraphDiff changes,
            int syncType) {

        switch (syncType) {
            case DataChannel.FLUSH_CASCADE_SYNC:
            case DataChannel.FLUSH_NOCASCADE_SYNC:
                return new FlushCallbackAction(callbackRegistry, graphManager, changes);
            case DataChannel.ROLLBACK_CASCADE_SYNC:
                return new RollbackCallbackAction(callbackRegistry, graphManager, changes);
            default:
                throw new IllegalArgumentException("Unsupported sync type: " + syncType);
        }
    }

    LifecycleCallbackRegistry callbackRegistry;
    Collection updated;
    Collection persisted;
    Collection removed;
    private Set<Object> seenIds;
    private GraphManager graphManager;

    DataChannelSyncCallbackAction(LifecycleCallbackRegistry callbackRegistry,
            GraphManager graphManager, GraphDiff changes) {

        this.callbackRegistry = callbackRegistry;
        this.graphManager = graphManager;

        if (hasListeners()) {
            this.seenIds = new HashSet<Object>();
            changes.apply(this);
        }
    }

    protected abstract boolean hasListeners();

    abstract void applyPreCommit();

    abstract void applyPostCommit();

    void apply(LifecycleEvent callbackType, Collection<?> objects) {
        if (seenIds != null && objects != null) {
            callbackRegistry.performCallbacks(callbackType, objects);
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

    static class FlushCallbackAction extends DataChannelSyncCallbackAction {

        FlushCallbackAction(LifecycleCallbackRegistry callbackRegistry,
                GraphManager graphManager, GraphDiff changes) {
            super(callbackRegistry, graphManager, changes);
        }

        @Override
        protected boolean hasListeners() {
            return !(callbackRegistry.isEmpty(LifecycleEvent.PRE_UPDATE)
                    && callbackRegistry.isEmpty(LifecycleEvent.PRE_PERSIST)
                    && callbackRegistry.isEmpty(LifecycleEvent.POST_UPDATE)
                    && callbackRegistry.isEmpty(LifecycleEvent.POST_REMOVE) && callbackRegistry
                    .isEmpty(LifecycleEvent.POST_PERSIST));
        }

        @Override
        void applyPreCommit() {
            apply(LifecycleEvent.PRE_PERSIST, persisted);
            apply(LifecycleEvent.PRE_UPDATE, updated);
        }

        @Override
        void applyPostCommit() {
            apply(LifecycleEvent.POST_UPDATE, updated);
            apply(LifecycleEvent.POST_REMOVE, removed);
            apply(LifecycleEvent.POST_PERSIST, persisted);
        }
    }

    static class RollbackCallbackAction extends DataChannelSyncCallbackAction {

        RollbackCallbackAction(LifecycleCallbackRegistry callbackRegistry,
                GraphManager graphManager, GraphDiff changes) {
            super(callbackRegistry, graphManager, changes);
        }

        @Override
        protected boolean hasListeners() {
            return !callbackRegistry.isEmpty(LifecycleEvent.POST_LOAD);
        }

        @Override
        void applyPreCommit() {
            // noop
        }

        @Override
        void applyPostCommit() {
            apply(LifecycleEvent.POST_LOAD, updated);
            apply(LifecycleEvent.POST_LOAD, removed);
        }
    }
}
