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

import java.util.Collections;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitTransactionType;

import junit.framework.TestCase;

import org.apache.cayenne.jpa.conf.JpaDataSourceFactory;

public class ProviderTest extends TestCase {

    public void testCreateEntityManagerFactory() {
        // JTA Unit
        final JpaUnit u1 = new JpaUnit() {

            @Override
            public void addTransformer(ClassTransformer transformer) {
            }

            @Override
            JpaDataSourceFactory getJpaDataSourceFactory() {
                return new MockJpaDataSourceFactory();
            }

            @Override
            public PersistenceUnitTransactionType getTransactionType() {
                return PersistenceUnitTransactionType.JTA;
            }
        };
        u1.setPersistenceUnitName("U1");

        Provider p1 = new Provider() {

            @Override
            protected JpaUnit loadUnit(String emName) {
                return u1;
            }
        };

        EntityManagerFactory emf1 = p1.createEntityManagerFactory(
                "U1",
                Collections.EMPTY_MAP);
        assertTrue(emf1 instanceof JtaEntityManagerFactory);
    }

    public void testCreateContainerEntityManagerFactory() {
        Provider p = new Provider();

        // JTA Unit
        JpaUnit u1 = new JpaUnit() {
            @Override
            public void addTransformer(ClassTransformer transformer) {
            }

            @Override
            JpaDataSourceFactory getJpaDataSourceFactory() {
                return new MockJpaDataSourceFactory();
            }

            @Override
            public PersistenceUnitTransactionType getTransactionType() {
                return PersistenceUnitTransactionType.JTA;
            }
        };
        u1.setPersistenceUnitName("U1");

        EntityManagerFactory emf1 = p.createContainerEntityManagerFactory(
                u1,
                Collections.EMPTY_MAP);
        assertTrue(emf1 instanceof JtaEntityManagerFactory);

        // RESOURCE_LOCAL Unit
        JpaUnit u2 = new JpaUnit() {
            @Override
            public void addTransformer(ClassTransformer transformer) {
            }

            @Override
            JpaDataSourceFactory getJpaDataSourceFactory() {
                return new MockJpaDataSourceFactory();
            }

            @Override
            public PersistenceUnitTransactionType getTransactionType() {
                return PersistenceUnitTransactionType.RESOURCE_LOCAL;
            }
        };
        u2.setPersistenceUnitName("U2");

        EntityManagerFactory emf2 = p.createContainerEntityManagerFactory(
                u2,
                Collections.EMPTY_MAP);
        assertTrue(emf2 instanceof ResourceLocalEntityManagerFactory);

        // JTA Unit with RESOURCE_LOCAL override
        JpaUnit u3 = new JpaUnit() {
            @Override
            public void addTransformer(ClassTransformer transformer) {
            }

            @Override
            JpaDataSourceFactory getJpaDataSourceFactory() {
                return new MockJpaDataSourceFactory();
            }

            @Override
            public PersistenceUnitTransactionType getTransactionType() {
                return PersistenceUnitTransactionType.JTA;
            }
        };
        u3.setPersistenceUnitName("U3");

        EntityManagerFactory emf3 = p.createContainerEntityManagerFactory(u3, Collections
                .singletonMap(
                        Provider.TRANSACTION_TYPE_PROPERTY,
                        PersistenceUnitTransactionType.RESOURCE_LOCAL.name()));
        assertTrue(emf3 instanceof ResourceLocalEntityManagerFactory);

        // RESOURCE_LOCAL Unit with JTA override
        JpaUnit u4 = new JpaUnit() {
            @Override
            public void addTransformer(ClassTransformer transformer) {
            }

            @Override
            JpaDataSourceFactory getJpaDataSourceFactory() {
                return new MockJpaDataSourceFactory();
            }

            @Override
            public PersistenceUnitTransactionType getTransactionType() {
                return PersistenceUnitTransactionType.RESOURCE_LOCAL;
            }
        };
        u4.setPersistenceUnitName("U4");

        EntityManagerFactory emf4 = p.createContainerEntityManagerFactory(u4, Collections
                .singletonMap(
                        Provider.TRANSACTION_TYPE_PROPERTY,
                        PersistenceUnitTransactionType.JTA.name()));
        assertTrue(emf4 instanceof JtaEntityManagerFactory);
    }
}
