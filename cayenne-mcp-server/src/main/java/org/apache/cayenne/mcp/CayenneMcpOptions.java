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
 * Parsed command-line options for the MCP server.
 */
public class CayenneMcpOptions {

    private final boolean help;
    private final boolean version;

    private CayenneMcpOptions(boolean help, boolean version) {
        this.help = help;
        this.version = version;
    }

    public static CayenneMcpOptions parse(String[] args) {
        boolean help = false;
        boolean version = false;

        for (String arg : args) {
            switch (arg) {
                case "-h", "--help" -> help = true;
                case "-V", "--version" -> version = true;
                default -> {
                    System.err.println("Unknown option: " + arg);
                    System.err.println("Run with -h for usage.");
                    System.exit(2);
                }
            }
        }

        return new CayenneMcpOptions(help, version);
    }

    public boolean isHelp() {
        return help;
    }

    public boolean isVersion() {
        return version;
    }
}
