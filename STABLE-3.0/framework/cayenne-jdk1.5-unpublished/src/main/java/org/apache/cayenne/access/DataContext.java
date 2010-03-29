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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.BaseContext;
import org.apache.cayenne.CayenneException;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.DeleteDenyException;
import org.apache.cayenne.Fault;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.access.util.IteratedSelectObserver;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.cache.QueryCacheFactory;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.ChildDiffLoader;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.NamedQuery;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;
import org.apache.cayenne.util.EventUtil;
import org.apache.cayenne.util.GenericResponse;
import org.apache.cayenne.util.ObjectContextGraphAction;
import org.apache.cayenne.util.Util;

/**
 * The most common implementation of {@link ObjectContext}. DataContext is an isolated
 * container of an object graph, in a sense that any uncommitted changes to persistent
 * objects that are registered with the context, are not visible to the users of other
 * contexts.
 */
public class DataContext extends BaseContext implements DataChannel {

    // Set of DataContextDelegates to be notified.
    private DataContextDelegate delegate;

    protected boolean usingSharedSnaphsotCache;
    protected boolean validatingObjectsOnCommit;
    protected ObjectStore objectStore;

    // note that entity resolver is initialized from the parent channel the first time it
    // is accessed, and later cached in the context
    protected transient EntityResolver entityResolver;

    protected transient DataContextMergeHandler mergeHandler;

    ObjectContextGraphAction graphAction;

    /**
     * Stores the name of parent DataDomain. Used to defer initialization of the parent
     * QueryEngine after deserialization. This helps avoid an issue with certain servlet
     * engines (e.g. Tomcat) where HttpSessions with DataContext's are deserialized at
     * startup before Cayenne stack is fully initialized.
     */
    protected transient String lazyInitParentDomainName;

    /**
     * Returns the DataContext bound to the current thread.
     * 
     * @since 1.1
     * @return the DataContext associated with caller thread.
     * @throws IllegalStateException if there is no DataContext bound to the current
     *             thread.
     * @see org.apache.cayenne.conf.WebApplicationContextFilter
     * @deprecated since 3.0, replaced by BaseContex#getThreadObjectContext().
     */
    @Deprecated
    public static DataContext getThreadDataContext() throws IllegalStateException {
        return (DataContext) BaseContext.getThreadObjectContext();
    }

    /**
     * Binds a DataContext to the current thread. DataContext can later be retrieved by
     * users in the same thread by calling {@link DataContext#getThreadDataContext}. Using
     * null parameter will unbind currently bound DataContext.
     * 
     * @since 1.1
     * @deprecated since 3.0, replaced by BaseContex#bindThreadObjectContext().
     */
    @Deprecated
    public static void bindThreadDataContext(DataContext context) {
        BaseContext.bindThreadObjectContext(context);
    }

    /**
     * Factory method that creates and returns a new instance of DataContext based on
     * default domain. If more than one domain exists in the current configuration,
     * {@link DataContext#createDataContext(String)} must be used instead. ObjectStore
     * associated with created DataContext will have a cache stack configured using parent
     * domain settings.
     */
    public static DataContext createDataContext() {
        return Configuration.getSharedConfiguration().getDomain().createDataContext();
    }

    /**
     * Factory method that creates and returns a new instance of DataContext based on
     * default domain. If more than one domain exists in the current configuration,
     * {@link DataContext#createDataContext(String, boolean)} must be used instead.
     * ObjectStore associated with newly created DataContext will have a cache stack
     * configured according to the specified policy, overriding a parent domain setting.
     * 
     * @since 1.1
     */
    public static DataContext createDataContext(boolean useSharedCache) {
        return Configuration.getSharedConfiguration().getDomain().createDataContext(
                useSharedCache);
    }

    /**
     * Factory method that creates and returns a new instance of DataContext using named
     * domain as its parent. If there is no domain matching the name argument, an
     * exception is thrown.
     */
    public static DataContext createDataContext(String domainName) {
        DataDomain domain = Configuration.getSharedConfiguration().getDomain(domainName);
        if (domain == null) {
            throw new IllegalArgumentException("Non-existent domain: " + domainName);
        }
        return domain.createDataContext();
    }

    /**
     * Creates and returns new DataContext that will use a named DataDomain as its parent.
     * ObjectStore associated with newly created DataContext will have a cache stack
     * configured according to the specified policy, overriding a parent domain setting.
     * 
     * @since 1.1
     */
    public static DataContext createDataContext(String domainName, boolean useSharedCache) {

        DataDomain domain = Configuration.getSharedConfiguration().getDomain(domainName);
        if (domain == null) {
            throw new IllegalArgumentException("Non-existent domain: " + domainName);
        }
        return domain.createDataContext(useSharedCache);
    }

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
        // use a setter to properly initialize EntityResolver
        setChannel(channel);

        // inject self as parent context
        if (objectStore != null) {
            this.objectStore = objectStore;
            objectStore.setContext(this);

            DataDomain domain = getParentDataDomain();
            this.usingSharedSnaphsotCache = domain != null
                    && objectStore.getDataRowCache() == domain.getSharedSnapshotCache();
        }

