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

import org.apache.cayenne.CayenneRuntimeException;

/**
 * A factory of XMPPBridge. Note that to deploy an XMPPBridge, you need to have
 * <em>smack.jar</em> library in the runtime.
 * 
 * @since 1.2
 */
public class XMPPBridgeFactory implements EventBridgeFactory {

    public static final String XMPP_HOST_PROPERTY = "cayenne.XMPPBridge.xmppHost";

    /**
     * An optional property, port 5222 is used as default XMPP port.
     */
    public static final String XMPP_PORT_PROPERTY = "cayenne.XMPPBridge.xmppPort";

    /**
     * An optional property, "conference" is used as default chat service.
     */
    public static final String XMPP_CHAT_SERVICE_PROPERTY = "cayenne.XMPPBridge.xmppChatService";

    public static final String XMPP_SECURE_CONNECTION_PROPERTY = "cayenne.XMPPBridge.xmppSecure";
    public static final String XMPP_LOGIN_PROPERTY = "cayenne.XMPPBridge.xmppLogin";
    public static final String XMPP_PASSWORD_PROPERTY = "cayenne.XMPPBridge.xmppPassword";

    public EventBridge createEventBridge(
            Collection<EventSubject> localSubjects,
            String externalSubject,
            Map<String, Object> properties) {

        String chatService = (String) properties.get(XMPP_CHAT_SERVICE_PROPERTY);
        String host = (String) properties.get(XMPP_HOST_PROPERTY);

        String loginId = (String) properties.get(XMPP_LOGIN_PROPERTY);
        String password = (String) properties.get(XMPP_PASSWORD_PROPERTY);

        String secureConnectionString = (String) properties
                .get(XMPP_SECURE_CONNECTION_PROPERTY);
        boolean secureConnection = "true".equalsIgnoreCase(secureConnectionString);

        String portString = (String) properties.get(XMPP_PORT_PROPERTY);
        int port = -1;
        if (portString != null) {

            try {
                port = Integer.parseInt(portString);
            }
            catch (NumberFormatException e) {
                throw new CayenneRuntimeException("Invalid port: " + portString);
            }
        }

        XMPPBridge bridge = new XMPPBridge(localSubjects, externalSubject);

        bridge.setXmppHost(host);
        bridge.setXmppPort(port);
        bridge.setChatService(chatService);
        bridge.setSecureConnection(secureConnection);
        bridge.setLoginId(loginId);
        bridge.setPassword(password);

        return bridge;
    }
}
