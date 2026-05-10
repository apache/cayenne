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

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.McpJsonMapperSupplier;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.cayenne.mcp.tools.cgen.CgenRunTool;
import org.apache.cayenne.mcp.tools.hello.HelloTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ServiceLoader;

/**
 * Assembles and runs the MCP server on stdio.
 */
public class CayenneMcpServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CayenneMcpServer.class);

    public void run(String version, InputStream in, OutputStream out) {
        LOGGER.info("Starting Cayenne MCP server {}", version);

        McpJsonMapper jsonMapper = ServiceLoader.load(McpJsonMapperSupplier.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No McpJsonMapperSupplier found on classpath"))
                .get();

        StdioServerTransportProvider transport = new StdioServerTransportProvider(jsonMapper, in, out);

        McpSyncServer server = McpServer.sync(transport)
                .serverInfo("cayenne-mcp-server", version)
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .tools(HelloTool.spec(), CgenRunTool.spec(jsonMapper))
                .build();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Cayenne MCP server stopping");
            server.closeGracefully();
        }));

        LOGGER.info("Cayenne MCP server started, listening on stdio");
    }
}
