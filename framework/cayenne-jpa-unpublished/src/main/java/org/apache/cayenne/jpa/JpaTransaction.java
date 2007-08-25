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

import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.access.Transaction;

/**
 * A JPA wrapper around a Cayenne Transaction. For more info see <a
 * href="http://cayenne.apache.org/doc/understanding-transactions.html">this page</a>.
 */
public class JpaTransaction implements EntityTransaction {

    protected EntityManager entityManager;
    protected Transaction transaction;
    protected boolean rollbackOnly;

    public JpaTransaction(EntityManager entityManager) {
        this.entityManager = entityManager;
        reset();
    }
    
    /**
     * Creates a new internal Cayenne transaction.
     */
    protected void reset() {
        this.transaction = Transaction.internalTransaction(null);
    }

    /**
     * Start a resource transaction.
     * 
     * @throws IllegalStateException if isActive() is true.
     */
    public void begin() {
        if (isActive()) {
            throw new IllegalStateException("transaction active");
        }

        transaction.begin();
    }

    /**
     * Commit the current transaction, writing any unflushed changes to the database.
     * 
     * @throws IllegalStateException if isActive() is false.
     * @throws PersistenceException if the commit fails.
     */
    public void commit() {
        if (!isActive()) {
            throw new IllegalStateException("transaction not active");
        }

        try {
            entityManager.flush();
            transaction.commit();
        }
        catch (SQLException e) {
            throw new PersistenceException(e.getMessage(), e);
        }
        catch (CayenneException e) {
            throw new PersistenceException(e.getMessage(), e);
        }
        
        reset();
    }

    /**
     * Roll back the current transaction.
     * 
     * @throws IllegalStateException if isActive() is false.
     * @throws PersistenceException if an unexpected error condition is encountered.
     */
    public void rollback() {
        if (!isActive()) {
            throw new IllegalStateException("transaction not active");
        }

        try {
            transaction.rollback();
        }
        catch (SQLException e) {
            throw new PersistenceException(e.getMessage(), e);
        }
        catch (CayenneException e) {
            throw new PersistenceException(e.getMessage(), e);
        }
        
        reset();
    }

    /**
     * Indicate whether a transaction is in progress.
     * 
     * @throws PersistenceException if an unexpected error condition is encountered.
     */
    public boolean isActive() {
        return (transaction.getStatus() == Transaction.STATUS_ACTIVE);
    }

    public boolean getRollbackOnly() {
        return rollbackOnly;
    }

    public void setRollbackOnly() {
        rollbackOnly = true;
    }
}
