/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.Fault;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.access.ObjectDiff.ArcOperation;
import org.objectstyle.cayenne.access.event.SnapshotEvent;
import org.objectstyle.cayenne.access.event.SnapshotEventListener;
import org.objectstyle.cayenne.graph.CompoundDiff;
import org.objectstyle.cayenne.graph.GraphChangeHandler;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.GraphManager;
import org.objectstyle.cayenne.graph.NodeCreateOperation;
import org.objectstyle.cayenne.graph.NodeDeleteOperation;
import org.objectstyle.cayenne.graph.NodeDiff;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.ObjectIdQuery;
import org.objectstyle.cayenne.validation.ValidationException;
import org.objectstyle.cayenne.validation.ValidationResult;

/**
 * ObjectStore stores objects using their ObjectId as a key. It works as a dedicated
 * object cache for a DataContext. Users rarely need to access ObjectStore directly, as
 * DataContext serves as a facade, providing cover methods for most ObjectStore
 * operations.
 * 
 * @since 1.0
 * @author Andrus Adamchik
 */
// Synchronization Note: There is often a need to do double synchronize on an ObjectStore
// and an underlying DataRowCache. To avoid deadlocks, Cayenne consistently follows the
// policy of locking an ObjectStore first, and then locking DataRowStore. This pattern
// must be followed in any new related developments.
public class ObjectStore implements Serializable, SnapshotEventListener, GraphManager {

    protected transient Map newObjectsMap;

    protected Map objectMap = new HashMap();
    protected Map queryResultMap = new HashMap();
    protected Map changes = new HashMap();

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

    /**
     * The DataContext that owns this ObjectStore.
     */
    protected DataContext context;

    public ObjectStore() {
    }

    public ObjectStore(DataRowStore dataRowCache) {
        setDataRowCache(dataRowCache);
    }

    /**
     * @since 1.2
     */
    void recordObjectDeleted(Persistent object) {
        object.setPersistenceState(PersistenceState.DELETED);
        registerDiff(object, new NodeDeleteOperation(object.getObjectId()));
    }

    /**
     * @since 1.2
     */
    void recordObjectCreated(Persistent object) {
        registerDiff(object, new NodeCreateOperation(object.getObjectId()));
        registerNode(object.getObjectId(), object);
    }

    /**
     * Performs tracking of object relationship changes.
     * 
     * @since 1.2
     */
    // TODO: Andrus, 3/14/2006 - this method should be made non-public once we remove
    // direct ObjectStore access from CayenneDataObject.
    public void recordArcCreated(
            Persistent object,
            ObjectId targetId,
            String relationshipName) {

        registerDiff(object, new ArcOperation(
                object.getObjectId(),
                targetId,
                relationshipName,
                false));
    }

    /**
     * Performs tracking of object relationship changes.
     * 
     * @since 1.2
     */
    // TODO: Andrus, 3/14/2006 - this method should be made non-public once we remove
    // direct ObjectStore access from CayenneDataObject.
    public void recordArcDeleted(
            Persistent object,
            ObjectId targetId,
            String relationshipName) {
        registerDiff(object, new ArcOperation(
                object.getObjectId(),
                targetId,
                relationshipName,
                true));
    }

