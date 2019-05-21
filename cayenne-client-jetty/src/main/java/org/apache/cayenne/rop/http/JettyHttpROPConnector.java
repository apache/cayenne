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

package org.apache.cayenne.rop.http;

import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.remote.RemoteSession;
import org.apache.cayenne.rop.*;
import org.slf4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * This implementation of ROPConnector uses Jetty HTTP Client.
 * Depends on {@link ClientConnection} provider it uses HTTP/1.1 or HTTP/2 protocol.
 * <p>
 * {@link JettyHttpClientConnectionProvider} for HTTP/1.1 protocol.
 * {@link JettyHttp2ClientConnectionProvider} for HTTP/2 protocol.
 */
public class JettyHttpROPConnector implements ROPConnector {

    private static final Logger logger = LoggerFactory.getLogger(JettyHttpROPConnector.class);

    public static final String SESSION_COOKIE_NAME = "JSESSIONID";

    protected HttpClient httpClient;
    protected HttpClientConnection clientConnection;

    protected String url;
    protected String username;

    protected Long readTimeout = 5l;

    public JettyHttpROPConnector(HttpClient httpClient, String url, String username) {
        if (httpClient == null) {
            throw new IllegalArgumentException("org.eclipse.jetty.client.HttpClient should be provided " +
                    "for this ROPConnector implementation.");
        }

        this.httpClient = httpClient;
        this.url = url;
        this.username = username;
    }

    public void setClientConnection(HttpClientConnection clientConnection) {
        this.clientConnection = clientConnection;
    }

    public void setReadTimeout(Long readTimeout) {
        this.readTimeout = readTimeout;
    }

    @Override
    public InputStream establishSession() throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info(ROPUtil.getLogConnect(url, username, true));
        }

        try {
            ContentResponse response = httpClient.newRequest(url)
                    .method(HttpMethod.POST)
                    .param(ROPConstants.OPERATION_PARAMETER, ROPConstants.ESTABLISH_SESSION_OPERATION)
                    .timeout(readTimeout, TimeUnit.SECONDS)
                    .send();

            return new ByteArrayInputStream(response.getContent());
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new IOException("Exception while establishing session", e);
        }
    }

    @Override
    public InputStream establishSharedSession(String sharedSessionName) throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info(ROPUtil.getLogConnect(url, username, true, sharedSessionName));
        }

        try {
            ContentResponse response = httpClient.newRequest(url)
                    .method(HttpMethod.POST)
                    .param(ROPConstants.OPERATION_PARAMETER, ROPConstants.ESTABLISH_SHARED_SESSION_OPERATION)
                    .param(ROPConstants.SESSION_NAME_PARAMETER, sharedSessionName)
                    .timeout(readTimeout, TimeUnit.SECONDS)
                    .send();

            return new ByteArrayInputStream(response.getContent());
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new IOException("Exception while establishing shared session: " + sharedSessionName, e);
        }
    }

    @Override
    public InputStream sendMessage(byte[] message) throws IOException {
        try {
            Request request = httpClient.newRequest(url)
                    .method(HttpMethod.POST)
                    .header(HttpHeader.CONTENT_TYPE, "application/octet-stream")
                    .header(HttpHeader.ACCEPT_ENCODING, "gzip")
                    .content(new BytesContentProvider(message));

            addSessionCookie(request);

            InputStreamResponseListener listener = new InputStreamResponseListener();
            request.send(listener);

            /**
             * Waits for the given timeout for the response to be available, then returns it.
             * The wait ends as soon as all the HTTP headers have been received, without waiting for the content.
             */
            Response response = listener.get(readTimeout, TimeUnit.SECONDS);

            if (response.getStatus() >= 300) {
                throw new IOException(
                        "Did not receive successful HTTP response: status code = " + response.getStatus() +
                                ", status message = [" + response.getReason() + "]");
            }

            return listener.getInputStream();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new IOException("Exception while sending message", e);
        }
    }

    @Override
    public void close() throws IOException {
        if (httpClient != null) {
            if (logger.isInfoEnabled()) {
                logger.info(ROPUtil.getLogDisconnect(url, username, true));
            }

            try {
                httpClient.stop();
            } catch (Exception e) {
                throw new IOException("Exception while stopping Jetty HttpClient", e);
            }
        }
    }

    protected void addSessionCookie(Request request) {
        if (clientConnection != null) {
            RemoteSession session = clientConnection.getSession();

            if (session != null && session.getSessionId() != null) {
                request.header(HttpHeader.COOKIE, SESSION_COOKIE_NAME
                        + "="
                        + session.getSessionId());
            }
        }
    }
}
