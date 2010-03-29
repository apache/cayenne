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
import org.apache.cayenne.query.RefreshQuery;

/**
 * A Cayenne object facade to a persistent store. Instances of ObjectContext are used in
 * the application code to access Cayenne persistence features.
 * 
 * @since 1.2
 */
public interface ObjectContext extends Serializable {

    /**
     * Returns EntityResolver that stores all mapping information accessible by this
     * ObjectContext.
     */
    EntityResolver getEntityResolver();

    /**
     * Returns a collection of objects that are registered with this ObjectContext and
     * have a state PersistenceState.NEW
     */
    Collection<?> newObjects();

    /**
     * Returns a collection of objects that are registered with this ObjectContext and
     * have a state PersistenceState.DELETED
     */
    Collection<?> deletedObjects();

    /**
     * Returns a collection of objects that are registered with this ObjectContext and
     * have a state PersistenceState.MODIFIED
     */
    Collection<?> modifiedObjects();

    /**
     * Returns a collection of MODIFIED, DELETED or NEW objects.
     */
    Collection<?> uncommittedObjects();

    /**
     * Returns an object local to this ObjectContext and matching the ObjectId. If
     * <code>prototype</code> is not null, local object is refreshed with the prototype
     * values.
     * <p>
     * This method can do both "mapping" (i.e. finding an object with the same id in this
     * context) and "synchronization" (i.e. updating the state of the found object with
     * the state of the prototype object).
     * </p>
     */
    Persistent localObject(ObjectId id, Object prototype);

    /**
     * Creates a new persistent object of a given class scheduled to be inserted to the
     * database on next commit.
     */
    <T> T newObject(Class<T> persistentClass);

    /**
     * Registers a transient object with the context. The difference with
     * {@link #newObject(Class)} is that a user creates an object herself, before
     * attaching it to the context, instead of relying on Cayenne to do that.
     * 
     * @param object new object that needs to be made persistent.
     * @since 3.0
     */
    void registerNewObject(Object object);

    /**
     * Schedules a persistent object for deletion on next commit.
     * 
     * @throws DeleteDenyException if a {@link org.apache.cayenne.map.DeleteRule#DENY}
     *             delete rule is applicable for object deletion.
     */
    void deleteObject(Object object) throws DeleteDenyException;

    /**
     * Deletes a collection of objects by repeatedly calling deleteObject safely
     * (avoiding a concurrent modification exception).
     */
    void deleteObjects(Collection<?> objects) throws DeleteDenyException;

    /**
     * A callback method that child Persistent objects are expected to call before
     * accessing property values. This callback allows ObjectContext to "inflate"
     * unresolved objects on demand and also resolve properties that rely on lazy
     * faulting.
     * 
     * @since 3.0
     */
    void prepareForAccess(Persistent object, String property, boolean lazyFaulting);

    /**
     * @deprecated since 3.0 use {@link #prepareForAccess(Persistent, String, boolean)}.
     */
    @Deprecated
    void prepareForAccess(Persistent object, String property);

    /**
     * A callback method that child Persistent objects are expected to call from inside
     * the setter after modifying a value of a persistent property, including "simple" and
     * "arc" properties.
     */
    void propertyChanged(
            Persistent object,
            String property,
            Object oldValue,
            Object newValue);

    /**
     * Flushes all changes to objects in this context to the parent DataChannel, cascading
     * flush operation all the way through the stack, ultimately saving data in the
     * database.
     */
    void commitChanges();

    /**
     * Flushes all changes to objects in this context to the parent DataChannel. Same as
     * {@link #commitChanges()}, but no cascading flush occurs.
     */
    void commitChangesToParent();

    /**
     * Resets all uncommitted changes made to the objects in this ObjectContext, cascading
     * rollback operation all the way through the stack.
     */
    void rollbackChanges();

    /**
     * Resets all uncommitted changes made to the objects in this ObjectContext. Same as
     * {@link #rollbackChanges()()}, but rollback is local to this context and no
     * cascading changes undoing occurs.
     */
    void rollbackChangesLocally();

    /**
     * Executes a selecting query, returning a list of persistent objects or data rows.
     */
    @SuppressWarnings("unchecked")
    List performQuery(Query query);

    /**
     * Executes any kind of query providing the result in a form of QueryResponse.
     */
    QueryResponse performGenericQuery(Query query);

    /**
     * Returns GraphManager that manages object graph associated with this context.
     */
    GraphManager getGraphManager();

    /**
     * Returns an DataChannel used by this context.
     */
    DataChannel getChannel();

    /**
     * Creates and returns a new child ObjectContext.
     * 
     * @since 3.0
     */
    ObjectContext createChildContext();

    /**
     * Returns <code>true</code> if there are any modified, deleted or new objects
     * registered with this ObjectContext, <code>false</code> otherwise.
     * 
     * @since 3.0
     */
    boolean hasChanges();

    /**
     * Invalidates a Collection of persistent objects. This operation only applies to the
     * objects already committed to the database and does nothing to the NEW objects. It
     * would remove each object's snapshot from caches and change object's state to
     * HOLLOW. On the next access to this object, the object will be refetched.
     * 
     * @see RefreshQuery
     */
    void invalidateObjects(Collection objects);

    /**
     * Returns a user-defined property previously set via 'setUserProperty'. Note that it
     * is a caller responsibility to synchronize access to properties.
     * 
     * @since 3.0
     */
    public Object getUserProperty(String key);

    /**
     * Sets a user-defined property. Note that it is a caller responsibility to
     * synchronize access to properties.
     * 
     * @since 3.0
     */
    public void setUserProperty(String key, Object value);
}
