/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne.rop;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.configuration.rop.client.ClientConstants;
import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.rop.http.JettyHttpROPConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.net.URI;

/**
 * This {@link Provider} initializes HTTP/1.1 {@link ClientConnection} through {@link JettyHttpROPConnector} which uses
 * {@link org.eclipse.jetty.client.HttpClient}.
 */
public class JettyHttpClientConnectionProvider implements Provider<ClientConnection> {

    private static final Logger logger = LoggerFactory.getLogger(JettyHttpROPConnector.class);

    @Inject
    protected RuntimeProperties runtimeProperties;

    @Inject
    protected ROPSerializationService serializationService;

    @Override
    public ClientConnection get() throws DIRuntimeException {
        String sharedSession = runtimeProperties
                .get(ClientConstants.ROP_SERVICE_SHARED_SESSION_PROPERTY);

        JettyHttpROPConnector ropConnector = createJettyHttpRopConnector();
        ProxyRemoteService remoteService = new ProxyRemoteService(serializationService, ropConnector);

        HttpClientConnection clientConnection = new HttpClientConnection(remoteService, sharedSession);
        ropConnector.setClientConnection(clientConnection);

        return clientConnection;
    }

    protected JettyHttpROPConnector createJettyHttpRopConnector() {
        String url = runtimeProperties.get(ClientConstants.ROP_SERVICE_URL_PROPERTY);
        if (url == null) {
            throw new ConfigurationException(
                    "No property defined for '%s', can't initialize connection",
                    ClientConstants.ROP_SERVICE_URL_PROPERTY);
        }

        String username = runtimeProperties.get(ClientConstants.ROP_SERVICE_USERNAME_PROPERTY);
        long readTimeout = runtimeProperties.getLong(
                ClientConstants.ROP_SERVICE_TIMEOUT_PROPERTY,
                -1L);

        HttpClient httpClient = initJettyHttpClient();

        addBasicAuthentication(httpClient, url, username);

        JettyHttpROPConnector result = new JettyHttpROPConnector(httpClient, url, username);

        if (readTimeout > 0) {
            result.setReadTimeout(readTimeout);
        }

        return result;
    }

    protected HttpClient initJettyHttpClient() {
        try {
            HttpClient httpClient = new HttpClient(new SslContextFactory());
            httpClient.start();

            return httpClient;
        } catch (Exception e) {
            throw new CayenneRuntimeException("Exception while starting Jetty HttpClient.", e);
        }
    }

    protected void addBasicAuthentication(HttpClient httpClient, String url, String username) {
        String password = runtimeProperties.get(ClientConstants.ROP_SERVICE_PASSWORD_PROPERTY);
        String realm = runtimeProperties.get(ClientConstants.ROP_SERVICE_REALM_PROPERTY);

        if (username != null && password != null) {
            if (realm == null && logger.isWarnEnabled()) {
                logger.warn("In order to use JettyClient with BASIC Authentication " +
                        "you should provide Constants.ROP_SERVICE_REALM_PROPERTY.");
                return;
            }

            if (logger.isInfoEnabled()) {
                logger.info(
                        "Adding authentication" +
                                "\nUser: " + username +
                                "\nRealm: " + realm);
            }

            AuthenticationStore auth = httpClient.getAuthenticationStore();
            auth.addAuthentication(new BasicAuthentication(URI.create(url), realm, username, password));
        }
    }

}
