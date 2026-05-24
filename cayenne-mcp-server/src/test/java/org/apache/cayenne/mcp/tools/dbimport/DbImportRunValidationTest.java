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

import org.apache.cayenne.modeler.pref.PreferenceNodeIds;
import org.apache.cayenne.modeler.pref.PrefsLocator;
import org.apache.cayenne.modeler.pref.adapters.DataMapPrefs;
import org.apache.cayenne.modeler.pref.dbconnector.DBConnector;
import org.apache.cayenne.mcp.tools.dbimport.protocol.DbImportErrorCode;
import org.apache.cayenne.mcp.tools.dbimport.protocol.DbImportRunResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

public class DbImportRunValidationTest {

    private static DbImportRunTool tool;
    private static PrefsLocator prefsLocator;

    private Preferences testPrefsRoot;

    @BeforeAll
    public static void setUpClass() {
        prefsLocator = new PrefsLocator(Preferences.userRoot().node("cayenne-test/dbimport-validation"));
        tool = new DbImportRunTool(prefsLocator);
    }

    @AfterEach
    public void cleanupPrefs() throws BackingStoreException {
        if (testPrefsRoot != null) {
            testPrefsRoot.removeNode();
            testPrefsRoot = null;
        }
    }

    @Test
    public void projectNotFound() {
        DbImportRunResult result = tool.run("/no/such/file/cayenne-project.xml", "TestMap");

        assertEquals("validation_failed", result.status());
        assertEquals(DbImportErrorCode.project_not_found, result.error().code());
        assertFalse(result.validation().projectFound());
        assertNull(result.validation().dataMapFound());
        assertNull(result.validation().reverseEngineeringConfigPresent());
        assertNull(result.validation().dbConnectorPresent());
        assertNull(result.validation().jdbcDriverLoadable());
        assertNull(result.validation().jdbcConnectionOpened());
        assertNull(result.resolved());
        assertNoCredentialsInResult(result);
    }

    @Test
    public void projectParseFailed(@TempDir Path tmp) throws IOException {
        Path badXml = tmp.resolve("cayenne-project.xml");
        Files.writeString(badXml, "this is not xml <<< garbage");

        DbImportRunResult result = tool.run(badXml.toString(), "TestMap");

        assertEquals("validation_failed", result.status());
        assertEquals(DbImportErrorCode.project_parse_failed, result.error().code());
        assertTrue(result.validation().projectFound());
        assertNull(result.validation().dataMapFound());
        assertNull(result.validation().reverseEngineeringConfigPresent());
        assertNull(result.validation().dbConnectorPresent());
        assertNull(result.validation().jdbcDriverLoadable());
        assertNull(result.validation().jdbcConnectionOpened());
        assertNoCredentialsInResult(result);
    }

    @Test
    public void dataMapNotFound() throws URISyntaxException {
        String projectPath = fixtureProject("no-dbimport");

        DbImportRunResult result = tool.run(projectPath, "NoSuchMap");

        assertEquals("validation_failed", result.status());
        assertEquals(DbImportErrorCode.datamap_not_found, result.error().code());
        assertTrue(result.error().message().contains("NoSuchMap"));
        assertTrue(result.error().message().contains("TestMap"), "Available maps should be listed");
        assertTrue(result.validation().projectFound());
        assertFalse(result.validation().dataMapFound());
        assertNull(result.validation().reverseEngineeringConfigPresent());
        assertNull(result.validation().dbConnectorPresent());
        assertNull(result.validation().jdbcDriverLoadable());
        assertNull(result.validation().jdbcConnectionOpened());
        assertNoCredentialsInResult(result);
    }

    @Test
    public void reverseEngineeringConfigMissing() throws URISyntaxException {
        String projectPath = fixtureProject("no-dbimport");

        DbImportRunResult result = tool.run(projectPath, "TestMap");

        assertEquals("validation_failed", result.status());
        assertEquals(DbImportErrorCode.reverse_engineering_config_missing, result.error().code());
        assertTrue(result.validation().projectFound());
        assertTrue(result.validation().dataMapFound());
        assertFalse(result.validation().reverseEngineeringConfigPresent());
        assertNull(result.validation().dbConnectorPresent());
        assertNull(result.validation().jdbcDriverLoadable());
        assertNull(result.validation().jdbcConnectionOpened());
        assertNoCredentialsInResult(result);
    }

    @Test
    public void dbConnectorNotConfigured(@TempDir Path tmp) throws IOException {
        Path projectFile = copyFixture("with-dbimport", tmp);
        Preferences isolatedRoot = isolatedPrefsRoot();
        // No connector stored — DataMapPrefs node is empty

        DbImportRunTool isolatedTool = new DbImportRunTool(new PrefsLocator(isolatedRoot));
        DbImportRunResult result = isolatedTool.run(projectFile.toString(), "TestMap");

        assertEquals("validation_failed", result.status());
        assertEquals(DbImportErrorCode.dbconnector_not_configured, result.error().code());
        assertTrue(result.validation().projectFound());
        assertTrue(result.validation().dataMapFound());
        assertTrue(result.validation().reverseEngineeringConfigPresent());
        assertFalse(result.validation().dbConnectorPresent());
        assertNull(result.validation().jdbcDriverLoadable());
        assertNull(result.validation().jdbcConnectionOpened());
        assertNull(result.resolved());
        assertNoCredentialsInResult(result);
    }

