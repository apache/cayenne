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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cayenne.cache.NestedQueryCache;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.configuration.CayenneRuntime;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.exp.ValueInjector;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.GraphEvent;
import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.Property;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;
import org.apache.cayenne.util.ObjectContextGraphAction;

/**
 * A common base superclass for Cayenne ObjectContext implementors.
 * 
 * @since 3.0
 */
public abstract class BaseContext implements ObjectContext, DataChannel {

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
     * @throws IllegalStateException if there is no ObjectContext bound to the current
     *             thread.
     */
    public static ObjectContext getThreadObjectContext() throws IllegalStateException {
        ObjectContext context = threadObjectContext.get();
        if (context == null) {
            throw new IllegalStateException("Current thread has no bound ObjectContext.");
        }

        return context;
    }

    /**
     * Binds a ObjectContext to the current thread. ObjectContext can later be retrieved
     * by users in the same thread by calling {@link BaseContext#getThreadObjectContext}.
     * Using null parameter will unbind currently bound ObjectContext.
     * 
     * @since 3.0
     */
    public static void bindThreadObjectContext(ObjectContext context) {
        threadObjectContext.set(context);
    }

    // transient variables that should be reinitialized on deserialization from the
    // registry
    protected transient DataChannel channel;
    protected transient QueryCache queryCache;
    protected transient EntityResolver entityResolver;

    protected boolean validatingObjectsOnCommit = true;

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

    protected BaseContext() {
        graphAction = new ObjectContextGraphAction(this);
    }

    /**
     * Checks whether this context is attached to Cayenne runtime stack and if not,
     * attempts to attach itself to the runtime using Injector returned from the call to
     * {@link CayenneRuntime#getThreadInjector()}. If thread Injector is not available and
     * the context is not attached, throws CayenneRuntimeException.
     * <p>
     * This method is called internally by the context before access to transient
     * variables to allow the context to attach to the stack lazily following
     * deserialization.
     * 
     * @return true if the context successfully attached to the thread runtime, false - if
     *         it was already attached.
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
     * Attaches this context to the CayenneRuntime whose Injector is passed as an argument
     * to this method.
     * 
     * @since 3.1
     */
    protected void attachToRuntime(Injector injector) {

        // TODO: nested contexts handling??
        attachToChannel(injector.getInstance(DataChannel.class));
        setQueryCache(new NestedQueryCache(injector.getInstance(QueryCache.class)));
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
    }

    public abstract void commitChanges();

    public abstract void commitChangesToParent();

    public abstract Collection<?> deletedObjects();

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
     * Returns whether this ObjectContext performs object validation before commit is
     * executed.
     * 
     * @since 1.1
     */
    public boolean isValidatingObjectsOnCommit() {
        return validatingObjectsOnCommit;
    }

    /**
     * Sets the property defining whether this ObjectContext should perform object
     * validation before commit is executed.
     * 
     * @since 1.1
     */
    public void setValidatingObjectsOnCommit(boolean flag) {
        this.validatingObjectsOnCommit = flag;
    }

    /**
     * @since 3.1
     */
    public <T> T localObject(T objectFromAnotherContext) {

        if (objectFromAnotherContext == null) {
            throw new NullPointerException("Null object argument");
        }

        ObjectId id = ((Persistent) objectFromAnotherContext).getObjectId();

        // first look for the ID in the local GraphManager
        T localObject = (T) getGraphManager().getNode(id);
        if (localObject != null) {
            return localObject;
        }

        synchronized (getGraphManager()) {

            // check for race condition - the object may have appeared in the
            // GraphManager just recently...
            localObject = (T) getGraphManager().getNode(id);
            if (localObject != null) {
                return localObject;
            }

            // create a hollow object, optimistically assuming that the ID we got from
            // 'objectFromAnotherContext' is a valid ID either in the parent context or in
            // the DB. This essentially defers possible FaultFailureExceptions.

            ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(
                    id.getEntityName());
            Persistent persistent = (Persistent) descriptor.createObject();

            persistent.setObjectContext(this);
            persistent.setObjectId(id);
            persistent.setPersistenceState(PersistenceState.HOLLOW);

            getGraphManager().registerNode(id, persistent);

            return (T) persistent;
        }
    }

    public abstract GraphManager getGraphManager();

