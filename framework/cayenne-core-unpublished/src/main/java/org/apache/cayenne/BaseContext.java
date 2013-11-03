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
import org.apache.cayenne.query.Select;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;
import org.apache.cayenne.util.ObjectContextGraphAction;

/**
 * A common base superclass for Cayenne ObjectContext implementors.
 * 
 * @since 3.0
 */
public abstract class BaseContext implements ObjectContext {

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
     * {@link BaseContext#getThreadObjectContext}. Using null parameter will
     * unbind currently bound ObjectContext.
     * 
     * @since 3.0
     */
    public static void bindThreadObjectContext(ObjectContext context) {
        threadObjectContext.set(context);
    }

    // transient variables that should be reinitialized on deserialization from
    // the
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

    @Override
    public abstract void commitChanges();

    @Override
    public abstract void commitChangesToParent();

    @Override
    public abstract Collection<?> deletedObjects();

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
            T localObject = (T) getGraphManager().getNode(id);
            if (localObject != null) {
                return localObject;
            }

            // create a hollow object, optimistically assuming that the ID we
            // got from
            // 'objectFromAnotherContext' is a valid ID either in the parent
            // context or in
            // the DB. This essentially defers possible FaultFailureExceptions.

            ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(id.getEntityName());
            Persistent persistent = (Persistent) descriptor.createObject();

            persistent.setObjectContext(this);
            persistent.setObjectId(id);
            persistent.setPersistenceState(PersistenceState.HOLLOW);

            getGraphManager().registerNode(id, persistent);

            return (T) persistent;
        }
    }

    @Override
    public abstract GraphManager getGraphManager();

    @Override
    public abstract Collection<?> modifiedObjects();

    @Override
    public abstract <T> T newObject(Class<T> persistentClass);

    @Override
    public abstract void registerNewObject(Object object);

    @Override
    public abstract Collection<?> newObjects();

    @Override
    public abstract QueryResponse performGenericQuery(Query query);

    @Override
    public abstract List performQuery(Query query);

    /**
     * @since 3.2
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> select(Select<T> query) {
        return performQuery(query);
    }

    /**
     * @since 3.2
     */
    @Override
    public <T> T selectOne(Select<T> query) {
        List<T> objects = select(query);

        if (objects.size() == 0) {
            return null;
        } else if (objects.size() > 1) {
            throw new CayenneRuntimeException("Expected zero or one object, instead query matched: " + objects.size());
        }

        return objects.get(0);
    }

    /**
     * @since 3.2
     */
    @Override
    public <T> void iterate(Select<T> query, ResultIteratorCallback<T> callback) {
        ResultIterator<T> it = iterator(query);
        try {
            callback.iterate(it);
        } finally {
            it.close();
        }
    }

    @Override
    public abstract <T> ResultIterator<T> iterator(Select<T> query);

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

            // 5/28/2013 - Commented out this block to allow for modifying
            // objects in the postLoad callback
            // sanity check...
            // if (object.getPersistenceState() != PersistenceState.COMMITTED) {
            //
            // String state =
            // PersistenceState.persistenceStateName(object.getPersistenceState());
            //
            // // TODO: andrus 4/13/2006, modified and deleted states are
            // // possible due to
            // // a race condition, should we handle them here?
            // throw new
            // FaultFailureException("Error resolving fault for ObjectId: " +
            // oid + " and state (" + state
            // +
            // "). Possible cause - matching row is missing from the database.");
            // }
        }

        // resolve relationship fault
        if (lazyFaulting && property != null) {
            ClassDescriptor classDescriptor = getEntityResolver().getClassDescriptor(
                    object.getObjectId().getEntityName());
            PropertyDescriptor propertyDescriptor = classDescriptor.getProperty(property);

            // If we don't have a property descriptor, there's not much we can
            // do.
            // Let the caller know that the specified property could not be
            // found and list
            // all of the properties that could be so the caller knows what can
            // be used.
            if (propertyDescriptor == null) {
                final StringBuilder errorMessage = new StringBuilder();

                errorMessage.append(String.format("Property '%s' is not declared for entity '%s'.", property, object
                        .getObjectId().getEntityName()));

                errorMessage.append(" Declared properties are: ");

                // Grab each of the declared properties.
                final List<String> properties = new ArrayList<String>();
                classDescriptor.visitProperties(new PropertyVisitor() {
                    @Override
                    public boolean visitAttribute(final AttributeProperty property) {
                        properties.add(property.getName());

                        return true;
                    }
                    @Override
                    public boolean visitToOne(final ToOneProperty property) {
                        properties.add(property.getName());

                        return true;
                    }
                    @Override
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
                    } else {
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
    
    @Override
    public void propertyChanged(Persistent object, String property, Object oldValue, Object newValue) {
        graphAction.handlePropertyChange(object, property, oldValue, newValue);
    }

    @Override
    public abstract void rollbackChanges();

    @Override
    public abstract void rollbackChangesLocally();

    @Override
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
    @Override
    public EventManager getEventManager() {
        return channel != null ? channel.getEventManager() : null;
    }

    @Override
    public GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes, int syncType) {
        switch (syncType) {
        case DataChannel.ROLLBACK_CASCADE_SYNC:
            return onContextRollback(originatingContext);
        case DataChannel.FLUSH_NOCASCADE_SYNC:
            return onContextFlush(originatingContext, changes, false);
        case DataChannel.FLUSH_CASCADE_SYNC:
            return onContextFlush(originatingContext, changes, true);
        default:
            throw new CayenneRuntimeException("Unrecognized SyncMessage type: " + syncType);
        }
    }

    GraphDiff onContextRollback(ObjectContext originatingContext) {
        rollbackChanges();
        return new CompoundDiff();
    }

    protected abstract GraphDiff onContextFlush(ObjectContext originatingContext, GraphDiff changes, boolean cascade);

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
                    userProperties = new ConcurrentHashMap<String, Object>();
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
     * If ObjEntity qualifier is set, asks it to inject initial value to an
     * object. Also performs all Persistent initialization operations
     */
    protected void injectInitialValue(Object obj) {
        // must follow this exact order of property initialization per CAY-653,
        // i.e. have
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
            entity = getEntityResolver().getObjEntity(object.getClass());
        } catch (CayenneRuntimeException ex) {
            // ObjEntity cannot be fetched, ignored
            entity = null;
        }

        if (entity != null) {
            if (entity.getDeclaredQualifier() instanceof ValueInjector) {
                ((ValueInjector) entity.getDeclaredQualifier()).injectValue(object);
            }
        }

        // invoke callbacks
        getEntityResolver().getCallbackRegistry().performCallbacks(LifecycleEvent.POST_ADD, object);
    }

    /**
     * @since 3.1
     */
    @Override
    public <T> void deleteObjects(T... objects) throws DeleteDenyException {
        if (objects == null || objects.length == 0) {
            return;
        }

        ObjectContextDeleteAction action = new ObjectContextDeleteAction(this);

        for (Object object : objects) {
            action.performDelete((Persistent) object);
        }
    }

    @Override
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
