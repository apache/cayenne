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
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TransactionRequiredException;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;

/**
 * Base implementation of a non-JTA EntityManager.
 * 
 * @author Andrus Adamchik
 */
public abstract class JpaEntityManager implements EntityManager {

    protected PersistenceContextType contextType;
    protected FlushModeType flushMode;
    protected JpaEntityManagerFactory factory;
    protected EntityTransaction transaction;
    protected boolean open;
    protected Object delegate;

    /**
     * Creates a new JpaEntityManager, initializing it with a parent factory.
     */
    public JpaEntityManager(JpaEntityManagerFactory factory) {

        if (factory == null) {
            throw new IllegalArgumentException("Null entity manager factory");
        }

        this.factory = factory;
        this.open = true;
    }

    protected PersistenceUnitInfo getPersistenceUnitInfo() {
        return factory.getPersistenceUnitInfo();
    }

    /**
     * Set the lock mode for an entity object contained in the persistence context.
     */
    public abstract void lock(Object entity, LockModeType lockMode);

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

    /**
     * Determine whether the EntityManager is open.
     * 
     * @return true until the EntityManager has been closed.
     */
    public boolean isOpen() {
        return open && (factory == null || factory.isOpen());
    }

    /**
     * Make an instance managed and persistent.
     * 
     * @param entity an object to be made persistent
     * @throws IllegalArgumentException if not an entity.
     * @throws TransactionRequiredException if there is no transaction and the persistence
     *             context is of type PersistenceContextType.TRANSACTION
     */
    public void persist(Object entity) {
        checkClosed();
        checkTransaction();

        persistInternal(entity);
    }

    protected abstract void persistInternal(Object entity);

    /**
     * Merge the state of the given entity into the current persistence context. Cayenne:
     * Is this like localObject(s)?
     * 
     * @param entity
     * @return the instance that the state was merged to
     * @throws IllegalArgumentException if instance is not an entity or is a removed
     *             entity
     * @throws TransactionRequiredException if there is no transaction and the persistence
     *             context is of type PersistenceContextType.TRANSACTION
     */
    public <T> T merge(T entity) {
        checkClosed();
        checkTransaction();
        return mergeInternal(entity);
    }

    protected abstract <T> T mergeInternal(T entity);

    /**
     * Remove the entity instance.
     * 
     * @param entity
     * @throws IllegalArgumentException if not an entity or if a detached entity
     * @throws TransactionRequiredException if there is no transaction and the persistence
     *             context is of type PersistenceContextType.TRANSACTION
     */
    public void remove(Object entity) {
        checkClosed();
        checkTransaction();

        removeInternal(entity);
    }

    protected abstract void removeInternal(Object entity);

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
        return findInternal(entityClass, primaryKey);
    }

    protected abstract <T> T findInternal(Class<T> entityClass, Object primaryKey);

    /**
     * Get an instance, whose state may be lazily fetched. If the requested instance does
     * not exist in the database, throws EntityNotFoundException when the instance state
     * is first accessed. (The persistence provider runtime is permitted to throw the
     * EntityNotFoundException when getReference is called.) The application should not
     * expect that theinstance state will be available upon detachment, unless it was
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
     * @throws TransactionRequiredException if there is no transaction
     * @throws PersistenceException if the flush fails
     */
    public void flush() {
        checkClosed();
        checkTransaction();

        flushInternal();
    }

    protected abstract void flushInternal();

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
     * @throws TransactionRequiredException if there is no transaction and the persistence
     *             context is of type PersistenceContextType.TRANSACTION
     * @throws EntityNotFoundException if the entity no longer exists in the database
     */
    public void refresh(Object entity) {
        checkClosed();
        refreshInternal(entity);
    }

    protected abstract void refreshInternal(Object entity);

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
        return containsInternal(entity);
    }

    protected abstract boolean containsInternal(Object entity);

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
    public abstract Query createNamedQuery(String name);

    /**
     * Create an instance of Query for executing a native SQL statement, e.g., for update
     * or delete.
     * 
     * @param sqlString a native SQL query string
     * @return the new query instance
     */
    public abstract Query createNativeQuery(String sqlString);

    /**
     * Create an instance of Query for executing a native SQL query.
     * 
     * @param sqlString a native SQL query string
     * @param resultClass the class of the resulting instance(s)
     * @return the new query instance
     */
    public abstract Query createNativeQuery(String sqlString, Class resultClass);

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
     * Return the resource-level transaction object. The EntityTransaction instance may be
     * used serially to begin and commit multiple transactions.
     * 
     * @return EntityTransaction instance
     * @throws IllegalStateException if invoked on a JTA EntityManager.
     */
    public EntityTransaction getTransaction() {
        // note - allowed to be called on a closed EM

        if (factory.getUnitInfo().getTransactionType() == PersistenceUnitTransactionType.JTA) {
            throw new IllegalStateException(
                    "'getTransaction' is called on a JTA EntityManager");
        }

        if (transaction == null) {
            this.transaction = createResourceLocalTransaction();
        }

        return transaction;
    }

    /**
     * A method that creates a new resource-local transaction.
     */
    protected abstract EntityTransaction createResourceLocalTransaction();

    /**
     * Indicates to the EntityManager that a JTA transaction is active. This method should
     * be called on a JTA application managed EntityManager that was created outside the
     * scope of the active transaction to associate it with the current JTA transaction.
     * 
     * @throws TransactionRequiredException if there is no transaction.
     */
    public abstract void joinTransaction();

    /**
     * Returns the underlying provider object for the EntityManager, if available.
     */
    public Object getDelegate() {
        return delegate;
    }

    /**
     * Sets the underlying provider object for the EntityManager.
     */
    public void setDelegate(Object delegate) {
        this.delegate = delegate;
    }

    /*
     * Throws an exception if called on closed factory.
     */
    protected void checkClosed() throws IllegalStateException {
        if (!isOpen()) {
            throw new IllegalStateException(
                    "An attempt to access closed EntityManagerFactory.");
        }
    }

    /**
     * @throws TransactionRequiredException if there is no transaction and the persistence
     *             context is of type PersistenceContextType.TRANSACTION
     */
    protected void checkTransaction() throws TransactionRequiredException {
        if ((transaction == null) && (contextType == PersistenceContextType.TRANSACTION)) {
            throw new TransactionRequiredException();
        }
    }

}
