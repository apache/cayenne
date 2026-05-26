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

import org.apache.cayenne.mcp.tools.cgen.protocol.CgenFileEntry;
import org.apache.cayenne.mcp.tools.cgen.protocol.CgenFileKind;
import org.apache.cayenne.mcp.tools.cgen.protocol.CgenRunResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that drive cgen against fixture projects written into a temp directory.
 * These tests intentionally run via Failsafe (IT suffix) so the shaded JAR is available.
 */
public class CgenRunIT {

    @TempDir
    Path tempDir;

    private CgenRunTool tool;
    private Path projectFile;
    private Path destDir;

    @BeforeEach
    void setUp() throws IOException {
        tool = new CgenRunTool();
        destDir = tempDir.resolve("generated");
        projectFile = writeFixture("PersonMap", "com.example", destDir, true);
    }

    @Test
    public void generatesFilesOnFirstRun() {
        CgenRunResult result = tool.run(projectFile.toString(), "PersonMap");

        assertEquals("generated", result.status());
        assertNotNull(result.resolved());
        assertEquals(destDir.toAbsolutePath().toString(), result.resolved().destDir());

        assertTrue(result.validation().projectFound());
        assertTrue(result.validation().dataMapFound());
        assertTrue(result.validation().cgenConfigPresent());
        assertTrue(result.validation().destDirSpecified());
        assertTrue(result.validation().destDirWritable());
        assertNull(result.error());

        // makePairs=true → superclass + subclass for each entity
        int filesWritten = result.summary().filesWritten();
        assertTrue(filesWritten > 0, "Expected at least one generated file");
        assertEquals(filesWritten, result.files().size());
        assertEquals(result.summary().filesConsidered(), result.files().size(),
                "All considered files should have been written on a fresh run");

        List<CgenFileKind> kinds = result.files().stream().map(CgenFileEntry::kind).toList();
        assertTrue(kinds.contains(CgenFileKind.entity_super), "Expected entity_super file");
        assertTrue(kinds.contains(CgenFileKind.entity_sub), "Expected entity_sub file");

        for (CgenFileEntry entry : result.files()) {
            assertTrue(Files.exists(Path.of(entry.path())),
                    "Generated file should exist on disk: " + entry.path());
            assertEquals("Person", entry.sourceEntity());
        }
    }

    @Test
    public void upToDateOnSecondRun() {
        tool.run(projectFile.toString(), "PersonMap");

        CgenRunResult second = tool.run(projectFile.toString(), "PersonMap");

        assertEquals("up_to_date", second.status());
        assertEquals(0, second.summary().filesWritten());
        assertTrue(second.files().isEmpty());
        assertTrue(second.summary().filesConsidered() > 0);
        assertNull(second.error());
    }

    @Test
    public void subclassNotOverwrittenWhenMakePairsIsTrue() throws IOException {
        // Pre-create the subclass file so it already exists before cgen runs.
        // With makePairs=true, cgen must never overwrite an existing subclass.
        Path subclassFile = destDir.resolve("com/example/Person.java");
        Files.createDirectories(subclassFile.getParent());
        Files.writeString(subclassFile, "// existing subclass\n");

        CgenRunResult result = tool.run(projectFile.toString(), "PersonMap");

        // The superclass (_Person.java) should be generated; the subclass should not.
        List<String> generatedPaths = result.files().stream().map(CgenFileEntry::path).toList();
        assertTrue(generatedPaths.stream().noneMatch(p -> p.endsWith("Person.java") && !p.contains("_")),
                "Existing subclass must not appear in written files");

        int skipped = result.summary().filesConsidered() - result.summary().filesWritten();
        assertTrue(skipped >= 1, "At least one file (the existing subclass) should have been skipped");
    }

    @Test
    public void regeneratesAfterDataMapChange() throws IOException {
        // First run — generates files
        CgenRunResult first = tool.run(projectFile.toString(), "PersonMap");
        assertEquals("generated", first.status());

        // Bump the DataMap's mtime to be clearly newer than the generated files.
        // Use setLastModifiedTime rather than a wall-clock sleep to avoid
        // filesystem mtime granularity issues (Windows has 1-second resolution).
        long maxGeneratedMtime = first.files().stream()
                .mapToLong(e -> Path.of(e.path()).toFile().lastModified())
                .max()
                .orElseThrow();
        Path dataMapFile = tempDir.resolve("PersonMap.map.xml");
        Files.setLastModifiedTime(dataMapFile, FileTime.fromMillis(maxGeneratedMtime + 5_000L));

        // Second run — must detect that the DataMap is newer than the generated files
        // and regenerate the superclass(es).
        CgenRunResult second = tool.run(projectFile.toString(), "PersonMap");
        assertEquals("generated", second.status(),
                "Expected regeneration after DataMap mtime was bumped past generated files");
        assertTrue(second.summary().filesWritten() > 0);
    }

    private Path writeFixture(String mapName, String pkg, Path destDir, boolean makePairs) throws IOException {

        // Project descriptor
        Path projectDescriptor = tempDir.resolve("cayenne-project.xml");
        Files.writeString(projectDescriptor, String.format("""
                <?xml version="1.0" encoding="utf-8"?>
                <domain xmlns="http://cayenne.apache.org/schema/12/domain" project-version="12">
                    <map name="%s"/>
                </domain>
                """, mapName));

        // DataMap with one entity and embedded cgen config
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

        return projectDescriptor;
    }
}
