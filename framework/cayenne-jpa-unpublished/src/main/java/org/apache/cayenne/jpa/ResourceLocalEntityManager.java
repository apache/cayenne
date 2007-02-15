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

package org.apache.cayenne.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.Transaction;

public class ResourceLocalEntityManager implements EntityManager, CayenneEntityManager {

    protected EntityTransaction transaction;
    protected ResourceLocalEntityManagerFactory factory;
    protected FlushModeType flushMode;
    protected boolean open;
    protected ObjectContext context;

    public ResourceLocalEntityManager(ObjectContext context,
            ResourceLocalEntityManagerFactory factory) {

        if (factory == null) {
            throw new IllegalArgumentException("Null entity manager factory");
        }

        this.open = true;
        this.context = context;
        this.factory = factory;
    }

    /**
     * Returns a DataChannel of the peer ObjectContext.
     */
    public DataChannel getChannel() {
        return context.getChannel();
    }

    /**
     * Returns parent EntityManagerFactory.
     */
    protected ResourceLocalEntityManagerFactory getFactory() {
        return factory;
    }

    /**
     * Close an application-managed EntityManager. After an EntityManager has been closed,
     * all methods on the EntityManager instance will throw the IllegalStateException
     * except for isOpen, which will return false. This method can only be called when the
     * EntityManager is not associated with an active transaction.
     * 
     * @throws IllegalStateException if the EntityManager is associated with an active
     *             transaction or if the EntityManager is container-managed.
     */
    public void close() {
        checkClosed();

        if (transaction != null && transaction.isActive()) {
            throw new IllegalStateException("Active transaction in progress");
        }

        open = false;
    }

    public boolean isOpen() {
        return open && (factory == null || factory.isOpen());
    }

    public Object getDelegate() {
        return factory.getProvider();
    }

    /**
     * Make an instance managed and persistent.
     * 
     * @param entity an object to be made persistent
     * @throws IllegalArgumentException if not an entity.
     */
    public void persist(Object entity) {
        checkClosed();
        checkEntityType(entity);
        context.registerNewObject(entity);
    }

    /**
     * Merge the state of the given entity into the current persistence context. Cayenne:
     * Is this like localObject(s)?
     * 
     * @param entity
     * @return the instance that the state was merged to
     * @throws IllegalArgumentException if instance is not an entity or is a removed
     *             entity
     */
    public <T> T merge(T entity) {
        checkClosed();
        checkEntityType(entity);
        checkNotRemoved(entity);
        Persistent persistent = (Persistent) entity;
        return (T) context.localObject(persistent.getObjectId(), persistent);
    }

    /**
     * Remove the entity instance.
     * 
     * @param entity
     * @throws IllegalArgumentException if not an entity or if a detached entity.
     */
    public void remove(Object entity) {
        checkClosed();
        checkEntityType(entity);
        checkAttached(entity);
        context.deleteObject((Persistent) entity);
    }

