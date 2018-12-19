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
public class XMPPModule implements Module {

    /**
     * A DI container key for the Map&lt;String, String&gt; storing
     * {@link org.apache.cayenne.event.XMPPBridge} properties
     *
     * @since 4.0
     */
    public static final String XMPP_BRIDGE_PROPERTIES_MAP = "cayenne.server.xmpp_bridge";

    public static void contributeHost(Binder binder, String host) {
        contributeProperties(binder).put(XMPPBridge.XMPP_HOST_PROPERTY, host);
    }

    public static void contributePort(Binder binder, int port) {
        contributeProperties(binder).put(XMPPBridge.XMPP_PORT_PROPERTY, Integer.toString(port));
    }

    public static void contributeLogin(Binder binder, String login, String password) {
        contributeProperties(binder).put(XMPPBridge.XMPP_LOGIN_PROPERTY, login);
        contributeProperties(binder).put(XMPPBridge.XMPP_PASSWORD_PROPERTY, password);
    }

    public static void contributeChatService(Binder binder, String chatService) {
        contributeProperties(binder).put(XMPPBridge.XMPP_CHAT_SERVICE_PROPERTY, chatService);
    }

    public static void contributeSecureConnection(Binder binder, boolean secure) {
        contributeProperties(binder).put(XMPPBridge.XMPP_SECURE_CONNECTION_PROPERTY, Boolean.toString(secure));
    }

    private static MapBuilder<String> contributeProperties(Binder binder) {
        return binder.bindMap(String.class, XMPP_BRIDGE_PROPERTIES_MAP);
    }

    @Override
    public void configure(Binder binder) {
        // init properties' defaults
        contributeChatService(binder, XMPPBridge.DEFAULT_CHAT_SERVICE);

        binder.bind(EventBridge.class).toProvider(XMPPBridgeProvider.class).withoutScope();
    }
}
