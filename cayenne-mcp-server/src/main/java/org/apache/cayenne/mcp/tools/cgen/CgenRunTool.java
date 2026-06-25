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

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.gen.CgenConfigList;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.MetadataUtils;
import org.apache.cayenne.gen.ToolsUtilsFactory;
import org.apache.cayenne.gen.internal.Utils;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.mcp.project.McpProjectLoaderModule;
import org.apache.cayenne.mcp.log.McpLoggingHandler;
import org.apache.cayenne.mcp.tools.cgen.protocol.CgenError;
import org.apache.cayenne.mcp.tools.cgen.protocol.CgenErrorCode;
import org.apache.cayenne.mcp.tools.cgen.protocol.CgenFileEntry;
import org.apache.cayenne.mcp.tools.cgen.protocol.CgenResolvedConfig;
import org.apache.cayenne.mcp.tools.cgen.protocol.CgenRunResult;
import org.apache.cayenne.mcp.tools.cgen.protocol.CgenSummary;
import org.apache.cayenne.mcp.tools.cgen.protocol.CgenValidation;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectLoader;
import org.apache.cayenne.project.ProjectModule;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.tools.ToolsInjectorBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP tool that runs Cayenne class generation (cgen) for a single DataMap inside
 * a Cayenne project, using the cgen configuration stored in the DataMap XML.
 * Returns a structured JSON delta describing what changed on disk.
 *
 * @since 5.0
 */
public class CgenRunTool {

    public static final String NAME = "cgen_run";

    private final Injector injector;

    public CgenRunTool() {
        this.injector = new ToolsInjectorBuilder()
                .addModule(new ProjectModule())
                .addModule(new McpProjectLoaderModule())
                .create();
    }

    public static McpServerFeatures.SyncToolSpecification spec(McpJsonMapper jsonMapper) {
        CgenRunTool tool = new CgenRunTool();

        McpSchema.Tool descriptor = new McpSchema.Tool(
                NAME,
                null,
                """
                        Run Cayenne class generator (cgen) for a named DataMap. Returns a JSON report of what was \
                        written, what was already up-to-date, and any errors.""",
                new McpSchema.JsonSchema(
                        "object",
                        Map.of(
                                "projectPath", Map.of(
                                        "type", "string",
                                        "description", "Absolute path to the top-level Cayenne project descriptor (cayenne-*.xml), not a DataMap file"),
                                "dataMap", Map.of(
                                        "type", "string",
                                        "description", "Name of the target DataMap as it appears in the <map name='...'> element of the project descriptor")
                        ),
                        List.of("projectPath", "dataMap"),
                        null, null, null
                ),
                null, null, null
        );

        return new McpServerFeatures.SyncToolSpecification(descriptor, (exchange, request) -> {
            Map<String, Object> args = request.arguments();
            String projectPath = args != null ? (String) args.getOrDefault("projectPath", "") : "";
            String dataMapName = args != null ? (String) args.getOrDefault("dataMap", "") : "";

            CgenRunResult result = tool.run(projectPath, dataMapName);

            String json;
            try {
                json = jsonMapper.writeValueAsString(result);
            } catch (IOException e) {
                json = """
                        {"status":"error","error":{"code":"cgen_runtime_error",\
                        "message":"Serialization failed: %s"}}""".formatted(e.getMessage());
            }

            return McpSchema.CallToolResult.builder()
                    .content(List.of(new McpSchema.TextContent(json)))
                    .isError(false)
                    .build();
        });
    }

