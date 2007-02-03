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

import java.util.Map;

import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.Transaction;

public class ResourceLocalEntityManager extends JpaEntityManager {

    private ObjectContext context;

    public ResourceLocalEntityManager(ObjectContext context, JpaEntityManagerFactory factory,
            Map parameters) {
        super(factory);
        this.context = context;
    }

    @Override
    protected EntityTransaction createResourceLocalTransaction() {
        return new JpaTransaction(Transaction.internalTransaction(null), this);
    }

    @Override
    protected void persistInternal(Object entity) {
        checkEntityType(entity);
        context.registerNewObject(entity);
    }

    @Override
    protected <T> T mergeInternal(T entity) {
        checkEntityType(entity);
        checkNotRemoved(entity);
        Persistent persistent = (Persistent) entity;
        return (T) context.localObject(persistent.getObjectId(), persistent);
    }

    @Override
    protected void removeInternal(Object entity) {
        checkEntityType(entity);
        checkAttached(entity);
        context.deleteObject((Persistent) entity);
    }

    @Override
    protected <T> T findInternal(Class<T> entityClass, Object primaryKey) {
        checkEntityType(entityClass);
        checkIdType(entityClass, primaryKey);
        return (T) DataObjectUtils.objectForPK(context, entityClass, primaryKey);
    }

    @Override
    protected void flushInternal() {
        try {
            context.commitChanges();
        }
        catch (CayenneRuntimeException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    protected void refreshInternal(Object entity) {
        // TODO: Andrus, 2/10/2006 - implement
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    protected boolean containsInternal(Object entity) {
        checkEntityType(entity);

        Persistent p = (Persistent) entity;
        return p.getObjectContext() == context;
    }

    @Override
    public Query createNamedQuery(String name) {
        checkClosed();

        return new JpaQuery(context, name);
    }

    @Override
    public Query createNativeQuery(String sqlString, Class resultClass) {
        checkClosed();
        checkEntityType(resultClass);

        return new JpaNativeQuery(context, sqlString, resultClass);
    }

    @Override
    public Query createNativeQuery(String sqlString) {
        checkClosed();

        return new JpaNativeQuery(context, sqlString, getPersistenceUnitInfo()
                .getPersistenceUnitName());
    }

    @Override
    public void joinTransaction() {
        // TODO: andrus, 7/24/2006 - noop
    }

    @Override
    public void lock(Object entity, LockModeType lockMode) {
        // TODO: andrus, 8/15/2006 - noop
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
    
    protected void checkIdType(Class entityClass, Object id) {
        
    }
}
