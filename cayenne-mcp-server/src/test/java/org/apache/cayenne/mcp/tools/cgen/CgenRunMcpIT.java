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

import org.apache.cayenne.mcp.TestMcpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the {@code cgen_run} tool over the MCP stdio protocol.
 * Complements {@link CgenRunIT}, which calls the tool directly; this class exercises
 * the full JSON-RPC round-trip through the in-process MCP server.
 */
public class CgenRunMcpIT {

    @RegisterExtension
    TestMcpClient client = new TestMcpClient();

    @TempDir
    Path tempDir;

    private Path projectFile;
    private Path destDir;

    @BeforeEach
    void setUp() throws IOException {
        destDir = tempDir.resolve("generated");
        writeFixture("PersonMap", "com.example", destDir, true);
        projectFile = tempDir.resolve("cayenne-project.xml");
    }

    @Test
    public void toolsListIncludesCgenRun() {
        client.send("""
                {"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}""");

        String response = client.readLine(5_000);
        assertTrue(response.contains("\"cgen_run\""), "tools/list missing 'cgen_run': " + response);
    }

    @Test
    public void projectNotFoundReturnsValidationError() {
        client.send("""
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
                }""", client.readLineAsJson(5_000));
    }

    @Test
    public void generatesFilesOnFirstRun() {
        client.sendToolCall(4, "cgen_run", projectFile, "PersonMap");

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
                }""".formatted(superPath, subPath, destPath), client.readLineAsJson(15_000));
    }

    @Test
    public void upToDateOnSecondRun() {
        client.sendToolCall(5, "cgen_run", projectFile, "PersonMap");
        client.readLine(15_000);

        client.sendToolCall(6, "cgen_run", projectFile, "PersonMap");

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
                }""".formatted(destPath), client.readLineAsJson(15_000));
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
