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

package org.apache.cayenne.rop.http2;

import org.apache.cayenne.remote.RemoteSession;
import org.apache.cayenne.rop.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.HTTP2ClientConnectionFactory;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

/**
 * This implementation of ROPConnector uses Jetty HTTP Client over HTTP2 Client (high-level API)
 * Depends on HttpClientTransport it works with or without ALPN.
 * <p>
 * {@link HighHttp2ClientConnectionProvider}
 * {@link HighHttp2ALPNClientConnectionProvider}
 */
public class HighHttp2ROPConnector implements ROPConnector {

    private static Log logger = LogFactory.getLog(HighHttp2ROPConnector.class);

    public static final String SESSION_COOKIE_NAME = "JSESSIONID";

    protected HttpClient httpClient;
    protected HttpClientConnection clientConnection;
    protected boolean ALPN;

    protected String url;

    protected String username;
    protected String password;
    protected String realm;

    protected long readTimeout;

    public HighHttp2ROPConnector(String url, String username, String password, String realm, boolean ALPN) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.realm = realm;
        this.ALPN = ALPN;
    }

    public void setClientConnection(HttpClientConnection clientConnection) {
        this.clientConnection = clientConnection;
    }

    public void setReadTimeout(Long readTimeout) {
        this.readTimeout = readTimeout;
    }

    @Override
    public InputStream establishSession() throws Exception {
        close();

        if (logger.isInfoEnabled()) {
            logger.info(ROPUtil.getLogConnect(url, username, password, null));
        }

        httpClient = new HttpClient(getClientTransport(), new SslContextFactory());

        if (readTimeout > 0) {
            httpClient.setIdleTimeout(readTimeout);
        }

        httpClient.start();
        addAuthHeader();

        ContentResponse response = httpClient.newRequest(url)
                .method(HttpMethod.POST)
                .param(ROPConstants.OPERATION_PARAMETER, ROPConstants.ESTABLISH_SESSION_OPERATION)
                .send();

        return new ByteArrayInputStream(response.getContent());
    }

    @Override
    public InputStream establishSharedSession(String name) throws Exception {
        close();

        if (logger.isInfoEnabled()) {
            logger.info(ROPUtil.getLogConnect(url, username, password, name));
        }

        httpClient = new HttpClient(getClientTransport(), new SslContextFactory());

        if (readTimeout > 0) {
            httpClient.setIdleTimeout(readTimeout);
        }

        httpClient.start();
        addAuthHeader();

        ContentResponse response = httpClient.newRequest(url)
                .method(HttpMethod.POST)
                .param(ROPConstants.OPERATION_PARAMETER, ROPConstants.ESTABLISH_SHARED_SESSION_OPERATION)
                .param(ROPConstants.SESSION_NAME_PARAMETER, name)
                .send();

        return new ByteArrayInputStream(response.getContent());
    }

    @Override
    public InputStream sendMessage(byte[] message) throws Exception {
        Request request = httpClient.newRequest(url)
                .method(HttpMethod.POST)
                .content(new BytesContentProvider(message));

        addSessionCookie(request);

        ContentResponse response = request.send();
        return new ByteArrayInputStream(response.getContent());
    }

    @Override
    public void close() throws Exception {
        if (httpClient != null) {
            if (logger.isInfoEnabled()) {
                logger.info(ROPUtil.getLogDisconnect(url, username, password));
            }

            httpClient.stop();
        }
    }

    protected void addAuthHeader() {
        if (username != null && password != null) {
            if (realm == null && logger.isWarnEnabled()) {
                logger.warn("In order to use HighHttp2ROPConnector with BASIC Authentication " +
                        "you should provide Constants.ROP_SERVICE_REALM_PROPERTY");
                return;
            }

            httpClient.getAuthenticationStore()
                    .addAuthentication(new BasicAuthentication(URI.create(url), realm, username, password));
        }

    }

    protected void addSessionCookie(Request request) {
        if (clientConnection == null)
            return;

        RemoteSession session = clientConnection.getSession();
        if (session != null && session.getSessionId() != null) {
            request.header(HttpHeader.COOKIE, SESSION_COOKIE_NAME
                    + "="
                    + session.getSessionId());
        }
    }

    protected HttpClientTransportOverHTTP2 getClientTransport() {
        if (ALPN) return new HttpClientTransportOverHTTP2(new HTTP2Client());

        return new HttpClientTransportOverHTTP2(new HTTP2Client()) {
            @Override
            public Connection newConnection(EndPoint endPoint, Map<String, Object> context) throws IOException {
                return new HTTP2ClientConnectionFactory().newConnection(endPoint, context);
            }
        };
    }
}
