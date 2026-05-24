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

import org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectDistribution;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Discovers a CayenneModeler installation reachable from the directory that holds
 * the running MCP jar. Pure logic: given a starting directory and an {@link OsKind},
 * it returns either a {@link Found} match or a {@link NotFound} with one note per
 * <em>eligible</em> probe (probes filtered out by the OS gate are not listed).
 *
 * @since 5.0
 */
final class ModelerDiscovery {

    /** Mac {@code .app} bundle launcher (literal, produced by {@code cayenne-modeler-mac}). */
    static final String WINDOWS_EXE_NAME = "CayenneModeler.exe";
    static final String GENERIC_JAR_NAME = "CayenneModeler.jar";

    sealed interface DiscoveryResult permits Found, NotFound {}

    record Found(OpenProjectDistribution distribution, LauncherKind launcherKind, Path launcher)
            implements DiscoveryResult {}

    record NotFound(List<String> probeNotes) implements DiscoveryResult {}

    private ModelerDiscovery() {
    }

    static DiscoveryResult discover(Path mcpDir, OsKind osKind) {
        List<String> notes = new ArrayList<>();

        if (osKind == OsKind.MAC) {
            Optional<Path> mac = probeMacApp(mcpDir);
            if (mac.isPresent()) {
                return new Found(OpenProjectDistribution.mac, LauncherKind.MAC_APP, mac.get());
            }
            notes.add("mac: no .app bundle ancestor with Contents/Resources/mcp/ and Contents/MacOS/");
        }

        if (osKind == OsKind.WINDOWS) {
            Optional<Path> win = probeWindowsExe(mcpDir);
            if (win.isPresent()) {
                return new Found(OpenProjectDistribution.windows, LauncherKind.WINDOWS_EXE, win.get());
            }
            notes.add("windows: no " + WINDOWS_EXE_NAME + " sibling of the MCP jar");
        }

        Optional<Path> generic = probeGenericJar(mcpDir);
        if (generic.isPresent()) {
            return new Found(OpenProjectDistribution.generic, LauncherKind.GENERIC_JAR, generic.get());
        }
        notes.add("generic: no " + GENERIC_JAR_NAME + " sibling of the MCP jar");

        return new NotFound(List.copyOf(notes));
    }

    /**
     * Mac bundle detection: walks up {@code mcpDir} and accepts any ancestor whose
     * name ends in {@code .app} and contains a {@code Contents/MacOS/} subdirectory.
     * The bundle name is intentionally <em>not</em> compared against a literal — users
     * routinely rename {@code .app} bundles.
     */
    static Optional<Path> probeMacApp(Path mcpDir) {
        if (mcpDir == null || mcpDir.getFileName() == null
                || !"mcp".equals(mcpDir.getFileName().toString())) {
            return Optional.empty();
        }
        Path resources = mcpDir.getParent();
        if (resources == null || resources.getFileName() == null
                || !"Resources".equals(resources.getFileName().toString())) {
            return Optional.empty();
        }
        Path contents = resources.getParent();
        if (contents == null || contents.getFileName() == null
                || !"Contents".equals(contents.getFileName().toString())) {
            return Optional.empty();
        }
        Path bundle = contents.getParent();
        if (bundle == null || bundle.getFileName() == null
                || !bundle.getFileName().toString().endsWith(".app")) {
            return Optional.empty();
        }
        if (!Files.isDirectory(bundle.resolve("Contents/MacOS"))) {
            return Optional.empty();
        }
        return Optional.of(bundle);
    }

    /** Strict literal match: {@code mcpDir/CayenneModeler.exe} must exist. */
    static Optional<Path> probeWindowsExe(Path mcpDir) {
        if (mcpDir == null) {
            return Optional.empty();
        }
        Path candidate = mcpDir.resolve(WINDOWS_EXE_NAME);
        return Files.isRegularFile(candidate) ? Optional.of(candidate) : Optional.empty();
    }

    /** Strict literal match: {@code mcpDir/CayenneModeler.jar} must exist. */
    static Optional<Path> probeGenericJar(Path mcpDir) {
        if (mcpDir == null) {
            return Optional.empty();
        }
        Path candidate = mcpDir.resolve(GENERIC_JAR_NAME);
        return Files.isRegularFile(candidate) ? Optional.of(candidate) : Optional.empty();
    }

}
