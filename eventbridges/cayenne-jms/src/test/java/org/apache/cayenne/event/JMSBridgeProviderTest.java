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

package org.apache.cayenne.event;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DefaultRuntimeProperties;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.log.Slf4jJdbcEventLogger;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.tx.DefaultTransactionFactory;
import org.apache.cayenne.tx.DefaultTransactionManager;
import org.apache.cayenne.tx.TransactionFactory;
import org.apache.cayenne.tx.TransactionManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JMSBridgeProviderTest {

    private final DataDomain DOMAIN = new DataDomain("test");
    private final EventManager EVENT_MANAGER = new DefaultEventManager();
    protected static final String TOPIC_CONNECTION_FACTORY_TEST = "SomeTopicConnectionFactory";

    @Test
    public void testGetJMSBridge() throws Exception {
        Injector injector = DIBootstrap.createInjector(new DefaultBindings(), new JMSModule());
        EventBridge bridge = injector.getInstance(EventBridge.class);

        assertNotNull(bridge);
        assertTrue(bridge instanceof JMSBridge);
    }

    @Test
    public void testUseProperties() throws Exception {
        Module module = new Module() {

            public void configure(Binder binder) {
                JMSModule.contributeTopicConnectionFactory(binder, TOPIC_CONNECTION_FACTORY_TEST);
            }
        };

        Injector injector = DIBootstrap.createInjector(new DefaultBindings(), new JMSModule(), module);
        JMSBridge bridge = (JMSBridge) injector.getInstance(EventBridge.class);

        assertEquals(TOPIC_CONNECTION_FACTORY_TEST, bridge.getTopicConnectionFactoryName());
    }

    @Test
    public void testUseDefaultProperties() throws Exception {
        Injector injector = DIBootstrap.createInjector(new DefaultBindings(), new JMSModule());
        JMSBridge bridge = (JMSBridge) injector.getInstance(EventBridge.class);

        assertEquals(JMSBridge.TOPIC_CONNECTION_FACTORY_DEFAULT, bridge.getTopicConnectionFactoryName());
    }

    class DefaultBindings implements Module {
        @Override
        public void configure(Binder binder) {
            binder.bindMap(String.class, Constants.PROPERTIES_MAP);
            binder.bind(DataDomain.class).toInstance(DOMAIN);
            binder.bind(EventManager.class).toInstance(EVENT_MANAGER);
            binder.bind(TransactionManager.class).to(DefaultTransactionManager.class);
            binder.bind(TransactionFactory.class).to(DefaultTransactionFactory.class);
            binder.bind(JdbcEventLogger.class).to(Slf4jJdbcEventLogger.class);
            binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
        }
    }
}
