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
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.ObjectDiff.ArcOperation;
import org.apache.cayenne.access.event.SnapshotEvent;
import org.apache.cayenne.access.event.SnapshotEventListener;
import org.apache.cayenne.graph.ArcId;
import org.apache.cayenne.graph.ChildDiffLoader;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.graph.NodeCreateOperation;
import org.apache.cayenne.graph.NodeDeleteOperation;
import org.apache.cayenne.graph.NodeDiff;
import org.apache.cayenne.graph.NodePropertyChangeOperation;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ObjectStore stores objects using their ObjectId as a key. It works as a dedicated
 * object cache for a DataContext. Users rarely need to access ObjectStore directly, as
 * DataContext serves as a facade, providing cover methods for most ObjectStore
 * operations.
 * 
 * @since 1.0
 */
public class ObjectStore implements Serializable, SnapshotEventListener, GraphManager {

    /**
     * Actual content is ObjectId -> PersistentWrapper
     */
    protected Map<Object, Persistent> objectMap;
    protected Map<Object, ObjectDiff> changes;

    /**
     * Map that tracks flattened paths for given object Id that is present in db.
     * Presence of path in this map is used to separate insert from update case of flattened records.
     * @since 4.1
     * @deprecated since 4.2.2 it is unused
     */
    protected Map<Object, Map<String, ObjectId>> trackedFlattenedPaths;

    // a sequential id used to tag GraphDiffs so that they can later be sorted in the
    // original creation order
    int currentDiffId;

    /**
     * Stores a reference to the DataRowStore.
     * <p>
     * <i>Serialization note: </i> It is up to the owner of this ObjectStore to initialize
     * DataRowStore after deserialization of this object. ObjectStore will not know how to
     * restore the DataRowStore by itself.
     * </p>
     */
    protected transient DataRowStore dataRowCache;

    // used to avoid incorrect on-demand DataRowStore initialization after deserialization
    protected boolean dataRowCacheSet;

    private Collection<GraphDiff> lifecycleEventInducedChanges;

    /**
     * The DataContext that owns this ObjectStore.
     */
    protected DataContext context;

    /**
     * Creates an ObjectStore with {@link DataRowStore} and a map to use for storing
     * registered objects. Passed map doesn't require any special synchronization
     * behavior, as ObjectStore is synchronized itself.
     * 
     * @since 3.0
     */
    public ObjectStore(DataRowStore dataRowCache, Map<Object, Persistent> objectMap) {
        setDataRowCache(dataRowCache);
        if (objectMap != null) {
            this.objectMap = objectMap;
        }
        else {
            throw new CayenneRuntimeException("Object map is null.");
        }
        this.changes = new HashMap<>();
    }

    /**
     * @since 3.0
     */
    void childContextSyncStarted() {
        lifecycleEventInducedChanges = new ArrayList<>();
    }

    /**
     * @since 3.0
     */
    void childContextSyncStopped() {
        lifecycleEventInducedChanges = null;
    }

    /**
     * @since 3.0
     */
    Collection<GraphDiff> getLifecycleEventInducedChanges() {
        return lifecycleEventInducedChanges != null
                ? lifecycleEventInducedChanges
                : Collections.emptyList();
    }

    void registerLifecycleEventInducedChange(GraphDiff diff) {
        if (ChildDiffLoader.isProcessingChildDiff()) {
            // reset so that subsequent event-induced changes could get registered...
            ChildDiffLoader.setExternalChange(Boolean.FALSE);
        }
        else {
            lifecycleEventInducedChanges.add(diff);
        }
    }

