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
import org.apache.cayenne.mcp.tools.openproject.ModelerDiscovery.DiscoveryResult;
import org.apache.cayenne.mcp.tools.openproject.ModelerDiscovery.Found;
import org.apache.cayenne.mcp.tools.openproject.ModelerDiscovery.NotFound;
import org.apache.cayenne.mcp.tools.openproject.ModelerLauncher.LaunchResult;
import org.apache.cayenne.mcp.tools.openproject.HandshakeWatcher.HandshakeData;
import org.apache.cayenne.mcp.tools.openproject.HandshakeWatcher.WatchResult;
import org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectError;
import org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectErrorCode;
import org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectHandshake;
import org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectResolved;
import org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectResult;
import org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectValidation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * MCP tool that launches CayenneModeler with a project file pre-loaded. The tool
 * locates the Modeler installation alongside the running MCP jar, spawns it with
 * {@code --mcp-handshake <nonce>}, and waits for the Modeler to confirm
 * successful project load via a {@link java.util.prefs.Preferences} handshake.
 *
 * @since 5.0
 */
public class OpenProjectTool {

    public static final String NAME = "open_project";

    static final Duration HANDSHAKE_TIMEOUT = Duration.ofSeconds(15);

    public static McpServerFeatures.SyncToolSpecification spec(McpJsonMapper jsonMapper) {
        OpenProjectTool tool = new OpenProjectTool();

        McpSchema.Tool descriptor = new McpSchema.Tool(
                NAME,
                null,
                """
                        Launch CayenneModeler with the given project file. Non-blocking; waits for \
                        the Modeler to report a startup handshake before returning.""",
                new McpSchema.JsonSchema(
                        "object",
                        Map.of(
                                "projectPath", Map.of(
                                        "type", "string",
                                        "description", "Absolute path to the top-level Cayenne project descriptor (cayenne-*.xml)")
                        ),
                        List.of("projectPath"),
                        null, null, null
                ),
                null, null, null
        );

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
                    .content(List.of(new McpSchema.TextContent(json)))
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
                    new OpenProjectValidation(false, null, null));
        }
        if (!Files.isReadable(projectFile)) {
            return validationFailed(OpenProjectErrorCode.project_not_found,
                    "No readable file at " + projectPath,
                    new OpenProjectValidation(false, null, null));
        }

        // Step 2 — locate the MCP jar's directory.
        Optional<Path> mcpDir = McpJarLocator.locate(OpenProjectTool.class);
        if (mcpDir.isEmpty()) {
            return validationFailed(OpenProjectErrorCode.mcp_jar_location_unresolved,
                    "Could not resolve the running MCP server jar's location",
                    new OpenProjectValidation(true, false, null));
        }

        // Step 3 — discover a Modeler installation.
        OsKind osKind = OsKind.detect();
        DiscoveryResult discovery = ModelerDiscovery.discover(mcpDir.get(), osKind);
        if (discovery instanceof NotFound nf) {
            String notes = String.join("; ", nf.probeNotes());
            return validationFailed(OpenProjectErrorCode.modeler_not_found,
                    "No CayenneModeler installation found relative to MCP jar at %s. Probes: %s"
                            .formatted(mcpDir.get(), notes),
                    new OpenProjectValidation(true, true, false));
        }
        Found found = (Found) discovery;
        OpenProjectValidation allPassed = new OpenProjectValidation(true, true, true);

        // Step 4 — launch.
        String nonce = UUID.randomUUID().toString().replace("-", "");
        LaunchResult launch;
        try {
            launch = ModelerLauncher.launch(found.launcherKind(), found.launcher(), projectFile, nonce);
        } catch (IOException e) {
            return new OpenProjectResult(
                    "error",
                    new OpenProjectResolved(found.distribution(), found.launcher().toString(), List.of()),
                    allPassed,
                    null,
                    new OpenProjectError(OpenProjectErrorCode.launch_failed,
                            "Failed to start Modeler process: " + e.getMessage())
            );
        }

        OpenProjectResolved resolved = new OpenProjectResolved(
                found.distribution(),
                found.launcher().toString(),
                launch.command());

        // Step 5 — wait for the handshake.
        BooleanSupplier alive = launch.processAlivenessMeaningful()
                ? () -> launch.process().isAlive()
                : () -> true;
        WatchResult watch = HandshakeWatcher.await(nonce, alive, HANDSHAKE_TIMEOUT);

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
            case TIMEOUT -> {
                boolean stillAlive = launch.processAlivenessMeaningful() && launch.process().isAlive();
                String hint = stillAlive
                        ? "Modeler process is still running but did not confirm opening the project — check the Modeler window for an error dialog"
                        : "Modeler process is not alive at the timeout boundary";
                yield new OpenProjectResult(
                        "error",
                        resolved,
                        allPassed,
                        null,
                        new OpenProjectError(OpenProjectErrorCode.launch_not_confirmed,
                                "Handshake did not appear within %ds. %s.".formatted(
                                        HANDSHAKE_TIMEOUT.toSeconds(), hint)));
            }
        };
    }

    private static OpenProjectResult validationFailed(OpenProjectErrorCode code, String message,
                                                      OpenProjectValidation validation) {
        return new OpenProjectResult(
                "validation_failed",
                null,
                validation,
                null,
                new OpenProjectError(code, message)
        );
    }
}
