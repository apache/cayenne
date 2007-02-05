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
package org.apache.cayenne.jpa.itest.ch5;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.cayenne.itest.jpa.ItestSetup;
import org.apache.cayenne.itest.jpa.JpaTestCase;
import org.apache.cayenne.jpa.itest.ch5.entity.SimpleEntity;
import org.apache.geronimo.transaction.jta11.GeronimoTransactionManagerJTA11;
import org.apache.openejb.client.LocalInitialContextFactory;
import org.apache.openejb.persistence.JtaEntityManager;
import org.apache.openejb.persistence.JtaEntityManagerRegistry;

public class _5_5_1_JtaEntityManagerTest extends JpaTestCase {

    public void testPersist() throws Exception {

        getDbHelper().deleteAll("SimpleEntity");

        GeronimoTransactionManagerJTA11 tm = new GeronimoTransactionManagerJTA11();
        System.setProperty(
                Context.INITIAL_CONTEXT_FACTORY,
                LocalInitialContextFactory.class.getName());

        // somehow OpenEJB LocalInitialContextFactory requires 2 IC's to be initilaized to
        // fully bootstrap the environment
        new InitialContext();
        new InitialContext().bind("java:comp/TransactionSynchronizationRegistry", tm);

        EntityManagerFactory factory = ItestSetup
                .getInstance()
                .createEntityManagerFactory();
        JtaEntityManagerRegistry registry = new JtaEntityManagerRegistry(tm);

        tm.begin();

        EntityManager entityManager = new JtaEntityManager(
                registry,
                factory,
                new Properties(),
                false);

        SimpleEntity e = new SimpleEntity();
        e.setProperty1("XXX");
        entityManager.persist(e);
        tm.commit();

        assertEquals(1, getDbHelper().getRowCount("SimpleEntity"));
    }

    // TODO: andrus, 1/3/2007 - implement - need to emulate the container environment
    // public void testPersistTransactionRequiredException() throws Exception {
    // // throws TransactionRequiredException if invoked on a
    // // container-managed entity manager of type
    // // PersistenceContextType.TRANSACTION and there is
    // // no transaction.
    //
    // EntityManager em = getEntityManager();
    //
    // SimpleEntity e = new SimpleEntity();
    // e.setProperty1("XXX");
    //
    // try {
    // em.persist(e);
    // em.getTransaction().commit();
    // fail("Must have thrown TransactionRequiredException");
    // }
    // catch (TransactionRequiredException ex) {
    // // expected
    // }
    // }

}
