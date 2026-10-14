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

package org.apache.cayenne.access;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.ObjectStore;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.ObjectDiff.ArcOperation;
import org.apache.cayenne.access.event.SnapshotEvent;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.graph.ArcId;
import org.apache.cayenne.graph.ChildDiffLoader;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.NodeCreateOperation;
import org.apache.cayenne.graph.NodeDeleteOperation;
import org.apache.cayenne.graph.NodeDiff;
import org.apache.cayenne.graph.NodePropertyChangeOperation;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyDescriptor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * The default {@link ObjectStore} implementation. Exposes a few locks for the callers to provide concurrency:
 * per-object {@link #withObjectLock(ObjectId, Supplier) object lock} and broader {@link #withChangesLock(Supplier) changes lock} used by commits, etc. To prevent
 * deadlocks, the changes lock is always taken before any object lock.
 *
 * @since 5.0
 */
public class DataContextObjectStore implements ObjectStore {

    // the number of monitors guarding object property updates on read. Updates of different objects contend only when
    // the objects happen to share a monitor, and only for the duration of an in-memory merge. The threads that can be
    // merging at the same instant are bounded by the cores, so the count follows them: 4x the cores, up to 64, as a
    // power of two.
    private static final int OBJECT_LOCK_STRIPES = Math.min(
            64,
            Integer.highestOneBit(Runtime.getRuntime().availableProcessors() * 4 * 2 - 1));

    protected final DataContext context;
    protected final DataRowStore dataRowCache;
    protected final Map<ObjectId, Persistent> objectMap;

    private final ReentrantLock changesLock;
    protected volatile Map<ObjectId, ObjectDiff> changes;

    /**
     * Presence of path in this map is used to separate insert from update case of flattened records.
     */
    protected final Map<ObjectId, Map<CayennePath, ObjectId>> trackedFlattenedPaths;
    private Collection<GraphDiff> lifecycleEventInducedChanges;
    private final ChangeRecorder changeRecorder;

    // created on the first write, as not every context sees one
    private volatile Object[] locks;

    // a sequential id used to tag GraphDiffs so that they can later be sorted in the
    // original creation order. Guarded by the changes lock
    int currentDiffId;

    public DataContextObjectStore(
            DataContext context,
            DataRowStore dataRowCache,
            Map<ObjectId, Persistent> objectMap,
            boolean syncWithSnapshotCache) {

        this.context = Objects.requireNonNull(context);
        this.dataRowCache = dataRowCache;
        this.changeRecorder = new ChangeRecorder();
        this.changesLock = new ReentrantLock();

        this.objectMap = Objects.requireNonNull(objectMap, "Object map is null.");
        if (objectMap instanceof ReferenceMap<ObjectId, Persistent> referenceMap) {
            referenceMap.setKeyCleanupCallback(this::onObjectKeyCleanup);
        }

        this.changes = new ConcurrentHashMap<>();
        this.trackedFlattenedPaths = new ConcurrentHashMap<>();

        if (syncWithSnapshotCache && dataRowCache != null && dataRowCache.getEventManager() != null) {
            // setting itself as non-blocking listener, since the event sending thread is likely in the middle of a
            // query or a commit on the sender's context, and should not wait for the listeners
            dataRowCache.getEventManager().addNonBlockingListener(
                    this,
                    SnapshotEvent.class,
                    DataContextObjectStore::snapshotsChanged,
                    dataRowCache.getSnapshotEventSubject(),
                    dataRowCache);
        }
    }

    void childContextSyncStarted() {
        lifecycleEventInducedChanges = new ArrayList<>();
    }

    void childContextSyncStopped() {
        lifecycleEventInducedChanges = null;
    }

    Collection<GraphDiff> getLifecycleEventInducedChanges() {
        return lifecycleEventInducedChanges != null
                ? lifecycleEventInducedChanges
                : Collections.emptyList();
    }

    void registerLifecycleEventInducedChange(GraphDiff diff) {
        if (ChildDiffLoader.isProcessingChildDiff()) {
            // reset so that subsequent event-induced changes could get registered...
            ChildDiffLoader.setExternalChange(Boolean.FALSE);
        } else {
            lifecycleEventInducedChanges.add(diff);
        }
    }

    /**
     * Runs the action under the lock guarding the uncommitted changes, returning its result. A commit holds the lock
     * for its whole duration, so that no changes are recorded while the ones being committed are read. The lock is
     * reentrant. It is always taken before an object lock, never while holding one.
     */
    public <T> T withChangesLock(Supplier<T> action) {
        changesLock.lock();
        try {
            return action.get();
        } finally {
            changesLock.unlock();
        }
    }

    /**
     * Returns the monitor guarding writes to the state of the object with the id: attribute and relationship values,
     * persistence state and snapshot version. Every writer of object state synchronizes on it for the duration of a
     * write, so that concurrent writes do not interleave and other writers never see an object half-written. Readers
     * do not synchronize. A monitor may be shared by several ids, so a writer must not hold more than one at a time,
     * and must not perform blocking operations, such as queries, while holding it.
     */
    public <T> T withObjectLock(ObjectId id, Supplier<T> action) {
        synchronized (objectLock(id)) {
            return action.get();
        }
    }

    private Object objectLock(ObjectId id) {
        Object[] locks = this.locks;
        if (locks == null) {
            synchronized (this) {
                locks = this.locks;
                if (locks == null) {
                    locks = new Object[OBJECT_LOCK_STRIPES];
                    for (int i = 0; i < OBJECT_LOCK_STRIPES; i++) {
                        locks[i] = new Object();
                    }
                    this.locks = locks;
                }
            }
        }

        int h = id.hashCode();
        return locks[(h ^ (h >>> 16)) & (OBJECT_LOCK_STRIPES - 1)];
    }

    /**
     * Registers object change.
     */
    ObjectDiff registerDiff(ObjectId id, NodeDiff diff) {

        return withChangesLock(() -> {
            if (diff != null) {
                diff.setDiffId(++currentDiffId);
            }

            ObjectDiff objectDiff = changes.get(id);

            if (objectDiff == null) {

                Persistent object = objectMap.get(id);
                if (object == null) {
                    throw new CayenneRuntimeException("No object is registered in context with Id %s", id);
                }

                // the state transition and the baseline of the object taken by ObjectDiff are done under the object
                // lock, so that a concurrent merge of a newer snapshot does not interleave with them
                objectDiff = withObjectLock(id, () -> {
                    if (object.getPersistenceState() == PersistenceState.COMMITTED) {
                        object.setPersistenceState(PersistenceState.MODIFIED);

                        // the object may lag behind the cached snapshot, as a snapshot from a newer statement is cached
                        // without refreshing the objects that are not part of its result
                        DataRow snapshot = getCachedSnapshot(id);
                        if (snapshot != null && snapshot.getVersion() > object.getSnapshotVersion()) {
                            DataContextDelegate delegate = context.nonNullDelegate();
                            if (delegate.shouldMergeChanges(object, snapshot)) {
                                ClassDescriptor descriptor = context
                                        .getEntityResolver()
                                        .getClassDescriptor(id.getEntityName());
                                DataRowUtils.forceMergeWithSnapshot(context, descriptor, object, snapshot);
                                object.setSnapshotVersion(snapshot.getVersion());
                                delegate.finishedMergeChanges(object);
                            }
                        }
                    }

                    return new ObjectDiff(object);
                });

                objectDiff.setDiffId(++currentDiffId);
                changes.put(id, objectDiff);
            }

            if (diff != null) {
                objectDiff.addDiff(diff, this);
            }

            return objectDiff;
        });
    }

    /**
     * Returns a number of objects currently registered with this ObjectStore.
     */
    public int registeredObjectsCount() {
        return objectMap.size();
    }

    /**
     * Returns a DataRowStore associated with this ObjectStore.
     */
    public DataRowStore getDataRowCache() {
        return dataRowCache;
    }

    /**
     * Turns registered objects with the given ids HOLLOW, discarding their uncommitted changes. Ids of unregistered
     * or NEW objects are ignored. Does not touch the snapshot cache.
     */
    void objectsInvalidated(Collection<ObjectId> ids) {
        withChangesLock(() -> {
            for (ObjectId id : ids) {
                Persistent object = objectMap.get(id);

                // NEW objects have nothing to refetch. HOLLOW objects are still processed, as they may have pending
                // changes
                if (object == null || object.getPersistenceState() == PersistenceState.NEW) {
                    continue;
                }

                changes.remove(id);
                withObjectLock(id, () -> {
                    if (object.getPersistenceState() != PersistenceState.NEW) {
                        object.setPersistenceState(PersistenceState.HOLLOW);
                    }
                    return null;
                });
            }
            return null;
        });
    }

    /**
     * Evicts a collection of Persistent objects from the ObjectStore, invalidates the underlying
     * cache snapshots. Changes objects state to TRANSIENT. This method can be used for
     * manual cleanup of Cayenne cache.
     */
    // this method is exactly the same as "objectsInvalidated", only additionally it
    // throws out registered objects
    public void objectsUnregistered(Collection<? extends Persistent> objects) {
        if (objects.isEmpty()) {
            return;
        }

        Collection<ObjectId> ids = withChangesLock(() -> {
            Collection<ObjectId> unregistered = new ArrayList<>(objects.size());
            for (Persistent object : objects) {
                ObjectId id = object.getObjectId();

                // remove object but not snapshot
                if (id != null) {
                    objectMap.remove(id);
                    changes.remove(id);
                    trackedFlattenedPaths.remove(id);

                    withObjectLock(id, () -> {
                        object.setObjectContext(null);
                        object.setPersistenceState(PersistenceState.TRANSIENT);
                        return null;
                    });
                } else {
                    object.setObjectContext(null);
                    object.setPersistenceState(PersistenceState.TRANSIENT);
                }

                unregistered.add(id);
            }
            return unregistered;
        });

        // TODO, andrus 3/28/2006 - DRC is null in nested contexts... implement
        // propagation of unregister operation through the stack ... or do the opposite
        // and keep unregister local even for non-nested DC?
        if (getDataRowCache() != null) {
            // send an event for removed snapshots
            getDataRowCache().processSnapshotChanges(
                    this,
                    Collections.emptyMap(),
                    Collections.emptyList(),
                    ids,
                    Collections.emptyList());
        }
    }

    /**
     * Reverts changes to all stored uncomitted objects.
     */
    public void objectsRolledBack() {
        withChangesLock(() -> {
            for (Persistent object : registeredObjects()) {
                ObjectId id = object.getObjectId();
                withObjectLock(id, () -> {
                    switch (object.getPersistenceState()) {
                        case PersistenceState.NEW -> {
                            objectMap.remove(id);
                            object.setObjectContext(null);
                            object.setObjectId(null);
                            object.setPersistenceState(PersistenceState.TRANSIENT);
                        }
                        // deleted is only a persistence state, so rolling back sets state to committed
                        case PersistenceState.DELETED, PersistenceState.MODIFIED ->
                                object.setPersistenceState(PersistenceState.HOLLOW);
                        default -> { /* Transient, committed and hollow need no handling */ }
                    }
                    return null;
                });
            }

            // reset changes ... using a new map to allow event listeners to analyze the
            // original changes map after the rollback
            this.changes = new ConcurrentHashMap<>();
            return null;
        });
    }

    /**
     * Builds and returns GraphDiff reflecting all uncommitted object changes.
     */
    ObjectStoreGraphDiff getChanges() {
        return new ObjectStoreGraphDiff(this);
    }

    /**
     * Returns internal changes map.
     */
    Map<ObjectId, ObjectDiff> getChangesByObjectId() {
        return changes;
    }

    void postprocessAfterPhantomCommit() {
        withChangesLock(() -> {
            for (ObjectId id : changes.keySet()) {

                Persistent object = objectMap.get(id);

                // assume that no new or deleted objects are present (as otherwise commit
                // wouldn't have been phantom).
                withObjectLock(id, () -> {
                    object.setPersistenceState(PersistenceState.COMMITTED);
                    return null;
                });
            }

            // clear caches
            this.changes.clear();
            return null;
        });
    }

    /**
     * Processes objects state after commit. Normally runs on the committing thread, that already holds the changes
     * lock for the duration of the commit.
     */
    public void postprocessAfterCommit(GraphDiff parentChanges) {

        withChangesLock(() -> {
            // scan through changed objects, set persistence state to committed
            for (ObjectId id : changes.keySet()) {
                Persistent object = objectMap.get(id);

                withObjectLock(id, () -> {
                    switch (object.getPersistenceState()) {
                        case PersistenceState.DELETED -> {
                            objectMap.remove(id);
                            trackedFlattenedPaths.remove(id);
                            object.setObjectContext(null);
                            object.setPersistenceState(PersistenceState.TRANSIENT);
                        }
                        case PersistenceState.NEW, PersistenceState.MODIFIED ->
                                object.setPersistenceState(PersistenceState.COMMITTED);
                    }
                    return null;
                });
            }

            // re-register changed object ids
            if (!parentChanges.isNoop()) {
                parentChanges.apply(new GraphChangeHandler() {
                    @Override
                    public void nodeIdChanged(ObjectId id, ObjectId newId) {
                        processIdChange(id, newId);
                    }
                });
            }

            // create new instance of changes map so that event listeners who stored the
            // original diff don't get affected
            this.changes = new ConcurrentHashMap<>();
            return null;
        });
    }

    /**
     * Returns a snapshot for ObjectId from the underlying snapshot cache, or null if the cache contains no snapshot.
     * Snapshots are keyed by the concrete entity of an object, so an id of a superentity is matched against the ids of
     * its subentities as well. A nested context has no snapshot cache of its own, so it takes the current state of the
     * object in the parent context as the snapshot.
     */
    @Override
    public DataRow getCachedSnapshot(ObjectId oid) {

        if (dataRowCache == null) {
            return parentContextSnapshot(oid, false);
        }

        EntityInheritanceTree inheritanceTree = context.getEntityResolver().getInheritanceTree(oid.getEntityName());
        for (ObjectId candidateId : inheritanceTree.polymorphicIds(oid)) {
            DataRow row = dataRowCache.getCachedSnapshot(candidateId);
            if (row != null) {
                return row;
            }
        }

        return null;
    }

    /**
     * Returns a snapshot for ObjectId from the underlying snapshot cache. If the cache contains no snapshot, it is
     * fetched from the database. Returns null if no matching row exists.
     *
     * @since 1.2
     */
    @Override
    public DataRow getSnapshot(ObjectId oid) {

        if (dataRowCache == null) {
            return parentContextSnapshot(oid, true);
        }

        DataRow row = getCachedSnapshot(oid);
        if (row != null) {
            return row;
        }

        // a temporary id without a replacement can't match anything in the database
        if (context.getChannel() == null || oid.isTemporary() && !oid.isReplacementIdAttached()) {
            return null;
        }

        ObjectSelect<DataRow> query = ObjectSelect
                .query(Persistent.class, oid.getEntityName())
                .where(ExpressionFactory.matchAllDbExp(oid.getIdSnapshot()))
                .fetchDataRows();

        List<?> rows = QueryResults.firstList(context.getChannel().onQuery(context, query, false));
        return switch (rows.size()) {
            case 0 -> null;
            case 1 -> (DataRow) rows.getFirst();
            default -> throw new CayenneRuntimeException(
                    "Expected zero or one row for id %s, instead query matched: %d", oid, rows.size());
        };
    }

    /**
     * Resolves a snapshot for a context without a snapshot cache of its own, i.e. a nested context. From the nested
     * context perspective, the parent context state is the committed state, so a snapshot is taken from the parent
     * object if there's one, and is looked up in the parent context store otherwise.
     */
    private DataRow parentContextSnapshot(ObjectId oid, boolean fetch) {

        if (!(context.getChannel() instanceof DataContextChannel(DataContext parent))) {
            return null;
        }

        DataContextObjectStore parentStore = parent.getObjectStore();
        EntityInheritanceTree inheritanceTree = parent.getEntityResolver().getInheritanceTree(oid.getEntityName());
        for (ObjectId candidateId : inheritanceTree.polymorphicIds(oid)) {
            Persistent parentObject = parentStore.getObject(candidateId);
            if (parentObject != null) {
                // the parent's current state is the child's snapshot, and it is as new as the parent object. Child
                // objects take their versions from the parent objects, so the snapshot merges into them
                return ObjectSnapshots.snapshot(parent, parentObject, parentObject.getSnapshotVersion());
            }
        }

        return fetch ? parent.getObjectStore().getSnapshot(oid) : parent.getObjectStore().getCachedSnapshot(oid);
    }

    /**
     * Returns an iterator over the registered objects.
     */
    public Iterator<Persistent> getObjectIterator() {
        return objectMap.values().iterator();
    }

    /**
     * Returns <code>true</code> if there are any modified, deleted or new objects
     * registered with this ObjectStore, <code>false</code> otherwise. This method will
     * treat "phantom" modifications are real ones. I.e. if you "change" an object
     * property to an equivalent value, this method will still think such object is
     * modified. Phantom modifications are only detected and discarded during commit.
     */
    @Override
    public boolean hasChanges() {
        return !changes.isEmpty();
    }

    /**
     * Return a subset of registered objects that are in a certain persistence state.
     * Collection is returned by copy.
     */
    @Override
    public List<Persistent> objectsInState(int state) {
        List<Persistent> filteredObjects = new ArrayList<>();

        for (Persistent object : objectMap.values()) {
            if (object.getPersistenceState() == state) {
                filteredObjects.add(object);
            }
        }

        return filteredObjects;
    }

    /**
     * Processes a DataRowStore snapshot change event, updating Persistent objects that have the changes.
     * <p>
     * <i>Implementation note: </i> This method should not attempt to alter the underlying
     * DataRowStore, since it is normally invoked *AFTER* the DataRowStore was modified as
     * a result of some external interaction.
     * </p>
     */
    private void snapshotsChanged(SnapshotEvent event) {
        // filter events that we should not process
        if (event.getPostedBy() != this && event.getSource() == this.getDataRowCache()) {
            processSnapshotEvent(event);
        }
    }

    void processSnapshotEvent(SnapshotEvent event) {

        Map<ObjectId, Map<String, Object>> modifiedDiffs = event.getModifiedDiffs();
        if (modifiedDiffs != null && !modifiedDiffs.isEmpty()) {
            // the merge plans of the refreshed objects, built once per descriptor per event
            Map<ClassDescriptor, MergePlan> plans = new HashMap<>();
            for (Map.Entry<ObjectId, Map<String, Object>> entry : modifiedDiffs.entrySet()) {
                processUpdatedSnapshot(entry.getKey(), entry.getValue(), event.getVersion(), plans);
            }
        }

        Collection<ObjectId> deletedIDs = event.getDeletedIds();
        if (deletedIDs != null && !deletedIDs.isEmpty()) {
            for (ObjectId deletedID : deletedIDs) {
                processDeletedID(deletedID);
            }
        }

        processInvalidatedIDs(event.getInvalidatedIds());
        processIndirectlyModifiedIDs(event.getIndirectlyModifiedIds());

        // TODO: andrus, 3/28/2006 - 'SnapshotEventDecorator' serves as a bridge (or
        // rather a noop wrapper) between old snapshot events and new GraphEvents. Once
        // SnapshotEvents are replaced with GraphEvents (in 2.0) we won't need it
        GraphDiff diff = new SnapshotEventDecorator(event);

        ObjectContext originatingContext = (event.getPostedBy() instanceof ObjectContext objectContext)
                ? objectContext
                : null;
        context.fireDataChannelChanged(originatingContext, diff);
    }

    void processIdChange(ObjectId id, ObjectId newId) {
        withChangesLock(() -> {
            Persistent object = objectMap.remove(id);

            if (object != null) {
                withObjectLock(id, () -> {
                    object.setObjectId(newId);
                    return null;
                });
                objectMap.put(newId, object);

                ObjectDiff change = changes.remove(id);
                if (change != null) {
                    changes.put(newId, change);
                }
            }

            Map<CayennePath, ObjectId> paths = trackedFlattenedPaths.remove(id);
            if (paths != null) {
                trackedFlattenedPaths.put(newId, paths);
            }
            return null;
        });
    }

    void processDeletedID(ObjectId id) {

        Persistent object = objectMap.get(id);
        if (object == null) {
            return;
        }

        DataContextDelegate delegate = context.nonNullDelegate();

        switch (object.getPersistenceState()) {
            case PersistenceState.COMMITTED, PersistenceState.HOLLOW, PersistenceState.DELETED -> {
                if (delegate.shouldProcessDelete(object)) {
                    objectMap.remove(id);
                    changes.remove(id);
                    trackedFlattenedPaths.remove(id);

                    withObjectLock(id, () -> {
                        // setting DataContext to null will also set state to transient
                        object.setObjectContext(null);
                        return null;
                    });
                    delegate.finishedProcessDelete(object);
                }
            }
            case PersistenceState.MODIFIED -> {
                if (delegate.shouldProcessDelete(object)) {
                    // the object is re-inserted on commit, so it becomes NEW. Recording that takes the changes lock,
                    // so it is taken before the object lock
                    withChangesLock(() -> {
                        withObjectLock(id, () -> {
                            object.setPersistenceState(PersistenceState.NEW);
                            return null;
                        });
                        changes.remove(id);
                        registerObject(id, object);
                        changeRecorder.nodeCreated(id);
                        return null;
                    });
                    delegate.finishedProcessDelete(object);
                }
            }
        }
    }

    void processInvalidatedIDs(Collection<ObjectId> invalidatedIDs) {
        if (invalidatedIDs != null && !invalidatedIDs.isEmpty()) {
            for (ObjectId oid : invalidatedIDs) {
                Persistent object = getObject(oid);

                if (object == null) {
                    continue;
                }

                switch (object.getPersistenceState()) {
                    case PersistenceState.COMMITTED -> {
                        withObjectLock(oid, () -> {
                            if (object.getPersistenceState() == PersistenceState.COMMITTED) {
                                object.setPersistenceState(PersistenceState.HOLLOW);
                            }
                            return null;
                        });
                    }
                    case PersistenceState.MODIFIED -> {
                        // the snapshot was just invalidated, so this is a fetch, done before any lock is taken
                        DataRow snapshot = getSnapshot(oid);
                        DataContextDelegate delegate = context.nonNullDelegate();
                        if (snapshot != null && delegate.shouldMergeChanges(object, snapshot)) {
                            ClassDescriptor descriptor = context
                                    .getEntityResolver()
                                    .getClassDescriptor(oid.getEntityName());
                            mergeDirty(object, descriptor, snapshot, snapshot.getVersion());
                            delegate.finishedMergeChanges(object);
                        }
                    }
                    case PersistenceState.HOLLOW, PersistenceState.DELETED -> { /* do nothing */ }
                }
            }
        }
    }

    /**
     * @since 1.1
     */
    void processIndirectlyModifiedIDs(Collection<ObjectId> indirectlyModifiedIDs) {
        for (ObjectId oid : indirectlyModifiedIDs) {
            final Persistent object = objectMap.get(oid);

            if (object == null || object.getPersistenceState() != PersistenceState.COMMITTED) {
                continue;
            }

            // for now break all "independent" object relationships...
            // in the future we may want to be more precise and go after modified
            // relationships only, or even process updated lists without invalidating...

            DataContextDelegate delegate = context.nonNullDelegate();

            if (delegate.shouldMergeChanges(object, null)) {

                ClassDescriptor descriptor = context
                        .getEntityResolver()
                        .getClassDescriptor(oid.getEntityName());

                withObjectLock(oid, () -> {
                    for (PropertyDescriptor property : descriptor.getProperties()) {
                        switch (property) {
                            case ToManyProperty toMany -> toMany.invalidate(object);
                            case ToOneProperty toOne
                                    when toOne.getRelationship().isSourceIndependentFromTargetChange() ->
                                    toOne.invalidate(object);
                            default -> {
                            }
                        }
                    }
                    return null;
                });

                delegate.finishedProcessDelete(object);
            }
        }
    }

    /**
     * @param diff    the changed keys of the snapshot of the id, with their new values
     * @param version the version of the snapshot the diff is of
     * @param plans   the merge plans built so far for this event, by descriptor. A COMMITTED object is refreshed with
     *                the plan of its descriptor, which is built on the first object of that descriptor
     * @since 1.1
     */
    void processUpdatedSnapshot(ObjectId id, Map<String, Object> diff, long version,
                                Map<ClassDescriptor, MergePlan> plans) {

        Persistent object = objectMap.get(id);

        // no object, or HOLLOW object require no processing
        if (object == null) {
            return;
        }

        switch (object.getPersistenceState()) {
            case PersistenceState.COMMITTED -> {
                // consult delegate if it exists
                DataContextDelegate delegate = context.nonNullDelegate();
                if (delegate.shouldMergeChanges(object, diff)) {
                    ClassDescriptor descriptor = context
                            .getEntityResolver()
                            .getClassDescriptor(id.getEntityName());

                    // TODO: andrus, 5/26/2006 - call to 'getSnapshot' is expensive,
                    // however my attempts to merge the 'diff' instead of snapshot
                    // via 'refreshObjectWithSnapshot' resulted in even worse performance.
                    // This sounds counterintuitive (Not sure if this is some HotSpot related glitch)...
                    // still keeping the old algorithm here until we switch from snapshot events
                    // to GraphEvents and all this code becomes obsolete.

                    // the snapshot may need a fetch, so it is taken before the object lock
                    DataRow snapshot = getSnapshot(id);
                    if (snapshot != null) {
                        MergePlan plan = plans.computeIfAbsent(descriptor, MergePlan::of);
                        withObjectLock(id, () -> {
                            // the object may have been modified since the state check above. A MODIFIED object merges
                            // the cached snapshot when its first change is recorded, so there's nothing to do for it
                            if (object.getPersistenceState() == PersistenceState.COMMITTED
                                    && snapshot.getVersion() >= object.getSnapshotVersion()) {
                                DataRowUtils.refreshObjectWithSnapshot(plan, object, snapshot, true);
                                object.setSnapshotVersion(snapshot.getVersion());
                            }
                            return null;
                        });
                    }
                    delegate.finishedMergeChanges(object);
                }
            }
            // merge modified and deleted
            case PersistenceState.DELETED, PersistenceState.MODIFIED -> {
                // consult delegate if it exists
                DataContextDelegate delegate = context.nonNullDelegate();
                if (delegate.shouldMergeChanges(object, diff)) {
                    ClassDescriptor descriptor = context
                            .getEntityResolver()
                            .getClassDescriptor(id.getEntityName());
                    mergeDirty(object, descriptor, diff, version);
                    delegate.finishedMergeChanges(object);
                }
            }
        }
    }

    /**
     * Merges a snapshot into a MODIFIED or DELETED object, leaving the uncommitted changes in place. Such a merge may
     * record changes to the object, so it takes the changes lock before the object lock. The snapshot is only merged
     * if it is not older than the one the object already reflects.
     */
    private void mergeDirty(Persistent object, ClassDescriptor descriptor, Map<String, Object> snapshot, long version) {
        withChangesLock(() -> withObjectLock(object.getObjectId(), () -> {
            int state = object.getPersistenceState();
            if ((state == PersistenceState.MODIFIED || state == PersistenceState.DELETED)
                    && version >= object.getSnapshotVersion()) {
                DataRowUtils.forceMergeWithSnapshot(context, descriptor, object, snapshot);
                object.setSnapshotVersion(version);
            }
            return null;
        }));
    }

    @Override
    public Persistent getObject(ObjectId id) {
        return objectMap.get(id);
    }

    @Override
    public Collection<Persistent> registeredObjects() {
        return new ArrayList<>(objectMap.values());
    }

    @Override
    public void registerObject(ObjectId id, Persistent nodeObject) {
        objectMap.put(id, nodeObject);
    }

    /**
     * Returns the object registered under the id, or, if there is none, creates a HOLLOW object of the id's entity,
     * registers and returns it. Registration is atomic, so concurrent callers get the same object for an id
     * (CAY-845), and the object is fully initialized before it becomes visible to other threads.
     */
    Persistent getOrCreateObject(ObjectId id) {
        return objectMap.computeIfAbsent(id, this::createHollowObject);
    }

    private Persistent createHollowObject(ObjectId id) {
        ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(id.getEntityName());
        Persistent object = (Persistent) descriptor.createObject();

        object.setObjectContext(context);
        object.setObjectId(id);
        object.setPersistenceState(PersistenceState.HOLLOW);
        return object;
    }

    @Override
    public Persistent unregisterObject(ObjectId id) {
        Persistent object = objectMap.get(id);
        if (object != null) {
            objectsUnregistered(Collections.singleton(object));
        }

        return object;
    }

    GraphChangeHandler changeRecorder() {
        return changeRecorder;
    }

    public ObjectId getFlattenedId(ObjectId objectId, CayennePath path) {
        return trackedFlattenedPaths
                .getOrDefault(objectId, Collections.emptyMap()).get(path);
    }

    public Collection<ObjectId> getFlattenedIds(ObjectId objectId) {
        return trackedFlattenedPaths
                .getOrDefault(objectId, Collections.emptyMap()).values();
    }

    public Map<CayennePath, ObjectId> getFlattenedPathIdMap(ObjectId objectId) {
        return trackedFlattenedPaths
                .getOrDefault(objectId, Collections.emptyMap());
    }

    /**
     * Mark that flattened path for object has data row in DB.
     *
     * @since 4.1
     */
    public void markFlattenedPath(ObjectId objectId, CayennePath path, ObjectId id) {
        trackedFlattenedPaths
                .computeIfAbsent(objectId, o -> new ConcurrentHashMap<>())
                .put(path, id);
    }

    /**
     * @param key object id that was removed from the {@link #objectMap}
     * @since 4.2.2
     */
    void onObjectKeyCleanup(Object key) {
        trackedFlattenedPaths.remove(key);
    }

    static class SnapshotEventDecorator implements GraphDiff {

        SnapshotEvent event;

        SnapshotEventDecorator(SnapshotEvent event) {
            this.event = event;
        }

        SnapshotEvent getEvent() {
            return event;
        }

        @Override
        public void apply(GraphChangeHandler handler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isNoop() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void undo(GraphChangeHandler handler) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Records object graph changes as uncommitted diffs.
     */
    private class ChangeRecorder implements GraphChangeHandler {

        @Override
        public void nodeCreated(ObjectId id) {
            NodeDiff diff = new NodeCreateOperation(id);

            if (lifecycleEventInducedChanges != null) {
                registerLifecycleEventInducedChange(diff);
            }

            registerDiff(id, diff);
        }

        @Override
        public void nodeRemoved(ObjectId id) {
            NodeDiff diff = new NodeDeleteOperation(id);

            if (lifecycleEventInducedChanges != null) {
                registerLifecycleEventInducedChange(diff);
            }

            registerDiff(id, diff);
        }

        /**
         * Records dirty object snapshot.
         */
        @Override
        public void nodePropertyChanged(ObjectId id, String property, Object oldValue, Object newValue) {
            if (lifecycleEventInducedChanges != null) {
                registerLifecycleEventInducedChange(
                        new NodePropertyChangeOperation(id, property, oldValue, newValue));
            }

            registerDiff(id, null);
        }

        @Override
        public void arcCreated(ObjectId id, ObjectId targetId, ArcId arcId) {
            NodeDiff diff = new ArcOperation(id, targetId, arcId, false);

            if (lifecycleEventInducedChanges != null) {
                registerLifecycleEventInducedChange(diff);
            }

            registerDiff(id, diff);
        }

        @Override
        public void arcDeleted(ObjectId id, ObjectId targetId, ArcId arcId) {
            NodeDiff diff = new ArcOperation(id, targetId, arcId, true);

            if (lifecycleEventInducedChanges != null) {
                registerLifecycleEventInducedChange(diff);
            }

            registerDiff(id, diff);
        }
    }
}
