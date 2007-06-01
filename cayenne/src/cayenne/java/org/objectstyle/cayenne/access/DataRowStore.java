/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.collections.map.LRUMap;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.access.event.SnapshotEvent;
import org.objectstyle.cayenne.access.util.QueryUtils;
import org.objectstyle.cayenne.access.util.SelectObserver;
import org.objectstyle.cayenne.event.EventBridge;
import org.objectstyle.cayenne.event.EventBridgeFactory;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.event.EventSubject;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * A fixed size cache of DataRows keyed by ObjectId.
 * <p>
 * <strong>Synchronization Note: </strong> DataRowStore synchronizes most operations on
 * its own instance.
 * </p>
 * 
 * @author Andrei Adamchik
 * @since 1.1
 */
public class DataRowStore implements Serializable {

    private static Logger logObj = Logger.getLogger(DataRowStore.class);

    // property keys
    public static final String SNAPSHOT_EXPIRATION_PROPERTY = "cayenne.DataRowStore.snapshot.expiration";
    public static final String SNAPSHOT_CACHE_SIZE_PROPERTY = "cayenne.DataRowStore.snapshot.size";
    public static final String REMOTE_NOTIFICATION_PROPERTY = "cayenne.DataRowStore.remote.notify";
    public static final String EVENT_BRIDGE_FACTORY_PROPERTY = "cayenne.DataRowStore.EventBridge.factory";

    // default property values

    // default expiration time is 2 hours
    public static final long SNAPSHOT_EXPIRATION_DEFAULT = 2 * 60 * 60;
    public static final int SNAPSHOT_CACHE_SIZE_DEFAULT = 10000;
    public static final boolean REMOTE_NOTIFICATION_DEFAULT = false;

    // use String for class name, since JavaGroups may not be around,
    // causing CNF exceptions
    public static final String EVENT_BRIDGE_FACTORY_DEFAULT = "org.objectstyle.cayenne.event.JavaGroupsBridgeFactory";

    protected String name;
    protected LRUMap snapshots;
    protected LRUMap snapshotLists;
    protected boolean notifyingRemoteListeners;

    protected transient EventBridge remoteNotificationsHandler;

    // IMPORTANT: EventSubject must be an ivar to avoid its deallocation
    // too early, and thus disabling events.
    protected transient EventSubject eventSubject;

    /**
     * Creates new named DataRowStore with default configuration.
     */
    public DataRowStore(String name) {
        this(name, Collections.EMPTY_MAP);
    }

    /**
     * Creates new DataRowStore with a specified name and a set of properties. If no
     * properties are defined, default values are used.
     * 
     * @param name DataRowStore name. Used to idenitfy this DataRowStore in events, etc.
     *            Can't be null.
     * @param properties Properties map used to configure DataRowStore parameters. Can be
     *            null.
     */
    public DataRowStore(String name, Map properties) {
        if (name == null) {
            throw new IllegalArgumentException("DataRowStore name can't be null.");
        }

        this.name = name;
        this.eventSubject = createSubject();
        initWithProperties(properties);
    }

    private EventSubject createSubject() {
        return EventSubject.getSubject(this.getClass(), name);
    }

