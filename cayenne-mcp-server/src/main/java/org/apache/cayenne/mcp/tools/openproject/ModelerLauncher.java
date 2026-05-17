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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the {@link ProcessBuilder} argv for each Modeler launcher kind and starts
 * the process. Stdout/stderr are discarded so they cannot pollute the MCP server's
 * JSON-RPC stream.
 *
 * @since 5.0
 */
final class ModelerLauncher {

    record LaunchResult(Process process, List<String> command, boolean processAlivenessMeaningful) {}

    private ModelerLauncher() {
    }

    static List<String> buildCommand(LauncherKind kind, Path launcher, Path projectPath, String nonce) {
        List<String> args = new ArrayList<>();
        switch (kind) {
            case MAC_APP -> {
                args.add("open");
                args.add("-n");
                args.add(launcher.toString());
                args.add("--args");
                args.add("--mcp-handshake");
                args.add(nonce);
                args.add(projectPath.toString());
            }
            case WINDOWS_EXE -> {
                args.add(launcher.toString());
                args.add("--mcp-handshake");
                args.add(nonce);
                args.add(projectPath.toString());
            }
            case GENERIC_JAR -> {
                args.add(currentJavaBinary().toString());
                args.add("-jar");
                args.add(launcher.toString());
                args.add("--mcp-handshake");
                args.add(nonce);
                args.add(projectPath.toString());
            }
        }
        return List.copyOf(args);
    }

    static LaunchResult launch(LauncherKind kind, Path launcher, Path projectPath, String nonce)
            throws IOException {

        List<String> command = buildCommand(kind, launcher, projectPath, nonce);

        ProcessBuilder pb = new ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .directory(workingDirectory(kind, launcher).toFile());

        Process process = pb.start();

        // Process.isAlive() only carries signal for processes we own end-to-end.
        // On Mac, the process we spawn is `open`, which exits in milliseconds with
        // no relationship to whether the Modeler actually started.
        boolean alivenessMeaningful = kind != LauncherKind.MAC_APP;

        return new LaunchResult(process, command, alivenessMeaningful);
    }

    private static Path workingDirectory(LauncherKind kind, Path launcher) {
        if (kind == LauncherKind.MAC_APP) {
            // launcher is the .app directory; the .app's parent is the install directory.
            Path parent = launcher.getParent();
            return parent != null ? parent : launcher;
        }
        Path parent = launcher.getParent();
        return parent != null ? parent : launcher;
    }

    /**
     * Returns the {@code java} binary from the JVM the MCP server is running on.
     * We deliberately do <em>not</em> consult {@code JAVA_HOME} or {@code PATH} —
     * those can point at a JRE older than the Modeler needs.
     */
    static Path currentJavaBinary() {
        String javaHome = System.getProperty("java.home");
        String exe = OsKind.detect() == OsKind.WINDOWS ? "java.exe" : "java";
        return Paths.get(javaHome, "bin", exe);
    }
}
