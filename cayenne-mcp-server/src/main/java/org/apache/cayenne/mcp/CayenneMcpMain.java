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

/**
 * Entry point for the Cayenne MCP server.
 */
public class CayenneMcpMain {

    public static void main(String[] args) {
        StderrLogging.install();

        CayenneMcpOptions opts = CayenneMcpOptions.parse(args);

        String version = version();

        if (opts.isHelp()) {
            System.err.println("Usage: java -jar cayenne-mcp-server.jar [options]");
            System.err.println();
            System.err.println("Options:");
            System.err.println("  -h, --help     Print this help and exit.");
            System.err.println("  -V, --version  Print the server version and exit.");
            System.exit(0);
        }

        if (opts.isVersion()) {
            System.err.println("cayenne-mcp-server " + version);
            System.exit(0);
        }

        new CayenneMcpServer().run(version);
    }

    static String version() {
        String v = CayenneMcpMain.class.getPackage().getImplementationVersion();
        return v != null ? v : "dev";
    }
}
