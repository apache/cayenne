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
package org.apache.cayenne.mcp;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for server startup flags.
 * Requires the shaded jar to be built before this test runs (failsafe executes after package).
 */
public class CayenneMcpServerIT {

    @Test
    public void unknownArgExitsWithCode2() throws Exception {
        Process process = McpStarter.start("--unknown-flag");
        assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Process did not exit");
        assertEquals(2, process.exitValue(), "Expected exit code 2 for unknown arg");
    }

    @Test
    public void helpFlagExitsWithCode0() throws Exception {
        Process process = McpStarter.start("--help");
        assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Process did not exit");
        assertEquals(0, process.exitValue(), "Expected exit code 0 for --help");
    }

    @Test
    public void versionFlagExitsWithCode0() throws Exception {
        Process process = McpStarter.start("--version");
        assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Process did not exit");
        assertEquals(0, process.exitValue(), "Expected exit code 0 for --version");

        String err = new String(process.getErrorStream().readAllBytes());
        assertTrue(err.contains("cayenne-mcp-server"), "Version output missing server name: " + err);
    }


}
