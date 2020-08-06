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
import org.apache.cayenne.configuration.rop.client.ClientConstants;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.rop.http.JettyHttpROPConnector;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * This {@link Provider} initializes HTTP/2 {@link ClientConnection} through {@link JettyHttpROPConnector} which uses
 * {@link org.eclipse.jetty.client.HttpClient} over {@link org.eclipse.jetty.http2.client.HTTP2Client}.
 * It works without ALPN by default.
 * <p>
 * In order to use it with ALPN you have to set {@link ClientConstants#ROP_SERVICE_USE_ALPN_PROPERTY} to true
 * and provide the alpn-boot-XXX.jar into the bootstrap classpath.
 */
public class JettyHttp2ClientConnectionProvider extends JettyHttpClientConnectionProvider {

    @Override
    protected HttpClient initJettyHttpClient() {
        try {
            HttpClientTransportOverHTTP2 http2 = new HttpClientTransportOverHTTP2(new HTTP2Client());

            boolean useALPN = runtimeProperties.getBoolean(ClientConstants.ROP_SERVICE_USE_ALPN_PROPERTY, false);
            http2.setUseALPN(useALPN);

            HttpClient httpClient = new HttpClient(http2, new SslContextFactory());
            httpClient.start();

            return httpClient;
        } catch (Exception e) {
            throw new CayenneRuntimeException("Exception while starting Jetty HttpClient over HTTP/2.", e);
        }
    }

}
