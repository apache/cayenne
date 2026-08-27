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

package org.apache.cayenne.modeler.mcp;

import org.apache.cayenne.modeler.pref.PrefsLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Writes a "Modeler is up and the requested project is loaded" handshake entry into
 * {@link Preferences} under a nonce-scoped node. Used by the MCP server's {@code open_project}
 * tool to confirm the launch succeeded without polling {@code Process.isAlive()}.
 */
public class McpHandshakeWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpHandshakeWriter.class);

    /**
     * Asynchronously writes the handshake. Returns immediately; the actual write happens
     * on a short-lived daemon thread so the EDT is not blocked by a slow preferences backend.
     */
    public static void write(PrefsLocator locator, String nonce, String[] originalArgs, String resolvedProjectPath) {
        Thread t = new Thread(() -> doWrite(locator, nonce, originalArgs, resolvedProjectPath),
                "mcp-handshake-writer");
        t.setDaemon(true);
        t.start();
    }

    private static void doWrite(PrefsLocator locator, String nonce, String[] originalArgs, String resolvedProjectPath) {
        try {
            Preferences prefs = locator.handshakeNode(nonce);
            prefs.put("startedAt", Instant.now().toString());
            prefs.putLong("pid", ProcessHandle.current().pid());
            prefs.put("args", originalArgs != null ? String.join(" ", originalArgs) : "");
            prefs.put("projectPath", resolvedProjectPath != null ? resolvedProjectPath : "");
            prefs.flush();
        } catch (BackingStoreException | RuntimeException e) {
            // Never propagate - a prefs failure must not crash the Modeler. The MCP-side
            // wait loop will translate the missing handshake into LAUNCH_NOT_CONFIRMED.
            LOGGER.warn("Failed to write MCP handshake for nonce {}: {}", nonce, e.toString());
        }
    }
}