    /**
     * Registers object change.
     * 
     * @since 1.2
     */
    synchronized ObjectDiff registerDiff(Object nodeId, NodeDiff diff) {

        if (diff != null) {
            diff.setDiffId(++currentDiffId);
        }

        ObjectDiff objectDiff = changes.get(nodeId);

        if (objectDiff == null) {

            ObjectStorePersistentWrapper persistentWrapper = (ObjectStorePersistentWrapper)objectMap.get(nodeId);
            if (persistentWrapper == null) {
                throw new CayenneRuntimeException("No object is registered in context with Id %s", nodeId);
            }
            Persistent object = persistentWrapper.dataObject();

            if (object.getPersistenceState() == PersistenceState.COMMITTED) {
                object.setPersistenceState(PersistenceState.MODIFIED);

                // TODO: andrus 3/23/2006 snapshot versions are obsolete, but there is no
                // replacement yet, so we still need to handle them...
                if (object instanceof DataObject) {

                    DataObject dataObject = (DataObject) object;
                    DataRow snapshot = getCachedSnapshot((ObjectId) nodeId);

                    if (snapshot != null
                            && snapshot.getVersion() != dataObject.getSnapshotVersion()) {
                        DataContextDelegate delegate = context.nonNullDelegate();
                        if (delegate.shouldMergeChanges(dataObject, snapshot)) {
                            ClassDescriptor descriptor = context
                                    .getEntityResolver()
                                    .getClassDescriptor(
                                            ((ObjectId) nodeId).getEntityName());
                            DataRowUtils.forceMergeWithSnapshot(
                                    context,
                                    descriptor,
                                    dataObject,
                                    snapshot);
                            dataObject.setSnapshotVersion(snapshot.getVersion());
                            delegate.finishedMergeChanges(dataObject);
                        }
                    }
                }
            }

            objectDiff = new ObjectDiff(persistentWrapper);
            objectDiff.setDiffId(++currentDiffId);
            changes.put(nodeId, objectDiff);
        }

        if (diff != null) {
            objectDiff.addDiff(diff, this);
        }

        return objectDiff;
    }

    private Persistent getUnwrapped(Object nodeId) {
        Persistent persistent = objectMap.get(nodeId);
        if(persistent == null) {
            return null;
        }
        return ((ObjectStorePersistentWrapper) persistent).dataObject();
    }

    /**
     * Returns a number of objects currently registered with this ObjectStore.
     * 
     * @since 1.2
     */
    public int registeredObjectsCount() {
        AtomicInteger counter = new AtomicInteger();
        objectMap.forEach((id, obj) -> {
            ObjectStorePersistentWrapper wrapper = (ObjectStorePersistentWrapper) obj;
            if(wrapper.hasObject()){
                counter.incrementAndGet();
            }
        });
        return counter.get();
    }

    /**
     * Returns a DataRowStore associated with this ObjectStore.
     */
    public DataRowStore getDataRowCache() {

        // perform deferred initialization...

        // Andrus, 11/7/2005 - potential problem with on-demand deferred initialization is
        // that deserialized context won't receive any events... which maybe ok, since it
        // didn't while it was stored in serialized form.
        if (dataRowCache == null && context != null && dataRowCacheSet) {
            synchronized (this) {
                if (dataRowCache == null) {
                    DataDomain domain = context.getParentDataDomain();
                    if (domain != null) {
                        setDataRowCache(domain.getSharedSnapshotCache());
                    }
                }
            }
        }

        return dataRowCache;
    }

    /**
     * Sets parent DataRowStore. Registers to receive SnapshotEvents if the cache is
     * configured to allow ObjectStores to receive such events.
     */
    // note that as of 1.2, ObjectStore does not access DataRowStore directly when
    // retrieving snapshots. Instead it sends a query via the DataContext's channel so
    // that every element in the channel chain could intercept snapshot requests
    public void setDataRowCache(DataRowStore dataRowCache) {
        if (dataRowCache == this.dataRowCache) {
            return;
        }

        if (this.dataRowCache != null && this.dataRowCache.getEventManager() != null) {
            this.dataRowCache.getEventManager().removeListener(
                    this,
                    this.dataRowCache.getSnapshotEventSubject());
        }

        this.dataRowCache = dataRowCache;

        if (dataRowCache != null && dataRowCache.getEventManager() != null) {
            // setting itself as non-blocking listener,
            // since event sending thread will likely be locking sender's
            // ObjectStore and snapshot cache itself.
            dataRowCache.getEventManager().addNonBlockingListener(
                    this,
                    "snapshotsChanged",
                    SnapshotEvent.class,
                    dataRowCache.getSnapshotEventSubject(),
                    dataRowCache);
        }

        dataRowCacheSet = dataRowCache != null;
    }

