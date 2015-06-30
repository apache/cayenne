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
package org.apache.cayenne.configuration.server;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataRowStoreFactory;
import org.apache.cayenne.access.DefaultDataRowStoreFactory;
import org.apache.cayenne.access.DefaultObjectMapRetainStrategy;
import org.apache.cayenne.access.ObjectMapRetainStrategy;
import org.apache.cayenne.cache.MapQueryCache;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DefaultObjectStoreFactory;
import org.apache.cayenne.configuration.DefaultRuntimeProperties;
import org.apache.cayenne.configuration.ObjectStoreFactory;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.event.MockEventManager;
import org.apache.cayenne.log.CommonsJdbcEventLogger;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.tx.DefaultTransactionFactory;
import org.apache.cayenne.tx.DefaultTransactionManager;
import org.apache.cayenne.tx.TransactionFactory;
import org.apache.cayenne.tx.TransactionManager;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DataContextFactoryTest {

    @Test
    public void testCreateDataContextWithDedicatedCache() throws Exception {

        final EventManager eventManager = new MockEventManager();
        final DataDomain domain = new DataDomain("d1");

        domain.setSharedCacheEnabled(false);

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(JdbcEventLogger.class).to(CommonsJdbcEventLogger.class);
                binder.bind(DataDomain.class).toInstance(domain);
                binder.bind(EventManager.class).toInstance(eventManager);
                binder.bind(QueryCache.class).toInstance(new MapQueryCache(5));
                binder.bind(RuntimeProperties.class).toInstance(
                        new DefaultRuntimeProperties(Collections.EMPTY_MAP));
                binder.bind(ObjectMapRetainStrategy.class).to(
                        DefaultObjectMapRetainStrategy.class);
                binder.bind(ObjectStoreFactory.class).to(DefaultObjectStoreFactory.class);
                binder.bind(TransactionFactory.class).to(DefaultTransactionFactory.class);
                binder.bind(TransactionManager.class).to(DefaultTransactionManager.class);
                binder.bind(DataRowStoreFactory.class).to(DefaultDataRowStoreFactory.class);
                binder.bindMap(Constants.DATA_ROW_STORE_PROPERTIES_MAP);
            }
        };

        Injector injector = DIBootstrap.createInjector(testModule);

        DataContextFactory factory = new DataContextFactory();
        injector.injectMembers(factory);

        DataContext c3 = (DataContext) factory.createContext();
        assertNotNull(c3.getObjectStore().getDataRowCache());
        assertNull(domain.getSharedSnapshotCache());
        assertNotSame(
                c3.getObjectStore().getDataRowCache(),
                domain.getSharedSnapshotCache());
    }

    @Test
    public void testCreateDataContextValidation() throws Exception {
        final EventManager eventManager = new MockEventManager();
        final DataDomain domain = new DataDomain("d1");

        domain.setValidatingObjectsOnCommit(true);

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(JdbcEventLogger.class).to(CommonsJdbcEventLogger.class);
                binder.bind(DataDomain.class).toInstance(domain);
                binder.bind(EventManager.class).toInstance(eventManager);
                binder.bind(QueryCache.class).toInstance(new MapQueryCache(5));
                binder.bind(RuntimeProperties.class).toInstance(
                        new DefaultRuntimeProperties(Collections.EMPTY_MAP));
                binder.bind(ObjectMapRetainStrategy.class).to(
                        DefaultObjectMapRetainStrategy.class);
                binder.bind(ObjectStoreFactory.class).to(DefaultObjectStoreFactory.class);
                binder.bind(TransactionFactory.class).to(DefaultTransactionFactory.class);
                binder.bind(TransactionManager.class).to(DefaultTransactionManager.class);
                binder.bind(DataRowStoreFactory.class).to(DefaultDataRowStoreFactory.class);
                binder.bindMap(Constants.DATA_ROW_STORE_PROPERTIES_MAP);
            }
        };

        Injector injector = DIBootstrap.createInjector(testModule);

        DataContextFactory factory = new DataContextFactory();
        injector.injectMembers(factory);

        DataContext c1 = (DataContext) factory.createContext();
        assertTrue(c1.isValidatingObjectsOnCommit());

        domain.setValidatingObjectsOnCommit(false);

        DataContext c2 = (DataContext) factory.createContext();
        assertFalse(c2.isValidatingObjectsOnCommit());
    }

}
