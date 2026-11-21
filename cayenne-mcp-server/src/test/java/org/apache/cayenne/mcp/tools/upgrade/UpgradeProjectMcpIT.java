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

import org.apache.cayenne.mcp.TestMcpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the {@code upgrade_project} tool over the MCP stdio protocol. Complements
 * {@link UpgradeProjectToolTest}, which calls the tool directly.
 */
public class UpgradeProjectMcpIT {

    @RegisterExtension
    TestMcpClient client = new TestMcpClient();

    @Test
    public void toolsListIncludesUpgradeProject() {
        client.send("""
                {"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}""");

        String response = client.readLine(5_000);
        assertTrue(response.contains("\"upgrade_project\""), "tools/list missing 'upgrade_project': " + response);
    }

    @Test
    public void projectNotFound() {
        client.send("""
                {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{\
                "name":"upgrade_project","arguments":{\
                "projectPath":"/no/such/file.xml"}}}""");

        assertEquals("""
                {
                  "status" : "error",
                  "fromVersion" : null,
                  "toVersion" : "15",
                  "changes" : [ ],
                  "error" : {
                    "code" : "project_not_found",
                    "message" : "No readable file at /no/such/file.xml"
                  }
                }""", client.readLineAsJson(5_000));
    }
}
