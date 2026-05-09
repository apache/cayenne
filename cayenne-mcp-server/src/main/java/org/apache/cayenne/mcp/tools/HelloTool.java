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
package org.apache.cayenne.mcp.tools;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

/**
 * Placeholder tool that returns "hello world". Verifies the MCP wiring is functional.
 */
public class HelloTool {

    public static final String NAME = "hello";

    public static McpServerFeatures.SyncToolSpecification spec() {
        McpSchema.Tool tool = new McpSchema.Tool(
                NAME,
                null,
                "Returns a greeting. Use this to verify the Cayenne MCP server is running.",
                new McpSchema.JsonSchema("object", null, null, null, null, null),
                null,
                null,
                null
        );

        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, request) ->
                McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent("hello world")))
                        .isError(false)
                        .build()
        );
    }
}
