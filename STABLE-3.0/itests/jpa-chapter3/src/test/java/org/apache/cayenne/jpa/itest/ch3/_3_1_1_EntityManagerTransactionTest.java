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
package org.apache.cayenne.jpa.itest.ch3;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.apache.cayenne.itest.jpa.ItestSetup;
import org.apache.cayenne.itest.jpa.JpaTestCase;
import org.apache.cayenne.jpa.itest.ch3.entity.SimpleEntity;

public class _3_1_1_EntityManagerTransactionTest extends JpaTestCase {

    public void testGetTransaction() throws Exception {
        getDbHelper().deleteAll("SimpleEntity");

        EntityManager entityManager = ItestSetup.getInstance().createEntityManager();

        try {

            EntityTransaction tx = entityManager.getTransaction();

            // Per spec, The EntityTransaction instance may be used serially to
            // begin and commit multiple transactions

            tx.begin();
            SimpleEntity e1 = new SimpleEntity();
            e1.setProperty1("X");
            entityManager.persist(e1);
            tx.commit();

            tx.begin();
            SimpleEntity e2 = new SimpleEntity();
            e2.setProperty1("Y");
            entityManager.persist(e2);
            tx.commit();
        }
        finally {
            entityManager.close();
        }

        assertEquals(2, getDbHelper().getRowCount("SimpleEntity"));
    }
}