    /**
     * Registers object change.
     * 
     * @since 1.2
     */
    synchronized ObjectDiff registerDiff(Persistent object, NodeDiff diff) {

        ObjectId id = object.getObjectId();

        if (object.getPersistenceState() == PersistenceState.COMMITTED) {
            object.setPersistenceState(PersistenceState.MODIFIED);

            // TODO: andrus 3/23/2006 snapshot versions are obsolete, but there is no
            // replacement yet, so we still need to handle them...
            if (object instanceof DataObject) {

                DataObject dataObject = (DataObject) object;
                DataRow snapshot = getCachedSnapshot(id);

                if (snapshot != null
                        && snapshot.getVersion() != dataObject.getSnapshotVersion()) {
                    DataContextDelegate delegate = dataObject
                            .getDataContext()
                            .nonNullDelegate();
                    if (delegate.shouldMergeChanges(dataObject, snapshot)) {
                        ObjEntity entity = dataObject
                                .getDataContext()
                                .getEntityResolver()
                                .lookupObjEntity(object);
                        DataRowUtils.forceMergeWithSnapshot(entity, dataObject, snapshot);
                        dataObject.setSnapshotVersion(snapshot.getVersion());
                        delegate.finishedMergeChanges(dataObject);
                    }
                }
            }
        }

        if (diff != null) {
            diff.setDiffId(++currentDiffId);
        }

        ObjectDiff objectDiff = (ObjectDiff) changes.get(id);

        if (objectDiff == null) {
            objectDiff = new ObjectDiff(this, object);
            objectDiff.setDiffId(++currentDiffId);
            changes.put(id, objectDiff);
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
     */
    public int cachedQueriesCount() {
        return queryResultMap.size();
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

        if (this.dataRowCache != null && dataRowCache.getEventManager() != null) {
            dataRowCache.getEventManager().removeListener(
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
     * @see #objectsUnregistered(Collection)
     */
    public synchronized void objectsInvalidated(Collection objects) {
        if (objects.isEmpty()) {
            return;
        }

        Collection ids = new ArrayList(objects.size());
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();

            // we don't care about NEW objects,
            // but we still do care about HOLLOW, since snapshot might still be
            // present
            if (object.getPersistenceState() == PersistenceState.NEW) {
                continue;
            }

            object.setPersistenceState(PersistenceState.HOLLOW);

            // remove cached changes
            changes.remove(object.getObjectId());

            // remember the id
            ids.add(object.getObjectId());
        }

        // TODO, andrus 3/28/2006 - DRC is null in nested contexts... implement
        // propagation of invalidate operation through the stack
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

        Collection ids = new ArrayList(objects.size());

        Iterator it = objects.iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();

            ObjectId id = object.getObjectId();

            // remove object but not snapshot
            objectMap.remove(id);
            changes.remove(id);
            ids.add(id);

            object.setDataContext(null);
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
            DataObject object = (DataObject) it.next();
            int objectState = object.getPersistenceState();
            switch (objectState) {
                case PersistenceState.NEW:
                    it.remove();

                    object.setDataContext(null);
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
        this.changes = new HashMap();
    }

    /**
     * Performs tracking of object relationship changes.
     * 
     * @since 1.1
     * @deprecated since 1.2 use {@link #recordArcDeleted(Persistent, ObjectId, String)}.
     */
    public void objectRelationshipUnset(
            DataObject source,
            DataObject target,
            ObjRelationship relationship,
            boolean processFlattened) {

        ObjectId targetId = (target != null) ? target.getObjectId() : null;

        recordArcDeleted(source, targetId, relationship.getName());
    }

    /**
     * Performs tracking of object relationship changes.
     * 
     * @since 1.1
     * @deprecated since 1.2 use {@link #recordArcCreated(Persistent, ObjectId, String)}.
     */
    public void objectRelationshipSet(
            DataObject source,
            DataObject target,
            ObjRelationship relationship,
            boolean processFlattened) {

        ObjectId targetId = (target != null) ? target.getObjectId() : null;
        recordArcCreated(source, targetId, relationship.getName());
    }

    /**
     * Updates snapshots in the underlying DataRowStore. If <code>refresh</code> is
     * true, all snapshots in <code>snapshots</code> will be loaded into DataRowStore,
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
     */
    // TODO:, andrus 5/25/2006 - mark as deprecated after 1.2 - this method is no longer
    // used.
    public void snapshotsUpdatedForObjects(List objects, List snapshots, boolean refresh) {
        DataRowStore cache = getDataRowCache();
        if (cache != null) {
            synchronized (this) {
                cache.snapshotsUpdatedForObjects(objects, snapshots, refresh);
            }
        }
    }

    /**
     * Processes internal objects after the parent DataContext was committed. Changes
     * object persistence state and handles snapshot updates.
     * 
     * @since 1.1
     * @deprecated since 1.2 unused.
     */
    public synchronized void objectsCommitted() {
        postprocessAfterCommit(new CompoundDiff());
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
    Map getChangesByObjectId() {
        return changes;
    }

    /**
     * @since 1.2
     */
    void postprocessAfterPhantomCommit() {

        Iterator it = changes.keySet().iterator();
        while (it.hasNext()) {
            ObjectId id = (ObjectId) it.next();

            Persistent object = (Persistent) objectMap.get(id);

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

        Iterator entries = objectMap.entrySet().iterator();

        // have to scan through all entries
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();

            DataObject object = (DataObject) entry.getValue();

            switch (object.getPersistenceState()) {
                case PersistenceState.DELETED:
                    entries.remove();
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
        this.changes = new HashMap();
    }

    /**
     * Adds a new object to the ObjectStore.
     * 
     * @deprecated since 1.2 as a different change tracking algorithm is used.
     */
    public synchronized void addObject(DataObject object) {
        recordObjectCreated(object);
    }

    /**
     * Starts tracking the registration of new objects from this ObjectStore. Used in
     * conjunction with unregisterNewObjects() to control garbage collection when an
     * instance of ObjectStore is used over a longer time for batch processing.
     * 
     * @see org.objectstyle.cayenne.access.ObjectStore#unregisterNewObjects()
     */
    public synchronized void startTrackingNewObjects() {
        newObjectsMap = new HashMap();
    }

    /**
     * Unregisters the newly registered DataObjects from this objectStore. Used in
     * conjunction with startTrackingNewObjects() to control garbage collection when an
     * instance of ObjectStore is used over a longer time for batch processing.
     * 
     * @see org.objectstyle.cayenne.access.ObjectStore#startTrackingNewObjects()
     */
    public synchronized void unregisterNewObjects() {
        if (newObjectsMap != null) {
            objectsUnregistered(newObjectsMap.values());
            newObjectsMap = null;
        }
    }

    /**
     * Returns a DataObject registered for a given ObjectId, or null if no such object
     * exists. This method does not do a database fetch.
     * 
     * @deprecated since 1.2 a GraphManager {@link #getNode(Object)} method should be
     *             used.
     */
    public DataObject getObject(ObjectId id) {
        return (DataObject) getNode(id);
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
            List results = context.getChannel().onQuery(context, query).firstList();
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
     */
    public synchronized List getCachedQueryResult(String name) {
        // results should have been stored as rows or objects when
        // they were originally cached... do no conversions here
        return (List) queryResultMap.get(name);
    }

    /**
     * Caches a list of query results.
     * 
     * @since 1.1
     */
    public synchronized void cacheQueryResult(String name, List results) {
        queryResultMap.put(name, results);
    }

    /**
     * Returns a snapshot for ObjectId from the underlying snapshot cache. If cache
     * contains no snapshot, it will attempt fetching it using provided QueryEngine. If
     * fetch attempt fails or inconsistent data is returned, underlying cache will throw a
     * CayenneRuntimeException.
     * 
     * @since 1.1
     * @deprecated since 1.2. Use {@link #getSnapshot(ObjectId)} instead.
     */
    public synchronized DataRow getSnapshot(ObjectId oid, QueryEngine engine) {
        return getDataRowCache().getSnapshot(oid, engine);
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
            List results = context.getChannel().onQuery(context, query).firstList();
            return results.isEmpty() ? null : (DataRow) results.get(0);
        }
        else {
            return null;
        }
    }

    /**
     * Returns a list of objects that are registered with this DataContext, regardless of
     * their persistence state. List is returned by copy and can be modified by the
     * caller.
     * 
     * @deprecated since 1.2 use GraphManager method {@link #registeredNodes()}.
     */
    public synchronized List getObjects() {
        return new ArrayList(objectMap.values());
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
     * Return a subset of registered objects that are in a certian persistence state.
     * Collection is returned by copy.
     */
    public synchronized List objectsInState(int state) {
        List filteredObjects = new ArrayList();

        Iterator it = objectMap.values().iterator();
        while (it.hasNext()) {
            DataObject nextObj = (DataObject) it.next();
            if (nextObj.getPersistenceState() == state)
                filteredObjects.add(nextObj);
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
     * Performs validation of all uncommitted objects in the ObjectStore. If validation
     * fails, a ValidationException is thrown, listing all encountered failures. This is a
     * utility method for the users to call. Cayenne itself uses a different mechanism to
     * validate objects on commit.
     * 
     * @since 1.1
     * @throws ValidationException
     * @deprecated since 1.2 - This method is no longer used in Cayenne internally.
     */
    public synchronized void validateUncommittedObjects() throws ValidationException {

        // we must iterate over a copy of object list,
        // as calling validateFor* on DataObjects can have a side effect
        // of modifying this ObjectStore, and thus resulting in
        // ConcurrentModificationExceptions in the Iterator

        Collection deleted = null;
        Collection inserted = null;
        Collection updated = null;

        Iterator allIt = getObjectIterator();
        while (allIt.hasNext()) {
            DataObject dataObject = (DataObject) allIt.next();
            switch (dataObject.getPersistenceState()) {
                case PersistenceState.NEW:
                    if (inserted == null) {
                        inserted = new ArrayList();
                    }
                    inserted.add(dataObject);
                    break;
                case PersistenceState.MODIFIED:
                    if (updated == null) {
                        updated = new ArrayList();
                    }
                    updated.add(dataObject);
                    break;
                case PersistenceState.DELETED:
                    if (deleted == null) {
                        deleted = new ArrayList();
                    }
                    deleted.add(dataObject);
                    break;
            }
        }

        ValidationResult validationResult = new ValidationResult();

        if (deleted != null) {
            Iterator it = deleted.iterator();
            while (it.hasNext()) {
                DataObject dataObject = (DataObject) it.next();
                dataObject.validateForDelete(validationResult);
            }
        }

        if (inserted != null) {
            Iterator it = inserted.iterator();
            while (it.hasNext()) {
                DataObject dataObject = (DataObject) it.next();
                dataObject.validateForInsert(validationResult);
            }
        }

        if (updated != null) {
            Iterator it = updated.iterator();
            while (it.hasNext()) {
                DataObject dataObject = (DataObject) it.next();
                dataObject.validateForUpdate(validationResult);
            }
        }

        if (validationResult.hasFailures()) {
            throw new ValidationException(validationResult);
        }
    }

    /**
     * Initializes object with data from cache or from the database, if this object is not
     * fully resolved.
     * 
     * @since 1.1
     */
    public void resolveHollow(DataObject object) {
        if (object.getPersistenceState() != PersistenceState.HOLLOW) {
            return;
        }

        // no way to resolve faults outside of DataContext.
        DataContext context = object.getDataContext();
        if (context == null) {
            object.setPersistenceState(PersistenceState.TRANSIENT);
            return;
        }

        synchronized (this) {
            ObjectIdQuery query = new ObjectIdQuery(
                    object.getObjectId(),
                    false,
                    ObjectIdQuery.CACHE);
            List results = context.getChannel().onQuery(context, query).firstList();

            // handle deleted object
            if (results.size() == 0) {
                processDeletedID(object.getObjectId());
            }
            else if (object.getPersistenceState() == PersistenceState.HOLLOW) {

                // if HOLLOW is returned (from parent DC?), rerun the query with forced
                // fetch
                query = new ObjectIdQuery(
                        object.getObjectId(),
                        false,
                        ObjectIdQuery.CACHE_REFRESH);
                results = context.getChannel().onQuery(context, query).firstList();
                if (results.size() == 0) {
                    processDeletedID(object.getObjectId());
                }
            }
        }
    }

    void processIdChange(Object nodeId, Object newId) {
        Persistent object = (Persistent) objectMap.remove(nodeId);

        if (object != null) {
            object.setObjectId((ObjectId) newId);
            objectMap.put(newId, object);

            Object change = changes.remove(nodeId);
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
        DataObject object = (DataObject) objectMap.get(nodeId);

        if (object != null) {

            DataContextDelegate delegate;

            switch (object.getPersistenceState()) {
                case PersistenceState.COMMITTED:
                case PersistenceState.HOLLOW:
                case PersistenceState.DELETED:

                    // consult delegate
                    delegate = context.nonNullDelegate();

                    if (delegate.shouldProcessDelete(object)) {
                        objectMap.remove(nodeId);
                        changes.remove(nodeId);

                        // setting DataContext to null will also set
                        // state to transient
                        object.setObjectContext(null);
                        delegate.finishedProcessDelete(object);
                    }

                    break;

                case PersistenceState.MODIFIED:

                    // consult delegate
                    delegate = context.nonNullDelegate();
                    if (delegate.shouldProcessDelete(object)) {
                        object.setPersistenceState(PersistenceState.NEW);
                        changes.remove(nodeId);
                        recordObjectCreated(object);
                        delegate.finishedProcessDelete(object);
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
                        DataContext context = object.getDataContext();
                        DataRow diff = getSnapshot(oid);
                        // consult delegate if it exists
                        DataContextDelegate delegate = context.nonNullDelegate();
                        if (delegate.shouldMergeChanges(object, diff)) {
                            ObjEntity entity = context
                                    .getEntityResolver()
                                    .lookupObjEntity(object);
                            DataRowUtils.forceMergeWithSnapshot(entity, object, diff);
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
            Object oid = indirectlyModifiedIt.next();

            // access object map directly - the method should be called in a synchronized
            // context...
            DataObject object = (DataObject) objectMap.get(oid);

            if (object == null
                    || object.getPersistenceState() != PersistenceState.COMMITTED) {
                continue;
            }

            // for now "break" all "independent" object relationships...
            // in the future we may want to be more precise and go after modified
            // relationships only, or even process updated lists without invalidating...

            DataContextDelegate delegate = object.getDataContext().nonNullDelegate();

            if (delegate.shouldMergeChanges(object, null)) {
                ObjEntity entity = context.getEntityResolver().lookupObjEntity(object);
                Iterator relationshipIterator = entity.getRelationships().iterator();
                while (relationshipIterator.hasNext()) {
                    ObjRelationship relationship = (ObjRelationship) relationshipIterator
                            .next();

                    if (relationship.isSourceIndependentFromTargetChange()) {
                        Object fault = relationship.isToMany()
                                ? Fault.getToManyFault()
                                : Fault.getToOneFault();
                        object.writePropertyDirectly(relationship.getName(), fault);
                    }
                }

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

        // access object map directly - the method should be called ina synchronized
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
                        ObjEntity entity = context.getEntityResolver().lookupObjEntity(
                                object);

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
                                entity,
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
                        ObjEntity entity = context.getEntityResolver().lookupObjEntity(
                                object);
                        DataRowUtils.forceMergeWithSnapshot(entity, object, diff);
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
    public synchronized Collection registeredNodes() {
        return new ArrayList(objectMap.values());
    }

    /**
     * @since 1.2
     */
    public void registerNode(Object nodeId, Object nodeObject) {
        objectMap.put(nodeId, nodeObject);

        if (newObjectsMap != null) {
            newObjectsMap.put(nodeId, nodeObject);
        }
    }

    /**
     * @since 1.2
     */
    public Object unregisterNode(Object nodeId) {
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
    public void graphCommitAborted() {
    }

    /**
     * Does nothing.
     * 
     * @since 1.2
     */
    public void graphCommitStarted() {
    }

    /**
     * Does nothing.
     * 
     * @since 1.2
     */
    public void graphCommitted() {
    }

    /**
     * Does nothing.
     * 
     * @since 1.2
     */
    public void graphRolledback() {
    }

    /**
     * Does nothing.
     * 
     * @since 1.2
     */
    public void nodeIdChanged(Object nodeId, Object newId) {
        // noop
    }

    /**
     * Does nothing.
     * 
     * @since 1.2
     */
    public void nodeCreated(Object nodeId) {
        // noop
    }

    /**
     * Does nothing.
     * 
     * @since 1.2
     */
    public void nodeRemoved(Object nodeId) {
        // noop
    }

    /**
     * Does nothing.
     * 
     * @since 1.2
     */
    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {

        // noop
    }

    /**
     * Does nothing.
     * 
     * @since 1.2
     */
    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        // noop
    }

    /**
     * Does nothing.
     * 
     * @since 1.2
     */
    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        // noop
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
}