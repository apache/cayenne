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

package org.apache.cayenne.event;

import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JMSBridgeFactoryTest {

    protected Collection<EventSubject> subjects = Collections.singleton(new EventSubject("test"));
    protected String externalSubject = "subject";

    @Test
    public void testCreateEventBridge() throws Exception {
        EventBridge bridge = new JMSBridgeFactory().createEventBridge(
                subjects,
                externalSubject,
                Collections.<String, String>emptyMap());

        assertNotNull(bridge);
        assertTrue(bridge instanceof JMSBridge);
        assertEquals(subjects, bridge.getLocalSubjects());
        assertEquals(externalSubject, bridge.getExternalSubject());
    }

    @Test
    public void testUseProperties() throws Exception {
        JMSBridgeFactory bridgeFactory = new JMSBridgeFactory();

        Map<String, String> properties = new HashMap<>();
        properties.put(JMSBridge.TOPIC_CONNECTION_FACTORY_PROPERTY, JMSBridgeProviderTest.TOPIC_CONNECTION_FACTORY_TEST);

        JMSBridge bridge = (JMSBridge) bridgeFactory.createEventBridge(
                subjects,
                externalSubject,
                properties);

        assertEquals(bridge.getTopicConnectionFactoryName(), JMSBridgeProviderTest.TOPIC_CONNECTION_FACTORY_TEST);
    }

    @Test
    public void testUseDefaultProperties() throws Exception {
        JMSBridgeFactory bridgeFactory = new JMSBridgeFactory();
        JMSBridge bridge = (JMSBridge) bridgeFactory.createEventBridge(
                subjects,
                externalSubject,
                Collections.<String, String>emptyMap());

        assertEquals(bridge.getTopicConnectionFactoryName(), JMSBridge.TOPIC_CONNECTION_FACTORY_DEFAULT);
    }

}
