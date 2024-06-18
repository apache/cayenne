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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.DeleteDenyException;
import org.apache.cayenne.FaultFailureException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.ResultIteratorCallback;
import org.apache.cayenne.cache.NestedQueryCache;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.ChildDiffLoader;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.GraphEvent;
import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.*;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyDescriptor;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.tx.TransactionFactory;
import org.apache.cayenne.util.EventUtil;
import org.apache.cayenne.util.GenericResponse;
import org.apache.cayenne.util.ObjectContextGraphAction;
import org.apache.cayenne.util.Util;

/**
 * The most common implementation of {@link ObjectContext}. DataContext is an
 * isolated container of an object graph, in a sense that any uncommitted
 * changes to persistent objects that are registered with the context, are not
 * visible to the users of other contexts.
 */
public class DataContext implements ObjectContext {

    /**
     * A holder of a ObjectContext bound to the current thread.
     *
     * @since 3.0
     */
    protected static final ThreadLocal<ObjectContext> threadObjectContext = new ThreadLocal<ObjectContext>();

    /**
     * Returns the ObjectContext bound to the current thread.
     *
     * @since 3.0
     * @return the ObjectContext associated with caller thread.
     * @throws IllegalStateException
     *             if there is no ObjectContext bound to the current thread.
     */
    public static ObjectContext getThreadObjectContext() throws IllegalStateException {
        ObjectContext context = threadObjectContext.get();
        if (context == null) {
            throw new IllegalStateException("Current thread has no bound ObjectContext.");
        }

        return context;
    }

    /**
     * Binds a ObjectContext to the current thread. ObjectContext can later be
     * retrieved by users in the same thread by calling
     * {@link #getThreadObjectContext}. Using null parameter will
     * unbind currently bound ObjectContext.
     *
     * @since 3.0
     */
    public static void bindThreadObjectContext(ObjectContext context) {
        threadObjectContext.set(context);
    }


    private DataContextDelegate delegate;
    protected boolean usingSharedSnapshotCache;
    protected ObjectStore objectStore;

    /**
     * Graph action that handles property changes
     *
     * @since 3.1
     */
    protected ObjectContextGraphAction graphAction;

    /**
     * Stores user defined properties associated with this DataContext.
     *
     * @since 3.0
     */
    protected volatile Map<String, Object> userProperties;

    // transient variables that should be reinitialized on deserialization from the registry
    protected transient DataChannel channel;
    protected transient QueryCache queryCache;
    protected transient EntityResolver entityResolver;

    /**
     * @deprecated since 4.0 used in a method that itself should be deprecated,
     *             so this is a temp code
     */
    @Deprecated
    protected transient TransactionFactory transactionFactory;

    protected transient DataContextMergeHandler mergeHandler;

    protected boolean validatingObjectsOnCommit = true;

    protected transient DataContextObjectCreator objectCreator;

    /**
     * Creates a new DataContext that is not attached to the Cayenne stack.
     */
    public DataContext() {
        this(null, null);
    }

    /**
     * Creates a new DataContext with parent DataChannel and ObjectStore.
     * 
     * @since 1.2
     */
    public DataContext(DataChannel channel, ObjectStore objectStore) {

        graphAction = new ObjectContextGraphAction(this);
        objectCreator = new DataContextObjectCreator(this);

        // inject self as parent context
        if (objectStore != null) {
            this.objectStore = objectStore;
            objectStore.setContext(this);
        }

        if (channel != null) {
            attachToChannel(channel);
        }

        if (objectStore != null) {
            DataDomain domain = getParentDataDomain();
            this.usingSharedSnapshotCache = domain != null
                    && objectStore.getDataRowCache() == domain.getSharedSnapshotCache();
        }
    }

    /**
     * Checks whether this context is attached to Cayenne runtime stack and if
     * not, attempts to attach itself to the runtime using Injector returned
     * from the call to {@link CayenneRuntime#getThreadInjector()}. If thread
     * Injector is not available and the context is not attached, throws
     * CayenneRuntimeException.
     * <p>
     * This method is called internally by the context before access to
     * transient variables to allow the context to attach to the stack lazily
     * following deserialization.
     *
     * @return true if the context successfully attached to the thread runtime,
     *         false - if it was already attached.
     * @since 3.1
     */
    protected boolean attachToRuntimeIfNeeded() {
        if (channel != null) {
            return false;
        }

        Injector injector = CayenneRuntime.getThreadInjector();
        if (injector == null) {
            throw new CayenneRuntimeException("Can't attach to Cayenne runtime. "
                    + "Null injector returned from CayenneRuntime.getThreadInjector()");
        }

        attachToRuntime(injector);
        return true;
    }

