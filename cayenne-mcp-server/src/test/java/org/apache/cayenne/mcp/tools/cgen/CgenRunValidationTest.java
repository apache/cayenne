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

import org.apache.cayenne.mcp.tools.cgen.protocol.CgenErrorCode;
import org.apache.cayenne.mcp.tools.cgen.protocol.CgenRunResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class CgenRunValidationTest {

    // Shared tool instance — DI bootstrap is expensive, reuse across test methods.
    private static CgenRunTool tool;

    @BeforeAll
    public static void setUp() {
        tool = new CgenRunTool();
    }

    @Test
    public void projectNotFound() {
        CgenRunResult result = tool.run("/no/such/file/cayenne-project.xml", "TestMap");

        assertEquals("validation_failed", result.status());
        assertEquals(0, result.summary().filesConsidered());
        assertEquals(0, result.summary().filesWritten());
        assertTrue(result.files().isEmpty());
        assertNull(result.resolved());
        assertEquals(CgenErrorCode.project_not_found, result.error().code());

        assertFalse(result.validation().projectFound());
        assertNull(result.validation().dataMapFound());
        assertNull(result.validation().cgenConfigPresent());
        assertNull(result.validation().destDirSpecified());
        assertNull(result.validation().destDirWritable());
    }

    @Test
    public void projectParseFailed(@TempDir Path tmp) throws IOException {
        Path badXml = tmp.resolve("cayenne-project.xml");
        Files.writeString(badXml, "this is not xml <<< garbage");

        CgenRunResult result = tool.run(badXml.toString(), "TestMap");

        assertEquals("validation_failed", result.status());
        assertEquals(CgenErrorCode.project_parse_failed, result.error().code());

        assertTrue(result.validation().projectFound());
        assertNull(result.validation().dataMapFound());
        assertNull(result.validation().cgenConfigPresent());
        assertNull(result.validation().destDirSpecified());
        assertNull(result.validation().destDirWritable());
    }

    @Test
    public void dataMapNotFound() throws URISyntaxException {
        String projectPath = fixtureProject("no-cgen");

        CgenRunResult result = tool.run(projectPath, "NoSuchMap");

        assertEquals("validation_failed", result.status());
        assertEquals(CgenErrorCode.datamap_not_found, result.error().code(),
                "Unexpected error: " + result.error().message());
        assertTrue(result.error().message().contains("NoSuchMap"));
        assertTrue(result.error().message().contains("TestMap"), "Available maps should be listed");

        assertTrue(result.validation().projectFound());
        assertFalse(result.validation().dataMapFound());
        assertNull(result.validation().cgenConfigPresent());
        assertNull(result.validation().destDirSpecified());
        assertNull(result.validation().destDirWritable());
    }

    @Test
    public void cgenConfigMissing() throws URISyntaxException {
        String projectPath = fixtureProject("no-cgen");

        CgenRunResult result = tool.run(projectPath, "TestMap");

        assertEquals("validation_failed", result.status());
        assertEquals(CgenErrorCode.cgen_config_missing, result.error().code());

        assertTrue(result.validation().projectFound());
        assertTrue(result.validation().dataMapFound());
        assertFalse(result.validation().cgenConfigPresent());
        assertNull(result.validation().destDirSpecified());
        assertNull(result.validation().destDirWritable());
    }

    @Test
    public void destDirNotSpecified() throws URISyntaxException {
        String projectPath = fixtureProject("no-destdir");

        CgenRunResult result = tool.run(projectPath, "TestMap");

        assertEquals("validation_failed", result.status());
        assertEquals(CgenErrorCode.destdir_not_specified, result.error().code());

        assertTrue(result.validation().projectFound());
        assertTrue(result.validation().dataMapFound());
        assertTrue(result.validation().cgenConfigPresent());
        assertFalse(result.validation().destDirSpecified());
        assertNull(result.validation().destDirWritable());
    }

    private static String fixtureProject(String fixture) throws URISyntaxException {
        return Paths.get(CgenRunValidationTest.class
                .getResource("/cgen-fixtures/" + fixture + "/cayenne-project.xml")
                .toURI()).toString();
    }
}
