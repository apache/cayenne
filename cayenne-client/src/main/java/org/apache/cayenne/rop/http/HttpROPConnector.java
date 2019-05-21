/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.rop.http;

import org.apache.cayenne.remote.RemoteSession;
import org.apache.cayenne.rop.HttpClientConnection;
import org.apache.cayenne.rop.ROPConnector;
import org.apache.cayenne.rop.ROPConstants;
import org.apache.cayenne.rop.ROPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpROPConnector implements ROPConnector {

    private static final Logger logger = LoggerFactory.getLogger(HttpROPConnector.class);

    public static final String SESSION_COOKIE_NAME = "JSESSIONID";

    private HttpClientConnection clientConnection;

    private String url;

    private String username;
    private String password;

    private Long readTimeout;
    
    public HttpROPConnector(String url, String username, String password) {
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
    public InputStream establishSession() throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info(ROPUtil.getLogConnect(url, username, password != null));
        }
		
		Map<String, String> requestParams = new HashMap<>();
		requestParams.put(ROPConstants.OPERATION_PARAMETER, ROPConstants.ESTABLISH_SESSION_OPERATION);
		
        return doRequest(requestParams);
    }

    @Override
    public InputStream establishSharedSession(String sharedSessionName) throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info(ROPUtil.getLogConnect(url, username, password != null, sharedSessionName));
        }

		Map<String, String> requestParams = new HashMap<>();
		requestParams.put(ROPConstants.OPERATION_PARAMETER, ROPConstants.ESTABLISH_SHARED_SESSION_OPERATION);
		requestParams.put(ROPConstants.SESSION_NAME_PARAMETER, sharedSessionName);

		return doRequest(requestParams);
    }

    @Override
    public InputStream sendMessage(byte[] message) throws IOException {
        return doRequest(message);
    }

    @Override
    public void close() throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info(ROPUtil.getLogDisconnect(url, username, password != null));
        }
    }

    protected InputStream doRequest(Map<String, String> params) throws IOException {
        URLConnection connection = new URL(url).openConnection();

        if (readTimeout != null) {
            connection.setReadTimeout(readTimeout.intValue());
        }

        addAuthHeader(connection);

        connection.setDoOutput(true);

        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("charset", "utf-8");

        try (OutputStream output = connection.getOutputStream()) {
            output.write(ROPUtil.getParamsAsString(params).getBytes(StandardCharsets.UTF_8));
            output.flush();
        }

        return connection.getInputStream();
    }

    protected InputStream doRequest(byte[] data) throws IOException {
        URLConnection connection = new URL(url).openConnection();

        if (readTimeout != null) {
            connection.setReadTimeout(readTimeout.intValue());
        }

        addAuthHeader(connection);
        addSessionCookie(connection);
        connection.setDoOutput(true);

        connection.setRequestProperty("Content-Type", "application/octet-stream");

        if (data != null) {
            try (OutputStream output = connection.getOutputStream()) {
                output.write(data);
                output.flush();
            }
        }

        return connection.getInputStream();
    }

    protected void addAuthHeader(URLConnection connection) {
        String basicAuth = ROPUtil.getBasicAuth(username, password);

        if (basicAuth != null) {
            connection.addRequestProperty("Authorization", basicAuth);
        }
    }

    protected void addSessionCookie(URLConnection connection) {
        RemoteSession session = clientConnection.getSession();
        if (session != null && session.getSessionId() != null) {
            connection.addRequestProperty("Cookie", SESSION_COOKIE_NAME
                    + "="
                    + session.getSessionId());
        }
    }

}