    /**
     * Attaches this context to the CayenneRuntime whose Injector is passed as
     * an argument to this method.
     *
     * @since 3.1
     */
    protected void attachToRuntime(Injector injector) {
        attachToChannel(injector.getInstance(DataChannel.class));
        setQueryCache(new NestedQueryCache(injector.getInstance(QueryCache.class)));
        this.transactionFactory = injector.getInstance(TransactionFactory.class);
    }

    /**
     * Attaches to a provided DataChannel.
     *
     * @since 3.1
     */
    protected void attachToChannel(DataChannel channel) {

        if (channel == null) {
            throw new NullPointerException("Null channel");
        }

        setChannel(channel);
        setEntityResolver(channel.getEntityResolver());

        if (mergeHandler != null) {
            mergeHandler.setActive(false);
            mergeHandler = null;
        }

        EventManager eventManager = channel.getEventManager();

        if (eventManager != null) {
            mergeHandler = new DataContextMergeHandler(this);

            // listen to our channel events...
            // note that we must reset listener on channel switch, as there is
            // no
            // guarantee that a new channel uses the same EventManager.
            EventUtil.listenForChannelEvents(channel, mergeHandler);
        }

        if (!usingSharedSnapshotCache && getObjectStore() != null) {
            DataRowStore cache = getObjectStore().getDataRowCache();

            if (cache != null) {
                cache.setEventManager(eventManager);
            }
        }
    }

    @Override
    public DataChannel getChannel() {
        attachToRuntimeIfNeeded();
        return channel;
    }

    /**
     * Sets a new DataChannel for this context.
     *
     * @since 3.1
     */
    public void setChannel(DataChannel channel) {
        this.channel = channel;
    }

    @Override
    public EntityResolver getEntityResolver() {
        attachToRuntimeIfNeeded();
        return entityResolver;
    }

