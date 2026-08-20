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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

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

    /**
     * An absent {@code <destDir>} means the same thing as the {@code <destDir>.</destDir>} the Cayenne
     * encoder writes when no output directory is configured: generate next to the DataMap. It used to be
     * a validation failure, which made a saved project behave differently depending on whether the tag
     * had ever been written out.
     */
    @Test
    public void destDirDefaultsToTheDataMapDirectory(@TempDir Path tempDir) throws IOException {
        Path projectFile = copyFixture("no-destdir", tempDir);

        CgenRunResult result = tool.run(projectFile.toString(), "TestMap");

        assertNull(result.error());
        assertEquals("generated", result.status());
        assertEquals(tempDir.toAbsolutePath().toString(), result.resolved().destDir());

        assertTrue(result.validation().projectFound());
        assertTrue(result.validation().dataMapFound());
        assertTrue(result.validation().cgenConfigPresent());
        assertTrue(result.validation().destDirSpecified());
        assertTrue(result.validation().destDirWritable());
    }

    private static Path copyFixture(String fixture, Path targetDir) throws IOException {
        Path source = Paths.get(URI.create(CgenRunValidationTest.class
                .getResource("/cgen-fixtures/" + fixture).toString()));
        try (Stream<Path> files = Files.list(source)) {
            for (Path file : files.toList()) {
                Files.copy(file, targetDir.resolve(file.getFileName().toString()));
            }
        }
        return targetDir.resolve("cayenne-project.xml");
    }

    private static String fixtureProject(String fixture) throws URISyntaxException {
        return Paths.get(CgenRunValidationTest.class
                .getResource("/cgen-fixtures/" + fixture + "/cayenne-project.xml")
                .toURI()).toString();
    }
}
