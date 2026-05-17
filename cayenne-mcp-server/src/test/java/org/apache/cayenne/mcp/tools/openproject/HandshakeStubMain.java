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
package org.apache.cayenne.mcp.tools.openproject;

import java.time.Instant;
import java.util.prefs.Preferences;

/**
 * Stand-alone main that mimics what CayenneModeler does on the handshake side: parses
 * {@code --mcp-handshake <nonce>}, writes the four preferences keys, flushes, and
 * sleeps. Used by {@code OpenProjectStubIT} to drive the launch + handshake round-trip
 * without requiring a real Modeler build.
 */
public class HandshakeStubMain {

    public static void main(String[] args) throws Exception {
        String nonce = null;
        String projectPath = null;
        for (int i = 0; i < args.length; i++) {
            if ("--mcp-handshake".equals(args[i]) && i + 1 < args.length) {
                nonce = args[i + 1];
                i++;
                continue;
            }
            // Last positional arg is the project path (matches the launcher contract).
            projectPath = args[i];
        }

        if (nonce != null) {
            Preferences prefs = Preferences.userRoot()
                    .node("/org/apache/cayenne/modeler/mcp-handshake/" + nonce);
            prefs.put("startedAt", Instant.now().toString());
            prefs.putLong("pid", ProcessHandle.current().pid());
            prefs.put("args", String.join(" ", args));
            prefs.put("projectPath", projectPath != null ? projectPath : "");
            prefs.flush();
        }

        // Sleep until killed by the test (or for an unreasonable bound that prevents
        // a forgotten process from sticking around).
        Thread.sleep(60_000);
    }
}
