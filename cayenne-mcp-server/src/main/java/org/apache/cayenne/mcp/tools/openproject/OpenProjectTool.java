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

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.cayenne.mcp.tools.openproject.ModelerLauncher.LaunchResult;
import org.apache.cayenne.mcp.tools.openproject.ModelerResolver.Resolved;
import org.apache.cayenne.mcp.tools.openproject.ModelerResolver.Unresolved;
import org.apache.cayenne.mcp.tools.openproject.HandshakeWatcher.HandshakeData;
import org.apache.cayenne.mcp.tools.openproject.HandshakeWatcher.WatchResult;
import org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectError;
import org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectErrorCode;
import org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectHandshake;
import org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectResolved;
import org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectResult;
import org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectValidation;

import org.apache.cayenne.modeler.pref.PrefsLocator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MCP tool that launches CayenneModeler with a project file pre-loaded. The tool
 * picks a Modeler of the same version as the server (see {@link ModelerResolver}), spawns it with
 * {@code --mcp-handshake <nonce>}, and waits for the Modeler to confirm
 * successful project load via a {@link java.util.prefs.Preferences} handshake.
 *
 * @since 5.0
 */
public class OpenProjectTool {

    public static final String NAME = "open_project";

    static final Duration HANDSHAKE_TIMEOUT = Duration.ofSeconds(15);

    private final PrefsLocator prefsLocator;
    private final ModelerResolver modelerResolver;

    OpenProjectTool(PrefsLocator prefsLocator, ModelerResolver modelerResolver) {
        this.prefsLocator = prefsLocator;
        this.modelerResolver = modelerResolver;
    }