    /**
     * Evicts a collection of DataObjects from the ObjectStore, invalidates the underlying
     * cache snapshots. Changes objects state to TRANSIENT. This method can be used for
     * manual cleanup of Cayenne cache.
     */
    // this method is exactly the same as "objectsInvalidated", only additionally it
    // throws out registered objects
    public synchronized void objectsUnregistered(Collection objects) {
        if (objects.isEmpty()) {
            return;
        }

        Collection<ObjectId> ids = new ArrayList<>(objects.size());

        for (Object object1 : objects) {
            Persistent object = (Persistent) object1;

            ObjectId id = object.getObjectId();

            // remove object but not snapshot
            objectMap.remove(id);
            changes.remove(id);
            ids.add(id);

            object.setObjectContext(null);
            object.setPersistenceState(PersistenceState.TRANSIENT);
        }

        // TODO, andrus 3/28/2006 - DRC is null in nested contexts... implement
        // propagation of unregister operation through the stack ... or do the opposite
        // and keep unregister local even for non-nested DC?
        if (getDataRowCache() != null) {
            // send an event for removed snapshots
            getDataRowCache().processSnapshotChanges(
                    this,
                    Collections.<ObjectId, DataRow>emptyMap(),
                    Collections.<ObjectId>emptyList(),
                    ids,
                    Collections.<ObjectId>emptyList());
        }
    }

    /**
     * Reverts changes to all stored uncomitted objects.
     * 
     * @since 1.1
     */
    public synchronized void objectsRolledBack() {
        Iterator<Persistent> it = getObjectIterator();

        // collect candidates
        while (it.hasNext()) {
            Persistent object = it.next();
            int objectState = object.getPersistenceState();
            switch (objectState) {
                case PersistenceState.NEW:
                    it.remove();

                    object.setObjectContext(null);
                    object.setObjectId(null);
                    object.setPersistenceState(PersistenceState.TRANSIENT);
                    break;
                case PersistenceState.DELETED:
                    // Do the same as for modified... deleted is only a persistence state,
                    // so
                    // rolling the object back will set the state to committed
                case PersistenceState.MODIFIED:
                    // this will clean any modifications and defer refresh from snapshot
                    // till the next object accessor is called
                    object.setPersistenceState(PersistenceState.HOLLOW);
                    break;
                default:
                    // Transient, committed and hollow need no handling
                    break;
            }
        }

        // reset changes ... using new HashMap to allow event listeners to analyze the
        // original changes map after the rollback
        this.changes = new HashMap<>();
    }

    /**
     * Builds and returns GraphDiff reflecting all uncommitted object changes.
     * 
     * @since 1.2
     */
    ObjectStoreGraphDiff getChanges() {
        return new ObjectStoreGraphDiff(this);
    }

    /**
     * Returns internal changes map.
     * 
     * @since 1.2
     */
    Map<Object, ObjectDiff> getChangesByObjectId() {
        return changes;
    }

    /**
     * @since 1.2
     */
    void postprocessAfterPhantomCommit() {

        for (Object id : changes.keySet()) {

            Persistent object = getUnwrapped(id);

            // assume that no new or deleted objects are present (as otherwise commit
            // wouldn't have been phantom).
            object.setPersistenceState(PersistenceState.COMMITTED);
        }

        // clear caches
        this.changes.clear();
    }

    /**
     * Internal unsynchronized method to process objects state after commit.
     * 
     * @since 1.2
     */
    public void postprocessAfterCommit(GraphDiff parentChanges) {

        // scan through changed objects, set persistence state to committed
        for (Object id : changes.keySet()) {
            Persistent object = getUnwrapped(id);

            switch (object.getPersistenceState()) {
                case PersistenceState.DELETED:
                    objectMap.remove(id);
                    object.setObjectContext(null);
                    object.setPersistenceState(PersistenceState.TRANSIENT);
                    break;
                case PersistenceState.NEW:
                case PersistenceState.MODIFIED:
                    object.setPersistenceState(PersistenceState.COMMITTED);
                    break;
            }
        }

        // re-register changed object ids
        if (!parentChanges.isNoop()) {
            parentChanges.apply(new GraphChangeHandler() {

                @Override
                public void arcCreated(Object nodeId, Object targetNodeId, ArcId arcId) {
                }

                @Override
                public void arcDeleted(Object nodeId, Object targetNodeId, ArcId arcId) {
                }

                @Override
                public void nodeCreated(Object nodeId) {
                }

                @Override
                public void nodeIdChanged(Object nodeId, Object newId) {
                    processIdChange(nodeId, newId);
                }

                @Override
                public void nodePropertyChanged(
                        Object nodeId,
                        String property,
                        Object oldValue,
                        Object newValue) {
                }

                @Override
                public void nodeRemoved(Object nodeId) {
                }
            });
        }

        // create new instance of changes map so that event listeners who stored the
        // original diff don't get affected
        this.changes = new HashMap<>();
    }

