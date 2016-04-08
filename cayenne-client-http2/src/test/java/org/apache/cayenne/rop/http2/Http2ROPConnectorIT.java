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

import org.apache.cayenne.rop.ROPConstants;
import org.apache.cayenne.util.Http2Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class Http2ROPConnectorIT {

    private static final String MESSAGE = "test message";
    private static final String SEND_MESSAGE_SESSION = "send message session";

    private static Http2ROPConnector ropConnector;
    private static Http2Server server;

    @BeforeClass
    public static void setUpClass() throws Exception {
        // Start the test server
        class TestServlet extends HttpServlet {
            @Override
            protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                String sharedSessionName = req.getParameter(ROPConstants.SESSION_NAME_PARAMETER);

                if (sharedSessionName == null) {
                    resp.getOutputStream().write(MESSAGE.getBytes());
                } else if (sharedSessionName.equals(SEND_MESSAGE_SESSION)) {
                    resp.getOutputStream().write(toByteArray(req.getInputStream()));
                } else {
                    resp.getOutputStream().write((MESSAGE + " " + sharedSessionName).getBytes());
                }
            }
        }

        server = Http2Server.servlet(new TestServlet()).start();
        ropConnector = new Http2ROPConnector(server.getBasePath(), null, null, null);
    }

    @AfterClass
    public static void tearDownClass() {
        server.stop();
        ropConnector.close();
    }

    @Test
    public void testEstablishSession() throws Exception {
        String message = read(ropConnector.establishSession());
        assertEquals(MESSAGE, message);
    }

    @Test
    public void testEstablishSharedSession() throws Exception {
        String sharedSessionName = "test session";
        String message = read(ropConnector.establishSharedSession(sharedSessionName));
        assertEquals(MESSAGE + " " + sharedSessionName, message);
    }

    @Test
    public void sendMessage() throws Exception {
        ropConnector.establishSharedSession(SEND_MESSAGE_SESSION);

        byte[] message = toByteArray(ropConnector.sendMessage(MESSAGE.getBytes()));
        assertArrayEquals(MESSAGE.getBytes(), message);
    }

    private static String read(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
    }

    private static byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int reads = inputStream.read();
        while (reads != -1) {
            baos.write(reads);
            reads = inputStream.read();
        }

        return baos.toByteArray();
    }

}
