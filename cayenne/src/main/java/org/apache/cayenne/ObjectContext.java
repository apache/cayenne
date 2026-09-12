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

package org.apache.cayenne;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.Select;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A Cayenne object facade to a persistent store. Instances of ObjectContext are
 * used in the application code to access Cayenne persistence features.
 *
 * @since 1.2
 */
public interface ObjectContext {

    /**
     * Returns a DataChannel that this context uses to access the persistent store.
     */
    DataChannel getChannel();

    /**
     * Returns EntityResolver that stores all mapping information accessible by
     * this ObjectContext.
     */
    EntityResolver getEntityResolver();

    /**
     * Returns a collection of objects that are registered with this
     * ObjectContext and have a state PersistenceState.NEW
     */
    default Collection<Persistent> newObjects() {
        return getObjectStore().objectsInState(PersistenceState.NEW);
    }

    /**
     * Returns a collection of objects that are registered with this
     * ObjectContext and have a state PersistenceState.DELETED
     */
    default Collection<Persistent> deletedObjects() {
        return getObjectStore().objectsInState(PersistenceState.DELETED);
    }

    /**
     * Returns a collection of objects that are registered with this
     * ObjectContext and have a state PersistenceState.MODIFIED
     */
    default Collection<Persistent> modifiedObjects() {
        return getObjectStore().objectsInState(PersistenceState.MODIFIED);
    }

    /**
     * Returns a collection of MODIFIED, DELETED or NEW objects.
     */
    Collection<Persistent> uncommittedObjects();

    /**
     * Returns a local copy of 'objectFromAnotherContext' object. "Local" means
     * that the returned object is registered in this context. If the local
     * object hasn't been previously cached in this context, a hollow object is
     * created and returned to the caller. No DB query is performed to resolve
     * an object.
     * <p>
     * Note that passing an object with a non-existing id, may later result in
     * FaultFailureException on attempt to read returned object properties.
     * 
     * @since 3.1
     */
    <T extends Persistent> T localObject(T objectFromAnotherContext);

    /**
     * Returns an object matching a single-column primary key, or null if there's no such object. If the entity is
     * mapped with a compound PK, CayenneRuntimeException is thrown.
     * <p>
     * A resolved object registered in this context is returned without a query. Otherwise the object is looked up in
     * the caches down the DataChannel stack, and then in the database.
     *
     * @since 5.0
     */
    @SuppressWarnings("unchecked")
    default <T extends Persistent> T objectForPK(Class<T> type, Object pk) {
        return (T) objectForPK(singleColumnId(entityForClass(type), pk));
    }

    /**
     * Returns an object matching a primary key, or null if there's no such object. The PK map parameter should use
     * database PK column names as keys.
     * <p>
     * A resolved object registered in this context is returned without a query. Otherwise the object is looked up in
     * the caches down the DataChannel stack, and then in the database.
     *
     * @since 5.0
     */
    @SuppressWarnings("unchecked")
    default <T extends Persistent> T objectForPK(Class<T> type, Map<String, ?> pk) {
        return (T) objectForPK(ObjectId.of(entityForClass(type).getName(), pk));
    }

    /**
     * Returns an object matching a single-column primary key, or null if there's no such object. If the entity is
     * mapped with a compound PK, CayenneRuntimeException is thrown.
     * <p>
     * A resolved object registered in this context is returned without a query. Otherwise the object is looked up in
     * the caches down the DataChannel stack, and then in the database.
     *
     * @since 5.0
     */
    default Persistent objectForPK(String entityName, Object pk) {
        Objects.requireNonNull(entityName, "Null entity name");

        ObjEntity entity = getEntityResolver().getObjEntity(entityName);
        if (entity == null) {
            throw new CayenneRuntimeException("Non-existent ObjEntity: %s", entityName);
        }

        return objectForPK(singleColumnId(entity, pk));
    }

    /**
     * Returns an object matching a primary key, or null if there's no such object. The PK map parameter should use
     * database PK column names as keys.
     * <p>
     * A resolved object registered in this context is returned without a query. Otherwise the object is looked up in
     * the caches down the DataChannel stack, and then in the database.
     *
     * @since 5.0
     */
    default Persistent objectForPK(String entityName, Map<String, ?> pk) {
        return objectForPK(ObjectId.of(entityName, pk));
    }

    /**
     * Returns an object matching an ObjectId, or null if there's no such object. The id may be that of a superentity
     * of the actual object entity.
     * <p>
     * A resolved object registered in this context is returned without a query. Otherwise the object is looked up in
     * the caches down the DataChannel stack, and then in the database.
     *
     * @throws FaultFailureException if more than one row matches the id.
     * @since 5.0
     */
    Persistent objectForPK(ObjectId id);

