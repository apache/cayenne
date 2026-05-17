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

package org.apache.cayenne.modeler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Parsed CayenneModeler command line. Grammar:
 * <pre>
 * CayenneModeler [--mcp-handshake &lt;nonce&gt;] [&lt;projectPath&gt;]
 * </pre>
 * Parsing is lenient: unknown flags and malformed values are logged and ignored so the
 * Modeler always starts. The Modeler is end-user software, not a build tool.
 */
public record CliArgs(File initialProject, String mcpHandshakeNonce, String[] rawArgs) {

    private static final Logger LOGGER = LoggerFactory.getLogger(CliArgs.class);

    static final String MCP_HANDSHAKE_FLAG = "--mcp-handshake";

    public static CliArgs parse(String[] args) {
        String[] safeArgs = args != null ? args : new String[0];

        String nonce = null;
        File project = null;

        for (int i = 0; i < safeArgs.length; i++) {
            String arg = safeArgs[i];
            if (MCP_HANDSHAKE_FLAG.equals(arg)) {
                if (i + 1 < safeArgs.length) {
                    nonce = safeArgs[++i];
                } else {
                    LOGGER.warn("{} flag is missing a value, ignoring", MCP_HANDSHAKE_FLAG);
                }
            } else if (arg.startsWith("--")) {
                LOGGER.warn("Ignoring unrecognised flag: {}", arg);
            } else if (project == null) {
                project = validProjectFile(arg);
            } else {
                LOGGER.warn("Ignoring extra positional argument: {}", arg);
            }
        }

        return new CliArgs(project, nonce, safeArgs);
    }

    private static File validProjectFile(String pathArg) {
        File f = new File(pathArg);
        if (f.isFile()
                && f.getName().startsWith("cayenne")
                && f.getName().endsWith(".xml")) {
            return f;
        }
        return null;
    }
}
