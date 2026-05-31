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
import org.apache.cayenne.mcp.tools.dbimport.protocol.DbImportRunResult;
import org.apache.cayenne.modeler.pref.PreferenceNodeIds;
import org.apache.cayenne.modeler.pref.PrefsLocator;
import org.apache.cayenne.modeler.pref.adapters.DataMapPrefs;
import org.apache.cayenne.modeler.pref.dbconnector.DBConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

public class DbImportRunIT {

    private static final String HSQL_ADAPTER = HSQLDBAdapter.class.getName();
    private static final String HSQL_DRIVER = "org.hsqldb.jdbc.JDBCDriver";

    @TempDir
    Path tempDir;

    private String dbUrl;
    private Connection dbConn;
    private Preferences prefsRoot;
    private PrefsLocator prefsLocator;
    private DbImportRunTool tool;
    private Path projectFile;
    private Path dataMapFile;

    @BeforeEach
    public void setUp() throws Exception {
        dbUrl = "jdbc:hsqldb:mem:dbimport_it_%s;shutdown=true"
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

        prefsRoot = Preferences.userRoot()
                .node("cayenne-test/dbimport-it-" + UUID.randomUUID().toString().replace("-", ""));
        prefsLocator = new PrefsLocator(prefsRoot);
        tool = new DbImportRunTool(prefsLocator);

        // Write fixture files
        projectFile = DbImportRunValidationTest.copyFixture("with-dbimport", tempDir);
        dataMapFile = tempDir.resolve("TestMap.map.xml");

        // Store connector in prefs
        storeConnector(dbUrl, "SA", "", HSQL_DRIVER, HSQL_ADAPTER);
    }

    @AfterEach
    public void tearDown() throws Exception {
        prefsRoot.removeNode();
        if (dbConn != null && !dbConn.isClosed()) {
            dbConn.close();
        }
    }

    @Test
    public void happyPath() throws IOException {
        DbImportRunResult result = tool.run(projectFile.toString(), "TestMap");

        assertEquals("imported", result.status());
        assertNull(result.error());
        assertNotNull(result.resolved());
        assertEquals(dbUrl, result.resolved().jdbcUrl());
        assertEquals(HSQL_DRIVER, result.resolved().jdbcDriver());

        assertTrue(result.summary().tokensApplied() > 0);
        assertEquals(result.summary().tokensConsidered(), result.summary().tokensApplied());
        assertEquals(2, result.summary().entitiesAdded(), "Expected 2 new entities (artist, painting)");
        assertEquals(0, result.summary().entitiesRemoved());
        assertTrue(result.summary().relationshipsAdded() >= 0, "Relationship count must be non-negative");

        assertTrue(result.validation().projectFound());
        assertTrue(result.validation().dataMapFound());
        assertTrue(result.validation().dbConnectorPresent());
        assertTrue(result.validation().jdbcDriverLoadable());
        assertTrue(result.validation().jdbcConnectionOpened());

        // DataMap XML on disk should now contain the two db-entities
        String xml = Files.readString(dataMapFile);
        assertTrue(xml.contains("artist"), "DataMap XML should contain 'artist' entity");
        assertTrue(xml.contains("painting"), "DataMap XML should contain 'painting' entity");
    }

    @Test
    public void idempotency() {
        tool.run(projectFile.toString(), "TestMap");

        DbImportRunResult second = tool.run(projectFile.toString(), "TestMap");

        assertEquals("up_to_date", second.status());
        assertNull(second.error());
        assertEquals(0, second.summary().tokensConsidered());
        assertEquals(0, second.summary().tokensApplied());
        assertEquals(0, second.summary().entitiesAdded());
        assertEquals(0, second.summary().entitiesModified());
        assertEquals(0, second.summary().entitiesRemoved());
        assertEquals(0, second.summary().relationshipsAdded());
    }

    @Test
    public void drift() throws Exception {
        // First import — establishes baseline
        tool.run(projectFile.toString(), "TestMap");

        // Add a column to the live DB
        try (Statement s = dbConn.createStatement()) {
            s.execute("ALTER TABLE artist ADD COLUMN bio VARCHAR(1000)");
        }

        DbImportRunResult result = tool.run(projectFile.toString(), "TestMap");

        assertEquals("imported", result.status(), result.error() != null ? result.error().message() : "");
        assertEquals(1, result.summary().tokensApplied());
        assertEquals(1, result.summary().entitiesModified());
        assertEquals(0, result.summary().entitiesAdded());

        // Verify column is in the XML
        String xml = Files.readString(dataMapFile);
        assertTrue(xml.contains("bio"), "bio attribute should be present in DataMap XML");
    }

    @Test
    public void connectorNotConfigured() {
        // Clear the connector from prefs
        new DataMapPrefs(prefsLocator.dataMapNode(
                PreferenceNodeIds.idForPath(dataMapFile.toUri().getRawPath())))
                .setConnector(new DBConnector()); // empty connector clears URL sentinel

        DbImportRunResult result = tool.run(projectFile.toString(), "TestMap");

        assertEquals("validation_failed", result.status());
        assertFalse(result.validation().dbConnectorPresent());
        assertNull(result.validation().jdbcDriverLoadable());
    }

    private void storeConnector(
            String url,
            String user,
            String password,
            String driver,
            String adapter) {

        DBConnector c = new DBConnector();
        c.setUrl(url);
        c.setUserName(user);
        c.setPassword(password);
        c.setJdbcDriver(driver);
        c.setDbAdapter(adapter);

        Preferences mapPrefs = prefsLocator.dataMapNode(PreferenceNodeIds.idForPath(dataMapFile.toUri().getRawPath()));
        new DataMapPrefs(mapPrefs).setConnector(c);
    }
}
