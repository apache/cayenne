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

package org.apache.cayenne.tools;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

import org.apache.cayenne.test.jdbc.SQLReader;
import org.apache.cayenne.test.resource.ResourceUtil;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.0
 */
public class DbImportIT extends BaseTaskIT {

    @Test
    public void notConfiguredTaskFailure() throws Exception {
        GradleRunner runner = createRunner("dbimport_failure", "cdbimport", "--info");

        BuildResult result = runner.buildAndFail();

        // new version of Gradle (4.3.1 as of 05/12/2017) seems not return task status, so ignore this
//        assertNotNull(result.task(":cdbimport"));
//        assertEquals(TaskOutcome.FAILED, result.task(":cdbimport").getOutcome());

        assertTrue(result.getOutput().contains("No datamap configured in task or in cayenne.defaultDataMap"));
    }

    @Test
    public void emptyDbTaskSuccess() throws Exception {
        prepareDerbyDatabase("empty_db"); // create empty db to avoid problems on Java 11
        GradleRunner runner = createRunner("dbimport_empty_db", "cdbimport", "--info");

        BuildResult result = runner.build();

        assertNotNull(result.task(":cdbimport"));
        assertEquals(TaskOutcome.SUCCESS, result.task(":cdbimport").getOutcome());

        File dataMap = new File(projectDir.getAbsolutePath() + "/datamap.map.xml");
        assertTrue(dataMap.exists());
        assertTrue(result.getOutput().contains("Detected changes: No changes to import."));
    }

    @Test
    public void emptyDbTaskWithDependency() throws Exception {
        prepareDerbyDatabase("empty_db"); // create empty db to avoid problems on Java 11
        GradleRunner runner = createRunner("dbimport-with-project-dependency", "cdbimport", "--info");

        BuildResult result = runner.build();

        assertNotNull(result.task(":cdbimport"));
        assertEquals(TaskOutcome.SUCCESS, result.task(":cdbimport").getOutcome());

        File dataMap = new File(projectDir.getAbsolutePath() + "/datamap.map.xml");
        assertTrue(dataMap.exists());
        assertTrue(result.getOutput().contains("Detected changes: No changes to import."));
    }

    @Test
    public void simpleDbTaskSuccess() throws Exception {
        String dbUrl = prepareDerbyDatabase("test_map_db");
        File dataMap = new File(projectDir.getAbsolutePath() + "/datamap.map.xml");
        assertFalse(dataMap.exists());

        GradleRunner runner = createRunner("dbimport_simple_db", "cdbimport", "--info", "-PdbUrl=" + dbUrl);

        BuildResult result = runner.build();

        assertNotNull(result.task(":cdbimport"));
        assertEquals(TaskOutcome.SUCCESS, result.task(":cdbimport").getOutcome());

        assertTrue(dataMap.exists());

        // Check few lines from reverse engineering output
        assertTrue(result.getOutput().contains("Table: APP.PAINTING"));
        assertTrue(result.getOutput().contains("Db Relationship : toOne  (EXHIBIT.GALLERY_ID, GALLERY.GALLERY_ID)"));
        assertTrue(result.getOutput().contains("Db Relationship : toMany (GALLERY.GALLERY_ID, PAINTING.GALLERY_ID)"));
        assertTrue(result.getOutput().contains("Create Table         ARTIST"));
        assertFalse(result.getOutput().contains("Create Table         PAINTING1"));
        assertTrue(result.getOutput().contains("Skip relation: '.APP.ARTIST.ARTIST_ID <- .APP.PAINTING1.ARTIST_ID # 1'"));
        assertTrue(result.getOutput().contains("Migration Complete Successfully."));
    }

    @Test
    public void excludeRelDbTaskSuccess() throws Exception {
        String dbUrl = prepareDerbyDatabase("exclude_Table");
        File dataMap = new File(projectDir.getAbsolutePath() + "/datamap.map.xml");
        assertFalse(dataMap.exists());

        GradleRunner runner = createRunner("dbimport_excludeRel", "cdbimport", "--info", "-PdbUrl=" + dbUrl);

        BuildResult result = runner.build();

        assertNotNull(result.task(":cdbimport"));
        assertEquals(TaskOutcome.SUCCESS, result.task(":cdbimport").getOutcome());

        assertTrue(dataMap.exists());

        // Check few lines from reverse engineering output
        assertTrue(result.getOutput().contains("Table: SCHEMA_01.TEST1"));
        assertTrue(result.getOutput().contains("Table: SCHEMA_01.TEST2"));
    }

    @Test
    public void withProjectTaskSuccess() throws Exception {
        String dbUrl = prepareDerbyDatabase("test_project_db");
        File dataMap = new File(projectDir.getAbsolutePath() + "/datamap.map.xml");
        assertFalse(dataMap.exists());
        File project = new File(projectDir.getAbsolutePath() + "/cayenne-project.xml");
        assertFalse(project.exists());

        GradleRunner runner = createRunner("dbimport_with_project", "cdbimport", "--info", "-PdbUrl=" + dbUrl);

        BuildResult result = runner.build();

        assertNotNull(result.task(":cdbimport"));
        assertEquals(TaskOutcome.SUCCESS, result.task(":cdbimport").getOutcome());

        assertTrue(dataMap.isFile());
        assertTrue(project.isFile());

        // Check few lines from reverse engineering output
        assertTrue(result.getOutput().contains("Table: APP.PAINTING"));
        assertTrue(result.getOutput().contains("Db Relationship : toOne  (EXHIBIT.GALLERY_ID, GALLERY.GALLERY_ID)"));
        assertTrue(result.getOutput().contains("Db Relationship : toMany (GALLERY.GALLERY_ID, PAINTING.GALLERY_ID)"));
        assertTrue(result.getOutput().contains("Create Table         ARTIST"));
        assertFalse(result.getOutput().contains("Create Table         PAINTING1"));
        assertTrue(result.getOutput().contains("Skip relation: '.APP.ARTIST.ARTIST_ID <- .APP.PAINTING1.ARTIST_ID # 1'"));
        assertTrue(result.getOutput().contains("Migration Complete Successfully."));
    }

    private String prepareDerbyDatabase(String sqlFile) throws Exception {
        URL sqlUrl = Objects.requireNonNull(ResourceUtil.getResource(getClass(), sqlFile + ".sql"));
        String dbUrl = "jdbc:derby:" + projectDir.getAbsolutePath() + "/build/" + sqlFile;

        // Try to open connection, it may fail at first time, so ignore it
        try (Connection unused = DriverManager.getConnection(dbUrl + ";create=true")) {
        } catch (SQLException ignore) {
        }

        try (Connection connection = DriverManager.getConnection(dbUrl + ";create=true")) {
            try (Statement stmt = connection.createStatement()) {
                for (String sql : SQLReader.statements(sqlUrl, ";")) {
                    stmt.execute(sql);
                }
            }
        }

        // shutdown Derby DB, so it can be used by test build later
        try(Connection connection = DriverManager.getConnection(dbUrl + ";shutdown=true")) {
        } catch (SQLException ignored) {
            // should be thrown according to the Derby docs...
        }

        return dbUrl + ";create=true";
    }
}