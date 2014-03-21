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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.Select;

/**
 * A Cayenne object facade to a persistent store. Instances of ObjectContext are
 * used in the application code to access Cayenne persistence features.
 * 
 * @since 1.2
 */
public interface ObjectContext extends DataChannel, Serializable {

    /**
     * Returns EntityResolver that stores all mapping information accessible by
     * this ObjectContext.
     */
    EntityResolver getEntityResolver();

    /**
     * Returns a collection of objects that are registered with this
     * ObjectContext and have a state PersistenceState.NEW
     */
    Collection<?> newObjects();

    /**
     * Returns a collection of objects that are registered with this
     * ObjectContext and have a state PersistenceState.DELETED
     */
    Collection<?> deletedObjects();

    /**
     * Returns a collection of objects that are registered with this
     * ObjectContext and have a state PersistenceState.MODIFIED
     */
    Collection<?> modifiedObjects();

    /**
     * Returns a collection of MODIFIED, DELETED or NEW objects.
     */
    Collection<?> uncommittedObjects();

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
     * Creates a new persistent object of a given class scheduled to be inserted
     * to the database on next commit.
     */
    <T> T newObject(Class<T> persistentClass);

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
    void registerNewObject(Object object);

    /**
     * Schedules deletion of a collection of persistent objects.
     * 
     * @throws DeleteDenyException
     *             if a {@link org.apache.cayenne.map.DeleteRule#DENY} delete
     *             rule is applicable for object deletion.
     */
    void deleteObjects(Collection<?> objects) throws DeleteDenyException;

    /**
     * Schedules deletion of one or more persistent objects. Same as
     * {@link #deleteObjects(Collection)} only with a vararg argument list for
     * easier deletion of individual objects.
     * 
     * @throws DeleteDenyException
     *             if a {@link org.apache.cayenne.map.DeleteRule#DENY} delete
     *             rule is applicable for object deletion.
     * @since 3.1
     */
    <T> void deleteObjects(T... objects) throws DeleteDenyException;

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
     * Executes a selecting query, returning a list of persistent objects or
     * data rows.
     */
    List performQuery(Query query);

    /**
     * Executes a selecting query, returning a list of persistent objects or
     * data rows.
     * 
     * @since 3.2
     */
    <T> List<T> select(Select<T> query);

    /**
     * Executes a selecting query, returning either NULL if query matched no
     * objects, or a single object. If query matches more than one object,
     * {@link CayenneRuntimeException} is thrown.
     * 
     * @since 3.2
     */
    <T> T selectOne(Select<T> query);

    /**
     * Creates a ResultIterator based on the provided query and passes it to a
     * callback for processing. The caller does not need to worry about closing
     * the iterator. This method takes care of it.
     * 
     * @since 3.2
     */
    <T> void iterate(Select<T> query, ResultIteratorCallback<T> callback);

    /**
     * Creates a ResultIterator based on the provided query. It is usually
     * backed by an open result set and is useful for processing of large data
     * sets, preserving a constant memory footprint. The caller must wrap
     * iteration in try/finally and close the ResultIterator explicitly. Or use
     * {@link #iterate(Select, ResultIteratorCallback)} as an alternative.
     * 
     * @since 3.2
     */
    <T> ResultIterator<T> iterator(Select<T> query);

    /**
     * Executes any kind of query providing the result in a form of
     * QueryResponse.
     */
    QueryResponse performGenericQuery(Query query);

    /**
     * Returns GraphManager that manages object graph associated with this
     * context.
     */
    GraphManager getGraphManager();

    /**
     * Returns an DataChannel used by this context.
     */
    DataChannel getChannel();

    /**
     * Returns <code>true</code> if there are any modified, deleted or new
     * objects registered with this ObjectContext, <code>false</code> otherwise.
     * 
     * @since 3.0
     */
    boolean hasChanges();

    /**
     * Invalidates a Collection of persistent objects. This operation only
     * applies to the objects already committed to the database and does nothing
     * to the NEW objects. It would remove each object's snapshot from caches
     * and change object's state to HOLLOW. On the next access to this object,
     * the object will be refetched.
     */
    void invalidateObjects(Collection<?> objects);

    /**
     * Invalidates one or more persistent objects. Same as
     * {@link #invalidateObjects(Collection)} only with a vararg argument list
     * for easier invalidation of individual objects. If no arguments are passed
     * to this method, it does nothing.
     * 
     * @since 3.1
     */
    <T> void invalidateObjects(T... objects);

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
}
