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

package org.apache.cayenne.modeler.dialog.datadomain;

import org.apache.cayenne.access.DataRowStore;
import org.apache.cayenne.event.JMSBridgeFactory;
import org.scopemvc.core.Selector;

/**
 */
public class JMSConfigModel extends CacheSyncConfigModel {
    private static final String[] storedProperties =
        new String[] {
            DataRowStore.EVENT_BRIDGE_FACTORY_PROPERTY,
            JMSBridgeFactory.TOPIC_CONNECTION_FACTORY_PROPERTY };

    public static final Selector TOPIC_FACTORY_SELECTOR =
        Selector.fromString("topicFactory");

    public String[] supportedProperties() {
        return storedProperties;
    }

    public Selector selectorForKey(String key) {
        return (JMSBridgeFactory.TOPIC_CONNECTION_FACTORY_PROPERTY.equals(key))
            ? TOPIC_FACTORY_SELECTOR
            : null;
    }

    public String defaultForKey(String key) {
        return (JMSBridgeFactory.TOPIC_CONNECTION_FACTORY_PROPERTY.equals(key))
            ? JMSBridgeFactory.TOPIC_CONNECTION_FACTORY_DEFAULT
            : null;
    }

    public String getTopicFactory() {
        return getProperty(JMSBridgeFactory.TOPIC_CONNECTION_FACTORY_PROPERTY);
    }

    public void setTopicFactory(String topicFactory) {
        setProperty(JMSBridgeFactory.TOPIC_CONNECTION_FACTORY_PROPERTY, topicFactory);
    }
}
