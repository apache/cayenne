/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne.rop;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.rop.http2.HighHttp2ROPConnector;

/**
 * This {@link Provider} initializes {@link ClientConnection} through {@link HighHttp2ROPConnector}.
 * It works with ALPN.
 */
public class HighHttp2ALPNClientConnectionProvider implements Provider<ClientConnection> {

    @Inject
    protected RuntimeProperties runtimeProperties;

    @Inject
    protected ROPSerializationService serializationService;

    private HighHttp2ROPConnector ropConnector;

    @Override
    public ClientConnection get() throws DIRuntimeException {
        String sharedSession = runtimeProperties
                .get(Constants.ROP_SERVICE_SHARED_SESSION_PROPERTY);

        ropConnector = createHttp2ROPConnectorALPN();
        ProxyRemoteService remoteService = new ProxyRemoteService(serializationService, ropConnector);

        HttpClientConnection clientConnection = new HttpClientConnection(remoteService, sharedSession);
        ropConnector.setClientConnection(clientConnection);
        return clientConnection;
    }

    protected HighHttp2ROPConnector createHttp2ROPConnectorALPN() {
        String url = runtimeProperties.get(Constants.ROP_SERVICE_URL_PROPERTY);
        if (url == null) {
            throw new ConfigurationException(
                    "No property defined for '%s', can't initialize HTTP/2 connection",
                    Constants.ROP_SERVICE_URL_PROPERTY);
        }

        String username = runtimeProperties.get(Constants.ROP_SERVICE_USERNAME_PROPERTY);
        String password = runtimeProperties.get(Constants.ROP_SERVICE_PASSWORD_PROPERTY);
        String realm = runtimeProperties.get(Constants.ROP_SERVICE_REALM_PROPERTY);

        long readTimeout = runtimeProperties.getLong(
                Constants.ROP_SERVICE_TIMEOUT_PROPERTY,
                -1L);

        HighHttp2ROPConnector ropConnector = new HighHttp2ROPConnector(url, username, password, realm, true);

        if (readTimeout > 0) {
            ropConnector.setReadTimeout(readTimeout);
        }

        return ropConnector;
    }

}
