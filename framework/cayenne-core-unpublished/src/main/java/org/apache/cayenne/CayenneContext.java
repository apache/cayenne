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

import org.apache.cayenne.access.jdbc.CollectionResultIterator;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.util.EventUtil;
import org.apache.cayenne.validation.ValidationException;
import org.apache.cayenne.validation.ValidationResult;

/**
 * A default generic implementation of ObjectContext suitable for accessing
 * Cayenne from either an ORM or a client tiers. Communicates with Cayenne via a
 * {@link org.apache.cayenne.DataChannel}.
 * 
 * @since 1.2
 */
public class CayenneContext extends BaseContext {

    CayenneContextGraphManager graphManager;

    // object that merges "backdoor" changes that come from the channel.
    CayenneContextMergeHandler mergeHandler;

    /**
     * Creates a new CayenneContext with no channel and disabled graph events.
     */
    public CayenneContext() {
        this(null);
    }

    /**
     * Creates a new CayenneContext, initializing it with a channel instance.
     * CayenneContext created using this constructor WILL NOT broadcast graph
     * change events.
     */
    public CayenneContext(DataChannel channel) {
        this(channel, false, false);
    }

    /**
     * Creates a new CayenneContext, initializing it with a channel.
     */
    public CayenneContext(DataChannel channel, boolean changeEventsEnabled, boolean lifecyleEventsEnabled) {

        graphManager = new CayenneContextGraphManager(this, changeEventsEnabled, lifecyleEventsEnabled);

        if (channel != null) {
            attachToChannel(channel);
        }
    }

    /**
     * @since 3.1
     */
    @Override
    protected void attachToChannel(DataChannel channel) {
        super.attachToChannel(channel);

        if (mergeHandler != null) {
            mergeHandler.active = false;
            mergeHandler = null;
        }

        EventManager eventManager = channel.getEventManager();
        if (eventManager != null) {
            mergeHandler = new CayenneContextMergeHandler(this);

            // listen to our channel events...
            // note that we must reset listener on channel switch, as there is
            // no
            // guarantee that a new channel uses the same EventManager.
            EventUtil.listenForChannelEvents(channel, mergeHandler);
        }
    }

    /**
     * Returns true if this context posts individual object modification events.
     * Subject used for these events is
     * <code>ObjectContext.GRAPH_CHANGED_SUBJECT</code>.
     */
    public boolean isChangeEventsEnabled() {
        return graphManager.changeEventsEnabled;
    }

    /**
     * Returns true if this context posts lifecycle events. Subjects used for
     * these events are
     * <code>ObjectContext.GRAPH_COMMIT_STARTED_SUBJECT, ObjectContext.GRAPH_COMMITTED_SUBJECT,
     * ObjectContext.GRAPH_COMMIT_ABORTED_SUBJECT, ObjectContext.GRAPH_ROLLEDBACK_SUBJECT.</code>
     * .
     */
    public boolean isLifecycleEventsEnabled() {
        return graphManager.lifecycleEventsEnabled;
    }

    @Override
    public GraphManager getGraphManager() {
        return graphManager;
    }

    CayenneContextGraphManager internalGraphManager() {
        return graphManager;
    }

    /**
     * Commits changes to uncommitted objects. First checks if there are changes
     * in this context and if any changes are detected, sends a commit message
     * to remote Cayenne service via an internal instance of CayenneConnector.
     */
    @Override
    public void commitChanges() {
        doCommitChanges(true);
    }

    GraphDiff doCommitChanges(boolean cascade) {

        int syncType = cascade ? DataChannel.FLUSH_CASCADE_SYNC : DataChannel.FLUSH_NOCASCADE_SYNC;

        GraphDiff commitDiff = null;

        synchronized (graphManager) {

            if (graphManager.hasChanges()) {

                if (isValidatingObjectsOnCommit()) {
                    ValidationResult result = new ValidationResult();
                    Iterator<?> it = graphManager.dirtyNodes().iterator();
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
                }

                graphManager.graphCommitStarted();

                GraphDiff changes = graphManager.getDiffsSinceLastFlush();

                try {
                    commitDiff = channel.onSync(this, changes, syncType);
                } catch (Throwable th) {
                    graphManager.graphCommitAborted();

                    if (th instanceof CayenneRuntimeException) {
                        throw (CayenneRuntimeException) th;
                    } else {
                        throw new CayenneRuntimeException("Commit error", th);
                    }
                }

                graphManager.graphCommitted(commitDiff);

                // this event is caught by peer nested ObjectContexts to
                // synchronize the
                // state
                fireDataChannelCommitted(this, changes);
            }
        }

        return commitDiff;
    }

    @Override
    public void commitChangesToParent() {
        doCommitChanges(false);
    }

