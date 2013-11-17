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

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.reflect.PropertyUtils;

/**
 * Factory to create JavaGroupsBridge instances. If JavaGroups library is not installed
 * this factory will return a noop EventBridge as a failover mechanism.
 *
 * For further information about JavaGroups consult the <a href="http://www.jgroups.org/">documentation</a>.
 * 
 * @since 1.1
 */
public class JavaGroupsBridgeFactory implements EventBridgeFactory {

    public static final String MCAST_ADDRESS_DEFAULT = "228.0.0.5";
    public static final String MCAST_PORT_DEFAULT = "22222";

    public static final String MCAST_ADDRESS_PROPERTY = "cayenne.JavaGroupsBridge.mcast.address";
    public static final String MCAST_PORT_PROPERTY = "cayenne.JavaGroupsBridge.mcast.port";

    /**
     * Defines a property for JavaGroups XML configuration file.
     */
    public static final String JGROUPS_CONFIG_URL_PROPERTY = "javagroupsbridge.config.url";

    /**
     * Creates a JavaGroupsBridge instance. Since JavaGroups is not shipped with Cayenne
     * and should be installed separately, a common misconfiguration problem may be the
     * absense of JavaGroups jar file. This factory returns a dummy noop EventBridge, if
     * this is the case. This would allow the application to continue to run, but without
     * remote notifications.
     */
    public EventBridge createEventBridge(
            Collection<EventSubject> localSubjects,
            String externalSubject,
            Map<String, Object> properties) {

        try {
            // sniff JavaGroups presence
            Class.forName("org.jgroups.Channel");
            return createJavaGroupsBridge(localSubjects, externalSubject, properties);
        }
        catch (Exception ex) {
            // recover from no JavaGroups
            return createNoopBridge();
        }
    }

    private EventBridge createNoopBridge() {
        return new NoopEventBridge();
    }

    private EventBridge createJavaGroupsBridge(
            Collection<EventSubject> localSubjects,
            String externalSubject,
            Map<String, Object> properties) {

        // create JavaGroupsBridge using reflection to avoid triggering
        // ClassNotFound exceptions due to JavaGroups absence.

        try {
            Constructor<?> c = Class
                    .forName("org.apache.cayenne.event.JavaGroupsBridge")
                    .getConstructor(Collection.class, String.class);

            Object bridge = c.newInstance(localSubjects, externalSubject);

            // configure properties
            String multicastAddress = (String) properties.get(MCAST_ADDRESS_PROPERTY);
            String multicastPort = (String) properties.get(MCAST_PORT_PROPERTY);
            String configURL = (String) properties.get(JGROUPS_CONFIG_URL_PROPERTY);

            PropertyUtils.setProperty(bridge, "configURL", configURL);
            PropertyUtils.setProperty(
                    bridge,
                    "multicastAddress",
                    multicastAddress != null ? multicastAddress : MCAST_ADDRESS_DEFAULT);
            PropertyUtils.setProperty(bridge, "multicastPort", multicastPort != null
                    ? multicastPort
                    : MCAST_PORT_DEFAULT);

            return (EventBridge) bridge;
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error creating JavaGroupsBridge", ex);
        }
    }

    // mockup EventBridge
    class NoopEventBridge extends EventBridge {

        public NoopEventBridge() {
            super(Collections.EMPTY_SET, null);
        }

        @Override
        public boolean receivesExternalEvents() {
            return false;
        }

        @Override
        public boolean receivesLocalEvents() {
            return false;
        }

        @Override
        protected void startupExternal() {
        }

        @Override
        protected void shutdownExternal() {
        }

        @Override
        protected void sendExternalEvent(CayenneEvent localEvent) {
        }

        @Override
        public void startup(EventManager eventManager, int mode, Object eventsSource) {
            this.eventManager = eventManager;
        }

        @Override
        public void shutdown() {
        }
    }
}
