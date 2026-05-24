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
package org.apache.cayenne.mcp.tools.openproject;

import org.apache.cayenne.mcp.tools.openproject.ModelerDiscovery.DiscoveryResult;
import org.apache.cayenne.mcp.tools.openproject.ModelerDiscovery.Found;
import org.apache.cayenne.mcp.tools.openproject.ModelerDiscovery.NotFound;
import org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectDistribution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModelerDiscoveryTest {

    // -------- Mac --------

    @Test
    public void macAppWithLiteralName(@TempDir Path tmp) throws IOException {
        Path bundle = tmp.resolve("CayenneModeler.app");
        Path mcpDir = makeMacBundle(bundle);

        DiscoveryResult result = ModelerDiscovery.discover(mcpDir, OsKind.MAC);

        Found found = assertInstanceOf(Found.class, result);
        assertEquals(OpenProjectDistribution.mac, found.distribution());
        assertEquals(LauncherKind.MAC_APP, found.launcherKind());
        assertEquals(bundle, found.launcher());
    }

    @Test
    public void macAppWithRenamedVersionedName(@TempDir Path tmp) throws IOException {
        Path bundle = tmp.resolve("CayenneModeler-5.0.app");
        Path mcpDir = makeMacBundle(bundle);

        DiscoveryResult result = ModelerDiscovery.discover(mcpDir, OsKind.MAC);

        Found found = assertInstanceOf(Found.class, result);
        assertEquals(bundle, found.launcher(),
                "modelerPath must reflect the actual on-disk bundle name, even if renamed");
    }

    @Test
    public void macAppWithSpacesInName(@TempDir Path tmp) throws IOException {
        Path bundle = tmp.resolve("My Cayenne Modeler.app");
        Path mcpDir = makeMacBundle(bundle);

        DiscoveryResult result = ModelerDiscovery.discover(mcpDir, OsKind.MAC);

        assertInstanceOf(Found.class, result);
    }

    @Test
    public void macAppRejectedWhenMacOsDirAbsent(@TempDir Path tmp) throws IOException {
        // Build a .app that has Contents/Resources/mcp but no Contents/MacOS — corrupt bundle.
        Path bundle = tmp.resolve("CayenneModeler.app");
        Path mcpDir = bundle.resolve("Contents/Resources/mcp");
        Files.createDirectories(mcpDir);
        // Intentionally do NOT create Contents/MacOS.

        DiscoveryResult result = ModelerDiscovery.discover(mcpDir, OsKind.MAC);

        assertInstanceOf(NotFound.class, result);
    }

    // -------- Windows --------

    @Test
    public void windowsExeMatchesLiteralName(@TempDir Path tmp) throws IOException {
        Files.createFile(tmp.resolve(ModelerDiscovery.WINDOWS_EXE_NAME));

        DiscoveryResult result = ModelerDiscovery.discover(tmp, OsKind.WINDOWS);

        Found found = assertInstanceOf(Found.class, result);
        assertEquals(OpenProjectDistribution.windows, found.distribution());
        assertEquals(LauncherKind.WINDOWS_EXE, found.launcherKind());
    }

    @Test
    public void windowsExeRejectsRenamedVersion(@TempDir Path tmp) throws IOException {
        // A versioned/renamed .exe must NOT match — literal name only.
        Files.createFile(tmp.resolve("CayenneModeler-5.0.exe"));

        DiscoveryResult result = ModelerDiscovery.discover(tmp, OsKind.WINDOWS);

        NotFound nf = assertInstanceOf(NotFound.class, result);
        assertTrue(nf.probeNotes().stream().anyMatch(n -> n.contains("CayenneModeler.exe")));
    }

    @Test
    public void windowsExeMatchesEvenWithUnrelatedSiblings(@TempDir Path tmp) throws IOException {
        Files.createFile(tmp.resolve(ModelerDiscovery.WINDOWS_EXE_NAME));
        Files.createFile(tmp.resolve("helper.exe"));

        DiscoveryResult result = ModelerDiscovery.discover(tmp, OsKind.WINDOWS);

        Found found = assertInstanceOf(Found.class, result);
        assertEquals(tmp.resolve(ModelerDiscovery.WINDOWS_EXE_NAME), found.launcher());
    }

    @Test
    public void windowsNativeWinsOverGenericOnSameOs(@TempDir Path tmp) throws IOException {
        Files.createFile(tmp.resolve(ModelerDiscovery.WINDOWS_EXE_NAME));
        Files.createFile(tmp.resolve(ModelerDiscovery.GENERIC_JAR_NAME));

        DiscoveryResult result = ModelerDiscovery.discover(tmp, OsKind.WINDOWS);

        Found found = assertInstanceOf(Found.class, result);
        assertEquals(OpenProjectDistribution.windows, found.distribution(),
                "Windows-native probe must precede generic on Windows");
    }

    // -------- Generic --------

    @Test
    public void genericJarMatchesLiteralName(@TempDir Path tmp) throws IOException {
        Files.createFile(tmp.resolve(ModelerDiscovery.GENERIC_JAR_NAME));

        DiscoveryResult result = ModelerDiscovery.discover(tmp, OsKind.OTHER);

        Found found = assertInstanceOf(Found.class, result);
        assertEquals(OpenProjectDistribution.generic, found.distribution());
        assertEquals(LauncherKind.GENERIC_JAR, found.launcherKind());
    }

    @Test
    public void genericJarRejectsRenamedVersion(@TempDir Path tmp) throws IOException {
        Files.createFile(tmp.resolve("cayenne-modeler-5.0.jar"));

        DiscoveryResult result = ModelerDiscovery.discover(tmp, OsKind.OTHER);

        assertInstanceOf(NotFound.class, result);
    }

    @Test
    public void genericJarIgnoresUnrelatedJars(@TempDir Path tmp) throws IOException {
        Files.createFile(tmp.resolve(ModelerDiscovery.GENERIC_JAR_NAME));
        Files.createFile(tmp.resolve("helper.jar"));

        DiscoveryResult result = ModelerDiscovery.discover(tmp, OsKind.OTHER);

        Found found = assertInstanceOf(Found.class, result);
        assertEquals(tmp.resolve(ModelerDiscovery.GENERIC_JAR_NAME), found.launcher());
    }

    // -------- OS gate enforcement --------

    @Test
    public void osGateRejectsExeOnMac(@TempDir Path tmp) throws IOException {
        Files.createFile(tmp.resolve(ModelerDiscovery.WINDOWS_EXE_NAME));

        DiscoveryResult result = ModelerDiscovery.discover(tmp, OsKind.MAC);

        NotFound nf = assertInstanceOf(NotFound.class, result);
        // Mac eligible probes: mac, generic (2)
        assertEquals(2, nf.probeNotes().size());
        // None of the notes should mention .exe (Windows probe wasn't eligible).
        assertTrue(nf.probeNotes().stream().noneMatch(n -> n.contains(".exe")));
    }

    @Test
    public void osGateRejectsAppOnWindows(@TempDir Path tmp) throws IOException {
        Path bundle = tmp.resolve("CayenneModeler.app");
        Path mcpDir = makeMacBundle(bundle);

        DiscoveryResult result = ModelerDiscovery.discover(mcpDir, OsKind.WINDOWS);

        assertInstanceOf(NotFound.class, result);
    }

    @Test
    public void osGateRestrictsProbesOnOther(@TempDir Path tmp) throws IOException {
        Path bundle = tmp.resolve("CayenneModeler.app");
        makeMacBundle(bundle);
        Files.createFile(tmp.resolve(ModelerDiscovery.WINDOWS_EXE_NAME));

        DiscoveryResult result = ModelerDiscovery.discover(tmp, OsKind.OTHER);

        NotFound nf = assertInstanceOf(NotFound.class, result);
        // OTHER eligible probes: generic (1)
        assertEquals(1, nf.probeNotes().size());
    }

    @Test
    public void notFoundOnEmptyDir(@TempDir Path tmp) {
        DiscoveryResult result = ModelerDiscovery.discover(tmp, OsKind.OTHER);

        NotFound nf = assertInstanceOf(NotFound.class, result);
        assertEquals(1, nf.probeNotes().size(),
                "OTHER has 1 eligible probe: generic");
    }

    // -------- Helpers --------

    private static Path makeMacBundle(Path bundle) throws IOException {
        Files.createDirectories(bundle.resolve("Contents/MacOS"));
        Path mcpDir = bundle.resolve("Contents/Resources/mcp");
        Files.createDirectories(mcpDir);
        return mcpDir;
    }
}