    /**
     * @param serverVersion the MCP server version, that is also the version of the Modeler to open projects in
     * @param cacheDir      a directory to cache the downloaded Modeler in, or {@code null} for the default one
     */
    public static McpServerFeatures.SyncToolSpecification spec(
            McpJsonMapper jsonMapper,
            PrefsLocator prefsLocator,
            String serverVersion,
            Path cacheDir) {

        ModelerResolver resolver = new ModelerResolver(
                OsKind.detect(),
                mcpJarDir(),
                serverVersion,
                ModelerDownloader.create(cacheDir));

        OpenProjectTool tool = new OpenProjectTool(prefsLocator, resolver);

        McpSchema.Tool descriptor = McpSchema.Tool.builder(
                        NAME,
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "projectPath", Map.of(
                                                "type", "string",
                                                "description", "Absolute path to the top-level Cayenne project descriptor (cayenne-*.xml)")
                                ),
                                "required", List.of("projectPath")
                        ))
                .description("""
                        Launch CayenneModeler with the given project file. Non-blocking; waits for \
                        the Modeler to report a startup handshake before returning. Uses the Modeler the server \
                        is bundled with, or else downloads the Modeler of the server version on the first call, \
                        which takes a few seconds longer.""")
                .build();

        return new McpServerFeatures.SyncToolSpecification(descriptor, (exchange, request) -> {
            Map<String, Object> args = request.arguments();
            String projectPath = args != null ? (String) args.getOrDefault("projectPath", "") : "";

            OpenProjectResult result = tool.run(projectPath);

            String json;
            try {
                json = jsonMapper.writeValueAsString(result);
            } catch (IOException e) {
                json = """
                        {"status":"error","error":{"code":"launch_failed","message":"Serialization failed: %s"}}\
                        """.formatted(e.getMessage());
            }

            return McpSchema.CallToolResult.builder()
                    .content(List.of(McpSchema.TextContent.builder(json).build()))
                    .isError(false)
                    .build();
        });
    }

    /**
     * Runs the open-project flow for the given project file. See class javadoc for the
     * end-to-end contract.
     */
    public OpenProjectResult run(String projectPath) {

        // Step 1 — project file readable?
        Path projectFile;
        try {
            projectFile = Path.of(projectPath);
        } catch (RuntimeException e) {
            return validationFailed(OpenProjectErrorCode.project_not_found,
                    "Invalid path '%s': %s".formatted(projectPath, e.getMessage()),
                    new OpenProjectValidation(false, null));
        }
        if (!Files.isReadable(projectFile)) {
            return validationFailed(OpenProjectErrorCode.project_not_found,
                    "No readable file at " + projectPath,
                    new OpenProjectValidation(false, null));
        }

        // Step 2 — pick a Modeler.
        return switch (modelerResolver.resolve()) {
            case Resolved r -> launchAndAwait(r, projectFile);
            case Unresolved u -> validationFailed(u.code(), u.message(), new OpenProjectValidation(true, false));
        };
    }

    /**
     * Steps 3-4 of the open-project flow: spawn the Modeler with the handshake nonce
     * and wait for it to confirm a successful project load.
     */
    private OpenProjectResult launchAndAwait(Resolved modeler, Path projectFile) {
        OpenProjectValidation allPassed = new OpenProjectValidation(true, true);

        // Step 3 — launch.
        String nonce = UUID.randomUUID().toString().replace("-", "");
        LaunchResult launch;
        try {
            launch = ModelerLauncher.launch(
                    modeler.found().launcherKind(), modeler.found().launcher(), projectFile, nonce);
        } catch (IOException e) {
            return new OpenProjectResult(
                    "error",
                    resolved(modeler, List.of()),
                    allPassed,
                    null,
                    new OpenProjectError(OpenProjectErrorCode.launch_failed,
                            "Failed to start Modeler process: " + e.getMessage())
            );
        }

        OpenProjectResolved resolved = resolved(modeler, launch.command());

        // Step 4 — wait for the handshake.
        WatchResult watch = HandshakeWatcher.await(nonce, () -> true, HANDSHAKE_TIMEOUT, prefsLocator);

        return switch (watch.outcome()) {
            case HANDSHAKE_RECEIVED -> {
                HandshakeData data = watch.data();
                yield new OpenProjectResult(
                        "launched",
                        resolved,
                        allPassed,
                        new OpenProjectHandshake(
                                nonce,
                                data.pid(),
                                data.startedAt(),
                                data.resolvedProjectPath(),
                                watch.waitMs()),
                        null);
            }
            case SPAWNED_PROCESS_EXITED -> {
                Process p = launch.process();
                String exit;
                try {
                    exit = "exit code " + p.exitValue();
                } catch (IllegalThreadStateException e) {
                    exit = "exit code unavailable";
                }
                yield new OpenProjectResult(
                        "error",
                        resolved,
                        allPassed,
                        null,
                        new OpenProjectError(OpenProjectErrorCode.launch_exited_early,
                                "Spawned Modeler process exited before reporting handshake (%s)"
                                        .formatted(exit)));
            }
            case TIMEOUT -> new OpenProjectResult(
                    "error",
                    resolved,
                    allPassed,
                    null,
                    new OpenProjectError(OpenProjectErrorCode.launch_not_confirmed,
                            "Handshake did not appear within %ds".formatted(HANDSHAKE_TIMEOUT.toSeconds())));
        };
    }

    private static OpenProjectResolved resolved(Resolved modeler, List<String> command) {
        return new OpenProjectResolved(
                modeler.found().distribution(),
                modeler.source(),
                modeler.found().launcher().toString(),
                command);
    }

    private static OpenProjectResult validationFailed(
            OpenProjectErrorCode code,
            String message,
            OpenProjectValidation validation) {
        return new OpenProjectResult(
                "validation_failed",
                null,
                validation,
                null,
                new OpenProjectError(code, message)
        );
    }

    static Path mcpJarDir() {
        try {
            ProtectionDomain pd = OpenProjectTool.class.getProtectionDomain();
            if (pd == null) {
                return null;
            }
            CodeSource cs = pd.getCodeSource();
            if (cs == null) {
                return null;
            }
            URL url = cs.getLocation();
            if (url == null) {
                return null;
            }
            Path location = Paths.get(url.toURI());
            return Files.isRegularFile(location) && location.getParent() != null ? location.getParent() : location;
        } catch (URISyntaxException | RuntimeException e) {
            return null;
        }
    }
}
