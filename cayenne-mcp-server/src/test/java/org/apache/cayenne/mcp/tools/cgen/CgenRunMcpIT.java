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
package org.apache.cayenne.mcp.tools.cgen;

import org.apache.cayenne.mcp.InProcessMcpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the {@code cgen_run} tool over the MCP stdio protocol.
 * Complements {@link CgenRunIT}, which calls the tool directly; this class exercises
 * the full JSON-RPC round-trip through the in-process MCP server.
 */
public class CgenRunMcpIT {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @TempDir
    Path tempDir;

    private InProcessMcpServer server;
    private BufferedWriter writer;
    private BufferedReader reader;
    private Path projectFile;
    private Path destDir;

    @BeforeEach
    void setUp() throws Exception {
        destDir = tempDir.resolve("generated");
        writeFixture("PersonMap", "com.example", destDir, true);
        projectFile = tempDir.resolve("cayenne-project.xml");

        server = InProcessMcpServer.start();
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

    @AfterEach
    void stopServer() throws Exception {
        writer.close();
        assertTrue(server.waitFor(10, TimeUnit.SECONDS), "Server thread did not stop after stdin was closed");
    }

    @Test
    public void toolsListIncludesCgenRun() throws Exception {
        send("""
                {"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}""");

        String response = readLine(5_000);
        assertTrue(response.contains("\"cgen_run\""), "tools/list missing 'cgen_run': " + response);
    }

    @Test
    public void projectNotFoundReturnsValidationError() throws Exception {
        send("""
                {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{\
                "name":"cgen_run","arguments":{\
                "projectPath":"/no/such/file.xml","dataMap":"X"}}}""");

        assertEquals("""
                {
                  "status" : "validation_failed",
                  "summary" : {
                    "filesConsidered" : 0,
                    "filesWritten" : 0
                  },
                  "files" : [ ],
                  "resolved" : null,
                  "warnings" : [ ],
                  "validation" : {
                    "projectFound" : false,
                    "dataMapFound" : null,
                    "cgenConfigPresent" : null,
                    "destDirSpecified" : null,
                    "destDirWritable" : null
                  },
                  "error" : {
                    "code" : "project_not_found",
                    "message" : "No readable file at /no/such/file.xml"
                  }
                }""", extractPayload(readLine(5_000)));
    }

    @Test
    public void generatesFilesOnFirstRun() throws Exception {
        send(callJson(4, projectFile, "PersonMap"));

        // JSON-escape backslashes so Windows paths round-trip correctly through Jackson serialization
        String superPath = destDir.resolve("com/example/auto/_Person.java").toAbsolutePath().toString().replace("\\", "\\\\");
        String subPath   = destDir.resolve("com/example/Person.java").toAbsolutePath().toString().replace("\\", "\\\\");
        String destPath  = destDir.toAbsolutePath().toString().replace("\\", "\\\\");

        assertEquals("""
                {
                  "status" : "generated",
                  "summary" : {
                    "filesConsidered" : 2,
                    "filesWritten" : 2
                  },
                  "files" : [ {
                    "path" : "%s",
                    "kind" : "entity_super",
                    "sourceEntity" : "Person"
                  }, {
                    "path" : "%s",
                    "kind" : "entity_sub",
                    "sourceEntity" : "Person"
                  } ],
                  "resolved" : {
                    "destDir" : "%s"
                  },
                  "warnings" : [ ],
                  "validation" : {
                    "projectFound" : true,
                    "dataMapFound" : true,
                    "cgenConfigPresent" : true,
                    "destDirSpecified" : true,
                    "destDirWritable" : true
                  },
                  "error" : null
                }""".formatted(superPath, subPath, destPath), extractPayload(readLine(15_000)));
    }

    @Test
    public void upToDateOnSecondRun() throws Exception {
        send(callJson(5, projectFile, "PersonMap"));
        readLine(15_000);

        send(callJson(6, projectFile, "PersonMap"));

        String destPath = destDir.toAbsolutePath().toString().replace("\\", "\\\\");

        assertEquals("""
                {
                  "status" : "up_to_date",
                  "summary" : {
                    "filesConsidered" : 2,
                    "filesWritten" : 0
                  },
                  "files" : [ ],
                  "resolved" : {
                    "destDir" : "%s"
                  },
                  "warnings" : [ ],
                  "validation" : {
                    "projectFound" : true,
                    "dataMapFound" : true,
                    "cgenConfigPresent" : true,
                    "destDirSpecified" : true,
                    "destDirWritable" : true
                  },
                  "error" : null
                }""".formatted(destPath), extractPayload(readLine(15_000)));
    }
    
    private String extractPayload(String mcpResponse) throws Exception {
        JsonNode root = MAPPER.readTree(mcpResponse);
        String text = root.at("/result/content/0/text").asText();
        JsonNode payload = MAPPER.readTree(text);
        // Normalize CRLF → LF: Jackson's DefaultPrettyPrinter uses System.lineSeparator()
        // which is \r\n on Windows, but Java text blocks always use \n.
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(payload)
                .replace("\r\n", "\n");
    }

    private String callJson(int id, Path project, String dataMap) {
        String escapedPath = project.toString().replace("\\", "\\\\");
        return String.format(
                "{\"jsonrpc\":\"2.0\",\"id\":%d,\"method\":\"tools/call\",\"params\":{" +
                "\"name\":\"cgen_run\",\"arguments\":{" +
                "\"projectPath\":\"%s\",\"dataMap\":\"%s\"}}}",
                id, escapedPath, dataMap);
    }

    private void send(String json) throws IOException {
        writer.write(json);
        writer.newLine();
        writer.flush();
    }

    private String readLine(long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (reader.ready()) {
                String line = reader.readLine();
                if (line != null && !line.isBlank()) {
                    return line;
                }
            }
            Thread.sleep(20);
        }
        throw new AssertionError("No response within " + timeoutMs + "ms");
    }

    private void writeFixture(String mapName, String pkg, Path destDir, boolean makePairs) throws IOException {
        Files.writeString(tempDir.resolve("cayenne-project.xml"), String.format("""
                <?xml version="1.0" encoding="utf-8"?>
                <domain xmlns="http://cayenne.apache.org/schema/12/domain" project-version="12">
                    <map name="%s"/>
                </domain>
                """, mapName));

        Files.writeString(tempDir.resolve(mapName + ".map.xml"), String.format("""
                <?xml version="1.0" encoding="utf-8"?>
                <data-map xmlns="http://cayenne.apache.org/schema/12/modelMap"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          project-version="12">
                    <property name="defaultPackage" value="%s"/>
                    <obj-entity name="Person" className="%s.Person"/>
                    <cgen xmlns="http://cayenne.apache.org/schema/12/cgen">
                        <destDir>%s</destDir>
                        <mode>entity</mode>
                        <makePairs>%s</makePairs>
                        <usePkgPath>true</usePkgPath>
                        <overwrite>false</overwrite>
                    </cgen>
                </data-map>
                """, pkg, pkg, destDir.toAbsolutePath(), makePairs));
    }
}
