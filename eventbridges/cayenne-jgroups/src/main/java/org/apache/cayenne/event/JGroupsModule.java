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

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.MapBuilder;
import org.apache.cayenne.di.Module;

/**
 * @since 4.0
 */
public class JGroupsModule implements Module {

    /**
     * A DI container key for the Map&lt;String, String&gt; storing
     * {@link org.apache.cayenne.event.JavaGroupsBridge} properties
     *
     * @since 4.0
     */
    public static final String JAVA_GROUPS_BRIDGE_PROPERTIES_MAP = "cayenne.server.java_group_bridge";

    public static void contributeMulticastAddress(Binder binder, String address) {
        contributeProperties(binder).put(JavaGroupsBridge.MCAST_ADDRESS_PROPERTY, address);
    }

    public static void contributeMulticastPort(Binder binder, int port) {
        contributeProperties(binder).put(JavaGroupsBridge.MCAST_PORT_PROPERTY, Integer.toString(port));
    }

    public static void contributeConfigUrl(Binder binder, String config) {
        contributeProperties(binder).put(JavaGroupsBridge.JGROUPS_CONFIG_URL_PROPERTY, config);
    }

    private static MapBuilder<String> contributeProperties(Binder binder) {
        return binder.bindMap(String.class, JAVA_GROUPS_BRIDGE_PROPERTIES_MAP);
    }

    @Override
    public void configure(Binder binder) {
        // init properties' defaults
        contributeMulticastAddress(binder, JavaGroupsBridge.MCAST_ADDRESS_DEFAULT);
        contributeMulticastPort(binder, JavaGroupsBridge.MCAST_PORT_DEFAULT_INT);

        binder.bind(EventBridge.class).toProvider(JavaGroupsBridgeProvider.class);
    }
}
