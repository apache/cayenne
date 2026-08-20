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

import org.apache.cayenne.dba.hsqldb.HSQLDBAdapter;
import org.apache.cayenne.mcp.TestMcpClient;
import org.apache.cayenne.modeler.pref.PreferenceNodeIds;
import org.apache.cayenne.modeler.pref.PrefsLocator;
import org.apache.cayenne.modeler.pref.adapters.DataMapPrefs;
import org.apache.cayenne.modeler.pref.dbconnector.DBConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the {@code dbimport_run} tool over the MCP stdio protocol.
 * Complements {@link DbImportRunIT}, which calls the tool directly; this class exercises
 * the full JSON-RPC round-trip through the in-process MCP server.
 */
public class DbImportRunMcpIT {

    private static final String HSQL_ADAPTER = HSQLDBAdapter.class.getName();
    private static final String HSQL_DRIVER = "org.hsqldb.jdbc.JDBCDriver";

    @RegisterExtension
    TestMcpClient client = new TestMcpClient();

    @TempDir
    Path tempDir;

    private String dbUrl;
    private Connection dbConn;
    private Path projectFile;
    private Path dataMapFile;
    private Preferences dataMapPrefsNode;

    @BeforeEach
    void setUp() throws Exception {
        dbUrl = "jdbc:hsqldb:mem:dbimport_mcp_%s;shutdown=true"
                .formatted(UUID.randomUUID().toString().replace("-", ""));
        dbConn = DriverManager.getConnection(dbUrl, "SA", "");
        try (Statement s = dbConn.createStatement()) {
            s.execute("""
                    CREATE TABLE artist (
                        id   INTEGER NOT NULL PRIMARY KEY,
                        name VARCHAR(255)
                    )""");
            s.execute("""
                    CREATE TABLE painting (
                        id        INTEGER NOT NULL PRIMARY KEY,
                        title     VARCHAR(255),
                        artist_id INTEGER,
                        FOREIGN KEY (artist_id) REFERENCES artist(id)
                    )""");
        }

        projectFile = DbImportRunValidationTest.copyFixture("with-dbimport", tempDir);
        dataMapFile = tempDir.resolve("TestMap.map.xml");

        // The in-process MCP server uses new PrefsLocator() → Preferences.userRoot().
        // Write the connector there so the server can find it when the tool runs.
        String dataMapId = PreferenceNodeIds.idForPath(dataMapFile.toUri().getRawPath());
        PrefsLocator locator = new PrefsLocator(Preferences.userRoot());
        dataMapPrefsNode = locator.dataMapNode(dataMapId);
        DBConnector connector = new DBConnector();
        connector.setUrl(dbUrl);
        connector.setUserName("SA");
        connector.setPassword("");
        connector.setJdbcDriver(HSQL_DRIVER);
        connector.setDbAdapter(HSQL_ADAPTER);
        new DataMapPrefs(dataMapPrefsNode).setConnector(connector);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (dataMapPrefsNode != null) {
            try {
                dataMapPrefsNode.removeNode();
            } catch (Exception ignored) {
            }
        }
        if (dbConn != null && !dbConn.isClosed()) {
            dbConn.close();
        }
    }

    @Test
    public void toolsListIncludesDbImportRun() {
        client.send("""
                {"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}""");

        String response = client.readLine(5_000);
        assertTrue(response.contains("\"dbimport_run\""), "tools/list missing 'dbimport_run': " + response);
    }

    @Test
    public void projectNotFoundReturnsValidationError() {
        client.send("""
                {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{\
                "name":"dbimport_run","arguments":{\
                "projectPath":"/no/such/file.xml","dataMap":"TestMap"}}}""");

        assertEquals("""
                {
                  "status" : "validation_failed",
                  "summary" : {
                    "tokensConsidered" : 0,
                    "tokensApplied" : 0,
                    "entitiesAdded" : 0,
                    "entitiesRemoved" : 0,
                    "entitiesModified" : 0,
                    "relationshipsAdded" : 0
                  },
                  "resolved" : null,
                  "warnings" : [ ],
                  "validation" : {
                    "projectFound" : false,
                    "dataMapFound" : null,
                    "dbConnectorPresent" : null,
                    "jdbcDriverLoadable" : null,
                    "jdbcConnectionOpened" : null
                  },
                  "error" : {
                    "code" : "project_not_found",
                    "message" : "No readable file at /no/such/file.xml"
                  }
                }""", client.readLineAsJson(5_000));
    }

    @Test
    public void happyPathImportsEntities() {
        client.sendToolCall(4, "dbimport_run", projectFile, "TestMap");

        String dataMapPath = dataMapFile.toAbsolutePath().toString().replace("\\", "\\\\");
        String escapedDbUrl = dbUrl.replace("\\", "\\\\");

        assertEquals("""
                {
                  "status" : "imported",
                  "summary" : {
                    "tokensConsidered" : 2,
                    "tokensApplied" : 2,
                    "entitiesAdded" : 2,
                    "entitiesRemoved" : 0,
                    "entitiesModified" : 0,
                    "relationshipsAdded" : 0
                  },
                  "resolved" : {
                    "dataMapFile" : "%s",
                    "jdbcUrl" : "%s",
                    "jdbcDriver" : "%s",
                    "dbAdapter" : "%s"
                  },
                  "warnings" : [ "Can't find ObjEntity for PAINTING" ],
                  "validation" : {
                    "projectFound" : true,
                    "dataMapFound" : true,
                    "dbConnectorPresent" : true,
                    "jdbcDriverLoadable" : true,
                    "jdbcConnectionOpened" : true
                  },
                  "error" : null
                }""".formatted(dataMapPath, escapedDbUrl, HSQL_DRIVER, HSQL_ADAPTER),
                client.readLineAsJson(30_000));
    }

    @Test
    public void upToDateOnSecondRun() {
        client.sendToolCall(5, "dbimport_run", projectFile, "TestMap");
        client.readLine(30_000);

        client.sendToolCall(6, "dbimport_run", projectFile, "TestMap");

        String dataMapPath = dataMapFile.toAbsolutePath().toString().replace("\\", "\\\\");
        String escapedDbUrl = dbUrl.replace("\\", "\\\\");

        assertEquals("""
                {
                  "status" : "up_to_date",
                  "summary" : {
                    "tokensConsidered" : 0,
                    "tokensApplied" : 0,
                    "entitiesAdded" : 0,
                    "entitiesRemoved" : 0,
                    "entitiesModified" : 0,
                    "relationshipsAdded" : 0
                  },
                  "resolved" : {
                    "dataMapFile" : "%s",
                    "jdbcUrl" : "%s",
                    "jdbcDriver" : "%s",
                    "dbAdapter" : "%s"
                  },
                  "warnings" : [ ],
                  "validation" : {
                    "projectFound" : true,
                    "dataMapFound" : true,
                    "dbConnectorPresent" : true,
                    "jdbcDriverLoadable" : true,
                    "jdbcConnectionOpened" : true
                  },
                  "error" : null
                }""".formatted(dataMapPath, escapedDbUrl, HSQL_DRIVER, HSQL_ADAPTER),
                client.readLineAsJson(30_000));
    }
}
