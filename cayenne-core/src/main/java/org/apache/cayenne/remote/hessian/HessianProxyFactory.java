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

package org.apache.cayenne.remote.hessian;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.cayenne.remote.RemoteSession;

/**
 * A proxy factory that handles HTTP sessions.
 * 
 * @since 1.2
 */
class HessianProxyFactory extends com.caucho.hessian.client.HessianProxyFactory {

    static final String SESSION_COOKIE_NAME = "JSESSIONID";

    private HessianConnection clientConnection;

    HessianProxyFactory(HessianConnection clientConnection) {
        this.clientConnection = clientConnection;
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        URLConnection connection = super.openConnection(url);

        // unfortunately we can't read response cookies without completely reimplementing
        // 'HessianProxy.invoke()'. Currently (3.0.13) it doesn't allow to cleanly
        // intercept response... so extract session id from the RemoteSession....

        // add session cookie
        RemoteSession session = clientConnection.getSession();
        if (session != null && session.getSessionId() != null) {
            connection.setRequestProperty("Cookie", SESSION_COOKIE_NAME
                    + "="
                    + session.getSessionId());
        }

        return connection;
    }
}
