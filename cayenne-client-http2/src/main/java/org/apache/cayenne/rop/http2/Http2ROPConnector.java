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
import org.apache.cayenne.rop.HttpClientConnection;
import org.apache.cayenne.rop.ROPConnector;
import org.apache.cayenne.rop.ROPConstants;
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
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;

public class Http2ROPConnector implements ROPConnector {

    private static Log logger = LogFactory.getLog(Http2ROPConnector.class);

    public static final String SESSION_COOKIE_NAME = "JSESSIONID";

    private HttpClient httpClient;
    private HttpClientConnection clientConnection;

    private String url;

    private String username;
    private String password;
    private String realm;

    private long readTimeout;

    public Http2ROPConnector(String url, String username, String password, String realm) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.realm = realm;
    }

    public void setClientConnection(HttpClientConnection clientConnection) {
        this.clientConnection = clientConnection;
    }

    public void setReadTimeout(Long readTimeout) {
        this.readTimeout = readTimeout;
    }

    @Override
    public InputStream establishSession() {
        if (logger.isInfoEnabled()) {
            logConnect(null);
        }

        try {
            close();
            httpClient = new HttpClient(new HttpClientTransportOverHTTP2(new HTTP2Client()), new SslContextFactory());
            httpClient.start();
            addAuthHeader();

            ContentResponse response = httpClient.newRequest(url)
                    .method(HttpMethod.POST)
                    .param(ROPConstants.OPERATION_PARAMETER, ROPConstants.ESTABLISH_SESSION_OPERATION)
                    .timeout(readTimeout, TimeUnit.SECONDS)
                    .send();

            return new ByteArrayInputStream(response.getContent());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public InputStream establishSharedSession(String name) {
        if (logger.isInfoEnabled()) {
            logConnect(name);
        }

        try {
            close();
            httpClient = new HttpClient(new HttpClientTransportOverHTTP2(new HTTP2Client()), new SslContextFactory());
            httpClient.start();
            addAuthHeader();

            ContentResponse response = httpClient.newRequest(url)
                    .method(HttpMethod.POST)
                    .param(ROPConstants.OPERATION_PARAMETER, ROPConstants.ESTABLISH_SHARED_SESSION_OPERATION)
                    .param(ROPConstants.SESSION_NAME_PARAMETER, name)
                    .timeout(readTimeout, TimeUnit.SECONDS)
                    .send();

            return new ByteArrayInputStream(response.getContent());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public InputStream sendMessage(byte[] message) {
        try {
            Request request = httpClient.newRequest(url)
                    .method(HttpMethod.POST)
                    .content(new BytesContentProvider(message))
                    .timeout(readTimeout, TimeUnit.SECONDS);

            addSessionCookie(request);

            ContentResponse response = request.send();
            return new ByteArrayInputStream(response.getContent());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            if (httpClient != null) {
                if (logger.isInfoEnabled()) {
                    logDisconnect();
                }

                httpClient.stop();
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    protected void addAuthHeader() {
        if (username != null && password != null && realm != null) {
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

    private void logConnect(String sharedSessionName) {
        StringBuilder log = new StringBuilder("Connecting to [");
        if (username != null) {
            log.append(username);

            if (password != null) {
                log.append(":*******");
            }

            log.append("@");
        }

        log.append(url);
        log.append("]");

        if (sharedSessionName != null) {
            log.append(" - shared session '").append(sharedSessionName).append("'");
        } else {
            log.append(" - dedicated session.");
        }

        logger.info(log.toString());
    }

    private void logDisconnect() {
        StringBuilder log = new StringBuilder("Disconnecting from [");
        if (username != null) {
            log.append(username);

            if (password != null) {
                log.append(":*******");
            }

            log.append("@");
        }

        log.append(url);
        log.append("]");

        logger.info(log.toString());
    }

}
