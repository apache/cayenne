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
package org.apache.cayenne.rop.http;

import org.apache.cayenne.remote.RemoteSession;
import org.apache.cayenne.rop.HttpClientConnection;
import org.apache.cayenne.rop.ROPConnector;
import org.apache.cayenne.rop.ROPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpROPConnector implements ROPConnector {

    private static Log logger = LogFactory.getLog(HttpROPConnector.class);

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
            logConnect(null);
        }
		
		Map<String, String> requestParams = new HashMap<>();
		requestParams.put(ROPConstants.OPERATION_PARAMETER, ROPConstants.ESTABLISH_SESSION_OPERATION);
		
        return doRequest(requestParams);
    }

    @Override
    public InputStream establishSharedSession(String name) throws IOException {
        if (logger.isInfoEnabled()) {
            logConnect(name);
        }

		Map<String, String> requestParams = new HashMap<>();
		requestParams.put(ROPConstants.OPERATION_PARAMETER, ROPConstants.ESTABLISH_SHARED_SESSION_OPERATION);
		requestParams.put(ROPConstants.SESSION_NAME_PARAMETER, name);

		return doRequest(requestParams);
    }

    @Override
    public InputStream sendMessage(byte[] message) throws IOException {
        return doRequest(message);
    }
	
	protected InputStream doRequest(Map<String, String> params) throws IOException {
		URLConnection connection = new URL(url).openConnection();

		StringBuilder urlParams = new StringBuilder();

		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (urlParams.length() > 0) {
				urlParams.append('&');
			}

			urlParams.append(entry.getKey());
			urlParams.append('=');
			urlParams.append(entry.getValue());
		}

		if (readTimeout != null) {
			connection.setReadTimeout(readTimeout.intValue());
		}

		addAuthHeader(connection);

		connection.setDoOutput(true);
		
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setRequestProperty("charset", "utf-8");

		try (OutputStream output = connection.getOutputStream()) {
			output.write(urlParams.toString().getBytes(StandardCharsets.UTF_8));
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
        String basicAuth = getBasicAuth(username, password);

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

    public String getBasicAuth(String user, String password) {
        if (user != null && password != null) {
            return "Basic " + base64(user + ":" + password);
        }

        return null;
    }

    /**
     * Creates the Base64 value.
     */
    private String base64(String value) {
        StringBuffer cb = new StringBuffer();

        int i = 0;
        for (i = 0; i + 2 < value.length(); i += 3) {
            long chunk = (int) value.charAt(i);
            chunk = (chunk << 8) + (int) value.charAt(i + 1);
            chunk = (chunk << 8) + (int) value.charAt(i + 2);

            cb.append(encode(chunk >> 18));
            cb.append(encode(chunk >> 12));
            cb.append(encode(chunk >> 6));
            cb.append(encode(chunk));
        }

        if (i + 1 < value.length()) {
            long chunk = (int) value.charAt(i);
            chunk = (chunk << 8) + (int) value.charAt(i + 1);
            chunk <<= 8;

            cb.append(encode(chunk >> 18));
            cb.append(encode(chunk >> 12));
            cb.append(encode(chunk >> 6));
            cb.append('=');
        }
        else if (i < value.length()) {
            long chunk = (int) value.charAt(i);
            chunk <<= 16;

            cb.append(encode(chunk >> 18));
            cb.append(encode(chunk >> 12));
            cb.append('=');
            cb.append('=');
        }

        return cb.toString();
    }

    public static char encode(long d) {
        d &= 0x3f;
        if (d < 26)
            return (char) (d + 'A');
        else if (d < 52)
            return (char) (d + 'a' - 26);
        else if (d < 62)
            return (char) (d + '0' - 52);
        else if (d == 62)
            return '+';
        else
            return '/';
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
        }
        else {
            log.append(" - dedicated session.");
        }

        logger.info(log.toString());
    }
}
