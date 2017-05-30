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

import java.io.IOException;
import java.net.URLDecoder;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @since 4.0
 */
public class DbGenerateTaskIT extends BaseTaskIT {

    @Test
    public void notConfiguredTaskFailure() throws IOException {
        GradleRunner runner = createRunner("cdbgen_failure", "cdbgen", "--info");

        BuildResult result = runner.buildAndFail();

        // NOTE: There will be no result for the task, as build will fail earlier because
        // datamap is required parameter that is validated directly by Gradle before task execution.
        //assertNotNull(result.task(":cdbgen"));
        //assertEquals(TaskOutcome.FAILED, result.task(":cdbgen").getOutcome());

        assertTrue(result.getOutput().contains("No datamap configured in task or in cayenne.defaultDataMap"));
    }

    @Test
    public void defaultConfigTaskSuccess() throws Exception {
        String dbUrl = "jdbc:derby:build/testdb";

        GradleRunner runner = createRunner(
                "cdbgen_simple",
                "cdbgen",
                "-PdbUrl=" + dbUrl,
                "-PdataMap=" + URLDecoder.decode(getClass().getResource("test_datamap.map.xml").getFile(), "UTF-8"),
                "--info"
        );

        BuildResult result = runner.build();

        assertNotNull(result.task(":cdbgen"));
        assertEquals(TaskOutcome.SUCCESS, result.task(":cdbgen").getOutcome());

        assertTrue(result.getOutput().contains(
                "generator options - [dropTables: false, dropPK: false, createTables: true, createPK: true, createFK: true]"));

        /* // check that DB is really created
        try (Connection connection = DriverManager.getConnection(dbUrl)) {
            try (ResultSet rs = connection.getMetaData()
                    .getTables(null, null, "artist", new String[]{"TABLE"})) {
                assertTrue(rs.next());
            }
        } */
    }

    @Test
    public void customConfigTaskSuccess() throws IOException {
        GradleRunner runner = createRunner(
                "cdbgen_custom",
                "customCdbgen",
                "-PdataMap=" + URLDecoder.decode(getClass().getResource("test_datamap.map.xml").getFile(), "UTF-8"),
                "--info"
        );

        BuildResult result = runner.build();

        assertNotNull(result.task(":customCdbgen"));
        assertEquals(TaskOutcome.SUCCESS, result.task(":customCdbgen").getOutcome());

        assertTrue(result.getOutput().contains(
                "generator options - [dropTables: true, dropPK: true, createTables: false, createPK: false, createFK: false]"));
    }

}