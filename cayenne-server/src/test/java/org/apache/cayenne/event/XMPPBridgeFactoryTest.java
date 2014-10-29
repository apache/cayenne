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

    @Test
    public void testCreateEventBridge() {
        XMPPBridgeFactory factory = new XMPPBridgeFactory();

        Collection subjects = Collections.singleton(EventSubject.getSubject(
                getClass(),
                "test"));
        Map properties = new HashMap();
        properties.put(XMPPBridgeFactory.XMPP_HOST_PROPERTY, "somehost.com");
        properties.put(XMPPBridgeFactory.XMPP_PORT_PROPERTY, "12345");

        EventBridge bridge = factory.createEventBridge(
                subjects,
                "remote-subject",
                properties);

        assertTrue(bridge instanceof XMPPBridge);

        XMPPBridge xmppBridge = (XMPPBridge) bridge;

        assertEquals(subjects, xmppBridge.getLocalSubjects());
        assertEquals("remote-subject", xmppBridge.getExternalSubject());
        assertEquals("somehost.com", xmppBridge.getXmppHost());
        assertEquals(12345, xmppBridge.getXmppPort());
    }
}
