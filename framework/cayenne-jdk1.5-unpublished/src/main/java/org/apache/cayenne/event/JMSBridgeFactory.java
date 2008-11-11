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

import java.util.Collection;
import java.util.Map;

/**
 * Factory to create JMSBridge instances.
 * 
 * @since 1.1
 */
public class JMSBridgeFactory implements EventBridgeFactory {

    // this is an OpenJMS default for the factory name. Likely it won't work with
    // anything else
    public static final String TOPIC_CONNECTION_FACTORY_DEFAULT = "JmsTopicConnectionFactory";

    public static final String TOPIC_CONNECTION_FACTORY_PROPERTY = "cayenne.JMSBridge.topic.connection.factory";

    /**
     * @since 1.2
     */
    public EventBridge createEventBridge(Collection<EventSubject> localSubjects, String externalSubject, Map<String, Object> properties) {
        JMSBridge bridge = new JMSBridge(localSubjects, externalSubject);

        // configure properties
        String topicConnectionFactory = (String) properties
                .get(TOPIC_CONNECTION_FACTORY_PROPERTY);

        bridge.setTopicConnectionFactoryName(topicConnectionFactory != null
                ? topicConnectionFactory
                : TOPIC_CONNECTION_FACTORY_DEFAULT);

        return bridge;
    }
}
