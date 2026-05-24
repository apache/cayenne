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
package org.apache.cayenne.mcp.tools.dbimport;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.datasource.DriverDataSource;
import org.apache.cayenne.dbsync.DbSyncModule;
import org.apache.cayenne.dbsync.reverse.configuration.ToolsModule;
import org.apache.cayenne.dbsync.reverse.dbimport.DbImportConfiguration;
import org.apache.cayenne.dbsync.reverse.dbimport.DbImportModule;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.pref.PreferenceNodeIds;
import org.apache.cayenne.modeler.pref.PrefsLocator;
import org.apache.cayenne.modeler.pref.adapters.ClasspathPrefs;
import org.apache.cayenne.modeler.pref.adapters.DataMapPrefs;
import org.apache.cayenne.modeler.pref.dbconnector.DBConnector;
import org.apache.cayenne.mcp.log.McpLoggingHandler;
import org.apache.cayenne.mcp.project.McpProjectLoaderModule;
import org.apache.cayenne.mcp.tools.dbimport.protocol.DbImportError;
import org.apache.cayenne.mcp.tools.dbimport.protocol.DbImportErrorCode;
import org.apache.cayenne.mcp.tools.dbimport.protocol.DbImportResolved;
import org.apache.cayenne.mcp.tools.dbimport.protocol.DbImportRunResult;
import org.apache.cayenne.mcp.tools.dbimport.protocol.DbImportSummary;
import org.apache.cayenne.mcp.tools.dbimport.protocol.DbImportValidation;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectLoader;
import org.apache.cayenne.project.ProjectModule;
import org.apache.cayenne.resource.URLResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * MCP tool that runs Cayenne dbimport for a single DataMap inside a Cayenne project.
 * Reads reverse-engineering filter config from the DataMap's {@code <reverse-engineering>}
 * block and JDBC connection info from the DBConnector stored in CayenneModeler preferences
 * for this DataMap. Returns a structured JSON summary of what changed.
 *
 * @since 5.0
 */
public class DbImportRunTool {

    public static final String NAME = "dbimport_run";

    private static final Logger LOGGER = LoggerFactory.getLogger(DbImportRunTool.class);

    private final PrefsLocator prefsLocator;
    private final Injector injector;

    // Memoized driver classloader — reused while the classpath entries list is unchanged
    private volatile CachedClassLoader cachedClassLoader;

    public DbImportRunTool(PrefsLocator prefsLocator) {
        this.prefsLocator = prefsLocator;
        this.injector = DIBootstrap.createInjector(
                new DbSyncModule(),
                new ToolsModule(LOGGER),
                new DbImportModule(),
                new ProjectModule(),
                new McpProjectLoaderModule(),
                binder -> binder.bind(InstrumentedDbImportAction.class).to(InstrumentedDbImportAction.class)
        );
    }

