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

package org.apache.cayenne.access;

import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DefaultRuntimeProperties;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.event.DefaultEventManager;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.log.CommonsJdbcEventLogger;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.tx.DefaultTransactionFactory;
import org.apache.cayenne.tx.DefaultTransactionManager;
import org.apache.cayenne.tx.TransactionFactory;
import org.apache.cayenne.tx.TransactionManager;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@UseServerRuntime(CayenneProjects.MULTI_TIER_PROJECT)
public class DataRowStoreFactoryIT extends ServerCase {

    @Test
    public void testGetDataRowStore() throws Exception {
        ServerRuntime runtime = getUnitTestInjector().getInstance(ServerRuntime.class);
        DataRowStore dataStore = runtime.getInjector().getInstance(DataRowStoreFactory.class)
                .createDataRowStore("test");

        assertNotNull(dataStore);
    }

    @Test
    public void testGetDataRowStoreWithParameters() {
        final DataDomain DOMAIN = new DataDomain("test");
        final EventManager EVENT_MANAGER = new DefaultEventManager();
        final int CACHE_SIZE = 500;
        final int EXPIRATION_PROPERTY = 60 * 60 * 24;

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bindMap(Constants.PROPERTIES_MAP);
                binder.bind(DataDomain.class).toInstance(DOMAIN);
                binder.bind(EventManager.class).toInstance(EVENT_MANAGER);
                binder.bind(TransactionManager.class).to(DefaultTransactionManager.class);
                binder.bind(TransactionFactory.class).to(DefaultTransactionFactory.class);
                binder.bind(JdbcEventLogger.class).to(CommonsJdbcEventLogger.class);
                binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
                binder.bind(DataRowStoreFactory.class).to(DefaultDataRowStoreFactory.class);
                binder.bindMap(Constants.DATA_ROW_STORE_PROPERTIES_MAP)
                        .put(DataRowStore.SNAPSHOT_CACHE_SIZE_PROPERTY, String.valueOf(CACHE_SIZE))
                        .put(DataRowStore.SNAPSHOT_EXPIRATION_PROPERTY, String.valueOf(EXPIRATION_PROPERTY));
            }
        };

        Injector injector = DIBootstrap.createInjector(testModule);
        DataRowStore dataStore = injector.getInstance(DataRowStoreFactory.class)
                .createDataRowStore("test");

        assertNotNull(dataStore);
        assertEquals(dataStore.maximumSize(), CACHE_SIZE);
    }

    @Test
    public void testGetDataRowStoreWithBridge() {
        final DataDomain DOMAIN = new DataDomain("test");
        final EventManager EVENT_MANAGER = new DefaultEventManager();

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bindMap(Constants.PROPERTIES_MAP);
                binder.bind(DataDomain.class).toInstance(DOMAIN);
                binder.bind(EventManager.class).toInstance(EVENT_MANAGER);
                binder.bind(TransactionManager.class).to(DefaultTransactionManager.class);
                binder.bind(TransactionFactory.class).to(DefaultTransactionFactory.class);
                binder.bind(JdbcEventLogger.class).to(CommonsJdbcEventLogger.class);
                binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
                binder.bind(DataRowStoreFactory.class).to(DefaultDataRowStoreFactory.class);
                binder.bindMap(Constants.DATA_ROW_STORE_PROPERTIES_MAP);
            }
        };

        Injector injector = DIBootstrap.createInjector(testModule);
        DataRowStore dataStore = injector.getInstance(DataRowStoreFactory.class)
                .createDataRowStore("test");

        dataStore.stopListeners();
        dataStore.startListeners();
        dataStore.shutdown();
    }

}
