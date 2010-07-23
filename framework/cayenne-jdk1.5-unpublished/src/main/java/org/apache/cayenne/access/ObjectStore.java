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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.apache.cayenne.graph.ChildDiffLoader;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.graph.NodeCreateOperation;
import org.apache.cayenne.graph.NodeDeleteOperation;
import org.apache.cayenne.graph.NodeDiff;
import org.apache.cayenne.graph.NodePropertyChangeOperation;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;
import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceMap;

/**
 * ObjectStore stores objects using their ObjectId as a key. It works as a dedicated
 * object cache for a DataContext. Users rarely need to access ObjectStore directly, as
 * DataContext serves as a facade, providing cover methods for most ObjectStore
 * operations.
 * 
 * @since 1.0
 */
// Synchronization Note: There is often a need to do double synchronize on an ObjectStore
// and an underlying DataRowCache. To avoid deadlocks, Cayenne consistently follows the
// policy of locking an ObjectStore first, and then locking DataRowStore. This pattern
// must be followed in any new related developments.
public class ObjectStore implements Serializable, SnapshotEventListener, GraphManager {

    /**
     * Factory method to create default Map for storing registered objects.
     * 
     * @since 3.0
     * @return a map with hard referenced keys and weak referenced values.
     */
    static Map<Object, Persistent> createObjectMap() {
        return new ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK);
    }

    protected Map<Object, Persistent> objectMap;
    protected Map<Object, ObjectDiff> changes;

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
    private boolean dataRowCacheSet;

    private Collection<GraphDiff> lifecycleEventInducedChanges;

    /**
     * The DataContext that owns this ObjectStore.
     */
    protected DataContext context;

    public ObjectStore() {
        this(null);
    }

    public ObjectStore(DataRowStore dataRowCache) {
        this(dataRowCache, null);
    }

    /**
     * Creates an ObjectStore with {@link DataRowStore} and a map to use for storing
     * registered objects. Passed map doesn't require any special synchronization
     * behavior, as ObjectStore is synchronized itself.
     * 
     * @since 3.0
     */
    public ObjectStore(DataRowStore dataRowCache, Map<Object, Persistent> objectMap) {
        setDataRowCache(dataRowCache);
        this.objectMap = objectMap != null ? objectMap : ObjectStore.createObjectMap();
        this.changes = new HashMap<Object, ObjectDiff>();
    }

    /**
     * @since 3.0
     */
    void childContextSyncStarted() {
        lifecycleEventInducedChanges = new ArrayList<GraphDiff>();
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
                : Collections.EMPTY_LIST;
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

            Persistent object = objectMap.get(nodeId);
            
            if (object == null) {
                throw new CayenneRuntimeException("No object is registered in context with Id " + nodeId);
            }
            
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

            objectDiff = new ObjectDiff(object);
            objectDiff.setDiffId(++currentDiffId);
            changes.put(nodeId, objectDiff);
        }

        if (diff != null) {
            objectDiff.addDiff(diff);
        }

        return objectDiff;
    }

    /**
     * Returns a number of objects currently registered with this ObjectStore.
     * 
     * @since 1.2
     */
    public int registeredObjectsCount() {
        return objectMap.size();
    }

    /**
     * Returns a number of query results cached by this object store. Note that each
     * result is a list and can possibly contain a large number of entries.
     * 
     * @since 1.2
     * @deprecated since 3.0. See {@link DataContext#getQueryCache()}.
     */
    @Deprecated
    public int cachedQueriesCount() {
        return context != null && context.getQueryCache() != null ? context
                .getQueryCache()
                .size() : 0;
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
     * Invalidates a collection of DataObjects. Changes objects state to HOLLOW.
     * 
     * @deprecated since 3.0, use {@link DataContext#invalidateObjects(Collection)} or
     *             {@link RefreshQuery}.
     */
    @Deprecated
    public synchronized void objectsInvalidated(Collection objects) {
        if (context != null) {
            context.invalidateObjects(objects);
        }
    }

    /**
     * Evicts a collection of DataObjects from the ObjectStore, invalidates the underlying
     * cache snapshots. Changes objects state to TRANSIENT. This method can be used for
     * manual cleanup of Cayenne cache.
     * 
     * @see #objectsInvalidated(Collection)
     */
    // this method is exactly the same as "objectsInvalidated", only additionally it
    // throws out registered objects
    public synchronized void objectsUnregistered(Collection objects) {
        if (objects.isEmpty()) {
            return;
        }

        Collection<ObjectId> ids = new ArrayList<ObjectId>(objects.size());

        Iterator it = objects.iterator();
        while (it.hasNext()) {
            Persistent object = (Persistent) it.next();

            ObjectId id = object.getObjectId();

            // remove object but not snapshot
            objectMap.remove(id);
            changes.remove(id);
            ids.add(id);

            object.setObjectContext(null);
            object.setObjectId(null);
            object.setPersistenceState(PersistenceState.TRANSIENT);
        }

        // TODO, andrus 3/28/2006 - DRC is null in nested contexts... implement
        // propagation of unregister operation through the stack ... or do the opposite
        // and keep unregister local even for non-nested DC?
        if (getDataRowCache() != null) {
            // send an event for removed snapshots
            getDataRowCache().processSnapshotChanges(
                    this,
                    Collections.EMPTY_MAP,
                    Collections.EMPTY_LIST,
                    ids,
                    Collections.EMPTY_LIST);
        }
    }

    /**
     * Reverts changes to all stored uncomitted objects.
     * 
     * @since 1.1
     */
    public synchronized void objectsRolledBack() {
        Iterator it = getObjectIterator();

        // collect candidates
        while (it.hasNext()) {
            Persistent object = (Persistent) it.next();
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
        this.changes = new HashMap<Object, ObjectDiff>();
    }

    /**
     * Updates snapshots in the underlying DataRowStore. If <code>refresh</code> is true,
     * all snapshots in <code>snapshots</code> will be loaded into DataRowStore,
     * regardless of the existing cache state. If <code>refresh</code> is false, only
     * missing snapshots are loaded. This method is normally called internally by the
     * DataContext owning the ObjectStore to update the caches after a select query.
     * 
     * @param objects a list of object whose snapshots need to be updated.
     * @param snapshots a list of snapshots. Must be of the same length and use the same
     *            order as <code>objects</code> list.
     * @param refresh controls whether existing cached snapshots should be replaced with
     *            the new ones.
     * @since 1.1
     * @deprecated since 3.0 unused
     */
    @Deprecated
    public void snapshotsUpdatedForObjects(List objects, List snapshots, boolean refresh) {
        DataRowStore cache = getDataRowCache();
        if (cache != null) {
            synchronized (this) {
                cache.snapshotsUpdatedForObjects(objects, snapshots, refresh);
            }
        }
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

            Persistent object = objectMap.get(id);

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
    void postprocessAfterCommit(GraphDiff parentChanges) {

        // scan through changed objects, set persistence state to committed
        for (Object id : changes.keySet()) {
            Persistent object = objectMap.get(id);

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

                public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
                }

                public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
                }

                public void nodeCreated(Object nodeId) {
                }

                public void nodeIdChanged(Object nodeId, Object newId) {
                    processIdChange(nodeId, newId);
                }

                public void nodePropertyChanged(
                        Object nodeId,
                        String property,
                        Object oldValue,
                        Object newValue) {
                }

                public void nodeRemoved(Object nodeId) {
                }
            });
        }

        // create new instance of changes map so that event listeners who stored the
        // original diff don't get affected
        this.changes = new HashMap<Object, ObjectDiff>();
    }

    /**
     * Starts tracking the registration of new objects from this ObjectStore. Used in
     * conjunction with unregisterNewObjects() to control garbage collection when an
     * instance of ObjectStore is used over a longer time for batch processing.
     * 
     * @deprecated since 3.0 as ObjectStore holds weak reference to unmodified objects and
     *             this feature is useless.
     */
    @Deprecated
    public synchronized void startTrackingNewObjects() {
        // noop
    }

    /**
     * Unregisters the newly registered DataObjects from this objectStore. Used in
     * conjunction with startTrackingNewObjects() to control garbage collection when an
     * instance of ObjectStore is used over a longer time for batch processing.
     * 
     * @deprecated since 3.0 as ObjectStore holds weak reference to unmodified objects and
     *             this feature is useless.
     */
    @Deprecated
    public synchronized void unregisterNewObjects() {
        // noop
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
     * Returns cached query results for a given query, or null if no results are cached.
     * Note that ObjectStore will only lookup results in its local cache, and not the
     * shared cache associated with the underlying DataRowStore.
     * 
     * @since 1.1
     * @deprecated since 3.0. See {@link DataContext#getQueryCache()}.
     */
    @Deprecated
    public synchronized List getCachedQueryResult(String name) {
        return context != null && context.getQueryCache() != null ? context
                .getQueryCache()
                .get(new CacheQueryMetadata(name)) : null;
    }

    /**
     * Caches a list of query results.
     * 
     * @since 1.1
     * @deprecated since 3.0. See {@link DataContext#getQueryCache()}.
     */
    @Deprecated
    public synchronized void cacheQueryResult(String name, List results) {
        if (context != null) {
            context.getQueryCache().put(new CacheQueryMetadata(name), results);
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
    public synchronized Iterator getObjectIterator() {
        return objectMap.values().iterator();
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
        List<Persistent> filteredObjects = new ArrayList<Persistent>();

        for (Persistent object : objectMap.values()) {
            if (object.getPersistenceState() == state) {
                filteredObjects.add(object);
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

        Map modifiedDiffs = event.getModifiedDiffs();
        if (modifiedDiffs != null && !modifiedDiffs.isEmpty()) {
            Iterator oids = modifiedDiffs.entrySet().iterator();

            while (oids.hasNext()) {
                Map.Entry entry = (Map.Entry) oids.next();
                processUpdatedSnapshot(entry.getKey(), (DataRow) entry.getValue());
            }
        }

        Collection deletedIDs = event.getDeletedIds();
        if (deletedIDs != null && !deletedIDs.isEmpty()) {
            Iterator it = deletedIDs.iterator();
            while (it.hasNext()) {
                processDeletedID(it.next());
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

    /**
     * Initializes object with data from cache or from the database, if this object is not
     * fully resolved.
     * 
     * @since 1.1
     * @deprecated since 3.0 use
     *             {@link ObjectContext#prepareForAccess(Persistent, String, boolean)}.
     */
    @Deprecated
    public void resolveHollow(Persistent object) {
        context.prepareForAccess(object, null, false);
    }

    void processIdChange(Object nodeId, Object newId) {
        Persistent object = objectMap.remove(nodeId);

        if (object != null) {
            object.setObjectId((ObjectId) newId);
            objectMap.put(newId, object);

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
    void processDeletedID(Object nodeId) {

        // access object map directly - the method should be called in a synchronized
        // context...
        Persistent object = objectMap.get(nodeId);

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
    void processInvalidatedIDs(Collection invalidatedIDs) {
        if (invalidatedIDs != null && !invalidatedIDs.isEmpty()) {
            Iterator it = invalidatedIDs.iterator();
            while (it.hasNext()) {
                ObjectId oid = (ObjectId) it.next();
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
    void processIndirectlyModifiedIDs(Collection indirectlyModifiedIDs) {
        Iterator indirectlyModifiedIt = indirectlyModifiedIDs.iterator();
        while (indirectlyModifiedIt.hasNext()) {
            ObjectId oid = (ObjectId) indirectlyModifiedIt.next();

            // access object map directly - the method should be called in a synchronized
            // context...
            final DataObject object = (DataObject) objectMap.get(oid);

            if (object == null
                    || object.getPersistenceState() != PersistenceState.COMMITTED) {
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

                    public boolean visitToMany(ToManyProperty property) {
                        property.invalidate(object);
                        return true;
                    }

                    public boolean visitToOne(ToOneProperty property) {
                        if (property
                                .getRelationship()
                                .isSourceIndependentFromTargetChange()) {
                            property.invalidate(object);
                        }
                        return true;
                    }

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
    void processUpdatedSnapshot(Object nodeId, DataRow diff) {

        // access object map directly - the method should be called in a synchronized
        // context...
        DataObject object = (DataObject) objectMap.get(nodeId);

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
                                .getClassDescriptor(((ObjectId) nodeId).getEntityName());

                        // TODO: andrus, 5/26/2006 - call to 'getSnapshot' is expensive,
                        // however my attempts to merge the 'diff' instead of snapshot
                        // via 'refreshObjectWithSnapshot' resulted in even worse
                        // performance.
                        // This sounds counterintuitive (Not sure if this is some HotSpot
                        // related glitch)... still keeping the old algorithm here until
                        // we
                        // switch from snapshot events to GraphEvents and all this code
                        // becomes obsolete.
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
                                .getClassDescriptor(((ObjectId) nodeId).getEntityName());
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
    public synchronized Object getNode(Object nodeId) {
        return objectMap.get(nodeId);
    }

    // non-synchronized version of getNode for private use
    final Object getNodeNoSync(Object nodeId) {
        return objectMap.get(nodeId);
    }

    /**
     * Returns all registered DataObjects. List is returned by copy and can be modified by
     * the caller.
     * 
     * @since 1.2
     */
    public synchronized Collection<Object> registeredNodes() {
        return new ArrayList<Object>(objectMap.values());
    }

    /**
     * @since 1.2
     */
    public synchronized void registerNode(Object nodeId, Object nodeObject) {
        objectMap.put(nodeId, (Persistent) nodeObject);
    }

    /**
     * @since 1.2
     */
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
    public void nodeIdChanged(Object nodeId, Object newId) {
        throw new UnsupportedOperationException("nodeIdChanged");
    }

    /**
     * @since 1.2
     */
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
    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        NodeDiff diff = new ArcOperation(nodeId, targetNodeId, arcId.toString(), false);

        if (lifecycleEventInducedChanges != null) {
            registerLifecycleEventInducedChange(diff);
        }

        registerDiff(nodeId, diff);
    }

    /**
     * @since 1.2
     */
    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        NodeDiff diff = new ArcOperation(nodeId, targetNodeId, arcId.toString(), true);

        if (lifecycleEventInducedChanges != null) {
            registerLifecycleEventInducedChange(diff);
        }

        registerDiff(nodeId, diff);
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

        public void apply(GraphChangeHandler handler) {
            throw new UnsupportedOperationException();
        }

        public boolean isNoop() {
            throw new UnsupportedOperationException();
        }

        public void undo(GraphChangeHandler handler) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * @deprecated since 3.0 as this inner class is used to provide backwards
     *             compatibility for some deprecated methods.
     */
    @Deprecated
    final class CacheQueryMetadata implements QueryMetadata {

        private String cacheKey;

        CacheQueryMetadata(String cacheKey) {
            this.cacheKey = cacheKey;
        }

        public String getCacheKey() {
            return cacheKey;
        }

        public List<Object> getResultSetMapping() {
            return null;
        }

        public Query getOrginatingQuery() {
            return null;
        }

        public String[] getCacheGroups() {
            return null;
        }

        public String getCachePolicy() {
            return null;
        }

        public QueryCacheStrategy getCacheStrategy() {
            return null;
        }

        public DataMap getDataMap() {
            return null;
        }

        public DbEntity getDbEntity() {
            return null;
        }

        public int getFetchLimit() {
            return 0;
        }

        public int getFetchOffset() {
            return 0;
        }

        /**
         * @deprecated since 3.0
         */
        @Deprecated
        public int getFetchStartIndex() {
            return getFetchOffset();
        }

        public ObjEntity getObjEntity() {
            return null;
        }

        public ClassDescriptor getClassDescriptor() {
            return null;
        }

        public int getPageSize() {
            return 0;
        }

        public PrefetchTreeNode getPrefetchTree() {
            return null;
        }

        public Map<String, String> getPathSplitAliases() {
            return null;
        }

        public Procedure getProcedure() {
            return null;
        }

        public boolean isFetchingDataRows() {
            return false;
        }

        public boolean isRefreshingObjects() {
            return false;
        }

        public boolean isResolvingInherited() {
            return false;
        }

        public int getStatementFetchSize() {
            return 0;
        }
    }
}
