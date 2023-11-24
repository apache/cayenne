/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access;

import org.apache.cayenne.access.flush.DataDomainFlushActionFactory;
import org.apache.cayenne.access.flush.operation.DbRowOpSorter;
import org.apache.cayenne.access.flush.DefaultDataDomainFlushActionFactory;
import org.apache.cayenne.access.flush.operation.DefaultDbRowOpSorter;
import org.apache.cayenne.configuration.DefaultRuntimeProperties;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.configuration.runtime.CoreModule;
import org.apache.cayenne.configuration.runtime.CoreModuleExtender;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.event.DefaultEventManager;
import org.apache.cayenne.event.EventBridge;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.event.MockEventBridge;
import org.apache.cayenne.event.MockEventBridgeProvider;
import org.apache.cayenne.event.NoopEventBridgeProvider;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.log.Slf4jJdbcEventLogger;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.tx.DefaultTransactionFactory;
import org.apache.cayenne.tx.DefaultTransactionManager;
import org.apache.cayenne.tx.TransactionFactory;
import org.apache.cayenne.tx.TransactionManager;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@UseCayenneRuntime(CayenneProjects.MULTI_TIER_PROJECT)
public class DefaultDataRowStoreFactoryIT extends RuntimeCase {

    @Test
    public void testGetDataRowStore() {
        CayenneRuntime runtime = getUnitTestInjector().getInstance(CayenneRuntime.class);
        DataRowStore dataStore = runtime.getInjector().getInstance(DataRowStoreFactory.class)
                .createDataRowStore("test");

        assertNotNull(dataStore);
    }

    @Test
    public void testGetDataRowStoreWithParameters() {
        final DataDomain DOMAIN = new DataDomain("test");
        final EventManager EVENT_MANAGER = new DefaultEventManager();
        final int CACHE_SIZE = 500;

        Module testModule = binder -> {
            binder.bind(DataDomain.class).toInstance(DOMAIN);
            binder.bind(EventManager.class).toInstance(EVENT_MANAGER);
            binder.bind(TransactionManager.class).to(DefaultTransactionManager.class);
            binder.bind(TransactionFactory.class).to(DefaultTransactionFactory.class);
            binder.bind(JdbcEventLogger.class).to(Slf4jJdbcEventLogger.class);
            binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
            binder.bind(EventBridge.class).toProvider(NoopEventBridgeProvider.class);
            binder.bind(DataRowStoreFactory.class).to(DefaultDataRowStoreFactory.class);
            CoreModule.extend(binder).snapshotCacheSize(CACHE_SIZE);
        };

        Injector injector = DIBootstrap.createInjector(testModule);
        DataRowStore dataStore = injector.getInstance(DataRowStoreFactory.class)
                .createDataRowStore("test");

        assertNotNull(dataStore);
        assertEquals(dataStore.maximumSize(), CACHE_SIZE);
        assertNull(dataStore.getEventBridge());
    }

    @Test
    public void testGetDataRowStoreWithBridge() {
        final DataDomain DOMAIN = new DataDomain("test");
        final EventManager EVENT_MANAGER = new DefaultEventManager();

        Module testModule = binder -> {
            binder.bind(DataDomain.class).toInstance(DOMAIN);
            binder.bind(EventManager.class).toInstance(EVENT_MANAGER);
            binder.bind(TransactionManager.class).to(DefaultTransactionManager.class);
            binder.bind(TransactionFactory.class).to(DefaultTransactionFactory.class);
            binder.bind(JdbcEventLogger.class).to(Slf4jJdbcEventLogger.class);
            binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
            binder.bind(EventBridge.class).toProvider(MockEventBridgeProvider.class);
            binder.bind(DataRowStoreFactory.class).to(DefaultDataRowStoreFactory.class);
            binder.bind(DataDomainFlushActionFactory.class).to(DefaultDataDomainFlushActionFactory.class);
            binder.bind(DbRowOpSorter.class).to(DefaultDbRowOpSorter.class);
            binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
            binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
            new TestExtender(binder).initAllExtensions();
        };

        Injector injector = DIBootstrap.createInjector(testModule);
        DataRowStore dataStore = injector.getInstance(DataRowStoreFactory.class)
                .createDataRowStore("test");

        assertEquals(dataStore.getEventBridge().getClass(), MockEventBridge.class);
    }

    static class TestExtender extends CoreModuleExtender {
        public TestExtender(Binder binder) {
            super(binder);
        }

        @Override
        protected CoreModuleExtender initAllExtensions() {
            return super.initAllExtensions();
        }
    }

}