        this.graphAction = new DataContextGraphAction(this);
    }

    /**
     * Returns {@link QueryCache} used by this DataContext, creating it on the fly if
     * needed. Uses parent DataDomain {@link QueryCacheFactory} to initialize the cache
     * for the first time, passing parent DataDomain's properties.
     * 
     * @since 3.0
     */
    @Override
    public QueryCache getQueryCache() {
        if (queryCache == null) {
            synchronized (this) {
                if (queryCache == null) {

                    DataDomain domain = getParentDataDomain();
                    queryCache = domain.getQueryCacheFactory().getQueryCache(
                            domain.getProperties());
                }
            }
        }

        return queryCache;
    }

    /**
     * Creates and returns a new child ObjectContext.
     * 
     * @since 3.0
     */
    public ObjectContext createChildContext() {
        return createChildDataContext();
    }

    /**
     * Creates and returns a new child DataContext.
     * 
     * @since 1.2
     * @deprecated since 3.0 use {@link #createChildContext()}.
     */
    @Deprecated
    public DataContext createChildDataContext() {
        DataContextFactory factory = getParentDataDomain().getDataContextFactory();

        // child ObjectStore should not have direct access to snapshot cache, so do not
        // pass it in constructor.
        ObjectStore objectStore = new ObjectStore();

        DataContext child = factory != null
                ? factory.createDataContext(this, objectStore)
                : new DataContext(this, objectStore);

        child.setValidatingObjectsOnCommit(isValidatingObjectsOnCommit());
        child.usingSharedSnaphsotCache = isUsingSharedSnapshotCache();
        return child;
    }

    /**
     * @since 1.2
     */
    public void setChannel(DataChannel channel) {
        if (this.channel != channel) {

            if (this.mergeHandler != null) {
                this.mergeHandler.setActive(false);
            }

            this.entityResolver = null;
            this.mergeHandler = null;

            this.channel = channel;

            if (channel != null) {

                // cache entity resolver, as we have no idea how expensive it is to query
                // it on the channel every time
                this.entityResolver = channel.getEntityResolver();

                EventManager eventManager = channel.getEventManager();

                if (eventManager != null) {
                    this.mergeHandler = new DataContextMergeHandler(this);

                    // listen to our channel events...
                    // note that we must reset listener on channel switch, as there is no
                    // guarantee that a new channel uses the same EventManager.
                    EventUtil.listenForChannelEvents(channel, mergeHandler);
                }

                if (!usingSharedSnaphsotCache && getObjectStore() != null) {
                    DataRowStore cache = getObjectStore().getDataRowCache();

                    if (cache != null) {
                        cache.setEventManager(eventManager);
                    }
                }
            }
        }
    }

    @Override
    public DataChannel getChannel() {
        awakeFromDeserialization();
        return super.getChannel();
    }

    /**
     * Returns a DataDomain used by this DataContext. DataDomain is looked up in the
     * DataChannel hierarchy. If a channel is not a DataDomain or a DataContext, null is
     * returned.
     * 
     * @return DataDomain that is a direct or indirect parent of this DataContext in the
     *         DataChannel hierarchy.
     * @since 1.1
     */
    public DataDomain getParentDataDomain() {
        awakeFromDeserialization();

        if (channel == null) {
            return null;
        }

        if (channel instanceof DataDomain) {
            return (DataDomain) channel;
        }

        List response = channel.onQuery(this, new DataDomainQuery()).firstList();

        if (response != null
                && response.size() > 0
                && response.get(0) instanceof DataDomain) {
            return (DataDomain) response.get(0);
        }

        return null;
    }

    /**
     * Sets a DataContextDelegate for this context. Delegate is notified of certain events
     * in the DataContext lifecycle and can customize DataContext behavior.
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
     * @return a delegate instance if it is initialized, or a shared noop implementation
     *         the context has no delegate. Useful to prevent extra null checks and
     *         conditional logic in the code.
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
     * Returns <code>true</code> if there are any modified, deleted or new objects
     * registered with this DataContext, <code>false</code> otherwise.
     */
    public boolean hasChanges() {
        return getObjectStore().hasChanges();
    }

    /**
     * Returns a list of objects that are registered with this DataContext and have a
     * state PersistenceState.NEW
     */
    @Override
    public Collection<?> newObjects() {
        return getObjectStore().objectsInState(PersistenceState.NEW);
    }

    /**
     * Returns a list of objects that are registered with this DataContext and have a
     * state PersistenceState.DELETED
     */
    @Override
    public Collection<?> deletedObjects() {
        return getObjectStore().objectsInState(PersistenceState.DELETED);
    }

    /**
     * Returns a list of objects that are registered with this DataContext and have a
     * state PersistenceState.MODIFIED
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
        Collection<Object> objects = new ArrayList<Object>(len > 100 ? len / 2 : len);

        Iterator it = getObjectStore().getObjectIterator();
        while (it.hasNext()) {
            Persistent object = (Persistent) it.next();
            int state = object.getPersistenceState();
            if (state == PersistenceState.MODIFIED
                    || state == PersistenceState.NEW
                    || state == PersistenceState.DELETED) {

                objects.add(object);
            }
        }

        return objects;
    }

    /**
     * Returns a DataRow reflecting current, possibly uncommitted, object state.
     * <p>
     * <strong>Warning:</strong> This method will return a partial snapshot if an object
     * or one of its related objects that propagate their keys to this object have
     * temporary ids. DO NOT USE this method if you expect a DataRow to represent a
     * complete object state.
     * </p>
     * 
     * @since 1.1
     */
    public DataRow currentSnapshot(final Persistent object) {

        // for a HOLLOW object return snapshot from cache
        if (object.getPersistenceState() == PersistenceState.HOLLOW
                && object.getObjectContext() != null) {

            return getObjectStore().getSnapshot(object.getObjectId());
        }

        ObjEntity entity = getEntityResolver().lookupObjEntity(object);
        final ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(
                entity.getName());
        final DataRow snapshot = new DataRow(10);

        descriptor.visitProperties(new PropertyVisitor() {

            public boolean visitAttribute(AttributeProperty property) {
                ObjAttribute objAttr = property.getAttribute();

                // processing compound attributes correctly
                snapshot.put(objAttr.getDbAttributePath(), property
                        .readPropertyDirectly(object));
                return true;
            }

            public boolean visitToMany(ToManyProperty property) {
                // do nothing
                return true;
            }

            public boolean visitToOne(ToOneProperty property) {
                ObjRelationship rel = property.getRelationship();

                // if target doesn't propagates its key value, skip it
                if (rel.isSourceIndependentFromTargetChange()) {
                    return true;
                }

                Object targetObject = property.readPropertyDirectly(object);
                if (targetObject == null) {
                    return true;
                }

                // if target is Fault, get id attributes from stored snapshot
                // to avoid unneeded fault triggering
                if (targetObject instanceof Fault) {
                    DataRow storedSnapshot = getObjectStore().getSnapshot(
                            object.getObjectId());
                    if (storedSnapshot == null) {
                        throw new CayenneRuntimeException(
                                "No matching objects found for ObjectId "
                                        + object.getObjectId()
                                        + ". Object may have been deleted externally.");
                    }

                    DbRelationship dbRel = rel.getDbRelationships().get(0);
                    for (DbJoin join : dbRel.getJoins()) {
                        String key = join.getSourceName();
                        snapshot.put(key, storedSnapshot.get(key));
                    }

                    return true;
                }

                // target is resolved and we have an FK->PK to it,
                // so extract it from target...
                Persistent target = (Persistent) targetObject;
                Map<String, Object> idParts = target.getObjectId().getIdSnapshot();

                // this may happen in uncommitted objects - see the warning in the JavaDoc
                // of
                // this method.
                if (idParts.isEmpty()) {
                    return true;
                }

                DbRelationship dbRel = rel.getDbRelationships().get(0);
                Map<String, Object> fk = dbRel.srcFkSnapshotWithTargetSnapshot(idParts);
                snapshot.putAll(fk);
                return true;
            }
        });

        // process object id map
        // we should ignore any object id values if a corresponding attribute
        // is a part of relationship "toMasterPK", since those values have been
        // set above when db relationships where processed.
        Map<String, Object> thisIdParts = object.getObjectId().getIdSnapshot();
        if (thisIdParts != null) {

            // put only those that do not exist in the map
            for (Map.Entry<String, Object> entry : thisIdParts.entrySet()) {
                String nextKey = entry.getKey();
                if (!snapshot.containsKey(nextKey)) {
                    snapshot.put(nextKey, entry.getValue());
                }
            }
        }

        return snapshot;
    }

    /**
     * Converts a list of data rows to a list of DataObjects.
     * 
     * @since 1.1
     * @deprecated since 3.0 as refreshing and resolvingInheritanceHierarchy flags are
     *             deprecated. Use {@link #objectsFromDataRows(ClassDescriptor, List)}
     *             instead.
     */
    @Deprecated
    public List objectsFromDataRows(
            ObjEntity entity,
            List dataRows,
            boolean refresh,
            boolean resolveInheritanceHierarchy) {

        ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(
                entity.getName());
        return objectsFromDataRows(descriptor, dataRows);
    }

    /**
     * Converts a list of DataRows to a List of DataObject registered with this
     * DataContext.
     * 
     * @since 3.0
     */
    public List objectsFromDataRows(
            ClassDescriptor descriptor,
            List<? extends DataRow> dataRows) {
        return new ObjectResolver(this, descriptor, true)
                .synchronizedObjectsFromDataRows(dataRows);
    }

    /**
     * Converts a list of DataRows to a List of DataObject registered with this
     * DataContext.
     * 
     * @deprecated since 3.0 as refresh and resolveInheritanceHierarchy flags are
     *             deprecated. Use {@link #objectsFromDataRows(ClassDescriptor, List)}
     *             instead.
     * @since 1.1
     * @see DataRow
     */
    @Deprecated
    public List objectsFromDataRows(
            Class<?> objectClass,
            List<? extends DataRow> dataRows,
            boolean refresh,
            boolean resolveInheritanceHierarchy) {
        ObjEntity entity = this.getEntityResolver().lookupObjEntity(objectClass);

        if (entity == null) {
            throw new CayenneRuntimeException("Unmapped Java class: " + objectClass);
        }

        ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(
                entity.getName());
        return objectsFromDataRows(descriptor, dataRows);
    }

    /**
     * Creates a DataObject from DataRow.
     * 
     * @see DataRow
     */
    public <T extends DataObject> T objectFromDataRow(
            Class<T> objectClass,
            DataRow dataRow,
            boolean refresh) {

        ObjEntity entity = this.getEntityResolver().lookupObjEntity(objectClass);

        if (entity == null) {
            throw new CayenneRuntimeException("Unmapped Java class: " + objectClass);
        }

        ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(
                entity.getName());
        List<T> list = objectsFromDataRows(descriptor, Collections.singletonList(dataRow));
        return list.get(0);
    }

    /**
     * Creates a DataObject from DataRow. This variety of the 'objectFromDataRow' method
     * is normally used for generic classes.
     * 
     * @see DataRow
     * @since 3.0
     */
    public DataObject objectFromDataRow(
            String entityName,
            DataRow dataRow,
            boolean refresh) {

        ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(entityName);
        List<?> list = objectsFromDataRows(descriptor, Collections.singletonList(dataRow));

        return (DataObject) list.get(0);
    }

    /**
     * @deprecated since 3.0, use {@link #newObject(String)} instead.
     */
    @Deprecated
    public DataObject createAndRegisterNewObject(String objEntityName) {
        return (DataObject) newObject(objEntityName);
    }

    /**
     * Creates and registers a new persistent object.
     * 
     * @since 1.2
     */
    @Override
    public <T> T newObject(Class<T> persistentClass) {
        if (persistentClass == null) {
            throw new NullPointerException("Null 'persistentClass'");
        }

        ObjEntity entity = getEntityResolver().lookupObjEntity(persistentClass);
        if (entity == null) {
            throw new IllegalArgumentException("Class is not mapped with Cayenne: "
                    + persistentClass.getName());
        }

        return (T) newObject(entity.getName());
    }

    /**
     * Instantiates a new object and registers it with this context. Object class is
     * determined from the mapped entity. Object class must have a default constructor.
     * <p/>
     * <i>Note: in most cases {@link #newObject(Class)} method should be used, however
     * this method is helpful when generic persistent classes are used.</i>
     * 
     * @since 3.0
     */
    public Persistent newObject(String entityName) {
        ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(entityName);
        if (descriptor == null) {
            throw new IllegalArgumentException("Invalid entity name: " + entityName);
        }

        Persistent object;
        try {
            object = (Persistent) descriptor.createObject();
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error instantiating object.", ex);
        }

        // this will initialize to-many lists
        descriptor.injectValueHolders(object);

        ObjectId id = new ObjectId(entityName);

        // note that the order of initialization of persistence artifacts below is
        // important - do not change it lightly
        object.setObjectId(id);
        object.setObjectContext(this);
        object.setPersistenceState(PersistenceState.NEW);
        getObjectStore().registerNode(id, object);
        getObjectStore().nodeCreated(id);
        
        injectInitialValue(object);

        // invoke callbacks
        getEntityResolver().getCallbackRegistry().performCallbacks(
                LifecycleEvent.POST_ADD,
                object);

        return object;
    }

    /**
     * Instantiates new object and registers it with itself. Object class must have a
     * default constructor.
     * 
     * @since 1.1
     * @deprecated since 3.0, use {@link #newObject(Class)} instead.
     */
    @Deprecated
    public DataObject createAndRegisterNewObject(Class objectClass) {
        if (objectClass == null) {
            throw new NullPointerException("DataObject class can't be null.");
        }

        ObjEntity entity = getEntityResolver().lookupObjEntity(objectClass);
        if (entity == null) {
            throw new IllegalArgumentException("Class is not mapped with Cayenne: "
                    + objectClass.getName());
        }

        return createAndRegisterNewObject(entity.getName());
    }

    /**
     * Registers a transient object with the context, recursively registering all
     * transient persistent objects attached to this object via relationships.
     * <p/>
     * <i>Note that since 3.0 this method takes Object as an argument instead of a
     * {@link DataObject}.</i>
     * 
     * @param object new object that needs to be made persistent.
     */
    @Override
    public void registerNewObject(Object object) {
        if (object == null) {
            throw new NullPointerException("Can't register null object.");
        }

        ObjEntity entity = getEntityResolver().lookupObjEntity(object);
        if (entity == null) {
            throw new IllegalArgumentException(
                    "Can't find ObjEntity for Persistent class: "
                            + object.getClass().getName()
                            + ", class is likely not mapped.");
        }

        final Persistent persistent = (Persistent) object;

        // sanity check - maybe already registered
        if (persistent.getObjectId() != null) {
            if (persistent.getObjectContext() == this) {
                // already registered, just ignore
                return;
            }
            else if (persistent.getObjectContext() != null) {
                throw new IllegalStateException(
                        "Persistent is already registered with another DataContext. "
                                + "Try using 'localObjects()' instead.");
            }
        }
        else {
            persistent.setObjectId(new ObjectId(entity.getName()));
        }

        persistent.setObjectContext(this);
        persistent.setPersistenceState(PersistenceState.NEW);

        getObjectStore().registerNode(persistent.getObjectId(), object);
        getObjectStore().nodeCreated(persistent.getObjectId());

        // now we need to find all arc changes, inject missing value holders and pull in
        // all transient connected objects

        ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(
                entity.getName());
        if (descriptor == null) {
            throw new IllegalArgumentException("Invalid entity name: " + entity.getName());
        }

        descriptor.visitProperties(new PropertyVisitor() {

            public boolean visitToMany(ToManyProperty property) {
                property.injectValueHolder(persistent);

                if (!property.isFault(persistent)) {

                    Object value = property.readProperty(persistent);
                    Collection<Map.Entry> collection = (value instanceof Map)
                            ? ((Map) value).entrySet()
                            : (Collection) value;

                    Iterator<Map.Entry> it = collection.iterator();
                    while (it.hasNext()) {
                        Object target = it.next();

                        if (target instanceof Persistent) {
                            Persistent targetDO = (Persistent) target;

                            // make sure it is registered
                            registerNewObject(targetDO);
                            getObjectStore().arcCreated(
                                    persistent.getObjectId(),
                                    targetDO.getObjectId(),
                                    property.getName());
                        }
                    }
                }
                return true;
            }

            public boolean visitToOne(ToOneProperty property) {
                Object target = property.readPropertyDirectly(persistent);

                if (target instanceof Persistent) {

                    Persistent targetDO = (Persistent) target;

                    // make sure it is registered
                    registerNewObject(targetDO);
                    getObjectStore().arcCreated(
                            persistent.getObjectId(),
                            targetDO.getObjectId(),
                            property.getName());
                }
                return true;
            }

            public boolean visitAttribute(AttributeProperty property) {
                return true;
            }
        });
        
        injectInitialValue(object);

        // invoke callbacks
        getEntityResolver().getCallbackRegistry().performCallbacks(
                LifecycleEvent.POST_ADD,
                persistent);
    }

    /**
     * Unregisters a Collection of DataObjects from the DataContext and the underlying
     * ObjectStore. This operation also unsets DataContext and ObjectId for each object
     * and changes its state to TRANSIENT.
     * 
     * @see #invalidateObjects(Collection)
     */
    public void unregisterObjects(Collection dataObjects) {
        getObjectStore().objectsUnregistered(dataObjects);
    }

    /**
     * Schedules all objects in the collection for deletion on the next commit of this
     * DataContext. Object's persistence state is changed to PersistenceState.DELETED;
     * objects related to this object are processed according to delete rules, i.e.
     * relationships can be unset ("nullify" rule), deletion operation is cascaded
     * (cascade rule).
     * <p>
     * <i>"Nullify" delete rule side effect: </i> passing a collection representing
     * to-many relationship with nullify delete rule may result in objects being removed
     * from collection.
     * </p>
     * 
     * @since 1.2
     */
    public void deleteObjects(Collection objects) {
        if (objects.isEmpty()) {
            return;
        }

        // clone object list... this maybe a relationship collection with nullify delete
        // rule, so modifying
        for (Persistent object : new ArrayList<Persistent>(objects)) {
            deleteObject(object);
        }
    }

    /**
     * Schedules an object for deletion on the next commit of this DataContext. Object's
     * persistence state is changed to PersistenceState.DELETED; objects related to this
     * object are processed according to delete rules, i.e. relationships can be unset
     * ("nullify" rule), deletion operation is cascaded (cascade rule).
     * 
     * @param object a persistent object that we want to delete.
     * @throws DeleteDenyException if a DENY delete rule is applicable for object
     *             deletion.
     * @throws NullPointerException if object is null.
     */
    @Override
    public void deleteObject(Object object) throws DeleteDenyException {
        new DataContextDeleteAction(this).performDelete((Persistent) object);
    }

    /**
     * Refetches object data for ObjectId. This method is used internally by Cayenne to
     * resolve objects in state <code>PersistenceState.HOLLOW</code>. It can also be used
     * to refresh certain objects.
     * 
     * @throws CayenneRuntimeException if object id doesn't match any records, or if there
     *             is more than one object is fetched.
     * @deprecated since 3.0 use {@link ObjectIdQuery} with appropriate refresh settings.
     */
    @Deprecated
    public DataObject refetchObject(ObjectId oid) {

        if (oid == null) {
            throw new NullPointerException("Null ObjectId");
        }

        if (oid.isTemporary()) {
            throw new CayenneRuntimeException("Can't refetch ObjectId "
                    + oid
                    + ", as it is a temporary id.");
        }

        synchronized (getObjectStore()) {
            DataObject object = (DataObject) objectStore.getNode(oid);

            // clean up any cached data for this object
            if (object != null) {
                this.invalidateObjects(Collections.singleton(object));
            }
        }

        DataObject object = (DataObject) DataObjectUtils.objectForQuery(
                this,
                new ObjectIdQuery(oid));

        if (object == null) {
            throw new CayenneRuntimeException(
                    "Refetch failure: no matching objects found for ObjectId " + oid);
        }

        return object;
    }

    /**
     * If the parent channel is a DataContext, reverts local changes to make this context
     * look like the parent, if the parent channel is a DataDomain, reverts all changes.
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
     * Reverts any changes that have occurred to objects registered with DataContext; also
     * performs cascading rollback of all parent DataContexts.
     */
    @Override
    public void rollbackChanges() {

        if (objectStore.hasChanges()) {
            GraphDiff diff = getObjectStore().getChanges();

            // call channel with changes BEFORE reverting them, so that any interceptors
            // could record them

            if (channel != null) {
                channel.onSync(this, diff, DataChannel.ROLLBACK_CASCADE_SYNC);
            }

            getObjectStore().objectsRolledBack();
            fireDataChannelRolledback(this, diff);
        }
        else {
            if (channel != null) {
                channel.onSync(
                        this,
                        new CompoundDiff(),
                        DataChannel.ROLLBACK_CASCADE_SYNC);
            }
        }

    }

    /**
     * "Flushes" the changes to the parent {@link DataChannel}. If the parent channel is a
     * DataContext, it updates its objects with this context's changes, without a database
     * update. If it is a DataDomain (the most common case), the changes are written to
     * the database. To cause cascading commit all the way to the database, one must use
     * {@link #commitChanges()}.
     * 
     * @since 1.2
     * @see #commitChanges()
     */
    @Override
    public void commitChangesToParent() {
        flushToParent(false);
    }

    /**
     * Synchronizes object graph with the database. Executes needed insert, update and
     * delete queries (generated internally).
     */
    @Override
    public void commitChanges() throws CayenneRuntimeException {
        flushToParent(true);
    }

    @Override
    protected GraphDiff onContextFlush(
            ObjectContext originatingContext,
            GraphDiff changes,
            boolean cascade) {

        boolean childContext = this != originatingContext && changes != null;

        try {
            if (childContext) {
                getObjectStore().childContextSyncStarted();
                changes.apply(new ChildDiffLoader(this));
                fireDataChannelChanged(originatingContext, changes);
            }

            return (cascade) ? flushToParent(true) : new CompoundDiff();
        }
        finally {
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
            throw new CayenneRuntimeException(
                    "Cannot commit changes - channel is not set.");
        }

        int syncType = cascade
                ? DataChannel.FLUSH_CASCADE_SYNC
                : DataChannel.FLUSH_NOCASCADE_SYNC;

        ObjectStore objectStore = getObjectStore();
        GraphDiff parentChanges = null;

        // prevent multiple commits occurring simultaneously
        synchronized (objectStore) {

            ObjectStoreGraphDiff changes = objectStore.getChanges();
            boolean noop = isValidatingObjectsOnCommit()
                    ? changes.validateAndCheckNoop()
                    : changes.isNoop();

            if (noop) {
                // need to clear phantom changes
                objectStore.postprocessAfterPhantomCommit();
            }
            else {

                try {
                    parentChanges = getChannel().onSync(this, changes, syncType);

                    // note that this is a hack resulting from a fix to CAY-766... To
                    // support
                    // valid object state in PostPersist callback,
                    // 'postprocessAfterCommit' is
                    // invoked by DataDomain.onSync(..). Unless the parent is DataContext,
                    // and
                    // this method is not invoked!! As a result, PostPersist will contain
                    // temp
                    // ObjectIds in nested contexts and perm ones in flat contexts.
                    // Pending better callback design .....
                    if (objectStore.hasChanges()) {
                        objectStore.postprocessAfterCommit(parentChanges);
                    }

                    // this event is caught by peer nested DataContexts to synchronize the
                    // state
                    fireDataChannelCommitted(this, changes);
                }
                // "catch" is needed to unwrap OptimisticLockExceptions
                catch (CayenneRuntimeException ex) {
                    Throwable unwound = Util.unwindException(ex);

                    if (unwound instanceof CayenneRuntimeException) {
                        throw (CayenneRuntimeException) unwound;
                    }
                    else {
                        throw new CayenneRuntimeException("Commit Exception", unwound);
                    }
                }
            }

            // merge changes from parent as well as changes caused by lifecycle event
            // callbacks/listeners...

            CompoundDiff diff = new CompoundDiff();

            diff.addAll(objectStore.getLifecycleEventInducedChanges());
            if (parentChanges != null) {
                diff.add(parentChanges);
            }

            // this event is caught by child DataContexts to update temporary
            // ObjectIds with permanent
            if (!diff.isNoop()) {
                fireDataChannelCommitted(getChannel(), diff);
            }

            return diff;
        }

    }

    /**
     * Performs a single database select query returning result as a ResultIterator. It is
     * caller's responsibility to explicitly close the ResultIterator. A failure to do so
     * will result in a database connection not being released. Another side effect of an
     * open ResultIterator is that an internal Cayenne transaction that originated in this
     * method stays open until the iterator is closed. So users should normally close the
     * iterator within the same thread that opened it.
     */
    public ResultIterator performIteratedQuery(Query query) throws CayenneException {
        if (Transaction.getThreadTransaction() != null) {
            return internalPerformIteratedQuery(query);
        }
        else {

            // manually manage a transaction, so that a ResultIterator wrapper could close
            // it when it is done.
            Transaction tx = getParentDataDomain().createTransaction();
            Transaction.bindThreadTransaction(tx);

            ResultIterator result;
            try {
                result = internalPerformIteratedQuery(query);
            }
            catch (Exception e) {
                Transaction.bindThreadTransaction(null);
                tx.setRollbackOnly();
                throw new CayenneException(e);
            }
            finally {
                // note: we are keeping the transaction bound to the current thread on
                // success - iterator will unbind it. Unsetting a transaction here would
                // result in some strangeness, at least on Ingres

                if (tx.getStatus() == Transaction.STATUS_MARKED_ROLLEDBACK) {
                    try {
                        tx.rollback();
                    }
                    catch (Exception rollbackEx) {
                    }
                }
            }

            return new TransactionResultIteratorDecorator(result, tx);
        }
    }

    /**
     * Runs an iterated query in transactional context provided by the caller.
     * 
     * @since 1.2
     */
    ResultIterator internalPerformIteratedQuery(Query query) throws CayenneException {
        // note that for now DataChannel API does not support cursors (aka
        // ResultIterator), so we have to go directly to the DataDomain.
        IteratedSelectObserver observer = new IteratedSelectObserver();
        getParentDataDomain().performQueries(Collections.singletonList(query), observer);
        return observer.getResultIterator();
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
            throw new CayenneRuntimeException(
                    "Can't run query - parent DataChannel is not set.");
        }

        return onQuery(this, query);
    }

    /**
     * Performs a single selecting query. Various query setting control the behavior of
     * this method and the results returned:
     * <ul>
     * <li>Query caching policy defines whether the results are retrieved from cache or
     * fetched from the database. Note that queries that use caching must have a name that
     * is used as a caching key.</li>
     * <li>Query refreshing policy controls whether to refresh existing data objects and
     * ignore any cached values.</li>
     * <li>Query data rows policy defines whether the result should be returned as
     * DataObjects or DataRows.</li>
     * </ul>
     * <p>
     * <i>Since 1.2 takes any Query parameter, not just GenericSelectQuery</i>
     * </p>
     * 
     * @return A list of DataObjects or a DataRows, depending on the value returned by
     *         {@link QueryMetadata#isFetchingDataRows()}.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List performQuery(Query query) {
        query = nonNullDelegate().willPerformQuery(this, query);
        if (query == null) {
            return new ArrayList<Object>(1);
        }

        List result = onQuery(this, query).firstList();
        return result != null ? result : new ArrayList<Object>(1);
    }

    /**
     * An implementation of a {@link DataChannel} method that is used by child contexts to
     * execute queries. Not intended for direct use.
     * 
     * @since 1.2
     */
    public QueryResponse onQuery(ObjectContext context, Query query) {
        return new DataContextQueryAction(this, context, query).execute();
    }

    /**
     * Performs a single database query that does not select rows. Returns an array of
     * update counts.
     * 
     * @since 1.1
     */
    public int[] performNonSelectingQuery(Query query) {
        int[] count = performGenericQuery(query).firstUpdateCount();
        return count != null ? count : new int[0];
    }

    /**
     * Performs a named mapped query that does not select rows. Returns an array of update
     * counts.
     * 
     * @since 1.1
     */
    public int[] performNonSelectingQuery(String queryName) {
        return performNonSelectingQuery(new NamedQuery(queryName));
    }

    /**
     * Performs a named mapped non-selecting query using a map of parameters. Returns an
     * array of update counts.
     * 
     * @since 1.1
     */
    public int[] performNonSelectingQuery(String queryName, Map<String, ?> parameters) {
        return performNonSelectingQuery(new NamedQuery(queryName, parameters));
    }

    /**
     * Returns a list of objects or DataRows for a named query stored in one of the
     * DataMaps. Internally Cayenne uses a caching policy defined in the named query. If
     * refresh flag is true, a refresh is forced no matter what the caching policy is.
     * 
     * @param queryName a name of a GenericSelectQuery defined in one of the DataMaps. If
     *            no such query is defined, this method will throw a
     *            CayenneRuntimeException.
     * @param expireCachedLists A flag that determines whether refresh of <b>cached
     *            lists</b> is required in case a query uses caching.
     * @since 1.1
     */
    public List<?> performQuery(String queryName, boolean expireCachedLists) {
        return performQuery(queryName, Collections.EMPTY_MAP, expireCachedLists);
    }

    /**
     * Returns a list of objects or DataRows for a named query stored in one of the
     * DataMaps. Internally Cayenne uses a caching policy defined in the named query. If
     * refresh flag is true, a refresh is forced no matter what the caching policy is.
     * 
     * @param queryName a name of a GenericSelectQuery defined in one of the DataMaps. If
     *            no such query is defined, this method will throw a
     *            CayenneRuntimeException.
     * @param parameters A map of parameters to use with stored query.
     * @param expireCachedLists A flag that determines whether refresh of <b>cached
     *            lists</b> is required in case a query uses caching.
     * @since 1.1
     */
    public List<?> performQuery(
            String queryName,
            Map parameters,
            boolean expireCachedLists) {
        NamedQuery query = new NamedQuery(queryName, parameters);
        query.setForceNoCache(expireCachedLists);
        return performQuery(query);
    }

    /**
     * Returns EntityResolver. EntityResolver can be null if DataContext has not been
     * attached to an DataChannel.
     */
    @Override
    public EntityResolver getEntityResolver() {
        awakeFromDeserialization();
        return entityResolver;
    }

    /**
     * Returns <code>true</code> if the ObjectStore uses shared cache of a parent
     * DataDomain.
     * 
     * @since 1.1
     */
    public boolean isUsingSharedSnapshotCache() {
        return usingSharedSnaphsotCache;
    }

    /**
     * Returns whether this DataContext performs object validation before commit is
     * executed.
     * 
     * @since 1.1
     */
    public boolean isValidatingObjectsOnCommit() {
        return validatingObjectsOnCommit;
    }

    /**
     * Sets the property defining whether this DataContext should perform object
     * validation before commit is executed.
     * 
     * @since 1.1
     */
    public void setValidatingObjectsOnCommit(boolean flag) {
        this.validatingObjectsOnCommit = flag;
    }

    // ---------------------------------------------
    // Serialization Support
    // ---------------------------------------------

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        // If the "parent" of this datacontext is a DataDomain, then just write the
        // name of it. Then when deserialization happens, we can get back the DataDomain
        // by name, from the shared configuration (which will either load it if need be,
        // or return an existing one.

        if (this.channel == null && this.lazyInitParentDomainName != null) {
            out.writeObject(lazyInitParentDomainName);
        }
        else if (this.channel instanceof DataDomain) {
            DataDomain domain = (DataDomain) this.channel;
            out.writeObject(domain.getName());
        }
        else {
            // Hope that whatever this.parent is, that it is Serializable
            out.writeObject(this.channel);
        }

        // Serialize local snapshots cache
        if (!isUsingSharedSnapshotCache()) {
            out.writeObject(objectStore.getDataRowCache());
        }
    }

    // serialization support
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        // 1. read non-transient properties
        in.defaultReadObject();

        // 2. read parent or its name
        Object value = in.readObject();
        if (value instanceof DataChannel) {
            // A real QueryEngine object - use it
        	// call a setter to initialize EntityResolver 
            setChannel((DataChannel) value);
        }
        else if (value instanceof String) {
            // The name of a DataDomain - use it
            this.lazyInitParentDomainName = (String) value;
        }
        else {
            throw new CayenneRuntimeException(
                    "Parent attribute of DataContext was neither a QueryEngine nor "
                            + "the name of a valid DataDomain:"
                            + value);
        }

        // 3. Deserialize local snapshots cache
        if (!isUsingSharedSnapshotCache()) {
            DataRowStore cache = (DataRowStore) in.readObject();
            objectStore.setDataRowCache(cache);
        }

        // CayenneDataObjects have a transient datacontext
        // because at deserialize time the datacontext may need to be different
        // than the one at serialize time (for programmer defined reasons).
        // So, when a dataobject is resurrected because it's datacontext was
        // serialized, it will then set the objects datacontext to the correctone
        // If deserialized "otherwise", it will not have a datacontext (good)

        synchronized (getObjectStore()) {
            Iterator it = objectStore.getObjectIterator();
            while (it.hasNext()) {
                Persistent object = (Persistent) it.next();
                object.setObjectContext(this);
            }
        }
    }

    // Re-attaches itself to the parent domain with previously stored name.
    //
    // TODO: Andrus 11/7/2005 - this is one of the places where Cayenne
    // serialization relies on shared config... This is bad. We need some
    // sort of thread-local solution that would allow to use an alternative configuration.
    //
    private final void awakeFromDeserialization() {
        if (channel == null && lazyInitParentDomainName != null) {

            // call a setter to ensure EntityResolver is extracted from channel
            setChannel(Configuration.getSharedConfiguration().getDomain(
                    lazyInitParentDomainName));
        }
    }

    /**
     * @since 1.2
     */
    @Override
    public void propertyChanged(
            Persistent object,
            String property,
            Object oldValue,
            Object newValue) {
        graphAction.handlePropertyChange(object, property, oldValue, newValue);
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
     * Returns an object local to this DataContext and matching the ObjectId. If
     * <code>prototype</code> is not null, local object is refreshed with the prototype
     * values.
     * <p>
     * In case you pass a non-null second parameter, you are responsible for setting
     * correct persistence state of the returned local object, as generally there is no
     * way for Cayenne to determine the resulting local object state.
     * 
     * @since 1.2
     */
    @Override
    public Persistent localObject(ObjectId id, Object prototype) {

        // ****** Warning: when changing the code below, don't forget to change
        // CayenneContext's implementation which right now relies on copy/paste "reuse"

        if (id == null) {
            throw new IllegalArgumentException("Null ObjectId");
        }

        // note that per-object ClassDescriptor lookup is needed as even if all
        // objects where fetched as a part of the same query, as they may belong to
        // different subclasses
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

                int state = cachedObject.getPersistenceState();

                // TODO: Andrus, 1/24/2006 implement smart merge for modified objects...
                if (cachedObject != prototype
                        && state != PersistenceState.MODIFIED
                        && state != PersistenceState.DELETED) {

                    descriptor.injectValueHolders(cachedObject);

                    if (prototype != null
                            && ((Persistent) prototype).getPersistenceState() != PersistenceState.HOLLOW) {

                        descriptor.shallowMerge(prototype, cachedObject);

                        if (state == PersistenceState.HOLLOW) {
                            cachedObject.setPersistenceState(PersistenceState.COMMITTED);
                        }
                    }
                }

                return cachedObject;
            }
            // create and merge into a new object
            else {

                Persistent localObject;

                localObject = (Persistent) descriptor.createObject();

                localObject.setObjectContext(this);
                localObject.setObjectId(id);

                getGraphManager().registerNode(id, localObject);

                if (prototype != null
                        && ((Persistent) prototype).getPersistenceState() != PersistenceState.HOLLOW) {
                    localObject.setPersistenceState(PersistenceState.COMMITTED);
                    descriptor.injectValueHolders(localObject);
                    descriptor.shallowMerge(prototype, localObject);
                }
                else {
                    localObject.setPersistenceState(PersistenceState.HOLLOW);
                }

                return localObject;
            }
        }
    }

    protected void fireDataChannelChanged(Object postedBy, GraphDiff changes) {
        super.fireDataChannelChanged(postedBy, changes);
    }
}
