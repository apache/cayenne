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

import javax.persistence.EntityManager;

import org.apache.cayenne.itest.jpa.ItestSetup;

/**
 * A TestCase superclass that provides an entity manager and transaction management.
 * 
 */
public class EntityManagerCase extends JpaTestCase {

    protected EntityManager entityManager;

    @Override
    protected void setUp() throws Exception {
        entityManager = ItestSetup.getInstance().createEntityManager();
        entityManager.getTransaction().begin();
    }

    @Override
    protected void tearDown() throws Exception {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
        entityManager.close();
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    protected void setEntityManager(EntityManager em) {
        this.entityManager = em;
    }
}
