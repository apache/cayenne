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

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.transaction.Status;
import javax.transaction.TransactionSynchronizationRegistry;

import org.apache.cayenne.access.DataDomain;

/**
 * An EntityManagerFactory that registers all EntityManagers that it creates with an
 * active JTA Transaction so that they could flush the object state to the database during
 * commit.
 * 
 */
public class JtaEntityManagerFactory extends ResourceLocalEntityManagerFactory {

    static final String TX_SYNC_REGISTRY_KEY = "java:comp/TransactionSynchronizationRegistry";

    protected TransactionSynchronizationRegistry transactionRegistry;

    /**
     * Non-public constructor used for unit testing.
     */
    JtaEntityManagerFactory(PersistenceUnitInfo unitInfo) {
        super(unitInfo);
    }

    public JtaEntityManagerFactory(Provider provider, DataDomain domain,
            PersistenceUnitInfo unitInfo) {
        super(provider, domain, unitInfo);
    }

    /**
     * Returns JTA 11 TransactionSynchronizationRegistry, looking it up via JNDI on first
     * access, and caching it for the following invocations.
     */
    protected TransactionSynchronizationRegistry getTransactionRegistry() {
        if (transactionRegistry == null) {
            InitialContext jndiContext;
            try {
                jndiContext = new InitialContext();
            }
            catch (NamingException e) {
                throw new JpaProviderException("Error creating JNDI context", e);
            }

            try {
                transactionRegistry = (TransactionSynchronizationRegistry) jndiContext
                        .lookup(TX_SYNC_REGISTRY_KEY);
            }
            catch (NamingException e) {
                throw new JpaProviderException("Failed to look up "
                        + TX_SYNC_REGISTRY_KEY, e);
            }
        }

        return transactionRegistry;
    }

    /**
     * Returns whether there is a JTA transaction in progress.
     */
    protected boolean isActiveTransaction() {
        int txStatus = getTransactionRegistry().getTransactionStatus();
        return txStatus == Status.STATUS_ACTIVE
                || txStatus == Status.STATUS_MARKED_ROLLBACK;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EntityManager createEntityManager(Map map) {
        checkClosed();
        CayenneEntityManager em = new JtaEntityManager(createObjectContext(), this);
        em = new TypeCheckingEntityManager(em);

        if (isActiveTransaction()) {
            em.joinTransaction();
        }

        return em;
    }
}