    /**
     * @deprecated since 3.1 Cayenne users should use {@link #localObject(Object)}; the
     *             internal code has been refactored to avoid using this method all
     *             together.
     */
    @Deprecated
    public Persistent localObject(ObjectId id, Object prototype) {

        if (id == null) {
            throw new IllegalArgumentException("Null ObjectId");
        }

        ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(
                id.getEntityName());

        // have to synchronize almost the entire method to prevent multiple threads from
        // messing up dataobjects per CAY-845. Originally only parts of "else" were
        // synchronized, but we had to expand the lock scope to ensure consistent
        // behavior.
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

                if (prototype != null
                        && ((Persistent) prototype).getPersistenceState() != PersistenceState.HOLLOW) {
                    localObject.setPersistenceState(PersistenceState.COMMITTED);
                    descriptor.shallowMerge(prototype, localObject);
                }
                else {
                    localObject.setPersistenceState(PersistenceState.HOLLOW);
                }

                return localObject;
            }
        }
    }

    public abstract Collection<?> modifiedObjects();

    public abstract <T> T newObject(Class<T> persistentClass);

    public abstract void registerNewObject(Object object);

    public abstract Collection<?> newObjects();

    public abstract QueryResponse performGenericQuery(Query query);

    @SuppressWarnings("unchecked")
    public abstract List performQuery(Query query);

    public void prepareForAccess(Persistent object, String property, boolean lazyFaulting) {
        if (object.getPersistenceState() == PersistenceState.HOLLOW) {

            ObjectId oid = object.getObjectId();
            List<?> objects = performQuery(new ObjectIdQuery(
                    oid,
                    false,
                    ObjectIdQuery.CACHE));

            if (objects.size() == 0) {
                throw new FaultFailureException(
                        "Error resolving fault, no matching row exists in the database for ObjectId: "
                                + oid);
            }
            else if (objects.size() > 1) {
                throw new FaultFailureException(
                        "Error resolving fault, more than one row exists in the database for ObjectId: "
                                + oid);
            }

            // sanity check...
            if (object.getPersistenceState() != PersistenceState.COMMITTED) {

                String state = PersistenceState.persistenceStateName(object
                        .getPersistenceState());

                // TODO: andrus 4/13/2006, modified and deleted states are possible due to
                // a race condition, should we handle them here?

                throw new FaultFailureException(
                        "Error resolving fault for ObjectId: "
                                + oid
                                + " and state ("
                                + state
                                + "). Possible cause - matching row is missing from the database.");
            }
        }

        // resolve relationship fault
        if (lazyFaulting && property != null) {
            ClassDescriptor classDescriptor = getEntityResolver().getClassDescriptor(
                    object.getObjectId().getEntityName());
            Property propertyDescriptor = classDescriptor.getProperty(property);

            // If we don't have a property descriptor, there's not much we can do.
            // Let the caller know that the specified property could not be found and list
            // all of the properties that could be so the caller knows what can be used.
            if (propertyDescriptor == null) {
                final StringBuilder errorMessage = new StringBuilder();

                errorMessage.append(String.format(
                        "Property '%s' is not declared for entity '%s'.",
                        property,
                        object.getObjectId().getEntityName()));

                errorMessage.append(" Declared properties are: ");

                // Grab each of the declared properties.
                final List<String> properties = new ArrayList<String>();
                classDescriptor.visitProperties(new PropertyVisitor() {

                    public boolean visitAttribute(final AttributeProperty property) {
                        properties.add(property.getName());

                        return true;
                    }

                    public boolean visitToOne(final ToOneProperty property) {
                        properties.add(property.getName());

                        return true;
                    }

                    public boolean visitToMany(final ToManyProperty property) {
                        properties.add(property.getName());

                        return true;
                    }
                });

                // Now add the declared property names to the error message.
                boolean first = true;
                for (String declaredProperty : properties) {
                    if (first) {
                        errorMessage.append(String.format("'%s'", declaredProperty));

                        first = false;
                    }
                    else {
                        errorMessage.append(String.format(", '%s'", declaredProperty));
                    }
                }

                errorMessage.append(".");

                throw new CayenneRuntimeException(errorMessage.toString());
            }

            // this should trigger fault resolving
            propertyDescriptor.readProperty(object);
        }
    }

    public void propertyChanged(
            Persistent object,
            String property,
            Object oldValue,
            Object newValue) {

        graphAction.handlePropertyChange(object, property, oldValue, newValue);
    }

    public abstract void rollbackChanges();

    public abstract void rollbackChangesLocally();

    public abstract Collection<?> uncommittedObjects();

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
    public EventManager getEventManager() {
        return channel != null ? channel.getEventManager() : null;
    }

    public GraphDiff onSync(
            ObjectContext originatingContext,
            GraphDiff changes,
            int syncType) {
        switch (syncType) {
            case DataChannel.ROLLBACK_CASCADE_SYNC:
                return onContextRollback(originatingContext);
            case DataChannel.FLUSH_NOCASCADE_SYNC:
                return onContextFlush(originatingContext, changes, false);
            case DataChannel.FLUSH_CASCADE_SYNC:
                return onContextFlush(originatingContext, changes, true);
            default:
                throw new CayenneRuntimeException("Unrecognized SyncMessage type: "
                        + syncType);
        }
    }

    GraphDiff onContextRollback(ObjectContext originatingContext) {
        rollbackChanges();
        return new CompoundDiff();
    }

    protected abstract GraphDiff onContextFlush(
            ObjectContext originatingContext,
            GraphDiff changes,
            boolean cascade);

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
     * @since 1.2
     */
    protected void fireDataChannelChanged(Object postedBy, GraphDiff changes) {
        EventManager manager = getEventManager();

        if (manager != null) {
            GraphEvent e = new GraphEvent(this, postedBy, changes);
            manager.postEvent(e, DataChannel.GRAPH_CHANGED_SUBJECT);
        }
    }

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
    public <T> void invalidateObjects(T... objects) {
        if (objects != null && objects.length > 0) {
            performGenericQuery(new RefreshQuery(Arrays.asList(objects)));
        }
    }

    /**
     * Returns a map of user-defined properties associated with this DataContext.
     * 
     * @since 3.0
     */
    protected Map<String, Object> getUserProperties() {

        // as not all users will take advantage of properties, creating the
        // map on demand to keep the context lean...
        if (userProperties == null) {
            synchronized (this) {
                if (userProperties == null) {
                    userProperties = new ConcurrentHashMap<String, Object>();
                }
            }
        }

        return userProperties;
    }

    /**
     * Returns a user-defined property previously set via 'setUserProperty'. Note that it
     * is a caller responsibility to synchronize access to properties.
     * 
     * @since 3.0
     */
    public Object getUserProperty(String key) {
        return getUserProperties().get(key);
    }

    /**
     * Sets a user-defined property. Note that it is a caller responsibility to
     * synchronize access to properties.
     * 
     * @since 3.0
     */
    public void setUserProperty(String key, Object value) {
        getUserProperties().put(key, value);
    }

    /**
     * If ObjEntity qualifier is set, asks it to inject initial value to an object. Also
     * performs all Persistent initialization operations
     */
    protected void injectInitialValue(Object obj) {
        // must follow this exact order of property initialization per CAY-653, i.e. have
        // the id and the context in place BEFORE setPersistence is called

        Persistent object = (Persistent) obj;

        object.setObjectContext(this);
        object.setPersistenceState(PersistenceState.NEW);

        GraphManager graphManager = getGraphManager();
        synchronized (graphManager) {
            graphManager.registerNode(object.getObjectId(), object);
            graphManager.nodeCreated(object.getObjectId());
        }

        ObjEntity entity;
        try {
            entity = getEntityResolver().lookupObjEntity(object.getClass());
        }
        catch (CayenneRuntimeException ex) {
            // ObjEntity cannot be fetched, ignored
            entity = null;
        }

        if (entity != null) {
            if (entity.getDeclaredQualifier() instanceof ValueInjector) {
                ((ValueInjector) entity.getDeclaredQualifier()).injectValue(object);
            }
        }

        // invoke callbacks
        getEntityResolver().getCallbackRegistry().performCallbacks(
                LifecycleEvent.POST_ADD,
                object);
    }

    /**
     * Schedules an object for deletion on the next commit of this context. Object's
     * persistence state is changed to PersistenceState.DELETED; objects related to this
     * object are processed according to delete rules, i.e. relationships can be unset
     * ("nullify" rule), deletion operation is cascaded (cascade rule).
     * 
     * @param object a persistent object that we want to delete.
     * @throws DeleteDenyException if a DENY delete rule is applicable for object
     *             deletion.
     * @throws NullPointerException if object is null.
     * @deprecated since 3.1 use {@link #deleteObjects(Object...)} method instead. This
     *             method is redundant.
     */
    @Deprecated
    public void deleteObject(Object object) {
        deleteObjects(object);
    }

    /**
     * @since 3.1
     */
    public <T> void deleteObjects(T... objects) throws DeleteDenyException {
        if (objects == null || objects.length == 0) {
            return;
        }

        ObjectContextDeleteAction action = new ObjectContextDeleteAction(this);

        for (Object object : objects) {
            action.performDelete((Persistent) object);
        }
    }

    public void deleteObjects(Collection<?> objects) throws DeleteDenyException {
        if (objects.isEmpty()) {
            return;
        }

        ObjectContextDeleteAction action = new ObjectContextDeleteAction(this);

        // Make a copy to iterate over to avoid ConcurrentModificationException.
        List<Object> copy = new ArrayList<Object>(objects);
        for (Object object : copy) {
            action.performDelete((Persistent) object);
        }
    }
}