    protected void initWithProperties(Map properties) {
        ExtendedProperties propertiesWrapper = new ExtendedProperties();

        if (properties != null) {
            propertiesWrapper.putAll(properties);
        }

        long snapshotsExpiration = propertiesWrapper.getLong(
                SNAPSHOT_EXPIRATION_PROPERTY,
                SNAPSHOT_EXPIRATION_DEFAULT);

        int snapshotsCacheSize = propertiesWrapper.getInt(
                SNAPSHOT_CACHE_SIZE_PROPERTY,
                SNAPSHOT_CACHE_SIZE_DEFAULT);

        boolean notifyRemote = propertiesWrapper.getBoolean(
                REMOTE_NOTIFICATION_PROPERTY,
                REMOTE_NOTIFICATION_DEFAULT);

        String eventBridgeFactory = propertiesWrapper.getString(
                EVENT_BRIDGE_FACTORY_PROPERTY,
                EVENT_BRIDGE_FACTORY_DEFAULT);

        if (logObj.isDebugEnabled()) {
            logObj.debug("DataRowStore property "
                    + SNAPSHOT_EXPIRATION_PROPERTY
                    + " = "
                    + snapshotsExpiration);
            logObj.debug("DataRowStore property "
                    + SNAPSHOT_CACHE_SIZE_PROPERTY
                    + " = "
                    + snapshotsCacheSize);
            logObj.debug("DataRowStore property "
                    + REMOTE_NOTIFICATION_PROPERTY
                    + " = "
                    + notifyRemote);
            logObj.debug("DataRowStore property "
                    + EVENT_BRIDGE_FACTORY_PROPERTY
                    + " = "
                    + eventBridgeFactory);
        }

        // init ivars from properties
        this.notifyingRemoteListeners = notifyRemote;

        // TODO: ENTRY EXPIRATION is not supported by commons LRU Map
        this.snapshots = new LRUMap(snapshotsCacheSize);

        // TODO: cache size should really be a sum of all result lists sizes...
        // so we must track it outside the LRUMap...
        this.snapshotLists = new LRUMap(snapshotsCacheSize);

        // init event bridge only if we are notifying remote listeners
        if (notifyingRemoteListeners) {
            try {
                EventBridgeFactory factory = (EventBridgeFactory) Class
                        .forName(eventBridgeFactory)
                        .newInstance();
                this.remoteNotificationsHandler = factory
                        .createEventBridge(getSnapshotEventSubject(), properties);

                // listen to EventBridge ... must add itself as non-blocking listener
                // otherwise a deadlock can occur as "processRemoteEvent" will attempt to
                // obtain a lock on this object when the dispatch queue is locked... And
                // another commit thread may have this object locked and attempt to lock
                // dispatch queue

                EventManager.getDefaultManager().addNonBlockingListener(this,
                        "processRemoteEvent",
                        SnapshotEvent.class,
                        getSnapshotEventSubject(),
                        remoteNotificationsHandler);

                // start EventBridge - it will listen to all event sources for this
                // subject
                remoteNotificationsHandler.startup(EventManager.getDefaultManager(),
                        EventBridge.RECEIVE_LOCAL_EXTERNAL);
            }
            catch (Exception ex) {
                throw new CayenneRuntimeException("Error initializing DataRowStore.", ex);
            }
        }
    }

    /**
     * Returns current cache size.
     */
    public int size() {
        return snapshots.size();
    }

    /**
     * Returns maximum allowed cache size.
     */
    public int maximumSize() {
        return snapshots.maxSize();
    }

    /**
     * Shuts down any remote notification connections, and clears internal cache.
     */
    public void shutdown() {
        if (remoteNotificationsHandler != null) {
            try {
                remoteNotificationsHandler.shutdown();
            }
            catch (Exception ex) {
                logObj.info("Exception shutting down EventBridge.", ex);
            }
            remoteNotificationsHandler = null;
        }

        clear();
    }

    /**
     * Returns the name of this SnapshotCache. Name allows to create EventSubjects for
     * event notifications addressed to or sent from this SnapshotCache.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns cached snapshot or null if no snapshot is currently cached for the given
     * ObjectId.
     */
    public synchronized DataRow getCachedSnapshot(ObjectId oid) {
        return (DataRow) snapshots.get(oid);
    }

