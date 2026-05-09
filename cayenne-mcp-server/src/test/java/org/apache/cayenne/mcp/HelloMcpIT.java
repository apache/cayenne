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
package org.apache.cayenne.mcp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the hello tool over the MCP stdio protocol.
 * Requires the shaded jar to be built before this test runs (failsafe executes after package).
 */
public class HelloMcpIT {

    private Process process;
    private BufferedWriter writer;
    private BufferedReader reader;

    @BeforeEach
    void startServer() throws Exception {
        process = McpStarter.start();

        Thread stderrDrain = new Thread(() -> {
            try (InputStream in = process.getErrorStream()) {
                in.transferTo(OutputStream.nullOutputStream());
            } catch (IOException ignored) {
            }
        });
        stderrDrain.setDaemon(true);
        stderrDrain.start();

        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        sendMessage("""
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{\
                "protocolVersion":"2024-11-05",\
                "capabilities":{},\
                "clientInfo":{"name":"test","version":"1.0"}}}""");
        String initResponse = readLine();
        assertTrue(initResponse.contains("\"id\":1"), "initialize response missing: " + initResponse);

        sendMessage("""
                {"jsonrpc":"2.0","method":"notifications/initialized","params":{}}""");
    }

    @AfterEach
    void stopServer() throws Exception {
        writer.close();
        assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Server process did not exit after stdin was closed");
    }

    @Test
    public void toolsListIncludesHello() throws Exception {
        sendMessage("""
                {"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}""");

        String listResponse = readLine();
        assertTrue(listResponse.contains("\"hello\""), "tools/list missing 'hello' tool: " + listResponse);
    }

    @Test
    public void helloToolReturnsGreeting() throws Exception {
        sendMessage("""
                {"jsonrpc":"2.0","id":3,"method":"tools/call",\
                "params":{"name":"hello","arguments":{}}}""");

        String callResponse = readLine();
        assertTrue(callResponse.contains("hello world"), "tools/call missing 'hello world': " + callResponse);
        assertTrue(callResponse.contains("\"id\":3"), "tools/call response id mismatch: " + callResponse);
    }

    private void sendMessage(String json) throws IOException {
        writer.write(json);
        writer.newLine();
        writer.flush();
    }

    private String readLine() throws Exception {
        long deadline = System.currentTimeMillis() + 5000L;
        while (System.currentTimeMillis() < deadline) {
            if (reader.ready()) {
                String line = reader.readLine();
                if (line != null && !line.isBlank()) {
                    return line;
                }
            }
            Thread.sleep(20);
        }
        throw new AssertionError("No response within " + 5000L + "ms");
    }


}