    /**
     * @since 3.1
     */
    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }


    /**
     * Returns a DataDomain used by this DataContext. DataDomain is looked up in
     * the DataChannel hierarchy. If a channel is not a DataDomain or a
     * DataContext, null is returned.
     * 
     * @return DataDomain that is a direct or indirect parent of this
     *         DataContext in the DataChannel hierarchy.
     * @since 1.1
     */
    public DataDomain getParentDataDomain() {
        attachToRuntimeIfNeeded();

        if (channel == null) {
            return null;
        }

        if (channel instanceof DataDomain) {
            return (DataDomain) channel;
        }

        List<?> response = channel.onQuery(this, new DataDomainQuery()).firstList();

        if (response != null && response.size() > 0 && response.get(0) instanceof DataDomain) {
            return (DataDomain) response.get(0);
        }

        return null;
    }

    /**
     * Sets a DataContextDelegate for this context. Delegate is notified of
     * certain events in the DataContext lifecycle and can customize DataContext
     * behavior.
     * 
     * @since 1.1
     */
    public void setDelegate(DataContextDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns a delegate currently associated with this DataContext.
     * 
     * @since 1.1
     */
    public DataContextDelegate getDelegate() {
        return delegate;
    }

    /**
     * @return a delegate instance if it is initialized, or a shared noop
     *         implementation the context has no delegate. Useful to prevent
     *         extra null checks and conditional logic in the code.
     * @since 1.1
     */
    DataContextDelegate nonNullDelegate() {
        return (delegate != null) ? delegate : NoopDelegate.noopDelegate;
    }

    /**
     * Returns ObjectStore associated with this DataContext.
     */
    public ObjectStore getObjectStore() {
        return objectStore;
    }

    /**
     * Returns <code>true</code> if there are any modified, deleted or new
     * objects registered with this DataContext, <code>false</code> otherwise.
     */
    public boolean hasChanges() {
        return getObjectStore().hasChanges();
    }

    /**
     * Returns a list of objects that are registered with this DataContext and
     * have a state PersistenceState.NEW
     */
    @Override
    public Collection<?> newObjects() {
        return getObjectStore().objectsInState(PersistenceState.NEW);
    }

    /**
     * Returns a list of objects that are registered with this DataContext and
     * have a state {@link PersistenceState#DELETED}
     */
    @Override
    public Collection<?> deletedObjects() {
        return getObjectStore().objectsInState(PersistenceState.DELETED);
    }

    @Override
    public void deleteObject(Object object) throws DeleteDenyException {
        deleteObjects(object);
    }

    /**
     * @since 3.1
     */
    @Override
    public <T> void deleteObjects(T... objects) throws DeleteDenyException {
        if (objects == null || objects.length == 0) {
            return;
        }

        DataContextDeleteAction action = new DataContextDeleteAction(this);

        for (Object object : objects) {
            action.performDelete((Persistent) object);
        }
    }

    @Override
    public void deleteObjects(Collection<?> objects) throws DeleteDenyException {
        if (objects.isEmpty()) {
            return;
        }

        DataContextDeleteAction action = new DataContextDeleteAction(this);

        // Make a copy to iterate over to avoid ConcurrentModificationException
        for (Object object : List.copyOf(objects)) {
            action.performDelete((Persistent) object);
        }
    }

    /**
     * Returns a list of objects that are registered with this DataContext and
     * have a state {@link PersistenceState#MODIFIED}
     */
    @Override
    public Collection<?> modifiedObjects() {
        return getObjectStore().objectsInState(PersistenceState.MODIFIED);
    }

    /**
     * Returns a collection of all uncommitted registered objects.
     * 
     * @since 1.2
     */
    @Override
    public Collection<?> uncommittedObjects() {

        int len = getObjectStore().registeredObjectsCount();
        if (len == 0) {
            return Collections.EMPTY_LIST;
        }

        // guess target collection size
        Collection<Object> objects = new ArrayList<>(len > 100 ? len / 2 : len);
        Iterator<?> it = getObjectStore().getObjectIterator();
        while (it.hasNext()) {
            Persistent object = (Persistent) it.next();
            int state = object.getPersistenceState();
            if (state == PersistenceState.MODIFIED || state == PersistenceState.NEW
                    || state == PersistenceState.DELETED) {

                objects.add(object);
            }
        }

        return objects;
    }

    public QueryCache getQueryCache() {
        attachToRuntimeIfNeeded();
        return queryCache;
    }

    /**
     * Sets a QueryCache to be used for storing cached query results.
     */
    public void setQueryCache(QueryCache queryCache) {
        this.queryCache = queryCache;
    }

    /**
     * Returns EventManager associated with the ObjectStore.
     *
     * @since 1.2
     */
    @Override
    public EventManager getEventManager() {
        return channel != null ? channel.getEventManager() : null;
    }

    /**
     * @since 1.2
     */
    protected void fireDataChannelCommitted(Object postedBy, GraphDiff changes) {
        EventManager manager = getEventManager();

        if (manager != null) {
            GraphEvent e = new GraphEvent(this, postedBy, changes);
            manager.postEvent(e, DataChannel.GRAPH_FLUSHED_SUBJECT);
        }
    }

    /**
     * @since 1.2
     */
    protected void fireDataChannelRolledback(Object postedBy, GraphDiff changes) {
        EventManager manager = getEventManager();

        if (manager != null) {
            GraphEvent e = new GraphEvent(this, postedBy, changes);
            manager.postEvent(e, DataChannel.GRAPH_ROLLEDBACK_SUBJECT);
        }
    }

    /**
     * @since 3.1
     */
    @Override
    public <T extends Persistent> T localObject(T objectFromAnotherContext) {

        if (objectFromAnotherContext == null) {
            throw new NullPointerException("Null object argument");
        }

        ObjectId id = objectFromAnotherContext.getObjectId();

        // first look for the ID in the local GraphManager
        synchronized (getGraphManager()) {
            @SuppressWarnings("unchecked")
            T localObject = (T) getGraphManager().getNode(id);
            if (localObject != null) {
                return localObject;
            }

            // create a hollow object, optimistically assuming that the ID we got from
            // 'objectFromAnotherContext' is a valid ID either in the parent context or in the DB.
            // This essentially defers possible FaultFailureExceptions.

            ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(id.getEntityName());
            @SuppressWarnings("unchecked")
            T persistent = (T) descriptor.createObject();

            persistent.setObjectContext(this);
            persistent.setObjectId(id);
            persistent.setPersistenceState(PersistenceState.HOLLOW);

            getGraphManager().registerNode(id, persistent);

            return persistent;
        }
    }

    /**
     * Returns a {@link DataRow} reflecting current, possibly uncommitted, object state.
     * <p>
     * <strong>Warning:</strong> This method will return a partial snapshot if
     * an object or one of its related objects that propagate their keys to this
     * object have temporary ids. DO NOT USE this method if you expect a DataRow
     * to represent a complete object state.
     * </p>
     *
     * @param object persistent object to create snapshot for
     * @return current snapshot of the persistent object
     * @since 1.1
     */
    public DataRow currentSnapshot(final Persistent object) {
        return new DataContextSnapshotBuilder(getEntityResolver(), getObjectStore(), object)
                .build();
    }

    @Override
    public void prepareForAccess(Persistent object, String property, boolean lazyFaulting) {
        if (object.getPersistenceState() == PersistenceState.HOLLOW) {
            ObjectId oid = object.getObjectId();
            List<?> objects = performQuery(new ObjectIdQuery(oid, false, ObjectIdQuery.CACHE));
            if (objects.size() == 0) {
                throw new FaultFailureException(
                        "Error resolving fault, no matching row exists in the database for ObjectId: " + oid);
            } else if (objects.size() > 1) {
                throw new FaultFailureException(
                        "Error resolving fault, more than one row exists in the database for ObjectId: " + oid);
            }
            // here once was a sanity check for the COMMITTED state, that was faulty due to the race condition
        }

        // resolve relationship fault
        if (lazyFaulting && property != null) {
            ClassDescriptor classDescriptor = getEntityResolver().getClassDescriptor(
                    object.getObjectId().getEntityName());
            PropertyDescriptor propertyDescriptor = classDescriptor.getProperty(property);

            // If we don't have a property descriptor, there's not much we can do.
            // Let the caller know that the specified property could not be found and list
            // all the properties that could be so the caller knows what can be used.
            if (propertyDescriptor == null) {
                List<String> properties = new CollectingNamePropertyVisitor().allProperties(classDescriptor);
                String errorMessage = String.format("Property '%s' is not declared for entity '%s'.",
                        property, object.getObjectId().getEntityName()) +
                        " Declared properties are: '" + String.join("', '", properties) + "'.";
                throw new CayenneRuntimeException(errorMessage);
            }

            // this should trigger fault resolving
            propertyDescriptor.readProperty(object);
        }
    }

    /**
     * Converts a list of DataRows to a List of Persistent registered with this
     * DataContext.
     * 
     * @since 3.0
     */
    public List objectsFromDataRows(ClassDescriptor descriptor, List<? extends DataRow> dataRows) {
        // TODO: If data row cache is not available it means that current data context is child.
        //       We need to redirect this method call to parent data context as an internal query.
        //       It is not obvious and has some overhead. Redesign for nested contexts should be done.
        if (getObjectStore().getDataRowCache() == null) {
            return objectsFromDataRowsFromParentContext(descriptor, dataRows);
        }
        return new ObjectResolver(this, descriptor, true).synchronizedObjectsFromDataRows(dataRows);
    }

    private List <?> objectsFromDataRowsFromParentContext(ClassDescriptor descriptor, List<? extends DataRow> dataRows) {
        return getChannel().onQuery(this, new ObjectsFromDataRowsQuery(descriptor, dataRows)).firstList();
    }

    /**
     * Creates a Persistent from DataRow.
     * 
     * @see DataRow
     * @since 3.1
     */
    public <T extends Persistent> T objectFromDataRow(Class<T> objectClass, DataRow dataRow) {
        ObjEntity entity = this.getEntityResolver().getObjEntity(objectClass);

        if (entity == null) {
            throw new CayenneRuntimeException("Unmapped Java class: %s", objectClass);
        }

        ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(entity.getName());
        List<T> list = objectsFromDataRows(descriptor, Collections.singletonList(dataRow));
        return list.get(0);
    }

    /**
     * Creates a Persistent from DataRow. This variety of the
     * 'objectFromDataRow' method is normally used for generic classes.
     * 
     * @see DataRow
     * @since 3.1
     * @since 5.0 returns {@link Persistent} instead of the deprecated DataObject
     */
    public Persistent objectFromDataRow(String entityName, DataRow dataRow) {
        ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(entityName);
        List<?> list = objectsFromDataRows(descriptor, Collections.singletonList(dataRow));

        return (Persistent) list.get(0);
    }

    /**
     * Creates and registers a new persistent object.
     * 
     * @since 1.2
     */
    @Override
    public <T> T newObject(Class<T> persistentClass) {
        return objectCreator.newObject(persistentClass);
    }

    /**
     * Instantiates a new object and registers it with this context. Object
     * class is determined from the mapped entity. Object class must have a
     * default constructor.
     * <p>
     * <i>Note: in most cases {@link #newObject(Class)} method should be used,
     * however this method is helpful when generic persistent classes are
     * used.</i>
     * 
     * @since 3.0
     */
    public Persistent newObject(String entityName) {
        return objectCreator.newObject(entityName);
    }

    /**
     * Registers a transient object with the context, recursively registering
     * all transient persistent objects attached to this object via
     * relationships.
     * <p>
     * <i>Note that since 3.0 this method takes Object as an argument instead of a {@link Persistent}.</i>
     * 
     * @param object
     *            new object that needs to be made persistent.
     */
    @Override
    public void registerNewObject(Object object) {
        objectCreator.registerNewObject(object);
    }

    /**
     * Unregisters a Collection of Persistent objects from the DataContext and the underlying ObjectStore.
     * This operation also unsets DataContext for each object and changes its state to {@link PersistenceState#TRANSIENT}
     * 
     * @see #invalidateObjects(Collection)
     */
    public void unregisterObjects(Collection<?> objects) {
        getObjectStore().objectsUnregistered(objects);
    }

    @Override
    public void invalidateObjects(Collection<?> objects) {

        // don't allow null collections as a matter of coding discipline
        if (objects == null) {
            throw new NullPointerException("Null collection of objects to invalidate");
        }

        if (!objects.isEmpty()) {
            performGenericQuery(new RefreshQuery(objects));
        }
    }

    /**
     * @since 3.1
     */
    @Override
    public <T> void invalidateObjects(T... objects) {
        if (objects != null && objects.length > 0) {
            performGenericQuery(new RefreshQuery(Arrays.asList(objects)));
        }
    }

    @Override
    public void propertyChanged(Persistent object, String property, Object oldValue, Object newValue) {
        graphAction.handlePropertyChange(object, property, oldValue, newValue);
    }

    /**
     * If the parent channel is a DataContext, reverts local changes to make
     * this context look like the parent, if the parent channel is a DataDomain,
     * reverts all changes.
     * 
     * @since 1.2
     */
    @Override
    public void rollbackChangesLocally() {
        if (objectStore.hasChanges()) {
            GraphDiff diff = getObjectStore().getChanges();

            getObjectStore().objectsRolledBack();
            fireDataChannelRolledback(this, diff);
        }
    }

    /**
     * Reverts any changes that have occurred to objects registered with
     * DataContext; also performs cascading rollback of all parent DataContexts.
     */
    @Override
    public void rollbackChanges() {

        if (objectStore.hasChanges()) {
            GraphDiff diff = getObjectStore().getChanges();

            // call channel with changes BEFORE reverting them, so that any interceptors could record them
            if (channel != null) {
                channel.onSync(this, diff, DataChannel.ROLLBACK_CASCADE_SYNC);
            }

            getObjectStore().objectsRolledBack();
            fireDataChannelRolledback(this, diff);
        } else {
            if (channel != null) {
                channel.onSync(this, new CompoundDiff(), DataChannel.ROLLBACK_CASCADE_SYNC);
            }
        }

    }

    /**
     * "Flushes" the changes to the parent {@link DataChannel}. If the parent
     * channel is a DataContext, it updates its objects with this context's
     * changes, without a database update. If it is a DataDomain (the most
     * common case), the changes are written to the database. To cause cascading
     * commit all the way to the database, one must use {@link #commitChanges()} .
     * 
     * @since 1.2
     * @see #commitChanges()
     */
    @Override
    public void commitChangesToParent() {
        flushToParent(false);
    }

    /**
     * Synchronizes object graph with the database. Executes needed insert,
     * update and delete queries (generated internally).
     */
    @Override
    public void commitChanges() throws CayenneRuntimeException {
        flushToParent(true);
    }

    protected GraphDiff onContextFlush(ObjectContext originatingContext, GraphDiff changes, boolean cascade) {

        boolean childContext = this != originatingContext && changes != null;

        try {
            if (childContext) {
                getObjectStore().childContextSyncStarted();
                changes.apply(new ChildDiffLoader(this));
                fireDataChannelChanged(originatingContext, changes);
            }

            return (cascade) ? flushToParent(true) : new CompoundDiff();
        } finally {
            if (childContext) {
                getObjectStore().childContextSyncStopped();
            }
        }
    }

    /**
     * Synchronizes with the parent channel, performing a flush or a commit.
     * 
     * @since 1.2
     */
    GraphDiff flushToParent(boolean cascade) {

        if (this.getChannel() == null) {
            throw new CayenneRuntimeException("Cannot commit changes - channel is not set.");
        }

        int syncType = cascade ? DataChannel.FLUSH_CASCADE_SYNC : DataChannel.FLUSH_NOCASCADE_SYNC;

        ObjectStore objectStore = getObjectStore();
        GraphDiff parentChanges = null;

        // prevent multiple commits occurring simultaneously
        synchronized (objectStore) {

            ObjectStoreGraphDiff changes = objectStore.getChanges();
            boolean noop = isValidatingObjectsOnCommit() ? changes.validateAndCheckNoop() : changes.isNoop();

            if (noop) {
                // need to clear phantom changes
                objectStore.postprocessAfterPhantomCommit();
            } else {

                try {
                    parentChanges = getChannel().onSync(this, changes, syncType);

                    // note that this is a hack resulting from a fix to CAY-766...
                    // To support valid object state in PostPersist callback,
                    // 'postprocessAfterCommit' is invoked by DataDomain.onSync(..).
                    // Unless the parent is DataContext, and this method is not invoked!! As a result, PostPersist
                    // will contain temp ObjectIds in nested contexts and perm ones in flat contexts.
                    // Pending better callback design .....
                    if (objectStore.hasChanges()) {
                        objectStore.postprocessAfterCommit(parentChanges);
                    }

                    // this event is caught by peer nested DataContexts to synchronize the state
                    fireDataChannelCommitted(this, changes);
                }
                // "catch" is needed to unwrap OptimisticLockExceptions
                catch (CayenneRuntimeException ex) {
                    Throwable unwound = Util.unwindException(ex);

                    if (unwound instanceof CayenneRuntimeException) {
                        throw (CayenneRuntimeException) unwound;
                    } else {
                        throw new CayenneRuntimeException("Commit Exception", unwound);
                    }
                }
            }

            // merge changes from parent as well as changes caused by lifecycle event callbacks/listeners...
            CompoundDiff diff = new CompoundDiff();

            diff.addAll(objectStore.getLifecycleEventInducedChanges());
            if (parentChanges != null) {
                diff.add(parentChanges);
            }

            // this event is caught by child DataContexts to update temporary ObjectIds with permanent
            if (!diff.isNoop()) {
                fireDataChannelCommitted(getChannel(), diff);
            }

            return diff;
        }

    }

    /**
     * @since 4.0
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> select(Select<T> query) {
        return performQuery(query);
    }

    /**
     * @since 4.0
     */
    @Override
    public <T> T selectOne(Select<T> query) {
        List<T> objects = select(query);

        if (objects.size() == 0) {
            return null;
        } else if (objects.size() > 1) {
            throw new CayenneRuntimeException("Expected zero or one object, instead query matched: %d", objects.size());
        }

        return objects.get(0);
    }

    /**
     * @since 4.0
     */
    @Override
    public <T> T selectFirst(Select<T> query) {
        List<T> objects = select(query);

        return (objects == null || objects.isEmpty()) ? null : objects.get(0);
    }

    /**
     * @since 4.0
     */
    @Override
    public <T> void iterate(Select<T> query, ResultIteratorCallback<T> callback) {

        try (ResultIterator<T> it = iterator(query);) {
            for (T t : it) {
                callback.next(t);
            }
        }
    }

    /**
     * Performs a single database select query returning result as a {@link ResultIterator}.
     * <p>
     * It is caller's responsibility to explicitly close the {@link ResultIterator}.
     * A failure to do so will result in a <b>database connection not being released</b>.
     * Another side effect of an open {@link ResultIterator} is that an internal Cayenne transaction
     * that originated in this method stays open until the iterator is closed.
     * So users should normally close the iterator within the same thread that opened it.
     * <p>
     */
    @Override
    public <T> ResultIterator<T> iterator(final Select<T> query) {
        return performIteratedQueryInternal(query, false);
    }

    /**
     * Performs a single database select query returning result as a {@link ResultIterator}.
     * <p>
     * It is caller's responsibility to explicitly close the {@link ResultIterator}.
     * A failure to do so will result in a <b>database connection not being released</b>.
     * Another side effect of an open {@link ResultIterator} is that an internal Cayenne transaction
     * that originated in this method stays open until the iterator is closed.
     * So users should normally close the iterator within the same thread that opened it.
     * <p>
     * Note that {@code performIteratedQuery} always returns {@link ResultIterator} over DataRows.
     * <p>
     * Use {@link #iterate(Select, org.apache.cayenne.ResultIteratorCallback)} to get access to objects.
     */
    @SuppressWarnings({ "rawtypes" })
    public ResultIterator performIteratedQuery(Query query) {
        return performIteratedQueryInternal(query, true);
    }

    @SuppressWarnings("unchecked")
    private <T> ResultIterator<T> performIteratedQueryInternal(Query query, boolean fetchDataRows) {
        IteratedQueryDecorator queryDecorator = new IteratedQueryDecorator(query, fetchDataRows);
        Query queryToRun = nonNullDelegate().willPerformQuery(this, queryDecorator);
        QueryResponse queryResponse = onQuery(this, queryToRun);
        return (ResultIterator<T>)queryResponse.firstIterator();
    }

    /**
     * Executes a query returning a generic response.
     * 
     * @since 1.2
     */
    @Override
    public QueryResponse performGenericQuery(Query query) {

        query = nonNullDelegate().willPerformGenericQuery(this, query);
        if (query == null) {
            return new GenericResponse();
        }

        if (this.getChannel() == null) {
            throw new CayenneRuntimeException("Can't run query - parent DataChannel is not set.");
        }

        return onQuery(this, query);
    }

    /**
     * Performs a single selecting query. Various query setting control the
     * behavior of this method and the results returned:
     * <ul>
     * <li>Query caching policy defines whether the results are retrieved from
     * cache or fetched from the database. Note that queries that use caching
     * must have a name that is used as a caching key.</li>
     * <li>Query refreshing policy controls whether to refresh existing data
     * objects and ignore any cached values.</li>
     * <li>Query data rows policy defines whether the result should be returned
     * as Persistent objects or DataRows.</li>
     * </ul>
     * <p>
     * <i>Since 1.2 takes any Query parameter, not just GenericSelectQuery</i>
     * </p>
     * 
     * @return A list of Persistent objects or a DataRows, depending on the value
     *         returned by {@link QueryMetadata#isFetchingDataRows()}.
     *         Сan also return an iterator if the query is an instance of iteratedQuery.
     */
    @Override
    public List performQuery(Query query) {
        query = nonNullDelegate().willPerformQuery(this, query);
        if (query == null) {
            return new ArrayList<>(1);
        }

        List<?> result = onQuery(this, query).firstList();
        return result != null ? result : new ArrayList<>(1);
    }

    /**
     * An implementation of a {@link DataChannel} method that is used by child
     * contexts to execute queries. Not intended for direct use.
     * 
     * @since 1.2
     */
    public QueryResponse onQuery(ObjectContext context, Query query) {
        return new DataContextQueryAction(this, context, query).execute();
    }

    @Override
    public GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes, int syncType) {
        switch (syncType) {
            case DataChannel.ROLLBACK_CASCADE_SYNC:
                return onContextRollback();
            case DataChannel.FLUSH_NOCASCADE_SYNC:
                return onContextFlush(originatingContext, changes, false);
            case DataChannel.FLUSH_CASCADE_SYNC:
                return onContextFlush(originatingContext, changes, true);
            default:
                throw new CayenneRuntimeException("Unrecognized SyncMessage type: %d", syncType);
        }
    }

    GraphDiff onContextRollback() {
        rollbackChanges();
        return new CompoundDiff();
    }

    /**
     * Performs a single database query that does not select rows. Returns an
     * array of update counts.
     * 
     * @since 1.1
     */
    public int[] performNonSelectingQuery(Query query) {
        int[] count = performGenericQuery(query).firstUpdateCount();
        return count != null ? count : new int[0];
    }

    /**
     * Performs a named mapped query that does not select rows. Returns an array
     * of update counts.
     * 
     * @since 1.1
     */
    public int[] performNonSelectingQuery(String queryName) {
        return performNonSelectingQuery(MappedExec.query(queryName));
    }

    /**
     * Performs a named mapped non-selecting query using a map of parameters.
     * Returns an array of update counts.
     * 
     * @since 1.1
     */
    public int[] performNonSelectingQuery(String queryName, Map<String, ?> parameters) {
        return performNonSelectingQuery(MappedExec.query(queryName).params(parameters));
    }

    /**
     * Returns a list of objects or DataRows for a named query stored in one of
     * the DataMaps. Internally Cayenne uses a caching policy defined in the
     * named query. If refresh flag is true, a refresh is forced no matter what
     * the caching policy is.
     * 
     * @param queryName
     *            a name of a GenericSelectQuery defined in one of the DataMaps.
     *            If no such query is defined, this method will throw a
     *            CayenneRuntimeException.
     * @param expireCachedLists
     *            A flag that determines whether refresh of <b>cached lists</b>
     *            is required in case a query uses caching.
     * @since 1.1
     */
    public List<?> performQuery(String queryName, boolean expireCachedLists) {
        return performQuery(queryName, Collections.emptyMap(), expireCachedLists);
    }

    /**
     * Returns a list of objects or DataRows for a named query stored in one of
     * the DataMaps. Internally Cayenne uses a caching policy defined in the
     * named query. If refresh flag is true, a refresh is forced no matter what
     * the caching policy is.
     * 
     * @param queryName
     *            a name of a GenericSelectQuery defined in one of the DataMaps.
     *            If no such query is defined, this method will throw a
     *            CayenneRuntimeException.
     * @param parameters
     *            A map of parameters to use with stored query.
     * @param expireCachedLists
     *            A flag that determines whether refresh of <b>cached lists</b>
     *            is required in case a query uses caching.
     * @since 1.1
     */
    public List<?> performQuery(String queryName, Map <String,?>parameters, boolean expireCachedLists) {
        return (List<?>) performQuery(expireCachedLists ?
                MappedSelect.query(queryName).params(parameters).forceNoCache() :
                MappedSelect.query(queryName).params(parameters));
    }

    /**
     * Returns <code>true</code> if the ObjectStore uses shared cache of a
     * parent DataDomain.
     * 
     * @since 1.1
     */
    public boolean isUsingSharedSnapshotCache() {
        return usingSharedSnapshotCache;
    }

    /**
     * @since 3.1
     */
    public void setUsingSharedSnapshotCache(boolean flag) {
        this.usingSharedSnapshotCache = flag;
    }

    // ---------------------------------------------
    // Serialization Support
    // ---------------------------------------------

    private void writeObject(ObjectOutputStream out) throws IOException {
        // See CAY-2382
        synchronized (getObjectStore()) {
            out.defaultWriteObject();
        }
        // Serialize local snapshots cache
        if (!isUsingSharedSnapshotCache()) {
            out.writeObject(objectStore.getDataRowCache());
        }
    }

    // serialization support
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // read non-transient properties
        in.defaultReadObject();

        // deserialize local snapshots cache
        if (!isUsingSharedSnapshotCache()) {
            DataRowStore cache = (DataRowStore) in.readObject();
            objectStore.setDataRowCache(cache);
        }

        // PersistentObjects have a transient DataContext because at deserialize time
        // the DataContext may need to be different from the one at serialize time (for programmer defined reasons).
        // So, when a Persistent is resurrected because it's DataContext was serialized,
        // it will then set the objects DataContext to the correct one.
        // If deserialized "otherwise", it will not have a DataContext.

        synchronized (getObjectStore()) {
            Iterator<?> it = objectStore.getObjectIterator();
            while (it.hasNext()) {
                Persistent object = (Persistent) it.next();
                object.setObjectContext(this);
            }
        }

        objectCreator = new DataContextObjectCreator(this);

        // ... deferring initialization of transient properties of this context till first access,
        // so that it can attach to Cayenne runtime using appropriate thread injector.
    }

    /**
     * Returns this context's ObjectStore.
     * 
     * @since 1.2
     */
    @Override
    public GraphManager getGraphManager() {
        return objectStore;
    }

    /**
     * An internal version of {@link #localObject(Persistent)} that operates on
     * ObjectId instead of Persistent, and wouldn't attempt to look up an object
     * in the parent channel.
     * 
     * @since 3.1
     */
    Persistent findOrCreateObject(ObjectId id) {

        if (id == null) {
            throw new IllegalArgumentException("Null ObjectId");
        }

        // have to synchronize almost the entire method to prevent multiple threads from
        // messing up Persistent objects per CAY-845. Originally only parts of "else" were synchronized,
        // but we had to expand the lock scope to ensure consistent behavior.
        synchronized (getGraphManager()) {
            Persistent cachedObject = (Persistent) getGraphManager().getNode(id);

            // return an existing object
            if (cachedObject != null) {

                int state = cachedObject.getPersistenceState();

                // TODO: Andrus, 1/24/2006 implement smart merge for modified objects...
                if (state != PersistenceState.MODIFIED && state != PersistenceState.DELETED) {
                    ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(id.getEntityName());
                    descriptor.injectValueHolders(cachedObject);
                }

                return cachedObject;
            }

            // create and register a hollow object
            ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(id.getEntityName());
            Persistent localObject = (Persistent) descriptor.createObject();

            localObject.setObjectContext(this);
            localObject.setObjectId(id);

            getGraphManager().registerNode(id, localObject);
            localObject.setPersistenceState(PersistenceState.HOLLOW);

            return localObject;
        }

    }

    /**
     * @since 1.2
     */
    protected void fireDataChannelChanged(Object postedBy, GraphDiff changes) {
        EventManager manager = getEventManager();

        if (manager != null) {
            GraphEvent e = new GraphEvent(this, postedBy, changes);
            manager.postEvent(e, DataChannel.GRAPH_CHANGED_SUBJECT);
        }
    }

    TransactionFactory getTransactionFactory() {
        attachToRuntimeIfNeeded();
        return transactionFactory;
    }

    /**
     * @since 4.0
     * @deprecated since 4.0 avoid using this directly. Transaction management
     *             at this level will be eventually removed
     */
    @Deprecated
    public void setTransactionFactory(TransactionFactory transactionFactory) {
        this.transactionFactory = transactionFactory;
    }

    @Override
    public <T> ResultBatchIterator<T> batchIterator(Select<T> query, int size) {
        return new ResultBatchIterator<T>(iterator(query), size);
    }

    /**
     * Returns whether this ObjectContext performs object validation before
     * commit is executed.
     *
     * @since 1.1
     */
    public boolean isValidatingObjectsOnCommit() {
        return validatingObjectsOnCommit;
    }

    /**
     * Sets the property defining whether this ObjectContext should perform
     * object validation before commit is executed.
     *
     * @since 1.1
     */
    public void setValidatingObjectsOnCommit(boolean flag) {
        this.validatingObjectsOnCommit = flag;
    }

    /**
     * Returns a map of user-defined properties associated with this
     * DataContext.
     *
     * @since 3.0
     */
    protected Map<String, Object> getUserProperties() {

        // as not all users will take advantage of properties, creating the
        // map on demand to keep the context lean...
        if (userProperties == null) {
            synchronized (this) {
                if (userProperties == null) {
                    userProperties = new ConcurrentHashMap<>();
                }
            }
        }

        return userProperties;
    }

    /**
     * Returns a user-defined property previously set via 'setUserProperty'.
     * Note that it is a caller responsibility to synchronize access to
     * properties.
     *
     * @since 3.0
     */
    @Override
    public Object getUserProperty(String key) {
        return getUserProperties().get(key);
    }

    /**
     * Sets a user-defined property. Note that it is a caller responsibility to
     * synchronize access to properties.
     *
     * @since 3.0
     */
    @Override
    public void setUserProperty(String key, Object value) {
        getUserProperties().put(key, value);
    }

    /**
     * {@inheritDoc}
     *
     * @since 5.0
     */
    @Override
    public void removeUserProperty(String key) {
        getUserProperties().remove(key);
    }

    /**
     * {@inheritDoc}
     *
     * @since 5.0
     */
    @Override
    public void clearUserProperties() {
        getUserProperties().clear();
    }

}
