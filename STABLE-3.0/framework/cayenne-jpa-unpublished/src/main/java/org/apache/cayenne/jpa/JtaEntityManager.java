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
import javax.persistence.TransactionRequiredException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import org.apache.cayenne.ObjectContext;

/**
 * An EntityManager that can participate in JTA transactions.
 * 
 */
public class JtaEntityManager extends ResourceLocalEntityManager {

    protected Object currentTxKey;

    public JtaEntityManager(ObjectContext context, JtaEntityManagerFactory factory) {
        super(context, factory);
    }

    private JtaEntityManagerFactory getJtaFactory() {
        return (JtaEntityManagerFactory) getFactory();
    }

    /**
     * @throws IllegalStateException, as this entity manager is of JTA kind.
     */
    public EntityTransaction getTransaction() {
        throw new IllegalStateException(
                "'getTransaction' is called on a JTA EntityManager");
    }

    /**
     * Indicates to the EntityManager that a JTA transaction is active. This method should
     * be called on a JTA application managed EntityManager that was created outside the
     * scope of the active transaction to associate it with the current JTA transaction.
     * 
     * @throws TransactionRequiredException if there is no transaction.
     */
    @Override
    public void joinTransaction() {
        if (currentTxKey == null) {
            TransactionSynchronizationRegistry registry = getJtaFactory()
                    .getTransactionRegistry();
            registry.registerInterposedSynchronization(new TransactionBinding());
            currentTxKey = registry.getTransactionKey();
        }
    }

    /**
     * @throws TransactionRequiredException if there is no transaction.
     */
    @Override
    public void persist(Object entity) {
        checkTransaction();
        super.persist(entity);
    }

    /**
     * @throws TransactionRequiredException if there is no transaction.
     */
    @Override
    public <T> T merge(T entity) {
        checkTransaction();
        return super.merge(entity);
    }

    /**
     * @throws TransactionRequiredException if there is no transaction.
     */
    @Override
    public void remove(Object entity) {
        checkTransaction();
        super.remove(entity);
    }

    /**
     * @throws TransactionRequiredException if there is no transaction.
     */
    @Override
    public void refresh(Object entity) {
        checkTransaction();
        super.refresh(entity);
    }

    /**
     * @throws TransactionRequiredException if there is no transaction.
     */
    public void flush() {
        checkTransaction();
        super.flush();
    };

    /**
     * @throws TransactionRequiredException if there is no transaction in progress.
     */
    protected void checkTransaction() throws TransactionRequiredException {
        if (!getJtaFactory().isActiveTransaction()) {
            throw new TransactionRequiredException();
        }
    }

    class TransactionBinding implements Synchronization {

        public void afterCompletion(int status) {
            if (status != Status.STATUS_COMMITTED) {
                clear();
            }

            currentTxKey = null;
        }

        public void beforeCompletion() {
            flush();
        }
    }
}
