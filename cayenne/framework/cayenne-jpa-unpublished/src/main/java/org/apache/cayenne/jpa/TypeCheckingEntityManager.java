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

import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.Persistent;

/**
 * An EntityManager decorator that checks that only properly enhanced entities are passwed
 * to the underlying EntityManager.
 * 
 * @author Andrus Adamchik
 */
// TODO: andrus, 2/18/2007 - in the future this wrapper can also enhance entities on the
// fly, for now it simply does the type checks before passing entities to the underlying
// EM.
public class TypeCheckingEntityManager implements CayenneEntityManager {

    protected CayenneEntityManager entityManager;

    public TypeCheckingEntityManager(CayenneEntityManager entityManager) {
        this.entityManager = entityManager;
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

    protected void enhance(Class entityClass) {

    }

    public void clear() {
        entityManager.clear();
    }

    public void close() {
        entityManager.close();
    }

    public boolean contains(Object entity) {
        checkEntityType(entity);
        return entityManager.contains(entity);
    }

    public Query createNamedQuery(String name) {
        return entityManager.createNamedQuery(name);
    }

    public Query createNativeQuery(String sqlString, Class resultClass) {
        checkEntityType(resultClass);
        return entityManager.createNativeQuery(sqlString, resultClass);
    }

    public Query createNativeQuery(String sqlString, String resultSetMapping) {
        return entityManager.createNativeQuery(sqlString, resultSetMapping);
    }

    public Query createNativeQuery(String sqlString) {
        return entityManager.createNativeQuery(sqlString);
    }

    public Query createQuery(String ejbqlString) {
        return entityManager.createQuery(ejbqlString);
    }

    public <T> T find(Class<T> entityClass, Object primaryKey) {
        checkEntityType(entityClass);
        return entityManager.find(entityClass, primaryKey);
    }

    public void flush() {
        entityManager.flush();
    }

    public DataChannel getChannel() {
        return entityManager.getChannel();
    }

    public Object getDelegate() {
        return entityManager.getDelegate();
    }

    public FlushModeType getFlushMode() {
        return entityManager.getFlushMode();
    }

    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        return entityManager.getReference(entityClass, primaryKey);
    }

    public EntityTransaction getTransaction() {
        return entityManager.getTransaction();
    }

    public boolean isOpen() {
        return entityManager.isOpen();
    }

    public void joinTransaction() {
        entityManager.joinTransaction();
    }

    public void lock(Object entity, LockModeType lockMode) {
        entityManager.lock(entity, lockMode);
    }

    public <T> T merge(T entity) {
        checkEntityType(entity);
        return entityManager.merge(entity);
    }

    public void persist(Object entity) {
        checkEntityType(entity);
        entityManager.persist(entity);
    }

    public void refresh(Object entity) {
        entityManager.refresh(entity);
    }

    public void remove(Object entity) {
        checkEntityType(entity);
        entityManager.remove(entity);
    }

    public void setFlushMode(FlushModeType flushMode) {
        entityManager.setFlushMode(flushMode);
    }
}
