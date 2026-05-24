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

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 5 extension that wraps the in-process MCP server lifecycle and provides methods
 * for sending JSON-RPC requests, reading responses, and extracting tool-result payloads.
 * Declare as an instance field annotated with {@code @RegisterExtension}.
 */
public class TestMcpClient implements BeforeEachCallback, AfterEachCallback {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private TestMcpServer server;
    private BufferedWriter writer;
    private BufferedReader reader;

    @Override
    public void beforeEach(ExtensionContext context) {
        server = TestMcpServer.start();
        writer = new BufferedWriter(new OutputStreamWriter(server.getOutputStream()));
        reader = new BufferedReader(new InputStreamReader(server.getInputStream()));

        send("""
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{\
                "protocolVersion":"2024-11-05",\
                "capabilities":{},\
                "clientInfo":{"name":"test","version":"1.0"}}}""");
        String initResponse = readLine(5_000);
        assertTrue(initResponse.contains("\"id\":1"), "initialize response missing: " + initResponse);
        send("""
                {"jsonrpc":"2.0","method":"notifications/initialized","params":{}}""");
    }

    @Override
    public void afterEach(ExtensionContext context) {
        try {
            writer.close();
            assertTrue(server.waitFor(10, TimeUnit.SECONDS), "Server thread did not stop after stdin was closed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for server to stop", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to close client", e);
        }
    }

    /**
     * Sends a JSON-RPC message to the server.
     */
    public void send(String json) {
        try {
            writer.write(json);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to send message", e);
        }
    }

    /**
     * Reads the next non-blank response line, blocking up to {@code timeoutMs} milliseconds.
     */
    public String readLine(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        try {
            while (System.currentTimeMillis() < deadline) {
                if (reader.ready()) {
                    String line = reader.readLine();
                    if (line != null && !line.isBlank()) {
                        return line;
                    }
                }
                Thread.sleep(20);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for response", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read response", e);
        }
        throw new AssertionError("No response within " + timeoutMs + "ms");
    }

    /**
     * Reads the next response line and returns the pretty-printed tool result JSON payload.
     */
    public String readLineAsJson(long timeoutMs) {
        String mcpResponse = readLine(timeoutMs);
        JsonNode root = MAPPER.readTree(mcpResponse);
        String text = root.at("/result/content/0/text").asString();
        JsonNode payload = MAPPER.readTree(text);
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(payload)
                .replace("\r\n", "\n");
    }

    /**
     * Sends a {@code tools/call} JSON-RPC request for a tool that accepts
     * {@code projectPath} and {@code dataMap} arguments.
     */
    public void sendToolCall(int id, String toolName, Path project, String dataMap) {
        String escapedPath = project.toString().replace("\\", "\\\\");
        send("""
                {"jsonrpc":"2.0","id":%d,"method":"tools/call","params":\
                {"name":"%s","arguments":\
                {"projectPath":"%s","dataMap":"%s"}}}""".formatted(id, toolName, escapedPath, dataMap));
    }
}