    @Override
    public void rollbackChanges() {
        synchronized (graphManager) {
            if (graphManager.hasChanges()) {

                GraphDiff diff = graphManager.getDiffs();
                graphManager.graphReverted();

                channel.onSync(this, diff, DataChannel.ROLLBACK_CASCADE_SYNC);
                fireDataChannelRolledback(this, diff);
            }
        }
    }

    @Override
    public void rollbackChangesLocally() {
        synchronized (graphManager) {
            if (graphManager.hasChanges()) {
                GraphDiff diff = graphManager.getDiffs();
                graphManager.graphReverted();

                fireDataChannelRolledback(this, diff);
            }
        }
    }

    /**
     * Creates and registers a new Persistent object instance.
     */
    @Override
    public <T> T newObject(Class<T> persistentClass) {
        if (persistentClass == null) {
            throw new NullPointerException("Persistent class can't be null.");
        }

        ObjEntity entity = getEntityResolver().getObjEntity(persistentClass);
        if (entity == null) {
            throw new CayenneRuntimeException("No entity mapped for class: " + persistentClass);
        }

        ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(entity.getName());
        T object = (T) descriptor.createObject();
        descriptor.injectValueHolders(object);
        registerNewObject((Persistent) object, entity.getName(), descriptor);
        return object;
    }

    /**
     * @since 3.0
     */
    @Override
    public void registerNewObject(Object object) {
        if (object == null) {
            throw new NullPointerException("An attempt to register null object.");
        }

        ObjEntity entity = getEntityResolver().getObjEntity(object.getClass());
        ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(entity.getName());
        registerNewObject((Persistent) object, entity.getName(), descriptor);
    }

    /**
     * Runs a query, returning result as list.
     */
    @Override
    public List performQuery(Query query) {
        List result = onQuery(this, query).firstList();
        return result != null ? result : new ArrayList<Object>(1);
    }

    @Override
    public QueryResponse performGenericQuery(Query query) {
        return onQuery(this, query);
    }

    public QueryResponse onQuery(ObjectContext context, Query query) {
        return new CayenneContextQueryAction(this, context, query).execute();
    }

    @Override
    public Collection<?> uncommittedObjects() {
        synchronized (graphManager) {
            return graphManager.dirtyNodes();
        }
    }

    @Override
    public Collection<?> deletedObjects() {
        synchronized (graphManager) {
            return graphManager.dirtyNodes(PersistenceState.DELETED);
        }
    }

    @Override
    public Collection<?> modifiedObjects() {
        synchronized (graphManager) {
            return graphManager.dirtyNodes(PersistenceState.MODIFIED);
        }
    }

    @Override
    public Collection<?> newObjects() {
        synchronized (graphManager) {
            return graphManager.dirtyNodes(PersistenceState.NEW);
        }
    }

    // ****** non-public methods ******

    void registerNewObject(Persistent object, String entityName, ClassDescriptor descriptor) {
        /**
         * We should create new id only if it is not set for this object. It
         * could have been created, for instance, in child context
         */
        ObjectId id = object.getObjectId();
        if (id == null) {
            id = new ObjectId(entityName);
            object.setObjectId(id);
        }

        injectInitialValue(object);
    }

    Persistent createFault(ObjectId id) {
        ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(id.getEntityName());

        Persistent object = (Persistent) descriptor.createObject();

        object.setPersistenceState(PersistenceState.HOLLOW);
        object.setObjectContext(this);
        object.setObjectId(id);

        graphManager.registerNode(id, object);

        return object;
    }

    @Override
    protected GraphDiff onContextFlush(ObjectContext originatingContext, GraphDiff changes, boolean cascade) {

        boolean childContext = this != originatingContext && changes != null;

        if (childContext) {

            // PropertyChangeProcessingStrategy oldStrategy =
            // getPropertyChangeProcessingStrategy();
            // setPropertyChangeProcessingStrategy(PropertyChangeProcessingStrategy.RECORD);
            try {
                changes.apply(new CayenneContextChildDiffLoader(this));
            } finally {
                // setPropertyChangeProcessingStrategy(oldStrategy);
            }

            fireDataChannelChanged(originatingContext, changes);
        }

        return (cascade) ? doCommitChanges(true) : new CompoundDiff();
    }

    /**
     * Returns <code>true</code> if there are any modified, deleted or new
     * objects registered with this CayenneContext, <code>false</code>
     * otherwise.
     */
    public boolean hasChanges() {
        return graphManager.hasChanges();
    }

    /**
     * This method simply returns an iterator over a list of selected objects.
     * There's no performance benefit of using it vs. regular "select".
     * 
     * @since 3.2
     */
    public <T> ResultIterator<T> iterator(org.apache.cayenne.query.Select<T> query) {
        List<T> objects = select(query);
        return new CollectionResultIterator<T>(objects);
    }

}