    @Test
    public void jdbcDriverNotLoadable(@TempDir Path tmp) throws IOException {
        Path projectFile = copyFixture("with-dbimport", tmp);
        Path dataMapFile = tmp.resolve("TestMap.map.xml");

        Preferences isolatedRoot = isolatedPrefsRoot();
        PrefsLocator locator = new PrefsLocator(isolatedRoot);

        DBConnector connector = new DBConnector();
        connector.setUrl("jdbc:hsqldb:mem:drivertest");
        connector.setUserName("SA");
        connector.setPassword("");
        connector.setJdbcDriver("com.nonexistent.Driver");
        connector.setDbAdapter("org.apache.cayenne.dba.hsqldb.HSQLDBNoSchemaAdapter");
        new DataMapPrefs(locator.dataMapNode(
                PreferenceNodeIds.idForPath(dataMapFile.toAbsolutePath().toString())))
                .setConnector(connector);

        DbImportRunTool isolatedTool = new DbImportRunTool(locator);
        DbImportRunResult result = isolatedTool.run(projectFile.toString(), "TestMap");

        assertEquals("validation_failed", result.status());
        assertEquals(DbImportErrorCode.jdbc_driver_not_loadable, result.error().code());
        assertTrue(result.error().message().contains("com.nonexistent.Driver"));
        assertTrue(result.validation().dbConnectorPresent());
        assertFalse(result.validation().jdbcDriverLoadable());
        assertNull(result.validation().jdbcConnectionOpened());
        assertNotNull(result.resolved(), "resolved should be populated even on driver failure");
        assertEquals("jdbc:hsqldb:mem:drivertest", result.resolved().jdbcUrl());
        assertNoCredentialsInResult(result);
    }

    @Test
    public void jdbcConnectionFailed(@TempDir Path tmp) throws IOException {
        Path projectFile = copyFixture("with-dbimport", tmp);
        Path dataMapFile = tmp.resolve("TestMap.map.xml");

        Preferences isolatedRoot = isolatedPrefsRoot();
        PrefsLocator locator = new PrefsLocator(isolatedRoot);

        DBConnector connector = new DBConnector();
        // HSQL network server on localhost:1 — not running, will refuse
        connector.setUrl("jdbc:hsqldb:hsql://localhost:1/nonexistent");
        connector.setUserName("SA");
        connector.setPassword("");
        connector.setJdbcDriver("org.hsqldb.jdbc.JDBCDriver");
        connector.setDbAdapter("org.apache.cayenne.dba.hsqldb.HSQLDBNoSchemaAdapter");
        new DataMapPrefs(locator.dataMapNode(
                PreferenceNodeIds.idForPath(dataMapFile.toAbsolutePath().toString())))
                .setConnector(connector);

        DbImportRunTool isolatedTool = new DbImportRunTool(locator);
        DbImportRunResult result = isolatedTool.run(projectFile.toString(), "TestMap");

        assertEquals("validation_failed", result.status());
        assertEquals(DbImportErrorCode.jdbc_connection_failed, result.error().code());
        assertTrue(result.validation().jdbcDriverLoadable());
        assertFalse(result.validation().jdbcConnectionOpened());
        assertNotNull(result.resolved());
        assertNoCredentialsInResult(result);
    }

    private static String fixtureProject(String fixture) throws URISyntaxException {
        return Paths.get(DbImportRunValidationTest.class
                .getResource("/dbimport-fixtures/" + fixture + "/cayenne-project.xml")
                .toURI()).toString();
    }

    private Preferences isolatedPrefsRoot() {
        testPrefsRoot = Preferences.userRoot()
                .node("cayenne-test/dbimport-val-" + UUID.randomUUID().toString().replace("-", ""));
        return testPrefsRoot;
    }

    static Path copyFixture(String fixture, Path dir) throws IOException {
        for (String name : new String[]{"cayenne-project.xml", "TestMap.map.xml"}) {
            try (InputStream in = DbImportRunValidationTest.class
                    .getResourceAsStream("/dbimport-fixtures/" + fixture + "/" + name)) {
                Files.copy(in, dir.resolve(name));
            }
        }
        return dir.resolve("cayenne-project.xml");
    }

    private static void assertNoCredentialsInResult(DbImportRunResult result) {
        // Structural guarantee: DbImportResolved has no password/userName fields.
        // Belt-and-suspenders: verify the toString representation contains neither.
        String s = result.toString();
        assertFalse(s.contains("password"), "password must not appear in result");
        assertFalse(s.contains("userName"), "userName must not appear in result");
    }
}