    private ObjEntity entityForClass(Class<?> persistentClass) {
        Objects.requireNonNull(persistentClass, "Null persistent class");

        ObjEntity entity = getEntityResolver().getObjEntity(persistentClass);
        if (entity == null) {
            throw new CayenneRuntimeException("Unmapped persistent class: %s", persistentClass.getName());
        }
        return entity;
    }

    private static ObjectId singleColumnId(ObjEntity entity, Object pk) {
        Objects.requireNonNull(pk, "Null PK");

        Collection<String> pkAttributes = entity.getPrimaryKeyNames();
        if (pkAttributes.size() != 1) {
            throw new CayenneRuntimeException("PK of %s contains %d columns, expected 1.",
                    entity.getName(), pkAttributes.size());
        }

        return ObjectId.of(entity.getName(), pkAttributes.iterator().next(), pk);
    }

    /**
     * Creates a new persistent object of a given class scheduled to be inserted
     * to the database on next commit.
     */
    <T extends Persistent> T newObject(Class<T> persistentClass);

    /**
     * Creates a new persistent object of a given entity scheduled to be inserted to the database on next commit.
     * The object class is determined from the mapped entity. In most cases {@link #newObject(Class)} should be
     * used instead, but this method is helpful when generic persistent classes are used.
     *
     * @since 5.0
     */
    Persistent newObject(String entityName);

    /**
     * Registers a transient object with the context. The difference with
     * {@link #newObject(Class)} is that a user creates an object herself,
     * before attaching it to the context, instead of relying on Cayenne to do
     * that.
     * 
     * @param object
     *            new object that needs to be made persistent.
     * @since 3.0
     */
    void registerNewObject(Persistent object);

    /**
     * Schedules deletion of a persistent object.
     *
     * @throws DeleteDenyException
     *             if a {@link org.apache.cayenne.map.DeleteRule#DENY} delete
     *             rule is applicable for object deletion.
     */
    default void deleteObject(Persistent object) throws DeleteDenyException {
        deleteObjects(object);
    }

    /**
     * Schedules deletion of a collection of persistent objects.
     *
     * @throws DeleteDenyException
     *             if a {@link org.apache.cayenne.map.DeleteRule#DENY} delete
     *             rule is applicable for object deletion.
     */
    default void deleteObjects(Collection<? extends Persistent> objects) throws DeleteDenyException {
        // toArray() copies, which also guards against ConcurrentModificationException when delete rules modify the
        // source collection (e.g. a to-many list) mid-iteration
        deleteObjects(objects.toArray(new Persistent[0]));
    }

    /**
     * Schedules deletion of one or more persistent objects.
     *
     * @throws DeleteDenyException
     *             if a {@link org.apache.cayenne.map.DeleteRule#DENY} delete
     *             rule is applicable for object deletion.
     * @since 3.1
     */
    <T extends Persistent> void deleteObjects(T... objects) throws DeleteDenyException;

    /**
     * A callback method that child Persistent objects are expected to call
     * before accessing property values. This callback allows ObjectContext to
     * "inflate" unresolved objects on demand and also resolve properties that
     * rely on lazy faulting.
     * 
     * @since 3.0
     */
    void prepareForAccess(Persistent object, String property, boolean lazyFaulting);

    /**
     * A callback method that child Persistent objects are expected to call from
     * inside the setter after modifying a value of a persistent property,
     * including "simple" and "arc" properties.
     */
    void propertyChanged(Persistent object, String property, Object oldValue, Object newValue);

    /**
     * Flushes all changes to objects in this context to the parent DataChannel,
     * cascading flush operation all the way through the stack, ultimately
     * saving data in the database.
     */
    void commitChanges();

    /**
     * Flushes all changes to objects in this context to the parent DataChannel.
     * Same as {@link #commitChanges()}, but no cascading flush occurs.
     */
    void commitChangesToParent();

    /**
     * Resets all uncommitted changes made to the objects in this ObjectContext,
     * cascading rollback operation all the way through the stack.
     */
    void rollbackChanges();

    /**
     * Resets all uncommitted changes made to the objects in this ObjectContext.
     * Same as {@link #rollbackChanges()}, but rollback is local to this
     * context and no cascading changes undoing occurs.
     */
    void rollbackChangesLocally();