    public static McpServerFeatures.SyncToolSpecification spec(McpJsonMapper jsonMapper,
                                                               PrefsLocator prefsLocator) {
        DbImportRunTool tool = new DbImportRunTool(prefsLocator);

        McpSchema.Tool descriptor = new McpSchema.Tool(
                NAME,
                null,
                """
                        Run Cayenne dbimport for a named DataMap. Reads reverse-engineering filters \
                        from the DataMap's <reverse-engineering> block and JDBC connection from the \
                        DBConnector that CayenneModeler stored for this DataMap. Returns token-count \
                        summary and resolved connection metadata; actual schema changes are written \
                        to the DataMap XML on disk.""",
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

            DbImportRunResult result = tool.run(projectPath, dataMapName);

            String json;
            try {
                json = jsonMapper.writeValueAsString(result);
            } catch (IOException e) {
                json = """
                        {"status":"error","error":{"code":"dbimport_runtime_error",\
                        "message":"Serialization failed: %s"}}""".formatted(e.getMessage());
            }

            return McpSchema.CallToolResult.builder()
                    .content(List.of(new McpSchema.TextContent(json)))
                    .isError(false)
                    .build();
        });
    }

    public DbImportRunResult run(String projectPath, String dataMapName) {

        // Step 1 — project file readable?
        Path projectFile = Path.of(projectPath);
        if (!Files.isReadable(projectFile)) {
            return validationFailed(DbImportErrorCode.project_not_found,
                    "No readable file at %s".formatted(projectPath),
                    new DbImportValidation(false, null, null, null, null, null));
        }

        // Step 2 — parses as Cayenne project?
        Project project;
        try {
            // TODO: loading the project here, and then again within InstrumentedDbImportAction
            project = injector
                    .getInstance(ProjectLoader.class)
                    .loadProject(new URLResource(projectFile.toUri().toURL()));
        } catch (Exception e) {
            return validationFailed(DbImportErrorCode.project_parse_failed,
                    "Cayenne project loader rejected the descriptor: %s".formatted(e.getMessage()),
                    new DbImportValidation(true, null, null, null, null, null));
        }

        // Step 3 — DataMap present?
        DataChannelDescriptor descriptor = (DataChannelDescriptor) project.getRootNode();
        DataMap dataMap = descriptor.getDataMap(dataMapName);
        if (dataMap == null) {
            String available = descriptor.getDataMaps().stream()
                    .map(DataMap::getName)
                    .sorted()
                    .collect(Collectors.joining("', '", "'", "'"));
            return validationFailed(DbImportErrorCode.datamap_not_found,
                    "Project loaded successfully but contains no DataMap named '%s'. Available DataMaps: %s."
                            .formatted(dataMapName, available),
                    new DbImportValidation(true, false, null, null, null, null));
        }

        // Step 4 — <reverse-engineering> config present?
        DataChannelMetaData metaData = injector.getInstance(DataChannelMetaData.class);
        ReverseEngineering reverseEngineering = metaData.get(dataMap, ReverseEngineering.class);
        if (reverseEngineering == null) {
            return validationFailed(DbImportErrorCode.reverse_engineering_config_missing,
                    "DataMap '%s' has no <reverse-engineering> configuration. Configure dbimport for this DataMap in CayenneModeler before invoking the tool."
                            .formatted(dataMapName),
                    new DbImportValidation(true, true, false, null, null, null));
        }

        // Resolve DataMap file path for preference node lookup
        Path dataMapFile = resolveDataMapFile(dataMap);

        // Step 5 — DBConnector stored in preferences?
        String dataMapId = PreferenceNodeIds.idForPath(dataMapFile.toAbsolutePath().toString());
        DBConnector connector = new DataMapPrefs(prefsLocator.dataMapNode(dataMapId)).getConnector();
        if (connector == null) {
            return validationFailed(DbImportErrorCode.dbconnector_not_configured,
                    """
                            No DBConnector is stored in preferences for DataMap '%s' (project '%s'). \
                            In CayenneModeler, open this project, right-click the DataMap, \
                            choose 'Reengineer Database Schema…', and pick a connection — \
                            its values will be saved to this DataMap's preferences.\
                            """.formatted(dataMapName, projectPath),
                    new DbImportValidation(true, true, true, false, null, null));
        }

        DbImportResolved resolved = new DbImportResolved(
                dataMapFile.toAbsolutePath().toString(),
                connector.getUrl(),
                connector.getJdbcDriver(),
                connector.getDbAdapter()
        );

        // Step 6 — JDBC driver loadable from Preferences → Classpath?
        ClassLoader driverCl = buildDriverClassLoader();
        Driver driver;
        try {
            driver = (Driver) driverCl.loadClass(connector.getJdbcDriver()).getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            return validationFailed(DbImportErrorCode.jdbc_driver_not_loadable,
                    "JDBC driver class '%s' could not be loaded. In CayenneModeler, open Preferences → Classpath, add the driver jar, then re-run — the MCP server reads the classpath fresh on each call."
                            .formatted(connector.getJdbcDriver()),
                    new DbImportValidation(true, true, true, true, false, null),
                    resolved);
        } catch (Exception e) {
            return validationFailed(DbImportErrorCode.jdbc_driver_not_loadable,
                    "JDBC driver class '%s' could not be instantiated: %s"
                            .formatted(connector.getJdbcDriver(), e.getMessage()),
                    new DbImportValidation(true, true, true, true, false, null),
                    resolved);
        }

        // Step 7 — JDBC connection opens?
        Connection conn;
        try {
            conn = new DriverDataSource(
                    driver,
                    connector.getUrl(),
                    connector.getUserName(),
                    connector.getPassword()).getConnection();

        } catch (SQLException e) {
            String sqlState = e.getSQLState() != null ? " [SQLState: %s]".formatted(e.getSQLState()) : "";
            return validationFailed(DbImportErrorCode.jdbc_connection_failed,
                    "Could not open JDBC connection to '%s' as '%s': %s%s."
                            .formatted(connector.getUrl(), connector.getUserName(), e.getMessage(), sqlState),
                    new DbImportValidation(true, true, true, true, true, false),
                    resolved);
        }

        // All validation passed — run dbimport
        Supplier<List<String>> stopCapture = McpLoggingHandler.startCapture("org.apache.cayenne.dbsync");
        List<String> warnings;
        try {
            InstrumentedDbImportAction action = injector.getInstance(InstrumentedDbImportAction.class);
            DbImportConfiguration config = buildConfig(connector, dataMapFile, projectFile);
            action.execute(config);

            int considered = action.getCapturedTokens().size();
            DbImportSummary summary = action.buildSummary(considered);
            String status = considered == 0 ? "up_to_date" : "imported";
            warnings = stopCapture.get();
            return new DbImportRunResult(status, summary, resolved, warnings, DbImportValidation.ALL_PASSED, null);
        } catch (Exception e) {
            warnings = stopCapture.get();
            InstrumentedDbImportAction action = injector.getInstance(InstrumentedDbImportAction.class);
            DbImportSummary partial = action.buildSummary(0);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            return new DbImportRunResult("error", partial, resolved, warnings, DbImportValidation.ALL_PASSED,
                    new DbImportError(DbImportErrorCode.dbimport_runtime_error, msg));
        } finally {
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private DbImportConfiguration buildConfig(DBConnector connector, Path dataMapFile, Path projectFile) {
        DbImportConfiguration config = new DbImportConfiguration();
        config.setDriver(connector.getJdbcDriver());
        config.setUrl(connector.getUrl());
        config.setUsername(connector.getUserName());
        config.setPassword(connector.getPassword());
        config.setAdapter(connector.getDbAdapter());
        config.setTargetDataMap(dataMapFile.toFile());
        config.setCayenneProject(projectFile.toFile());
        config.setUseDataMapReverseEngineering(true);
        return config;
    }

    private Path resolveDataMapFile(DataMap dataMap) {
        try {
            URL sourceUrl = dataMap.getConfigurationSource().getURL();
            return Path.of(sourceUrl.toURI());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot resolve DataMap file path for '%s'".formatted(dataMap.getName()), e);
        }
    }

    private ClassLoader buildDriverClassLoader() {
        List<String> entries = new ClasspathPrefs(prefsLocator.appNode(ClasspathPrefs.NODE)).getEntries();
        CachedClassLoader cached = this.cachedClassLoader;
        if (cached != null && cached.entries.equals(entries)) {
            return cached.loader;
        }
        URL[] urls = entries.stream()
                .map(e -> {
                    try {
                        return Path.of(e).toUri().toURL();
                    } catch (Exception ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toArray(URL[]::new);
        ClassLoader loader = new URLClassLoader(urls, getClass().getClassLoader());
        this.cachedClassLoader = new CachedClassLoader(entries, loader);
        return loader;
    }

    private static DbImportRunResult validationFailed(DbImportErrorCode code, String message,
                                                      DbImportValidation validation) {
        return validationFailed(code, message, validation, null);
    }

    private static DbImportRunResult validationFailed(DbImportErrorCode code, String message,
                                                      DbImportValidation validation,
                                                      DbImportResolved resolved) {
        return new DbImportRunResult(
                "validation_failed",
                DbImportSummary.ZERO,
                resolved,
                List.of(),
                validation,
                new DbImportError(code, message)
        );
    }

    private record CachedClassLoader(List<String> entries, ClassLoader loader) {
    }
}
