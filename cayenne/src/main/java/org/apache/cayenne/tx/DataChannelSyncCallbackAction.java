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
package org.apache.cayenne.tx;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectStore;
import org.apache.cayenne.graph.ArcId;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.access.lifecycle.LifecycleEventManager;
import org.apache.cayenne.access.lifecycle.LifecycleEventsManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

abstract class DataChannelSyncCallbackAction implements GraphChangeHandler {

    enum Op {
        INSERT, UPDATE, DELETE
    }

    public static DataChannelSyncCallbackAction getCallbackAction(
            LifecycleEventsManager lifecycleEventsManager,
            ObjectStore objectStore,
            GraphDiff changes,
            int syncType) {

        return switch (syncType) {
            case DataChannel.FLUSH_CASCADE_SYNC, DataChannel.FLUSH_NOCASCADE_SYNC ->
                    new FlushCallbackAction(lifecycleEventsManager, objectStore, changes);
            case DataChannel.ROLLBACK_CASCADE_SYNC ->
                    new RollbackCallbackAction(lifecycleEventsManager, objectStore, changes);
            default -> throw new IllegalArgumentException("Unsupported sync type: " + syncType);
        };
    }

    LifecycleEventsManager lifecycleEventsManager;
    Collection<Persistent> updated;
    Collection<Persistent> persisted;
    Collection<Persistent> removed;

    private Map<ObjectId, Op> seenIds;
    private final ObjectStore objectStore;

    DataChannelSyncCallbackAction(
            LifecycleEventsManager lifecycleEventsManager,
            ObjectStore objectStore,
            GraphDiff changes) {

        this.lifecycleEventsManager = lifecycleEventsManager;
        this.objectStore = objectStore;

        if (hasListeners()) {
            this.seenIds = new HashMap<>();
            changes.apply(this);
        }
    }

    protected abstract boolean hasListeners();

    public abstract void applyPreCommit();

    public abstract void applyPostCommit();

    void apply(LifecycleEventManager eventManager, Collection<Persistent> objects) {
        if (seenIds != null && objects != null) {
            eventManager.onEvent(objects);
        }
    }

    @Override
    public void nodeCreated(ObjectId id) {
        Op op = seenIds.put(id, Op.INSERT);
        if (op == null) {

            Persistent node = objectStore.getObject(id);
            if (node != null) {

                if (persisted == null) {
                    persisted = new ArrayList<>();
                }

                persisted.add(node);
            }
        }
    }

    @Override
    public void nodeRemoved(ObjectId id) {
        Op op = seenIds.put(id, Op.DELETE);

        // the node may have been updated prior to delete
        if (op != Op.DELETE) {

            Persistent node = objectStore.getObject(id);
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
    public void arcCreated(ObjectId id, ObjectId targetId, ArcId arcId) {
        // TODO: andrus, 9/21/2006 - should we register to-many relationship updates?
        nodeUpdated(id);
    }

    @Override
    public void arcDeleted(ObjectId id, ObjectId targetId, ArcId arcId) {
        // TODO: andrus, 9/21/2006 - should we register to-many relationship updates?
        nodeUpdated(id);
    }

    @Override
    public void nodePropertyChanged(
            ObjectId id,
            String property,
            Object oldValue,
            Object newValue) {
        nodeUpdated(id);
    }

    private void nodeUpdated(ObjectId id) {
        Op op = seenIds.put(id, Op.UPDATE);

        if (op == null) {

            Persistent node = objectStore.getObject(id);
            if (node != null) {

                if (updated == null) {
                    updated = new ArrayList<>();
                }

                updated.add(node);
            }
        }
    }

    static class FlushCallbackAction extends DataChannelSyncCallbackAction {

        FlushCallbackAction(LifecycleEventsManager lifecycleEventsManager,
                            ObjectStore objectStore, GraphDiff changes) {
            super(lifecycleEventsManager, objectStore, changes);
        }

        @Override
        protected boolean hasListeners() {
            return lifecycleEventsManager.preUpdate().hasListeners()
                    || lifecycleEventsManager.prePersist().hasListeners()
                    || lifecycleEventsManager.postUpdate().hasListeners()
                    || lifecycleEventsManager.postRemove().hasListeners()
                    || lifecycleEventsManager.postPersist().hasListeners();
        }

        @Override
        public void applyPreCommit() {
            apply(lifecycleEventsManager.prePersist(), persisted);
            apply(lifecycleEventsManager.preUpdate(), updated);
        }

        @Override
        public void applyPostCommit() {
            apply(lifecycleEventsManager.postUpdate(), updated);
            apply(lifecycleEventsManager.postRemove(), removed);
            apply(lifecycleEventsManager.postPersist(), persisted);
        }
    }

    static class RollbackCallbackAction extends DataChannelSyncCallbackAction {

        RollbackCallbackAction(LifecycleEventsManager lifecycleEventsManager,
                               ObjectStore objectStore, GraphDiff changes) {
            super(lifecycleEventsManager, objectStore, changes);
        }

        @Override
        protected boolean hasListeners() {
            return lifecycleEventsManager.postLoad().hasListeners();
        }

        @Override
        public void applyPreCommit() {
            // noop
        }

        @Override
        public void applyPostCommit() {
            apply(lifecycleEventsManager.postLoad(), updated);
            apply(lifecycleEventsManager.postLoad(), removed);
        }
    }
}
