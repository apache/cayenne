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

import junit.framework.TestCase;

public class ResourceLocalEntityManagerTest extends TestCase {

    public void testOpenClose() throws Exception {
        ResourceLocalEntityManagerFactory factory = new ResourceLocalEntityManagerFactory(
                new MockPersistenceUnitInfo()) {

            @Override
            public boolean isOpen() {
                return true;
            }
        };

        final EntityTransaction tx = new MockEntityTransaction() {

            @Override
            public boolean isActive() {
                return true;
            }
        };

        ResourceLocalEntityManager m = new ResourceLocalEntityManager(
                new MockObjectContext(),
                factory) {

            @Override
            public EntityTransaction getTransaction() {
                this.transaction = tx;
                return tx;
            }
        };

        assertTrue(m.isOpen());
        m.close();
        assertFalse(m.isOpen());

        // check that all methods throw ... or at least some :-)
        try {
            m.close();
            fail("Closed EntityManager is supposed to throw");
        }
        catch (IllegalStateException e) {
            // expected
        }

        try {
            m.contains(new Object());
            fail("Closed EntityManager is supposed to throw");
        }
        catch (IllegalStateException e) {
            // expected
        }

        try {
            m.createNamedQuery("X");
            fail("Closed EntityManager is supposed to throw");
        }
        catch (IllegalStateException e) {
            // expected
        }

        try {
            m.createNativeQuery("SELECT * FROM A");
            fail("Closed EntityManager is supposed to throw");
        }
        catch (IllegalStateException e) {
            // expected
        }

        try {
            m.setFlushMode(FlushModeType.AUTO);
            fail("Closed EntityManager is supposed to throw");
        }
        catch (IllegalStateException e) {
            // expected
        }
    }

    public void testCloseActiveTransactionInProgress() {
        ResourceLocalEntityManagerFactory factory = new ResourceLocalEntityManagerFactory(
                new MockPersistenceUnitInfo()) {

            @Override
            public boolean isOpen() {
                return true;
            }
        };

        final EntityTransaction tx = new MockEntityTransaction() {

            @Override
            public boolean isActive() {
                return true;
            }
        };

        ResourceLocalEntityManager m = new ResourceLocalEntityManager(
                new MockObjectContext(),
                factory) {

            @Override
            public EntityTransaction getTransaction() {
                this.transaction = tx;
                return tx;
            }
        };

        assertTrue(m.isOpen());

        // make sure we trigger transaction creation
        assertNotNull(m.getTransaction());

        try {
            m.close();
            fail("EntityManager is supposed to throw on 'close' when it has transaction in progress");
        }
        catch (IllegalStateException e) {
            // expected
        }
    }

    public void testCloseFactoryClosed() {
        final boolean[] factoryCloseState = new boolean[1];

        ResourceLocalEntityManagerFactory factory = new ResourceLocalEntityManagerFactory(
                new MockPersistenceUnitInfo()) {

            @Override
            public boolean isOpen() {
                return !factoryCloseState[0];
            }
        };

        ResourceLocalEntityManager m = new ResourceLocalEntityManager(
                new MockObjectContext(),
                factory);

        assertTrue(m.isOpen());
        factoryCloseState[0] = true;
        assertFalse(m.isOpen());
    }
}