    /**
     * Runs cgen for the named DataMap inside the given project file and returns the result envelope.
     */
    public CgenRunResult run(String projectPath, String dataMapName) {

        // Step 1 — project file readable?
        Path projectFile = Path.of(projectPath);
        if (!Files.isReadable(projectFile)) {
            return validationFailed(CgenErrorCode.project_not_found,
                    "No readable file at " + projectPath,
                    new CgenValidation(false, null, null, null, null));
        }

        // Step 2 — parses as Cayenne project?
        Project project;
        try {
            ProjectLoader loader = injector.getInstance(ProjectLoader.class);
            project = loader.loadProject(new URLResource(projectFile.toUri().toURL()));
        } catch (Exception e) {
            return validationFailed(CgenErrorCode.project_parse_failed,
                    "Cayenne project loader rejected the descriptor: " + e.getMessage(),
                    new CgenValidation(true, null, null, null, null));
        }

        // Step 3 — DataMap present?
        DataChannelDescriptor descriptor = (DataChannelDescriptor) project.getRootNode();
        DataMap dataMap = descriptor.getDataMap(dataMapName);
        if (dataMap == null) {
            String available = descriptor.getDataMaps().stream()
                    .map(DataMap::getName)
                    .sorted()
                    .collect(Collectors.joining("', '", "'", "'"));
            return validationFailed(CgenErrorCode.datamap_not_found,
                    "Project loaded successfully but contains no DataMap named '" + dataMapName
                            + "'. Available DataMaps: " + available + ".",
                    new CgenValidation(true, false, null, null, null));
        }

        // Step 4 — cgen configuration: use the <cgen> block stored in the DataMap, or, if there is none,
        // synthesize a default. This mirrors CayenneModeler and the Maven/Gradle plugins, all of which
        // fall back to a default config rather than failing — a missing <cgen> block is not an error.
        DataChannelMetaData metaData = injector.getInstance(DataChannelMetaData.class);
        CgenConfigList configList = metaData.get(dataMap, CgenConfigList.class);
        boolean usedDefaultConfig = configList == null || configList.getAll().isEmpty();
        CgenConfiguration cgenConfig = usedDefaultConfig
                ? defaultConfig(dataMap)
                : configList.getAll().getFirst();

        // Set the DataMap file's mtime as the timestamp so fileNeedUpdate() can detect DataMap changes correctly.
        if (dataMap.getConfigurationSource() != null) {
            try {
                Path dataMapFile = Path.of(dataMap.getConfigurationSource().getURL().toURI());
                cgenConfig.setTimestamp(Files.getLastModifiedTime(dataMapFile).toMillis());
            } catch (Exception e) {
                // URI conversion failed (e.g. non-file: URL) or some problems with file mtime read,
                // better to regen all, than silently fail.
                cgenConfig.setForce(true);
            }
        }

        // Step 5 — destDir specified?
        Path destDir = cgenConfig.buildOutputPath();
        if (destDir == null) {
            return validationFailed(CgenErrorCode.destdir_not_specified,
                    "cgen configuration for '" + dataMapName + "' does not specify a destination directory.",
                    new CgenValidation(true, true, true, false, null));
        }

        // Step 6 — destDir writable (create if absent)?
        if (!ensureWritable(destDir)) {
            return validationFailed(CgenErrorCode.destdir_not_writable,
                    "Destination directory '" + destDir + "' exists but is not writable by the MCP server process.",
                    new CgenValidation(true, true, true, true, false));
        }

        CgenResolvedConfig resolvedConfig = new CgenResolvedConfig(destDir.toAbsolutePath().toString());
        CgenValidation allPassed = new CgenValidation(true, true, true, true, true);

        // Execute cgen
        InstrumentedClassGenerationAction action = new InstrumentedClassGenerationAction(cgenConfig);
        action.setUtilsFactory(injector.getInstance(ToolsUtilsFactory.class));
        action.setMetadataUtils(injector.getInstance(MetadataUtils.class));

        action.prepareArtifacts();
        int filesConsidered = action.countFilesConsidered();

        var stopCapture = McpLoggingHandler.startCapture("org.apache.cayenne.gen");
        List<String> warnings;
        Exception caughtException = null;
        try {
            action.execute();
        } catch (Exception e) {
            caughtException = e;
        } finally {
            warnings = stopCapture.get();
        }

        if (caughtException != null) {
            List<CgenFileEntry> partial = action.getWrittenFiles();
            String msg = caughtException.getMessage() != null
                    ? caughtException.getMessage() : caughtException.getClass().getName();
            return new CgenRunResult(
                    "error",
                    new CgenSummary(filesConsidered, partial.size()),
                    partial,
                    resolvedConfig,
                    warnings,
                    allPassed,
                    new CgenError(CgenErrorCode.cgen_runtime_error, msg)
            );
        }

        List<CgenFileEntry> writtenFiles = action.getWrittenFiles();
        String status = writtenFiles.isEmpty() ? "up_to_date" : "generated";

        return new CgenRunResult(
                status,
                new CgenSummary(filesConsidered, writtenFiles.size()),
                writtenFiles,
                resolvedConfig,
                warnings,
                allPassed,
                null
        );
    }

    /**
     * Builds a default cgen configuration for a DataMap that has no embedded {@code <cgen>} block.
     * Mirrors {@code CgenPanel.createDefaultCgenConfiguration} in CayenneModeler: generate every
     * entity and embeddable, with the destination derived from the standard Maven source layout
     * ({@code src/main/resources} → {@code src/main/java}, likewise for {@code test}). For projects
     * that don't follow the Maven layout the destination falls back to the DataMap's own directory.
     */
    private static CgenConfiguration defaultConfig(DataMap dataMap) {
        Path mapDir = Utils.getRootPathForDataMap(dataMap);
        Path outputPath = Utils.getMavenSrcPathForPath(mapDir).map(Path::of).orElse(mapDir);

        CgenConfiguration config = new CgenConfiguration();
        config.setDataMap(dataMap);
        dataMap.getObjEntities().forEach(config::loadEntity);
        dataMap.getEmbeddables().forEach(config::loadEmbeddable);
        config.setRootPath(mapDir);
        config.updateOutputPath(outputPath);
        return config;
    }

    private static CgenRunResult validationFailed(CgenErrorCode code, String message, CgenValidation validation) {
        return new CgenRunResult(
                "validation_failed",
                new CgenSummary(0, 0),
                List.of(),
                null,
                List.of(),
                validation,
                new CgenError(code, message)
        );
    }

    private static boolean ensureWritable(Path dir) {
        if (Files.exists(dir)) {
            return Files.isWritable(dir);
        }
        try {
            Files.createDirectories(dir);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