    /**
     * Find by primary key.
     * 
     * @param entityClass
     * @param primaryKey
     * @return the found entity instance or null if the entity does not exist
     * @throws IllegalArgumentException if the first argument does not denote an entity
     *             type or the second argument is not a valid type for that
     */
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        checkClosed();
        checkEntityType(entityClass);
        return (T) DataObjectUtils.objectForPK(context, entityClass, primaryKey);
    }

    /**
     * Get an instance, whose state may be lazily fetched. If the requested instance does
     * not exist in the database, throws EntityNotFoundException when the instance state
     * is first accessed. (The persistence provider runtime is permitted to throw the
     * EntityNotFoundException when getReference is called.) The application should not
     * expect that the instance state will be available upon detachment, unless it was
     * accessed by the application while the entity manager was open.
     * 
     * @param entityClass
     * @param primaryKey
     * @return the found entity instance
     * @throws IllegalArgumentException if the first argument does not denote an entity
     *             type or the second argument is not a valid type for that entity’s
     *             primary key
     * @throws EntityNotFoundException if the entity state cannot be accessed
     */
    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        checkClosed();

        // TODO: force refresh?
        T ref = find(entityClass, primaryKey);

        if (ref == null) {
            throw new EntityNotFoundException("Could not find "
                    + entityClass.toString()
                    + " with primary key value "
                    + primaryKey.toString());
        }

        return ref;
    }

    /**
     * Synchronize the persistence context to the underlying database.
     * 
     * @throws PersistenceException if the flush fails
     */
    public void flush() {
        checkClosed();

        try {
            context.commitChanges();
        }
        catch (CayenneRuntimeException e) {
            throw new PersistenceException(e);
        }
    }

    /**
     * Set the flush mode that applies to all objects contained in the persistence
     * context.
     * 
     * @param flushMode
     */
    public void setFlushMode(FlushModeType flushMode) {
        checkClosed();

        this.flushMode = flushMode;
    }

    /**
     * Get the flush mode that applies to all objects contained in the persistence
     * context.
     * 
     * @return flushMode
     */
    public FlushModeType getFlushMode() {
        checkClosed();

        return flushMode;
    }

    /**
     * Refresh the state of the instance from the database, overwriting changes made to
     * the entity, if any.
     * 
     * @param entity
     * @throws IllegalArgumentException if not an entity or entity is not managed
     * @throws EntityNotFoundException if the entity no longer exists in the database
     */
    public void refresh(Object entity) {
        checkClosed();
        // TODO: Andrus, 2/10/2006 - implement
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Clear the persistence context, causing all managed entities to become detached.
     * Changes made to entities that have not been flushed to the database will not be
     * persisted.
     */
    public void clear() {
        checkClosed();
        // TODO: Andrus, 2/10/2006 - implement
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Check if the instance belongs to the current persistence context.
     * 
     * @throws IllegalArgumentException if not an entity
     */
    public boolean contains(Object entity) {
        checkClosed();
        checkEntityType(entity);

        Persistent p = (Persistent) entity;
        return p.getObjectContext() == context;
    }

    /**
     * Create an instance of Query for executing an EJB QL statement.
     * 
     * @param ejbqlString an EJB QL query string
     * @return the new query instance
     * @throws IllegalArgumentException if query string is not valid
     */
    public Query createQuery(String ejbqlString) {
        checkClosed();

        // TODO: Andrus, 2/10/2006 - implement
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Create an instance of Query for executing a named query (in EJB QL or native SQL).
     * 
     * @param name the name of a query defined in metadata
     * @return the new query instance
     * @throws IllegalArgumentException if a query has not been defined with the given
     *             name
     */
    public Query createNamedQuery(String name) {
        checkClosed();

        return new JpaQuery(context, name);
    }

    public Query createNativeQuery(String sqlString, Class resultClass) {
        checkClosed();
        checkEntityType(resultClass);

        return new JpaNativeQuery(context, sqlString, resultClass);
    }

    /**
     * Create an instance of Query for executing a native SQL statement, e.g., for update
     * or delete.
     * 
     * @param sqlString a native SQL query string
     * @return the new query instance
     */
    public Query createNativeQuery(String sqlString) {
        checkClosed();

        return new JpaNativeQuery(context, sqlString, factory
                .getPersistenceUnitInfo()
                .getPersistenceUnitName());
    }

    /**
     * Create an instance of Query for executing a native SQL query.
     * 
     * @param sqlString a native SQL query string
     * @param resultSetMapping the name of the result set mapping
     * @return the new query instance
     */
    public Query createNativeQuery(String sqlString, String resultSetMapping) {
        checkClosed();

        // TODO: Andrus, 2/10/2006 - implement
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Indicates to the EntityManager that a JTA transaction is active. This method should
     * be called on a JTA application managed EntityManager that was created outside the
     * scope of the active transaction to associate it with the current JTA transaction.
     * <p>
     * This implementation throws a JpaProviderException, as it only supports
     * resource-local operation.
     * 
     * @throws JpaProviderException as this impementation only supports resource-local
     *             operation.
     */
    public void joinTransaction() {
        throw new JpaProviderException(
                "'joinTransaction' is called on a RESOURCE_LOCAL EntityManager");
    }

    public void lock(Object entity, LockModeType lockMode) {
        // TODO: andrus, 8/15/2006 - noop
    }

    /**
     * Return the resource-level transaction object. The EntityTransaction instance may be
     * used serially to begin and commit multiple transactions.
     * 
     * @return EntityTransaction instance
     */
    public EntityTransaction getTransaction() { // note - allowed to be called on a closed
        if (transaction == null) {
            this.transaction = new JpaTransaction(
                    Transaction.internalTransaction(null),
                    this);
        }

        return transaction;
    }

    /**
     * Checks if an entity is attached to the current EntityManager, throwing
     * IllegalArgumentException if not.
     */
    protected void checkAttached(Object entity) throws IllegalArgumentException {
        if (entity instanceof Persistent) {
            Persistent p = (Persistent) entity;
            if (p.getPersistenceState() == PersistenceState.TRANSIENT
                    || p.getObjectContext() == null) {
                throw new IllegalArgumentException("entity is detached: " + entity);
            }
        }
        else {
            throw new IllegalArgumentException("entity must be Persistent: " + entity);
        }
    }

    /**
     * Checks if an entity is not removed in the current EntityManager, throwing
     * IllegalArgumentException if it is.
     */
    protected void checkNotRemoved(Object entity) throws IllegalArgumentException {
        if (entity instanceof Persistent) {
            Persistent p = (Persistent) entity;
            if (p.getPersistenceState() == PersistenceState.DELETED) {
                throw new IllegalArgumentException("entity is removed: " + entity);
            }
        }
    }

    protected void checkEntityType(Class entityClass) throws IllegalArgumentException {
        if (entityClass == null) {
            throw new IllegalArgumentException("Null entity class");
        }

        if (!Persistent.class.isAssignableFrom(entityClass)) {
            throw new IllegalArgumentException("Entity class must be Persistent, got: "
                    + entityClass.getName());
        }
    }

    protected void checkEntityType(Object entity) throws IllegalArgumentException {
        if (entity == null) {
            throw new IllegalArgumentException("Null entity");
        }

        if (!(entity instanceof Persistent)) {
            String className = (entity != null) ? entity.getClass().getName() : "<null>";
            throw new IllegalArgumentException("entity must be Persistent: " + className);
        }
    }

    /**
     * Throws an exception if called on closed factory.
     */
    protected void checkClosed() throws IllegalStateException {
        if (!isOpen()) {
            throw new IllegalStateException(
                    "An attempt to access closed EntityManagerFactory.");
        }
    }
}