    /**
     * Returns a snapshot for ObjectId. If snapshot is currently cached, it is returned.
     * If not, a provided QueryEngine is used to fetch it from the database. If there is
     * no database row for a given id, null is returned.
     */
    public synchronized DataRow getSnapshot(ObjectId oid, QueryEngine engine) {

        // try cache
        DataRow cachedSnapshot = getCachedSnapshot(oid);
        if (cachedSnapshot != null) {
            return cachedSnapshot;
        }

        if (logObj.isDebugEnabled()) {
            logObj.debug("no cached snapshot for ObjectId: " + oid);
        }

        // try getting it from database
        SelectQuery select = QueryUtils.selectObjectForId(oid);
        SelectObserver observer = new SelectObserver();
        engine.performQueries(Collections.singletonList(select), observer);
        List results = observer.getResults(select);

        if (results.size() > 1) {
            throw new CayenneRuntimeException("More than 1 object found for ObjectId "
                    + oid
                    + ". Fetch matched "
                    + results.size()
                    + " objects.");
        } else if (results.size() == 0) {
            return null;
        } else {
            DataRow snapshot = (DataRow) results.get(0);
            snapshots.put(oid, snapshot);
            return snapshot;
        }
    }

    /**
     * Registers a list of snapshots with internal cache, using a String key.
     */
    public void cacheSnapshots(String key, List snapshots) {
        snapshotLists.put(key, snapshots);
    }

    /**
     * Returns a list of previously cached snapshots.
     */
    public List getCachedSnapshots(String key) {
        if (key == null) {
            return null;
        }

        return (List) snapshotLists.get(key);
    }

    /**
     * Returns EventSubject used by this SnapshotCache to notify of snapshot changes.
     */
    public EventSubject getSnapshotEventSubject() {
        return eventSubject;
    }

    /**
     * Expires and removes all stored snapshots without sending any notification events.
     */
    public synchronized void clear() {
        snapshots.clear();
        snapshotLists.clear();
    }

    /**
     * Evicts a snapshot from cache without generating any SnapshotEvents.
     */
    public synchronized void forgetSnapshot(ObjectId id) {
        snapshots.remove(id);
    }

    /**
     * Handles remote events received via EventBridge. Performs needed snapshot updates,
     * and then resends the event to local listeners.
     */
    public void processRemoteEvent(SnapshotEvent event) {
        if (event.getPostedBy() == this
                || event.getSource() != remoteNotificationsHandler) {
            return;
        }

        if (logObj.isDebugEnabled()) {
            logObj.debug("remote event: " + event);
        }

        Collection deletedSnapshotIds = event.getDeletedIds();
        Map diffs = event.getModifiedDiffs();
        Collection indirectlyModifiedIds = event.getIndirectlyModifiedIds();

        if (deletedSnapshotIds.isEmpty()
                && diffs.isEmpty()
                && indirectlyModifiedIds.isEmpty()) {
            logObj.warn("processRemoteEvent.. bogus call... no changes.");
            return;
        }

        synchronized (this) {
            processDeletedIDs(deletedSnapshotIds);
            processUpdateDiffs(diffs);
            sendUpdateNotification(
                    event.getSource(),
                    diffs,
                    deletedSnapshotIds,
                    indirectlyModifiedIds);
        }
    }

    /**
     * Processes changes made to snapshots. Modifies internal cache state, and then sends
     * the event to all listeners. Source of these changes is usually an ObjectStore.
     */
    public void processSnapshotChanges(
            Object source,
            Map updatedSnapshots,
            Collection deletedSnapshotIds,
            Collection indirectlyModifiedIds) {

        // update the internal cache, prepare snapshot event

        if (deletedSnapshotIds.isEmpty()
                && updatedSnapshots.isEmpty()
                && indirectlyModifiedIds.isEmpty()) {
            logObj.warn("postSnapshotsChangeEvent.. bogus call... no changes.");
            return;
        }

        synchronized (this) {
            processDeletedIDs(deletedSnapshotIds);
            Map diffs = processUpdatedSnapshots(updatedSnapshots);
            sendUpdateNotification(
                    source,
                    diffs,
                    deletedSnapshotIds,
                    indirectlyModifiedIds);
        }
    }

