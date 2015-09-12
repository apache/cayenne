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
import org.apache.cayenne.log.CommonsJdbcEventLogger;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.tx.DefaultTransactionFactory;
import org.apache.cayenne.tx.DefaultTransactionManager;
import org.apache.cayenne.tx.TransactionFactory;
import org.apache.cayenne.tx.TransactionManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class XMPPBridgeProviderTest {

    private final DataDomain DOMAIN = new DataDomain("test");
    private final EventManager EVENT_MANAGER = new DefaultEventManager();
    protected static final String HOST_TEST = "somehost.com";
    protected static final String CHAT_SERVICE_TEST = "conference";
    protected static final String LOGIN_TEST = "login";
    protected static final String PASSWORD_TEST = "password";
    protected static final boolean SECURE_CONNECTION_TEST = true;
    protected static final int PORT_TEST = 12345;

    @Test
    public void testGetXMPPBridge() throws Exception {
        Module module = new Module() {
            public void configure(Binder binder) {
                binder.bindMap(Constants.PROPERTIES_MAP);
                binder.bind(DataDomain.class).toInstance(DOMAIN);
                binder.bind(EventManager.class).toInstance(EVENT_MANAGER);
                binder.bind(TransactionManager.class).to(DefaultTransactionManager.class);
                binder.bind(TransactionFactory.class).to(DefaultTransactionFactory.class);
                binder.bind(JdbcEventLogger.class).to(CommonsJdbcEventLogger.class);
                binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
                binder.bind(EventBridge.class).toProvider(XMPPBridgeProvider.class);
                binder.bindMap(Constants.XMPP_BRIDGE_PROPERTIES_MAP);
            }
        };

        Injector injector = DIBootstrap.createInjector(module);
        EventBridge bridge = injector.getInstance(EventBridge.class);

        assertNotNull(bridge);
        assertTrue(bridge instanceof XMPPBridge);
    }

    @Test
    public void testUseProperties() throws Exception {
        Module module = new Module() {
            public void configure(Binder binder) {
                binder.bindMap(Constants.PROPERTIES_MAP);
                binder.bind(DataDomain.class).toInstance(DOMAIN);
                binder.bind(EventManager.class).toInstance(EVENT_MANAGER);
                binder.bind(TransactionManager.class).to(DefaultTransactionManager.class);
                binder.bind(TransactionFactory.class).to(DefaultTransactionFactory.class);
                binder.bind(JdbcEventLogger.class).to(CommonsJdbcEventLogger.class);
                binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
                binder.bind(EventBridge.class).toProvider(XMPPBridgeProvider.class);
                binder.bindMap(Constants.XMPP_BRIDGE_PROPERTIES_MAP)
                        .put(XMPPBridge.XMPP_HOST_PROPERTY, HOST_TEST)
                        .put(XMPPBridge.XMPP_CHAT_SERVICE_PROPERTY, CHAT_SERVICE_TEST)
                        .put(XMPPBridge.XMPP_LOGIN_PROPERTY, LOGIN_TEST)
                        .put(XMPPBridge.XMPP_PASSWORD_PROPERTY, PASSWORD_TEST)
                        .put(XMPPBridge.XMPP_SECURE_CONNECTION_PROPERTY, String.valueOf(SECURE_CONNECTION_TEST))
                        .put(XMPPBridge.XMPP_PORT_PROPERTY, String.valueOf(PORT_TEST));
            }
        };

        Injector injector = DIBootstrap.createInjector(module);
        XMPPBridge bridge = (XMPPBridge) injector.getInstance(EventBridge.class);

        assertEquals(HOST_TEST, bridge.getXmppHost());
        assertEquals(CHAT_SERVICE_TEST, bridge.getChatService());
        assertEquals(LOGIN_TEST, bridge.getLoginId());
        assertEquals(PASSWORD_TEST, bridge.getPassword());
        assertEquals(SECURE_CONNECTION_TEST, bridge.isSecureConnection());
        assertEquals(PORT_TEST, bridge.getXmppPort());
    }

    public void testUseDefaultProperties() throws Exception {
        Module module = new Module() {
            public void configure(Binder binder) {
                binder.bindMap(Constants.PROPERTIES_MAP);
                binder.bind(DataDomain.class).toInstance(DOMAIN);
                binder.bind(EventManager.class).toInstance(EVENT_MANAGER);
                binder.bind(TransactionManager.class).to(DefaultTransactionManager.class);
                binder.bind(TransactionFactory.class).to(DefaultTransactionFactory.class);
                binder.bind(JdbcEventLogger.class).to(CommonsJdbcEventLogger.class);
                binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
                binder.bind(EventBridge.class).toProvider(XMPPBridgeProvider.class);
                binder.bindMap(Constants.XMPP_BRIDGE_PROPERTIES_MAP);
            }
        };

        Injector injector = DIBootstrap.createInjector(module);
        XMPPBridge bridge = (XMPPBridge) injector.getInstance(EventBridge.class);

        assertEquals(bridge.getChatService(), XMPPBridge.DEFAULT_CHAT_SERVICE);
        assertEquals(bridge.getXmppPort(), XMPPBridge.DEFAULT_XMPP_PORT);
    }
}