    /**
     * Returns a snapshot for ObjectId from the underlying snapshot cache. If cache
     * contains no snapshot, a null is returned.
     * 
     * @since 1.1
     */
    public DataRow getCachedSnapshot(ObjectId oid) {

        if (context != null && context.getChannel() != null) {
            ObjectIdQuery query = new CachedSnapshotQuery(oid);
            List<?> results = context.getChannel().onQuery(context, query).firstList();
            return results.isEmpty() ? null : (DataRow) results.get(0);
        }
        else {
            return null;
        }
    }

    /**
     * Returns a snapshot for ObjectId from the underlying snapshot cache. If cache
     * contains no snapshot, it will attempt fetching it using provided QueryEngine. If
     * fetch attempt fails or inconsistent data is returned, underlying cache will throw a
     * CayenneRuntimeException.
     * 
     * @since 1.2
     */
    public synchronized DataRow getSnapshot(ObjectId oid) {

        if (context != null && context.getChannel() != null) {
            ObjectIdQuery query = new ObjectIdQuery(oid, true, ObjectIdQuery.CACHE);
            List<?> results = context.getChannel().onQuery(context, query).firstList();
            return results.isEmpty() ? null : (DataRow) results.get(0);
        }
        else {
            return null;
        }
    }

    /**
     * Returns an iterator over the registered objects.
     */
    public synchronized Iterator<Persistent> getObjectIterator() {
        return new WrapperIterator(objectMap.values().iterator());
    }

    /**
     * Returns <code>true</code> if there are any modified, deleted or new objects
     * registered with this ObjectStore, <code>false</code> otherwise. This method will
     * treat "phantom" modifications are real ones. I.e. if you "change" an object
     * property to an equivalent value, this method will still think such object is
     * modified. Phantom modifications are only detected and discarded during commit.
     */
    public synchronized boolean hasChanges() {
        return !changes.isEmpty();
    }

    /**
     * Return a subset of registered objects that are in a certain persistence state.
     * Collection is returned by copy.
     */
    public synchronized List<Persistent> objectsInState(int state) {
        List<Persistent> filteredObjects = new ArrayList<>();

        for (Persistent object : objectMap.values()) {
            ObjectStorePersistentWrapper wrapper = (ObjectStorePersistentWrapper) object;
            if (wrapper.hasObject() && object.getPersistenceState() == state) {
                filteredObjects.add(wrapper.dataObject());
            }
        }

        return filteredObjects;
    }

    /**
     * SnapshotEventListener implementation that processes snapshot change event, updating
     * DataObjects that have the changes.
     * <p>
     * <i>Implementation note: </i> This method should not attempt to alter the underlying
     * DataRowStore, since it is normally invoked *AFTER* the DataRowStore was modified as
     * a result of some external interaction.
     * </p>
     * 
     * @since 1.1
     */
    @Override
    public void snapshotsChanged(SnapshotEvent event) {
        // filter events that we should not process
        if (event.getPostedBy() != this && event.getSource() == this.getDataRowCache()) {
            processSnapshotEvent(event);
        }
    }

