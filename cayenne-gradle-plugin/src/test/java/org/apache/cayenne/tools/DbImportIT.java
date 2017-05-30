/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import java.io.IOException;
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
    public void notConfiguredTaskFailure() throws IOException {
        GradleRunner runner = createRunner("dbimport_failure", "cdbimport", "--info");

        BuildResult result = runner.buildAndFail();

        assertNotNull(result.task(":cdbimport"));
        assertEquals(TaskOutcome.FAILED, result.task(":cdbimport").getOutcome());

        assertTrue(result.getOutput().contains("No datamap configured in task or in cayenne.defaultDataMap"));
    }

    @Test
    public void emptyDbTaskSuccess() throws IOException {
        GradleRunner runner = createRunner("dbimport_empty_db", "cdbimport", "--info");

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

    private String prepareDerbyDatabase(String sqlFile) throws Exception {
        URL sqlUrl = Objects.requireNonNull(ResourceUtil.getResource(getClass(), sqlFile + ".sql"));
        String dbUrl = "jdbc:derby:" + projectDir.getAbsolutePath() + "/build/" + sqlFile;
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