    /**
     * Executes a selecting query, returning a list of persistent objects or data rows.
     *
     * @throws ClassCastException if the query does not implement {@link Select}
     * @deprecated use {@link #select(Select)}. All selecting queries implement {@link Select}.
     */
    @Deprecated(since = "5.0", forRemoval = true)
    default List performQuery(Query query) {
        return select((Select<?>) query);
    }

    /**
     * Executes a selecting query, returning a list of persistent objects or
     * data rows.
     * 
     * @since 4.0
     */
    <T> List<T> select(Select<T> query);

    /**
     * Executes a selecting query, returning either NULL if query matched no
     * objects, or a single object. If query matches more than one object,
     * {@link CayenneRuntimeException} is thrown.
     * 
     * @since 4.0
     */
    <T> T selectOne(Select<T> query);

    /**
     * Selects a single object using provided query. The query itself can
     * match any number of objects, but will return only the first one. It
     * returns null if no objects were matched.
     * <p>
     * If it matched more than one object, the first object from the list is
     * returned. This makes 'selectFirst' different from
     * {@link #selectOne(Select)}, which would throw in this situation.
     * 'selectFirst' is useful e.g. when the query is ordered and we only want
     * to see the first object (e.g. "most recent news article"), etc.
     * <p>
     * Selecting the first object via "Select.selectFirst(ObjectContext)"
     * is more comprehensible than selecting via "ObjectContext.selectFirst(Select)",
     * because implementations of "Select" set fetch size limit to one.
     *
     * @since 4.0
     */
    <T> T selectFirst(Select<T> query);

    /**
     * Creates a ResultIterator based on the provided query and passes it to a
     * callback for processing. The caller does not need to worry about closing
     * the iterator. This method takes care of it.
     * 
     * @since 4.0
     */
    <T> void iterate(Select<T> query, ResultIteratorCallback<T> callback);

    /**
     * Creates a ResultIterator based on the provided query. It is usually
     * backed by an open result set and is useful for processing of large data
     * sets, preserving a constant memory footprint. The caller must wrap
     * iteration in try/finally (or try-with-resources for Java 1.7 and higher) and
     * close the ResultIterator explicitly.
     * Or use {@link #iterate(Select, ResultIteratorCallback)} as an alternative.
     * 
     * @since 4.0
     */
    <T> ResultIterator<T> iterator(Select<T> query);

    /**
     * Creates a ResultBatchIterator based on the provided query and batch size. It is usually
     * backed by an open result set and is useful for processing of large data
     * sets, preserving a constant memory footprint. The caller must wrap
     * iteration in try/finally (or try-with-resources for Java 1.7 and higher) and
     * close the ResultBatchIterator explicitly.
     *
     * @since 4.0
     */
    <T> ResultBatchIterator<T> batchIterator(Select<T> query, int size);

    /**
     * Executes any kind of query, returning all of its result sets, update counts, iterators and OUT parameters as a
     * list of {@link QueryResultItem}s in the order they were produced.
     */
    List<QueryResultItem> execute(Query query);

    /**
     * Returns the store of persistent objects registered with this context.
     *
     * @since 5.0
     */
    ObjectStore getObjectStore();

    /**
     * Returns <code>true</code> if there are any modified, deleted or new
     * objects registered with this ObjectContext, <code>false</code> otherwise.
     * 
     * @since 3.0
     */
    default boolean hasChanges() {
        return getObjectStore().hasChanges();
    }

    /**
     * Invalidates a Collection of persistent objects. This operation only
     * applies to the objects already committed to the database and does nothing
     * to the NEW objects. It would remove each object's snapshot from caches
     * and change object's state to HOLLOW. On the next access to this object,
     * the object will be refetched.
     */
    void invalidateObjects(Collection<? extends Persistent> objects);

    /**
     * Invalidates one or more persistent objects. Same as
     * {@link #invalidateObjects(Collection)} only with a vararg argument list
     * for easier invalidation of individual objects. If no arguments are passed
     * to this method, it does nothing.
     * 
     * @since 3.1
     */
    <T extends Persistent> void invalidateObjects(T... objects);

    /**
     * Returns a user-defined property previously set via 'setUserProperty'.
     * Concurrent access to properties does not require any special
     * synchronization
     * 
     * @since 3.0
     */
    Object getUserProperty(String key);

    /**
     * Sets a user-defined property. Concurrent access to properties does not
     * require any special synchronization
     * 
     * @since 3.0
     */
    void setUserProperty(String key, Object value);

    /**
     * Removes a user-defined property.
     *
     * @since 5.0
     */
    void removeUserProperty(String key);

    /**
     * Removes all user-defined properties.
     *
     * @since 5.0
     */
    void clearUserProperties();
}
