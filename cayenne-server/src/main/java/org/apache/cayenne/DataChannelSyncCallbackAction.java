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
package org.apache.cayenne;

import org.apache.cayenne.graph.ArcId;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 3.1
 */
// note: made public in 3.1 to be used in all tiers
public abstract class DataChannelSyncCallbackAction implements GraphChangeHandler {

    static enum Op {
        INSERT, UPDATE, DELETE
    }

    public static DataChannelSyncCallbackAction getCallbackAction(
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
    Collection<Object> updated;
    Collection<Object> persisted;
    Collection<Object> removed;
    private Map<Object, Op> seenIds;
    private GraphManager graphManager;

    DataChannelSyncCallbackAction(LifecycleCallbackRegistry callbackRegistry,
            GraphManager graphManager, GraphDiff changes) {

        this.callbackRegistry = callbackRegistry;
        this.graphManager = graphManager;

        if (hasListeners()) {
            this.seenIds = new HashMap<>();
            changes.apply(this);
        }
    }

    protected abstract boolean hasListeners();

    public abstract void applyPreCommit();

    public abstract void applyPostCommit();

    void apply(LifecycleEvent callbackType, Collection<?> objects) {
        if (seenIds != null && objects != null) {
            callbackRegistry.performCallbacks(callbackType, objects);
        }
    }

    @Override
    public void nodeCreated(Object nodeId) {
        Op op = seenIds.put(nodeId, Op.INSERT);
        if (op == null) {

            Object node = graphManager.getNode(nodeId);
            if (node != null) {

                if (persisted == null) {
                    persisted = new ArrayList<>();
                }

                persisted.add(node);
            }
        }
    }

    @Override
    public void nodeRemoved(Object nodeId) {
        Op op = seenIds.put(nodeId, Op.DELETE);
        
        // the node may have been updated prior to delete
        if (op != Op.DELETE) {

            Object node = graphManager.getNode(nodeId);
            if (node != null) {

                if (removed == null) {
                    removed = new ArrayList<>();
                }

                removed.add(node);

                if (op == Op.UPDATE) {
                    updated.remove(node);
                }

                // don't care about preceding Op.INSERT, as NEW -> DELETED objects are
                // purged from the change log upstream and we don't see them here
            }
        }
    }

    @Override
    public void arcCreated(Object nodeId, Object targetNodeId, ArcId arcId) {
        // TODO: andrus, 9/21/2006 - should we register to-many relationship updates?
        nodeUpdated(nodeId);
    }

    @Override
    public void arcDeleted(Object nodeId, Object targetNodeId, ArcId arcId) {
        // TODO: andrus, 9/21/2006 - should we register to-many relationship updates?
        nodeUpdated(nodeId);
    }

    @Override
    public void nodeIdChanged(Object nodeId, Object newId) {
    }

    @Override
    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {
        nodeUpdated(nodeId);
    }

    private void nodeUpdated(Object nodeId) {
        Op op = seenIds.put(nodeId, Op.UPDATE);
        
        if (op == null) {

            Object node = graphManager.getNode(nodeId);
            if (node != null) {

                if (updated == null) {
                    updated = new ArrayList<>();
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
        public void applyPreCommit() {
            apply(LifecycleEvent.PRE_PERSIST, persisted);
            apply(LifecycleEvent.PRE_UPDATE, updated);
        }

        @Override
        public void applyPostCommit() {
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
        public void applyPreCommit() {
            // noop
        }

        @Override
        public void applyPostCommit() {
            apply(LifecycleEvent.POST_LOAD, updated);
            apply(LifecycleEvent.POST_LOAD, removed);
        }
    }
}
