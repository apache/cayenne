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

import java.nio.file.Path;

/**
 * Parsed command-line options for the MCP server.
 */
public class CayenneMcpOptions {

    private final boolean help;
    private final boolean version;
    private final Path cacheDir;

    private CayenneMcpOptions(boolean help, boolean version, Path cacheDir) {
        this.help = help;
        this.version = version;
        this.cacheDir = cacheDir;
    }

    public static CayenneMcpOptions parse(String[] args) {
        boolean help = false;
        boolean version = false;
        Path cacheDir = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h", "--help" -> help = true;
                case "-V", "--version" -> version = true;
                case "--cache-dir" -> {
                    if (i + 1 >= args.length) {
                        fail("Option --cache-dir requires a directory argument");
                    }
                    String value = args[++i];
                    cacheDir = value.isBlank() ? null : Path.of(value);
                }
                default -> fail("Unknown option: " + args[i]);
            }
        }

        return new CayenneMcpOptions(help, version, cacheDir);
    }

    private static void fail(String message) {
        System.err.println(message);
        System.err.println("Run with -h for usage.");
        System.exit(2);
    }

    public boolean isHelp() {
        return help;
    }

    public boolean isVersion() {
        return version;
    }

    /**
     * A directory to cache the downloaded files in (e.g. the CayenneModeler jar), or {@code null} for the default one.
     *
     * @since 6.0
     */
    public Path getCacheDir() {
        return cacheDir;
    }
}
