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

package org.apache.cayenne;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.cache.MapQueryCache;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.util.EventUtil;
import org.apache.cayenne.validation.ValidationException;
import org.apache.cayenne.validation.ValidationResult;

/**
 * A default generic implementation of ObjectContext suitable for accessing Cayenne from
 * either an ORM or a client tiers. Communicates with Cayenne via a
 * {@link org.apache.cayenne.DataChannel}.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class CayenneContext extends BaseContext {

    protected EntityResolver entityResolver;

    CayenneContextGraphManager graphManager;

    // note that it is important to reuse the same action within the property change
    // thread to avoid a loop of "propertyChange" calls on handling reverse relationships.
    // Here we go further and make action a thread-safe ivar that tracks its own thread
    // state.
    CayenneContextGraphAction graphAction;

    // object that merges "backdoor" changes that come from the channel.
    CayenneContextMergeHandler mergeHandler;

    QueryCache queryCache;

    /**
     * @since 3.0
     */
    boolean propertyChangeCallbacksDisabled;

    /**
     * Creates a new CayenneContext with no channel and disabled graph events.
     */
    public CayenneContext() {
        this(null);
    }

    /**
     * Creates a new CayenneContext, initializing it with a channel instance.
     * CayenneContext created using this constructor WILL NOT broadcast graph change
     * events.
     */
    public CayenneContext(DataChannel channel) {
        this(channel, false, false);
    }

    /**
     * Creates a new CayenneContext, initializing it with a channel. If
     * <code>graphEventsEnabled</code> is true, this context will broadcast GraphEvents
     * using ObjectContext.GRAPH_CHANGE_SUBJECT.
     */
    public CayenneContext(DataChannel channel, boolean changeEventsEnabled,
            boolean syncEventsEnabled) {

        this.graphAction = new CayenneContextGraphAction(this);
        this.graphManager = new CayenneContextGraphManager(
                this,
                changeEventsEnabled,
                syncEventsEnabled);

        setChannel(channel);
    }

    /**
     * Returns {@link QueryCache}, creating it on the fly if needed.
     * 
     * @since 3.0
     */
    QueryCache getQueryCache() {

        if (queryCache == null) {
            synchronized (this) {
                if (queryCache == null) {
                    // TODO: andrus, 7/27/2006 - figure out the factory stuff like we have
                    // in DataContext
                    queryCache = new MapQueryCache();
                }
            }
        }

        return queryCache;
    }

    /**
     * Sets the context channel, setting up a listener for channel events.
     */
    public void setChannel(DataChannel channel) {
        if (this.channel != channel) {

            if (this.mergeHandler != null) {
                this.mergeHandler.active = false;
                this.mergeHandler = null;
            }

            this.channel = channel;

            EventManager eventManager = (channel != null)
                    ? channel.getEventManager()
                    : null;
            if (eventManager != null) {
                this.mergeHandler = new CayenneContextMergeHandler(this);

                // listen to our channel events...
                // note that we must reset listener on channel switch, as there is no
                // guarantee that a new channel uses the same EventManager.
                EventUtil.listenForChannelEvents(channel, mergeHandler);
            }
        }
    }

    /**
     * Returns true if this context posts individual object modification events. Subject
     * used for these events is <code>ObjectContext.GRAPH_CHANGED_SUBJECT</code>.
     */
    public boolean isChangeEventsEnabled() {
        return graphManager.changeEventsEnabled;
    }

    /**
     * Returns true if this context posts lifecycle events. Subjects used for these events
     * are
     * <code>ObjectContext.GRAPH_COMMIT_STARTED_SUBJECT, ObjectContext.GRAPH_COMMITTED_SUBJECT,
     * ObjectContext.GRAPH_COMMIT_ABORTED_SUBJECT, ObjectContext.GRAPH_ROLLEDBACK_SUBJECT.</code>.
     */
    public boolean isLifecycleEventsEnabled() {
        return graphManager.lifecycleEventsEnabled;
    }

    /**
     * Returns an EntityResolver that provides mapping information needed for
     * CayenneContext operation. If EntityResolver is not set, this method would obtain
     * and cache one from the underlying DataChannel.
     */
    public EntityResolver getEntityResolver() {
        // load entity resolver on demand
        if (entityResolver == null) {
            synchronized (this) {
                if (entityResolver == null) {
                    setEntityResolver(channel.getEntityResolver());
                }
            }
        }

        return entityResolver;
    }

    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    public GraphManager getGraphManager() {
        return graphManager;
    }

    CayenneContextGraphManager internalGraphManager() {
        return graphManager;
    }

    CayenneContextGraphAction internalGraphAction() {
        return graphAction;
    }

    /**
     * Commits changes to uncommitted objects. First checks if there are changes in this
     * context and if any changes are detected, sends a commit message to remote Cayenne
     * service via an internal instance of CayenneConnector.
     */
    public void commitChanges() {
        doCommitChanges(true);
    }

    GraphDiff doCommitChanges(boolean cascade) {

        int syncType = cascade
                ? DataChannel.FLUSH_CASCADE_SYNC
                : DataChannel.FLUSH_NOCASCADE_SYNC;

        GraphDiff commitDiff = null;

        synchronized (graphManager) {

            if (graphManager.hasChanges()) {

                ValidationResult result = new ValidationResult();
                Iterator it = graphManager.dirtyNodes().iterator();
                while (it.hasNext()) {
                    Persistent p = (Persistent) it.next();
                    if (p instanceof Validating) {
                        switch (p.getPersistenceState()) {
                            case PersistenceState.NEW:
                                ((Validating) p).validateForInsert(result);
                                break;
                            case PersistenceState.MODIFIED:
                                ((Validating) p).validateForUpdate(result);
                                break;
                            case PersistenceState.DELETED:
                                ((Validating) p).validateForDelete(result);
                                break;
                        }
                    }
                }

                if (result.hasFailures()) {
                    throw new ValidationException(result);
                }

                graphManager.graphCommitStarted();

                try {
                    commitDiff = channel.onSync(this, graphManager
                            .getDiffsSinceLastFlush(), syncType);
                }
                catch (Throwable th) {
                    graphManager.graphCommitAborted();

                    if (th instanceof CayenneRuntimeException) {
                        throw (CayenneRuntimeException) th;
                    }
                    else {
                        throw new CayenneRuntimeException("Commit error", th);
                    }
                }

                graphManager.graphCommitted(commitDiff);
            }
        }

        return commitDiff;
    }

    public void commitChangesToParent() {
        doCommitChanges(false);
    }

    public void rollbackChanges() {
        synchronized (graphManager) {
            if (graphManager.hasChanges()) {

                GraphDiff diff = graphManager.getDiffs();
                graphManager.graphReverted();

                channel.onSync(this, diff, DataChannel.ROLLBACK_CASCADE_SYNC);
            }
        }
    }

    public void rollbackChangesLocally() {
        synchronized (graphManager) {
            if (graphManager.hasChanges()) {
                graphManager.graphReverted();
            }
        }
    }

    /**
     * Deletes an object locally, scheduling it for future deletion from the external data
     * store.
     */
    public void deleteObject(Object object) {
        new ObjectContextDeleteAction(this).performDelete((Persistent) object);
    }

    /**
     * Creates and registers a new Persistent object instance.
     */
    public <T extends Persistent> T newObject(Class<T> persistentClass) {
        if (persistentClass == null) {
            throw new NullPointerException("Persistent class can't be null.");
        }

        ObjEntity entity = getEntityResolver().lookupObjEntity(persistentClass);
        if (entity == null) {
            throw new CayenneRuntimeException("No entity mapped for class: "
                    + persistentClass);
        }

        ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(
                entity.getName());
        T object = (T) descriptor.createObject();
        registerNewObject(object, entity.getName(), descriptor);
        return object;
    }

    /**
     * @since 3.0
     */
    public void registerNewObject(Object object) {
        if (object == null) {
            throw new NullPointerException("An attempt to register null object.");
        }

        ObjEntity entity = getEntityResolver().lookupObjEntity(object.getClass());
        ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(
                entity.getName());
        registerNewObject((Persistent) object, entity.getName(), descriptor);
    }

    /**
     * Runs a query, returning result as list.
     */
    public List performQuery(Query query) {
        List result = onQuery(this, query).firstList();
        return result != null ? result : new ArrayList(1);
    }

    public QueryResponse performGenericQuery(Query query) {
        return onQuery(this, query);
    }

    // TODO: Andrus, 2/2/2006 - make public once CayenneContext is officially declared to
    // support DataChannel API.
    QueryResponse onQuery(ObjectContext context, Query query) {
        return new CayenneContextQueryAction(this, context, query).execute();
    }

    /**
     * Converts a list of Persistent objects registered in some other ObjectContext to a
     * list of objects local to this ObjectContext.
     * <p>
     * <i>Current limitation: all objects in the source list must be either in COMMITTED
     * or in HOLLOW state.</i>
     * </p>
     */
    public Persistent localObject(ObjectId id, Object prototype) {

        // TODO: Andrus, 1/26/2006 - this implementation is copied verbatim from
        // DataContext. Somehow need to pull out the common code or implement inheritance

        // ****** Copied from DataContext - start *******

        if (id == null) {
            throw new IllegalArgumentException("Null ObjectId");
        }

        ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(
                id.getEntityName());

        synchronized (getGraphManager()) {
            Persistent cachedObject = (Persistent) getGraphManager().getNode(id);

            // merge into an existing object
            if (cachedObject != null) {

                // TODO: Andrus, 1/24/2006 implement smart merge for modified objects...
                if (cachedObject != prototype
                        && cachedObject.getPersistenceState() != PersistenceState.MODIFIED
                        && cachedObject.getPersistenceState() != PersistenceState.DELETED) {

                    if (prototype != null
                            && ((Persistent) prototype).getPersistenceState() != PersistenceState.HOLLOW) {

                        descriptor.shallowMerge(prototype, cachedObject);

                        if (cachedObject.getPersistenceState() == PersistenceState.HOLLOW) {
                            cachedObject.setPersistenceState(PersistenceState.COMMITTED);
                        }
                    }
                }

                return cachedObject;
            }
            // create and merge into a new object
            else {

                // Andrus, 1/26/2006 - note that there is a tricky case of a temporary
                // object
                // passed from peer DataContext... In the past we used to throw an
                // exception
                // or return null. Now that we can have a valid (but generally
                // indistinguishible) case of such object passed from parent, we let it
                // slip... Not sure what's the best way of handling it that does not
                // involve
                // breaking encapsulation of the DataChannel to detect where in the
                // hierarchy
                // this context is.

                Persistent localObject;

                localObject = (Persistent) descriptor.createObject();

                localObject.setObjectContext(this);
                localObject.setObjectId(id);

                getGraphManager().registerNode(id, localObject);

                if (prototype != null) {
                    localObject.setPersistenceState(PersistenceState.COMMITTED);
                    descriptor.shallowMerge(prototype, localObject);
                }
                else {
                    localObject.setPersistenceState(PersistenceState.HOLLOW);
                }

                return localObject;
            }
        }

        // ****** Copied from DataContext - end *******
    }

    public void propertyChanged(
            Persistent object,
            String property,
            Object oldValue,
            Object newValue) {

        if (!isPropertyChangeCallbacksDisabled()) {
            graphAction.handlePropertyChange(object, property, oldValue, newValue);
        }
    }

    public Collection uncommittedObjects() {
        synchronized (graphManager) {
            return graphManager.dirtyNodes();
        }
    }

    public Collection deletedObjects() {
        synchronized (graphManager) {
            return graphManager.dirtyNodes(PersistenceState.DELETED);
        }
    }

    public Collection modifiedObjects() {
        synchronized (graphManager) {
            return graphManager.dirtyNodes(PersistenceState.MODIFIED);
        }
    }

    public Collection newObjects() {
        synchronized (graphManager) {
            return graphManager.dirtyNodes(PersistenceState.NEW);
        }
    }

    // ****** non-public methods ******

    void registerNewObject(
            Persistent object,
            String entityName,
            ClassDescriptor descriptor) {
        ObjectId id = new ObjectId(entityName);

        // must follow this exact order of property initialization per CAY-653, i.e. have
        // the id and the context in place BEFORE setPersistence is called
        object.setObjectId(id);
        object.setObjectContext(this);
        object.setPersistenceState(PersistenceState.NEW);

        synchronized (graphManager) {
            graphManager.registerNode(object.getObjectId(), object);
            graphManager.nodeCreated(object.getObjectId());
        }
    }

    Persistent createFault(ObjectId id) {
        ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(
                id.getEntityName());

        Persistent object;
        synchronized (graphManager) {
            object = (Persistent) descriptor.createObject();

            object.setPersistenceState(PersistenceState.HOLLOW);
            object.setObjectContext(this);
            object.setObjectId(id);

            graphManager.registerNode(id, object);
        }

        return object;
    }

    /**
     * @since 3.0
     */
    boolean isPropertyChangeCallbacksDisabled() {
        return propertyChangeCallbacksDisabled;
    }

    /**
     * @since 3.0
     */
    void setPropertyChangeCallbacksDisabled(boolean propertyChangeCallbacksDisabled) {
        this.propertyChangeCallbacksDisabled = propertyChangeCallbacksDisabled;
    }
}
