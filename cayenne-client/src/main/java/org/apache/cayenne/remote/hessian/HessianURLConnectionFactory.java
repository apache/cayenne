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

import org.apache.cayenne.remote.RemoteSession;

import java.io.IOException;
import java.net.URL;

public class HessianURLConnectionFactory extends com.caucho.hessian.client.HessianURLConnectionFactory {

    static final String SESSION_COOKIE_NAME = "JSESSIONID";

    private HessianConnection clientConnection;

    HessianURLConnectionFactory(HessianConnection clientConnection) {
        this.clientConnection = clientConnection;
    }

    @Override
    public com.caucho.hessian.client.HessianConnection open(URL url) throws IOException {
        com.caucho.hessian.client.HessianConnection hessianConnection = super.open(url);

        // add session cookie
        RemoteSession session = clientConnection.getSession();
        if (session != null && session.getSessionId() != null) {
            hessianConnection.addHeader("Cookie", SESSION_COOKIE_NAME
                    + "="
                    + session.getSessionId());
        }

        return hessianConnection;
    }
}