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

import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XMPPBridgeFactoryTest {

    protected Collection<EventSubject> subjects = Collections.singleton(new EventSubject("test"));
    protected String externalSubject = "subject";

    @Test
    public void testCreateEventBridge() {
        EventBridge bridge = new XMPPBridgeFactory().createEventBridge(
                subjects,
                externalSubject,
                Collections.<String, String>emptyMap());

        assertTrue(bridge instanceof XMPPBridge);
        assertEquals(subjects, bridge.getLocalSubjects());
        assertEquals(externalSubject, bridge.getExternalSubject());
    }

    @Test
    public void testUseMapPropertiesSetter() throws Exception {
        XMPPBridgeFactory bridgeFactory = new XMPPBridgeFactory();
        Map<String, String> properties = new HashMap<>();

        properties.put(XMPPBridge.XMPP_HOST_PROPERTY, XMPPBridgeProviderTest.HOST_TEST);
        properties.put(XMPPBridge.XMPP_CHAT_SERVICE_PROPERTY, XMPPBridgeProviderTest.CHAT_SERVICE_TEST);
        properties.put(XMPPBridge.XMPP_LOGIN_PROPERTY, XMPPBridgeProviderTest.LOGIN_TEST);
        properties.put(XMPPBridge.XMPP_PASSWORD_PROPERTY, XMPPBridgeProviderTest.PASSWORD_TEST);
        properties.put(XMPPBridge.XMPP_SECURE_CONNECTION_PROPERTY, String.valueOf(XMPPBridgeProviderTest.SECURE_CONNECTION_TEST));
        properties.put(XMPPBridge.XMPP_PORT_PROPERTY, String.valueOf(XMPPBridgeProviderTest.PORT_TEST));

        XMPPBridge bridge = (XMPPBridge) bridgeFactory.createEventBridge(subjects,
                externalSubject,
                properties);

        assertEquals(bridge.getXmppHost(), XMPPBridgeProviderTest.HOST_TEST);
        assertEquals(bridge.getChatService(), XMPPBridgeProviderTest.CHAT_SERVICE_TEST);
        assertEquals(bridge.getLoginId(), XMPPBridgeProviderTest.LOGIN_TEST);
        assertEquals(bridge.getPassword(), XMPPBridgeProviderTest.PASSWORD_TEST);
        assertEquals(bridge.getXmppPort(), XMPPBridgeProviderTest.PORT_TEST);
        assertEquals(bridge.isSecureConnection(), XMPPBridgeProviderTest.SECURE_CONNECTION_TEST);
    }
}
