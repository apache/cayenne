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
package org.apache.cayenne.itest.jpa;

import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.TransactionSynchronizationRegistry;

import org.apache.geronimo.transaction.jta11.GeronimoTransactionManagerJTA11;
import org.apache.openejb.persistence.JtaEntityManager;
import org.apache.openejb.persistence.JtaEntityManagerRegistry;

public class ContainerEntityManagerCase extends JpaTestCase {

    protected EntityManager entityManager;

    @Override
    protected void setUp() throws Exception {

        TransactionSynchronizationRegistry tm = new GeronimoTransactionManagerJTA11();
        EntityManagerFactory factory = ItestSetup
                .getInstance()
                .createEntityManagerFactory();
        JtaEntityManagerRegistry registry = new JtaEntityManagerRegistry(tm);
        entityManager = new JtaEntityManager(registry, factory, new Properties(), false);
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }
}
