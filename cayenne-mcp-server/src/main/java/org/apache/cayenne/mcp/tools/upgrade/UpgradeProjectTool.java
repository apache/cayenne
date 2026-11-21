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

package org.apache.cayenne.mcp.tools.upgrade;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.mcp.project.McpProjectLoaderModule;
import org.apache.cayenne.mcp.tools.upgrade.protocol.UpgradeError;
import org.apache.cayenne.mcp.tools.upgrade.protocol.UpgradeErrorCode;
import org.apache.cayenne.mcp.tools.upgrade.protocol.UpgradeProjectResult;
import org.apache.cayenne.project.upgrade.UpgradeHandler;
import org.apache.cayenne.projecttools.ProjectToolsModule;
import org.apache.cayenne.projecttools.upgrade.PostUpgradeState;
import org.apache.cayenne.projecttools.upgrade.PreUpgradeState;
import org.apache.cayenne.projecttools.upgrade.ProjectUpgrader;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.tools.ToolsInjectorBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * MCP tool that upgrades a Cayenne project saved by an older version of Cayenne to the current format, rewriting the
 * project XML and every DataMap XML on disk, the same as CayenneModeler does when it opens such project. Returns the
 * changes made by the upgrade, including those that need code changes from the user to keep the application working.
 *
 * @since 6.0
 */
public class UpgradeProjectTool {

    public static final String NAME = "upgrade_project";

    private final Injector injector;

    public UpgradeProjectTool() {
        this.injector = new ToolsInjectorBuilder()
                .addModule(new ProjectToolsModule())
                .addModule(new McpProjectLoaderModule())
                .create();
    }

    public static McpServerFeatures.SyncToolSpecification spec(McpJsonMapper jsonMapper) {
        UpgradeProjectTool tool = new UpgradeProjectTool();

        McpSchema.Tool descriptor = McpSchema.Tool.builder(
                        NAME,
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "projectPath", Map.of(
                                                "type", "string",
                                                "description", "Absolute path to the top-level Cayenne project descriptor (cayenne-*.xml), not a DataMap file")
                                ),
                                "required", List.of("projectPath")
                        ))
                .description("""
                        Upgrade a Cayenne project saved by an older version of Cayenne to the current format, \
                        rewriting the project and DataMap XML files in place and deleting the files the project no \
                        longer needs. Returns the list of changes made. The changes with "affectsRuntime": true \
                        removed or altered a part of the project that has no automatic replacement, and require \
                        code changes to keep the application working; their "details" hold the information that the \
                        upgraded project no longer has.""")
                .build();

        return new McpServerFeatures.SyncToolSpecification(descriptor, (exchange, request) -> {
            Map<String, Object> args = request.arguments();
            String projectPath = args != null ? (String) args.getOrDefault("projectPath", "") : "";

            UpgradeProjectResult result = tool.run(projectPath);

            String json;
            try {
                json = jsonMapper.writeValueAsString(result);
            } catch (IOException e) {
                json = """
                        {"status":"error","error":{"code":"upgrade_failed",\
                        "message":"Serialization failed: %s"}}""".formatted(e.getMessage());
            }

            return McpSchema.CallToolResult.builder()
                    .content(List.of(McpSchema.TextContent.builder(json).build()))
                    .isError(false)
                    .build();
        });
    }

    /**
     * Upgrades the project at the given path.
     */
    public UpgradeProjectResult run(String projectPath) {

        Path projectFile = Path.of(projectPath);
        if (!Files.isReadable(projectFile)) {
            return error(null, UpgradeErrorCode.project_not_found, "No readable file at " + projectPath);
        }

        ProjectUpgrader upgrader = injector.getInstance(ProjectUpgrader.class);
        Resource resource;
        PreUpgradeState preUpgrade;
        try {
            resource = new URLResource(projectFile.toUri().toURL());
            preUpgrade = upgrader.checkUpgradeNeeded(resource);
        } catch (Exception e) {
            return error(null, UpgradeErrorCode.project_parse_failed,
                    "Can't read the project version: " + message(e));
        }

        String fromVersion = preUpgrade.projectVersion();
        switch (preUpgrade.requiredUpgrade()) {
            case UPGRADE_NOT_NEEDED:
                return new UpgradeProjectResult("up_to_date", fromVersion, preUpgrade.supportedVersion(), List.of(),
                        null);

            case DOWNGRADE_NEEDED:
                return error(fromVersion, UpgradeErrorCode.downgrade_needed, """
                        The project version %s is newer than the version %s supported by this Cayenne release. \
                        Use a newer Cayenne release.""".formatted(fromVersion, preUpgrade.supportedVersion()));

            case INTERMEDIATE_UPGRADE_NEEDED:
                String release = preUpgrade.intermediateUpgradeRelease();
                return error(fromVersion, UpgradeErrorCode.intermediate_upgrade_needed, """
                        The project version %s is too old to be upgraded directly. Open the project in CayenneModeler \
                        %s to upgrade it to at least the version %s first.""".formatted(
                        fromVersion,
                        release != null ? "of Cayenne " + release : "of an older Cayenne release",
                        preUpgrade.intermediateUpgradeVersion()));

            default:
                break;
        }

        PostUpgradeState postUpgrade;
        try {
            postUpgrade = upgrader.upgrade(resource);
        } catch (Exception e) {
            return error(fromVersion, UpgradeErrorCode.upgrade_failed, message(e));
        }

        return new UpgradeProjectResult(
                "upgraded",
                fromVersion,
                UpgradeHandler.CURRENT_VERSION,
                postUpgrade.changes(),
                null);
    }

    private static UpgradeProjectResult error(String fromVersion, UpgradeErrorCode code, String message) {
        return new UpgradeProjectResult("error", fromVersion, UpgradeHandler.CURRENT_VERSION, List.of(),
                new UpgradeError(code, message));
    }

    private static String message(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getName();
    }
}