    private void processDeletedIDs(Collection deletedSnapshotIDs) {
        // DELETED: evict deleted snapshots
        if (!deletedSnapshotIDs.isEmpty()) {
            Iterator it = deletedSnapshotIDs.iterator();
            while (it.hasNext()) {
                snapshots.remove(it.next());
            }
        }
    }

    private Map processUpdatedSnapshots(Map updatedSnapshots) {
        Map diffs = null;

        // MODIFIED: replace/add snapshots, generate diffs for event
        if (!updatedSnapshots.isEmpty()) {
            Iterator it = updatedSnapshots.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                
                ObjectId key = (ObjectId) entry.getKey();
                DataRow newSnapshot = (DataRow) entry.getValue();
                DataRow oldSnapshot = (DataRow) snapshots.put(key, newSnapshot);

                // generate diff for the updated event, if this not a new
                // snapshot

                // The following cases should be handled here:

                // 1. There is no previously cached snapshot for a given id.
                // 2. There was a previously cached snapshot for a given id,
                //    but it expired from cache and was removed. Currently
                //    handled as (1); what are the consequences of that?
                // 3. There is a previously cached snapshot and it has the
                //    *same version* as the "replacesVersion" property of the
                //    new snapshot.
                // 4. There is a previously cached snapshot and it has a
                //    *different version* from "replacesVersion" property of
                //    the new snapshot. It means that we don't know how to merge
                //    the two (we don't even know which one is newer due to
                //    multithreading). Just throw out this snapshot....

                if (oldSnapshot != null) {
                    // case 4 above... have to throw out the snapshot since
                    // no good options exist to tell how to merge the two.
                    if (oldSnapshot.getVersion() != newSnapshot.getReplacesVersion()) {
                        logObj
                                .debug("snapshot version changed, don't know what to do... Old: "
                                        + oldSnapshot
                                        + ", New: "
                                        + newSnapshot);
                        forgetSnapshot(key);
                        continue;
                    }

                    Map diff = oldSnapshot.createDiff(newSnapshot);

                    if (diff != null) {
                        if (diffs == null) {
                            diffs = new HashMap();
                        }

                        diffs.put(key, diff);
                    }
                }
            }
        }

        return diffs;
    }

    private void processUpdateDiffs(Map diffs) {
        // apply snapshot diffs
        if (!diffs.isEmpty()) {
            Iterator it = diffs.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                ObjectId key = (ObjectId) entry.getKey();
                DataRow oldSnapshot = (DataRow) snapshots.remove(key);

                if (oldSnapshot == null) {
                    continue;
                }

                DataRow newSnapshot = oldSnapshot.applyDiff((DataRow) entry.getValue());
                snapshots.put(key, newSnapshot);
            }
        }
    }

    private void sendUpdateNotification(
            Object source,
            Map diffs,
            Collection deletedSnapshotIDs,
            Collection indirectlyModifiedIds) {

        // do not send bogus events... e.g. inserted objects are not counted
        if ((diffs != null && !diffs.isEmpty())
                || (deletedSnapshotIDs != null && !deletedSnapshotIDs.isEmpty())
                || (indirectlyModifiedIds != null && !indirectlyModifiedIds.isEmpty())) {

            SnapshotEvent event = new SnapshotEvent(
                    source,
                    this,
                    diffs,
                    deletedSnapshotIDs,
                    indirectlyModifiedIds);

            if (logObj.isDebugEnabled()) {
                logObj.debug("postSnapshotsChangeEvent: " + event);
            }

            // notify listeners

            // send synchronously, relying on listeners to
            // register as "non-blocking" if needed.
            EventManager.getDefaultManager().postEvent(event, getSnapshotEventSubject());
        }
    }

    public boolean isNotifyingRemoteListeners() {
        return notifyingRemoteListeners;
    }

    public void setNotifyingRemoteListeners(boolean notifyingRemoteListeners) {
        this.notifyingRemoteListeners = notifyingRemoteListeners;
    }

    // deserialization support
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        // restore subjects
        this.eventSubject = createSubject();
    }
}