    /**
     * @since 1.2
     */
    synchronized void processSnapshotEvent(SnapshotEvent event) {

        Map<ObjectId, DataRow> modifiedDiffs = event.getModifiedDiffs();
        if (modifiedDiffs != null && !modifiedDiffs.isEmpty()) {
            for (Map.Entry<ObjectId, DataRow> entry : modifiedDiffs.entrySet()) {
                processUpdatedSnapshot(entry.getKey(), entry.getValue());
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

        ObjectContext originatingContext = (event.getPostedBy() instanceof ObjectContext)
                ? (ObjectContext) event.getPostedBy()
                : null;
        context.fireDataChannelChanged(originatingContext, diff);
    }

    void processIdChange(Object nodeId, Object newId) {
        Persistent object = objectMap.remove(nodeId);

        if (object != null) {
            object.setObjectId((ObjectId) newId);
            objectMap.merge(newId, object, (oldValue, newValue) -> {
                ObjectStorePersistentWrapper oldWrapper = (ObjectStorePersistentWrapper) oldValue;
                ObjectStorePersistentWrapper newWrapper = (ObjectStorePersistentWrapper) newValue;
                if(oldWrapper.trackedFlattenedPaths != null) {
                    if(newWrapper.trackedFlattenedPaths != null) {
                        newWrapper.trackedFlattenedPaths.putAll(oldWrapper.trackedFlattenedPaths);
                    } else {
                        newWrapper.trackedFlattenedPaths = oldWrapper.trackedFlattenedPaths;
                    }
                }
                return newWrapper;
            });

            ObjectDiff change = changes.remove(nodeId);
            if (change != null) {
                changes.put(newId, change);
            }
        }
    }

    /**
     * Requires external synchronization.
     * 
     * @since 1.2
     */
    void processDeletedID(ObjectId nodeId) {

        // access object map directly - the method should be called in a synchronized context...
        Persistent object = getUnwrapped(nodeId);

        if (object != null) {

            DataObject dataObject = (object instanceof DataObject)
                    ? (DataObject) object
                    : null;

            DataContextDelegate delegate;

            switch (object.getPersistenceState()) {
                case PersistenceState.COMMITTED:
                case PersistenceState.HOLLOW:
                case PersistenceState.DELETED:

                    // consult delegate
                    delegate = context.nonNullDelegate();

                    if (dataObject == null || delegate.shouldProcessDelete(dataObject)) {
                        objectMap.remove(nodeId);
                        changes.remove(nodeId);

                        // setting DataContext to null will also set
                        // state to transient
                        object.setObjectContext(null);

                        if (dataObject != null) {
                            delegate.finishedProcessDelete(dataObject);
                        }
                    }

                    break;

                case PersistenceState.MODIFIED:

                    // consult delegate
                    delegate = context.nonNullDelegate();
                    if (dataObject != null && delegate.shouldProcessDelete(dataObject)) {
                        object.setPersistenceState(PersistenceState.NEW);
                        changes.remove(nodeId);
                        registerNode(nodeId, object);
                        nodeCreated(nodeId);

                        delegate.finishedProcessDelete(dataObject);
                    }

                    break;
            }
        }
    }

    /**
     * @since 1.1
     */
    void processInvalidatedIDs(Collection<ObjectId> invalidatedIDs) {
        if (invalidatedIDs != null && !invalidatedIDs.isEmpty()) {
            for (ObjectId oid : invalidatedIDs) {
                DataObject object = (DataObject) getNode(oid);

                if (object == null) {
                    continue;
                }

                // TODO: refactor "switch" to avoid code duplication

                switch (object.getPersistenceState()) {
                    case PersistenceState.COMMITTED:
                        object.setPersistenceState(PersistenceState.HOLLOW);
                        break;
                    case PersistenceState.MODIFIED:
                        DataContext context = (DataContext) object.getObjectContext();
                        DataRow diff = getSnapshot(oid);
                        // consult delegate if it exists
                        DataContextDelegate delegate = context.nonNullDelegate();
                        if (delegate.shouldMergeChanges(object, diff)) {
                            ClassDescriptor descriptor = context
                                    .getEntityResolver()
                                    .getClassDescriptor(oid.getEntityName());
                            DataRowUtils.forceMergeWithSnapshot(
                                    context,
                                    descriptor,
                                    object,
                                    diff);
                            delegate.finishedMergeChanges(object);
                        }

                    case PersistenceState.HOLLOW:
                        // do nothing
                        break;

                    case PersistenceState.DELETED:
                        // TODO: Do nothing? Or treat as merged?
                        break;
                }
            }
        }
    }

    /**
     * Requires external synchronization.
     * 
     * @since 1.1
     */
    void processIndirectlyModifiedIDs(Collection<ObjectId> indirectlyModifiedIDs) {
        for (ObjectId oid : indirectlyModifiedIDs) {
            // access object map directly - the method should be called in a synchronized context...
            final DataObject object = (DataObject) getUnwrapped(oid);

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
                descriptor.visitProperties(new PropertyVisitor() {

                    @Override
                    public boolean visitToMany(ToManyProperty property) {
                        property.invalidate(object);
                        return true;
                    }

                    @Override
                    public boolean visitToOne(ToOneProperty property) {
                        if (property
                                .getRelationship()
                                .isSourceIndependentFromTargetChange()) {
                            property.invalidate(object);
                        }
                        return true;
                    }

                    @Override
                    public boolean visitAttribute(AttributeProperty property) {
                        return true;
                    }
                });

                delegate.finishedProcessDelete(object);
            }
        }
    }

    /**
     * Requires external synchronization.
     * 
     * @since 1.1
     */
    void processUpdatedSnapshot(ObjectId nodeId, DataRow diff) {

        // access object map directly - the method should be called in a synchronized context...
        DataObject object = (DataObject) getUnwrapped(nodeId);

        // no object, or HOLLOW object require no processing
        if (object != null) {

            int state = object.getPersistenceState();
            if (state != PersistenceState.HOLLOW) {

                // perform same steps as resolveHollow()
                if (state == PersistenceState.COMMITTED) {
                    // consult delegate if it exists
                    DataContextDelegate delegate = context.nonNullDelegate();
                    if (delegate.shouldMergeChanges(object, diff)) {
                        ClassDescriptor descriptor = context
                                .getEntityResolver()
                                .getClassDescriptor(nodeId.getEntityName());

                        // TODO: andrus, 5/26/2006 - call to 'getSnapshot' is expensive,
                        // however my attempts to merge the 'diff' instead of snapshot
                        // via 'refreshObjectWithSnapshot' resulted in even worse performance.
                        // This sounds counterintuitive (Not sure if this is some HotSpot related glitch)...
                        // still keeping the old algorithm here until we switch from snapshot events
                        // to GraphEvents and all this code becomes obsolete.
                        DataRow snapshot = getSnapshot(object.getObjectId());

                        DataRowUtils.refreshObjectWithSnapshot(
                                descriptor,
                                object,
                                snapshot,
                                true);
                        delegate.finishedMergeChanges(object);
                    }
                }
                // merge modified and deleted
                else if (state == PersistenceState.DELETED
                        || state == PersistenceState.MODIFIED) {

                    // consult delegate if it exists
                    DataContextDelegate delegate = context.nonNullDelegate();
                    if (delegate.shouldMergeChanges(object, diff)) {
                        ClassDescriptor descriptor = context
                                .getEntityResolver()
                                .getClassDescriptor(nodeId.getEntityName());
                        DataRowUtils.forceMergeWithSnapshot(
                                context,
                                descriptor,
                                object,
                                diff);
                        delegate.finishedMergeChanges(object);
                    }
                }
            }
        }
    }

    /**
     * @since 1.2
     */
    public DataContext getContext() {
        return context;
    }

    /**
     * @since 1.2
     */
    public void setContext(DataContext context) {
        this.context = context;
    }

    // *********** GraphManager Methods ********
    // =========================================

    /**
     * Returns a registered DataObject or null of no object exists for the ObjectId.
     * 
     * @since 1.2
     */
    @Override
    public synchronized Object getNode(Object nodeId) {
        return getUnwrapped(nodeId);
    }

    // non-synchronized version of getNode for private use
    final Object getNodeNoSync(Object nodeId) {
        return getUnwrapped(nodeId);
    }

    /**
     * Returns all registered DataObjects. List is returned by copy and can be modified by
     * the caller.
     * 
     * @since 1.2
     */
    @Override
    public synchronized Collection<Object> registeredNodes() {
        List<Object> values = new ArrayList<>(objectMap.size());
        objectMap.forEach((id, persistent)
                -> {
            ObjectStorePersistentWrapper wrapper = (ObjectStorePersistentWrapper) persistent;
            if(wrapper.hasObject()) {
                values.add(wrapper.dataObject());
            }
        });
        return values;
    }

    /**
     * @since 1.2
     */
    @Override
    public synchronized void registerNode(Object nodeId, Object nodeObject) {
        objectMap.put(nodeId, new ObjectStorePersistentWrapper((Persistent) nodeObject));
    }

    /**
     * @since 1.2
     */
    @Override
    public synchronized Object unregisterNode(Object nodeId) {
        Object object = getNode(nodeId);
        if (object != null) {
            objectsUnregistered(Collections.singleton(object));
        }

        return object;
    }

    /**
     * Does nothing.
     * 
     * @since 1.2
     */
    @Override
    public void nodeIdChanged(Object nodeId, Object newId) {
        throw new UnsupportedOperationException("nodeIdChanged");
    }

    /**
     * @since 1.2
     */
    @Override
    public void nodeCreated(Object nodeId) {
        NodeDiff diff = new NodeCreateOperation(nodeId);

        if (lifecycleEventInducedChanges != null) {
            registerLifecycleEventInducedChange(diff);
        }

        registerDiff(nodeId, diff);
    }

    /**
     * @since 1.2
     */
    @Override
    public void nodeRemoved(Object nodeId) {

        NodeDiff diff = new NodeDeleteOperation(nodeId);

        if (lifecycleEventInducedChanges != null) {
            registerLifecycleEventInducedChange(diff);
        }

        registerDiff(nodeId, diff);
    }

    /**
     * Records dirty object snapshot.
     * 
     * @since 1.2
     */
    @Override
    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {

        if (lifecycleEventInducedChanges != null) {
            registerLifecycleEventInducedChange(new NodePropertyChangeOperation(
                    nodeId,
                    property,
                    oldValue,
                    newValue));
        }

        registerDiff(nodeId, null);
    }

    /**
     * @since 1.2
     */
    @Override
    public void arcCreated(Object nodeId, Object targetNodeId, ArcId arcId) {
        NodeDiff diff = new ArcOperation(nodeId, targetNodeId, arcId, false);

        if (lifecycleEventInducedChanges != null) {
            registerLifecycleEventInducedChange(diff);
        }

        registerDiff(nodeId, diff);
    }

    /**
     * @since 1.2
     */
    @Override
    public void arcDeleted(Object nodeId, Object targetNodeId, ArcId arcId) {
        NodeDiff diff = new ArcOperation(nodeId, targetNodeId, arcId, true);

        if (lifecycleEventInducedChanges != null) {
            registerLifecycleEventInducedChange(diff);
        }

        registerDiff(nodeId, diff);
    }

    /**
     * Check that flattened path for given object ID has data row in DB.
     * @since 4.1
     */
    boolean hasFlattenedPath(ObjectId objectId, String path) {
        ObjectStorePersistentWrapper wrapper = (ObjectStorePersistentWrapper) objectMap.get(objectId);
        return wrapper.hasFlattenedPath(path);
    }

    /**
     * @since 4.2
     */
    public ObjectId getFlattenedId(ObjectId objectId, String path) {
        ObjectStorePersistentWrapper wrapper = (ObjectStorePersistentWrapper) objectMap.get(objectId);
        return wrapper.getFlattenedId(path);
    }

    /**
     * @since 4.2
     */
    public Collection<ObjectId> getFlattenedIds(ObjectId objectId) {
        ObjectStorePersistentWrapper wrapper = (ObjectStorePersistentWrapper) objectMap.get(objectId);
        return wrapper.getFlattenedIds();
    }

    /**
     * @since 4.2.1
     */
    public Map<String, ObjectId> getFlattenedPathIdMap(ObjectId objectId) {
        ObjectStorePersistentWrapper wrapper = (ObjectStorePersistentWrapper) objectMap.get(objectId);
        return wrapper.getFlattenedPathIdMap();
    }

    /**
     * Mark that flattened path for object has data row in DB.
     * @since 4.1
     */
    public void markFlattenedPath(ObjectId objectId, String path, ObjectId id) {
        ObjectStorePersistentWrapper wrapper = (ObjectStorePersistentWrapper) objectMap.computeIfAbsent(objectId, objId -> {
            System.out.println("Synthetic node for " + objId);
            return new ObjectStorePersistentWrapper(null);
        });
        wrapper.markFlattenedPath(path, id);
    }

    // an ObjectIdQuery optimized for retrieval of multiple snapshots - it can be reset
    // with the new id
    final class CachedSnapshotQuery extends ObjectIdQuery {

        CachedSnapshotQuery(ObjectId oid) {
            super(oid, true, ObjectIdQuery.CACHE_NOREFRESH);
        }

        void resetId(ObjectId oid) {
            this.objectId = oid;
            this.replacementQuery = null;
        }
    }

    class SnapshotEventDecorator implements GraphDiff {

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

    static class WrapperIterator implements Iterator<Persistent> {

        final Iterator<ObjectStorePersistentWrapper> iterator;

        @SuppressWarnings({"unchecked", "rawtypes"})
        WrapperIterator(Iterator<Persistent> iterator) {
            this.iterator = (Iterator)iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Persistent next() {
            return iterator.next().dataObject();
        }

        @Override
        public void remove() {
            iterator.remove();
        }
    }
}
