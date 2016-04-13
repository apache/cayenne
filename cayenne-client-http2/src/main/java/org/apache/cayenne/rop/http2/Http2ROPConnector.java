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
import org.apache.cayenne.rop.ROPUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.api.server.ServerSessionListener;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.HTTP2ClientConnectionFactory;
import org.eclipse.jetty.http2.frames.DataFrame;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.io.ssl.SslClientConnectionFactory;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.FuturePromise;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This implementation of ROPConnector uses Jetty HTTP2 Client (low-level API) and directly specifies HTTP/2 protocol.
 * So you could use it without ALPN.
 */
public class Http2ROPConnector implements ROPConnector {

    private static Log logger = LogFactory.getLog(Http2ROPConnector.class);

    public static final String SESSION_COOKIE_NAME = "JSESSIONID";

    private Session session;
    private HTTP2Client http2Client;
    private HttpClientConnection clientConnection;

    private String url;

    private String username;
    private String password;

    private long readTimeout;

    public Http2ROPConnector(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public void setClientConnection(HttpClientConnection clientConnection) {
        this.clientConnection = clientConnection;
    }

    public void setReadTimeout(Long readTimeout) {
        this.readTimeout = readTimeout;
    }

    @Override
    public InputStream establishSession() throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info(ROPUtil.getLogConnect(url, username, password, null));
        }

        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(ROPConstants.OPERATION_PARAMETER, ROPConstants.ESTABLISH_SESSION_OPERATION);

        return establishSession(requestParams);
    }

    @Override
    public InputStream establishSharedSession(String name) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info(ROPUtil.getLogConnect(url, username, password, name));
        }

        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(ROPConstants.OPERATION_PARAMETER, ROPConstants.ESTABLISH_SHARED_SESSION_OPERATION);
        requestParams.put(ROPConstants.SESSION_NAME_PARAMETER, name);

        return establishSession(requestParams);
    }

    private InputStream establishSession(Map<String, String> params) throws Exception {
        close();
        http2Client = new HTTP2Client();

        SslContextFactory sslContextFactory = new SslContextFactory();
        http2Client.addBean(sslContextFactory);
        http2Client.setClientConnectionFactory((endPoint, context) ->
                new SslClientConnectionFactory(
                        sslContextFactory,
                        http2Client.getByteBufferPool(),
                        http2Client.getExecutor(),
                        new HTTP2ClientConnectionFactory())
                        .newConnection(endPoint, context));
        http2Client.start();

        HttpURI uri = new HttpURI(url);
        uri.setQuery(ROPUtil.getParamsAsString(params));

        FuturePromise<Session> sessionPromise = new FuturePromise<>();
        http2Client.connect(sslContextFactory, new InetSocketAddress(uri.getHost(), uri.getPort()), new ServerSessionListener.Adapter(), sessionPromise);
        session = sessionPromise.get(readTimeout, TimeUnit.SECONDS);

        HttpFields fields = new HttpFields();
        addAuthHeader(fields);

        MetaData.Request request = new MetaData.Request("POST", uri, HttpVersion.HTTP_2, fields);
        HeadersFrame headersFrame = new HeadersFrame(request, null, true);

        PipedOutputStream outputStream = new PipedOutputStream();
        InputStream inputStream = new PipedInputStream(outputStream);

        Stream.Listener responseListener = new Http2ROPResponseListener(outputStream);
        session.newStream(headersFrame, new FuturePromise<>(), responseListener);

        return inputStream;
    }

    @Override
    public InputStream sendMessage(byte[] message) throws Exception {
        HttpURI uri = new HttpURI(url);

        HttpFields fields = new HttpFields();
        addAuthHeader(fields);
        addSessionCookie(fields);

        MetaData.Request request = new MetaData.Request("POST", uri, HttpVersion.HTTP_2, fields);
        HeadersFrame headersFrame = new HeadersFrame(request, null, false);

        PipedOutputStream outputStream = new PipedOutputStream();
        InputStream inputStream = new PipedInputStream(outputStream);

        FuturePromise<Stream> promise = new FuturePromise<>();
        Stream.Listener responseListener = new Http2ROPResponseListener(outputStream);

        session.newStream(headersFrame, promise, responseListener);
        Stream stream = promise.get(readTimeout, TimeUnit.SECONDS);

        ByteBuffer content = BufferUtil.toBuffer(message);
        DataFrame requestContent = new DataFrame(stream.getId(), content, true);
        stream.data(requestContent, Callback.NOOP);

        return inputStream;
    }

    @Override
    public void close() throws Exception {
        if (http2Client != null) {
            if (logger.isInfoEnabled()) {
                logger.info(ROPUtil.getLogDisconnect(url, username, password));
            }

            http2Client.stop();
        }
    }

    protected void addAuthHeader(HttpFields fields) {
        String basicAuth = ROPUtil.getBasicAuth(username, password);

        if (basicAuth != null) {
            fields.add(HttpHeader.AUTHORIZATION, basicAuth);
        }
    }

    protected void addSessionCookie(HttpFields fields) {
        if (clientConnection == null)
            return;

        RemoteSession session = clientConnection.getSession();
        if (session != null && session.getSessionId() != null) {
            fields.add(HttpHeader.COOKIE, SESSION_COOKIE_NAME + "=" + session.getSessionId());
        }
    }

}
